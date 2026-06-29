package com.gamehub.games.abalone

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class AbaloneAction : GameAction() {
    @Serializable
    data class Move(
        val selectedMarbles: List<AbalonePos>,
        val direction: AbaloneDirection
    ) : AbaloneAction()
}
