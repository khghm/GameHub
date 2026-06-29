package com.gamehub.games.othello.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.othello.OthelloAction
import com.gamehub.games.othello.OthelloPiece
import com.gamehub.games.othello.OthelloState
import com.gamehub.shared.core.PlayerId

@Composable
fun OthelloScreen(
    state: OthelloState,
    onAction: (OthelloAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPiece = remember(state.currentPlayer) {
        state.currentPlayer?.let { state.getPlayerPiece(it) }
    }
    val isGameOver = remember(state.blackCount, state.whiteCount) {
        state.blackCount + state.whiteCount == 64 || 
        (state.currentPlayer == null && !state.hasLegalMovesForPlayer(state.getOpponentPiece(currentPiece ?: OthelloPiece.BLACK)))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1),
                        Color(0xFF1565C0)
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScorePanel(
            blackCount = state.blackCount,
            whiteCount = state.whiteCount,
            currentPiece = currentPiece,
            isGameOver = isGameOver
        )

        Spacer(modifier = Modifier.height(16.dp))

        Board(
            state = state,
            currentPlayer = state.currentPlayer,
            onAction = onAction,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        StatusPanel(
            currentPiece = currentPiece,
            isGameOver = isGameOver,
            blackCount = state.blackCount,
            whiteCount = state.whiteCount
        )
    }
}

@Composable
fun ScorePanel(
    blackCount: Int,
    whiteCount: Int,
    currentPiece: OthelloPiece?,
    isGameOver: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ScoreCard(
            color = Color.Black,
            count = blackCount,
            isActive = currentPiece == OthelloPiece.BLACK,
            isWinner = isGameOver && blackCount > whiteCount
        )
        ScoreCard(
            color = Color.White,
            count = whiteCount,
            isActive = currentPiece == OthelloPiece.WHITE,
            isWinner = isGameOver && whiteCount > blackCount
        )
    }
}

@Composable
fun ScoreCard(
    color: Color,
    count: Int,
    isActive: Boolean,
    isWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(120.dp, 100.dp)
            .shadow(
                elevation = if (isActive || isWinner) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = true
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) Color(0xFFFFD700) 
                            else if (isActive) Color(0xFF2196F3)
                            else Color(0xFF424242)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        color = color,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (color == Color.White) Color.Gray else Color.LightGray,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWinner) Color.Black else Color.White
            )
        }
    }
}

@Composable
fun Board(
    state: OthelloState,
    currentPlayer: PlayerId?,
    onAction: (OthelloAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(
            elevation = 16.dp,
            shape = RoundedCornerShape(12.dp),
            clip = true
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                repeat(8) { row ->
                    Row(
                        modifier = Modifier.weight(1f)
                    ) {
                        repeat(8) { col ->
                            Cell(
                                piece = state.board[row][col],
                                onClick = {
                                    if (currentPlayer != null) {
                                        onAction(OthelloAction.Move(row, col))
                                    }
                                },
                                isLegalMove = isLegalMove(state, row, col, currentPlayer),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Cell(
    piece: OthelloPiece,
    onClick: () -> Unit,
    isLegalMove: Boolean,
    modifier: Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF388E3C))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (isLegalMove || piece != OthelloPiece.EMPTY) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (isLegalMove) {
                            onClick()
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = if (isPressed) 0.95f else 1f
                scaleY = if (isPressed) 0.95f else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        when (piece) {
            OthelloPiece.BLACK -> Piece(Color.Black, Color(0xFF212121))
            OthelloPiece.WHITE -> Piece(Color.White, Color(0xFFF5F5F5))
            OthelloPiece.EMPTY -> {
                if (isLegalMove) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.3f)
                            .clip(CircleShape)
                            .background(
                                color = Color(0x80FFEB3B),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color(0xFFFFC107),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun Piece(mainColor: Color, shadowColor: Color) {
    val transition = updateTransition(targetState = Unit, label = "piece")
    
    val scale by transition.animateFloat(
        transitionSpec = { spring(stiffness = 300f) },
        label = "scale"
    ) { 1f }

    Box(
        modifier = Modifier
            .fillMaxSize(0.85f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = 8.dp.toPx()
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        mainColor,
                        shadowColor
                    )
                )
            )
            .border(
                width = 1.5.dp,
                color = if (mainColor == Color.White) Color(0xFFBDBDBD) else Color(0xFF424242),
                shape = CircleShape
            )
    )
}

@Composable
fun StatusPanel(
    currentPiece: OthelloPiece?,
    isGameOver: Boolean,
    blackCount: Int,
    whiteCount: Int,
    modifier: Modifier = Modifier
) {
    val message = when {
        isGameOver -> when {
            blackCount > whiteCount -> "🎉 سیاه برنده شد!"
            whiteCount > blackCount -> "🎉 سفید برنده شد!"
            else -> "🤝 مساوی!"
        }
        currentPiece == OthelloPiece.BLACK -> "نوبت سیاه"
        currentPiece == OthelloPiece.WHITE -> "نوبت سفید"
        else -> "منتظر بازیکن..."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGameOver) Color(0xFFD32F2F) else Color(0xFF212121)
            )
        }
    }
}

private fun isLegalMove(state: OthelloState, row: Int, col: Int, playerId: PlayerId?): Boolean {
    if (playerId == null) return false
    if (row !in 0..7 || col !in 0..7) return false
    if (state.board[row][col] != OthelloPiece.EMPTY) return false
    val piece = state.getPlayerPiece(playerId)
    val directions = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
    val opponent = if (piece == OthelloPiece.BLACK) OthelloPiece.WHITE else OthelloPiece.BLACK

    directions.forEach { (dr, dc) ->
        val currentFlips = mutableListOf<Pair<Int, Int>>()
        var r = row + dr
        var c = col + dc
        while (r in 0..7 && c in 0..7) {
            val currentPiece = state.board[r][c]
            when {
                currentPiece == opponent -> currentFlips.add(r to c)
                currentPiece == piece && currentFlips.isNotEmpty() -> return true
                else -> break
            }
            r += dr
            c += dc
        }
    }
    return false
}
