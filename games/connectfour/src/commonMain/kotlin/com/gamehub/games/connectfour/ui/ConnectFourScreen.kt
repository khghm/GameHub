package com.gamehub.games.connectfour.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.connectfour.ConnectFourState

@Composable
fun ConnectFourScreen(
    state: ConnectFourState,
    onColumnClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isGameOver = state.currentPlayer == null
    val safeGrid = state.grid.takeIf { it.size >= 6 && it.all { row -> row.size >= 7 } }
        ?: List(6) { List(7) { null } }

    // استخراج نام/شماره‌ی بازیکن فعلی
    val turnText = if (isGameOver) "پایان" else {
        val idx = state.players.indexOf(state.currentPlayer)
        if (idx >= 0) "نوبت: بازیکن ${idx + 1}" else "نوبت: ?"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("چهار خطی", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D47A1))
                    Spacer(Modifier.height(8.dp))
                    Text(turnText, fontSize = 18.sp, color = Color.Gray)
                }
            }
            Card(
                modifier = Modifier.padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
            ) {
                Column(Modifier.padding(12.dp)) {
                    for (r in 0 until 6) {
                        Row {
                            for (c in 0 until 7) {
                                val cell = safeGrid[r][c]
                                // به‌جای مقایسه‌ی رشته‌ای، از موقعیت بازیکن در آرایه استفاده کن
                                val cellColor = when (cell) {
                                    state.players.getOrNull(0) -> Color(0xFFE53935)  // بازیکن اول
                                    state.players.getOrNull(1) -> Color(0xFFFDD835)  // بازیکن دوم
                                    else -> Color.White
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(cellColor)
                                        .clickable(enabled = !isGameOver) { onColumnClick(c) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}