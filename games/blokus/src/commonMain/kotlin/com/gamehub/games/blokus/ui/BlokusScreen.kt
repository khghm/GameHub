package com.gamehub.games.blokus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gamehub.games.blokus.*

@Composable
fun BlokusScreen(
    state: BlokusState,
    onAction: (BlokusAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayerData = state.playerData[state.currentPlayer]
    var selectedPieceId by remember { mutableStateOf<Int?>(null) }
    var selectedRotation by remember { mutableStateOf(0) }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFF1A237E)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("بلوکِس!", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("نوبت: ${state.currentPlayer}", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        // Board
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            state.board.forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    row.forEachIndexed { colIndex, cell ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = when(cell) {
                                        BlokusColor.RED -> Color.Red
                                        BlokusColor.BLUE -> Color.Blue
                                        BlokusColor.GREEN -> Color.Green
                                        BlokusColor.YELLOW -> Color.Yellow
                                        else -> Color.White.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(2.dp)
                                )
                                .clickable(enabled = selectedPieceId != null) {
                                    if (selectedPieceId != null && currentPlayerData != null) {
                                        val action = BlokusAction.Place(selectedPieceId!!, selectedRotation, rowIndex, colIndex)
                                        onAction(action)
                                    }
                                }
                                .border(
                                    width = if ((rowIndex == 0 && colIndex == 0) ||
                                        (rowIndex == 0 && colIndex == 19) ||
                                        (rowIndex == 19 && colIndex == 0) ||
                                        (rowIndex == 19 && colIndex == 19)) 1.dp else 0.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Piece selector
        if (currentPlayerData != null) {
            Text("قطعات:", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(currentPlayerData.remainingPieces.toList()) { pieceId ->
                    val piece = BlokusPieces.first { it.id == pieceId }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PiecePreview(
                            piece = piece,
                            color = currentPlayerData.color,
                            rotation = selectedRotation,
                            selected = selectedPieceId == pieceId,
                            onClick = {
                                selectedPieceId = pieceId
                                selectedRotation = 0
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(piece.name, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rotation and pass buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val piece = selectedPieceId?.let { BlokusPieces.firstOrNull { p -> p.id == it } }
                    if (piece != null) {
                        selectedRotation = (selectedRotation - 1 + piece.shapes.size) % piece.shapes.size
                    }
                }, enabled = selectedPieceId != null) {
                    Text("چرخش چپ")
                }
                Button(onClick = {
                    val piece = selectedPieceId?.let { BlokusPieces.firstOrNull { p -> p.id == it } }
                    if (piece != null) {
                        selectedRotation = (selectedRotation + 1) % piece.shapes.size
                    }
                }, enabled = selectedPieceId != null) {
                    Text("چرخش راست")
                }
                Button(onClick = { onAction(BlokusAction.Pass) }) {
                    Text("پاس")
                }
            }
        }
    }
}

@Composable
fun PiecePreview(
    piece: BlokusPieceData,
    color: BlokusColor,
    rotation: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val safeRotation = rotation % piece.shapes.size
    val shape = piece.shapes[safeRotation]
    val minRow = shape.minOfOrNull { it.first } ?: 0
    val minCol = shape.minOfOrNull { it.second } ?: 0
    val maxRow = shape.maxOfOrNull { it.first } ?: 0
    val maxCol = shape.maxOfOrNull { it.second } ?: 0

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(if (selected) 2.dp else 0.dp)
            .clickable(onClick = onClick)
    ) {
        for (row in minRow..maxRow) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in minCol..maxCol) {
                    val isPresent = shape.any { it.first == row && it.second == col }
                    Box(
                        modifier = Modifier.size(12.dp).background(
                            color = if (isPresent) when(color) {
                                BlokusColor.RED -> Color.Red
                                BlokusColor.BLUE -> Color.Blue
                                BlokusColor.GREEN -> Color.Green
                                BlokusColor.YELLOW -> Color.Yellow
                                else -> Color.Gray
                            } else Color.Transparent,
                            shape = RoundedCornerShape(2.dp)
                        )
                    )
                }
            }
        }
    }
}
