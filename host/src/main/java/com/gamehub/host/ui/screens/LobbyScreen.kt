package com.gamehub.host.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.host.ui.theme.*
import com.gamehub.shared.registry.GameRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    modifier: Modifier = Modifier,
    onStartGame: (String) -> Unit,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onChatClick: () -> Unit,
    onPlayNowClick: (String) -> Unit,
    currentUsername: String,
    onTournamentsClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onPartyCreateClick: () -> Unit,
    onClanClick: () -> Unit,
    onSocietyClick: () -> Unit
) {
    val games = remember { GameRegistry.getAll() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = Surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سلام، $currentUsername",
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "آماده بازی هستی؟",
                        color = OnSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "چت",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onFriendsClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Secondary.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = "دوستان",
                            tint = Secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Primary, Secondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUsername.firstOrNull()?.uppercase() ?: "?",
                                color = OnPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick access row (horizontal scroll)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniChip("پارتی", "🎉", Tertiary, onPartyCreateClick)
            MiniChip("مسابقات", "🏆", Warning, onTournamentsClick)
            MiniChip("رتبه‌بندی", "📊", Secondary, onLeaderboardClick)
            MiniChip("کلن", "⚔️", Tertiary, onClanClick)
            MiniChip("انجمن", "💬", Success, onSocietyClick)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Games section (takes maximum space)
        Text(
            text = "🎮 بازی ها",
            color = OnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(games) { game ->
                ProfessionalGameCard(
                    title = game.metadata.name,
                    description = game.metadata.description,
                    gameId = game.metadata.id,
                    minPlayers = game.metadata.minPlayers,
                    maxPlayers = game.metadata.maxPlayers,
                    onClick = { onPlayNowClick(game.metadata.id) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun MiniChip(
    text: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun CompactQuickChip(
    text: String,
    icon: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProfessionalGameCard(
    title: String,
    description: String,
    gameId: String,
    minPlayers: Int,
    maxPlayers: Int,
    onClick: () -> Unit
) {
    val emoji = getGameEmoji(gameId)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Primary,
                                Secondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 44.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = description,
                    color = OnSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (minPlayers == maxPlayers) "👥 $minPlayers نفر" else "👥 $minPlayers-$maxPlayers نفر",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = 12.sp,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Text(
                    text = "بازی کن",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "شروع",
                    tint = OnPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getGameEmoji(gameId: String): String {
    return when (gameId) {
        "tictactoe" -> "❌"
        "uno" -> "🃏"
        "connectfour" -> "🔴"
        "ludo" -> "🎲"
        "chess" -> "♟️"
        "farkle" -> "🎲"
        "esmofamil" -> "📝"
        "backgammon" -> "🎲"
        "nard" -> "🎲"
        "abalone" -> "⚫"
        "spades-baloot" -> "♠️"
        "othello" -> "⚪"
        "baltazar" -> "🗡️"
        "bridge" -> "♠️"
        "checkers" -> "●"
        "blokus" -> "🔲"
        "yahtzee" -> "🎲"
        "hex" -> "⬡"
        "battleship" -> "🚢"
        "match-monster" -> "👾"
        "soccer-striker" -> "⚽"
        else -> "🎮"
    }
}
