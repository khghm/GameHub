package com.gamehub.games.checkers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.checkers.*

@Composable
fun CheckersScreen(
    state: CheckersState,
    onAction: (CheckersAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPosition by remember { mutableStateOf<Position?>(null) }
    val currentPlayerId = state.currentPlayer ?: return
    val validActions = CheckersEngine().getValidActions(state, currentPlayerId)
    val currentTurnPlayerIndex = state.players.indexOf(currentPlayerId)
    val currentTurnColor = if (currentTurnPlayerIndex == 0) CheckersColor.RED else CheckersColor.WHITE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF795548)),
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
                    text = "حرفه‌بazi (Checkers)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.turn == CheckersColor.RED) "نوبت قرمز" else "نوبت سفید",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "قرمز: ${12 - (state.capturedPieces[CheckersColor.WHITE] ?: 0)} | سفید: ${12 - (state.capturedPieces[CheckersColor.RED] ?: 0)}",
                    fontSize = 16.sp
                )
            }
        }

        CheckersBoard(
            state = state,
            selectedPosition = selectedPosition,
            currentPath = selectedPosition?.let { listOf(it) } ?: emptyList(),
            validActions = validActions,
            currentTurnColor = currentTurnColor,
            onCellClick = { pos ->
                when {
                    selectedPosition == null -> {
                        // Try to select a piece
                        val piece = state.board[pos.row][pos.col]
                        if (piece != null && piece.color == currentTurnColor) {
                            selectedPosition = pos
                        }
                    }
                    selectedPosition == pos -> {
                        // Deselect
                        selectedPosition = null
                    }
                    else -> {
                        // Try to find any valid action from selectedPosition to pos
                        val matchingAction = validActions.find { action ->
                            when (action) {
                                is CheckersAction.Move ->
                                    action.from == selectedPosition && action.to == pos
                                is CheckersAction.Capture ->
                                    action.path.first() == selectedPosition && action.path.last() == pos
                            }
                        }

                        if (matchingAction != null) {
                            onAction(matchingAction)
                            selectedPosition = null
                        } else {
                            // Try selecting this new position instead if it's a valid piece
                            val piece = state.board[pos.row][pos.col]
                            if (piece != null && piece.color == currentTurnColor) {
                                selectedPosition = pos
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CheckersBoard(
    state: CheckersState,
    selectedPosition: Position?,
    currentPath: List<Position>,
    validActions: List<CheckersAction>,
    currentTurnColor: CheckersColor,
    onCellClick: (Position) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            for (row in 0 until 8) {
                Row {
                    for (col in 0 until 8) {
                        val pos = Position(row, col)
                        val isLight = (row + col) % 2 == 0
                        val isSelected = selectedPosition?.row == row && selectedPosition?.col == col
                        val isInPath = currentPath.any { it.row == row && it.col == col }

                        val isValidTarget = when {
                            selectedPosition != null -> {
                                validActions.any { action ->
                                    when (action) {
                                        is CheckersAction.Move ->
                                            action.from == selectedPosition && action.to == pos
                                        is CheckersAction.Capture ->
                                            action.path.first() == selectedPosition && action.path.last() == pos
                                    }
                                }
                            }
                            else -> false
                        }

                        CheckersCell(
                            position = pos,
                            piece = state.board[row][col],
                            isLight = isLight,
                            isSelected = isSelected,
                            isInPath = isInPath,
                            isValidTarget = isValidTarget,
                            onClick = { onCellClick(pos) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckersCell(
    position: Position,
    piece: CheckersPiece?,
    isLight: Boolean,
    isSelected: Boolean,
    isInPath: Boolean,
    isValidTarget: Boolean,
    onClick: () -> Unit
) {
    val cellColor = when {
        isSelected -> Color(0xFFFFD700)
        isInPath -> Color(0xFFFFA500)
        isLight -> Color(0xFFF0D9B5)
        else -> Color(0xFF784212)
    }
    val borderColor = when {
        isValidTarget -> Color(0xFF4CAF50)
        isSelected -> Color(0xFFFFD700)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(cellColor)
            .border(if (isValidTarget) 4.dp else 2.dp, borderColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (piece != null) {
            val pieceColor = when (piece.color) {
                CheckersColor.RED -> Color(0xFFD32F2F)
                CheckersColor.WHITE -> Color.White
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(pieceColor, CircleShape)
                    .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (piece.type == CheckersPieceType.KING) {
                    Text(
                        text = "♔",
                        fontSize = 24.sp,
                        color = if (piece.color == CheckersColor.WHITE) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (isValidTarget) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}
