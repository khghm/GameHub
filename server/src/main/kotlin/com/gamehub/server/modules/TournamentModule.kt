package com.gamehub.server.modules

import com.gamehub.server.economy.EconomyService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class Tournament(
    val id: String,
    val gameId: String,
    val name: String,
    val format: String,
    val maxParticipants: Int,
    var currentParticipants: Int = 0,
    val entryFeeCoins: Long = 0,
    var prizePoolCoins: Long = 0,
    val platformFeePercent: Int = 10,
    val prizeDistribution: Map<String, Double>,
    val allowedBehaviorBands: List<String>,
    val minLevel: Int = 0,
    val minElo: Int = 0,
    val registrationStart: Long,
    val registrationEnd: Long,
    val startTime: Long,
    var endTime: Long? = null,
    var status: String = "waiting",
    var bracketData: Bracket? = null,
    var winnerId: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class TournamentRegistration(
    val userId: String,
    val tournamentId: String,
    val registrationTime: Long = System.currentTimeMillis(),
    val status: String = "registered",
    val seed: Int? = null,
    val groupIndex: Int? = null,
    val placement: Int? = null,
    var coinsWon: Long = 0
)

@Serializable
data class Bracket(
    val rounds: List<Round>
)

@Serializable
data class Round(
    val matches: List<Match>
)

@Serializable
data class Match(
    val id: String,
    var playerA: String?,   // nullable و mutable برای به‌روزرسانی در مرحله حذفی
    var playerB: String?,   // nullable و mutable
    var winner: String? = null,
    var status: String = "pending",
    var gameSessionId: String? = null
)

@Serializable
data class RegistrationResult(
    val success: Boolean,
    val message: String,
    val isWaitlist: Boolean = false,
    val position: Int? = null
)

object TournamentModule {
    private val tournaments = mutableMapOf<String, Tournament>()
    private val registrations = mutableMapOf<String, MutableList<TournamentRegistration>>()
    private val waitlists = mutableMapOf<String, MutableList<String>>()
    private val tournamentSubscribers = mutableMapOf<String, MutableList<io.ktor.websocket.WebSocketSession>>()
    private val json = Json { ignoreUnknownKeys = true }

    // ========== Creation ==========
    suspend fun createTournament(
        gameId: String,
        name: String,
        format: String,
        maxParticipants: Int,
        entryFeeCoins: Long,
        prizeDistribution: Map<String, Double>,
        allowedBehaviorBands: List<String>,
        minLevel: Int,
        minElo: Int,
        registrationStart: Long,
        registrationEnd: Long,
        startTime: Long,
        createdBy: String
    ): Tournament {
        val id = UUID.randomUUID().toString().take(8)
        val prizePoolCoins = maxParticipants * entryFeeCoins * (100 - 10) / 100
        val tournament = Tournament(
            id = id,
            gameId = gameId,
            name = name,
            format = format,
            maxParticipants = maxParticipants,
            entryFeeCoins = entryFeeCoins,
            prizePoolCoins = prizePoolCoins,
            platformFeePercent = 10,
            prizeDistribution = prizeDistribution,
            allowedBehaviorBands = allowedBehaviorBands,
            minLevel = minLevel,
            minElo = minElo,
            registrationStart = registrationStart,
            registrationEnd = registrationEnd,
            startTime = startTime,
            createdBy = createdBy,
            status = "waiting"
        )
        tournaments[id] = tournament
        registrations[id] = mutableListOf()
        waitlists[id] = mutableListOf()
        return tournament
    }

    // ========== Registration ==========
    suspend fun registerUser(
        tournamentId: String,
        userId: String,
        userLevel: Int,
        userElo: Int,
        userBehaviorBand: String,
        economyService: EconomyService?
    ): RegistrationResult {
        val tournament = tournaments[tournamentId] ?: return RegistrationResult(false, "تورنمنت یافت نشد")
        val now = System.currentTimeMillis()
        if (now < tournament.registrationStart) return RegistrationResult(false, "ثبت‌نام هنوز شروع نشده است")
        if (now > tournament.registrationEnd) return RegistrationResult(false, "زمان ثبت‌نام به پایان رسیده است")
        if (tournament.status != "waiting" && tournament.status != "registration") {
            return RegistrationResult(false, "تورنمنت در حال برگزاری است یا به پایان رسیده")
        }
        if (registrations[tournamentId]?.any { it.userId == userId } == true) {
            return RegistrationResult(false, "شما قبلاً در این تورنمنت ثبت‌نام کرده‌اید")
        }
        if (userLevel < tournament.minLevel) return RegistrationResult(false, "سطح شما کافی نیست")
        if (userElo < tournament.minElo) return RegistrationResult(false, "رنک شما کافی نیست")
        if (tournament.allowedBehaviorBands.isNotEmpty() && !tournament.allowedBehaviorBands.contains(userBehaviorBand)) {
            return RegistrationResult(false, "باند رفتاری شما مجاز نیست")
        }
        if (economyService != null && tournament.entryFeeCoins > 0) {
            try {
                economyService.deductCoins(userId, tournament.entryFeeCoins, "tournament_registration", null)
            } catch (e: Exception) {
                return RegistrationResult(false, "موجودی سکه کافی نیست")
            }
        }
        if (tournament.currentParticipants < tournament.maxParticipants) {
            tournament.currentParticipants++
            val registration = TournamentRegistration(userId = userId, tournamentId = tournamentId, status = "registered")
            registrations[tournamentId]?.add(registration)
            tournament.updatedAt = System.currentTimeMillis()
            if (tournament.currentParticipants >= tournament.maxParticipants) tournament.status = "registration"
            broadcastTournamentUpdate(tournamentId)
            return RegistrationResult(true, "ثبت‌نام با موفقیت انجام شد", false)
        } else {
            val waitlist = waitlists[tournamentId] ?: mutableListOf()
            if (!waitlist.contains(userId)) {
                waitlist.add(userId)
                waitlists[tournamentId] = waitlist
            }
            val position = waitlist.indexOf(userId) + 1
            return RegistrationResult(true, "ظرفیت پر است. در صف انتظار قرار گرفتید", true, position)
        }
    }

    suspend fun cancelRegistration(tournamentId: String, userId: String, economyService: EconomyService?): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        val now = System.currentTimeMillis()
        if (now >= tournament.startTime) return false
        val regList = registrations[tournamentId] ?: return false
        val registration = regList.find { it.userId == userId } ?: return false
        regList.remove(registration)
        tournament.currentParticipants--
        if (economyService != null && tournament.entryFeeCoins > 0) {
            economyService.addCoins(userId, tournament.entryFeeCoins, "tournament_cancel", null)
        }
        val waitlist = waitlists[tournamentId] ?: mutableListOf()
        if (waitlist.isNotEmpty() && tournament.currentParticipants < tournament.maxParticipants) {
            val newUserId = waitlist.removeAt(0)
            val newRegistration = TournamentRegistration(userId = newUserId, tournamentId = tournamentId, status = "registered")
            regList.add(newRegistration)
            tournament.currentParticipants++
        }
        tournament.updatedAt = System.currentTimeMillis()
        broadcastTournamentUpdate(tournamentId)
        return true
    }

    fun getRegistrations(tournamentId: String): List<TournamentRegistration> = registrations[tournamentId] ?: emptyList()
    fun getWaitlist(tournamentId: String): List<String> = waitlists[tournamentId] ?: emptyList()
    fun isRegistered(tournamentId: String, userId: String): Boolean = registrations[tournamentId]?.any { it.userId == userId } == true

    // ========== Bracket Generation ==========
    suspend fun startTournament(tournamentId: String): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        if (tournament.currentParticipants < 2) return false
        val now = System.currentTimeMillis()
        if (now < tournament.startTime) return false
        val players = registrations[tournamentId]?.filter { it.status == "registered" }?.map { it.userId }?.shuffled() ?: return false
        if (players.size < 2) return false
        val bracket = when (tournament.format) {
            "single_elimination" -> generateSingleEliminationBracket(players)
            "group_stage" -> generateGroupStageBracket(players, tournament.gameId)
            else -> return false
        }
        tournament.bracketData = bracket
        tournament.status = "in_progress"
        tournament.updatedAt = System.currentTimeMillis()
        broadcastTournamentUpdate(tournamentId)
        return true
    }

    private fun generateSingleEliminationBracket(players: List<String>): Bracket {
        val n = players.size
        val nextPowerOfTwo = generateNextPowerOfTwo(n)
        val byes = nextPowerOfTwo - n
        val orderedPlayers = players.toMutableList()
        val firstRoundPlayers = orderedPlayers.toMutableList()
        val byePlayers = if (byes > 0) {
            firstRoundPlayers.takeLast(byes).toMutableList()
        } else {
            mutableListOf()
        }
        if (byes > 0) {
            repeat(byes) { firstRoundPlayers.removeAt(firstRoundPlayers.size - 1) }
        }
        val firstRoundMatches = mutableListOf<Match>()
        for (i in 0 until firstRoundPlayers.size step 2) {
            val p1 = firstRoundPlayers[i]
            val p2 = if (i + 1 < firstRoundPlayers.size) firstRoundPlayers[i + 1] else null
            firstRoundMatches.add(Match(id = UUID.randomUUID().toString(), playerA = p1, playerB = p2, status = "pending"))
        }
        val rounds = mutableListOf<Round>()
        if (firstRoundMatches.isNotEmpty()) rounds.add(Round(firstRoundMatches))
        var roundMatches = firstRoundMatches
        var nextRoundPlayers = byePlayers.toMutableList()
        while (nextRoundPlayers.size + roundMatches.size > 1) {
            val nextMatches = mutableListOf<Match>()
            val currentRoundWinners = mutableListOf<String?>()
            roundMatches.forEach { match ->
                currentRoundWinners.add(match.winner)
            }
            val allPlayersForNextRound = (currentRoundWinners + nextRoundPlayers).filterNotNull().toMutableList()
            for (i in 0 until allPlayersForNextRound.size step 2) {
                val p1 = allPlayersForNextRound[i]
                val p2 = if (i + 1 < allPlayersForNextRound.size) allPlayersForNextRound[i + 1] else null
                nextMatches.add(Match(id = UUID.randomUUID().toString(), playerA = p1, playerB = p2, status = "pending"))
            }
            if (nextMatches.isNotEmpty()) rounds.add(Round(nextMatches))
            roundMatches = nextMatches
            nextRoundPlayers = mutableListOf()
        }
        return Bracket(rounds)
    }

    private fun generateGroupStageBracket(players: List<String>, gameId: String): Bracket {
        val n = players.size
        val groupSize = 4
        val numGroups = maxOf(2, (n + groupSize - 1) / groupSize)
        val groups = mutableListOf<MutableList<String>>()
        val shuffled = players.shuffled()
        for (i in 0 until numGroups) groups.add(mutableListOf())
        for (i in shuffled.indices) groups[i % numGroups].add(shuffled[i])
        val groupMatches = mutableListOf<Match>()
        groups.forEachIndexed { groupIdx, groupPlayers ->
            for (i in groupPlayers.indices) {
                for (j in i + 1 until groupPlayers.size) {
                    groupMatches.add(Match(id = UUID.randomUUID().toString(), playerA = groupPlayers[i], playerB = groupPlayers[j], status = "pending"))
                }
            }
        }
        val knockoutPlayers = List(numGroups * 2) { "TBD_${it}" }
        val knockoutBracket = generateSingleEliminationBracket(knockoutPlayers)
        val allRounds = mutableListOf<Round>()
        if (groupMatches.isNotEmpty()) allRounds.add(Round(groupMatches))
        allRounds.addAll(knockoutBracket.rounds)
        return Bracket(allRounds)
    }

    private fun generateNextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) power *= 2
        return power
    }

    fun getNextPendingMatch(tournamentId: String): Match? {
        val tournament = tournaments[tournamentId] ?: return null
        val bracket = tournament.bracketData ?: return null
        for (round in bracket.rounds) {
            for (match in round.matches) {
                if (match.status == "pending" && match.playerA != null && match.playerB != null) {
                    return match
                }
            }
        }
        return null
    }

    suspend fun reportMatchResult(tournamentId: String, matchId: String, winnerId: String): Boolean {
        val tournament = tournaments[tournamentId] ?: return false
        val bracket = tournament.bracketData ?: return false
        var targetMatch: Match? = null
        var targetRoundIndex = -1
        var targetMatchIndex = -1
        for (i in bracket.rounds.indices) {
            for (j in bracket.rounds[i].matches.indices) {
                if (bracket.rounds[i].matches[j].id == matchId) {
                    targetMatch = bracket.rounds[i].matches[j]
                    targetRoundIndex = i
                    targetMatchIndex = j
                    break
                }
            }
        }
        if (targetMatch == null) return false
        if (targetMatch.status != "in_progress" && targetMatch.status != "pending") return false
        if (targetMatch.playerA != winnerId && targetMatch.playerB != winnerId) return false

        // ثبت برنده
        targetMatch.winner = winnerId
        targetMatch.status = "completed"

        // حذف GameSession (اختیاری) – بعداً می‌توان از طریق GameSessionManager حذف کرد
        if (targetMatch.gameSessionId != null) {
            GameSessionManager.removeSession(targetMatch.gameSessionId!!)
        }

        // به‌روزرسانی مسابقه بعدی در براکت
        if (targetRoundIndex + 1 < bracket.rounds.size) {
            val nextRound = bracket.rounds[targetRoundIndex + 1]
            val nextMatchIndex = targetMatchIndex / 2
            if (nextMatchIndex < nextRound.matches.size) {
                val nextMatch = nextRound.matches[nextMatchIndex]
                if (nextMatchIndex % 2 == 0) {
                    nextMatch.playerA = winnerId
                } else {
                    nextMatch.playerB = winnerId
                }
                // اگر هر دو بازیکن مشخص شدند، وضعیت مسابقه را به pending تغییر دهیم
                if (nextMatch.playerA != null && nextMatch.playerB != null && nextMatch.status == "pending") {
                    // آماده برای شروع
                }
            }
        } else {
            // فینال – برنده نهایی
            tournament.winnerId = winnerId
            tournament.status = "completed"
            tournament.endTime = System.currentTimeMillis()
            // توزیع جوایز (اقتصاد سرویس باید از بیرون پاس داده شود)
            // فعلاً فقط لاگ می‌کنیم – در اندپوینت مجزا بعداً توزیع می‌کنیم
            println("🏆 Tournament $tournamentId completed. Winner: $winnerId")
        }

        tournament.updatedAt = System.currentTimeMillis()
        broadcastTournamentUpdate(tournamentId)
        return true
    }

    fun getTournament(id: String): Tournament? = tournaments[id]
    fun getAllTournaments(): List<Tournament> = tournaments.values.toList()

    fun addSubscriber(tournamentId: String, session: io.ktor.websocket.WebSocketSession) {
        tournamentSubscribers.getOrPut(tournamentId) { mutableListOf() }.add(session)
    }

    fun removeSubscriber(tournamentId: String, session: io.ktor.websocket.WebSocketSession) {
        tournamentSubscribers[tournamentId]?.remove(session)
    }

    suspend fun broadcastTournamentUpdate(tournamentId: String) {
        val tournament = tournaments[tournamentId] ?: return
        val state = json.encodeToString(tournament)
        tournamentSubscribers[tournamentId]?.forEach { ws ->
            try { ws.send(io.ktor.websocket.Frame.Text(state)) } catch (_: Exception) {}
        }
    }
    // ========== Serialization Helpers ==========
    fun serializeTournament(tournament: Tournament): String = json.encodeToString(tournament)
    fun serializeRegistrationResult(result: RegistrationResult): String = json.encodeToString(result)
    fun serializeTournamentList(tournaments: List<Tournament>): String = json.encodeToString(tournaments)
    // ========== Match Session Management ==========

    /**
     * شروع یک مسابقه و ایجاد GameSession
     * @return gameSessionId در صورت موفقیت، null در غیر این صورت
     */
    suspend fun startMatch(tournamentId: String, matchId: String): String? {
        val tournament = tournaments[tournamentId] ?: return null
        val bracket = tournament.bracketData ?: return null

        // پیدا کردن مسابقه
        var targetMatch: Match? = null
        for (round in bracket.rounds) {
            for (match in round.matches) {
                if (match.id == matchId) {
                    targetMatch = match
                    break
                }
            }
        }
        if (targetMatch == null) return null
        if (targetMatch.status != "pending") return null
        if (targetMatch.playerA == null || targetMatch.playerB == null) return null

        // ایجاد GameSession
        val gameSessionId = GameSessionManager.createSession(
            gameType = tournament.gameId,
            playerIds = listOf(targetMatch.playerA!!, targetMatch.playerB!!),
            gameId = matchId  // استفاده از matchId به عنوان gameId سفارشی
        )

        // به‌روزرسانی اطلاعات مسابقه
        targetMatch.gameSessionId = gameSessionId
        targetMatch.status = "in_progress"
        tournament.updatedAt = System.currentTimeMillis()

        broadcastTournamentUpdate(tournamentId)
        return gameSessionId
    }

    /**
     * دریافت اطلاعات مسابقه برای یک بازیکن خاص
     * @return جفت (gameSessionId, gameType) یا null
     */
    fun getMatchForPlayer(tournamentId: String, userId: String): Pair<String, String>? {
        val tournament = tournaments[tournamentId] ?: return null
        val bracket = tournament.bracketData ?: return null
        for (round in bracket.rounds) {
            for (match in round.matches) {
                if (match.status == "in_progress" && (match.playerA == userId || match.playerB == userId)) {
                    return Pair(match.gameSessionId ?: return null, tournament.gameId)
                }
            }
        }
        return null
    }
    // ========== Prize Distribution ==========

    /**
     * توزیع جوایز بین برندگان تورنمنت
     * @param economyService سرویس اقتصاد برای اعطای سکه
     * @return لیست جوایز اعطا شده
     */
    suspend fun distributePrizes(tournamentId: String, economyService: EconomyService?): List<Pair<String, Long>> {
        val tournament = tournaments[tournamentId] ?: return emptyList()
        if (tournament.status != "completed") return emptyList()

        val prizeDistribution = tournament.prizeDistribution
        val prizePool = tournament.prizePoolCoins
        val registrationsList = registrations[tournamentId] ?: return emptyList()

        // پیدا کردن برندگان (بر اساس placement)
        // در تورنمنت‌های حذفی، placement از براکت نهایی مشخص می‌شود
        // برای سادگی، فرض می‌کنیم که winnerId (قهرمان) مشخص است و سایر رتبه‌ها از registrations استخراج می‌شوند
        val winners = mutableListOf<Pair<String, Int>>() // userId, rank (1-based)

        // قهرمان (مقام اول)
        if (tournament.winnerId != null) {
            winners.add(tournament.winnerId!! to 1)
        }

        // نفرات دوم و سوم (در صورت وجود)
        // در حال حاضر از registrations که placement دارند استفاده می‌کنیم
        val placedPlayers = registrationsList.filter { it.placement != null && it.placement in 2..3 }
            .sortedBy { it.placement }
        for (p in placedPlayers) {
            winners.add(p.userId to p.placement!!)
        }

        // محاسبه و اعطای جوایز
        val awarded = mutableListOf<Pair<String, Long>>()
        for ((userId, rank) in winners) {
            val rankKey = rank.toString()
            val percentage = prizeDistribution[rankKey]
            if (percentage != null) {
                val prizeAmount = (prizePool * percentage).toLong()
                if (prizeAmount > 0 && economyService != null) {
                    try {
                        economyService.addCoins(userId, prizeAmount, "tournament_prize", null)
                        awarded.add(userId to prizeAmount)

                        // به‌روزرسانی coinsWon در registration
                        val registration = registrationsList.find { it.userId == userId }
                        if (registration != null) {
                            registration.coinsWon = prizeAmount
                        }
                    } catch (e: Exception) {
                        println("❌ Failed to award prize to $userId: ${e.message}")
                    }
                }
            }
        }

        // اگر توزیع انجام شد، وضعیت تورنمنت را به `prizes_distributed` تغییر بدهیم تا دوباره اجرا نشود
        if (awarded.isNotEmpty()) {
            tournament.status = "prizes_distributed"
            tournament.updatedAt = System.currentTimeMillis()
            broadcastTournamentUpdate(tournamentId)
        }

        return awarded
    }
}