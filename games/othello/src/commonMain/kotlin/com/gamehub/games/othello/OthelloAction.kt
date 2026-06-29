package com.gamehub.games.othello

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class OthelloAction : GameAction() {
    @Serializable
    @SerialName("OthelloAction.Move")
    data class Move(val row: Int, val col: Int) : OthelloAction()
}
