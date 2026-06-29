package com.gamehub.games.bridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.bridge.*
import com.gamehub.games.bridge.getValidBids
import com.gamehub.games.bridge.getValidPlays

@Composable
fun BridgeScreen(
    state: BridgeState,
    onAction: (BridgeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top info
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بورد ${state.currentBoard} از ${state.totalBoards}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // North
        HandSection(
            seat = Seat.NORTH,
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth()
        )

        // Middle: West, Center, East
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HandSection(
                seat = Seat.WEST,
                state = state,
                onAction = onAction,
                modifier = Modifier.width(100.dp)
            )

            // Center: Bidding or Play
            CenterTableSection(
                state = state,
                onAction = onAction,
                modifier = Modifier.weight(1f)
            )

            HandSection(
                seat = Seat.EAST,
                state = state,
                onAction = onAction,
                modifier = Modifier.width(100.dp)
            )
        }

        // South (player)
        HandSection(
            seat = Seat.SOUTH,
            state = state,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
            isPlayer = true
        )

        // Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "شمال-جنوب: ${state.scores[Team.NS]}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "شرق-غرب: ${state.scores[Team.EW]}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HandSection(
    seat: Seat,
    state: BridgeState,
    onAction: (BridgeAction) -> Unit,
    modifier: Modifier = Modifier,
    isPlayer: Boolean = false
) {
    val hand = state.hands[seat] ?: emptyList()
    val isCurrentTurn = state.currentPlayer == state.seatPlayers[seat]
    val isDummy = state.contract?.declarer?.partner() == seat && state.dummyRevealed && !state.biddingPhase
    val showCards = isPlayer || isDummy
    val validPlays = if (isCurrentTurn && !state.biddingPhase) getValidPlays(state, seat) else emptyList()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) Color(0xFFFFA726) else Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = seat.persianName,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal,
                fontSize = 18.sp,
                color = if (isCurrentTurn) Color.White else Color.Black
            )

            if (showCards) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    hand.forEach { card ->
                        val isValidPlay = validPlays.any { it is BridgeAction.PlayCard && it.card == card }
                        CardButton(
                            card = card,
                            onClick = { onAction(BridgeAction.PlayCard(card)) },
                            enabled = isCurrentTurn && !state.biddingPhase && isValidPlay,
                            isPlayer = isPlayer
                        )
                    }
                }
            } else {
                Text(
                    text = "${hand.size} کارت",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun CardButton(
    card: Card,
    onClick: () -> Unit,
    enabled: Boolean,
    isPlayer: Boolean
) {
    val color = when (card.suit) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        else -> Color.Black
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(if (isPlayer) 60.dp else 40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = color
        )
    ) {
        Text(
            text = "${card.rank.symbol}${card.suit.symbol}",
            fontSize = if (isPlayer) 16.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CenterTableSection(
    state: BridgeState,
    onAction: (BridgeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.biddingPhase) {
                BiddingSection(state = state, onAction = onAction)
            } else {
                PlaySection(state = state)
            }
        }
    }
}

@Composable
fun BiddingSection(
    state: BridgeState,
    onAction: (BridgeAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("مزایده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Bidding history
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.biddingHistory) { (seat, bid) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(seat.abbreviation, fontWeight = FontWeight.Bold)
                    Text(
                        text = when (bid) {
                            is Bid.Pass -> "پاس"
                            is Bid.Call -> "${bid.level}${bid.suit.symbol}"
                            is Bid.Double -> "×"
                            is Bid.Redouble -> "××"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bidding buttons if it's player's turn (South)
        val playerSeat = Seat.SOUTH
        if (state.currentBidder == playerSeat && state.currentPlayer == state.seatPlayers[playerSeat]) {
            val validActions = getValidBids(state, playerSeat)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                validActions.filterIsInstance<BridgeAction.MakeBid>().forEach { action ->
                    Button(
                        onClick = { onAction(action) },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text(
                            text = when (action.bid) {
                                is Bid.Pass -> "پاس"
                                is Bid.Call -> "${(action.bid as Bid.Call).level}${(action.bid as Bid.Call).suit.symbol}"
                                is Bid.Double -> "×"
                                is Bid.Redouble -> "××"
                            },
                            color = Color.White
                        )
                    }
                }
            }
        }

        state.contract?.let {
            Text(
                text = "قرارداد: ${it.level}${it.suit.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlaySection(state: BridgeState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.contract?.let {
            Text(
                text = "قرارداد: ${it.level}${it.suit.symbol}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Current trick
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(Seat.NORTH, Seat.EAST, Seat.SOUTH, Seat.WEST).forEach { seat ->
                state.currentTrick.cards[seat]?.let { card ->
                    CardButton(
                        card = card,
                        onClick = {},
                        enabled = false,
                        isPlayer = false
                    )
                } ?: Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(seat.abbreviation)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("شمال-جنوب: ${state.tricksWon[Team.NS]}", style = MaterialTheme.typography.titleSmall)
            Text("شرق-غرب: ${state.tricksWon[Team.EW]}", style = MaterialTheme.typography.titleSmall)
        }
    }
}
