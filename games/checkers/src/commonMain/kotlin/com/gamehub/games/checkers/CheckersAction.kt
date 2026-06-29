package com.gamehub.games.checkers

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class CheckersAction : GameAction() {
    @Serializable
    @SerialName("CheckersAction.Move")
    data class Move(
        val from: Position,
        val to: Position
    ) : CheckersAction()

    @Serializable
    @SerialName("CheckersAction.Capture")
    data class Capture(
        val path: List<Position> // First is starting pos, rest are landing positions
    ) : CheckersAction()
}
