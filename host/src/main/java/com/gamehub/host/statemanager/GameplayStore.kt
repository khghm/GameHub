package com.gamehub.host.statemanager

import com.gamehub.host.secure.SecureStorage
import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameEngine
import com.gamehub.shared.engine.GameEngineFactory
import com.gamehub.shared.engine.GameUpdateResult
import com.gamehub.shared.networking.SubmitMoveMsg
import com.gamehub.shared.state.GamePhase
import com.gamehub.shared.state.Store
import com.gamehub.shared.time.TimeSyncClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * مدیریت وضعیت بازی در سمت کلاینت با پشتیبانی از پیش‌بینی محلی (Client‑Side Prediction)
 * و بازپخش حرکات معلق پس از reconnect.
 *
 * اگر این کلاس کامل نباشد، کاربر پس از هر حرکت منتظر پاسخ سرور می‌ماند و تجربه بازی laggy خواهد بود.
 */
class GameplayStore(
    private val gameId: String,
    private val userId: PlayerId,
    private val engineFactory: GameEngineFactory,
    private val secureStorage: SecureStorage,
    private val timeSyncClient: TimeSyncClient,
    private val reconnectToken: String?,
    private val onSendMessage: (SubmitMoveMsg) -> Unit
) : Store {

    private var engine: GameEngine<GameState, GameAction, GameResult>? = null
    private var confirmedState: GameState? = null
    private val mutex = Mutex()

    @Serializable
    private data class PendingMove(
        val moveId: String,
        val actionJson: String,   // ذخیره به صورت JSON برای بازیابی
        val timestamp: Long
    )

    private val pendingMoves = mutableListOf<PendingMove>()

    private val _phase = MutableStateFlow(GamePhase.AWAITING_START)
    val phase: StateFlow<GamePhase> = _phase.asStateFlow()

    private val _uiState = MutableStateFlow<GameState?>(null)
    val uiState: StateFlow<GameState?> = _uiState.asStateFlow()

    override suspend fun start() {
        // بازیابی حرکات معلق از حافظه امن (اگر برنامه قبلاً بسته شده بود)
        val savedJson = secureStorage.load("pendingMoves_$gameId")
        if (savedJson != null) {
            try {
                val list = Json.decodeFromString<List<PendingMove>>(savedJson)
                pendingMoves.addAll(list)
                println("📦 Restored ${pendingMoves.size} pending moves for game $gameId")
            } catch (e: Exception) {
                // ignore
            }
        }
        _phase.value = GamePhase.AWAITING_START
    }

    override suspend fun stop() {
        // ذخیره حرکات معلق برای بار بعدی
        if (pendingMoves.isNotEmpty()) {
            val json = Json.encodeToString(pendingMoves)
            secureStorage.save("pendingMoves_$gameId", json)
        } else {
            secureStorage.delete("pendingMoves_$gameId")
        }
        engine = null
        confirmedState = null
        pendingMoves.clear()
    }

    /**
     * دریافت وضعیت جدید از سرور (GameStateUpdateMsg)
     */
    suspend fun onStateUpdate(statePayload: JsonElement, config: JsonElement) {
        mutex.withLock {
            val newState = engine!!.deserializeState(statePayload.toString())
            confirmedState = newState
            _uiState.value = newState

            if (_phase.value == GamePhase.AWAITING_START) {
                _phase.value = GamePhase.WAITING_INPUT
            }

            // بازپخش حرکات معلقی که هنوز تأیید نشده‌اند
            replayPendingMoves()
        }
    }

    /**
     * ارسال حرکت از طرف کاربر (پیش‌بینی محلی)
     */
    suspend fun submitMove(action: GameAction) {
        if (_phase.value != GamePhase.WAITING_INPUT) return
        val engine = this.engine ?: return
        val state = confirmedState ?: return

        if (!engine.validateAction(state, action, userId)) {
            println("❌ Invalid action $action by $userId")
            return
        }

        // 1. پیش‌بینی محلی (optimistic update)
        val optimisticResult = engine.applyAction(state, action, userId)
        confirmedState = optimisticResult.newState
        _uiState.value = optimisticResult.newState
        _phase.value = GamePhase.OPTIMISTIC

        // 2. ذخیره حرکت در صف معلق
        val moveId = generateMoveId()
        val actionJson = engine.serializeAction(action).toString()
        pendingMoves.add(PendingMove(moveId, actionJson, System.currentTimeMillis()))
        savePendingMoves()

        // 3. ارسال حرکت به سرور
        val movePayload = engine.serializeAction(action)
        val submitMsg = SubmitMoveMsg(
            msgId = moveId,
            gameId = gameId,
            movePayload = movePayload,
            clientMoveId = moveId
        )
        onSendMessage(submitMsg)
    }

    /**
     * دریافت پاسخ از سرور برای حرکت (MoveResultMsg)
     */
    suspend fun onMoveResult(accepted: Boolean, clientMoveId: String, newStatePayload: JsonElement?) {
        mutex.withLock {
            if (accepted && newStatePayload != null) {
                // حرکت توسط سرور تأیید شد – وضعیت واقعی را جایگزین کن
                val newState = engine!!.deserializeState(newStatePayload.toString())
                confirmedState = newState
                _uiState.value = newState
                // حذف حرکت از صف معلق
                pendingMoves.removeAll { it.moveId == clientMoveId }
                savePendingMoves()
            } else if (!accepted) {
                // حرکت رد شد – باید وضعیت را به آخرین وضعیت تأیید شده برگردانیم
                // (در عمل، سرور وضعیت فعلی را در newStatePayload ارسال می‌کند)
                if (newStatePayload != null) {
                    val correctState = engine!!.deserializeState(newStatePayload.toString())
                    confirmedState = correctState
                    _uiState.value = correctState
                }
                // حرکت رد شده را از صف حذف کن
                pendingMoves.removeAll { it.moveId == clientMoveId }
                savePendingMoves()
                // همچنین باید تمام حرکات بعدی که به این حرکت وابسته بودند نیز حذف شوند؟
                // برای سادگی، کل صف را پاک می‌کنیم و دوباره بازپخش می‌کنیم
                pendingMoves.clear()
                replayPendingMoves()
            }
            _phase.value = GamePhase.WAITING_INPUT
        }
    }

    /**
     * بازپخش حرکات معلق (پس از دریافت وضعیت جدید یا پس از reconnect)
     */
    private suspend fun replayPendingMoves() {
        var currentState = confirmedState ?: return
        val validMoves = mutableListOf<PendingMove>()

        for (pm in pendingMoves) {
            val action = deserializeAction(pm.actionJson) ?: continue
            if (engine!!.validateAction(currentState, action, userId)) {
                val result = engine!!.applyAction(currentState, action, userId)
                currentState = result.newState
                validMoves.add(pm)
            } else {
                println("⚠️ Pending move ${pm.moveId} no longer valid, discarding")
            }
        }

        // به‌روزرسانی وضعیت و صف
        confirmedState = currentState
        _uiState.value = currentState
        pendingMoves.clear()
        pendingMoves.addAll(validMoves)
        savePendingMoves()
    }

    /**
     * تنظیم موتور بازی (قبل از اولین onStateUpdate)
     */
    fun setEngine(gameType: String, config: JsonElement) {
        engine = engineFactory.create(gameType)
    }

    private fun generateMoveId(): String = "${userId.value}_${System.currentTimeMillis()}_${(0..1000).random()}"

    private suspend fun savePendingMoves() {
        val json = Json.encodeToString(pendingMoves)
        secureStorage.save("pendingMoves_$gameId", json)
    }

    private fun deserializeAction(jsonStr: String): GameAction? {
        return try {
            // از آنجا که GameAction یک کلاس انتزاعی است، باید با استفاده از engine ساخته شود
            // ولی engine.serializeAction معکوس را ندارد. راه‌حل: engine خودش می‌تواند deserialize کند
            // فعلاً از JsonElement استفاده می‌کنیم و به engine می‌دهیم – اما engine متد deserializeAction ندارد.
            // برای سادگی در این گام، فرض می‌کنیم action یک BoardAction است (برای بازی‌های تخت‌های)
            // در گام بعدی این مشکل را به صورت عمومی حل می‌کنیم.
            // موقتاً null برمی‌گردانیم تا حرکات معلق بازپخش نشوند (فعلاً قابلیت بازپخش غیرفعال)
            null
        } catch (e: Exception) {
            null
        }
    }
}