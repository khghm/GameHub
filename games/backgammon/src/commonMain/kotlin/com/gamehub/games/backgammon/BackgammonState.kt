package com.gamehub.games.backgammon

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable
import com.gamehub.shared.dice.DiceEngine
import kotlinx.serialization.Transient

@Serializable
enum class BackgammonColor {
    WHITE,
    BLACK
}

@Serializable
data class Point(
    val index: Int, // 1-24
    val checkers: List<BackgammonColor> = emptyList()
) {
    val isBlot: Boolean
        get() = checkers.size == 1
    val isBlocked: Boolean
        get() = checkers.size >= 2
    val owner: BackgammonColor?
        get() = checkers.firstOrNull()

    fun addChecker(color: BackgammonColor): Point {
        return copy(checkers = checkers + color)
    }

    fun removeLastChecker(): Point {
        return copy(checkers = checkers.dropLast(1))
    }

    fun clearCheckers(): Point {
        return copy(checkers = emptyList())
    }
}

@Serializable
data class DoublingCube(
    val value: Int = 1, // 1, 2, 4, 8, 16, 32, 64
    val owner: PlayerId? = null // null means center
)

@Serializable
data class BackgammonState(
    val players: List<PlayerId>,
    val currentPlayer: PlayerId?,
    val turn: BackgammonColor = BackgammonColor.WHITE,
    val points: List<Point> = createInitialPoints(),
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val borneOffWhite: Int = 0,
    val borneOffBlack: Int = 0,
    val dice: List<Int> = emptyList(),
    val diceRolled: Boolean = false,
    val doublingCube: DoublingCube = DoublingCube(),
    val canOfferDouble: Boolean = true,
    val gameOver: Boolean = false,
    @Transient
    val diceEngines: Map<PlayerId, DiceEngine> = emptyMap() // موتور تاس هر بازیکن
) : GameState() {
    companion object {
        fun createInitialPoints(): List<Point> {
            val points = MutableList(25) { idx -> Point(idx) } // 0 unused, 1-24

            // From user's image:
            points[24] = points[24].copy(checkers = List(2) { BackgammonColor.BLACK })
            points[13] = points[13].copy(checkers = List(5) { BackgammonColor.BLACK })
            points[8] = points[8].copy(checkers = List(3) { BackgammonColor.BLACK })
            points[6] = points[6].copy(checkers = List(5) { BackgammonColor.BLACK })
            points[1] = points[1].copy(checkers = List(2) { BackgammonColor.WHITE })
            points[19] = points[19].copy(checkers = List(5) { BackgammonColor.WHITE })
            points[17] = points[17].copy(checkers = List(3) { BackgammonColor.WHITE })
            points[12] = points[12].copy(checkers = List(5) { BackgammonColor.WHITE })

            return points
        }
    }

    fun getPlayerColor(playerId: PlayerId): BackgammonColor {
        return if (players.indexOf(playerId) == 0) BackgammonColor.WHITE else BackgammonColor.BLACK
    }
}
