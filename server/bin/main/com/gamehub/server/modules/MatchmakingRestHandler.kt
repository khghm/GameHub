package com.gamehub.server.modules

import com.gamehub.server.bot.CentralBotManager
import com.gamehub.server.matchmaking.MatchmakingService
import com.gamehub.server.rating.RatingService
import com.gamehub.server.repository.UserTutorialRepository
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.coroutines.delay
import java.util.UUID

object MatchmakingRestHandler {
    lateinit var service: MatchmakingService
    lateinit var centralBotManager: CentralBotManager
    lateinit var ratingService: RatingService
    lateinit var userTutorialRepository: UserTutorialRepository

    // Configurable max games per user type and game
    private val MAX_GAMES_DEFAULT = 3
    private val MAX_GAMES_GUEST = 2
    private val GAME_SPECIFIC_MAX_GAMES: Map<String, Int> = mapOf(
        // Example: "blokus" to 2, etc. - can be expanded later
    )

    private fun getMaxGames(playerId: String, playerName: String, gameType: String): Int {
        val isGuest = playerName.startsWith("Guest_")
        return GAME_SPECIFIC_MAX_GAMES[gameType] ?: if (isGuest) MAX_GAMES_GUEST else MAX_GAMES_DEFAULT
    }

    suspend fun joinQueue(playerId: String, playerName: String, gameType: String, mode: String = "casual"): String {
        // ===== 1. بررسی محدودیت تعداد بازی همزمان =====
        val activeCount = GameSessionManager.getActiveGamesCount(playerId)
        val maxGames = getMaxGames(playerId, playerName, gameType)
        if (activeCount >= maxGames) {
            return """{"status":"error","message":"شما در حال حاضر $activeCount بازی فعال دارید. لطفاً یکی را تمام کنید."}"""
        }

        val playerCountNeeded = when (gameType) {
            "blokus" -> 4
            "spades-baloot" -> 4
            else -> 2
        }

        // ===== 2. بررسی آموزش (Tutorial) – فقط یک بار =====
        val hasCompletedTutorial = checkTutorialCompleted(playerId, gameType)
        if (!hasCompletedTutorial && mode == "casual" && this::centralBotManager.isInitialized) {
            try {
                val allPlayers = mutableListOf(playerId)
                val botDifficulties = mutableMapOf<String, Int>()

                val tutorialBot = centralBotManager.createTutorialBot(PlayerId(playerId), gameType)
                allPlayers.add(tutorialBot.botId.value)
                botDifficulties[tutorialBot.botId.value] = tutorialBot.difficultyLevel

                // Add additional bots if needed for games that require more than 2 players
                if (playerCountNeeded > 2) {
                    for (i in 3..playerCountNeeded) {
                        val botProfile = centralBotManager.createShadowBot(gameType, difficultyLevel = 3)
                        allPlayers.add(botProfile.botId.value)
                        botDifficulties[botProfile.botId.value] = botProfile.difficultyLevel
                    }
                }

                val gameId = UUID.randomUUID().toString()
                // Assign all players
                service.assignMatch(gameId, *allPlayers.toTypedArray())
                GameSessionManager.createSession(
                    gameType = gameType,
                    playerIds = allPlayers,
                    gameId = gameId,
                    botDifficultyLevels = botDifficulties
                )
                markTutorialCompleted(playerId, gameType)
                return """{"status":"matched","gameId":"$gameId","isTutorial":true}"""
            } catch (e: Exception) {
                println("⚠️ Failed to create tutorial bot: ${e.message}")
            }
        }

        // ===== 3. بررسی مچ از پیش تعیین شده (Reconnect) =====
        var assignedGameId: String? = service.getAssignedGame(playerId)
        if (assignedGameId != null) {
            service.clearAssignedGame(playerId)
            return """{"status":"matched","gameId":"$assignedGameId"}"""
        }

        // ===== 4. اضافه کردن به صف مچ‌میکینگ =====
        val ratingInfo = ratingService.getRating(playerId, gameType)
        val initialRating = SkillRating(mean = ratingInfo.rating.toDouble(), standardDeviation = 350.0, volatility = 0.06)
        service.enqueueSolo(playerId, gameType, mode, initialRating, region = "IR")
        println("📥 [$playerId] added to queue for $gameType ($mode)")

        // ===== 5. منتظر مچ از طریق push-based system =====
        var waitTime = 0
        val startTime = System.currentTimeMillis()
        val maxWaitTime = if (playerCountNeeded == 2) 30000 else 60000 // 30 ثانیه برای 2 نفره، 60 برای چند نفره

        assignedGameId = null
        while (waitTime < maxWaitTime) {
            assignedGameId = service.getAssignedGame(playerId)
            if (assignedGameId != null) break
            delay(500)
            waitTime = (System.currentTimeMillis() - startTime).toInt()
        }

        if (assignedGameId != null) {
            service.clearAssignedGame(playerId)
            val session = GameSessionManager.getSession(assignedGameId)
            val allPlayers = session?.players?.map { it.value } ?: listOf(playerId)
            if (allPlayers.size == playerCountNeeded) {
                return if (allPlayers.size == 2) {
                    """{"status":"matched","gameId":"$assignedGameId"}"""
                } else {
                    """{"status":"matched","gameId":"$assignedGameId","playerIds":[${allPlayers.joinToString { "\"$it\"" }}]}"""
                }
            }
        }

        // ===== 6. Fallback به ربات معمولی (فقط Casual) =====
        if (mode == "casual" && this::centralBotManager.isInitialized) {
            try {
                val playerCountNeeded = when (gameType) {
                    "blokus" -> 4
                    else -> 2
                }
                val allPlayers = mutableListOf(playerId)
                val botDifficulties = mutableMapOf<String, Int>()

                // Add all required bots
                for (i in 1..(playerCountNeeded - 1)) {
                    val targetDifficulty = 3
                    val botProfile = if (i == 1) {
                        service.requestBot(gameType, targetDifficulty, setOf(playerId))
                    } else {
                        centralBotManager.createShadowBot(gameType, difficultyLevel = targetDifficulty)
                    }
                    if (botProfile != null) {
                        allPlayers.add(botProfile.botId.value)
                        botDifficulties[botProfile.botId.value] = botProfile.difficultyLevel
                    } else {
                        // If we can't get a bot, create a shadow bot
                        val shadowBot = centralBotManager.createShadowBot(gameType, 3)
                        allPlayers.add(shadowBot.botId.value)
                        botDifficulties[shadowBot.botId.value] = shadowBot.difficultyLevel
                    }
                }

                if (allPlayers.size == playerCountNeeded) {
                    val gameId = UUID.randomUUID().toString()
                    // Assign all players
                    service.assignMatch(gameId, *allPlayers.toTypedArray())
                    GameSessionManager.createSession(
                        gameType = gameType,
                        playerIds = allPlayers,
                        gameId = gameId,
                        botDifficultyLevels = botDifficulties
                    )
                    return """{"status":"matched","gameId":"$gameId","isBot":true}"""
                } else {
                    println("⚠️ Not enough bots available for $gameType")
                }
            } catch (e: Exception) {
                println("⚠️ Failed to assign bot: ${e.message}")
            }
        }

        return """{"status":"waiting","message":"No opponent or bot available"}"""
    }

    suspend fun leaveQueue(playerId: String, gameType: String, mode: String = "casual"): String {
        service.leaveSoloQueue(playerId, gameType, mode)
        return """{"status":"success","message":"Successfully left the queue"}"""
    }

    private suspend fun checkTutorialCompleted(userId: String, gameId: String): Boolean {
        return userTutorialRepository.isCompleted(userId, gameId)
    }

    private suspend fun markTutorialCompleted(userId: String, gameId: String) {
        userTutorialRepository.markCompleted(userId, gameId)
        println("📚 Tutorial completed for $userId in game $gameId")
    }
}