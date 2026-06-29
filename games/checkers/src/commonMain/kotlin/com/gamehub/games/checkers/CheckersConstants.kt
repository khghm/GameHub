package com.gamehub.games.checkers

import kotlinx.serialization.Serializable

// Position for board cells
@Serializable
data class Position(val row: Int, val col: Int)

// Checkers piece colors
@Serializable
enum class CheckersColor {
    RED, WHITE
}

// Checkers piece type (Man or King)
@Serializable
enum class CheckersPieceType {
    MAN, KING
}

// Checkers piece data class
@Serializable
data class CheckersPiece(
    val type: CheckersPieceType,
    val color: CheckersColor
)

// A capture move path for multi-jumps
@Serializable
data class CapturePath(
    val positions: List<Position>
)
