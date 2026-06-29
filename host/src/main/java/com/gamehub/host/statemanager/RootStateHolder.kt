// host/src/main/java/com/gamehub/host/statemanager/RootStateHolder.kt
package com.gamehub.host.statemanager

import com.gamehub.shared.state.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RootStateHolder(
    val gameplay: GameplayStore? = null,
    val spectator: SpectatorStore? = null,
    val lobby: LobbyStore,
    val social: SocialStore,
    val settings: SettingsStore,
    val error: ErrorStore
) {
    private val scope = CoroutineScope(SupervisorJob())

    suspend fun start() {
        listOfNotNull(gameplay, spectator, lobby, social, settings, error).forEach { store ->
            scope.launch { store.start() }
        }
    }

    suspend fun stop() {
        listOfNotNull(gameplay, spectator, lobby, social, settings, error).forEach { store ->
            scope.launch { store.stop() }
        }
    }
}