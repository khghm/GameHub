package com.gamehub.games.abalone

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
enum class AbaloneColor {
    BLACK, WHITE
}

@Serializable
data class AbalonePos(
    val q: Int,
    val r: Int
) {
    val s: Int get() = -q - r

    fun neighbor(dir: AbaloneDirection): AbalonePos {
        return AbalonePos(q + dir.dq, r + dir.dr)
    }

    companion object {
        fun isValid(q: Int, r: Int): Boolean {
            val s = -q - r
            return listOf(q, r, s).all { it in -4..4 }
        }
    }
}

@Serializable
enum class AbaloneDirection(val dq: Int, val dr: Int) {
    RIGHT(1, 0),
    DOWN_RIGHT(0, 1),
    DOWN_LEFT(-1, 1),
    LEFT(-1, 0),
    UP_LEFT(0, -1),
    UP_RIGHT(1, -1)
}

@Serializable
data class AbaloneMarble(
    val pos: AbalonePos,
    val color: AbaloneColor
)

@Serializable
data class AbaloneState(
    val marbles: List<AbaloneMarble>,
    val capturedBlack: Int = 0,
    val capturedWhite: Int = 0,
    val blackPlayerId: PlayerId,
    val whitePlayerId: PlayerId,
    val currentPlayer: PlayerId?,
    val selectedMarbles: List<AbalonePos> = emptyList()
) : GameState() {

    companion object {
        fun initial(blackPlayerId: PlayerId, whitePlayerId: PlayerId): AbaloneState {
            val initialMarbles = mutableListOf<AbaloneMarble>()

            // Black marbles (bottom of the board)
            val blackPositions = listOf(
                // Row r = 3
                AbalonePos(-1, 3), AbalonePos(0, 3), AbalonePos(1, 3), AbalonePos(2, 3), AbalonePos(3, 3),
                // Row r = 2
                AbalonePos(-2, 2), AbalonePos(-1, 2), AbalonePos(0, 2), AbalonePos(1, 2), AbalonePos(2, 2), AbalonePos(3, 2),
                // Row r = 1 (center 3)
                AbalonePos(-1, 1), AbalonePos(0, 1), AbalonePos(1, 1)
            )
            blackPositions.forEach { pos ->
                initialMarbles.add(AbaloneMarble(pos, AbaloneColor.BLACK))
            }

            // White marbles (top of the board)
            val whitePositions = listOf(
                // Row r = 3 (top)
                AbalonePos(-3, -3), AbalonePos(-2, -3), AbalonePos(-1, -3), AbalonePos(0, -3), AbalonePos(1, -3),
                // Row r = 2
                AbalonePos(-3, -2), AbalonePos(-2, -2), AbalonePos(-1, -2), AbalonePos(0, -2), AbalonePos(1, -2), AbalonePos(2, -2),
                // Row r = 1 (center 3)
                AbalonePos(-1, -1), AbalonePos(0, -1), AbalonePos(1, -1)
            )
            whitePositions.forEach { pos ->
                initialMarbles.add(AbaloneMarble(pos, AbaloneColor.WHITE))
            }

            return AbaloneState(
                marbles = initialMarbles,
                blackPlayerId = blackPlayerId,
                whitePlayerId = whitePlayerId,
                currentPlayer = blackPlayerId
            )
        }
    }

    fun getMarbleAt(pos: AbalonePos): AbaloneMarble? = marbles.find { it.pos == pos }

    fun isEmpty(pos: AbalonePos): Boolean = getMarbleAt(pos) == null && AbalonePos.isValid(pos.q, pos.r)
}
