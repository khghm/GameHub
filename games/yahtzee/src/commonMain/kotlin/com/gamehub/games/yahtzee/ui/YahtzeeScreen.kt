package com.gamehub.games.yahtzee.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.yahtzee.YahtzeeAction
import com.gamehub.games.yahtzee.YahtzeeCategory
import com.gamehub.games.yahtzee.YahtzeeState
import com.gamehub.games.yahtzee.YahtzeeEngine

@Composable
fun YahtzeeScreen(
    state: YahtzeeState,
    onAction: (YahtzeeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { YahtzeeEngine() }
    val currentPlayerData = state.currentPlayer?.let { state.playerData[it] }
    val upperSectionCategories = listOf(
        YahtzeeCategory.ONES,
        YahtzeeCategory.TWOS,
        YahtzeeCategory.THREES,
        YahtzeeCategory.FOURS,
        YahtzeeCategory.FIVES,
        YahtzeeCategory.SIXES
    )
    val lowerSectionCategories = listOf(
        YahtzeeCategory.THREE_OF_A_KIND,
        YahtzeeCategory.FOUR_OF_A_KIND,
        YahtzeeCategory.FULL_HOUSE,
        YahtzeeCategory.SMALL_STRAIGHT,
        YahtzeeCategory.LARGE_STRAIGHT,
        YahtzeeCategory.YAHTZEE,
        YahtzeeCategory.CHANCE
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1565C0))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Game Title
        Text(
            text = "Yahtzee (یاتزی)",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Players Panel
        PlayersPanel(state = state)

        // Dice Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "پرتاب‌های باقی‌مانده: ${state.rollsRemaining}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                DiceRow(
                    dice = state.dice,
                    heldDice = state.heldDice,
                    onToggleHold = { index ->
                        if (state.rollsRemaining in 1..2) {
                            val newHeld = if (state.heldDice.contains(index)) {
                                state.heldDice - index
                            } else {
                                state.heldDice + index
                            }
                            onAction(YahtzeeAction.HoldDice(newHeld))
                        }
                    }
                )

                Button(
                    onClick = { onAction(YahtzeeAction.Roll) },
                    enabled = state.rollsRemaining > 0,
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("پرتاب تاس", fontSize = 18.sp)
                }
            }
        }

        if (currentPlayerData != null) {
            // Upper Section Scorecard
            ScoreSection(
                title = "بخش بالا",
                categories = upperSectionCategories,
                playerData = currentPlayerData,
                dice = state.dice,
                engine = engine,
                onScoreCategory = { category -> onAction(YahtzeeAction.ScoreCategory(category)) }
            )

            // Upper Bonus
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentPlayerData.upperBonus > 0) Color(0xFFFFEB3B) else Color(0xFFE3F2FD)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("جایزه بخش بالا (≥ 63)", fontWeight = FontWeight.Bold)
                    Text(
                        "+${currentPlayerData.upperBonus}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Lower Section Scorecard
            ScoreSection(
                title = "بخش پایین",
                categories = lowerSectionCategories,
                playerData = currentPlayerData,
                dice = state.dice,
                engine = engine,
                onScoreCategory = { category -> onAction(YahtzeeAction.ScoreCategory(category)) }
            )

            // Yahtzee Bonus Count
            if (currentPlayerData.yahtzeeBonuses > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD54F))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("جایزه‌های اضافی Yahtzee", fontWeight = FontWeight.Bold)
                        Text(
                            "+${currentPlayerData.yahtzeeBonuses * 100}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Total Score
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("مجموع امتیاز", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(
                        "${currentPlayerData.totalScore}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayersPanel(state: YahtzeeState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.players.forEach { playerId ->
            val playerData = state.playerData[playerId]
            val isCurrent = state.currentPlayer == playerId
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Color(0xFFFF9800) else Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = playerId.value.take(8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${playerData?.totalScore ?: 0}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrent) {
                        Text(
                            text = "نوبت شما",
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiceRow(
    dice: List<Int>,
    heldDice: Set<Int>,
    onToggleHold: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        dice.forEachIndexed { index, value ->
            val isHeld = heldDice.contains(index)
            Dice(
                value = value,
                isHeld = isHeld,
                onClick = { onToggleHold(index) }
            )
        }
    }
}

@Composable
fun Dice(value: Int, isHeld: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(enabled = value != 0) { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHeld) Color(0xFFFF9800) else Color.White
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (value == 0) {
                Text(
                    text = "-",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                DicePips(value = value, isHeld = isHeld)
            }
        }
    }
}

@Composable
fun DicePips(value: Int, isHeld: Boolean) {
    val color = if (isHeld) Color.White else Color.Black
    val pipSize = 10.dp

    Box(modifier = Modifier.size(48.dp)) {
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
                        .padding(6.dp)
                )
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            3 -> {
                Box(
                    modifier = Modifier
                        .size(pipSize)
                        .background(color, CircleShape)
                        .align(Alignment.TopStart)
                        .padding(6.dp)
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
                        .padding(6.dp)
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
                            .padding(6.dp)
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
                            .padding(6.dp)
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
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreSection(
    title: String,
    categories: List<YahtzeeCategory>,
    playerData: com.gamehub.games.yahtzee.YahtzeePlayerData,
    dice: List<Int>,
    engine: YahtzeeEngine,
    onScoreCategory: (YahtzeeCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
                    .padding(12.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            categories.forEach { category ->
                val score = playerData.scores[category]
                val canScore = score == null
                val previewScore = if (canScore) engine.calculateScore(dice, category) else null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canScore) { onScoreCategory(category) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getCategoryName(category),
                        fontSize = 14.sp,
                        fontWeight = if (canScore) FontWeight.SemiBold else FontWeight.Normal
                    )

                    if (score != null) {
                        Text(
                            text = score.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (previewScore != null) {
                        Text(
                            text = previewScore.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text("-", fontSize = 18.sp)
                    }
                }
                if (category != categories.last()) {
                    Divider()
                }
            }
        }
    }
}

fun getCategoryName(category: YahtzeeCategory): String {
    return when (category) {
        YahtzeeCategory.ONES -> "۱‌ها"
        YahtzeeCategory.TWOS -> "۲‌ها"
        YahtzeeCategory.THREES -> "۳‌ها"
        YahtzeeCategory.FOURS -> "۴‌ها"
        YahtzeeCategory.FIVES -> "۵‌ها"
        YahtzeeCategory.SIXES -> "۶‌ها"
        YahtzeeCategory.THREE_OF_A_KIND -> "سه‌تایی"
        YahtzeeCategory.FOUR_OF_A_KIND -> "چهارتایی"
        YahtzeeCategory.FULL_HOUSE -> "فول هاوس"
        YahtzeeCategory.SMALL_STRAIGHT -> "ست کوچک"
        YahtzeeCategory.LARGE_STRAIGHT -> "ست بزرگ"
        YahtzeeCategory.YAHTZEE -> "یاتزی"
        YahtzeeCategory.CHANCE -> "شانس"
    }
}
