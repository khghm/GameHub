// host/src/main/java/com/gamehub/host/statemanager/SpectatorStore.kt
package com.gamehub.host.statemanager

import com.gamehub.shared.state.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpectatorStore : Store {
    private val _gameState = MutableStateFlow<String?>(null)
    val gameState: StateFlow<String?> = _gameState.asStateFlow()

    override suspend fun start() {}
    override suspend fun stop() {}

    fun onStateUpdate(stateJson: String) {
        _gameState.value = stateJson
    }
}