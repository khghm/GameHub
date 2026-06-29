package com.gamehub.games.esmofamil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.esmofamil.ESF_CATEGORIES
import com.gamehub.games.esmofamil.EsmoFamilAction
import com.gamehub.games.esmofamil.EsmoFamilPhase
import com.gamehub.games.esmofamil.EsmoFamilState

@Composable
fun EsmoFamilScreen(
    state: EsmoFamilState,
    currentPlayerId: String,
    onAction: (EsmoFamilAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var answers by remember { mutableStateOf<Map<Int, String?>>(emptyMap()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF9C27B0))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "اسم و فامیل",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "دور ${state.roundNumber} از ${state.maxRounds}",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEB3B))
            ) {
                Box(Modifier.padding(16.dp)) {
                    Text(
                        text = "حرف: ${state.currentLetter}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (state.phase == EsmoFamilPhase.ANSWERING) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(Modifier.padding(16.dp)) {
                        Text(
                            text = "زمان: ${state.timeRemaining}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Categories and answers
        when (state.phase) {
            EsmoFamilPhase.ANSWERING -> {
                ESF_CATEGORIES.forEachIndexed { idx, category ->
                    OutlinedTextField(
                        value = answers[idx] ?: "",
                        onValueChange = { newValue ->
                            answers = answers.toMutableMap().apply { this[idx] = newValue }
                        },
                        label = { Text(category, color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = {
                        onAction(EsmoFamilAction.SubmitAnswers(answers))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("ثبت پاسخ‌ها", fontSize = 18.sp)
                }
            }
            EsmoFamilPhase.SHOWING_RESULTS -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "نتایج دور ${state.roundNumber}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("در حال آماده‌سازی...", fontSize = 18.sp)
                    }
                }
            }
        }

        // Total Scores
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "امتیاز کل:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                state.stats.forEach { (playerId, stats) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(playerId.value.take(10), fontSize = 16.sp)
                        Text("${stats.totalScore}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
