package com.gamehub.host.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.chess.ChessState
import com.gamehub.games.chess.ChessPiece
import com.gamehub.games.chess.ChessColor
import com.gamehub.games.othello.OthelloPiece
import com.gamehub.games.checkers.CheckersColor
import com.gamehub.games.tictactoe.TicTacToeState
import com.gamehub.host.network.ReplayClient
import com.gamehub.host.network.ReplayClient.GameEvent
import com.gamehub.host.network.gameJson
import com.gamehub.shared.core.GameState
import com.gamehub.shared.registry.GameRegistry
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ساخت شکل شش‌ضلعی برای بالتازار
 */
private fun createHexagonShape(): Shape {
    return GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val radius = minOf(width, height) / 2

        for (i in 0 until 6) {
            val angle = Math.toRadians(60.0 * i - 30)
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
    }
}

private val HexagonShape = createHexagonShape()

@Composable
fun CompactConnectFourBoard(
    state: com.gamehub.games.connectfour.ConnectFourState,
    modifier: Modifier = Modifier
) {
    val safeGrid = state.grid.takeIf { it.size >= 6 && it.all { row -> row.size >= 7 } }
        ?: List(6) { List(7) { null } }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until 6) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (c in 0 until 7) {
                        val cell = safeGrid[r][c]
                        val cellColor = when (cell) {
                            state.players.getOrNull(0) -> Color(0xFFE53935)
                            state.players.getOrNull(1) -> Color(0xFFFDD835)
                            else -> Color.White
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(cellColor)
                        )
                    }
                }
            }
        }
    }
}

fun getPieceSymbol(piece: ChessPiece): String {
    return when (piece.type) {
        com.gamehub.games.chess.ChessPieceType.KING -> if (piece.color == ChessColor.WHITE) "♔" else "♚"
        com.gamehub.games.chess.ChessPieceType.QUEEN -> if (piece.color == ChessColor.WHITE) "♕" else "♛"
        com.gamehub.games.chess.ChessPieceType.ROOK -> if (piece.color == ChessColor.WHITE) "♖" else "♜"
        com.gamehub.games.chess.ChessPieceType.BISHOP -> if (piece.color == ChessColor.WHITE) "♗" else "♝"
        com.gamehub.games.chess.ChessPieceType.KNIGHT -> if (piece.color == ChessColor.WHITE) "♘" else "♞"
        com.gamehub.games.chess.ChessPieceType.PAWN -> if (piece.color == ChessColor.WHITE) "♙" else "♟"
    }
}

@Composable
fun CompactChessBoard(
    state: com.gamehub.games.chess.ChessState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            for (row in 0..7) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..7) {
                        val isLight = (row + col) % 2 == 0
                        val backgroundColor = if (isLight) Color(0xFFF0D9B5) else Color(0xFFB58863)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val piece = state.board[row][col]
                            if (piece != null) {
                                Text(
                                    text = getPieceSymbol(piece),
                                    fontSize = 28.sp,
                                    color = if (piece.color == ChessColor.WHITE) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactCheckersBoard(
    state: com.gamehub.games.checkers.CheckersState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            for (row in 0 until 8) {
                Row(Modifier.weight(1f)) {
                    for (col in 0 until 8) {
                        val isLight = (row + col) % 2 == 0
                        val backgroundColor = if (isLight) Color(0xFFF0D9B5) else Color(0xFF784212)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            val piece = state.board[row][col]
                            if (piece != null) {
                                val pieceColor = when (piece.color) {
                                    CheckersColor.RED -> Color(0xFFD32F2F)
                                    CheckersColor.WHITE -> Color.White
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(0.75f)
                                        .background(pieceColor, CircleShape)
                                        .border(1.5.dp, Color.Black, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (piece.type == com.gamehub.games.checkers.CheckersPieceType.KING) {
                                        Text(
                                            text = "♔",
                                            fontSize = 20.sp,
                                            color = if (piece.color == CheckersColor.WHITE) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
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

@Composable
fun CompactOthelloBoard(
    state: com.gamehub.games.othello.OthelloState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                repeat(8) { row ->
                    Row(Modifier.weight(1f)) {
                        repeat(8) { col ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(Color(0xFF388E3C)),
                                contentAlignment = Alignment.Center
                            ) {
                                val piece = state.board[row][col]
                                when (piece) {
                                    OthelloPiece.BLACK -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize(0.85f)
                                                .clip(CircleShape)
                                                .background(Color.Black)
                                                .border(
                                                    1.5.dp,
                                                    Color(0xFF424242),
                                                    CircleShape
                                                )
                                        )
                                    }
                                    OthelloPiece.WHITE -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize(0.85f)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                                .border(
                                                    1.5.dp,
                                                    Color(0xFFBDBDBD),
                                                    CircleShape
                                                )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    gameSessionId: String,
    onBack: () -> Unit
) {
    val replayClient = remember { ReplayClient() }

    var events by remember { mutableStateOf<List<GameEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentEventIndex by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(gameSessionId) {
        try {
            val result = replayClient.getGameEvents(gameSessionId)
            events = result
        } catch (e: Exception) {
            println("📱 ReplayScreen ERROR: ${e.message}")
            e.printStackTrace()
        } finally {
            loading = false
        }
    }

    // Auto-play loop
    LaunchedEffect(isPlaying, currentEventIndex, playbackSpeed) {
        while (isPlaying && currentEventIndex < events.size - 1) {
            delay((1000 / playbackSpeed).toLong())
            if (isPlaying && !isDragging) {
                currentEventIndex++
            }
        }
        if (isPlaying && currentEventIndex >= events.size - 1) {
            isPlaying = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { Text("🎬 Replay بازی", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("←", fontSize = 18.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بازگشت", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            )

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF6366F1))
                        Spacer(Modifier.height(16.dp))
                        Text("در حال بارگذاری رویدادها...", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    }
                }
            } else if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("هیچ رویدادی برای این بازی ثبت نشده", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                    }
                }
            } else {
                // Main scrollable container
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Replay Player Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Top Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "رویداد ${currentEventIndex + 1} از ${events.size}",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val gameType = events.firstOrNull()?.gameType ?: "نامشخص"
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(getGameEmoji(gameType), fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                GameRegistry.getAll().find { it.metadata.id == gameType }?.metadata?.name ?: gameType,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    // Playback Speed
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(0.5f, 1f, 1.5f, 2f).forEach { speed ->
                                            Surface(
                                                onClick = { playbackSpeed = speed },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (playbackSpeed == speed)
                                                    Color(0xFF6366F1)
                                                else
                                                    Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "${speed}x",
                                                    color = Color.White,
                                                    fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Draggable Progress Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    val progress = (currentEventIndex + 1f) / events.size

                                    // Progress Fill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                                )
                                            )
                                    )

                                    // Draggable Thumb
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxWidth(progress)
                                            .padding(end = 12.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6366F1))
                                        )
                                    }

                                    // Touch Area
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragStart = { isDragging = true },
                                                    onDragEnd = { isDragging = false },
                                                    onDragCancel = { isDragging = false }
                                                ) { change, _ ->
                                                    change.consume()
                                                    val width = size.width.toFloat()
                                                    if (width > 0) {
                                                        val newProgress = (change.position.x / width).coerceIn(0f, 1f)
                                                        currentEventIndex = (newProgress * (events.size - 1)).toInt().coerceIn(0, events.size - 1)
                                                    }
                                                }
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Control Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // First
                                    Button(
                                        onClick = {
                                            currentEventIndex = 0
                                            isPlaying = false
                                        },
                                        enabled = currentEventIndex > 0,
                                        modifier = Modifier.size(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.1f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("⏮️", fontSize = 24.sp)
                                    }

                                    // Previous
                                    Button(
                                        onClick = {
                                            currentEventIndex = (currentEventIndex - 1).coerceAtLeast(0)
                                            isPlaying = false
                                        },
                                        enabled = currentEventIndex > 0,
                                        modifier = Modifier.size(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.1f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("⏮", fontSize = 24.sp)
                                    }

                                    // Play/Pause
                                    Button(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier.size(72.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isPlaying) Color(0xFFFF9800) else Color(0xFF6366F1),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(if (isPlaying) "⏸" else "▶", fontSize = 28.sp)
                                    }

                                    // Next
                                    Button(
                                        onClick = {
                                            currentEventIndex = (currentEventIndex + 1).coerceAtMost(events.size - 1)
                                            isPlaying = false
                                        },
                                        enabled = currentEventIndex < events.size - 1,
                                        modifier = Modifier.size(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.1f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("⏭", fontSize = 24.sp)
                                    }

                                    // Last
                                    Button(
                                        onClick = {
                                            currentEventIndex = events.size - 1
                                            isPlaying = false
                                        },
                                        enabled = currentEventIndex < events.size - 1,
                                        modifier = Modifier.size(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.1f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("⏭️", fontSize = 24.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Game Board Display
                    item {
                        if (currentEventIndex < events.size) {
                            val currentEvent = events[currentEventIndex]
                            val gameState: GameState? = try {
                                val stateElement = currentEvent.payload["state"]
                                if (stateElement != null) {
                                    // Convert JsonElement to string and decode to GameState
                                    val stateJson = stateElement.toString()
                                    gameJson.decodeFromString(GameState.serializer(), stateJson)
                                } else null
                            } catch (e: Exception) {
                                println("📱 ReplayScreen: Failed to parse game state: ${e.message}")
                                e.printStackTrace()
                                null
                            }

                            // Game Board Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "🎮 وضعیت بازی",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Display appropriate game UI
                                    when (gameState) {
                                        is com.gamehub.games.tictactoe.TicTacToeState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactTicTacToeBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.connectfour.ConnectFourState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactConnectFourBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(7f / 6f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.uno.UnoState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactUnoBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.ludo.LudoState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactLudoBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.monopoly.MonopolyState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactMonopolyBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.chess.ChessState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactChessBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.farkle.FarkleState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactFarkleBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.esmofamil.EsmoFamilState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactEsmoFamilBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.backgammon.BackgammonState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactBackgammonBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.nard.NardState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactNardBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.abalone.AbaloneState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactAbaloneBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.spadesbaloot.SpadesBalootState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactSpadesBalootBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.othello.OthelloState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactOthelloBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.baltazar.BaltazarState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactBaltazarBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.bridge.BridgeState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactBridgeBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.checkers.CheckersState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactCheckersBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.blokus.BlokusState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactBlokusBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.yahtzee.YahtzeeState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactYahtzeeBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.hex.HexState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactHexBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.battleship.BattleshipState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactBattleshipBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        is com.gamehub.games.matchmonster.MatchMonsterState -> {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CompactMatchMonsterBoard(
                                                    state = gameState,
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .aspectRatio(1f)
                                                )
                                            }
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "نظرسازی این بازی در نسخه فعلی پشتیبانی نمی‌شود",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 14.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Current Event Details
                    item {
                        if (currentEventIndex < events.size) {
                            val currentEvent = events[currentEventIndex]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            currentEvent.eventType,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF6366F1).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "#${currentEvent.sequenceNumber}",
                                                color = Color(0xFF6366F1),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    if (currentEvent.playerId != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👤 بازیکن: ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                            Text(
                                                currentEvent.playerId,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🕐 زمان: ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                        Text(
                                            currentEvent.timestamp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Event List Header
                    item {
                        Text(
                            "📋 لیست رویدادها",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Event List
                    itemsIndexed(events) { index, event ->
                        val isSelected = index == currentEventIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentEventIndex = index
                                    isPlaying = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                                    ) {
                                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                            Text(
                                                "#${event.sequenceNumber}",
                                                color = if (isSelected) Color(0xFF6366F1) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            event.eventType,
                                            color = Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        )
                                        if (event.playerId != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                event.playerId,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Text("●", color = Color(0xFF6366F1), fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactTicTacToeBoard(
    state: TicTacToeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0..2) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (j in 0..2) {
                            val cell = state.grid[i][j]
                            val isX = cell != null && state.players.indexOf(cell) == 0
                            val isO = cell != null && state.players.indexOf(cell) == 1

                            val cellColor by animateColorAsState(
                                targetValue = when {
                                    isX -> Color(0xFFE53935)
                                    isO -> Color(0xFF1E88E5)
                                    else -> Color(0xFFFFFFFF)
                                },
                                animationSpec = tween(300),
                                label = "cellColor"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellColor)
                                    .border(1.5.dp, Color(0xFFBDBDBD), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isX) {
                                    Text(
                                        text = "✕",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else if (isO) {
                                    Text(
                                        text = "○",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactBlokusBoard(
    state: com.gamehub.games.blokus.BlokusState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            for (row in 0 until 20) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    for (col in 0 until 20) {
                        val cell = state.board.getOrNull(row)?.getOrNull(col)
                        val color = when (cell) {
                            com.gamehub.games.blokus.BlokusColor.RED -> Color.Red
                            com.gamehub.games.blokus.BlokusColor.BLUE -> Color.Blue
                            com.gamehub.games.blokus.BlokusColor.GREEN -> Color.Green
                            com.gamehub.games.blokus.BlokusColor.YELLOW -> Color.Yellow
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(0.5.dp)
                                .background(color, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactHexBoard(
    state: com.gamehub.games.hex.HexState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            for (i in 0 until 11) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (i * 3).dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (j in 0 until 11) {
                        val cell = state.grid[i][j]
                        val isRed = cell != null && state.players.indexOf(cell) == 0
                        val isBlue = cell != null && state.players.indexOf(cell) == 1
                        val cellColor = when {
                            isRed -> Color(0xFFEF5350)
                            isBlue -> Color(0xFF42A5F5)
                            else -> Color(0xFFCBD5E1)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(cellColor)
                                .border(
                                    width = if (cell != null) 1.dp else 0.5.dp,
                                    color = if (cell != null) {
                                        if (isRed) Color(0xFFC62828) else Color(0xFF1565C0)
                                    } else {
                                        Color(0xFF94A3B8)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("Range")
@Composable
fun CompactAbaloneBoard(
    state: com.gamehub.games.abalone.AbaloneState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Create a grid representation of abalone hex board for simplicity
            val grid = List(9) { MutableList(9) { "" } }
            state.marbles.forEach { marble ->
                // Convert axial to offset coordinates for display
                val offsetRow = marble.pos.r + 4
                val offsetCol = marble.pos.q + 4
                if (offsetRow in 0..8 && offsetCol in 0..8) {
                    grid[offsetRow][offsetCol] = if (marble.color == com.gamehub.games.abalone.AbaloneColor.BLACK) "●" else "○"
                }
            }

            for (row in 0 until 9) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (abs(row - 4) * 8).dp)
                ) {
                    for (col in 0 until 9) {
                        val q = col - 4
                        val r = row - 4
                        val s = -q - r
                        val isValid = s in -4..4
                        val cell = grid[row][col]
                        Box(
                            modifier = Modifier
                                .weight(if (isValid) 1f else 0f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .then(if (isValid) Modifier else Modifier.size(0.dp))
                                .clip(CircleShape)
                                .background(if (isValid) Color(0xFF4A4A6A) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell.isNotEmpty()) {
                                Text(
                                    text = cell,
                                    fontSize = 14.sp,
                                    color = if (cell == "●") Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactYahtzeeBoard(
    state: com.gamehub.games.yahtzee.YahtzeeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Yahtzee",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.dice.forEach { die ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (die == 0) "?" else die.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Rolls left: ${state.rollsRemaining}",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CompactBattleshipBoard(
    state: com.gamehub.games.battleship.BattleshipState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Battleship",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Show first player's target grid
            val firstPlayer = state.players.firstOrNull()
            val targetGrid = firstPlayer?.let { state.playerData[it]?.targetGrid } ?: List(10) { List(10) { com.gamehub.games.battleship.CellState.EMPTY } }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                for (row in 0 until 10) {
                    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                        for (col in 0 until 10) {
                            val cellState = targetGrid[row][col]
                            val color = when (cellState) {
                                com.gamehub.games.battleship.CellState.HIT -> Color(0xFFEF5350)
                                com.gamehub.games.battleship.CellState.MISS -> Color(0xFF42A5F5)
                                else -> Color(0xFF334155)
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactMatchMonsterBoard(
    state: com.gamehub.games.matchmonster.MatchMonsterState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Match Monster",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Show first player's board
            val firstPlayer = state.playerData.firstOrNull()
            val board = firstPlayer?.data?.board ?: List(8) { List(6) { null } }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (row in 0 until 8) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (col in 0 until 6) {
                            val tile = board[row][col]
                            val color = when (tile?.type) {
                                com.gamehub.games.matchmonster.MonsterType.FIRE -> Color(0xFFFF5722)
                                com.gamehub.games.matchmonster.MonsterType.WATER -> Color(0xFF2196F3)
                                com.gamehub.games.matchmonster.MonsterType.EARTH -> Color(0xFF4CAF50)
                                com.gamehub.games.matchmonster.MonsterType.AIR -> Color(0xFF03A9F4)
                                com.gamehub.games.matchmonster.MonsterType.DARK -> Color(0xFF212121)
                                com.gamehub.games.matchmonster.MonsterType.LIGHT -> Color(0xFFFFEB3B)
                                else -> Color(0xFF334155)
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(color, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactEsmoFamilBoard(
    state: com.gamehub.games.esmofamil.EsmoFamilState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "اسم فامیل",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF6366F1), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.currentLetter.toString(),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Round ${state.roundNumber}/${state.maxRounds}",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CompactFarkleBoard(
    state: com.gamehub.games.farkle.FarkleState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Farkle",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.dice.forEach { die ->
                    val isSelected = state.selectedDiceIds.contains(die.id)
                    val isRolled = die.state != com.gamehub.games.farkle.FarkleDiceState.IDLE
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isSelected) Color(0xFFFFC107) else Color.White,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF9800) else Color.Gray,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (!isRolled) "?" else die.value.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Turn score: ${state.turnScore}",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CompactBackgammonBoard(
    state: com.gamehub.games.backgammon.BackgammonState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5D4037))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Backgammon",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactNardBoard(
    state: com.gamehub.games.nard.NardState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5D4037))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Nard",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactSpadesBalootBoard(
    state: com.gamehub.games.spadesbaloot.SpadesBalootState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Spades/Baloot",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactBridgeBoard(
    state: com.gamehub.games.bridge.BridgeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Bridge",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactUnoBoard(
    state: com.gamehub.games.uno.UnoState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Uno",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactLudoBoard(
    state: com.gamehub.games.ludo.LudoState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Ludo",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactMonopolyBoard(
    state: com.gamehub.games.monopoly.MonopolyState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Monopoly",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CompactBaltazarBoard(
    state: com.gamehub.games.baltazar.BaltazarState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val allRows = listOf(
                listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
                listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4),
                listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 2 to 5),
                listOf(3 to 0, 3 to 1, 3 to 2, 3 to 3, 3 to 4, 3 to 5, 3 to 6),
                listOf(4 to 1, 4 to 2, 4 to 3, 4 to 4, 4 to 5, 4 to 6),
                listOf(5 to 2, 5 to 3, 5 to 4, 5 to 5, 5 to 6),
                listOf(6 to 2, 6 to 3, 6 to 4, 6 to 5, 6 to 6)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy((-4).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                allRows.forEachIndexed { rowIndex, rowCells ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.CenterHorizontally),
                        modifier = Modifier.offset(
                            x = if (rowIndex % 2 == 0) 0.dp else 16.dp
                        )
                    ) {
                        rowCells.forEach { (r, c) ->
                            val cell = state.getCell(r, c)
                            if (cell != null) {
                                val isHome = (r == 0 && c == 1) || (r == 6 && c == 5)
                                val bgColor = when {
                                    cell.owner == state.players.firstOrNull() -> Color(0xFFEF5350)
                                    cell.owner == state.players.lastOrNull() -> Color(0xFF42A5F5)
                                    cell.state == com.gamehub.games.baltazar.CellState.Closed -> Color(0xFF9E9E9E)
                                    else -> Color(0xFFE0F2F1)
                                }
                                val borderColor = when {
                                    isHome -> Color(0xFFFFD700)
                                    else -> Color(0xFF1A237E)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(HexagonShape)
                                        .background(bgColor)
                                        .border(1.5.dp, borderColor, HexagonShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cell.letter != null) {
                                        Text(
                                            cell.letter.toString(),
                                            color = if (cell.state == com.gamehub.games.baltazar.CellState.OpenNeutral) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (isHome) {
                                        Text(
                                            "👑",
                                            fontSize = 8.sp,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(bottom = 2.dp, end = 2.dp)
                                        )
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
        else -> "🎮"
    }
}
