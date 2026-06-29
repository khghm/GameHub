package com.gamehub.games.checkers

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class CheckersState(
    val players: List<PlayerId>,
    val currentPlayer: PlayerId?,
    val board: List<List<CheckersPiece?>>,
    val turn: CheckersColor = CheckersColor.RED,
    val lastPosition: Position? = null,
    val moveCount: Int = 0,
    val capturedPieces: Map<CheckersColor, Int> = mapOf(CheckersColor.RED to 0, CheckersColor.WHITE to 0)
) : GameState()
