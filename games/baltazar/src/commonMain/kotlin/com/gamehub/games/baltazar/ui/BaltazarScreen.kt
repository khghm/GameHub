package com.gamehub.games.baltazar.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.gamehub.games.baltazar.BaltazarAction
import com.gamehub.games.baltazar.BaltazarState
import com.gamehub.games.baltazar.CellState
import com.gamehub.shared.core.PlayerId
import kotlin.math.cos
import kotlin.math.sin

/**
 * ساخت شکل شش‌ضلعی
 */
private fun createHexagonShape(): Shape {
    return GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }
}

private val HexagonShape = createHexagonShape()

@Composable
fun BaltazarScreen(
    state: BaltazarState,
    onAction: (BaltazarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWord by remember(state.selectedCells) {
        derivedStateOf {
            state.selectedCells.mapNotNull { (r, c) -> state.getCell(r, c)?.letter }.joinToString("")
        }
    }
    val playerColorA = Color(0xFFEF5350)
    val playerColorB = Color(0xFF42A5F5)
    val isGameOver = state.winner != null

    val canSelectCell: (Int, Int) -> Boolean = { r, c ->
        val cell = state.getCell(r, c)
        cell != null && cell.state == CellState.OpenNeutral && !state.selectedCells.contains(r to c) && !isGameOver
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D47A1),
                        Color(0xFF1A237E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                state = state,
                playerColorA = playerColorA,
                playerColorB = playerColorB
            )

            WordDisplay(
                currentWord = currentWord,
                isGameOver = isGameOver
            )

            HexBoard(
                state = state,
                onAction = onAction,
                colorA = playerColorA,
                colorB = playerColorB,
                canSelectCell = canSelectCell
            )

            ActionButtons(
                onDeselect = { onAction(BaltazarAction.DeselectLast) },
                onSubmit = { onAction(BaltazarAction.SubmitWord) },
                canDeselect = state.selectedCells.isNotEmpty(),
                canSubmit = state.selectedCells.size >= 3,
                isGameOver = isGameOver
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GameHeader(
    state: BaltazarState,
    playerColorA: Color,
    playerColorB: Color
) {
    val playerA = state.players.first()
    val playerB = state.players.last()
    val isPlayerATurn = state.currentPlayer == playerA
    val isGameOver = state.winner != null

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "🎮 بالتازار",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )

            if (isGameOver) {
                Text(
                    text = "🎉 بازی تمام شد!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlayerATurn) playerColorA else Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = if (isPlayerATurn) "نوبت شما" else "نوبت حریف",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isPlayerATurn) playerColorB else Color.Gray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("B", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WordDisplay(
    currentWord: String,
    isGameOver: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isGameOver) "بازی تمام شده" else "کلمه انتخابی:",
                fontSize = 12.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentWord.ifEmpty { "حرف‌ها را انتخاب کنید" },
                fontSize = if (currentWord.isEmpty()) 16.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentWord.isEmpty()) Color(0xFFBDBDBD) else Color(0xFF1A237E),
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
fun HexBoard(
    state: BaltazarState,
    onAction: (BaltazarAction) -> Unit,
    colorA: Color,
    colorB: Color,
    canSelectCell: (Int, Int) -> Boolean
) {
    val playerA = state.players.first()
    val playerB = state.players.last()

    val allRows = listOf(
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
        listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4),
        listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 2 to 5),
        listOf(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5, 3 to 6),
        listOf(4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 5, 4 to 6),
        listOf(5 to 2, 5 to 3, 5 to 4, 5 to 5, 5 to 6),
        listOf(6 to 2, 6 to 3, 6 to 4, 6 to 5, 6 to 6)
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy((-4).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            allRows.forEachIndexed { rowIndex, rowCells ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.CenterHorizontally),
                    modifier = Modifier.offset(
                        x = if (rowIndex % 2 == 0) 0.dp else 20.dp
                    )
                ) {
                    rowCells.forEach { (r, c) ->
                        val cell = state.getCell(r, c)
                        if (cell != null) {
                            HexCell(
                                cell = cell,
                                isSelected = state.selectedCells.contains(r to c),
                                canSelect = canSelectCell(r, c),
                                playerA = playerA,
                                playerB = playerB,
                                colorA = colorA,
                                colorB = colorB,
                                isHome = (r == 0 && c == 1) || (r == 6 && c == 5),
                                onClick = { onAction(BaltazarAction.SelectCell(r, c)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HexCell(
    cell: com.gamehub.games.baltazar.HexCell,
    isSelected: Boolean,
    canSelect: Boolean,
    playerA: PlayerId,
    playerB: PlayerId,
    colorA: Color,
    colorB: Color,
    isHome: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        cell.owner == playerA -> colorA
        cell.owner == playerB -> colorB
        cell.state == CellState.Closed -> Color(0xFF9E9E9E)
        isSelected -> Color(0xFFFFA726)
        cell.state == CellState.OpenNeutral -> Color(0xFFE0F2F1)
        else -> Color(0xFFE3F2FD)
    }

    val borderColor = when {
        isHome -> Color(0xFFFFD700)
        canSelect -> Color(0xFF4CAF50)
        else -> Color(0xFF1A237E)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(HexagonShape)
            .background(bgColor)
            .border(2.dp, borderColor, HexagonShape)
            .clickable(
                enabled = canSelect,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.letter != null) {
            Text(
                cell.letter.toString(),
                color = if (cell.state == CellState.OpenNeutral) Color.Black else Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        if (isHome) {
            Text(
                "👑",
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 3.dp, end = 3.dp)
            )
        }
    }
}

@Composable
fun ActionButtons(
    onDeselect: () -> Unit,
    onSubmit: () -> Unit,
    canDeselect: Boolean,
    canSubmit: Boolean,
    isGameOver: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onDeselect,
            enabled = canDeselect && !isGameOver,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5722),
                disabledContainerColor = Color(0xFFBDBDBD)
            ),
            elevation = ButtonDefaults.buttonElevation(3.dp)
        ) {
            Text(
                "حذف آخرین",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Button(
            onClick = onSubmit,
            enabled = canSubmit && !isGameOver,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color(0xFFBDBDBD)
            ),
            elevation = ButtonDefaults.buttonElevation(3.dp)
        ) {
            Text(
                "ثبت کلمه",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
