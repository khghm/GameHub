package com.gamehub.server.bot

import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.bot.BotPoolEntry
import com.gamehub.shared.bot.BotProfile
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import java.util.concurrent.ConcurrentHashMap

class CentralBotManager(
    private val cache: CacheProvider,
    private val botProfileRepository: BotProfileRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val poolLocks = ConcurrentHashMap<String, Mutex>()

    companion object {
        private const val BOT_POOL_PREFIX = "bot:pool:"
        private const val BOT_INUSE_PREFIX = "bot:inuse:"
        private const val BOT_PROFILE_PREFIX = "bot:profile:"
    }

    suspend fun assignBot(gameId: String, targetDifficulty: Int, excludeBotIds: Set<PlayerId> = emptySet()): BotProfile? {
        val lock = poolLocks.getOrPut(gameId) { Mutex() }
        return lock.withLock {
            val poolKey = "${BOT_POOL_PREFIX}${gameId}"
            val allEntries = cache.zrangebyscore(poolKey, 0.0, 100.0, 1000)
            if (allEntries.isEmpty()) return@withLock null

            var bestEntry: BotPoolEntry? = null
            var bestDiff = Int.MAX_VALUE
            for (entryJson in allEntries) {
                val entry = json.decodeFromString<BotPoolEntry>(entryJson)
                if (entry.gameId != gameId) continue
                if (excludeBotIds.contains(entry.botId)) continue
                val diff = abs(entry.difficultyLevel - targetDifficulty)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestEntry = entry
                }
            }

            val selected = bestEntry ?: return@withLock null
            cache.zrem(poolKey, json.encodeToString(selected))
            val inuseKey = "${BOT_INUSE_PREFIX}${selected.botId.value}"
            cache.set(inuseKey, "occupied", 3600)
            botProfileRepository.updateLastGame(selected.botId.value)
            return getBotProfile(selected.botId)
        }
    }

    suspend fun releaseBot(botId: PlayerId, gameResult: String, newRating: SkillRating? = null) {
        val inuseKey = "${BOT_INUSE_PREFIX}${botId.value}"
        cache.delete(inuseKey)

        val profile = botProfileRepository.findById(botId.value) ?: return
        val newWins = profile.wins + if (gameResult == "win") 1 else 0
        val newLosses = profile.losses + if (gameResult == "loss") 1 else 0
        val newTotal = profile.totalGames + 1
        val newRatingValue = newRating ?: profile.rating
        botProfileRepository.updateStats(botId.value, newTotal, newWins, newLosses, newRatingValue)

        val updatedProfile = profile.copy(
            totalGames = newTotal,
            wins = newWins,
            losses = newLosses,
            rating = newRatingValue,
            totalGamesPlayed = profile.totalGamesPlayed + 1,
            winCount = profile.winCount + if (gameResult == "win") 1 else 0,
            lossCount = profile.lossCount + if (gameResult == "loss") 1 else 0,
            lastGameAt = System.currentTimeMillis()
        )
        addToPool(updatedProfile)
    }

    suspend fun addToPool(profile: BotProfile) {
        val poolKey = "${BOT_POOL_PREFIX}${profile.gameId}"
        val entry = BotPoolEntry(
            botId = profile.botId,
            gameId = profile.gameId,
            difficultyLevel = profile.difficultyLevel,
            rating = profile.rating
        )
        cache.zadd(poolKey, profile.difficultyLevel.toDouble(), json.encodeToString(entry))
        cache.set("${BOT_PROFILE_PREFIX}${profile.botId.value}", json.encodeToString(profile), 86400)
    }

    suspend fun createTutorialBot(userId: PlayerId, gameId: String): BotProfile {
        val botId = PlayerId("tutorial_${userId.value}_$gameId")
        val profile = BotProfile(
            botId = botId,
            username = BotProfileGenerator.generateTutorialUsername(gameId),
            avatarId = BotProfileGenerator.generateTutorialAvatarId(),
            gameId = gameId,
            difficultyLevel = 1,
            rating = SkillRating(mean = 800.0, standardDeviation = 200.0),
            isTutorial = true,
            isShadow = false,
            totalGamesPlayed = 0,
            winCount = 0,
            lossCount = 0
        )
        botProfileRepository.save(profile)
        addToPool(profile)
        return profile
    }

    suspend fun createShadowBot(gameId: String, difficultyLevel: Int): BotProfile {
        val botId = PlayerId("shadow_${gameId}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0, 8)}")
        val profile = BotProfile(
            botId = botId,
            username = BotProfileGenerator.generateUsername(),
            avatarId = BotProfileGenerator.generateAvatarId(),
            gameId = gameId,
            difficultyLevel = difficultyLevel,
            rating = calculateInitialRating(difficultyLevel),
            isTutorial = false,
            isShadow = true,
            totalGamesPlayed = 0,
            winCount = 0,
            lossCount = 0
        )
        botProfileRepository.save(profile)
        addToPool(profile)
        return profile
    }

    suspend fun initializePool(gameId: String? = null) {
        val bots = botProfileRepository.findAllActive(gameId)
        for (bot in bots) {
            addToPool(bot)
        }
        println("🤖 Bot pool initialized with ${bots.size} bots for game ${gameId ?: "all"}")
    }

    suspend fun rotateBotProfile(botId: String) {
        val profile = botProfileRepository.findById(botId) ?: return
        if (profile.isTutorial) return // آموزشی را تغییر نده
        val newUsername = BotProfileGenerator.generateUsername()
        val newAvatarId = BotProfileGenerator.generateAvatarId()
        botProfileRepository.rotateProfile(botId, newUsername, newAvatarId)
        // آپدیت کش
        val updatedProfile = profile.copy(username = newUsername, avatarId = newAvatarId, lastRotation = System.currentTimeMillis())
        cache.set("${BOT_PROFILE_PREFIX}$botId", json.encodeToString(updatedProfile), 86400)
        // به‌روزرسانی در پول (حذف و اضافه مجدد)
        val poolKey = "${BOT_POOL_PREFIX}${profile.gameId}"
        val oldEntry = BotPoolEntry(profile.botId, profile.gameId, profile.difficultyLevel, profile.rating)
        cache.zrem(poolKey, json.encodeToString(oldEntry))
        val newEntry = BotPoolEntry(profile.botId, profile.gameId, profile.difficultyLevel, profile.rating)
        cache.zadd(poolKey, profile.difficultyLevel.toDouble(), json.encodeToString(newEntry))
        println("🔄 Bot $botId rotated: new name=$newUsername")
    }

    suspend fun cleanupInactiveBots(daysInactive: Int) {
        val inactiveBots = botProfileRepository.findInactiveBots(daysInactive)
        for (bot in inactiveBots) {
            if (bot.isTutorial) continue
            botProfileRepository.deactivateBot(bot.botId.value)
            // حذف از Redis pool
            val poolKey = "${BOT_POOL_PREFIX}${bot.gameId}"
            val entry = BotPoolEntry(bot.botId, bot.gameId, bot.difficultyLevel, bot.rating)
            cache.zrem(poolKey, json.encodeToString(entry))
            cache.delete("${BOT_PROFILE_PREFIX}${bot.botId.value}")
            println("🗑️ Inactive bot ${bot.botId.value} removed")
        }
    }

    private suspend fun getBotProfile(botId: PlayerId): BotProfile? {
        val cached = cache.get("${BOT_PROFILE_PREFIX}${botId.value}")
        if (cached != null) return json.decodeFromString(cached)
        return botProfileRepository.findById(botId.value)
    }

    private fun calculateInitialRating(difficulty: Int): SkillRating {
        val mean = when (difficulty) {
            1 -> 800
            2 -> 900
            3 -> 1000
            4 -> 1100
            5 -> 1200
            6 -> 1300
            7 -> 1400
            8 -> 1500
            9 -> 1600
            else -> 1700
        }.toDouble()
        return SkillRating(mean, 200.0)
    }
}