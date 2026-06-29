package com.gamehub.games.abalone

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AbaloneEngine : GameDefinition<AbaloneState, AbaloneAction, GameResult> {

    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "abalone",
        name = "ابلون",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای"
    )

    override fun createInitialState(players: List<PlayerId>): AbaloneState {
        require(players.size == 2) { "Abalone requires exactly 2 players" }
        return AbaloneState.initial(players[0], players[1])
    }

    override fun validateAction(state: AbaloneState, action: AbaloneAction, playerId: PlayerId): Boolean {
        val currentColor = if (playerId == state.blackPlayerId) AbaloneColor.BLACK else AbaloneColor.WHITE
        if (state.currentPlayer != playerId) return false

        return when (action) {
            is AbaloneAction.Move -> {
                // Validate we own the marbles
                val selected = action.selectedMarbles.mapNotNull { state.getMarbleAt(it) }
                if (selected.size !in 1..3) return false
                if (selected.any { it.color != currentColor }) return false

                // Validate they are in a straight line
                if (!isValidMarbleGroup(selected)) return false

                // Now check if the move is possible
                val dir = action.direction
                val newPositions = action.selectedMarbles.map { it.neighbor(dir) }

                // Check for own marbles in the way
                val ownMarblesInWay = newPositions.any { newPos ->
                    state.getMarbleAt(newPos)?.let { it.color == currentColor && !action.selectedMarbles.contains(it.pos) } ?: false
                }
                if (ownMarblesInWay) return false

                // Check for opposing marbles and sumito condition
                val opposingMarbles = mutableListOf<AbaloneMarble>()
                for (selectedPos in action.selectedMarbles) {
                    val checkPos = selectedPos.neighbor(dir)
                    val marble = state.getMarbleAt(checkPos)
                    if (marble != null && marble.color != currentColor && !opposingMarbles.contains(marble)) {
                        opposingMarbles.add(marble)
                    }
                }
                if (opposingMarbles.isNotEmpty() && action.selectedMarbles.size <= opposingMarbles.size) {
                    return false
                }

                // All checks passed!
                true
            }
        }
    }

    private fun isValidMarbleGroup(marbles: List<AbaloneMarble>): Boolean {
        if (marbles.size == 1) return true
        val sorted = marbles.sortedWith(compareBy({ it.pos.q }, { it.pos.r }))
        val dirs = AbaloneDirection.entries
        for (dir in dirs) {
            var valid = true
            for (i in 0 until sorted.size - 1) {
                val next = sorted[i].pos.neighbor(dir)
                if (next != sorted[i + 1].pos) {
                    valid = false
                    break
                }
            }
            if (valid) return true
        }
        return false
    }

    override fun applyAction(state: AbaloneState, action: AbaloneAction, playerId: PlayerId): GameUpdateResult<AbaloneState, GameResult> {
        if (!validateAction(state, action, playerId)) {
            return GameUpdateResult(state)
        }

        val newState = when (action) {
            is AbaloneAction.Move -> applyMove(state, action)
        }

        // Check for win condition
        val result = when {
            newState.capturedWhite >= 6 -> GameResult.Win(newState.blackPlayerId)
            newState.capturedBlack >= 6 -> GameResult.Win(newState.whitePlayerId)
            else -> null
        }

        return GameUpdateResult(newState, result)
    }

    private fun applyMove(state: AbaloneState, action: AbaloneAction.Move): AbaloneState {
        val currentColor = if (state.currentPlayer == state.blackPlayerId) AbaloneColor.BLACK else AbaloneColor.WHITE
        val nextPlayer = if (state.currentPlayer == state.blackPlayerId) state.whitePlayerId else state.blackPlayerId

        val selectedPositions = action.selectedMarbles
        val dir = action.direction
        val newPositions = selectedPositions.map { it.neighbor(dir) }

        // Calculate new marbles
        val newMarbles = state.marbles.toMutableList()
        var newCapturedBlack = state.capturedBlack
        var newCapturedWhite = state.capturedWhite

        // First, let's check if any of the new positions are occupied by OUR own marbles (that we are NOT moving)
        val ourMarblesNotMoving = newMarbles.filter { it.color == currentColor && !selectedPositions.contains(it.pos) }
        val ownMarbleInWay = ourMarblesNotMoving.any { newPositions.contains(it.pos) }
        if (ownMarbleInWay) {
            return state.copy(currentPlayer = nextPlayer)
        }

        // Check for sumito - opposing marbles in the way
        val opposingMarbles = mutableListOf<AbaloneMarble>()
        // Check ALL the new positions (not just first selected) for opposing marbles
        for (selectedPos in selectedPositions) {
            val checkPos = selectedPos.neighbor(dir)
            val marble = state.getMarbleAt(checkPos)
            if (marble != null && marble.color != currentColor && !opposingMarbles.contains(marble)) {
                opposingMarbles.add(marble)
            }
        }
        // Now sort opposing marbles in the direction we're pushing
        opposingMarbles.sortWith(compareBy({ it.pos.q }, { it.pos.r }))

        if (opposingMarbles.isNotEmpty()) {
            // Check sumito condition: our group must be LARGER than opposing group
            if (selectedPositions.size <= opposingMarbles.size) {
                // Invalid sumito, return original state
                return state.copy(currentPlayer = nextPlayer)
            }

            // Push opposing marbles
            for (i in opposingMarbles.indices) {
                val marble = opposingMarbles[i]
                val pushPos = marble.pos.neighbor(dir)
                newMarbles.remove(marble)
                if (AbalonePos.isValid(pushPos.q, pushPos.r)) {
                    newMarbles.add(marble.copy(pos = pushPos))
                } else {
                    // Captured!
                    if (marble.color == AbaloneColor.BLACK) {
                        newCapturedWhite++
                    } else {
                        newCapturedBlack++
                    }
                }
            }
        }

        // Now move our marbles
        val marblesToMove = selectedPositions.mapNotNull { state.getMarbleAt(it) }
        marblesToMove.forEach { newMarbles.remove(it) }
        marblesToMove.zip(newPositions).forEach { (marble, newPos) ->
            if (AbalonePos.isValid(newPos.q, newPos.r)) {
                newMarbles.add(marble.copy(pos = newPos))
            }
        }

        return state.copy(
            marbles = newMarbles,
            capturedBlack = newCapturedBlack,
            capturedWhite = newCapturedWhite,
            currentPlayer = nextPlayer,
            selectedMarbles = emptyList()
        )
    }

    override fun isTerminal(state: AbaloneState): Boolean {
        return state.capturedWhite >= 6 || state.capturedBlack >= 6
    }

    override fun getResult(state: AbaloneState): GameResult? {
        return when {
            state.capturedWhite >= 6 -> GameResult.Win(state.blackPlayerId)
            state.capturedBlack >= 6 -> GameResult.Win(state.whitePlayerId)
            else -> null
        }
    }

    override fun getPlayers(state: AbaloneState): List<PlayerId> {
        return listOf(state.blackPlayerId, state.whitePlayerId)
    }

    override fun setCurrentPlayer(state: AbaloneState, playerId: PlayerId): AbaloneState {
        return state.copy(currentPlayer = playerId)
    }
}
