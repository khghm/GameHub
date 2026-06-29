package com.gamehub.games.othello

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult

class OthelloEngine : GameDefinition<OthelloState, OthelloAction, GameResult> {

    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "othello",
        name = "اتللو",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی استراتژیک تخته‌ای"
    )

    private val directions = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1, 0 to 1,
        1 to -1, 1 to 0, 1 to 1
    )

    override fun createInitialState(players: List<PlayerId>): OthelloState {
        val fullPlayers = if (players.size < 2) {
            players + PlayerId("BOT_1")
        } else players
        return OthelloState.initial(fullPlayers)
    }

    override fun validateAction(state: OthelloState, action: OthelloAction, playerId: PlayerId): Boolean {
        if (state.currentPlayer != playerId || state.gameOver) return false
        if (action !is OthelloAction.Move) return false

        return isValidMove(state, action.row, action.col, playerId)
    }

    private fun isValidMove(state: OthelloState, row: Int, col: Int, playerId: PlayerId): Boolean {
        if (row !in 0..7 || col !in 0..7) return false
        if (state.board[row][col] != OthelloPiece.EMPTY) return false
        val piece = state.getPlayerPiece(playerId)
        return getFlips(state, row, col, piece).isNotEmpty()
    }

    private fun getFlips(state: OthelloState, row: Int, col: Int, piece: OthelloPiece): List<Pair<Int, Int>> {
        val opponent = if (piece == OthelloPiece.BLACK) OthelloPiece.WHITE else OthelloPiece.BLACK
        val flips = mutableListOf<Pair<Int, Int>>()

        for ((dr, dc) in directions) {
            val currentFlips = mutableListOf<Pair<Int, Int>>()
            var r = row + dr
            var c = col + dc

            while (r in 0..7 && c in 0..7) {
                val currentPiece = state.board[r][c]
                when {
                    currentPiece == opponent -> currentFlips.add(r to c)
                    currentPiece == piece -> {
                        flips.addAll(currentFlips)
                        break
                    }
                    else -> break
                }
                r += dr
                c += dc
            }
        }
        return flips
    }

    override fun applyAction(state: OthelloState, action: OthelloAction, playerId: PlayerId): GameUpdateResult<OthelloState, GameResult> {
        if (!validateAction(state, action, playerId)) return GameUpdateResult(state)

        if (action !is OthelloAction.Move) return GameUpdateResult(state)

        val newBoard = state.board.map { it.toMutableList() }
        val piece = state.getPlayerPiece(playerId)
        val flips = getFlips(state, action.row, action.col, piece)

        newBoard[action.row][action.col] = piece
        for ((r, c) in flips) {
            newBoard[r][c] = piece
        }

        val nextPlayer = state.players.firstOrNull { it != playerId } ?: playerId
        var newBlackCount = 0
        var newWhiteCount = 0
        for (r in 0..7) {
            for (c in 0..7) {
                when (newBoard[r][c]) {
                    OthelloPiece.BLACK -> newBlackCount++
                    OthelloPiece.WHITE -> newWhiteCount++
                    else -> {}
                }
            }
        }

        val hasValidMoveNext = hasValidMoves(OthelloState(
            state.players,
            newBoard.map { it.toList() },
            nextPlayer,
            0,
            newBlackCount,
            newWhiteCount,
            false
        ), nextPlayer)

        val newConsecutivePasses = if (hasValidMoveNext) 0 else state.consecutivePasses + 1
        val newGameOver = newConsecutivePasses >= 2 || (newBlackCount + newWhiteCount == 64)

        val newState = OthelloState(
            state.players,
            newBoard.map { it.toList() },
            if (newGameOver) null else if (hasValidMoveNext) nextPlayer else nextPlayer,
            if (newGameOver) 0 else newConsecutivePasses,
            newBlackCount,
            newWhiteCount,
            newGameOver
        )

        return GameUpdateResult(newState, getResult(newState))
    }

    private fun hasValidMoves(state: OthelloState, playerId: PlayerId): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                if (isValidMove(state, r, c, playerId)) return true
            }
        }
        return false
    }

    override fun isTerminal(state: OthelloState): Boolean = state.gameOver

    override fun getResult(state: OthelloState): GameResult? {
        if (!state.gameOver) return null
        return when {
            state.blackCount > state.whiteCount -> GameResult.Win(state.players[0])
            state.whiteCount > state.blackCount -> GameResult.Win(state.players[1])
            else -> GameResult.Draw
        }
    }
}
