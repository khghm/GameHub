package com.gamehub.games.soccerstriker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gamehub.games.soccerstriker.BallData
import com.gamehub.games.soccerstriker.DiscData
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.games.soccerstriker.SoccerStrikerState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun SoccerStrikerScreen(
    state: SoccerStrikerState,
    onAction: (SoccerStrikerAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalAnimation by remember { mutableStateOf(false) }
    var goalTextScale by remember { mutableStateOf(0f) }

    // Local state for animation playback
    var localAnimationFrame by remember { mutableStateOf(0) }
    var isPlayingAnimation by remember { mutableStateOf(false) }

    // 🔑 پرچم برای ردگیری لحظه‌ای که انیمیشن تمام شده اما استیت جدید نیامده!
    var isHoldingFinalAnimationFrame by remember { mutableStateOf(false) }
    var heldFinalDiscs by remember { mutableStateOf<List<DiscData>?>(null) }
    var heldFinalBall by remember { mutableStateOf<BallData?>(null) }

    // 🔑 وقتی استیت جدید آمد و در حال شبیه‌سازی نیستیم، تمام متغیرهای انیمیشن را ریست کن!
    LaunchedEffect(state.isSimulating, state.discs.size) {
        if (!state.isSimulating) {
            isHoldingFinalAnimationFrame = false
            heldFinalDiscs = null
            heldFinalBall = null
            localAnimationFrame = 0
            isPlayingAnimation = false
        }
    }

    // Track current display frame
    val displayFrameIndex = if (state.isSimulating && state.animationFrames.isNotEmpty()) {
        localAnimationFrame
    } else {
        0
    }

    // Get current discs and ball for display
    val (displayDiscs, displayBall) = when {
        // 1. اگر در حال پخش انیمیشن هستیم
        state.isSimulating && state.animationFrames.isNotEmpty() && displayFrameIndex < state.animationFrames.size -> {
            val frame = state.animationFrames[displayFrameIndex]
            frame.discs to frame.ball
        }
        // 2. اگر در حالت "ماندن فریم آخر" هستیم (تا استیت جدید بیاید)
        isHoldingFinalAnimationFrame && heldFinalDiscs != null && heldFinalBall != null -> {
            heldFinalDiscs!! to heldFinalBall!!
        }
        // 3. در حالت عادی، فقط استیت سرور را نشان بده! هیچ استیت محلی نباید نمایش داده بشود!
        else -> {
            state.discs to state.ball
        }
    }

    // Goal animation
    LaunchedEffect(state.scoreRed, state.scoreBlue) {
        if (state.scoreRed > 0 || state.scoreBlue > 0) {
            showGoalAnimation = true
            goalTextScale = 0f
            for (i in 0..25) {
                goalTextScale = i / 25f
                delay(12)
            }
            delay(1200)
            showGoalAnimation = false
        }
    }



    // Local physics animation playback with accurate frame timing
    LaunchedEffect(state.isSimulating, state.animationFrames.size) {
        if (state.isSimulating && state.animationFrames.isNotEmpty() && !isPlayingAnimation) {
            isPlayingAnimation = true
            localAnimationFrame = 0

            val startTime = System.nanoTime()
            val frameDuration = 1_000_000_000L / 30 // ~33.33ms per frame

            // Play ALL frames (including the last one)
            while (localAnimationFrame < state.animationFrames.size) {
                val targetTime = startTime + (localAnimationFrame + 1) * frameDuration
                val currentTime = System.nanoTime()
                if (currentTime < targetTime) {
                    delay((targetTime - currentTime) / 1_000_000) // convert to ms
                }
                localAnimationFrame++
            }

            // 🔑 حالا انیمیشن تمام شده، فریم آخر را نگه دار تا استیت جدید بیاید!
            val lastFrame = state.animationFrames.last()
            heldFinalDiscs = lastFrame.discs
            heldFinalBall = lastFrame.ball
            isHoldingFinalAnimationFrame = true

            // Send completion
            onAction(SoccerStrikerAction.AnimationComplete)
            isPlayingAnimation = false
        }
    }

    // 🔑 وقتی استیت جدید آمد (isSimulating false شد)، نگهداری فریم آخر را تمام کن
    LaunchedEffect(state.isSimulating) {
        if (!state.isSimulating) {
            isHoldingFinalAnimationFrame = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12121A)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Scoreboard(
            scoreRed = state.scoreRed,
            scoreBlue = state.scoreBlue,
            currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .shadow(16.dp, RoundedCornerShape(20.dp))
        ) {
            // Create a copy of state with display values
            val displayState = remember(state, displayDiscs, displayBall) {
                state.copy(
                    discs = displayDiscs,
                    ball = displayBall
                )
            }
            GameField(
                state = displayState,
                onAction = onAction,
                showGoalAnimation = showGoalAnimation,
                goalTextScale = goalTextScale
            )

            // Only show a subtle loading indicator during actual physics calculation
            if (state.isSimulating && state.animationFrames.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFFD700),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        GameHint(
            gameOver = state.gameOver,
            isSimulating = state.isSimulating,
            currentTeam = if (state.currentPlayerIndex == 0) "قرمز" else "آبی"
        )
    }

    if (state.gameOver) {
        GameOverOverlay(
            winner = state.winner?.let { if (state.players.indexOf(it) == 0) "قرمز" else "آبی" } ?: "مساوی",
            scoreRed = state.scoreRed,
            scoreBlue = state.scoreBlue,
            onReset = { onAction(SoccerStrikerAction.Reset) }
        )
    }
}

// Scoreboard, TeamScore, GameHint بدون تغییر می‌مانند (همانند قبل)

@Composable
fun Scoreboard(
    scoreRed: Int,
    scoreBlue: Int,
    currentTeam: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2A)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamScore(
                teamName = "قرمز",
                score = scoreRed,
                isActive = currentTeam == "red",
                color = Color(0xFFFF3A30)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "نوبت",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFD700),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        if (currentTeam == "red") "قرمز" else "آبی",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }
            }

            TeamScore(
                teamName = "آبی",
                score = scoreBlue,
                isActive = currentTeam == "blue",
                color = Color(0xFF007AFF)
            )
        }
    }
}

@Composable
fun TeamScore(teamName: String, score: Int, isActive: Boolean, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            teamName,
            fontSize = 16.sp,
            color = if (isActive) color else Color.Gray,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(if (isActive) 72.dp else 60.dp),
            shadowElevation = if (isActive) 12.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    score.toString(),
                    fontSize = if (isActive) 32.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GameHint(gameOver: Boolean, isSimulating: Boolean, currentTeam: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2A)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        val text = when {
            gameOver -> "بازی تمام شد!"
            isSimulating -> "صبر کنید..."
            else -> "دیسک تیم $currentTeam را بکشید و رها کنید!"
        }

        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            textAlign = TextAlign.Center,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GameField(
    state: SoccerStrikerState,
    onAction: (SoccerStrikerAction) -> Unit,
    showGoalAnimation: Boolean,
    goalTextScale: Float
) {
    val textMeasurer = rememberTextMeasurer()
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var selectedDiscId by remember { mutableStateOf<String?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state.discs, state.currentPlayerIndex, state.gameOver, state.isSimulating) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startPos = down.position
                    var currentPos = startPos
                    val scaleX = size.width / SoccerStrikerState.FIELD_WIDTH
                    val scaleY = size.height / SoccerStrikerState.FIELD_HEIGHT
                    val currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"

                    // Prevent dragging if game over or simulating (including animation playback)
                    if (state.gameOver || state.isSimulating) {
                        return@awaitEachGesture
                    }

                    // Reset selection at the start of each gesture
                    selectedDiscId = null

                    // Find the disc under the touch point
                    var foundDisc: DiscData? = null
                    for (disc in state.discs) {
                        if (disc.team != currentTeam) continue
                        val center = Offset(disc.x * scaleX, disc.y * scaleY)
                        val distance = (startPos - center).getDistance()
                        // Reduce detection radius to avoid accidental selection
                        if (distance <= SoccerStrikerState.DISC_RADIUS * scaleX * 2.5) {
                            foundDisc = disc
                            break
                        }
                    }

                    if (foundDisc == null) {
                        return@awaitEachGesture
                    }

                    val discId = foundDisc.id
                    // Set selection immediately
                    selectedDiscId = discId
                    dragStart = startPos
                    dragEnd = startPos
                    isDragging = true

                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.isEmpty()) break
                        val change = changes.first()
                        val position = change.position

                        currentPos = position
                        dragEnd = position

                        change.consume()
                        if (event.changes.all { !it.pressed }) break
                    }

                    val dx = startPos.x - currentPos.x
                    val dy = startPos.y - currentPos.y
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance > 10f) {
                        val angle = atan2(dy, dx)
                        val power = (distance / 140f).coerceIn(0f, 1f)
                        // ارسال power بدون ضرب در ۱۰
                        onAction(SoccerStrikerAction.FlickDisc(discId, angle, power))
                    }

                    isDragging = false
                    dragStart = null
                    dragEnd = null
                    selectedDiscId = null
                }
            }
    ) {
        val scaleX = size.width / SoccerStrikerState.FIELD_WIDTH
        val scaleY = size.height / SoccerStrikerState.FIELD_HEIGHT

        drawGrassField(size)
        drawFieldMarkings(size, scaleX, scaleY)
        drawGoalNets(size, scaleX, scaleY)

        state.discs.forEach { disc ->
            drawDisc(
                disc = disc,
                scaleX = scaleX,
                scaleY = scaleY,
                isSelected = selectedDiscId == disc.id,
                textMeasurer = textMeasurer
            )
        }

        drawBall(state.ball, scaleX, scaleY)

        if (isDragging && dragStart != null && dragEnd != null && selectedDiscId != null) {
            val start = dragStart!!
            val end = dragEnd!!
            val distance = (start - end).getDistance()
            if (distance > 5f) {
                val power = (distance / 140f).coerceIn(0f, 1f)
                drawDragIndicator(start, end, power)
            }
        }

        if (showGoalAnimation) {
            drawGoalAnimation(size, textMeasurer, goalTextScale)
        }
    }
}

private fun DrawScope.drawGrassField(size: Size) {
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A5F3A),
            Color(0xFF2E8B57),
            Color(0xFF3CA55C),
            Color(0xFF2E8B57),
            Color(0xFF1A5F3A)
        ),
        startY = 0f,
        endY = size.height
    )
    drawRect(brush = gradient, size = size)

    val stripeSpacing = 80f
    var offset = 0f
    while (offset < size.height) {
        drawRect(
            color = Color.White.copy(alpha = 0.03f),
            topLeft = Offset(0f, offset),
            size = Size(size.width, stripeSpacing / 2)
        )
        offset += stripeSpacing
    }

    val lightBeams = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        ),
        center = Offset(size.width / 2, size.height / 2),
        radius = size.width * 0.8f
    )
    drawRect(brush = lightBeams, size = size)
}

private fun DrawScope.drawFieldMarkings(size: Size, scaleX: Float, scaleY: Float) {
    val lineColor = Color.White.copy(alpha = 0.85f)
    val lineWidth = 4f

    drawRect(
        color = lineColor,
        style = Stroke(width = lineWidth),
        size = size
    )

    drawLine(
        color = lineColor,
        start = Offset(0f, size.height / 2),
        end = Offset(size.width, size.height / 2),
        strokeWidth = lineWidth
    )

    drawCircle(
        color = lineColor,
        radius = 80f * scaleX,
        center = Offset(size.width / 2, size.height / 2),
        style = Stroke(width = 3f)
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = 8f * scaleX,
        center = Offset(size.width / 2, size.height / 2)
    )

    val goalAreaWidth = SoccerStrikerState.GOAL_WIDTH * 1.8f * scaleX
    val goalAreaHeight = 90f * scaleY
    val leftX = (size.width - goalAreaWidth) / 2

    drawRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(leftX, size.height - goalAreaHeight),
        size = Size(goalAreaWidth, goalAreaHeight),
        style = Stroke(width = 3f)
    )

    drawRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(leftX, 0f),
        size = Size(goalAreaWidth, goalAreaHeight),
        style = Stroke(width = 3f)
    )

    val penaltySpotRadius = 12f * scaleX
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = penaltySpotRadius,
        center = Offset(size.width / 2, size.height - goalAreaHeight - 60f * scaleY)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = penaltySpotRadius,
        center = Offset(size.width / 2, goalAreaHeight + 60f * scaleY)
    )
}

private fun DrawScope.drawGoalNets(size: Size, scaleX: Float, scaleY: Float) {
    val goalWidth = SoccerStrikerState.GOAL_WIDTH * scaleX
    val goalLeft = (size.width - goalWidth) / 2
    val netDepth = 40f * scaleY
    val netColor = Color(0x33FFFFFF)
    val netLineColor = Color(0x66FFFFFF)

    drawRect(
        color = netColor,
        topLeft = Offset(goalLeft, size.height - netDepth),
        size = Size(goalWidth, netDepth)
    )

    val gridSpacing = 15f * scaleX
    var x = goalLeft
    while (x <= goalLeft + goalWidth + 0.1f) {
        drawLine(
            color = netLineColor,
            start = Offset(x, size.height - netDepth),
            end = Offset(x, size.height),
            strokeWidth = 1.5f
        )
        x += gridSpacing
    }

    var y = size.height - netDepth
    while (y <= size.height + 0.1f) {
        drawLine(
            color = netLineColor,
            start = Offset(goalLeft, y),
            end = Offset(goalLeft + goalWidth, y),
            strokeWidth = 1.5f
        )
        y += gridSpacing
    }

    drawRect(
        color = netColor,
        topLeft = Offset(goalLeft, 0f),
        size = Size(goalWidth, netDepth)
    )

    x = goalLeft
    while (x <= goalLeft + goalWidth + 0.1f) {
        drawLine(
            color = netLineColor,
            start = Offset(x, 0f),
            end = Offset(x, netDepth),
            strokeWidth = 1.5f
        )
        x += gridSpacing
    }

    y = 0f
    while (y <= netDepth + 0.1f) {
        drawLine(
            color = netLineColor,
            start = Offset(goalLeft, y),
            end = Offset(goalLeft + goalWidth, y),
            strokeWidth = 1.5f
        )
        y += gridSpacing
    }
}

private fun DrawScope.drawDisc(
    disc: DiscData,
    scaleX: Float,
    scaleY: Float,
    isSelected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val center = Offset(disc.x * scaleX, disc.y * scaleY)
    val radius = SoccerStrikerState.DISC_RADIUS * scaleX
    val isRed = disc.team == "red"

    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = radius,
        center = center + Offset(6f * scaleX, 8f * scaleY)
    )

    if (isSelected) {
        val glowColors = if (isRed) {
            listOf(Color.Transparent, Color(0xFFFF3A30).copy(alpha = 0.6f), Color.Transparent)
        } else {
            listOf(Color.Transparent, Color(0xFF007AFF).copy(alpha = 0.6f), Color.Transparent)
        }
        val glowGradient = Brush.radialGradient(
            colors = glowColors,
            center = center,
            radius = radius * 2.5f
        )
        drawCircle(brush = glowGradient, radius = radius * 2.5f, center = center)

        drawCircle(
            color = Color.Yellow.copy(alpha = 0.8f),
            radius = radius + 10f * scaleX,
            center = center,
            style = Stroke(width = 5f * scaleX)
        )
    }

    val baseColors = if (isRed) {
        listOf(Color(0xFFFF6B6B), Color(0xFFFF3A30), Color(0xFFB31212), Color(0xFF8B0000))
    } else {
        listOf(Color(0xFF55ACFF), Color(0xFF007AFF), Color(0xFF0051AA), Color(0xFF003366))
    }

    val discGradient = Brush.radialGradient(
        colors = baseColors,
        center = center - Offset(radius * 0.3f, radius * 0.3f),
        radius = radius
    )
    drawCircle(brush = discGradient, radius = radius, center = center)

    val highlightGradient = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
        center = center - Offset(radius * 0.4f, radius * 0.4f),
        radius = radius * 0.6f
    )
    drawCircle(brush = highlightGradient, radius = radius * 0.6f, center = center)

    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = radius * 0.85f,
        center = center,
        style = Stroke(width = 3f * scaleX)
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.2f),
        radius = radius * 0.5f,
        center = center,
        style = Stroke(width = 2f * scaleX)
    )

    val num = disc.id.substringAfter("-")
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = (radius * 0.75f).coerceAtLeast(10f).sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f,
            offset = Offset(2f, 2f)
        )
    )
    val textLayout = textMeasurer.measure(text = num, style = textStyle)
    drawText(
        textMeasurer = textMeasurer,
        text = num,
        topLeft = Offset(
            center.x - textLayout.size.width / 2f,
            center.y - textLayout.size.height / 2f
        ),
        style = textStyle
    )
}

private fun DrawScope.drawBall(ball: BallData, scaleX: Float, scaleY: Float) {
    val center = Offset(ball.x * scaleX, ball.y * scaleY)
    val radius = SoccerStrikerState.BALL_RADIUS * scaleX

    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = radius,
        center = center + Offset(5f * scaleX, 7f * scaleY)
    )

    val ballGradient = Brush.radialGradient(
        colors = listOf(Color.White, Color(0xFFE8E8E8), Color(0xFFC0C0C0)),
        center = center - Offset(radius * 0.3f, radius * 0.3f),
        radius = radius
    )
    drawCircle(brush = ballGradient, radius = radius, center = center)

    val highlightGradient = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
        center = center - Offset(radius * 0.35f, radius * 0.35f),
        radius = radius * 0.5f
    )
    drawCircle(brush = highlightGradient, radius = radius * 0.5f, center = center)

    val pentagonCount = 12
    val angleStep = (2 * PI / pentagonCount).toFloat()
    val pentagonRadius = radius * 0.28f

    for (i in 0 until pentagonCount) {
        val angle = i * angleStep
        val distance = radius * 0.55f
        val cx = center.x + distance * cos(angle)
        val cy = center.y + distance * sin(angle)

        val path = Path().apply {
            for (j in 0..4) {
                val ja = j * (2 * PI / 5).toFloat() - PI.toFloat() / 2
                val px = cx + pentagonRadius * cos(ja)
                val py = cy + pentagonRadius * sin(ja)
                if (j == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(path = path, color = Color(0xFF1a1a1a))
    }

    val centerPath = Path().apply {
        for (j in 0..4) {
            val ja = j * (2 * PI / 5).toFloat() - PI.toFloat() / 2
            val px = center.x + pentagonRadius * 0.8f * cos(ja)
            val py = center.y + pentagonRadius * 0.8f * sin(ja)
            if (j == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path = centerPath, color = Color(0xFF1a1a1a))
}

private fun DrawScope.drawDragIndicator(start: Offset, end: Offset, power: Float) {
    val mainColor = when {
        power < 0.4f -> Color(0xFF00E676)
        power < 0.7f -> Color(0xFFFFEB3B)
        else -> Color(0xFFF44336)
    }

    val direction = (start - end).normalize()
    val arrowLength = (100f + power * 150f)
    val arrowStart = start
    val arrowEnd = start + direction * arrowLength

    val glowGradient = Brush.radialGradient(
        colors = listOf(mainColor.copy(alpha = 0.4f), Color.Transparent),
        center = arrowStart,
        radius = 60f
    )
    drawCircle(brush = glowGradient, radius = 60f, center = arrowStart)

    val strokeWidth = 8f + power * 8f
    val linePath = Path().apply {
        moveTo(arrowStart.x, arrowStart.y)
        lineTo(arrowEnd.x, arrowEnd.y)
    }

    drawPath(
        path = linePath,
        color = mainColor.copy(alpha = 0.25f),
        style = Stroke(width = strokeWidth + 20f, cap = StrokeCap.Round)
    )

    drawPath(
        path = linePath,
        color = mainColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    val arrowAngle = atan2(arrowEnd.y - arrowStart.y, arrowEnd.x - arrowStart.x)
    val arrowSize = 25f + power * 20f
    val arrowPath = Path().apply {
        moveTo(arrowEnd.x, arrowEnd.y)
        lineTo(
            arrowEnd.x - arrowSize * cos(arrowAngle - PI.toFloat() / 6),
            arrowEnd.y - arrowSize * sin(arrowAngle - PI.toFloat() / 6)
        )
        moveTo(arrowEnd.x, arrowEnd.y)
        lineTo(
            arrowEnd.x - arrowSize * cos(arrowAngle + PI.toFloat() / 6),
            arrowEnd.y - arrowSize * sin(arrowAngle + PI.toFloat() / 6)
        )
    }

    drawPath(
        path = arrowPath,
        color = mainColor,
        style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round)
    )

    val powerText = "${(power * 100).toInt()}%"
}

private fun Offset.normalize(): Offset {
    val length = getDistance()
    return if (length > 0) this / length else Offset.Zero
}

private fun DrawScope.drawGoalAnimation(
    size: Size,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    scale: Float
) {
    if (scale <= 0f) return

    val glowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFFFFD700).copy(alpha = 0.6f * scale),
            Color.Transparent
        ),
        center = Offset(size.width / 2, size.height / 2),
        radius = size.width * 0.8f
    )
    drawRect(brush = glowGradient, size = size)

    val emojis = listOf("⚽", "🎉", "🏆", "⭐", "👏")
    val textStyle = TextStyle(
        color = Color(0xFFFFD700),
        fontSize = (60 * scale).sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            blurRadius = 20f,
            offset = Offset(4f, 4f)
        )
    )

    val mainText = "GOOOAL!"
    val mainLayout = textMeasurer.measure(text = mainText, style = textStyle)
    drawText(
        textMeasurer = textMeasurer,
        text = mainText,
        topLeft = Offset(
            size.width / 2 - mainLayout.size.width / 2,
            size.height / 2 - mainLayout.size.height / 2 - 30f
        ),
        style = textStyle
    )

    val emojiText = emojis.random()
    val emojiStyle = TextStyle(
        fontSize = (50 * scale).sp,
        textAlign = TextAlign.Center
    )
    val emojiLayout = textMeasurer.measure(text = emojiText, style = emojiStyle)
    drawText(
        textMeasurer = textMeasurer,
        text = emojiText,
        topLeft = Offset(
            size.width / 2 - emojiLayout.size.width / 2,
            size.height / 2 - emojiLayout.size.height / 2 + 50f
        ),
        style = emojiStyle
    )
}

@Composable
fun GameOverOverlay(winner: String, scoreRed: Int, scoreBlue: Int, onReset: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2A)
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "🏆 بازی تمام شد!",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2A2A3A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "برنده",
                            fontSize = 18.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "$winner!",
                            fontSize = 42.sp,
                            color = if (winner == "قرمز") Color(0xFFFF3A30) else Color(0xFF007AFF),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "قرمز",
                            color = Color(0xFFFF3A30),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF3A30),
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                scoreRed.toString(),
                                modifier = Modifier.padding(20.dp),
                                fontSize = 36.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        "-",
                        color = Color.Gray,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "آبی",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF007AFF),
                            shadowElevation = 8.dp
                        ) {
                            Text(
                                scoreBlue.toString(),
                                modifier = Modifier.padding(20.dp),
                                fontSize = 36.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(8.dp)
                ) {
                    Text(
                        "🔄 بازی مجدد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
