package com.gamehub.games.matchmonster.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.gamehub.games.matchmonster.*
import com.gamehub.shared.core.PlayerId

@Composable
fun MatchMonsterScreen(
    state: MatchMonsterState,
    currentPlayerId: PlayerId,
    onAction: (MatchMonsterAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val playerDataMap = state.getPlayerDataMap()
    val playerData = playerDataMap[currentPlayerId] ?: return
    val opponentId = state.players.firstOrNull { it != currentPlayerId }
    val opponentData = opponentId?.let { playerDataMap[it] }

    var selectedTile by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0033),
                        Color(0xFF330066),
                        Color(0xFF1A0033)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            opponentData?.let { OpponentInfo(it) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFFFD700))
            )

            PlayerInfo(playerData)

            Spacer(modifier = Modifier.height(8.dp))

            GameBoard(
                board = playerData.board,
                selectedTile = selectedTile,
                onTileClick = { row, col ->
                    if (selectedTile == null) {
                        selectedTile = row to col
                    } else if (selectedTile == row to col) {
                        selectedTile = null
                    } else if (isAdjacent(selectedTile!!, row to col)) {
                        onAction(MatchMonsterAction.SwapTiles(selectedTile!!, row to col))
                        selectedTile = null
                    } else {
                        selectedTile = row to col
                    }
                }
            )
        }
    }
}

@Composable
private fun OpponentInfo(data: PlayerGameData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "حریف",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF333333))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(data.hp / 100f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF4444), Color(0xFFCC0000))
                        )
                    )
            )
        }
        Text(
            text = "HP: ${data.hp}",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PlayerInfo(data: PlayerGameData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "شما",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF333333))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(data.hp / 100f)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF44FF44), Color(0xFF00CC00))
                        )
                    )
            )
        }
        Text(
            text = "HP: ${data.hp}",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun GameBoard(
    board: List<List<Tile?>>,
    selectedTile: Pair<Int, Int>?,
    onTileClick: (Int, Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until 8) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 6) {
                    val tile = board[row][col]
                    val isSelected = selectedTile == row to col
                    TileCell(
                        tile = tile,
                        isSelected = isSelected,
                        onClick = { onTileClick(row, col) }
                    )
                }
            }
        }
    }
}

private fun isAdjacent(p1: Pair<Int, Int>, p2: Pair<Int, Int>): Boolean {
    val rowDiff = kotlin.math.abs(p1.first - p2.first)
    val colDiff = kotlin.math.abs(p1.second - p2.second)
    return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
}

@Composable
private fun TileCell(
    tile: Tile?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (tile?.type) {
        MonsterType.FIRE -> Color(0xFFFF6B35)
        MonsterType.WATER -> Color(0xFF00D4FF)
        MonsterType.EARTH -> Color(0xFF8B4513)
        MonsterType.AIR -> Color(0xFF90EE90)
        MonsterType.DARK -> Color(0xFF800080)
        MonsterType.LIGHT -> Color(0xFFFFFF00)
        null -> Color(0xFF333333)
    }

    var modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (isSelected) color.copy(alpha = 0.8f) else color)
        .border(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) Color.White else Color(0x44FFFFFF),
            shape = RoundedCornerShape(8.dp)
        )
    if (tile != null) {
        modifier = modifier.clickable(onClick = onClick)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (tile != null) {
            Text(
                text = when (tile.type) {
                    MonsterType.FIRE -> "🔥"
                    MonsterType.WATER -> "💧"
                    MonsterType.EARTH -> "🪨"
                    MonsterType.AIR -> "🌪️"
                    MonsterType.DARK -> "🌑"
                    MonsterType.LIGHT -> "☀️"
                },
                fontSize = 24.sp
            )
        }
    }
}
