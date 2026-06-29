// shared/src/commonMain/kotlin/com/gamehub/shared/state/GamePhase.kt
package com.gamehub.shared.state

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    AWAITING_START,
    WAITING_INPUT,
    OPTIMISTIC,
    RECONCILING,
    GAME_OVER
}