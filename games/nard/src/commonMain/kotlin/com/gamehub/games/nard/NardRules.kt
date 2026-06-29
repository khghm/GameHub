package com.gamehub.games.nard

import com.gamehub.shared.core.PlayerId

object NardRules {
    fun getPlayerColor(state: NardState, playerId: PlayerId): NardColor {
        val index = state.players.indexOf(playerId)
        return if (index == 0) NardColor.WHITE else NardColor.BLACK
    }

    fun getColorPlayer(state: NardState, color: NardColor): PlayerId {
        val index = if (color == NardColor.WHITE) 0 else 1
        return state.players[index]
    }

    fun allCheckersInHome(state: NardState, color: NardColor): Boolean {
        val barCount = if (color == NardColor.WHITE) state.barWhite else state.barBlack
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

    fun isHomeBoard(color: NardColor, point: Int): Boolean {
        return when (color) {
            NardColor.WHITE -> point in 19..24
            NardColor.BLACK -> point in 1..6
        }
    }

    fun hasCheckersOnBar(state: NardState, color: NardColor): Boolean {
        return if (color == NardColor.WHITE) state.barWhite > 0 else state.barBlack > 0
    }

    fun getWinType(state: NardState, winnerColor: NardColor): WinType {
        val loserColor = winnerColor.opponent()
        val loserBorneOff = if (loserColor == NardColor.WHITE) state.borneOffWhite else state.borneOffBlack
        val loserOnBar = if (loserColor == NardColor.WHITE) state.barWhite > 0 else state.barBlack > 0
        
        val loserInWinnerHome = (1..24).any { pointIndex ->
            val point = state.points[pointIndex]
            point.owner == loserColor && point.checkers.isNotEmpty() && isHomeBoard(winnerColor, pointIndex)
        }

        // Apply Jacoby Rule: Gammon and Backgammon only count if cube has been doubled
        if (state.jacobyRuleActive && !state.cubeHasBeenDoubled) {
            return WinType.SINGLE
        }

        return when {
            loserBorneOff == 0 && (loserOnBar || loserInWinnerHome) -> WinType.BACKGAMMON
            loserBorneOff == 0 -> WinType.GAMMON
            else -> WinType.SINGLE
        }
    }

    fun getValidMoves(
        state: NardState,
        color: NardColor,
        dice: List<Int>
    ): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val barCount = if (color == NardColor.WHITE) state.barWhite else state.barBlack

        if (barCount > 0) {
            // Must enter from bar: white enters on 1-6 (Black's home), black on 24-19 (White's home)
            val homeStart = if (color == NardColor.WHITE) 1 else 24
            val direction = if (color == NardColor.WHITE) 1 else -1
            for (die in dice.distinct()) {
                val targetPoint = homeStart + (die - 1) * direction
                val point = state.points[targetPoint]
                if (!point.isBlocked || point.owner != color.opponent()) {
                    moves.add(Pair(0, targetPoint))
                }
            }
        } else {
            val direction = if (color == NardColor.WHITE) 1 else -1
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
                        if (color == NardColor.WHITE && to > 24) {
                            // White bears off when over 24
                            if (isValidBearOff(state, color, from, die)) {
                                moves.add(Pair(from, 25))
                            }
                        } else if (color == NardColor.BLACK && to < 1) {
                            // Black bears off when under 1
                            if (isValidBearOff(state, color, from, die)) {
                                moves.add(Pair(from, 25))
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun isValidBearOff(state: NardState, color: NardColor, from: Int, die: Int): Boolean {
        val homeRange = if (color == NardColor.WHITE) 19..24 else 1..6
        val highestOccupied = homeRange.lastOrNull { i ->
            state.points[i].owner == color && state.points[i].checkers.isNotEmpty()
        } ?: return false

        val exactMatch = if (color == NardColor.WHITE) {
            (from + die) == 25
        } else {
            (from - die) == 0
        }

        // Can bear off from exact point or from highest point if die is larger
        return exactMatch || (from == highestOccupied && 
            ((color == NardColor.WHITE && die > (25 - from)) || 
             (color == NardColor.BLACK && die > from)))
    }

    fun applyMove(
        state: NardState,
        color: NardColor,
        from: Int,
        to: Int,
        die: Int
    ): NardState {
        val newPoints = state.points.toMutableList()
        var newBarWhite = state.barWhite
        var newBarBlack = state.barBlack
        var newBorneOffWhite = state.borneOffWhite
        var newBorneOffBlack = state.borneOffBlack

        if (from == 0) {
            if (color == NardColor.WHITE) newBarWhite-- else newBarBlack--
        } else {
            newPoints[from] = newPoints[from].removeLastChecker()
        }

        if (to == 25) {
            if (color == NardColor.WHITE) newBorneOffWhite++ else newBorneOffBlack++
        } else {
            val targetPoint = newPoints[to]
            if (targetPoint.isBlot && targetPoint.owner == color.opponent()) {
                newPoints[to] = newPoints[to].clearCheckers()
                if (color == NardColor.WHITE) newBarBlack++ else newBarWhite++
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

    // Get all possible move sequences that use maximum number of dice
    fun getAllValidMoveSequences(
        state: NardState,
        color: NardColor,
        dice: List<Int>
    ): List<List<NardAction.Move>> {
        val sequences = mutableListOf<List<NardAction.Move>>()
        val sortedDice = dice.sortedDescending()
        
        generateMoveSequences(state, color, sortedDice, emptyList(), sequences)
        
        if (sequences.isEmpty()) return emptyList()
        
        // Find sequences with maximum number of moves
        val maxMoves = sequences.maxOfOrNull { it.size } ?: 0
        return sequences.filter { it.size == maxMoves }
    }

    private fun generateMoveSequences(
        state: NardState,
        color: NardColor,
        remainingDice: List<Int>,
        currentSequence: List<NardAction.Move>,
        result: MutableList<List<NardAction.Move>>
    ) {
        if (remainingDice.isEmpty()) {
            result.add(currentSequence)
            return
        }

        val usedDice = mutableSetOf<Int>()
        for ((index, die) in remainingDice.withIndex()) {
            if (die in usedDice) continue
            usedDice.add(die)

            val validMoves = getValidMoves(state, color, listOf(die))
            if (validMoves.isEmpty()) {
                // Can't use this die, try skipping it
                val newRemaining = remainingDice.toMutableList()
                newRemaining.removeAt(index)
                generateMoveSequences(state, color, newRemaining, currentSequence, result)
            } else {
                for ((from, to) in validMoves) {
                    val newState = applyMove(state, color, from, to, die)
                    val newSequence = currentSequence + NardAction.Move(from, to, die)
                    val newRemaining = remainingDice.toMutableList()
                    newRemaining.removeAt(index)
                    generateMoveSequences(newState, color, newRemaining, newSequence, result)
                }
            }
        }
    }

    // Check if a move sequence is valid (part of a maximum move sequence)
    fun isMoveSequenceValid(
        state: NardState,
        color: NardColor,
        dice: List<Int>,
        moves: List<NardAction.Move>
    ): Boolean {
        val validSequences = getAllValidMoveSequences(state, color, dice)
        return validSequences.any { seq ->
            moves.size <= seq.size && 
            moves.zip(seq.take(moves.size)).all { (m1, m2) -> 
                m1.from == m2.from && m1.to == m2.to && m1.die == m2.die 
            }
        }
    }
}

internal fun NardColor.opponent(): NardColor {
    return if (this == NardColor.WHITE) NardColor.BLACK else NardColor.WHITE
}
