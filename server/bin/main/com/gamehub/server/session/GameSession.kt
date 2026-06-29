// server/src/main/kotlin/com/gamehub/server/session/GameSession.kt
package com.gamehub.server.session

import com.gamehub.games.backgammon.BackgammonState
import com.gamehub.games.blokus.BlokusState
import com.gamehub.games.chess.ChessState
import com.gamehub.games.ludo.LudoState
import com.gamehub.games.matchmonster.MatchMonsterState
import com.gamehub.games.monopoly.MonopolyState
import com.gamehub.games.soccerstriker.SoccerStrikerState
import com.gamehub.games.yahtzee.YahtzeeState
import com.gamehub.server.completion.MatchCompletionService
import com.gamehub.server.modules.GameSessionManager
import com.gamehub.server.repository.GameEventLogRepository
import com.gamehub.server.serverGameJson
import com.gamehub.server.wal.GameEvent
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameServerContract
import com.gamehub.shared.engine.GameSnapshot
import com.gamehub.shared.engine.GameUpdateResult
import com.gamehub.shared.engines.board.BoardState
import com.gamehub.shared.engines.card.CardGameState
import com.gamehub.shared.networking.GameOverMsg
import com.gamehub.shared.networking.GameStateUpdateMsg
import com.gamehub.shared.networking.WsMessage
import com.gamehub.shared.replay.GameEventType
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.util.UUID

class GameSession<State : GameState, Action : GameAction, Result : GameResult>(
    override val gameId: String,
    val gameSessionId: UUID,
    override val definition: GameDefinition<State, Action, Result>,
    initialPlayers: List<PlayerId>,
    private val cache: CacheProvider? = null,
    private val botDifficultyLevels: Map<PlayerId, Int> = emptyMap(),
    private val matchCompletionService: MatchCompletionService? = null,
    private val eventLogRepo: GameEventLogRepository? = null
) : GameServerContract<State, Action, Result> {

    private val playerSessions = mutableMapOf<PlayerId, WebSocketSession>()
    override val players: List<PlayerId> = initialPlayers.toList()
    override var currentState: State = definition.createInitialState(players)

    private val actionChannel = Channel<Pair<PlayerId, Action>>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var snapshotVersion = 0L
    private var botSimulators = mutableMapOf<PlayerId, BotPlayerSimulator<State, Action>>()
    private var isGameEnded = false
    // Snapshot دوره‌ای
    private var moveCount = 0
    private val SNAPSHOT_INTERVAL_MOVES = 5
    private val SNAPSHOT_INTERVAL_MS = 15000L
    private var lastSnapshotTime = System.currentTimeMillis()
    // ========== فیلدهای جدید برای Grace Period ==========
    // زمان مطلق پایان نوبت فعلی (بر اساس زمان سرور)
    private var turnDeadline: Long = 0L

    // آیا تایمر نوبت متوقف شده است؟
    private var isTurnTimerPaused: Boolean = false

    // زمان باقیمانده نوبت در زمان توقف (میلی‌ثانیه)
    private var pausedRemainingMs: Long = 0L

    // تعداد دفعات استفاده از Grace برای هر بازیکن
    private val graceUsedCount = mutableMapOf<PlayerId, Int>().withDefault { 0 }

    // تعداد نوبت‌های از دست رفته برای هر بازیکن
    private val missedTurnsCount = mutableMapOf<PlayerId, Int>().withDefault { 0 }

    // Job تایمر (برای لغو خودکار)
    private var turnTimerJob: Job? = null
    // Job Grace Period (برای قطعی)
    private var graceTimerJob: Job? = null

    // تنظیمات بازی (از کانفیگ خوانده می‌شود)
    private var disconnectGraceSeconds: Int = 20
    private var maxGracePeriodsPerPlayer: Int = 2
    private var maxMissedTurnsBeforeRemoval: Int = 3

    // شمارنده sequence برای Event Sourcing
    private var currentSequenceNumber = 0L

    init {
        // Debug serialization
        try {
            println("🔍 Debug serializing initial state...")
            val initialStateJson = serverGameJson.encodeToString(GameState.serializer(), currentState as GameState)
        } catch (e: Exception) {
            println("❌ Failed to serialize initial state!")
            e.printStackTrace()
        }

        // ثبت GAME_START در Event Log
        scope.launch {
            writeToWAL(
                eventType = GameEventType.GAME_START.name,
                playerId = null,
                payload = JsonObject(mapOf())
            )
        }

        startGameLoop()
        for ((playerId, difficulty) in botDifficultyLevels) {
            println("🤖 Starting bot simulator for $playerId with difficulty $difficulty")
            val simulator = BotPlayerSimulator(
                gameSession = this,
                botPlayerId = playerId,
                difficultyLevel = difficulty,
                scope = scope
            )
            simulator.start()
            botSimulators[playerId] = simulator
        }
    }

    fun addPlayerSession(playerId: PlayerId, session: WebSocketSession) {
        playerSessions[playerId] = session
    }

    override suspend fun submitAction(playerId: PlayerId, action: Action): GameUpdateResult<State, Result> {
        submitActionUnsafe(playerId, action)
        return GameUpdateResult(currentState, null)
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun submitActionUnsafe(playerId: PlayerId, action: GameAction) {
        try {
            actionChannel.send(playerId to (action as Action))
        } catch (e: Exception) {
            println("❌ خطا در submitActionUnsafe: ${e.message}")
        }
    }

    // Event Sourcing - ثبت رویداد در WAL
    private suspend fun writeToWAL(eventType: String, playerId: PlayerId? = null, payload: JsonObject) {
        if (eventLogRepo == null) return

        // Add current game state to the payload
        val stateJson = serverGameJson.encodeToString(GameState.serializer(), currentState as GameState)
        val payloadWithState = JsonObject(payload + mapOf("state" to serverGameJson.parseToJsonElement(stateJson)))

        val event = GameEvent(
            eventId = UUID.randomUUID(),
            gameSessionId = gameSessionId,
            gameType = definition.metadata.id,
            eventType = eventType,
            playerId = playerId?.value,
            timestamp = Instant.now(),
            sequenceNumber = ++currentSequenceNumber,
            payload = payloadWithState,
            isApplied = true,
            appliedAt = Instant.now()
        )

        try {
            eventLogRepo.insert(event)
            println("📝 Event logged: $eventType for game $gameSessionId (seq: $currentSequenceNumber)")
        } catch (e: Exception) {
            println("❌ Failed to write to WAL: ${e.message}")
        }
    }

    /**
     * استخراج شناسه بازیکن فعلی از وضعیت بازی
     * هر بازی ممکن است currentPlayer را در جای متفاوتی ذخیره کند
     */
    private fun getCurrentPlayerId(): PlayerId? {
        return when (val state = currentState) {
            is MonopolyState -> state.currentPlayer
            is BoardState -> state.currentPlayer
            is CardGameState -> state.currentPlayer
            is LudoState -> state.currentPlayer
            is ChessState -> state.currentPlayer
            is BackgammonState -> state.currentPlayer
            is com.gamehub.games.esmofamil.EsmoFamilState -> state.currentPlayer
            is com.gamehub.games.bridge.BridgeState -> state.currentPlayer
            is com.gamehub.games.checkers.CheckersState -> state.currentPlayer
            is com.gamehub.games.baltazar.BaltazarState -> state.currentPlayer
            is com.gamehub.games.othello.OthelloState -> state.currentPlayer
            is com.gamehub.games.spadesbaloot.SpadesBalootState -> state.currentPlayer
            is BlokusState -> state.currentPlayer
            is YahtzeeState -> state.currentPlayer
            is com.gamehub.games.battleship.BattleshipState -> state.currentPlayer
            is MatchMonsterState -> state.currentPlayer
            is SoccerStrikerState -> state.currentPlayer
            else -> {
                // fallback: try reflection (برای بازی‌های جدید که اضافه می‌شوند)
                try {
                    val field = state.javaClass.getDeclaredField("currentPlayer")
                    field.isAccessible = true
                    field.get(state) as? PlayerId
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun takeSnapshot(): GameSnapshot {
        val stateJson = serverGameJson.encodeToString(GameState.serializer(), currentState as GameState)
        return GameSnapshot(
            gameId = gameId,
            gameType = definition.metadata.id,
            stateJson = stateJson,
            players = players.map { it.value },
            version = snapshotVersion,
            // فیلدهای جدید
            turnDeadline = if (!isTurnTimerPaused) turnDeadline else null,
            isTurnTimerPaused = isTurnTimerPaused,
            pausedRemainingMs = pausedRemainingMs,
            graceUsedCount = graceUsedCount.mapKeys { it.key.value }.mapValues { it.value },
            missedTurnsCount = missedTurnsCount.mapKeys { it.key.value }.mapValues { it.value },
            lastActivePlayerId = getCurrentPlayerId()?.value
        )
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun restoreFromSnapshot(snapshot: GameSnapshot): State {
        val state = serverGameJson.decodeFromString(GameState.serializer(), snapshot.stateJson)
        currentState = state as State
        snapshotVersion = snapshot.version

        // بازیابی فیلدهای Grace
        turnDeadline = snapshot.turnDeadline ?: 0L
        isTurnTimerPaused = snapshot.isTurnTimerPaused
        pausedRemainingMs = snapshot.pausedRemainingMs
        graceUsedCount.clear()
        snapshot.graceUsedCount.forEach { (userId, count) ->
            graceUsedCount[PlayerId(userId)] = count
        }
        missedTurnsCount.clear()
        snapshot.missedTurnsCount.forEach { (userId, count) ->
            missedTurnsCount[PlayerId(userId)] = count
        }

        // اگر تایمر متوقف بود و زمان باقیمانده دارد، دوباره راه‌اندازی کن
        if (isTurnTimerPaused && pausedRemainingMs > 0) {
            resumeTurnTimer()
        } else if (!isTurnTimerPaused && turnDeadline > 0) {
            startTurnTimer()
        }

        return currentState
    }

    fun getState(): GameState = currentState as GameState

    private suspend fun saveSnapshotToRedis() {
        if (cache == null) return
        val snapshot = takeSnapshot()
        val snapshotJson = serverGameJson.encodeToString(GameSnapshot.serializer(), snapshot)
        cache.set("snapshot:$gameId", snapshotJson, 3600 * 24 * 7) // TTL 7 روز
        println("📸 Snapshot saved for game $gameId (version ${snapshot.version})")
    }

    @Suppress("UNCHECKED_CAST")
    private fun startGameLoop() {
        scope.launch {
            broadcastState()
            for ((playerId, action) in actionChannel) {
                try {
                    val typedDef = definition
                    if (typedDef.validateAction(currentState, action, playerId)) {
                        println("✅ Validation passed for ${action::class.simpleName} from $playerId")

                        // ثبت اکشن در Event Log قبل از اعمال
                        val actionJson = serverGameJson.encodeToString(GameAction.serializer(), action as GameAction)
                        writeToWAL(
                            eventType = GameEventType.PLAYER_ACTION.name,
                            playerId = playerId,
                            payload = serverGameJson.parseToJsonElement(actionJson).jsonObject
                        )

                        val result = typedDef.applyAction(currentState, action, playerId)
                        currentState = result.newState
                        if (getCurrentPlayerId() != null) {
                            startTurnTimer()
                        }
                        snapshotVersion++
                        broadcastState()

                        moveCount++
                        val now = System.currentTimeMillis()
                        if (moveCount >= SNAPSHOT_INTERVAL_MOVES || now - lastSnapshotTime >= SNAPSHOT_INTERVAL_MS) {
                            saveSnapshotToRedis()
                            moveCount = 0
                            lastSnapshotTime = now
                        }

                        if (typedDef.isTerminal(currentState)) {
                            // ثبت GAME_END در Event Log
                            writeToWAL(
                                eventType = GameEventType.GAME_END.name,
                                playerId = null,
                                payload = JsonObject(mapOf("result" to kotlinx.serialization.json.JsonPrimitive(serverGameJson.encodeToString(GameResult.serializer(), result.result as GameResult))))
                            )
                            broadcastGameEnd(result.result as? GameResult)
                            cache?.delete("snapshot:$gameId")
                            botSimulators.values.forEach { it.stop() }
                            for (player in players) {
                                GameSessionManager.removeActiveGameForUser(player.value, gameId)
                            }
                            break
                        }
                    } else {
                        println("❌ Validation failed for ${action::class.simpleName} from $playerId")
                    }
                } catch (e: Exception) {
                    println("❌ خطا در حلقه بازی: ${e.message}")
                }
            }
        }
    }

    suspend fun broadcastState() {
        try {
            val stateJson = serverGameJson.encodeToString(GameState.serializer(), currentState as GameState)
            val stateElement = serverGameJson.parseToJsonElement(stateJson)
            val currentPlayer = getCurrentPlayerId()?.value
            val msg = GameStateUpdateMsg(
                gameId = gameId,
                statePayload = stateElement,
                currentTurnPlayerId = currentPlayer
            )
            val json = serverGameJson.encodeToString(WsMessage.serializer(), msg)
            playerSessions.values.forEach { session ->
                try {
                    session.send(Frame.Text(json))
                } catch (e: Exception) {
                    println("❌ Failed to send state to player: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ خطا در broadcastState: ${e.message}")
            e.printStackTrace()
        }
    }
    /**
     * شروع تایمر نوبت برای بازیکن فعلی
     * اگر قبلاً تایمری در حال اجراست، آن را لغو می‌کند
     */
    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        val currentPlayer = getCurrentPlayerId() ?: return
        val turnTimeoutSeconds = 30 // بعداً از کانفیگ خوانده شود

        turnDeadline = System.currentTimeMillis() + (turnTimeoutSeconds * 1000L)
        isTurnTimerPaused = false

        turnTimerJob = scope.launch {
            delay(turnTimeoutSeconds * 1000L)
            if (!isTurnTimerPaused && getCurrentPlayerId() == currentPlayer) {
                handleTurnTimeout(currentPlayer)
            }
        }
    }


    /**
     * توقف تایمر نوبت (در زمان قطعی کاربر)
     */
    private fun pauseTurnTimer() {
        if (isTurnTimerPaused) return
        val remaining = turnDeadline - System.currentTimeMillis()
        if (remaining > 0) {
            pausedRemainingMs = remaining
            isTurnTimerPaused = true
        }
        turnTimerJob?.cancel()
    }

    /**
     * ادامه تایمر نوبت (پس از reconnect موفق)
     */
    private fun resumeTurnTimer() {
        if (!isTurnTimerPaused) return
        val currentPlayerId = getCurrentPlayerId()
        // اگر بازیکن فعلی عوض شده باشد (مثلاً در اثر timeout سایرین)، تایمر را راه‌اندازی نکن
        if (currentPlayerId == null) return

        if (pausedRemainingMs > 0) {
            turnDeadline = System.currentTimeMillis() + pausedRemainingMs
            isTurnTimerPaused = false
            println("⏱️ Resuming turn timer for $currentPlayerId, remaining ${pausedRemainingMs}ms")
            turnTimerJob = scope.launch {
                delay(pausedRemainingMs)
                if (!isTurnTimerPaused && getCurrentPlayerId() == currentPlayerId) {
                    handleTurnTimeout(currentPlayerId)
                }
            }
        }
        pausedRemainingMs = 0
    }

    /**
     * مدیریت انقضای نوبت (بدون حرکت)
     */
    private suspend fun handleTurnTimeout(playerId: PlayerId) {
        // Don't handle turn timeouts for EsmoFamil, since it's not a turn-based game
        if (currentState is com.gamehub.games.esmofamil.EsmoFamilState) {
            return
        }

        // ثبت TURN_TIMEOUT در Event Log
        writeToWAL(
            eventType = GameEventType.TURN_TIMEOUT.name,
            playerId = playerId,
            payload = JsonObject(mapOf())
        )

        val newMissed = (missedTurnsCount[playerId] ?: 0) + 1
        missedTurnsCount[playerId] = newMissed

        println("⏰ Turn timeout for $playerId (missed $newMissed/$maxMissedTurnsBeforeRemoval)")

        // اگر تعداد misses به حد مجاز رسید، بازیکن را حذف کن
        if (newMissed >= maxMissedTurnsBeforeRemoval) {
            println("🚫 Removing player $playerId due to repeated timeouts")
            // حذف بازیکن از بازی (به عنوان بازنده)
            val remainingPlayers = players.filter { it != playerId }
            if (remainingPlayers.size == 1) {
                val winner = remainingPlayers.first()
                if (!isGameEnded) {
                    handleGameEnd(GameResult.Win(winner))
                }
                return
            } else {
                // ادامه بازی بدون این بازیکن (در آینده پیاده‌سازی شود)
                // فعلاً فقط نوبت را رد می‌کنیم
                val skipResult = definition.skipTurn(currentState, playerId)
                currentState = skipResult.newState
                broadcastState()
                startTurnTimer()
            }
            return
        }

        // رد کردن نوبت با استفاده از definition.skipTurn
        val skipResult = definition.skipTurn(currentState, playerId)
        currentState = skipResult.newState

        if (skipResult.result != null) {
            handleGameEnd(skipResult.result!!)
        } else {
            broadcastState()
            startTurnTimer()
        }
    }
    // اضافه کردن به انتهای کلاس GameSession (قبل از آخرین } )
    /**
     * مدیریت پایان بازی و ذخیره نتیجه
     */
    private suspend fun handleGameEnd(result: GameResult) {
        println("🎮 handleGameEnd() CALLED for game: $gameId, result: $result, isGameEnded: $isGameEnded")
        if (isGameEnded) return
        isGameEnded = true

        println("🎮 Calling matchCompletionService.onMatchEnd()")
        matchCompletionService?.onMatchEnd(gameId, definition.metadata.id, players, result, gameSessionId.toString())
        cache?.delete("snapshot:$gameId")
        botSimulators.values.forEach { it.stop() }
        for (player in players) {
            GameSessionManager.removeActiveGameForUser(player.value, gameId)
        }
    }
    /**
     * ثبت قطعی یک بازیکن
     * @return true اگر Grace Period شروع شده باشد، false اگر سهمیه تمام شده یا کاربر از قبل خارج شده باشد
     */
    suspend fun onPlayerDisconnect(playerId: PlayerId): Boolean {
        val usedCount = graceUsedCount[playerId] ?: 0
        if (usedCount >= maxGracePeriodsPerPlayer) {
            handleTurnTimeout(playerId)
            return false
        }
        graceUsedCount[playerId] = usedCount + 1
        pauseTurnTimer()
        graceTimerJob?.cancel()
        graceTimerJob = scope.launch {
            delay(disconnectGraceSeconds * 1000L)
            if (isTurnTimerPaused) {
                handleTurnTimeout(playerId)
                isTurnTimerPaused = false
            }
        }
        return true
    }

    /**
     * ثبت reconnect موفق یک بازیکن
     */
    suspend fun onPlayerReconnect(playerId: PlayerId): Boolean {
        // لغو Grace Timer
        graceTimerJob?.cancel()

        // ادامه تایمر نوبت
        resumeTurnTimer()

        return true
    }

    /**
     * گرفتن وضعیت Grace برای یک بازیکن (برای نمایش در UI)
     */
    fun getGraceInfo(playerId: PlayerId): Map<String, Any> {
        return mapOf(
            "usedCount" to (graceUsedCount[playerId] ?: 0),
            "maxAllowed" to maxGracePeriodsPerPlayer,
            "missedTurns" to (missedTurnsCount[playerId] ?: 0)
        )
    }

    /**
     * به‌روزرسانی تنظیمات از GameConfig (بعداً تکمیل می‌شود)
     */
    fun updateConfig(disconnectGraceSeconds: Int, maxGracePeriods: Int, maxMissedTurns: Int) {
        this.disconnectGraceSeconds = disconnectGraceSeconds
        this.maxGracePeriodsPerPlayer = maxGracePeriods
        this.maxMissedTurnsBeforeRemoval = maxMissedTurns
    }

    private suspend fun broadcastGameEnd(result: GameResult?) {
        println("📢 broadcastGameEnd() CALLED, result: $result")
        val winnerId = when (result) {
            is GameResult.Win -> result.winner.value
            else -> null
        }
        val msg = GameOverMsg(
            gameId = gameId,
            winnerId = winnerId,
            results = emptyMap()
        )
        val json = serverGameJson.encodeToString(WsMessage.serializer(), msg)
        playerSessions.values.forEach { session ->
            try { session.send(Frame.Text(json)) } catch (_: Exception) {}
        }
        if (result != null && matchCompletionService != null) {
            println("📢 Calling matchCompletionService.onMatchEnd() (from broadcastGameEnd)")
            matchCompletionService.onMatchEnd(gameId, definition.metadata.id, players, result, gameSessionId.toString())
        }
    }
}
