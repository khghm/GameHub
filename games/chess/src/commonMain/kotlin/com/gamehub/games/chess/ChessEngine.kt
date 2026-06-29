package com.gamehub.games.chess

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult

class ChessEngine : GameDefinition<ChessState, ChessAction, GameResult> {

    override val metadata = GameMetadata(
        id = "chess",
        name = "شطرنج (Chess)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک شطرنج 8×8"
    )

    override fun createInitialState(players: List<PlayerId>): ChessState {
        val board = createInitialBoard()
        return ChessState(
            board = board,
            currentPlayer = players.firstOrNull(),
            players = players,
            turn = ChessColor.WHITE
        )
    }

    private fun createInitialBoard(): List<List<ChessPiece?>> {
        val board = MutableList(8) { MutableList<ChessPiece?>(8) { null } }

        for (col in 0..7) {
            board[1][col] = ChessPiece(ChessPieceType.PAWN, ChessColor.BLACK)
            board[6][col] = ChessPiece(ChessPieceType.PAWN, ChessColor.WHITE)
        }

        val backRank = listOf(
            ChessPieceType.ROOK,
            ChessPieceType.KNIGHT,
            ChessPieceType.BISHOP,
            ChessPieceType.QUEEN,
            ChessPieceType.KING,
            ChessPieceType.BISHOP,
            ChessPieceType.KNIGHT,
            ChessPieceType.ROOK
        )

        for (col in 0..7) {
            board[0][col] = ChessPiece(backRank[col], ChessColor.BLACK)
            board[7][col] = ChessPiece(backRank[col], ChessColor.WHITE)
        }

        return board
    }

    override fun validateAction(state: ChessState, action: ChessAction, playerId: PlayerId): Boolean {
        val currentColor = if (state.players.indexOf(playerId) == 0) ChessColor.WHITE else ChessColor.BLACK
        if (state.turn != currentColor) return false
        if (state.currentPlayer != playerId) return false

        return when (action) {
            is ChessAction.Move -> isValidMove(state, action, currentColor)
        }
    }

    private fun isValidMove(state: ChessState, action: ChessAction.Move, color: ChessColor): Boolean {
        val from = action.from
        val to = action.to

        if (from.row !in 0..7 || from.col !in 0..7 || to.row !in 0..7 || to.col !in 0..7) return false

        val piece = state.board[from.row][from.col] ?: return false
        if (piece.color != color) return false

        val targetPiece = state.board[to.row][to.col]
        if (targetPiece != null && targetPiece.color == color) return false

        if (!isValidPieceMove(state, piece, from, to)) return false

        val testState = applyMoveToBoard(state, action)
        if (isKingInCheck(testState, color)) return false

        return true
    }

    private fun isValidPieceMove(state: ChessState, piece: ChessPiece, from: Position, to: Position): Boolean {
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        val absRowDiff = kotlin.math.abs(rowDiff)
        val absColDiff = kotlin.math.abs(colDiff)

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

        if (kotlin.math.abs(colDiff) == 1 && rowDiff == direction) {
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
        if (kotlin.math.abs(rowDiff) != kotlin.math.abs(colDiff)) return false
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

    private fun applyMoveToBoard(state: ChessState, action: ChessAction.Move): ChessState {
        val newBoard = state.board.map { it.toMutableList() }.toMutableList()
        val piece = newBoard[action.from.row][action.from.col] ?: return state

        var enPassantTarget: Position? = null
        var castlingRights = state.castlingRights

        if (piece.type == ChessPieceType.PAWN && kotlin.math.abs(action.to.row - action.from.row) == 2) {
            val epRow = (action.from.row + action.to.row) / 2
            enPassantTarget = Position(epRow, action.from.col)
        }

        if (piece.type == ChessPieceType.KING) {
            castlingRights = when (piece.color) {
                ChessColor.WHITE -> castlingRights.copy(whiteKingSide = false, whiteQueenSide = false)
                ChessColor.BLACK -> castlingRights.copy(blackKingSide = false, blackQueenSide = false)
            }
        }

        if (piece.type == ChessPieceType.ROOK) {
            if (action.from == Position(7, 0)) castlingRights = castlingRights.copy(whiteQueenSide = false)
            if (action.from == Position(7, 7)) castlingRights = castlingRights.copy(whiteKingSide = false)
            if (action.from == Position(0, 0)) castlingRights = castlingRights.copy(blackQueenSide = false)
            if (action.from == Position(0, 7)) castlingRights = castlingRights.copy(blackKingSide = false)
        }

        newBoard[action.to.row][action.to.col] = if (action.promotion != null) {
            ChessPiece(action.promotion, piece.color)
        } else {
            piece
        }
        newBoard[action.from.row][action.from.col] = null

        if (piece.type == ChessPieceType.PAWN && state.enPassantTarget == action.to) {
            val captureRow = if (piece.color == ChessColor.WHITE) action.to.row + 1 else action.to.row - 1
            newBoard[captureRow][action.to.col] = null
        }

        val nextTurn = if (state.turn == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE
        val nextPlayerIndex = (state.players.indexOf(state.currentPlayer) + 1) % state.players.size
        val fullMoveNumber = if (state.turn == ChessColor.BLACK) state.fullMoveNumber + 1 else state.fullMoveNumber

        return state.copy(
            board = newBoard,
            turn = nextTurn,
            currentPlayer = state.players[nextPlayerIndex],
            castlingRights = castlingRights,
            enPassantTarget = enPassantTarget,
            fullMoveNumber = fullMoveNumber
        )
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

    private fun findKingPosition(state: ChessState, color: ChessColor): Position? {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece != null && piece.type == ChessPieceType.KING && piece.color == color) {
                    return Position(row, col)
                }
            }
        }
        return null
    }

    private fun hasLegalMoves(state: ChessState, color: ChessColor): Boolean {
        for (fromRow in 0..7) {
            for (fromCol in 0..7) {
                val piece = state.board[fromRow][fromCol]
                if (piece != null && piece.color == color) {
                    for (toRow in 0..7) {
                        for (toCol in 0..7) {
                            val testMove = ChessAction.Move(Position(fromRow, fromCol), Position(toRow, toCol))
                            if (isValidMove(state, testMove, color)) {
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    override fun applyAction(state: ChessState, action: ChessAction, playerId: PlayerId): GameUpdateResult<ChessState, GameResult> {
        if (!validateAction(state, action, playerId)) {
            return GameUpdateResult(state)
        }

        val newState = when (action) {
            is ChessAction.Move -> applyMoveToBoard(state, action)
        }

        val currentColor = if (newState.players.indexOf(newState.currentPlayer) == 0) ChessColor.WHITE else ChessColor.BLACK
        val opponentColor = if (currentColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        val result = when {
            isKingInCheck(newState, currentColor) && !hasLegalMoves(newState, currentColor) -> {
                GameResult.Win(newState.players[if (opponentColor == ChessColor.WHITE) 0 else 1])
            }
            !hasLegalMoves(newState, currentColor) -> GameResult.Draw
            else -> null
        }

        return GameUpdateResult(newState, result)
    }

    override fun isTerminal(state: ChessState): Boolean {
        val currentColor = if (state.players.indexOf(state.currentPlayer) == 0) ChessColor.WHITE else ChessColor.BLACK
        return !hasLegalMoves(state, currentColor)
    }

    override fun getResult(state: ChessState): GameResult? {
        val currentColor = if (state.players.indexOf(state.currentPlayer) == 0) ChessColor.WHITE else ChessColor.BLACK
        val opponentColor = if (currentColor == ChessColor.WHITE) ChessColor.BLACK else ChessColor.WHITE

        return when {
            isKingInCheck(state, currentColor) && !hasLegalMoves(state, currentColor) -> {
                GameResult.Win(state.players[if (opponentColor == ChessColor.WHITE) 0 else 1])
            }
            !hasLegalMoves(state, currentColor) -> GameResult.Draw
            else -> null
        }
    }

    override fun getPlayers(state: ChessState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: ChessState, playerId: PlayerId): ChessState {
        val nextTurn = if (state.players.indexOf(playerId) == 0) ChessColor.WHITE else ChessColor.BLACK
        return state.copy(currentPlayer = playerId, turn = nextTurn)
    }

    fun getValidMoves(state: ChessState, playerId: PlayerId): List<ChessAction.Move> {
        val color = if (state.players.indexOf(playerId) == 0) ChessColor.WHITE else ChessColor.BLACK
        val moves = mutableListOf<ChessAction.Move>()

        for (fromRow in 0..7) {
            for (fromCol in 0..7) {
                val piece = state.board[fromRow][fromCol]
                if (piece != null && piece.color == color) {
                    for (toRow in 0..7) {
                        for (toCol in 0..7) {
                            val move = ChessAction.Move(Position(fromRow, fromCol), Position(toRow, toCol))
                            if (isValidMove(state, move, color)) {
                                moves.add(move)
                            }
                        }
                    }
                }
            }
        }

        return moves
    }
}
