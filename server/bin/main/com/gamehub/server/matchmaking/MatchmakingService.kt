// server/src/main/kotlin/com/gamehub/server/matchmaking/MatchmakingService.kt
package com.gamehub.server.matchmaking

import com.gamehub.server.appMicrometerRegistry
import com.gamehub.server.anticheat.ShadowPoolManager
import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.bot.CentralBotManager
import com.gamehub.server.modules.GameSessionManager
import com.gamehub.shared.bot.BotProfile
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.core.BehaviorBand
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.QueueEntry
import com.gamehub.shared.matchmaking.SkillRating
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// Custom Matchmaking Metrics (using the global registry from Application.kt)
// Rename global caches with prefix for clarity and use cache provider for persistence
private const val CACHE_KEY_QUEUE_SIZE = "mm:cache:queue-size:"
private const val CACHE_KEY_REGISTERED_GAUGES = "mm:cache:registered-gauges"
private const val CACHE_KEY_MATCH_QUALITY = "mm:cache:match-quality:"
private const val CACHE_KEY_LAST_ACTIVITY = "mm:cache:last-activity:"
private const val CACHE_KEY_ACTIVE_GAME_MODES = "mm:cache:active-game-modes"

// Global metrics counters/timers
private val matchFoundCounter: Counter by lazy {
    Counter.builder("matchmaking.matches.found")
        .description("Total matches found successfully")
        .register(appMicrometerRegistry)
}

private val matchmakingWaitTimer: Timer by lazy {
    Timer.builder("matchmaking.wait.time")
        .description("Time players spent waiting in queue")
        .publishPercentiles(0.5, 0.75, 0.95, 0.99)
        .register(appMicrometerRegistry)
}

private val matchQualityGaugeMap: MutableMap<String, Gauge> = ConcurrentHashMap()

class MatchmakingService(
    private val cache: CacheProvider,
    private val behaviorService: BehaviorService,
    private val centralBotManager: CentralBotManager? = null,
    private val shadowPoolManager: ShadowPoolManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val bandCache = ConcurrentHashMap<String, String>() // cache for which band a user/party was in
    // Local in-memory cache for metrics (synced with cache provider)
    private val queueSizeCache = ConcurrentHashMap<String, Int>()
    private val registeredQueueGauges = ConcurrentHashMap<String, Boolean>()
    private val matchQualityCache = ConcurrentHashMap<String, Double>()
    private val lastActivityCache = ConcurrentHashMap<String, Long>()
    private val activeGameModes = ConcurrentHashMap.newKeySet<String>()

    companion object {
        private const val QUEUE_PREFIX = "mm:queue:"
        private const val META_PREFIX = "mm:meta:"
        private const val BASE_DELTA = 50.0
        private const val MAX_DELTA_MULTIPLIER = 5.0 // حداکثر ۵ برابر BASE_DELTA
        private const val K = 2.0 // Reduced from 200 to make delta increase slower
        private const val LAMBDA = 1.5
        private const val T_AGGRESSIVE = 6.0
        private const val QUEUE_TTL_SECONDS = 3600L // 1 ساعت
        private const val BANDS = "ABCDE"
        private const val SCORE_REFRESH_INTERVAL_MS = 5000L // 5 ثانیه
        private const val BOT_FALLBACK_WAIT_TIME_SECONDS = 30L // Wait 30s before using bot
    }

    init {
        // Load cached state from cache provider on init in a coroutine
        coroutineScope.launch {
            loadCachedState()
        }
        startScoreRefreshWorker()
    }

    private fun queueKey(gameId: String, mode: String, band: String) = "${QUEUE_PREFIX}$gameId:$mode:band$band"
    private fun metaKey(userId: String) = "${META_PREFIX}$userId"

    private suspend fun loadCachedState() {
        // Load active game modes
        val activeModesJson = cache.get(CACHE_KEY_ACTIVE_GAME_MODES)
        if (activeModesJson != null) {
            try {
                val modes = json.decodeFromString<List<String>>(activeModesJson)
                modes.forEach { activeGameModes.add(it) }
            } catch (e: Exception) {
                // Ignore invalid data
            }
        }

        // Load registered gauges
        val registeredGaugesJson = cache.get(CACHE_KEY_REGISTERED_GAUGES)
        if (registeredGaugesJson != null) {
            try {
                val gauges = json.decodeFromString<List<String>>(registeredGaugesJson)
                gauges.forEach { registeredQueueGauges[it] = true }
            } catch (e: Exception) {
                // Ignore invalid data
            }
        }
    }

    private suspend fun saveCachedState() {
        // Save active game modes
        cache.set(CACHE_KEY_ACTIVE_GAME_MODES, json.encodeToString(activeGameModes.toList()), QUEUE_TTL_SECONDS)
        // Save registered gauges
        cache.set(CACHE_KEY_REGISTERED_GAUGES, json.encodeToString(registeredQueueGauges.toList()), QUEUE_TTL_SECONDS)
    }

    private fun startScoreRefreshWorker() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    refreshAllQueueScores()
                    cleanupStaleMetrics() // اضافه کردن پاک‌سازی متریک‌های قدیمی
                    processActiveQueues() // پردازش صف‌های فعال برای مچ‌سازی (push-based)
                } catch (e: Exception) {
                    println("⚠️ Error in worker: ${e.message}")
                }
                delay(SCORE_REFRESH_INTERVAL_MS)
            }
        }
    }

    private suspend fun processActiveQueues() {
        val gameModesToRemove = mutableListOf<String>()

        for (gameModeKey in activeGameModes) {
            val (gameId, mode) = gameModeKey.split(":", limit = 2)
            val currentQueueSize = queueSize(gameId, mode)

            if (currentQueueSize == 0) {
                gameModesToRemove.add(gameModeKey)
                continue
            }

            // Get game config to know required players (default to 2 if not available)
            val requiredPlayers = getRequiredPlayersForGame(gameId)

            // ابتدا همه بازیکن‌های موجود در صف را می‌گیریم
            val allCandidates = mutableListOf<QueueEntry>()
            for (band in listOf("A", "B", "C", "D", "E")) {
                val key = queueKey(gameId, mode, band)
                val candidatesJson = cache.zrangebyscore(key, Double.MIN_VALUE, Double.MAX_VALUE, 100)
                for (candJson in candidatesJson) {
                    try {
                        val candidate = json.decodeFromString<QueueEntry>(candJson)
                        allCandidates.add(candidate)
                    } catch (e: Exception) {
                        // skip invalid
                    }
                }
            }

            // Take lock first to avoid race conditions
            val lock = locks.getOrPut("$gameId:$mode") { Mutex() }
            lock.withLock {
                // Refetch candidates after lock to ensure we have latest state
                val freshCandidates = mutableListOf<QueueEntry>()
                for (band in listOf("A", "B", "C", "D", "E")) {
                    val key = queueKey(gameId, mode, band)
                    val candidatesJson = cache.zrangebyscore(key, Double.MIN_VALUE, Double.MAX_VALUE, 100)
                    for (candJson in candidatesJson) {
                        try {
                            val candidate = json.decodeFromString<QueueEntry>(candJson)
                            // Double-check they are still in queue
                            if (cache.get(metaKey(candidate.userId)) != null) {
                                freshCandidates.add(candidate)
                            }
                        } catch (e: Exception) {
                            // skip invalid
                        }
                    }
                }

                // Process 2-player games
                if (requiredPlayers == 2) {
                    processTwoPlayerGame(gameId, mode, freshCandidates)
                } else {
                    // Process multiplayer games (3/4 players)
                    processMultiplayerGame(gameId, mode, freshCandidates, requiredPlayers)
                }
            }
        }

        // حذف گیم-مدهایی که دیگه بازیکن در صف ندارند
        for (gameModeKey in gameModesToRemove) {
            activeGameModes.remove(gameModeKey)
        }
        saveCachedState()
    }

    private suspend fun processTwoPlayerGame(gameId: String, mode: String, candidates: List<QueueEntry>) {
        val sortedCandidates = candidates.sortedBy { it.joinedAt }
        val matchedPairs = mutableListOf<Pair<QueueEntry, QueueEntry>>()
        val usedUserIds = mutableSetOf<String>()

        for (i in sortedCandidates.indices) {
            val candidate1 = sortedCandidates[i]
            if (usedUserIds.contains(candidate1.userId)) continue

            var bestMatch: QueueEntry? = null
            var bestQuality = 0.0

            for (j in i + 1 until sortedCandidates.size) {
                val candidate2 = sortedCandidates[j]
                if (usedUserIds.contains(candidate2.userId)) continue

                // فیلتر شادو پول
                val c1InShadow = shadowPoolManager?.isInShadowPool(candidate1.userId) == true
                val c2InShadow = shadowPoolManager?.isInShadowPool(candidate2.userId) == true
                if (c1InShadow != c2InShadow) continue

                // فیلتر اندازه گروه (فرد با فرد، گروه با گروه هم‌اندازه)
                if (candidate1.partySize != candidate2.partySize) continue

                val quality = calculateMatchQuality(candidate1, candidate2)
                if (quality > bestQuality) {
                    bestQuality = quality
                    bestMatch = candidate2
                }
            }

            if (bestMatch != null) {
                matchedPairs.add(candidate1 to bestMatch)
                usedUserIds.add(candidate1.userId)
                usedUserIds.add(bestMatch.userId)
            }
        }

        // Register matches
        for ((c1, c2) in matchedPairs) {
            createMatch(gameId, listOf(c1, c2))
        }

        // Handle bot fallback for players waiting too long
        val now = System.currentTimeMillis()
        for (candidate in sortedCandidates) {
            if (usedUserIds.contains(candidate.userId)) continue
            val waitTimeSeconds = (now - candidate.joinedAt) / 1000.0
            if (waitTimeSeconds >= BOT_FALLBACK_WAIT_TIME_SECONDS && candidate.partySize == 1) {
                tryBotFallback(gameId, mode, candidate)
            }
        }
    }

    private suspend fun processMultiplayerGame(gameId: String, mode: String, candidates: List<QueueEntry>, requiredPlayers: Int) {
        val sortedCandidates = candidates.sortedBy { it.joinedAt }
        val usedUserIds = mutableSetOf<String>()

        for (i in sortedCandidates.indices) {
            val candidate = sortedCandidates[i]
            if (usedUserIds.contains(candidate.userId)) continue

            val neededPlayers = requiredPlayers - candidate.partySize
            if (neededPlayers <= 0) continue

            // Find compatible candidates
            val compatibleCandidates = sortedCandidates
                .filter { !usedUserIds.contains(it.userId) && it.userId != candidate.userId }
                .filter {
                    val c1InShadow = shadowPoolManager?.isInShadowPool(candidate.userId) == true
                    val c2InShadow = shadowPoolManager?.isInShadowPool(it.userId) == true
                    c1InShadow == c2InShadow
                }
                .sortedBy { abs(it.skillRating.mean - candidate.skillRating.mean) }

            // Try to find a combination
            val selected = mutableListOf(candidate)
            var totalPlayers = candidate.partySize

            for (compatible in compatibleCandidates) {
                if (totalPlayers + compatible.partySize <= requiredPlayers) {
                    selected.add(compatible)
                    totalPlayers += compatible.partySize
                }
                if (totalPlayers == requiredPlayers) break
            }

            if (totalPlayers == requiredPlayers) {
                createMatch(gameId, selected)
                selected.forEach { usedUserIds.add(it.userId) }
            }
        }
    }

    private suspend fun createMatch(gameId: String, entries: List<QueueEntry>) {
        val quality = entries.windowed(2).map { calculateMatchQuality(it[0], it[1]) }.average()
        val playerIds = entries.flatMap {
            if (it.partyId != null) {
                // For party, we would need to get members, but for now use userId as placeholder
                listOf(it.userId)
            } else {
                listOf(it.userId)
            }
        }

        // First, verify all are still in queue and collect their current state
        val entriesStillInQueue = mutableListOf<QueueEntry>()
        for (entry in entries) {
            val meta = cache.get(metaKey(entry.userId))
            if (meta == null) {
                println("⚠️ User ${entry.userId} left queue, aborting match")
                return
            }
            entriesStillInQueue.add(entry)
        }

        // Now remove them from queue
        for (entry in entriesStillInQueue) {
            dequeue(entry.userId, gameId, entry.mode)
        }

        try {
            // Create match
            val gameIdForMatch = UUID.randomUUID().toString()
            assignMatch(gameIdForMatch, *playerIds.toTypedArray())
            GameSessionManager.createSession(gameId, playerIds, gameIdForMatch)

            // ثبت متریک
            matchFoundCounter.increment()
            val now = System.currentTimeMillis()
            entries.forEach { entry ->
                val waitTime = now - entry.joinedAt
                matchmakingWaitTimer.record(waitTime, TimeUnit.MILLISECONDS)
            }

            val qualityKey = "quality:$gameId:${entries.first().mode}"
            matchQualityCache[qualityKey] = quality
            if (!matchQualityGaugeMap.containsKey(qualityKey)) {
                matchQualityGaugeMap[qualityKey] = Gauge.builder("matchmaking.match.quality") { matchQualityCache[qualityKey] ?: 0.0 }
                    .tag("game", gameId)
                    .tag("mode", entries.first().mode)
                    .description("Quality score of the last match")
                    .register(appMicrometerRegistry)
            }

            println("🎯 Match created for $gameId:${entries.first().mode}: $playerIds")
        } catch (e: Exception) {
            // ROLLBACK: Re-enqueue all players!
            println("❌ Failed to create match, rolling back: ${e.message}")
            for (entry in entriesStillInQueue) {
                val effectiveBandName = behaviorService.getEffectiveBand(entry.userId)
                val key = queueKey(gameId, entry.mode, effectiveBandName)
                val entryJson = json.encodeToString(entry)
                cache.zadd(key, entry.skillRating.queueScore(0.0), entryJson, QUEUE_TTL_SECONDS)
                cache.set(metaKey(entry.userId), entryJson, QUEUE_TTL_SECONDS)
                bandCache[metaKey(entry.userId)] = effectiveBandName
                val gameModeKey = "$gameId:${entry.mode}"
                activeGameModes.add(gameModeKey)
            }
        }
    }

    private suspend fun tryBotFallback(gameId: String, mode: String, playerEntry: QueueEntry) {
        val targetDifficulty = calculateBotDifficulty(playerEntry.skillRating.mean)
        val bot = centralBotManager?.assignBot(gameId, targetDifficulty, setOf(PlayerId(playerEntry.userId)))
        if (bot != null) {
            println("🤖 Bot fallback used for ${playerEntry.userId}")
            // Create match with bot
            dequeue(playerEntry.userId, gameId, mode)
            val gameIdForMatch = UUID.randomUUID().toString()
            assignMatch(gameIdForMatch, playerEntry.userId, "bot:${bot.botId.value}")
            GameSessionManager.createSession(gameId, listOf(playerEntry.userId, "bot:${bot.botId.value}"), gameIdForMatch)

            matchFoundCounter.increment()
            val waitTime = System.currentTimeMillis() - playerEntry.joinedAt
            matchmakingWaitTimer.record(waitTime, TimeUnit.MILLISECONDS)
        }
    }

    private fun calculateBotDifficulty(playerMean: Double): Int {
        // Convert player mean (1200-2000) to difficulty 1-10
        return ((playerMean - 1200) / 80).coerceIn(1.0, 10.0).toInt()
    }

    private fun getRequiredPlayersForGame(gameId: String): Int {
        // Default to 2 players, can be extended with game config
        return when (gameId) {
            "tictactoe", "chess", "backgammon", "checkers", "hex" -> 2
            "ludo", "monopoly", "uno", "blokus" -> 4
            "connectfour" -> 2
            else -> 2
        }
    }

    private suspend fun refreshAllQueueScores() {
        // Use activeGameModes to know which games/modes to refresh
        val modes = listOf("casual", "ranked")

        for (gameModeKey in activeGameModes) {
            val (gameId, mode) = gameModeKey.split(":", limit = 2)
            for (band in listOf("A", "B", "C", "D", "E")) {
                refreshQueueScores(gameId, mode, band)
            }
        }
    }

    private suspend fun refreshQueueScores(gameId: String, mode: String, band: String) {
        val key = queueKey(gameId, mode, band)
        val candidates = cache.zrangebyscore(key, Double.MIN_VALUE, Double.MAX_VALUE, 1000)
        val now = System.currentTimeMillis()

        for (candJson in candidates) {
            try {
                val entry = json.decodeFromString<QueueEntry>(candJson)
                val waitTimeSeconds = (now - entry.joinedAt) / 1000.0
                val newScore = entry.skillRating.queueScore(waitTimeSeconds)
                val oldScore = entry.skillRating.queueScore(0.0)

                if (newScore != oldScore) {
                    // Update the score in the zset by removing and re-adding
                    cache.zrem(key, candJson)
                    cache.zadd(key, newScore, candJson, QUEUE_TTL_SECONDS)
                }
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
    }

    // ========== Solo Queue ==========
    suspend fun enqueueSolo(userId: String, gameId: String, mode: String, skill: SkillRating, region: String = "IR") {
        // پاک کردن ورودی قدیمی در صورت وجود
        dequeue(userId, gameId, mode)

        val effectiveBandName = behaviorService.getEffectiveBand(userId)
        val behavior = behaviorService.getBehavior(userId)
        val entry = QueueEntry(userId, gameId, mode, skill, BehaviorBand.valueOf(effectiveBandName), System.currentTimeMillis(), region = region)
        val key = queueKey(gameId, mode, effectiveBandName)
        val entryJson = json.encodeToString(entry)
        val waitTimeSeconds = 0.0 // تازه وارد صف شده
        cache.zadd(key, entry.skillRating.queueScore(waitTimeSeconds), entryJson, QUEUE_TTL_SECONDS)
        cache.set(metaKey(userId), entryJson, QUEUE_TTL_SECONDS)
        bandCache[metaKey(userId)] = effectiveBandName

        val gameModeKey = "$gameId:$mode"
        activeGameModes.add(gameModeKey) // اضافه کردن به لیست گیم-مدهای فعال

        // Update queue size metrics
        updateQueueSizeCache(gameId, mode)

        println("📥 [$gameId:$mode] $userId added to queue band ${behavior.band}")
    }

    suspend fun dequeue(userId: String, gameId: String, mode: String) {
        val cachedBand = bandCache.remove(metaKey(userId))
        val entryJson = cache.get(metaKey(userId)) ?: return
        val entry = json.decodeFromString<QueueEntry>(entryJson)
        val bandToUse = cachedBand ?: entry.band.name

        val key = queueKey(gameId, mode, bandToUse)
        cache.zrem(key, entryJson)
        cache.delete(metaKey(userId))

        // Update queue size metric after dequeue
        updateQueueSizeCache(gameId, mode)
    }

    suspend fun dequeueParty(partyId: String, gameId: String, mode: String, members: List<String>) {
        val cachedBand = bandCache.remove(metaKey(partyId))
        val entryJson = cache.get(metaKey(partyId)) ?: return
        val entry = json.decodeFromString<QueueEntry>(entryJson)
        val bandToUse = cachedBand ?: entry.band.name

        val key = queueKey(gameId, mode, bandToUse)
        cache.zrem(key, entryJson)
        cache.delete(metaKey(partyId))
        for (member in members) {
            cache.delete(metaKey(member))
            bandCache.remove(metaKey(member))
        }

        // Update queue size metric after dequeue
        updateQueueSizeCache(gameId, mode)
    }

    private suspend fun updateQueueSizeCache(gameId: String, mode: String) {
        val gaugeKey = "$gameId:$mode"
        val currentSize = queueSize(gameId, mode)
        queueSizeCache[gaugeKey] = currentSize
        lastActivityCache[gaugeKey] = System.currentTimeMillis() // به‌روزرسانی زمان آخرین فعالیت

        // Register gauge only once
        if (!registeredQueueGauges.containsKey(gaugeKey)) {
            Gauge.builder("matchmaking.queue.size") { queueSizeCache[gaugeKey] ?: 0 }
                .tag("game", gameId)
                .tag("mode", mode)
                .description("Number of players in queue")
                .register(appMicrometerRegistry)
            registeredQueueGauges[gaugeKey] = true
        }
    }

    private suspend fun cleanupStaleMetrics() {
        val now = System.currentTimeMillis()
        val maxInactivityMs = TimeUnit.HOURS.toMillis(1) // حذفmetricهایی که ۱ ساعت فعالیت نداشتند
        val keysToRemove = mutableListOf<String>()

        for ((key, lastActivity) in lastActivityCache) {
            if (now - lastActivity > maxInactivityMs) {
                keysToRemove.add(key)
            }
        }

        for (key in keysToRemove) {
            queueSizeCache.remove(key)
            registeredQueueGauges.remove(key)
            matchQualityCache.remove(key)
            lastActivityCache.remove(key)
            // Note: We can't unregister Micrometer gauges easily, but the cache is cleared!
        }
    }

    data class MatchCandidate(
        val entry: QueueEntry,
        val qualityScore: Double
    )

    private fun calculateMatchQuality(myEntry: QueueEntry, candidate: QueueEntry): Double {
        val skillDiff = abs(myEntry.skillRating.mean - candidate.skillRating.mean)
        val behaviorPenalty = if (myEntry.band != candidate.band) 20.0 else 0.0
        val differentRegionPenalty = if (myEntry.region == candidate.region) 0.0 else 15.0 // جریمه برای منطقه متفاوت (برای آینده)

        // مجموع وزن‌دار (مقدار کمتر = مچ بهتر)
        val rawScore = (skillDiff * 0.5) + behaviorPenalty + differentRegionPenalty

        // تبدیل به مقیاس ۰-۱۰۰ (مقدار بیشتر = مچ بهتر)
        return 100.0 - min(rawScore, 100.0)
    }

    suspend fun tryMatchSolo(gameId: String, mode: String, userId: String): String? {
        val lock = locks.getOrPut("$gameId:$mode") { Mutex() }
        return lock.withLock {
            val metaJson = cache.get(metaKey(userId)) ?: return@withLock null
            val myEntry = json.decodeFromString<QueueEntry>(metaJson)
            val myBand = myEntry.band.name
            val myScore = myEntry.skillRating.queueScore()
            val now = System.currentTimeMillis()
            val waitTimeSeconds = (now - myEntry.joinedAt) / 1000.0
            val delta = calculateDelta(waitTimeSeconds, isRanked = mode == "ranked")
            val minScore = myScore - delta
            val maxScore = myScore + delta

            // بررسی Shadow Pool برای کاربر فعلی
            val isCurrentInShadow = shadowPoolManager?.isInShadowPool(userId) == true

            val allCandidates = mutableListOf<MatchCandidate>()

            // ۱. بررسی باند اصلی
            val key = queueKey(gameId, mode, myBand)
            val candidates = cache.zrangebyscore(key, minScore, maxScore, 50) // افزایش به ۵۰ برای انتخاب بهتر
            for (candJson in candidates) {
                if (candJson == metaJson) continue
                val candidate = json.decodeFromString<QueueEntry>(candJson)
                if (candidate.userId == userId) continue

                // فیلتر Shadow Pool (دوطرفه!)
                val isCandidateInShadow = shadowPoolManager?.isInShadowPool(candidate.userId) == true
                if (isCurrentInShadow != isCandidateInShadow) continue // اگر یکی در شادو و دیگری نباشد، ادامه نده!

                // Double-check that candidate is still in queue
                if (cache.get(metaKey(candidate.userId)) == null) continue

                val quality = calculateMatchQuality(myEntry, candidate)
                allCandidates.add(MatchCandidate(candidate, quality))
            }

            // ۲. بررسی باندهای مجاور اگر لازم باشد
            if (waitTimeSeconds >= 4.0) {
                val bands = listOf("A", "B", "C", "D", "E")
                val myIndex = bands.indexOf(myBand)
                val adjacent = mutableListOf<String>()
                if (myIndex > 0) adjacent.add(bands[myIndex - 1])
                if (myIndex < bands.size - 1) adjacent.add(bands[myIndex + 1])
                for (adjBand in adjacent) {
                    val adjKey = queueKey(gameId, mode, adjBand)
                    val adjCandidates = cache.zrangebyscore(adjKey, minScore, maxScore, 50)
                    for (candJson in adjCandidates) {
                        if (candJson == metaJson) continue
                        val candidate = json.decodeFromString<QueueEntry>(candJson)
                        if (candidate.userId == userId) continue

                        // فیلتر Shadow Pool
                        if (isCurrentInShadow && shadowPoolManager?.isInShadowPool(candidate.userId) != true) continue

                        // Double-check that candidate is still in queue
                        if (cache.get(metaKey(candidate.userId)) == null) continue

                        val quality = calculateMatchQuality(myEntry, candidate)
                        allCandidates.add(MatchCandidate(candidate, quality))
                    }
                }
            }

            // ۳. انتخاب بهترین کاندید بر اساس امتیاز کیفیت
            if (allCandidates.isNotEmpty()) {
                val bestCandidate = allCandidates.maxByOrNull { it.qualityScore }!!
                println("🎯 Best match found: quality=${bestCandidate.qualityScore}, user=${bestCandidate.entry.userId}")

                // Record metrics
                matchFoundCounter.increment()
                val waitTimeMs = System.currentTimeMillis() - myEntry.joinedAt
                matchmakingWaitTimer.record(waitTimeMs, TimeUnit.MILLISECONDS)

                // Update queue size metric
                val gaugeKey = "$gameId:$mode"
                queueSizeCache[gaugeKey] = queueSize(gameId, mode)

                // Update match quality metric
                val qualityKey = "quality:$gameId:$mode"
                matchQualityCache[qualityKey] = bestCandidate.qualityScore
                if (!matchQualityGaugeMap.containsKey(qualityKey)) {
                    matchQualityGaugeMap[qualityKey] = Gauge.builder("matchmaking.match.quality") { matchQualityCache[qualityKey] ?: 0.0 }
                        .tag("game", gameId)
                        .tag("mode", mode)
                        .description("Quality score of the last match")
                        .register(appMicrometerRegistry)
                }

                dequeue(userId, gameId, mode)
                dequeue(bestCandidate.entry.userId, gameId, mode)
                return bestCandidate.entry.userId
            }

            return null
        }
    }

    // ========== Party Queue ==========
    /**
     * محاسبه مهارت گروه با Weighted Average (برای مهارت گروه، از میانگین وزن‌دار اعضا استفاده می‌شود)
     * وزن‌ها: بالاترین مهارت در گروه ۵۰%، بقیه به طور مساوی ۵۰% باقی‌مانده
     */
    fun calculatePartySkill(memberSkills: List<SkillRating>): SkillRating {
        if (memberSkills.isEmpty()) return SkillRating(1200.0, 350.0, 0.06)

        // مرتب‌سازی اعضا بر اساس مهارت (بالاترین اول)
        val sortedSkills = memberSkills.sortedByDescending { it.mean }
        val highestSkill = sortedSkills.first()

        // محاسبه میانگین وزن‌دار
        var weightedMean = highestSkill.mean * 0.5 // ۵۰% وزن برای بهترین بازیکن
        var weightedStdDev = highestSkill.standardDeviation * 0.5
        var weightedVolatility = highestSkill.volatility * 0.5

        val remainingWeight = 0.5 / (sortedSkills.size - 1).coerceAtLeast(1)
        for (i in 1 until sortedSkills.size) {
            weightedMean += sortedSkills[i].mean * remainingWeight
            weightedStdDev += sortedSkills[i].standardDeviation * remainingWeight
            weightedVolatility += sortedSkills[i].volatility * remainingWeight
        }

        return SkillRating(
            mean = weightedMean,
            standardDeviation = weightedStdDev,
            volatility = weightedVolatility
        )
    }

    suspend fun enqueueParty(partyId: String, leaderId: String, members: List<String>, gameId: String, mode: String, partySkill: SkillRating, partyBand: String, region: String = "IR") {
        // پاک کردن ورودی قدیمی در صورت وجود
        dequeueParty(partyId, gameId, mode, members)

        val entry = QueueEntry(partyId, gameId, mode, partySkill, BehaviorBand.valueOf(partyBand), System.currentTimeMillis(), partyId, members.size, region)
        val entryJson = json.encodeToString(entry)
        val key = queueKey(gameId, mode, partyBand)
        cache.zadd(key, partySkill.queueScore(), entryJson, QUEUE_TTL_SECONDS)
        cache.set(metaKey(partyId), entryJson, QUEUE_TTL_SECONDS)
        bandCache[metaKey(partyId)] = partyBand
        for (member in members) {
            cache.set(metaKey(member), entryJson, QUEUE_TTL_SECONDS)
        }

        val gameModeKey = "$gameId:$mode"
        activeGameModes.add(gameModeKey) // اضافه کردن به لیست گیم-مدهای فعال
    }

    suspend fun tryMatchParty(gameId: String, mode: String, partyId: String): List<String>? {
        val lock = locks.getOrPut("$gameId:$mode") { Mutex() }
        return lock.withLock {
            // Get party from cache
            val partyData = cache.get("party:$partyId") ?: return@withLock null
            val party = try {
                json.decodeFromString<com.gamehub.server.modules.Party>(partyData)
            } catch (e: Exception) {
                null
            } ?: return@withLock null

            val metaJson = cache.get(metaKey(partyId)) ?: return@withLock null
            val myEntry = json.decodeFromString<QueueEntry>(metaJson)
            val myBand = myEntry.band.name
            val myScore = myEntry.skillRating.queueScore()
            val now = System.currentTimeMillis()
            val waitTimeSeconds = (now - myEntry.joinedAt) / 1000.0
            val delta = calculateDelta(waitTimeSeconds, isRanked = mode == "ranked")
            val minScore = myScore - delta
            val maxScore = myScore + delta

            val allCandidates = mutableListOf<MatchCandidate>()
            val isCurrentInShadow = shadowPoolManager?.isInShadowPool(partyId) == true

            // بررسی باند اصلی
            val key = queueKey(gameId, mode, myBand)
            val candidates = cache.zrangebyscore(key, minScore, maxScore, 50)
            for (candJson in candidates) {
                val candidate = json.decodeFromString<QueueEntry>(candJson)
                if (candidate.userId == partyId) continue
                if (candidate.partyId != null && candidate.partySize == myEntry.partySize) {
                    // فیلتر Shadow Pool
                    if (isCurrentInShadow && shadowPoolManager?.isInShadowPool(candidate.userId) != true) continue

                    // Double-check that candidate is still in queue
                    if (cache.get(metaKey(candidate.userId)) == null) continue

                    val quality = calculateMatchQuality(myEntry, candidate)
                    allCandidates.add(MatchCandidate(candidate, quality))
                }
            }

            // بررسی باندهای مجاور
            if (waitTimeSeconds >= 4.0) {
                val bands = listOf("A", "B", "C", "D", "E")
                val myIndex = bands.indexOf(myBand)
                val adjacent = mutableListOf<String>()
                if (myIndex > 0) adjacent.add(bands[myIndex - 1])
                if (myIndex < bands.size - 1) adjacent.add(bands[myIndex + 1])
                for (adjBand in adjacent) {
                    val adjKey = queueKey(gameId, mode, adjBand)
                    val adjCandidates = cache.zrangebyscore(adjKey, minScore, maxScore, 50)
                    for (candJson in adjCandidates) {
                        val candidate = json.decodeFromString<QueueEntry>(candJson)
                        if (candidate.userId == partyId) continue
                        if (candidate.partyId != null && candidate.partySize == myEntry.partySize) {
                            // فیلتر Shadow Pool
                            if (isCurrentInShadow && shadowPoolManager?.isInShadowPool(candidate.userId) != true) continue

                            // Double-check that candidate is still in queue
                            if (cache.get(metaKey(candidate.userId)) == null) continue

                            val quality = calculateMatchQuality(myEntry, candidate)
                            allCandidates.add(MatchCandidate(candidate, quality))
                        }
                    }
                }
            }

            // انتخاب بهترین کاندید
            if (allCandidates.isNotEmpty()) {
                val bestCandidate = allCandidates.maxByOrNull { it.qualityScore }!!
                println("🎯 Best party match found: quality=${bestCandidate.qualityScore}, party=${bestCandidate.entry.userId}")

                // Get opponent party from cache
                val opponentPartyData = cache.get("party:${bestCandidate.entry.userId}")
                val opponentParty = opponentPartyData?.let {
                    try {
                        json.decodeFromString<com.gamehub.server.modules.Party>(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                // Record metrics
                matchFoundCounter.increment()
                val waitTimeMs = System.currentTimeMillis() - myEntry.joinedAt
                matchmakingWaitTimer.record(waitTimeMs, TimeUnit.MILLISECONDS)

                // Update queue size metric
                val gaugeKey = "$gameId:$mode"
                queueSizeCache[gaugeKey] = queueSize(gameId, mode)

                // Update match quality metric
                val qualityKey = "quality:$gameId:$mode"
                matchQualityCache[qualityKey] = bestCandidate.qualityScore
                if (!matchQualityGaugeMap.containsKey(qualityKey)) {
                    matchQualityGaugeMap[qualityKey] = Gauge.builder("matchmaking.match.quality") { matchQualityCache[qualityKey] ?: 0.0 }
                        .tag("game", gameId)
                        .tag("mode", mode)
                        .description("Quality score of the last match")
                        .register(appMicrometerRegistry)
                }

                // Dequeue our party
                dequeueParty(partyId, gameId, mode, party.members.map { it.userId })
                // Dequeue opponent party
                if (opponentParty != null) {
                    dequeueParty(bestCandidate.entry.userId, gameId, mode, opponentParty.members.map { it.userId })
                } else {
                    // Fallback if opponent party not found
                    dequeue(bestCandidate.entry.userId, gameId, mode)
                }

                return listOf(partyId, bestCandidate.entry.userId)
            }

            return null
        }
    }

    // ========== Bot Fallback ==========
    suspend fun requestBot(gameId: String, targetDifficulty: Int, excludePlayerIds: Set<String>): BotProfile? {
        val excludeIds = excludePlayerIds.map { PlayerId(it) }.toSet()
        return centralBotManager?.assignBot(gameId, targetDifficulty, excludeIds)
    }

    private fun calculateDelta(waitSeconds: Double, isRanked: Boolean = false): Double {
        val base = if (isRanked) BASE_DELTA * 0.7 else BASE_DELTA // برای رنکد، دلتای کوچک‌تر (جست‌وجو دقیق‌تر)
        val max = if (isRanked) BASE_DELTA * 3 else BASE_DELTA * MAX_DELTA_MULTIPLIER // برای رنکد، حداکثر دلتا کمتر
        if (waitSeconds <= 0) return base
        val t = min(waitSeconds, T_AGGRESSIVE)
        val ratio = (exp(LAMBDA * t) - 1) / (exp(LAMBDA * T_AGGRESSIVE) - 1)
        val calculatedDelta = base * (1 + K * ratio)
        return min(calculatedDelta, max)
    }

    suspend fun queueSize(gameId: String, mode: String): Int {
        val bands = listOf("A", "B", "C", "D", "E")
        var total = 0
        for (band in bands) {
            val key = queueKey(gameId, mode, band)
            total += cache.zcard(key).toInt()
        }
        return total
    }

    suspend fun assignMatch(gameId: String, vararg playerIds: String) {
        for (playerId in playerIds) {
            cache.set("mm:assigned:$playerId", gameId, 120)
        }
    }

    suspend fun leaveSoloQueue(userId: String, gameId: String, mode: String) {
        dequeue(userId, gameId, mode)
        println("📤 [$gameId:$mode] $userId left the solo queue")
    }

    suspend fun leavePartyQueue(partyId: String, gameId: String, mode: String, members: List<String>) {
        dequeueParty(partyId, gameId, mode, members)
        println("📤 [$gameId:$mode] Party $partyId left the queue")
    }

    suspend fun getAssignedGame(userId: String): String? = cache.get("mm:assigned:$userId")
    suspend fun clearAssignedGame(userId: String) = cache.delete("mm:assigned:$userId")

    // ========== Multiplayer (3/4 Player) Matchmaking ==========
    data class MultiplayerMatchResult(
        val gameId: String,
        val playerIds: List<String>
    )

    suspend fun tryMatchMultiplayer(
        gameId: String,
        mode: String,
        userId: String,
        requiredPlayers: Int
    ): MultiplayerMatchResult? {
        val lock = locks.getOrPut("$gameId:$mode") { Mutex() }
        return lock.withLock {
            val metaJson = cache.get(metaKey(userId)) ?: return@withLock null
            val myEntry = json.decodeFromString<QueueEntry>(metaJson)
            val waitTimeSeconds = (System.currentTimeMillis() - myEntry.joinedAt) / 1000.0
            val delta = calculateDelta(waitTimeSeconds, isRanked = mode == "ranked")
            val isCurrentInShadow = shadowPoolManager?.isInShadowPool(userId) == true

            val allCandidates = mutableListOf<QueueEntry>()
            val bandsToCheck = getBandsToCheck(myEntry.band, waitTimeSeconds)

            for (band in bandsToCheck) {
                val key = queueKey(gameId, mode, band.name)
                val candidates = cache.zrangebyscore(
                    key,
                    myEntry.skillRating.mean - delta,
                    myEntry.skillRating.mean + delta,
                    100
                )
                    .mapNotNull { candJson ->
                        try {
                            json.decodeFromString<QueueEntry>(candJson)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .filter { it.userId != userId && cache.get(metaKey(it.userId)) != null }
                    .filter { candidate ->
                        // فیلتر Shadow Pool (دوطرفه!)
                        val isCandidateInShadow = shadowPoolManager?.isInShadowPool(candidate.userId) == true
                        isCurrentInShadow == isCandidateInShadow // فقط اگر هر دو در یک حالت باشند!
                    }
                allCandidates.addAll(candidates)
            }

            // یافتن بهترین ترکیب (requiredPlayers-1 بازیکن)
            if (allCandidates.size >= requiredPlayers - 1) {
                val bestCombination = findBestCombination(myEntry, allCandidates, requiredPlayers - 1)
                if (bestCombination.size == requiredPlayers - 1) {
                    // همه را از صف خارج کرده و مچ را ایجاد کنید
                    val allPlayerEntries = listOf(myEntry) + bestCombination
                    val allPlayerIds = allPlayerEntries.map { it.userId }
                    allPlayerIds.forEach { dequeue(it, gameId, mode) }
                    val matchGameId = UUID.randomUUID().toString()
                    assignMatch(matchGameId, *allPlayerIds.toTypedArray())

                    // Record metrics
                    matchFoundCounter.increment()
                    val waitTimeMs = System.currentTimeMillis() - myEntry.joinedAt
                    matchmakingWaitTimer.record(waitTimeMs, TimeUnit.MILLISECONDS)

                    // Update queue size metric
                    val gaugeKey = "$gameId:$mode"
                    queueSizeCache[gaugeKey] = queueSize(gameId, mode)

                    return@withLock MultiplayerMatchResult(matchGameId, allPlayerIds)
                }
            }

            null
        }
    }

    private fun getBandsToCheck(currentBand: BehaviorBand, waitTimeSeconds: Double): List<BehaviorBand> {
        val allBands = BehaviorBand.values().toList()
        val currentIndex = allBands.indexOf(currentBand)
        val bands = mutableListOf(currentBand)

        if (waitTimeSeconds >= 4.0) {
            if (currentIndex > 0) bands.add(allBands[currentIndex - 1])
            if (currentIndex < allBands.size - 1) bands.add(allBands[currentIndex + 1])
        }
        if (waitTimeSeconds >= 8.0) {
            if (currentIndex > 1) bands.add(allBands[currentIndex - 2])
            if (currentIndex < allBands.size - 2) bands.add(allBands[currentIndex + 2])
        }

        return bands.distinct()
    }

    private fun findBestCombination(
        myEntry: QueueEntry,
        candidates: List<QueueEntry>,
        k: Int
    ): List<QueueEntry> {
        if (k == 0) return emptyList()
        if (candidates.size < k) return emptyList()

        // ساده‌سازی: k بازیکن اول را با کمترین اختلاف مهارت انتخاب می‌کنیم
        return candidates
            .sortedBy { abs(it.skillRating.mean - myEntry.skillRating.mean) }
            .take(k)
    }
}
