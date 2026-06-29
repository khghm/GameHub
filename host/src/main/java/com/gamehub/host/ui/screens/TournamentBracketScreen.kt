package com.gamehub.host.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.viewmodel.TournamentViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BracketMatch(
    val id: String,
    val playerA: String?,
    val playerB: String?,
    val winner: String?,
    val status: String,
    val gameSessionId: String?
)

@Composable
fun TournamentBracketScreen(
    tournamentId: String,
    viewModel: TournamentViewModel = viewModel(),
    onBack: () -> Unit,
    onPlayMatch: (String, String, String) -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val playerName = viewModel.playerName
    var bracketMatches by remember { mutableStateOf<List<BracketMatch>>(emptyList()) }

    LaunchedEffect(tournament) {
        tournament?.bracketData?.let { bracketJson ->
            bracketMatches = parseBracket(bracketJson)
        }
    }

    // بررسی مسابقه فعال برای کاربر
    fun checkMyMatch() {
        if (playerName.isNotEmpty()) {
            viewModel.getMyMatch(tournamentId, playerName)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.listTournaments()
        checkMyMatch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← بازگشت", color = Color.White) }
                Text("🏆 براکت تورنمنت", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Button(onClick = { checkMyMatch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("مسابقه من")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bracketMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("براکت هنوز تولید نشده است", color = Color.White)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(bracketMatches) { index, match ->
                        BracketMatchCard(
                            match = match,
                            round = (index / 2) + 1,
                            onPlay = {
                                if (match.status == "pending" && match.playerA != null && match.playerB != null) {
                                    viewModel.startMatch(tournamentId, match.id)
                                    onPlayMatch(match.id, tournament?.gameId ?: "tictactoe", playerName)
                                }
                            },
                            onReportResult = { winnerId ->
                                viewModel.reportResult(tournamentId, match.id, winnerId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BracketMatchCard(
    match: BracketMatch,
    round: Int,
    onPlay: () -> Unit,
    onReportResult: (String) -> Unit
) {
    val isFinished = match.winner != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFinished) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("دور $round", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            Text("بازیکن ۱: ${match.playerA ?: "---"}", color = Color.White)
            Text("بازیکن ۲: ${match.playerB ?: "---"}", color = Color.White)
            if (isFinished) {
                Text("برنده: ${match.winner}", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
            } else if (match.status == "in_progress") {
                Text("در حال برگزاری...", color = Color(0xFFFFA726))
                Button(onClick = { onReportResult(match.playerA ?: match.playerB ?: "") }) {
                    Text("گزارش نتیجه (موقت)")
                }
            } else if (match.playerA != null && match.playerB != null && match.status == "pending") {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("شروع مسابقه")
                }
            }
        }
    }
}

private fun parseBracket(json: String): List<BracketMatch> {
    val matches = mutableListOf<BracketMatch>()
    try {
        val obj = Json.parseToJsonElement(json).jsonObject
        val rounds = obj["rounds"]?.jsonArray ?: return emptyList()
        for (round in rounds) {
            val roundObj = round.jsonObject
            val matchesArray = roundObj["matches"]?.jsonArray ?: continue
            for (matchElem in matchesArray) {
                val matchObj = matchElem.jsonObject
                matches.add(
                    BracketMatch(
                        id = matchObj["id"]?.jsonPrimitive?.content ?: "",
                        playerA = matchObj["playerA"]?.jsonPrimitive?.contentOrNull,
                        playerB = matchObj["playerB"]?.jsonPrimitive?.contentOrNull,
                        winner = matchObj["winner"]?.jsonPrimitive?.contentOrNull,
                        status = matchObj["status"]?.jsonPrimitive?.content ?: "pending",
                        gameSessionId = matchObj["gameSessionId"]?.jsonPrimitive?.contentOrNull
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return matches
}