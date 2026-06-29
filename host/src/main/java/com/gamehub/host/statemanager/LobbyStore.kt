// host/src/main/java/com/gamehub/host/statemanager/LobbyStore.kt
package com.gamehub.host.statemanager

import com.gamehub.shared.matchmaking.SkillRating
import com.gamehub.shared.networking.MatchmakingRequest
import com.gamehub.shared.networking.MatchmakingRequestMsg
import com.gamehub.shared.networking.SoloRequest
import com.gamehub.shared.state.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LobbyStore(
    private val userId: String,
    private val onSendMessage: (MatchmakingRequestMsg) -> Unit
) : Store {

    private val _queueStatus = MutableStateFlow<QueueStatus>(QueueStatus.Idle)
    val queueStatus: StateFlow<QueueStatus> = _queueStatus.asStateFlow()

    private val _estimatedWaitTime = MutableStateFlow(0)
    val estimatedWaitTime: StateFlow<Int> = _estimatedWaitTime.asStateFlow()

    enum class QueueStatus { Idle, Queuing, Searching, Matched }

    override suspend fun start() {}

    override suspend fun stop() {
        if (_queueStatus.value != QueueStatus.Idle) {
            cancelQueue()
        }
    }

    suspend fun joinQueue(gameType: String, mode: String, rating: SkillRating) {
        if (_queueStatus.value != QueueStatus.Idle) return
        _queueStatus.value = QueueStatus.Queuing

        val request = SoloRequest(
            userId = userId,
            gameId = gameType,
            mode = com.gamehub.shared.core.GameMode.valueOf(mode.uppercase())
        )
        val msg = MatchmakingRequestMsg(request = request)
        onSendMessage(msg)
        _queueStatus.value = QueueStatus.Searching
    }

    suspend fun cancelQueue() {
        _queueStatus.value = QueueStatus.Idle
        // TODO: ارسال پیام لغو به سرور
    }

    fun onMatchFound(gameId: String) {
        _queueStatus.value = QueueStatus.Matched
        // TODO: ناوبری به صفحه بازی
    }
}