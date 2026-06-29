package com.gamehub.games.hex.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.hex.HexState

@Composable
fun HexScreen(
    state: HexState,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRedTurn = state.currentPlayer?.let { state.players.indexOf(it) == 0 } ?: false
    
    val currentPlayerColor by animateColorAsState(
        targetValue = if (isRedTurn) Color(0xFFEF5350) else Color(0xFF42A5F5),
        animationSpec = tween(500),
        label = "currentPlayerColor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = currentPlayerColor),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "هگس (Hex)",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(currentPlayerColor)
                    )
                    Text(
                        text = "نوبت: ${state.currentPlayer?.let { player ->
                            val idx = state.players.indexOf(player)
                            if (idx == 0) "قرمز" else if (idx == 1) "آبی" else "?"
                        } ?: "?"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentPlayerColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game Board
        Card(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 0.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                for (i in 0 until 11) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (i * 10).dp)
                    ) {
                        for (j in 0 until 11) {
                            val cell = state.grid[i][j]
                            val isRed = cell != null && state.players.indexOf(cell) == 0
                            val isBlue = cell != null && state.players.indexOf(cell) == 1
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val cellColor by animateColorAsState(
                                targetValue = when {
                                    isRed -> Color(0xFFEF5350)
                                    isBlue -> Color(0xFF42A5F5)
                                    else -> Color(0xFFCBD5E1)
                                },
                                animationSpec = tween(300),
                                label = "cellColor"
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.85f else 1f,
                                animationSpec = tween(100),
                                label = "cellScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .padding(2.dp)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .background(
                                        if (cell != null) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    cellColor,
                                                    cellColor.copy(alpha = 0.7f)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFE2E8F0),
                                                    Color(0xFFCBD5E1)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = if (cell != null) 1.8.dp else 1.dp,
                                        color = if (cell != null) {
                                            if (isRed) Color(0xFFC62828) else Color(0xFF1565C0)
                                        } else {
                                            Color(0xFF94A3B8)
                                        },
                                        shape = CircleShape
                                    )
                                    .shadow(
                                        elevation = if (cell != null) 6.dp else 2.dp,
                                        shape = CircleShape,
                                        ambientColor = cellColor
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { onCellClick(i, j) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            LegendItem(color = Color(0xFFEF5350), label = "Player 1 (قرمز)", isActive = isRedTurn)
            LegendItem(color = Color(0xFF42A5F5), label = "Player 2 (آبی)", isActive = !isRedTurn)
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = tween(300),
        label = "legendScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .shadow(
                    elevation = if (isActive) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = color
                )
                .background(color, CircleShape)
                .border(
                    width = if (isActive) 2.5.dp else 1.5.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            color = if (isActive) color else Color(0xFF94A3B8),
            fontSize = if (isActive) 16.sp else 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
