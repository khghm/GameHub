package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.gamehub.host.network.ApiClient
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val points: Int,
    val gamesPlayed: Int = 0
)

@Composable
fun LeaderboardScreen(
    currentUsername: String,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFriendsOnly by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { ApiClient() }
    val friends = authViewModel.currentUser.collectAsState().value?.friends ?: emptyList()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val json = apiClient.getLeaderboard()
                entries = parseLeaderboardJson(json)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    val friendUsernames = remember(friends) { friends.map { it.username }.toSet() }
    val filteredEntries = remember(entries, showFriendsOnly, friendUsernames) {
        if (showFriendsOnly) entries.filter { it.username in friendUsernames || it.username == currentUsername }
        else entries
    }

    val userRank = remember(filteredEntries, currentUsername) {
        filteredEntries.indexOfFirst { it.username == currentUsername } + 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← بازگشت", color = OnPrimary, fontSize = 16.sp)
                }
                Text("🏆 امتیازات", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                // Toggle friends filter
                FilterChip(
                    selected = showFriendsOnly,
                    onClick = { showFriendsOnly = !showFriendsOnly },
                    label = { Text("دوستان", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Success,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceVariant.copy(alpha = 0.3f),
                        labelColor = OnSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User rank card
            if (currentUsername.isNotEmpty() && userRank > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏅 رتبه شما: $userRank", fontWeight = FontWeight.Bold, color = OnBackground, fontSize = 18.sp)
                        Text(
                            "${filteredEntries.getOrNull(userRank - 1)?.points ?: 0} امتیاز",
                            color = OnBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (filteredEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("داده‌ای یافت نشد", color = OnSurfaceVariant, fontSize = 18.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filteredEntries) { index, entry ->
                        val rank = index + 1
                        val medalColor = when (rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> SurfaceVariant
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Rank
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(medalColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$rank",
                                        fontWeight = FontWeight.Bold,
                                        color = if (rank <= 3) Color.Black else OnSurface
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.username, fontWeight = FontWeight.Bold, color = OnSurface)
                                    Text(
                                        "${entry.gamesPlayed} بازی",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${entry.points} امتیاز", fontWeight = FontWeight.Bold, color = Primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("W:${entry.wins}", color = Success, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("L:${entry.losses}", color = Error, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text("D:${entry.draws}", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseLeaderboardJson(json: String): List<LeaderboardEntry> {
    val list = mutableListOf<LeaderboardEntry>()
    try {
        val jsonArray = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonArray
        jsonArray.forEachIndexed { index, element ->
            val obj = element.jsonObject
            val wins = obj["wins"]?.jsonPrimitive?.int ?: 0
            val losses = obj["losses"]?.jsonPrimitive?.int ?: 0
            val draws = obj["draws"]?.jsonPrimitive?.int ?: 0
            list.add(
                LeaderboardEntry(
                    rank = index + 1,
                    username = obj["username"]?.jsonPrimitive?.content ?: "",
                    wins = wins,
                    losses = losses,
                    draws = draws,
                    points = obj["points"]?.jsonPrimitive?.int ?: 0,
                    gamesPlayed = wins + losses + draws
                )
            )
        }
    } catch (_: Exception) {}
    return list
}