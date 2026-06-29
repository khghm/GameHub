package com.gamehub.games.abalone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamehub.games.abalone.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AbaloneScreen(
    state: AbaloneState,
    onAction: (AbaloneAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMarbles by remember { mutableStateOf<List<AbalonePos>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(Modifier.padding(16.dp)) {
                    Text(
                        "سیاه: ${state.capturedWhite}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(Modifier.padding(16.dp)) {
                    Text(
                        "سفید: ${state.capturedBlack}",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Turn indicator
        val isBlackTurn = state.currentPlayer == state.blackPlayerId
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isBlackTurn) Color.Black else Color.White
            )
        ) {
            Box(Modifier.padding(12.dp)) {
                Text(
                    "نوبت: ${if (isBlackTurn) "سیاه" else "سفید"}",
                    color = if (isBlackTurn) Color.White else Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game board
        AbaloneBoard(
            state = state,
            selectedMarbles = selectedMarbles,
            onMarbleClick = { pos ->
                if (selectedMarbles.contains(pos)) {
                    selectedMarbles = selectedMarbles - pos
                } else {
                    // Check if adding this marble keeps the group valid
                    val newSelected = selectedMarbles + pos
                    val marbles = newSelected.mapNotNull { state.getMarbleAt(it) }
                    val isValid = marbles.all { it.color == (if (isBlackTurn) AbaloneColor.BLACK else AbaloneColor.WHITE) } &&
                            marbles.size in 1..3
                    if (isValid) {
                        selectedMarbles = newSelected
                    }
                }
            }
        )

        // Direction buttons
        if (selectedMarbles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "جهت حرکت را انتخاب کنید:",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AbaloneDirection.entries.forEach { dir ->
                    Button(onClick = {
                        onAction(AbaloneAction.Move(selectedMarbles, dir))
                        selectedMarbles = emptyList()
                    }) {
                        Text(text = dir.name, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AbaloneBoard(
    state: AbaloneState,
    selectedMarbles: List<AbalonePos>,
    onMarbleClick: (AbalonePos) -> Unit
) {
    val hexSize = 30f
    val hexWidth = hexSize * 2
    val hexHeight = sqrt(3f) * hexSize

    // Generate all valid positions
    val allPositions = mutableListOf<AbalonePos>()
    for (q in -4..4) {
        for (r in -4..4) {
            val s = -q - r
            if (s in -4..4) {
                allPositions.add(AbalonePos(q, r))
            }
        }
    }

    Canvas(
        modifier = Modifier
            .size(400.dp)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val centerX = size.width.toFloat() / 2
                    val centerY = size.height.toFloat() / 2

                    // Find the closest hexagon to the tap position
                    val closestPos = allPositions.minByOrNull { pos ->
                        val (x, y) = axialToPixel(pos.q, pos.r, hexSize, centerX, centerY)
                        val dx = tapOffset.x - x
                        val dy = tapOffset.y - y
                        dx * dx + dy * dy
                    }

                    closestPos?.let {
                        onMarbleClick(it)
                    }
                }
            }
    ) {
        val centerX = size.width.toFloat() / 2
        val centerY = size.height.toFloat() / 2

        // Draw all hexagons
        allPositions.forEach { pos ->
            val (x, y) = axialToPixel(pos.q, pos.r, hexSize, centerX, centerY)
            drawHexagon(
                center = Offset(x, y),
                radius = hexSize,
                color = if (selectedMarbles.contains(pos)) Color(0xFFFFD700) else Color(0xFF4A4A6A),
                strokeColor = if (selectedMarbles.contains(pos)) Color(0xFFFFA726) else Color(0xFF6A6A9A)
            )
        }

        // Draw marbles
        state.marbles.forEach { marble ->
            val (x, y) = axialToPixel(marble.pos.q, marble.pos.r, hexSize, centerX, centerY)
            val isSelected = selectedMarbles.contains(marble.pos)
            drawCircle(
                color = if (marble.color == AbaloneColor.BLACK) Color.Black else Color.White,
                radius = if (isSelected) hexSize * 0.9f else hexSize * 0.8f,
                center = Offset(x, y)
            )
            drawCircle(
                color = if (isSelected) Color(0xFFFFD700) else Color.Gray,
                radius = if (isSelected) hexSize * 0.9f else hexSize * 0.8f,
                center = Offset(x, y),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private fun axialToPixel(q: Int, r: Int, size: Float, centerX: Float, centerY: Float): Pair<Float, Float> {
    val x = size * (3f / 2f * q.toFloat())
    val y = size * (sqrt(3f) / 2f * q.toFloat() + sqrt(3f) * r.toFloat())
    return Pair(x + centerX, y + centerY)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    center: Offset,
    radius: Float,
    color: Color,
    strokeColor: Color
) {
    val points = mutableListOf<Offset>()
    for (i in 0..5) {
        val angle = Math.toRadians(60.0 * i - 30).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        points.add(Offset(x, y))
    }

    // Draw fill
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            close()
        },
        color = color
    )

    // Draw stroke
    for (i in 0 until points.size) {
        val next = points[(i + 1) % points.size]
        drawLine(
            color = strokeColor,
            start = points[i],
            end = next,
            strokeWidth = 2.dp.toPx()
        )
    }
}
