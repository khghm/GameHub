package com.gamehub.games.farkle.ui

import androidx.compose.foundation.background
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
import com.gamehub.games.farkle.*

@Composable
fun FarkleScreen(
    state: FarkleState,
    onAction: (FarkleAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDiceIds by remember { mutableStateOf(state.selectedDiceIds.toMutableList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "فارکل (Farkle)",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        PlayerPanel(state = state)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "امتیاز نوبت فعلی",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${state.turnScore}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        DiceDisplay(
            dice = state.dice,
            selectedIds = selectedDiceIds,
            onDiceClick = { id ->
                val currentState = state.dice.find { it.id == id }
                if (currentState?.state == FarkleDiceState.ROLLED) {
                    if (selectedDiceIds.contains(id)) {
                        selectedDiceIds.remove(id)
                    } else {
                        selectedDiceIds.add(id)
                    }
                }
            }
        )

        ActionButtons(
            state = state,
            onAction = onAction,
            selectedIds = selectedDiceIds
        )
    }
}

@Composable
fun PlayerPanel(state: FarkleState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.players.forEach { playerId ->
            val stats = state.stats[playerId]
            val isCurrent = state.currentPlayer == playerId
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Color(0xFFFF9800) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playerId.value.take(8),
                        fontSize = 14.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${stats?.totalScore ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (stats?.hasEnteredGame == true) "ورود کرده" else "ورود نکرده",
                        fontSize = 12.sp,
                        color = if (isCurrent) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DiceDisplay(
    dice: List<FarkleDice>,
    selectedIds: List<Int>,
    onDiceClick: (Int) -> Unit
) {
    val engine = FarkleEngine()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dice.forEach { die ->
            val isSelected = selectedIds.contains(die.id)
            val isScoring = when (die.state) {
                FarkleDiceState.ROLLED -> engine.isScoringCombination(listOf(die.value))
                else -> true
            }

            Card(
                modifier = Modifier
                    .size(70.dp)
                    .clickable(enabled = die.state == FarkleDiceState.ROLLED) {
                        onDiceClick(die.id)
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSelected -> Color(0xFFFFA726)
                        die.state == FarkleDiceState.IDLE -> Color(0xFFE0E0E0)
                        else -> Color.White
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (die.state != FarkleDiceState.IDLE) {
                        DicePips(
                            value = die.value,
                            isHighlighted = isSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DicePips(value: Int, isHighlighted: Boolean) {
    val color = if (isHighlighted) Color.White else Color.Black
    val pipSize = 10.dp

    Box(modifier = Modifier.size(50.dp)) {
        when (value) {
            1 -> {
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.Center)
                )
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            3 -> {
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
            4 -> {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .size(pipSize)
                            .background(color, CircleShape)
                            .align(align)
                            .padding(8.dp)
                    )
                }
            }
            5 -> {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.Center,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .size(pipSize)
                            .background(color, CircleShape)
                            .align(align)
                            .padding(8.dp)
                    )
                }
            }
            6 -> {
                listOf(
                    Alignment.TopStart,
                    Alignment.CenterStart,
                    Alignment.BottomStart,
                    Alignment.TopEnd,
                    Alignment.CenterEnd,
                    Alignment.BottomEnd
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .size(pipSize)
                            .background(color, CircleShape)
                            .align(align)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    state: FarkleState,
    onAction: (FarkleAction) -> Unit,
    selectedIds: List<Int>
) {
    val engine = FarkleEngine()
    val canRoll = state.dice.any { it.state == FarkleDiceState.IDLE }
    val canSelect = selectedIds.isNotEmpty() && engine.isScoringCombination(
        selectedIds.mapNotNull { id -> state.dice.find { it.id == id }?.value }
    )
    val allSelected = state.dice.all { it.state == FarkleDiceState.SELECTED }
    val currentStats = state.stats[state.currentPlayer]
    val canBank = (state.selectedDiceIds.isNotEmpty() || selectedIds.isNotEmpty()) &&
            (currentStats?.hasEnteredGame == true ||
                    (state.turnScore + engine.calculateScore(selectedIds.mapNotNull { id -> state.dice.find { it.id == id }?.value })) >= state.entryThreshold)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onAction(FarkleAction.RollDice) },
                enabled = canRoll,
                modifier = Modifier.height(56.dp)
            ) {
                Text("پرتاب تاس", fontSize = 18.sp)
            }

            if (canSelect && !state.selectedDiceIds.containsAll(selectedIds)) {
                Button(onClick = { onAction(FarkleAction.SelectDice(selectedIds)) },
                    modifier = Modifier.height(56.dp)) {
                    Text("انتخاب تاس‌ها", fontSize = 18.sp)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (allSelected) {
                Button(onClick = { onAction(FarkleAction.ContinueHotDice) },
                    modifier = Modifier.height(56.dp)) {
                    Text("ادامه (Hot Dice)", fontSize = 18.sp)
                }
            }

            Button(
                onClick = { onAction(FarkleAction.BankScore) },
                enabled = canBank,
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("ذخیره امتیاز", fontSize = 18.sp)
            }
        }
    }
}
