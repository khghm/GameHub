package com.gamehub.games.nard

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
enum class NardColor {
    WHITE,
    BLACK
}

@Serializable
data class Point(
    val index: Int, // 1-24
    val checkers: List<NardColor> = emptyList()
) {
    val isBlot: Boolean
        get() = checkers.size == 1
    val isBlocked: Boolean
        get() = checkers.size >= 2
    val owner: NardColor?
        get() = checkers.firstOrNull()

    fun addChecker(color: NardColor): Point {
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
enum class WinType {
    SINGLE,
    GAMMON,
    BACKGAMMON
}

@Serializable
data class NardState(
    val players: List<PlayerId>,
    val currentPlayer: PlayerId?,
    val turn: NardColor = NardColor.WHITE,
    val points: List<Point> = createInitialPoints(),
    val barWhite: Int = 0,
    val barBlack: Int = 0,
    val borneOffWhite: Int = 0,
    val borneOffBlack: Int = 0,
    val dice: List<Int> = emptyList(),
    val diceRolled: Boolean = false,
    val doublingCube: DoublingCube = DoublingCube(),
    val canOfferDouble: Boolean = true,
    val doubleOffered: Boolean = false,
    val doubleOfferedBy: PlayerId? = null,
    val gameOver: Boolean = false,
    val winType: WinType? = null,
    val matchScoreWhite: Int = 0,
    val matchScoreBlack: Int = 0,
    val matchTargetScore: Int = 0,
    val isCrawfordGame: Boolean = false,
    val crawfordRuleApplied: Boolean = false,
    val jacobyRuleActive: Boolean = true,
    val cubeHasBeenDoubled: Boolean = false
) : GameState() {
    companion object {
        fun createInitialPoints(): List<Point> {
            val points = MutableList(25) { idx -> Point(idx) } // 0 unused, 1-24

            // Standard Nard/Backgammon initial setup
            // White (moves from 24 to 1)
            points[24] = points[24].copy(checkers = List(2) { NardColor.WHITE })
            points[13] = points[13].copy(checkers = List(5) { NardColor.WHITE })
            points[8] = points[8].copy(checkers = List(3) { NardColor.WHITE })
            points[6] = points[6].copy(checkers = List(5) { NardColor.WHITE })
            
            // Black (moves from 1 to 24)
            points[1] = points[1].copy(checkers = List(2) { NardColor.BLACK })
            points[12] = points[12].copy(checkers = List(5) { NardColor.BLACK })
            points[17] = points[17].copy(checkers = List(3) { NardColor.BLACK })
            points[19] = points[19].copy(checkers = List(5) { NardColor.BLACK })

            return points
        }
    }

    fun getPlayerColor(playerId: PlayerId): NardColor {
        return if (players.indexOf(playerId) == 0) NardColor.WHITE else NardColor.BLACK
    }
}
