package com.gamehub.games.backgammon

import com.gamehub.shared.core.PlayerId

object BackgammonRules {
    fun getPlayerColor(state: BackgammonState, playerId: PlayerId): BackgammonColor {
        val index = state.players.indexOf(playerId)
        return if (index == 0) BackgammonColor.WHITE else BackgammonColor.BLACK
    }

    fun getColorPlayer(state: BackgammonState, color: BackgammonColor): PlayerId {
        val index = if (color == BackgammonColor.WHITE) 0 else 1
        return state.players[index]
    }

    fun allCheckersInHome(state: BackgammonState, color: BackgammonColor): Boolean {
        val barCount = if (color == BackgammonColor.WHITE) state.barWhite else state.barBlack
        if (barCount > 0) return false

        for (i in 1..24) {
            if (!isHomeBoard(color, i)) {
                val point = state.points[i]
                if (point.owner == color && point.checkers.isNotEmpty()) {
                    return false
                }
            }
        }
        return true
    }

    fun isHomeBoard(color: BackgammonColor, point: Int): Boolean {
        return when (color) {
            BackgammonColor.WHITE -> point in 19..24
            BackgammonColor.BLACK -> point in 1..6
        }
    }

    fun getValidMoves(
        state: BackgammonState,
        color: BackgammonColor,
        dice: List<Int>
    ): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val barCount = if (color == BackgammonColor.WHITE) state.barWhite else state.barBlack

        if (barCount > 0) {
            // Must enter from bar: white enters on 1-6 (Black home), black on 24-19 (White home)
            val homeStart = if (color == BackgammonColor.WHITE) 1 else 24
            val direction = if (color == BackgammonColor.WHITE) 1 else -1
            for (die in dice.distinct()) {
                val targetPoint = homeStart + (die - 1) * direction
                val point = state.points[targetPoint]
                if (!point.isBlocked || point.owner != color.opponent()) {
                    moves.add(Pair(0, targetPoint))
                }
            }
        } else {
            val direction = if (color == BackgammonColor.WHITE) 1 else -1
            val canBearOff = allCheckersInHome(state, color)

            for (from in 1..24) {
                val point = state.points[from]
                if (point.owner != color) continue

                for (die in dice.distinct()) {
                    val to = from + (die * direction)

                    if (to in 1..24) {
                        val targetPoint = state.points[to]
                        if (!targetPoint.isBlocked || targetPoint.owner != color.opponent()) {
                            moves.add(Pair(from, to))
                        }
                    } else if (canBearOff) {
                        if (to > 24 && color == BackgammonColor.WHITE) {
                            // White bears off when over 24
                            val highestOccupied = (19..24).lastOrNull { i ->
                                state.points[i].owner == BackgammonColor.WHITE && state.points[i].checkers.isNotEmpty()
                            } ?: continue
                            if (from == highestOccupied || (from + die) == to) {
                                moves.add(Pair(from, 25))
                            }
                        } else if (to < 1 && color == BackgammonColor.BLACK) {
                            // Black bears off when under 1
                            val highestOccupied = (1..6).firstOrNull { i ->
                                state.points[i].owner == BackgammonColor.BLACK && state.points[i].checkers.isNotEmpty()
                            } ?: continue
                            if (from == highestOccupied || (from - die) == to) {
                                moves.add(Pair(from, 25))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    fun applyMove(
        state: BackgammonState,
        color: BackgammonColor,
        from: Int,
        to: Int,
        die: Int
    ): BackgammonState {
        val newPoints = state.points.toMutableList()
        var newBarWhite = state.barWhite
        var newBarBlack = state.barBlack
        var newBorneOffWhite = state.borneOffWhite
        var newBorneOffBlack = state.borneOffBlack

        if (from == 0) {
            if (color == BackgammonColor.WHITE) newBarWhite-- else newBarBlack--
        } else {
            newPoints[from] = newPoints[from].removeLastChecker()
        }

        if (to == 25) {
            if (color == BackgammonColor.WHITE) newBorneOffWhite++ else newBorneOffBlack++
        } else {
            val targetPoint = newPoints[to]
            if (targetPoint.isBlot && targetPoint.owner == color.opponent()) {
                newPoints[to] = newPoints[to].clearCheckers()
                if (color == BackgammonColor.WHITE) newBarBlack++ else newBarWhite++
            }
            newPoints[to] = newPoints[to].addChecker(color)
        }

        return state.copy(
            points = newPoints,
            barWhite = newBarWhite,
            barBlack = newBarBlack,
            borneOffWhite = newBorneOffWhite,
            borneOffBlack = newBorneOffBlack
        )
    }
}

internal fun BackgammonColor.opponent(): BackgammonColor {
    return if (this == BackgammonColor.WHITE) BackgammonColor.BLACK else BackgammonColor.WHITE
}
