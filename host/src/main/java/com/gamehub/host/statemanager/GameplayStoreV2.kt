// host/src/main/java/com/gamehub/host/statemanager/GameplayStoreV2.kt
package com.gamehub.host.statemanager

import com.gamehub.host.secure.SecureStorage
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameEngine
import com.gamehub.shared.engine.GameEngineFactory
import com.gamehub.shared.networking.SubmitMoveMsg
import com.gamehub.shared.state.GamePhase
import com.gamehub.shared.state.Store
import com.gamehub.shared.time.TimeSyncClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

class GameplayStoreV2(
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
    private val pendingMoves = mutableListOf<PendingMove>()
    private val inputBuffer = LinkedList<GameAction>()
    private val transactionHistory = LinkedList<GameTransactionRecord>()

    private val mutex = Mutex()
    private var isReconciling = false
    private var animationJob: Job? = null

    @Serializable
    private data class StoredPendingMove(
        val moveId: String,
        val actionJson: String,
        val timestamp: Long
    )

    private data class PendingMove(
        val moveId: String,
        val action: GameAction,
        val timestamp: Long,
        val optimisticState: GameState
    )

    private data class GameTransactionRecord(
        val previousState: GameState,
        val action: GameAction,
        val newState: GameState,
        val timestamp: Long
    )

    private val _phase = MutableStateFlow(GamePhase.AWAITING_START)
    val phase: StateFlow<GamePhase> = _phase.asStateFlow()

    private val _uiState = MutableStateFlow<GameState?>(null)
    val uiState: StateFlow<GameState?> = _uiState.asStateFlow()

    private val _pendingMovesCount = MutableStateFlow(0)
    val pendingMovesCount: StateFlow<Int> = _pendingMovesCount.asStateFlow()

    override suspend fun start() {
        // Restore pending moves from storage if they exist
        val savedJson = secureStorage.load("pendingMovesV2_$gameId")
        if (savedJson != null) {
            try {
                // We'll keep the stored pending moves but we'll recreate their actions once engine is initialized
                // For now, just store them as JSON and we'll deserialize them in onStateUpdate
            } catch (e: Exception) {
                // ignore errors
            }
        }
        _phase.value = GamePhase.AWAITING_START
    }

    override suspend fun stop() {
        animationJob?.cancel()
        secureStorage.delete("pendingMovesV2_$gameId")
        engine = null
        confirmedState = null
        pendingMoves.clear()
        inputBuffer.clear()
        transactionHistory.clear()
    }

    suspend fun onStateUpdate(statePayload: JsonElement, config: JsonElement) {
        mutex.withLock {
            val wasEngineNull = engine == null
            // First deserialize newState
            val newState = if (engine == null) {
                val gameType = extractGameTypeFromPayload(statePayload)
                val tempEngine = engineFactory.create(gameType)
                tempEngine.deserializeState(statePayload.toString())
            } else {
                engine!!.deserializeState(statePayload.toString())
            }

            if (engine == null) {
                val gameType = extractGameTypeFromPayload(statePayload)
                engine = engineFactory.create(gameType)
                // Restore pending moves if we just initialized the engine!
                val savedJson = secureStorage.load("pendingMovesV2_$gameId")
                if (savedJson != null) {
                    try {
                        val storedList = Json.decodeFromString<List<StoredPendingMove>>(savedJson)
                        val restoredMoves = mutableListOf<PendingMove>()
                        // We don't restore optimistic state, we'll recompute it in replayPendingMoves
                        for (stored in storedList) {
                            try {
                                // We don't have deserializeAction available right now, so skip restoring moves
                                // until that's added to GameEngine
                            } catch (_: Exception) {
                                // Skip invalid pending moves
                            }
                        }
                        pendingMoves.clear()
                        pendingMoves.addAll(restoredMoves)
                        _pendingMovesCount.value = pendingMoves.size
                    } catch (_: Exception) {
                        // Ignore
                    }
                }
            }

            confirmedState = newState
            engine!!.restoreState(newState)
            _uiState.value = newState

            if (_phase.value == GamePhase.AWAITING_START) {
                _phase.value = GamePhase.WAITING_INPUT
            }

            if (!isReconciling) {
                replayPendingMoves()
            }
        }
    }

    suspend fun submitMove(action: GameAction, skipBuffer: Boolean = false) {
        if (_phase.value != GamePhase.WAITING_INPUT) return
        val engine = this.engine ?: return
        val state = confirmedState ?: return

        if (!engine.validateAction(state, action, userId)) return

        if (!skipBuffer && isAnimationPlaying()) {
            inputBuffer.addLast(action)
            return
        }

        executeMove(action)
    }

    private suspend fun executeMove(action: GameAction) {
        val engine = this.engine ?: return
        val state = confirmedState ?: return

        val moveId = generateMoveId()
        val optimisticResult = engine.applyAction(state, action, userId)
        val optimisticState = optimisticResult.newState

        transactionHistory.addLast(
            GameTransactionRecord(state, action, optimisticState, System.currentTimeMillis())
        )
        if (transactionHistory.size > 10) transactionHistory.removeFirst()

        confirmedState = optimisticState
        _uiState.value = optimisticState
        _phase.value = GamePhase.OPTIMISTIC

        pendingMoves.add(PendingMove(moveId, action, System.currentTimeMillis(), optimisticState))
        _pendingMovesCount.value = pendingMoves.size
        secureStorage.save("pendingMovesV2_$gameId", serializePendingMoves())

        val movePayload = engine.serializeAction(action)
        val submitMsg = SubmitMoveMsg(
            msgId = moveId,
            gameId = gameId,
            movePayload = movePayload,
            clientMoveId = moveId
        )
        onSendMessage(submitMsg)

        if (inputBuffer.isNotEmpty()) {
            delay(50)
            val nextAction = inputBuffer.removeFirst()
            executeMove(nextAction)
        }
    }

    suspend fun onMoveResult(accepted: Boolean, clientMoveId: String, newStatePayload: JsonElement?, errorCode: String? = null) {
        mutex.withLock {
            if (accepted && newStatePayload != null) {
                val newState = engine!!.deserializeState(newStatePayload.toString())
                confirmedState = newState
                engine!!.restoreState(newState)
                _uiState.value = newState

                pendingMoves.removeAll { it.moveId == clientMoveId }
                _pendingMovesCount.value = pendingMoves.size
                secureStorage.save("pendingMovesV2_$gameId", serializePendingMoves())

                _phase.value = GamePhase.WAITING_INPUT
            } else if (!accepted) {
                isReconciling = true
                pendingMoves.removeAll { it.moveId == clientMoveId }
                val lastConfirmedState = confirmedState ?: return@withLock
                engine!!.restoreState(lastConfirmedState)
                _uiState.value = lastConfirmedState
                replayPendingMoves()
                isReconciling = false
                _phase.value = GamePhase.WAITING_INPUT
            }
        }
    }

    private suspend fun replayPendingMoves() {
        val validMoves = mutableListOf<PendingMove>()
        var currentState = confirmedState!!
        for (pm in pendingMoves) {
            if (engine!!.validateAction(currentState, pm.action, userId)) {
                val tx = engine!!.applyAction(currentState, pm.action, userId)
                currentState = tx.newState
                validMoves.add(pm.copy(optimisticState = currentState))
            }
        }
        pendingMoves.clear()
        pendingMoves.addAll(validMoves)
        _pendingMovesCount.value = pendingMoves.size
        confirmedState = currentState
        engine!!.restoreState(currentState)
        _uiState.value = currentState
        secureStorage.save("pendingMovesV2_$gameId", serializePendingMoves())
    }

    suspend fun undoLastMove(): Boolean {
        if (_phase.value != GamePhase.WAITING_INPUT) return false
        if (transactionHistory.size < 2) return false

        val lastTransaction = transactionHistory.removeLast()
        val previousTransaction = transactionHistory.lastOrNull()
        val targetState = previousTransaction?.previousState ?: lastTransaction.previousState

        confirmedState = targetState
        engine!!.restoreState(targetState)
        _uiState.value = targetState

        if (pendingMoves.isNotEmpty()) {
            pendingMoves.removeAt(pendingMoves.size - 1)
            _pendingMovesCount.value = pendingMoves.size
        }

        val undoMsg = SubmitMoveMsg(
            msgId = generateMoveId(),
            gameId = gameId,
            movePayload = engine!!.parseToJsonElement("{\"type\":\"undo\"}"),
            clientMoveId = "undo_${System.currentTimeMillis()}"
        )
        onSendMessage(undoMsg)
        return true
    }

    private fun isAnimationPlaying(): Boolean = animationJob?.isActive == true

    fun setAnimationPlaying(isPlaying: Boolean) {
        if (isPlaying) {
            animationJob = CoroutineScope(Dispatchers.Main).launch {
                delay(500)
                if (inputBuffer.isNotEmpty() && _phase.value == GamePhase.WAITING_INPUT) {
                    val nextAction = inputBuffer.removeFirst()
                    executeMove(nextAction)
                }
            }
        } else {
            animationJob?.cancel()
        }
    }

    private fun generateMoveId(): String = "${userId.value}_${System.currentTimeMillis()}_${pendingMoves.size}"
    private fun serializePendingMoves(): String {
        val storedList = pendingMoves.map { pm ->
            StoredPendingMove(
                moveId = pm.moveId,
                actionJson = engine!!.serializeAction(pm.action).toString(),
                timestamp = pm.timestamp
            )
        }
        return Json.encodeToString(storedList)
    }

    private fun extractGameTypeFromPayload(payload: JsonElement): String {
        return try {
            // [GameHub] دسترسی ایمن به jsonObject
            val obj = payload.jsonObject
            obj["gameType"]?.jsonPrimitive?.content ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}