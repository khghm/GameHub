package com.gamehub.games.nard

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class NardAction : GameAction() {
    @Serializable
    data object RollDice : NardAction()

    @Serializable
    data object OfferDouble : NardAction()

    @Serializable
    data object AcceptDouble : NardAction()

    @Serializable
    data object DeclineDouble : NardAction()

    @Serializable
    data class Move(
        val from: Int, // 0 for bar, 1-24 for points, 25 for borne off
        val to: Int,
        val die: Int
    ) : NardAction()

    @Serializable
    data object EndTurn : NardAction()
}
