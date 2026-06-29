package com.gamehub.games.chess

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class ChessState(
    val board: List<List<ChessPiece?>>,
    val currentPlayer: PlayerId?,
    val players: List<PlayerId>,
    val turn: ChessColor = ChessColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Position? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) : GameState()

@Serializable
data class Position(val row: Int, val col: Int)

@Serializable
data class ChessPiece(
    val type: ChessPieceType,
    val color: ChessColor
)

@Serializable
enum class ChessPieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

@Serializable
enum class ChessColor {
    WHITE, BLACK
}

@Serializable
data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
)
