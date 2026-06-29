package com.gamehub.games.backgammon

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class BackgammonAction : GameAction() {
    @Serializable
    data object RollDice : BackgammonAction()

    @Serializable
    data object OfferDouble : BackgammonAction()

    @Serializable
    data object AcceptDouble : BackgammonAction()

    @Serializable
    data object DeclineDouble : BackgammonAction()

    @Serializable
    data class Move(
        val from: Int, // 0 for bar, 1-24 for points, 25 for borne off
        val to: Int,
        val die: Int
    ) : BackgammonAction()

    @Serializable
    data object EndTurn : BackgammonAction()
}
