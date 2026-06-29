package com.gamehub.games.checkers

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult

class CheckersEngine : GameDefinition<CheckersState, CheckersAction, GameResult> {

    override val metadata = GameMetadata(
        id = "checkers",
        name = "چکرز",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک چکرز"
    )

    override fun createInitialState(players: List<PlayerId>): CheckersState {
        return CheckersState(
            players = players,
            currentPlayer = players.first(),
            board = createInitialBoard(),
            turn = CheckersColor.RED,
            lastPosition = null
        )
    }

    private fun createInitialBoard(): List<List<CheckersPiece?>> {
        val board = MutableList(8) { MutableList<CheckersPiece?>(8) { null } }
        for (row in 0 until 3) {
            for (col in 0 until 8) {
                if ((row + col) % 2 == 0) {
                    board[row][col] = CheckersPiece(CheckersPieceType.MAN, CheckersColor.RED)
                }
            }
        }
        for (row in 5 until 8) {
            for (col in 0 until 8) {
                if ((row + col) % 2 == 0) {
                    board[row][col] = CheckersPiece(CheckersPieceType.MAN, CheckersColor.WHITE)
                }
            }
        }
        return board
    }

    private fun Position.isValid(): Boolean = row in 0 until 8 && col in 0 until 8
    private fun Position.isDark(): Boolean = (row + col) % 2 == 0

    private fun getCapturePaths(
        state: CheckersState,
        color: CheckersColor,
        startPos: Position,
        visited: MutableSet<Position> = mutableSetOf()
    ): List<List<Position>> {
        val piece = state.board[startPos.row][startPos.col] ?: return emptyList()
        val paths = mutableListOf<List<Position>>()
        val directions = when (piece.type) {
            CheckersPieceType.MAN -> when (color) {
                CheckersColor.RED -> listOf(2 to -2, 2 to 2) // RED moves down
                CheckersColor.WHITE -> listOf(-2 to -2, -2 to 2) // WHITE moves up
            }
            CheckersPieceType.KING -> listOf(-2 to -2, -2 to 2, 2 to -2, 2 to 2)
        }

        for ((dr, dc) in directions) {
            val midRow = startPos.row + dr / 2
            val midCol = startPos.col + dc / 2
            val newRow = startPos.row + dr
            val newCol = startPos.col + dc
            val midPos = Position(midRow, midCol)
            val newPos = Position(newRow, newCol)

            if (newPos.isValid() && newPos.isDark()) {
                val midPiece = state.board[midRow][midCol]
                val targetPiece = state.board[newRow][newCol]

                if (midPiece != null && midPiece.color != color && targetPiece == null && !visited.contains(midPos)) {
                    val newVisited = (visited + midPos).toMutableSet()
                    val tempBoard = state.board.map { it.toMutableList() }
                    tempBoard[startPos.row][startPos.col] = null
                    tempBoard[midRow][midCol] = null

                    val promoted = if (piece.type == CheckersPieceType.MAN) {
                        when (color) {
                            CheckersColor.RED -> newRow == 7
                            CheckersColor.WHITE -> newRow == 0
                        }
                    } else false

                    tempBoard[newRow][newCol] = if (promoted) CheckersPiece(CheckersPieceType.KING, color) else piece

                    val furtherPaths = getCapturePaths(
                        state.copy(board = tempBoard),
                        color,
                        newPos,
                        newVisited
                    )

                    if (furtherPaths.isNotEmpty() && !promoted) {
                        furtherPaths.forEach { subPath ->
                            paths.add(listOf(startPos) + subPath)
                        }
                    } else {
                        paths.add(listOf(startPos, newPos))
                    }
                }
            }
        }
        return paths
    }

    private fun getAllCapturePaths(state: CheckersState, color: CheckersColor): List<List<Position>> {
        val allPaths = mutableListOf<List<Position>>()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val pos = Position(row, col)
                val piece = state.board[row][col]
                if (piece != null && piece.color == color) {
                    allPaths.addAll(getCapturePaths(state, color, pos))
                }
            }
        }
        val maxLength = allPaths.maxOfOrNull { it.size } ?: 0
        return allPaths.filter { it.size == maxLength }
    }

    private fun getSimpleMoves(state: CheckersState, color: CheckersColor): List<Pair<Position, Position>> {
        val moves = mutableListOf<Pair<Position, Position>>()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val pos = Position(row, col)
                val piece = state.board[row][col]
                if (piece != null && piece.color == color) {
                    val directions = when (piece.type) {
                        CheckersPieceType.MAN -> when (color) {
                            CheckersColor.RED -> listOf(1 to -1, 1 to 1) // RED moves down
                            CheckersColor.WHITE -> listOf(-1 to -1, -1 to 1) // WHITE moves up
                        }
                        CheckersPieceType.KING -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
                    }
                    for ((dr, dc) in directions) {
                        val newPos = Position(row + dr, col + dc)
                        if (newPos.isValid() && newPos.isDark() && state.board[newPos.row][newPos.col] == null) {
                            moves.add(pos to newPos)
                        }
                    }
                }
            }
        }
        return moves
    }

    override fun validateAction(state: CheckersState, action: CheckersAction, playerId: PlayerId): Boolean {
        val playerIndex = state.players.indexOf(playerId)
        val expectedColor = if (playerIndex == 0) CheckersColor.RED else CheckersColor.WHITE
        if (state.turn != expectedColor) return false
        if (state.currentPlayer != playerId) return false

        if (state.lastPosition != null) {
            if (action !is CheckersAction.Capture) return false
            val captures = getCapturePaths(state, expectedColor, state.lastPosition)
            return captures.any { it == action.path }
        }

        val allCaptures = getAllCapturePaths(state, expectedColor)
        if (allCaptures.isNotEmpty()) {
            if (action !is CheckersAction.Capture) return false
            return allCaptures.any { it == action.path }
        }

        if (action !is CheckersAction.Move) return false
        val simpleMoves = getSimpleMoves(state, expectedColor)
        return simpleMoves.contains(action.from to action.to)
    }

    override fun applyAction(state: CheckersState, action: CheckersAction, playerId: PlayerId): GameUpdateResult<CheckersState, GameResult> {
        val playerIndex = state.players.indexOf(playerId)
        val color = if (playerIndex == 0) CheckersColor.RED else CheckersColor.WHITE

        val newBoard = state.board.map { it.toMutableList() }
        var newCaptured = state.capturedPieces.toMutableMap()
        var newLastPosition: Position? = null
        var newTurn = state.turn
        var newCurrentPlayer = state.currentPlayer

        when (action) {
            is CheckersAction.Move -> {
                val piece = newBoard[action.from.row][action.from.col] ?: return GameUpdateResult(state)
                newBoard[action.from.row][action.from.col] = null

                val promoted = if (piece.type == CheckersPieceType.MAN) {
                    when (color) {
                        CheckersColor.RED -> action.to.row == 7
                        CheckersColor.WHITE -> action.to.row == 0
                    }
                } else false
                newBoard[action.to.row][action.to.col] = if (promoted) CheckersPiece(CheckersPieceType.KING, color) else piece

                newTurn = if (color == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED
                newCurrentPlayer = state.players[(playerIndex + 1) % state.players.size]
            }
            is CheckersAction.Capture -> {
                var currentPos = action.path.first()
                for (i in 1 until action.path.size) {
                    val nextPos = action.path[i]
                    val midRow = (currentPos.row + nextPos.row) / 2
                    val midCol = (currentPos.col + nextPos.col) / 2

                    val piece = newBoard[currentPos.row][currentPos.col] ?: continue
                    newBoard[currentPos.row][currentPos.col] = null

                    newCaptured[if (color == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED] =
                        (newCaptured[if (color == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED] ?: 0) + 1
                    newBoard[midRow][midCol] = null

                    val promoted = if (piece.type == CheckersPieceType.MAN) {
                        when (color) {
                            CheckersColor.RED -> nextPos.row == 7
                            CheckersColor.WHITE -> nextPos.row == 0
                        }
                    } else false

                    newBoard[nextPos.row][nextPos.col] = if (promoted) CheckersPiece(CheckersPieceType.KING, color) else piece
                    currentPos = nextPos
                }

                val furtherCaptures = getCapturePaths(
                    state.copy(board = newBoard),
                    color,
                    currentPos
                )
                if (furtherCaptures.isNotEmpty() && action.path.size == 2) {
                    newLastPosition = currentPos
                } else {
                    newTurn = if (color == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED
                    newCurrentPlayer = state.players[(playerIndex + 1) % state.players.size]
                }
            }
        }

        val intermediateState = state.copy(
            board = newBoard,
            turn = newTurn,
            currentPlayer = newCurrentPlayer,
            lastPosition = newLastPosition,
            capturedPieces = newCaptured,
            moveCount = state.moveCount + 1
        )

        val opponentColor = if (color == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED
        val hasLegalMoves = hasLegalMoves(intermediateState, opponentColor)
        val hasPieces = countPieces(intermediateState, opponentColor) > 0

        val result = when {
            !hasPieces -> GameResult.Win(state.players[playerIndex])
            !hasLegalMoves && newLastPosition == null -> GameResult.Win(state.players[playerIndex])
            else -> null
        }

        return GameUpdateResult(intermediateState, result)
    }

    private fun hasLegalMoves(state: CheckersState, color: CheckersColor): Boolean {
        val captures = getAllCapturePaths(state, color)
        if (captures.isNotEmpty()) return true
        val simpleMoves = getSimpleMoves(state, color)
        return simpleMoves.isNotEmpty()
    }

    private fun countPieces(state: CheckersState, color: CheckersColor): Int {
        var count = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                state.board[row][col]?.let {
                    if (it.color == color) count++
                }
            }
        }
        return count
    }

    override fun isTerminal(state: CheckersState): Boolean {
        val redPieces = countPieces(state, CheckersColor.RED)
        val whitePieces = countPieces(state, CheckersColor.WHITE)
        if (redPieces == 0 || whitePieces == 0) return true

        val nextColor = if (state.turn == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED
        if (!hasLegalMoves(state, nextColor) && state.lastPosition == null) return true

        return false
    }

    override fun getResult(state: CheckersState): GameResult? {
        val redPieces = countPieces(state, CheckersColor.RED)
        val whitePieces = countPieces(state, CheckersColor.WHITE)
        if (redPieces == 0) return GameResult.Win(state.players[1])
        if (whitePieces == 0) return GameResult.Win(state.players[0])

        val nextColor = if (state.turn == CheckersColor.RED) CheckersColor.WHITE else CheckersColor.RED
        if (!hasLegalMoves(state, nextColor) && state.lastPosition == null) {
            return GameResult.Win(state.players[if (state.turn == CheckersColor.RED) 1 else 0])
        }

        return null
    }

    fun getValidActions(state: CheckersState, playerId: PlayerId): List<CheckersAction> {
        val playerIndex = state.players.indexOf(playerId)
        val color = if (playerIndex == 0) CheckersColor.RED else CheckersColor.WHITE
        if (state.turn != color) return emptyList()

        if (state.lastPosition != null) {
            val captures = getCapturePaths(state, color, state.lastPosition)
            return captures.map { CheckersAction.Capture(it) }
        }

        val captures = getAllCapturePaths(state, color)
        if (captures.isNotEmpty()) {
            return captures.map { CheckersAction.Capture(it) }
        }

        val simpleMoves = getSimpleMoves(state, color)
        return simpleMoves.map { CheckersAction.Move(it.first, it.second) }
    }
}
