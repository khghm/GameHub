package com.gamehub.games.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.chess.ChessPieceType
import com.gamehub.games.chess.ChessState
import com.gamehub.games.chess.Position

@Composable
fun ChessScreen(
    state: ChessState,
    onMove: (from: Position, to: Position, promotion: ChessPieceType?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPosition by remember { mutableStateOf<Position?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF424242)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "شطرنج (Chess)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.turn == com.gamehub.games.chess.ChessColor.WHITE) "نوبت سفید" else "نوبت سیاه",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        ChessBoard(
            state = state,
            selectedPosition = selectedPosition,
            onCellClick = { position ->
                if (selectedPosition == null) {
                    // Select a piece
                    val piece = state.board[position.row][position.col]
                    val currentColor = if (state.players.indexOf(state.currentPlayer) == 0) {
                        com.gamehub.games.chess.ChessColor.WHITE
                    } else {
                        com.gamehub.games.chess.ChessColor.BLACK
                    }
                    if (piece != null && piece.color == currentColor) {
                        selectedPosition = position
                    }
                } else if (selectedPosition == position) {
                    // Deselect
                    selectedPosition = null
                } else {
                    // Try to move
                    onMove(selectedPosition!!, position, null)
                    selectedPosition = null
                }
            }
        )
    }
}

@Composable
fun ChessBoard(
    state: ChessState,
    selectedPosition: Position?,
    onCellClick: (Position) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            for (row in 0..7) {
                Row {
                    for (col in 0..7) {
                        val isLight = (row + col) % 2 == 0
                        val isSelected = selectedPosition?.row == row && selectedPosition?.col == col
                        ChessCell(
                            position = Position(row, col),
                            piece = state.board[row][col],
                            isLight = isLight,
                            isSelected = isSelected,
                            onClick = { onCellClick(Position(row, col)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChessCell(
    position: Position,
    piece: com.gamehub.games.chess.ChessPiece?,
    isLight: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> Color(0xFFFFD700)
        isLight -> Color(0xFFF0D9B5)
        else -> Color(0xFFB58863)
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .background(backgroundColor)
            .border(if (isSelected) 3.dp else 0.dp, Color(0xFFFF9800))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            Text(
                text = getPieceSymbol(piece),
                fontSize = 32.sp,
                color = if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) Color.White else Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getPieceSymbol(piece: com.gamehub.games.chess.ChessPiece): String {
    return when (piece.type) {
        ChessPieceType.KING -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♔" else "♚"
        ChessPieceType.QUEEN -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♕" else "♛"
        ChessPieceType.ROOK -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♖" else "♜"
        ChessPieceType.BISHOP -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♗" else "♝"
        ChessPieceType.KNIGHT -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♘" else "♞"
        ChessPieceType.PAWN -> if (piece.color == com.gamehub.games.chess.ChessColor.WHITE) "♙" else "♟"
    }
}
