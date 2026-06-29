package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.host.network.ApiClient
import com.gamehub.host.network.ApiClient.MatchRecordUI
import com.gamehub.host.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchHistoryScreen(
    username: String,
    onBack: () -> Unit,
    onViewReplay: (String) -> Unit
) {
    var records by remember { mutableStateOf<List<MatchRecordUI>>(emptyList()) }
    var selectedGameFilter by remember { mutableStateOf("All") }
    val apiClient = remember { ApiClient() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(username) {
        println("📱 MatchHistoryScreen: Loading history for username: $username")
        coroutineScope.launch {
            try {
                val loaded = apiClient.getMatchHistory(username)
                println("📱 MatchHistoryScreen: Loaded ${loaded.size} records!")
                records = loaded
            } catch (e: Exception) {
                println("📱 MatchHistoryScreen ERROR: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    val filteredRecords = remember(records, selectedGameFilter) {
        if (selectedGameFilter == "All") records
        else records.filter { it.gameType == selectedGameFilter }
    }

    val stats = remember(filteredRecords, username) {
        val wins = filteredRecords.count { it.winner == username }
        val losses = filteredRecords.count { it.winner != null && it.winner != username && !it.draw }
        val draws = filteredRecords.count { it.draw }
        Triple(wins, losses, draws)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                color = SurfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("←", fontSize = 20.sp, color = OnBackground)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بازگشت", color = OnBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                "📜 تاریخچه بازی‌ها",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Summary
        if (filteredRecords.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatSummaryItem("🏆", "برد", stats.first, Success)
                    StatSummaryItem("❌", "باخت", stats.second, Error)
                    StatSummaryItem("🤝", "مساوی", stats.third, Primary)
                    StatSummaryItem("🎮", "کل", stats.first + stats.second + stats.third, OnSurface)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val gameTypes = listOf("All", "tictactoe", "connectfour", "uno", "ludo")
            gameTypes.forEach { type ->
                FilterChip(
                    selected = selectedGameFilter == type,
                    onClick = { selectedGameFilter = type },
                    label = {
                        Text(
                            getGameNameInPersian(type),
                            fontSize = 13.sp,
                            fontWeight = if (selectedGameFilter == type) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedGameFilter == type) OnPrimary else OnSurfaceVariant
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        containerColor = SurfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎮", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "هنوز بازی‌ای انجام نشده",
                        color = OnSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 10.dp)
            ) {
                items(filteredRecords) { record ->
                    MatchCard(record, username, onViewReplay)
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    record: MatchRecordUI,
    username: String,
    onViewReplay: (String) -> Unit
) {
    val isWin = record.winner == username
    val isDraw = record.draw

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isWin -> Success.copy(alpha = 0.2f)
                            isDraw -> Primary.copy(alpha = 0.2f)
                            else -> Error.copy(alpha = 0.2f)
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when {
                                    isWin -> "🏆"
                                    isDraw -> "🤝"
                                    else -> "❌"
                                },
                                fontSize = 30.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            getGameNameInPersian(record.gameType),
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                .format(Date(record.timestamp)),
                            fontSize = 13.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isWin -> Success.copy(alpha = 0.2f)
                        isDraw -> Primary.copy(alpha = 0.2f)
                        else -> Error.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        when {
                            isWin -> "  برد  "
                            isDraw -> " مساوی "
                            else -> " باخت "
                        },
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isWin -> Success
                            isDraw -> Primary
                            else -> Error
                        },
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider
            HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(18.dp))

            // Replay Button - ALWAYS SHOWS
            Button(
                onClick = {
                    val sessionId = record.gameSessionId ?: ""
                    println("📱 Replay button clicked! gameSessionId: $sessionId")
                    if (sessionId.isNotEmpty()) {
                        onViewReplay(sessionId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تماشای بازی (Replay)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatSummaryItem(
    emoji: String,
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "$value",
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = OnSurfaceVariant
        )
    }
}

private fun getGameNameInPersian(gameType: String): String {
    return when (gameType) {
        "tictactoe" -> "دوز"
        "connectfour" -> "چهارخطی"
        "uno" -> "اونو"
        "ludo" -> "منچ"
        "All" -> "همه"
        else -> gameType
    }
}
