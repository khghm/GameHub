package com.gamehub.games.chess

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class ChessBotStrategy : BotStrategy<ChessState, ChessAction> {
    override val gameId: String = "chess"
    override val supportedDifficultyLevels: IntRange = 1..10

    // Piece values (in centipawns)
    private val pieceValues = mapOf(
        ChessPieceType.PAWN to 100,
        ChessPieceType.KNIGHT to 320,
        ChessPieceType.BISHOP to 330,
        ChessPieceType.ROOK to 500,
        ChessPieceType.QUEEN to 900,
        ChessPieceType.KING to 20000
    )

    // Position bonus tables (from Chess Programming Wiki)
    private val pawnTable = listOf(
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
        listOf(50, 50, 50, 50, 50, 50, 50, 50),
        listOf(10, 10, 20, 30, 30, 20, 10, 10),
        listOf(5, 5, 10, 25, 25, 10, 5, 5),
        listOf(0, 0, 0, 20, 20, 0, 0, 0),
        listOf(5, -5, -10, 0, 0, -10, -5, 5),
        listOf(5, 10, 10, -20, -20, 10, 10, 5),
        listOf(0, 0, 0, 0, 0, 0, 0, 0)
    )

    private val knightTable = listOf(
        listOf(-50, -40, -30, -30, -30, -30, -40, -50),
        listOf(-40, -20, 0, 0, 0, 0, -20, -40),
        listOf(-30, 0, 10, 15, 15, 10, 0, -30),
        listOf(-30, 5, 15, 20, 20, 15, 5, -30),
        listOf(-30, 0, 15, 20, 20, 15, 0, -30),
        listOf(-30, 5, 10, 15, 15, 10, 5, -30),
        listOf(-40, -20, 0, 5, 5, 0, -20, -40),
        listOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val bishopTable = listOf(
        listOf(-20, -10, -10, -10, -10, -10, -10, -20),
        listOf(-10, 0, 0, 0, 0, 0, 0, -10),
        listOf(-10, 0, 5, 10, 10, 5, 0, -10),
        listOf(-10, 5, 5, 10, 10, 5, 5, -10),
        listOf(-10, 0, 10, 10, 10, 10, 0, -10),
        listOf(-10, 10, 10, 10, 10, 10, 10, -10),
        listOf(-10, 5, 0, 0, 0, 0, 5, -10),
        listOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val rookTable = listOf(
        listOf(0, 0, 0, 0, 0, 0, 0, 0),
        listOf(5, 10, 10, 10, 10, 10, 10, 5),
        listOf(-5, 0, 0, 0, 0, 0, 0, -5),
        listOf(-5, 0, 0, 0, 0, 0, 0, -5),
        listOf(-5, 0, 0, 0, 0, 0, 0, -5),
        listOf(-5, 0, 0, 0, 0, 0, 0, -5),
        listOf(-5, 0, 0, 0, 0, 0, 0, -5),
        listOf(0, 0, 0, 5, 5, 0, 0, 0)
    )

    private val queenTable = listOf(
        listOf(-20, -10, -10, -5, -5, -10, -10, -20),
        listOf(-10, 0, 0, 0, 0, 0, 0, -10),
        listOf(-10, 0, 5, 5, 5, 5, 0, -10),
        listOf(-5, 0, 5, 5, 5, 5, 0, -5),
        listOf(0, 0, 5, 5, 5, 5, 0, -5),
        listOf(-10, 5, 5, 5, 5, 5, 0, -10),
        listOf(-10, 0, 5, 0, 0, 0, 0, -10),
        listOf(-20, -10, -10, -5, -5, -10, -10, -20)
    )

    private val kingTable = listOf(
        listOf(-30, -40, -40, -50, -50, -40, -40, -30),
        listOf(-30, -40, -40, -50, -50, -40, -40, -30),
        listOf(-30, -40, -40, -50, -50, -40, -40, -30),
        listOf(-30, -40, -40, -50, -50, -40, -40, -30),
        listOf(-20, -30, -30, -40, -40, -30, -30, -20),
        listOf(-10, -20, -20, -20, -20, -20, -20, -10),
        listOf(20, 20, 0, 0, 0, 0, 20, 20),
        listOf(20, 30, 10, 0, 0, 10, 30, 20)
    )

    private val engine = ChessEngine()

    override suspend fun getNextMove(
        state: ChessState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): ChessAction? {
        val validMoves = engine.getValidMoves(state, botPlayerId)
        if (validMoves.isEmpty()) return null

        // Delay based on difficulty
        val delayMs = when {
            difficultyLevel <= 3 -> 500L
            difficultyLevel <= 6 -> 1000L
            else -> 1500L
        }
        delay(delayMs)

        val botColor = if (state.players.indexOf(botPlayerId) == 0) ChessColor.WHITE else ChessColor.BLACK
        val depth = when {
            difficultyLevel <= 2 -> 1
            difficultyLevel <= 4 -> 2
            difficultyLevel <= 6 -> 3
            difficultyLevel <= 8 -> 4
            else -> 5
        }

        return when (difficultyLevel) {
            in 1..2 -> {
                // Level 1-2: Mostly random, sometimes capture
                val captureMoves = validMoves.filter { isCapture(state, it) }
                if (captureMoves.isNotEmpty() && Random.nextFloat() > 0.5) {
                    captureMoves.random()
                } else {
                    validMoves.random()
                }
            }
            in 3..10 -> {
                // Level 3-10: Minimax with alpha-beta pruning
                var bestMove: ChessAction.Move? = null
                var bestScore = Int.MIN_VALUE

                // Sort moves for better pruning
                val sortedMoves = validMoves.sortedWith(compareByDescending<ChessAction.Move> { 
                    if (isCapture(state, it)) pieceValues[state.board[it.to.row][it.to.col]?.type] ?: 0 else 0 
                }.thenByDescending { isPromotion(it) })

                for (move in sortedMoves) {
                    val newState = applyMoveToBoardForEvaluation(state, move)
                    val score = minimax(newState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, botColor)
                    if (score > bestScore) {
                        bestScore = score
                        bestMove = move
                    }
                }

                bestMove ?: validMoves.random()
            }
            else -> {
                validMoves.random()
            }
        }
    }

    private fun minimax(
        state: ChessState,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingPlayer: Boolean,
        ourColor: ChessColor
    ): Int {
        val currentPlayerIndex = if (maximizingPlayer) state.players.indexOfFirst {
            state.players.indexOf(it) == if (ourColor == ChessColor.WHITE) 0 else 1
        } else {
            1 - state.players.indexOfFirst { state.players.indexOf(it) == if (ourColor == ChessColor.WHITE) 0 else 1 }
        }

        val playerId = state.players.getOrNull(currentPlayerIndex)
        val validMoves = if (playerId != null) engine.getValidMoves(state, playerId) else emptyList()

        // Check for terminal states
        if (validMoves.isEmpty()) {
            val currentColor = if (playerId != null) {
                if (state.players.indexOf(playerId) == 0) ChessColor.WHITE else ChessColor.BLACK
            } else ourColor
            if (isKingInCheck(state, currentColor)) {
                return if (currentColor == ourColor) -100000 else 100000
            }
            return 0
        }

        if (depth == 0) {
            return quiescenceSearch(state, alpha, beta, ourColor)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (maximizingPlayer) {
            var maxEval = Int.MIN_VALUE
            // Sort moves for better pruning
            val sortedMoves = validMoves.sortedWith(compareByDescending<ChessAction.Move> { 
                if (isCapture(state, it)) pieceValues[state.board[it.to.row][it.to.col]?.type] ?: 0 else 0 
            }.thenByDescending { isPromotion(it) })
            
            for (move in sortedMoves) {
                val newState = applyMoveToBoardForEvaluation(state, move)
                val eval = minimax(newState, depth - 1, currentAlpha, currentBeta, false, ourColor)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            // Sort moves for better pruning
            val sortedMoves = validMoves.sortedWith(compareByDescending<ChessAction.Move> { 
                if (isCapture(state, it)) pieceValues[state.board[it.to.row][it.to.col]?.type] ?: 0 else 0 
            }.thenByDescending { isPromotion(it) })
            
            for (move in sortedMoves) {
                val newState = applyMoveToBoardForEvaluation(state, move)
                val eval = minimax(newState, depth - 1, currentAlpha, currentBeta, true, ourColor)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun quiescenceSearch(
        state: ChessState,
        alpha: Int,
        beta: Int,
        ourColor: ChessColor
    ): Int {
        val standPat = evaluateBoard(state, ourColor)
        if (standPat >= beta) return beta
        var newAlpha = if (alpha > standPat) alpha else standPat

        val currentPlayerIndex = state.players.indexOfFirst {
            state.players.indexOf(it) == if (ourColor == ChessColor.WHITE) 0 else 1
        }
        val playerId = state.players.getOrNull(currentPlayerIndex) ?: return standPat
        val allMoves = engine.getValidMoves(state, playerId)
        val captures = allMoves.filter { isCapture(state, it) || isPromotion(it) }

        // Sort captures by SEE (simplified: MVV/LVA)
        val sortedCaptures = captures.sortedWith(compareByDescending<ChessAction.Move> { 
            if (isCapture(state, it)) pieceValues[state.board[it.to.row][it.to.col]?.type] ?: 0 else 0 
        }.thenByDescending { isPromotion(it) })

        for (move in sortedCaptures) {
            val newState = applyMoveToBoardForEvaluation(state, move)
            val score = -quiescenceSearch(newState, -beta, -newAlpha, ourColor)
            if (score >= beta) return beta
            if (score > newAlpha) newAlpha = score
        }

        return newAlpha
    }

    private fun evaluateBoard(state: ChessState, ourColor: ChessColor): Int {
        var score = 0
        val opponentColor = if (ourColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        // Material and position score
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col] ?: continue
                val pieceValue = pieceValues[piece.type] ?: 0
                val positionBonus = getPositionBonus(piece.type, row, col, piece.color)

                if (piece.color == ourColor) {
                    score += pieceValue + positionBonus
                } else {
                    score -= pieceValue + positionBonus
                }
            }
        }

        // Bishop pair bonus
        if (countPieces(state, ourColor, ChessPieceType.BISHOP) >= 2) {
            score += 30
        }
        if (countPieces(state, opponentColor, ChessPieceType.BISHOP) >= 2) {
            score -= 30
        }

        // Center control bonus
        score += evaluateCenterControl(state, ourColor)
        score -= evaluateCenterControl(state, opponentColor)

        // King safety bonus
        score += evaluateKingSafety(state, ourColor)
        score -= evaluateKingSafety(state, opponentColor)

        // Pawn structure evaluation
        score += evaluatePawnStructure(state, ourColor)
        score -= evaluatePawnStructure(state, opponentColor)

        // Mobility (simple count of moves)
        score += evaluateMobility(state, ourColor)
        score -= evaluateMobility(state, opponentColor)

        return score
    }

    private fun countPieces(state: ChessState, color: ChessColor, type: ChessPieceType): Int {
        var count = 0
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece?.color == color && piece.type == type) {
                    count++
                }
            }
        }
        return count
    }

    private fun evaluateCenterControl(state: ChessState, color: ChessColor): Int {
        var score = 0
        val centerSquares = listOf(Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4))
        val extendedCenter = listOf(
            Position(2, 2), Position(2, 3), Position(2, 4), Position(2, 5),
            Position(3, 2), Position(3, 5),
            Position(4, 2), Position(4, 5),
            Position(5, 2), Position(5, 3), Position(5, 4), Position(5, 5)
        )

        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = state.board[row][col]
                if (piece?.color == color) {
                    if (pos in centerSquares) {
                        score += 10
                    } else if (pos in extendedCenter) {
                        score += 5
                    }
                }
            }
        }
        return score
    }

    private fun evaluateKingSafety(state: ChessState, color: ChessColor): Int {
        var score = 0
        val kingPos = findKingPosition(state, color) ?: return score

        // Bonus for pawn shelter
        val pawnRow = if (color == ChessColor.WHITE) kingPos.row + 1 else kingPos.row - 1
        for (colOffset in -1..1) {
            val col = kingPos.col + colOffset
            if (col in 0..7 && pawnRow in 0..7) {
                val piece = state.board[pawnRow][col]
                if (piece?.type == ChessPieceType.PAWN && piece.color == color) {
                    score += 10
                }
            }
        }
        return score
    }

    private fun evaluatePawnStructure(state: ChessState, color: ChessColor): Int {
        var score = 0
        val pawnCols = mutableMapOf<Int, MutableList<Int>>()

        // Collect pawn positions
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece?.type == ChessPieceType.PAWN && piece.color == color) {
                    pawnCols.getOrPut(col) { mutableListOf() }.add(row)
                }
            }
        }

        // Penalty for doubled pawns
        for ((_, rows) in pawnCols) {
            if (rows.size > 1) {
                score -= 20 * (rows.size - 1)
            }
        }

        // Penalty for isolated pawns
        for (col in pawnCols.keys) {
            if (col - 1 !in pawnCols && col + 1 !in pawnCols) {
                score -= 15
            }
        }

        return score
    }

    private fun evaluateMobility(state: ChessState, color: ChessColor): Int {
        val playerId = state.players.getOrNull(if (color == ChessColor.WHITE) 0 else 1) ?: return 0
        val moves = engine.getValidMoves(state, playerId)
        return moves.size * 2
    }

    private fun findKingPosition(state: ChessState, color: ChessColor): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece?.type == ChessPieceType.KING && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }

    private fun isKingInCheck(state: ChessState, color: ChessColor): Boolean {
        val kingPos = findKingPosition(state, color) ?: return false
        val opponentColor = if (color == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece != null && piece.color == opponentColor) {
                    if (isValidPieceMove(state, piece, Position(row, col), kingPos)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isValidPieceMove(state: ChessState, piece: ChessPiece, from: Position, to: Position): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val absRowDiff = abs(rowDiff)
        val absColDiff = abs(colDiff)

        return when (piece.type) {
            ChessPieceType.PAWN -> isValidPawnMove(state, piece.color, from, to, rowDiff, colDiff)
            ChessPieceType.ROOK -> isValidRookMove(state, from, to, rowDiff, colDiff)
            ChessPieceType.KNIGHT -> (absRowDiff == 2 && absColDiff == 1) || (absRowDiff == 1 && absColDiff == 2)
            ChessPieceType.BISHOP -> isValidBishopMove(state, from, to, rowDiff, colDiff)
            ChessPieceType.QUEEN -> isValidRookMove(state, from, to, rowDiff, colDiff) || isValidBishopMove(state, from, to, rowDiff, colDiff)
            ChessPieceType.KING -> absRowDiff <= 1 && absColDiff <= 1
        }
    }

    private fun isValidPawnMove(state: ChessState, color: ChessColor, from: Position, to: Position, rowDiff: Int, colDiff: Int): Boolean {
        val direction = if (color == ChessColor.WHITE) -1 else 1
        val startRow = if (color == ChessColor.WHITE) 6 else 1
        val targetPiece = state.board[to.row][to.col]

        if (colDiff == 0 && targetPiece == null) {
            if (rowDiff == direction) return true
            if (from.row == startRow && rowDiff == 2 * direction) {
                val middleRow = from.row + direction
                return state.board[middleRow][from.col] == null
            }
        }

        if (abs(colDiff) == 1 && rowDiff == direction) {
            if (targetPiece != null && targetPiece.color != color) return true
            if (state.enPassantTarget == to) return true
        }

        return false
    }

    private fun isValidRookMove(state: ChessState, from: Position, to: Position, rowDiff: Int, colDiff: Int): Boolean {
        if (rowDiff != 0 && colDiff != 0) return false
        return isPathClear(state, from, to)
    }

    private fun isValidBishopMove(state: ChessState, from: Position, to: Position, rowDiff: Int, colDiff: Int): Boolean {
        if (abs(rowDiff) != abs(colDiff)) return false
        return isPathClear(state, from, to)
    }

    private fun isPathClear(state: ChessState, from: Position, to: Position): Boolean {
        val rowStep = when {
            to.row > from.row -> 1
            to.row < from.row -> -1
            else -> 0
        }
        val colStep = when {
            to.col > from.col -> 1
            to.col < from.col -> -1
            else -> 0
        }

        var currentRow = from.row + rowStep
        var currentCol = from.col + colStep

        while (currentRow != to.row || currentCol != to.col) {
            if (state.board[currentRow][currentCol] != null) return false
            currentRow += rowStep
            currentCol += colStep
        }

        return true
    }

    private fun isCapture(state: ChessState, move: ChessAction.Move): Boolean {
        return state.board[move.to.row][move.to.col] != null
    }

    private fun isPromotion(move: ChessAction.Move): Boolean {
        return move.promotion != null
    }

    private fun getPositionBonus(type: ChessPieceType, row: Int, col: Int, color: ChessColor): Int {
        val actualRow = if (color == ChessColor.BLACK) 7 - row else row
        val actualCol = col

        return when (type) {
            ChessPieceType.PAWN -> pawnTable[actualRow][actualCol]
            ChessPieceType.KNIGHT -> knightTable[actualRow][actualCol]
            ChessPieceType.BISHOP -> bishopTable[actualRow][actualCol]
            ChessPieceType.ROOK -> rookTable[actualRow][actualCol]
            ChessPieceType.QUEEN -> queenTable[actualRow][actualCol]
            ChessPieceType.KING -> kingTable[actualRow][actualCol]
        }
    }

    private fun applyMoveToBoardForEvaluation(state: ChessState, move: ChessAction.Move): ChessState {
        val newBoard = state.board.map { it.toMutableList() }.toMutableList()
        val piece = newBoard[move.from.row][move.from.col] ?: return state

        newBoard[move.to.row][move.to.col] = if (move.promotion != null) {
            ChessPiece(move.promotion, piece.color)
        } else {
            piece
        }
        newBoard[move.from.row][move.from.col] = null

        if (piece.type == ChessPieceType.PAWN && state.enPassantTarget == move.to) {
            val captureRow = if (piece.color == ChessColor.WHITE) move.to.row + 1 else move.to.row - 1
            newBoard[captureRow][move.to.col] = null
        }

        val nextTurn = if (state.turn == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
        val nextPlayerIndex = (state.players.indexOf(state.currentPlayer) + 1) % state.players.size

        return state.copy(
            board = newBoard,
            turn = nextTurn,
            currentPlayer = state.players[nextPlayerIndex]
        )
    }
}
