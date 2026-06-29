package com.gamehub.games.blokus

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult

class BlokusEngine : GameDefinition<BlokusState, BlokusAction, GameResult> {

    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "blokus",
        name = "بلوکِس",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژیک پلیومینو"
    )

    override fun createInitialState(players: List<PlayerId>): BlokusState = BlokusState.initial(players)

    override fun validateAction(state: BlokusState, action: BlokusAction, playerId: PlayerId): Boolean {
        println("?? [Blokus] validateAction called, currentPlayer: ${state.currentPlayer}, incoming playerId: $playerId, action: $action")
        if (state.currentPlayer != playerId || state.gameOver) {
            println("?? [Blokus] Invalid: not current player or game over")
            return false
        }
        val playerData = state.playerData[playerId] ?: run {
            println("?? [Blokus] Invalid: no player data")
            return false
        }

        if (action is BlokusAction.Pass) {
            val hasMoves = hasValidMoves(state, playerId)
            println("?? [Blokus] Pass request: hasValidMoves = $hasMoves")
            return !hasMoves
        }

        if (action !is BlokusAction.Place) {
            println("?? [Blokus] Invalid: not place action")
            return false
        }

        val valid = isValidMove(state, playerId, action.pieceId, action.rotation, action.anchorRow, action.anchorCol)
        println("?? [Blokus] isValidMove for $action: $valid")
        return valid
    }

    fun isValidMove(
        state: BlokusState,
        playerId: PlayerId,
        pieceId: Int,
        rotation: Int,
        anchorRow: Int,
        anchorCol: Int
    ): Boolean {
        println("?? [Blokus] isValidMove: playerId=$playerId, pieceId=$pieceId, rot=$rotation, anchor=($anchorRow,$anchorCol)")
        val playerData = state.playerData[playerId] ?: run {
            println("?? [Blokus] isValidMove: no player data")
            return false
        }

        if (pieceId !in playerData.remainingPieces) {
            println("?? [Blokus] isValidMove: piece $pieceId not in remaining ${playerData.remainingPieces}")
            return false
        }

        val piece = BlokusPieces.firstOrNull { it.id == pieceId } ?: run {
            println("?? [Blokus] isValidMove: no piece $pieceId")
            return false
        }

        val safeRotation = rotation % piece.shapes.size
        val shape = piece.shapes[safeRotation]
        println("?? [Blokus] isValidMove: piece $pieceId has shapes.size=${piece.shapes.size}, safeRot=$safeRotation, shape=$shape")

        // Step 1: Check all cells are on board and empty
        val cells = shape.map { (dr, dc) -> (anchorRow + dr) to (anchorCol + dc) }
        println("?? [Blokus] isValidMove: cells=$cells")
        for ((r, c) in cells) {
            if (r !in 0..19 || c !in 0..19) {
                println("?? [Blokus] isValidMove: cell ($r,$c) out of bounds")
                return false
            }
            if (state.board[r][c] != BlokusColor.EMPTY) {
                println("?? [Blokus] isValidMove: cell ($r,$c) not empty (${state.board[r][c]})")
                return false
            }
        }

        // Step 2: Check first move rules
        val color = playerData.color

        if (!playerData.hasMadeFirstMove) {
            val startCorner = BlokusStartingCorners[color] ?: run {
                println("?? [Blokus] isValidMove: no start corner for color $color")
                return false
            }
            println("?? [Blokus] isValidMove: first move, startCorner=$startCorner")
            if (cells.none { it == startCorner }) {
                println("?? [Blokus] isValidMove: first move doesn't cover start corner")
                return false
            }
            println("?? [Blokus] isValidMove: first move valid!")
            return true
        }

        // Step3: Check subsequent move rules
        var hasCornerContact = false
        for ((r, c) in cells) {
            // Check for corner contact (diagonal neighbors same color)
            val diagonalNeighbors = listOf(
                (r - 1 to c - 1), (r - 1 to c + 1),
                (r + 1 to c - 1), (r + 1 to c + 1)
            )
            for ((nr, nc) in diagonalNeighbors) {
                if (nr in 0..19 && nc in 0..19 && state.board[nr][nc] == color) {
                    hasCornerContact = true
                }
            }

            // Check for edge contact (same color) (not allowed)
            val edgeNeighbors = listOf(
                (r - 1 to c), (r + 1 to c),
                (r to c - 1), (r to c + 1)
            )
            for ((nr, nc) in edgeNeighbors) {
                if (nr in 0..19 && nc in 0..19 && state.board[nr][nc] == color) {
                    println("?? [Blokus] isValidMove: edge contact with same color at ($nr,$nc)")
                    return false
                }
            }
        }

        println("?? [Blokus] isValidMove: hasCornerContact=$hasCornerContact")
        return hasCornerContact
    }

    fun hasValidMoves(state: BlokusState, playerId: PlayerId): Boolean {
        val playerData = state.playerData[playerId] ?: return false
        println("?? [Blokus] hasValidMoves for player: $playerId")

        for (pieceId in playerData.remainingPieces) {
            val piece = BlokusPieces.firstOrNull { it.id == pieceId } ?: continue
            for (rotation in piece.shapes.indices) {
                for (r in 0..19) {
                    for (c in 0..19) {
                        if (isValidMove(state, playerId, pieceId, rotation, r, c)) {
                            println("?? [Blokus] hasValidMoves FOUND valid move at ($r,$c) for piece $pieceId")
                            return true
                        }
                    }
                }
            }
        }
        println("?? [Blokus] hasValidMoves: NO valid moves found")
        return false
    }

    override fun applyAction(
        state: BlokusState,
        action: BlokusAction,
        playerId: PlayerId
    ): GameUpdateResult<BlokusState, GameResult> {
        if (!validateAction(state, action, playerId)) return GameUpdateResult(state)

        val playerData = state.playerData[playerId] ?: return GameUpdateResult(state)
        val newPlayerData: BlokusPlayerData

        val newBoard = state.board.map { it.toMutableList() }
        var newConsecutivePasses = 0
        var newLastPieceWasMonomino = false

        if (action is BlokusAction.Place) {
            val piece = BlokusPieces.first { it.id == action.pieceId }
            val safeRotation = action.rotation % piece.shapes.size
            val shape = piece.shapes[safeRotation]
            val cells = shape.map { (dr, dc) -> (action.anchorRow + dr) to (action.anchorCol + dc) }

            cells.forEach { (r, c) ->
                newBoard[r][c] = playerData.color
            }

            newPlayerData = playerData.copy(
                remainingPieces = playerData.remainingPieces - action.pieceId,
                hasMadeFirstMove = true
            )

            newLastPieceWasMonomino = action.pieceId == 1
        } else {
            // Pass
            newPlayerData = playerData
            newConsecutivePasses = state.consecutivePasses + 1
        }

        val newPlayerDataMap = state.playerData.toMutableMap()
        newPlayerDataMap[playerId] = newPlayerData

        var nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
        val nextPlayer = state.players[nextPlayerIndex]

        val newState = state.copy(
            board = newBoard.map { it.toList() },
            currentPlayerIndex = nextPlayerIndex,
            playerData = newPlayerDataMap,
            consecutivePasses = newConsecutivePasses,
            gameOver = newConsecutivePasses >= state.players.size,
            lastPieceWasMonomino = newLastPieceWasMonomino
        )

        return GameUpdateResult(newState, getResult(newState))
    }

    override fun isTerminal(state: BlokusState): Boolean = state.gameOver

    override fun getResult(state: BlokusState): GameResult? {
        if (!state.gameOver) return null

        // Calculate scores!
        val scores = state.players.map { player ->
            val data = state.playerData[player] ?: return@map player to 0
            val rawScore = data.remainingPieces.sumOf { id -> BlokusPieces.first { it.id == id }.size }
            var finalScore = rawScore
            // Bonuses!
            val placedAll21 = data.remainingPieces.isEmpty()
            if (placedAll21) {
                finalScore -= 15
                if (state.lastPieceWasMonomino) {
                    finalScore -= 5 // total -20
                }
            }
            player to finalScore
        }.toMap()

        // Find the winner (lowest score wins!)
        val sortedPlayers = state.players.sortedWith(
            compareBy({ scores[it] },
            { state.playerData[it]?.remainingPieces?.size })
        )

        return GameResult.Win(sortedPlayers.first())
    }
}
