package com.gamehub.games.spadesbaloot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.spadesbaloot.*
import com.gamehub.shared.core.PlayerId

@Composable
fun SpadesBalootScreen(
    state: SpadesBalootState,
    onAction: (SpadesBalootAction) -> Unit,
    modifier: Modifier = Modifier
) {
    // Assume local player is first in the list for now
    val localPlayerId = state.players.firstOrNull() ?: return

    // Check if game is finished
    if (state.gamePhase == GamePhase.FINISHED && state.winner != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎉 بازی تمام شد!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val isTeam1 = state.isTeam1(state.winner!!)
                    Text(
                        text = if (isTeam1) "تیم 1 برنده شد!" else "تیم 2 برنده شد!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTeam1) Color(0xFFD32F2F) else Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تیم 1", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${state.teamScore1} امتیاز", fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تیم 2", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("${state.teamScore2} امتیاز", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32)),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score display
        ScoreDisplay(state)

        // Opponent hands (top, left, right - simplified)
        OpponentsDisplay(state)

        // Current trick
        CurrentTrickDisplay(state)

        // Local player's hand
        HandDisplay(
            hand = state.hands[localPlayerId] ?: emptyList(),
            gamePhase = state.gamePhase,
            currentPlayer = state.currentPlayer,
            localPlayer = localPlayerId,
            leadSuit = state.leadSuit,
            spadesBroken = state.spadesBroken,
            onCardClick = { card ->
                if (state.currentPlayer == localPlayerId && state.gamePhase == GamePhase.PLAYING) {
                    onAction(SpadesBalootAction.PlayCard(card))
                }
            },
            onAction = onAction
        )
    }
}

@Composable
fun ScoreDisplay(state: SpadesBalootState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تیم 1", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("امتیاز: ${state.teamScore1}", fontSize = 16.sp)
                Text("کیف: ${state.teamBags1}", fontSize = 14.sp)
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("تیم 2", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("امتیاز: ${state.teamScore2}", fontSize = 16.sp)
                Text("کیف: ${state.teamBags2}", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun OpponentsDisplay(state: SpadesBalootState) {
    // Simplified opponent display
    Box(
        modifier = Modifier.height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left opponent
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                modifier = Modifier.size(60.dp, 90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🃏", fontSize = 30.sp)
                }
            }
            // Top opponent
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                modifier = Modifier.size(60.dp, 90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🃏", fontSize = 30.sp)
                }
            }
            // Right opponent
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                modifier = Modifier.size(60.dp, 90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🃏", fontSize = 30.sp)
                }
            }
        }
    }
}

@Composable
fun CurrentTrickDisplay(state: SpadesBalootState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.currentTrick.forEach { (player, card) ->
                CardView(card, 60.dp)
            }
        }
    }
}

@Composable
fun HandDisplay(
    hand: List<Card>,
    gamePhase: GamePhase,
    currentPlayer: PlayerId?,
    localPlayer: PlayerId,
    leadSuit: Suit?,
    spadesBroken: Boolean,
    onCardClick: (Card) -> Unit,
    onAction: (SpadesBalootAction) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(hand, key = { "${it.suit}_${it.rank}" }) { card ->
            val isYourTurn = currentPlayer == localPlayer
            val canPlay = isYourTurn && gamePhase == GamePhase.PLAYING
            CardView(
                card = card,
                size = 90.dp,
                onClick = if (canPlay) {
                    { onCardClick(card) }
                } else {
                    null
                }
            )
        }
    }

    // Phase-specific UI
    if (currentPlayer == localPlayer) {
        when (gamePhase) {
            GamePhase.BIDDING -> {
                BiddingUI(onBid = { bid, isNil -> onAction(SpadesBalootAction.Bid(bid, isNil)) })
            }
            else -> {}
        }
    }
}

@Composable
fun CardView(card: Card, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    val cardColor = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) {
        Color(0xFFD32F2F)
    } else {
        Color.Black
    }

    Box(
        modifier = Modifier
            .size(width = size * 0.7f, height = size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .shadow(if (onClick != null) 8.dp else 2.dp, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getRankDisplay(card.rank),
                fontSize = (size.value / 3.5).sp,
                color = cardColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = card.suit.symbol,
                fontSize = (size.value / 2.5).sp,
                color = cardColor
            )
        }
    }
}

private fun getRankDisplay(rank: Rank): String {
    return when (rank) {
        Rank.ACE -> "A"
        Rank.KING -> "K"
        Rank.QUEEN -> "Q"
        Rank.JACK -> "J"
        Rank.TEN -> "10"
        else -> rank.value.toString()
    }
}

@Composable
fun BiddingUI(onBid: (Int, Boolean) -> Unit) {
    var selectedBid by remember { mutableStateOf(3) }
    var isNil by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("مزایده بده!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Bid selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items((0..13).toList()) { bid ->
                    Button(
                        onClick = { selectedBid = bid },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedBid == bid) Color(0xFF0D47A1) else Color(0xFF1565C0)
                        )
                    ) {
                        Text("$bid", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Nil checkbox
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isNil, onCheckedChange = { isNil = it })
                Text("نیل (0 دست)", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onBid(selectedBid, isNil) },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("ثبت مزایده", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
