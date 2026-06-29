package com.gamehub.games.chess

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class ChessAction : GameAction() {
    @Serializable
    data class Move(
        val from: Position,
        val to: Position,
        val promotion: ChessPieceType? = null
    ) : ChessAction()
}
