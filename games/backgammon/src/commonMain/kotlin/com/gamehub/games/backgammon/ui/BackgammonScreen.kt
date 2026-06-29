package com.gamehub.games.backgammon.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.backgammon.*
import com.gamehub.shared.graphics.DebugOverlay
import com.gamehub.shared.graphics.DefaultTimeProvider
import com.gamehub.shared.graphics.DeviceTierDetector
import com.gamehub.shared.graphics.TimeProvider
import com.gamehub.shared.graphics.animation.bounceClick
import com.gamehub.shared.graphics.animation.pulse
import com.gamehub.shared.graphics.effects.borderGlow
import com.gamehub.shared.graphics.particles.*
import com.gamehub.shared.graphics.theme.ThemeManager
import com.gamehub.shared.haptics.HapticFeedbackManager
import com.gamehub.shared.haptics.HapticType
import com.gamehub.shared.haptics.NoopHapticFeedbackManager
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun BackgammonScreen(
    state: BackgammonState,
    onAction: (BackgammonAction) -> Unit,
    hapticManager: HapticFeedbackManager = NoopHapticFeedbackManager(),
    timeProvider: TimeProvider = DefaultTimeProvider(),
    modifier: Modifier = Modifier
) {
    // تشخیص سطح دستگاه برای بهینه‌سازی
    val isLowTier = DeviceTierDetector.isLowTier()
    val isHighTier = DeviceTierDetector.isHighTier()
    val animationDurationScale = DeviceTierDetector.getAnimationDurationScale()
    val maxParticles = DeviceTierDetector.getMaxParticleCount()
    val shouldEnableHeavyEffects = DeviceTierDetector.shouldEnableHeavyEffects()

    // No need for local showDebugOverlay anymore; DebugOverlay handles its own visibility

    // دریافت تم فعلی
    val graphicsSpec = ThemeManager.currentSpec.value
    val boardPrimaryColor = graphicsSpec.primaryColor
    val boardSecondaryColor = graphicsSpec.secondaryColor

    val isMyTurn by remember { derivedStateOf { state.currentPlayer != null && !state.gameOver } }
    val playerColor by remember(state.currentPlayer) {
        derivedStateOf {
            state.currentPlayer?.let { BackgammonRules.getPlayerColor(state, it) }
        }
    }
    val validMoves by remember(state, state.dice, state.currentPlayer) {
        derivedStateOf {
            playerColor?.let {
                BackgammonRules.getValidMoves(state, it, state.dice)
            } ?: emptyList()
        }
    }

    // انتخاب مهره و تاس
    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    var selectedDieIndex by remember { mutableStateOf<Int?>(null) }
    val validMovesForSelection by remember(selectedPoint, selectedDieIndex, validMoves) {
        derivedStateOf {
            validMoves
                .filter { it.first == selectedPoint }
                .map { it.second }
        }
    }

    // انیمیشن حرکت مهره
    var animatingMove by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val animatedCheckerPos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val animatedCheckerTarget = remember { mutableStateOf(Offset.Zero) }
    var checkerToAnimate by remember { mutableStateOf<BackgammonColor?>(null) }
    var boardSizeForAnimation by remember { mutableStateOf(Pair(0f, 0f)) }

    LaunchedEffect(animatingMove, boardSizeForAnimation) {
        if (animatingMove != null && checkerToAnimate != null && boardSizeForAnimation.first > 0) {
            val (w, h) = boardSizeForAnimation
            val fromPos = if (animatingMove!!.first == 0) {
                if (checkerToAnimate == BackgammonColor.WHITE) Offset(w / 2f, h * 0.65f) else Offset(w / 2f, h * 0.35f)
            } else {
                getPointCoordinates(animatingMove!!.first, w, h)
            }
            val toPos = if (animatingMove!!.second == 25) {
                if (checkerToAnimate == BackgammonColor.WHITE) Offset(w * 0.95f, h * 0.25f) else Offset(w * 0.05f, h * 0.75f)
            } else {
                getPointCoordinates(animatingMove!!.second, w, h)
            }
            animatedCheckerTarget.value = toPos
            animatedCheckerPos.snapTo(fromPos)
            animatedCheckerPos.animateTo(
                targetValue = toPos,
                animationSpec = tween(
                    durationMillis = (300 * animationDurationScale).toInt(),
                    easing = FastOutSlowInEasing
                )
            )
            animatingMove = null
            checkerToAnimate = null
        }
    }

    // سیستم پارتیکل
    val particleSystem = remember { ParticleSystem(maxParticles, timeProvider) }
    var showWinParticles by remember { mutableStateOf(false) }

    LaunchedEffect(state.gameOver, state.borneOffWhite, state.borneOffBlack) {
        if (state.gameOver && (state.borneOffWhite == 15 || state.borneOffBlack == 15)) {
            showWinParticles = true
            particleSystem.emitBurst(
                centerX = 0.5f, centerY = 0.5f,
                count = if (isHighTier) 60 else 30,
                colors = listOf(Color.Yellow, Color(0xFFFFD700), Color(0xFFFFA500)),
                minSpeed = 100f, maxSpeed = 400f,
                minLife = 0.8f, maxLife = 1.5f,
                minSize = 6f, maxSize = 16f,
                isNormalized = true,
                shape = ParticleShape.Circle
            )
            delay(3000)
            showWinParticles = false
            particleSystem.clear()
        }
    }

    // افکت برخورد (Hit)
    fun triggerHitEffect(x: Float, y: Float) {
        if (isLowTier) return
        particleSystem.emitBurst(
            centerX = x, centerY = y,
            count = 20,
            colors = listOf(Color.Red, Color(0xFFFF4500), Color.White),
            minSpeed = 50f, maxSpeed = 200f,
            minLife = 0.3f, maxLife = 0.8f,
            minSize = 4f, maxSize = 10f
        )
    }

    // افکت Bear Off
    fun triggerBearOffEffect(x: Float, y: Float) {
        if (isLowTier) return
        particleSystem.emitBurst(
            centerX = x, centerY = y,
            count = 15,
            colors = listOf(Color(0xFFFFD700), Color(0xFFDAA520), Color.White),
            minSpeed = 80f, maxSpeed = 250f,
            minLife = 0.5f, maxLife = 1.0f,
            minSize = 5f, maxSize = 12f
        )
    }

    // انیمیشن ورود صفحه
    val screenAlpha = remember { Animatable(0f) }
    val screenScale = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) {
        screenAlpha.animateTo(1f, tween(300))
        screenScale.animateTo(1f, spring(stiffness = 300f))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = screenAlpha.value
                scaleX = screenScale.value
                scaleY = screenScale.value
            }
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // کارت نوبت با پالس
            val pulseModifier = if (isMyTurn) Modifier.pulse(targetScale = 1.02f, durationMs = 800) else Modifier
            Card(
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                    .then(pulseModifier),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMyTurn) graphicsSpec.primaryColor else graphicsSpec.surfaceColor
                )
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (state.gameOver) "🎮 بازی تمام شد!" else if (isMyTurn) "⚫ نوبت شما!" else "⏳ منتظر حریف...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مهره‌های بیرون: ⚪ ${state.borneOffWhite} | ⚫ ${state.borneOffBlack}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (playerColor == BackgammonColor.WHITE) Color.White else Color.Black
                            )
                            .border(3.dp, if (isMyTurn) Color(0xFFFFD600) else Color.Gray, CircleShape)
                            .bounceClick { },
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            Spacer(Modifier.height(12.dp))

            // تخته بازی با افکت درخشش حاشیه
            Card(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp))
                    .borderGlow(
                        color = if (isMyTurn) graphicsSpec.accentColor else Color.Transparent,
                        radius = 10.dp,
                        borderWidth = 2.dp,
                        alpha = 0.6f
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6D4C41))
            ) {
                Box(
                    Modifier.fillMaxSize().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                boardSizeForAnimation = Pair(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                            }
                            .pointerInput(isMyTurn, state, selectedPoint, selectedDieIndex, validMoves, validMovesForSelection) {
                                detectTapGestures { tapOffset ->
                                    if (!isMyTurn || animatingMove != null) return@detectTapGestures
                                    val w = this.size.width.toFloat()
                                    val h = this.size.height.toFloat()
                                    val hit = findTappedPoint(
                                        tapOffset = tapOffset,
                                        boardWidth = w,
                                        boardHeight = h,
                                        state = state,
                                        validMoves = validMoves,
                                        validMovesForSelection = validMovesForSelection,
                                        selectedPoint = selectedPoint,
                                        playerColor = playerColor
                                    )
                                    if (hit != null) {
                                        handleTap(
                                            hitInfo = hit,
                                            state = state,
                                            selectedPoint = selectedPoint,
                                            selectedDieIndex = selectedDieIndex,
                                            onSelectPoint = { selectedPoint = it },
                                            onClearSelection = { selectedPoint = null; selectedDieIndex = null },
                                            validMovesForSelection = validMovesForSelection,
                                            onMakeMove = { from, to, die ->
                                                // قبل از اعمال حرکت، افکت برخورد/بیرون زدن را در صورت نیاز نمایش بده
                                                val isHit = (to in 1..24 && state.points[to].isBlot && state.points[to].owner == playerColor?.opponent())
                                                val isBearOff = (to == 25)
                                                if (isHit) {
                                                    val pos = getPointCoordinates(to, w, h)
                                                    triggerHitEffect(pos.x, pos.y)
                                                    hapticManager.performHapticFeedback(HapticType.Error)
                                                }
                                                if (isBearOff) {
                                                    val bearOffPos = if (playerColor == BackgammonColor.WHITE) Offset(w * 0.95f, h * 0.25f) else Offset(w * 0.05f, h * 0.75f)
                                                    triggerBearOffEffect(bearOffPos.x, bearOffPos.y)
                                                    hapticManager.performHapticFeedback(HapticType.Success)
                                                }
                                                hapticManager.performHapticFeedback(HapticType.Click)
                                                // شروع انیمیشن حرکت مهره
                                                animatingMove = Pair(from, to)
                                                checkerToAnimate = playerColor
                                                // اعمال حرکت پس از انیمیشن
                                                onAction(BackgammonAction.Move(from, to, die))
                                                selectedPoint = null
                                                selectedDieIndex = null
                                            },
                                            playerColor = playerColor
                                        )
                                    }
                                }
                            }
                    ) {
                        drawBoardBackground(boardPrimaryColor, boardSecondaryColor)
                        drawBoardPoints()
                        drawAllCheckers(
                            state = state,
                            selectedPoint = selectedPoint,
                            validMoves = validMovesForSelection,
                            playerColor = playerColor,
                            animatingChecker = if (animatingMove != null && checkerToAnimate != null) Pair(checkerToAnimate!!, animatingMove!!.first) else null,
                            animatedOffset = animatedCheckerPos.value,
                            shouldEnableHeavyEffects = shouldEnableHeavyEffects,
                            boardSize = size
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // بخش تاس و کنترل
            Card(
                Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = graphicsSpec.surfaceVariantColor)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.diceRolled) {
                        Text("🎲 تاس‌ها:", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            state.dice.forEachIndexed { index, die ->
                                val isSelected = selectedDieIndex == index
                                val dieScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.25f else 1f,
                                    animationSpec = spring(stiffness = 400f),
                                    label = "dieScale"
                                )
                                val rotation by animateFloatAsState(
                                    targetValue = if (state.diceRolled && !isLowTier) 360f else 0f,
                                    animationSpec = tween(400, easing = EaseOutBack),
                                    label = "dieRotation"
                                )
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .size(60.dp)
                                        .scale(dieScale)
                                        .graphicsLayer { rotationZ = rotation }
                                        .shadow(elevation = if (isSelected) 12.dp else 6.dp, shape = RoundedCornerShape(12.dp))
                                        .border(width = if (isSelected) 4.dp else 2.dp, color = if (isSelected) graphicsSpec.accentColor else Color(0xFFBDBDBD), shape = RoundedCornerShape(12.dp))
                                        .bounceClick { selectedDieIndex = if (isSelected) null else index },
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) graphicsSpec.accentColor.copy(alpha = 0.3f) else Color.White)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = die.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { selectedPoint = null; selectedDieIndex = null },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF757575))
                            ) {
                                Text("❌ لغو", fontSize = 16.sp, color = Color.White)
                            }
                            Button(
                                onClick = { onAction(BackgammonAction.EndTurn); selectedPoint = null; selectedDieIndex = null },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = graphicsSpec.accentColor)
                            ) {
                                Text("✓ پایان نوبت", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (isMyTurn) {
                        Button(
                            onClick = {
                                hapticManager.performHapticFeedback(HapticType.HeavyClick)
                                onAction(BackgammonAction.RollDice)
                            },
                            Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = graphicsSpec.primaryColor),
                            elevation = ButtonDefaults.buttonElevation(12.dp)
                        ) {
                            Text("🎲", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("پرتاب تاس", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // پارتیکل‌ها (اثر پایان بازی)
        if (showWinParticles) {
            ParticleEffect(particleSystem = particleSystem, isActive = true)
        }

        // دیباگ اورلی (برای نمایش اطلاعات عملکرد) - handles its own triple-tap
        DebugOverlay()
    }
}

// ---------- توابع کمکی رسم ----------

private fun DrawScope.drawBoardBackground(primary: Color, secondary: Color) {
    drawRect(brush = Brush.linearGradient(colors = listOf(primary, secondary)))
    drawRect(
        color = Color(0xFF4E342E),
        topLeft = Offset(size.width * 0.485f, 0f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.03f, size.height)
    )
}

private fun DrawScope.drawBoardPoints() {
    val margin = size.width * 0.05f
    val usableWidth = size.width - 2 * margin
    val pointWidth = usableWidth / 12f
    val pointHeight = size.height * 0.42f
    val topY = size.height * 0.04f
    val bottomY = size.height * 0.96f - pointHeight

    for (i in 0..11) {
        val x = margin + i * pointWidth
        val pointColor = if (i % 2 == 0) Color(0xFF4E342E) else Color(0xFFD7CCC8)

        // مثلث بالا
        val pathTop = Path().apply {
            moveTo(x, topY)
            lineTo(x + pointWidth, topY)
            lineTo(x + pointWidth / 2f, topY + pointHeight)
            close()
        }
        drawPath(pathTop, pointColor)

        // مثلث پایین
        val pathBottom = Path().apply {
            moveTo(x, size.height - topY)
            lineTo(x + pointWidth, size.height - topY)
            lineTo(x + pointWidth / 2f, size.height - topY - pointHeight)
            close()
        }
        drawPath(pathBottom, pointColor)
    }
}

private fun getPointCoordinates(pointIndex: Int, boardWidth: Float, boardHeight: Float): Offset {
    val margin = boardWidth * 0.05f
    val usableWidth = boardWidth - 2 * margin
    val pointWidth = usableWidth / 12f
    val pointHeight = boardHeight * 0.42f
    val topY = boardHeight * 0.04f
    val bottomY = boardHeight * 0.96f

    return when (pointIndex) {
        in 13..18 -> {
            val i = pointIndex - 13
            Offset(margin + i * pointWidth + pointWidth / 2f, topY + pointHeight / 2f)
        }
        in 19..24 -> {
            val i = pointIndex - 19
            Offset(margin + 6 * pointWidth + i * pointWidth + pointWidth / 2f, topY + pointHeight / 2f)
        }
        in 7..12 -> {
            val i = 12 - pointIndex
            Offset(margin + i * pointWidth + pointWidth / 2f, bottomY - pointHeight / 2f)
        }
        in 1..6 -> {
            val i = 6 - pointIndex
            Offset(margin + 6 * pointWidth + i * pointWidth + pointWidth / 2f, bottomY - pointHeight / 2f)
        }
        else -> {
            println("Warning: Invalid pointIndex $pointIndex passed to getPointCoordinates! Using center.")
            Offset(boardWidth / 2f, boardHeight / 2f)
        }
    }
}

private fun DrawScope.drawChecker(
    pos: Offset,
    radius: Float,
    color: BackgammonColor,
    isSelected: Boolean,
    isValidTarget: Boolean,
    shouldEnableHeavyEffects: Boolean
) {
    // سایه
    drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = radius, center = Offset(pos.x + 2f, pos.y + 2f))

    // بدنه (گرادینت شعاعی)
    val brush = if (color == BackgammonColor.WHITE) {
        Brush.radialGradient(listOf(Color.White, Color(0xFFE0E0E0)))
    } else {
        Brush.radialGradient(listOf(Color(0xFF424242), Color.Black))
    }
    drawCircle(brush = brush, radius = radius, center = pos)

    // حاشیه
    drawCircle(
        color = if (color == BackgammonColor.WHITE) Color(0xFFBDBDBD) else Color(0xFF212121),
        radius = radius,
        center = pos,
        style = Stroke(width = 2.dp.toPx())
    )

    // برجستگی (درخشش) برای دستگاه‌های قوی
    if (shouldEnableHeavyEffects) {
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = radius * 0.5f,
            center = Offset(pos.x - radius * 0.2f, pos.y - radius * 0.2f)
        )
    }

    // هایلایت انتخاب شده
    if (isSelected) {
        drawCircle(
            color = Color(0xFFFFD600),
            radius = radius + 6.dp.toPx(),
            center = pos,
            style = Stroke(width = 4.dp.toPx())
        )
    } else if (isValidTarget) {
        drawCircle(color = Color(0x80FFD700), radius = radius * 0.9f, center = pos)
        drawCircle(color = Color(0xFFFFD700), radius = radius * 0.9f, center = pos, style = Stroke(width = 4.dp.toPx()))
    }
}

private fun DrawScope.drawAllCheckers(
    state: BackgammonState,
    selectedPoint: Int?,
    validMoves: List<Int>,
    playerColor: BackgammonColor?,
    animatingChecker: Pair<BackgammonColor, Int>?,
    animatedOffset: Offset,
    shouldEnableHeavyEffects: Boolean,
    boardSize: androidx.compose.ui.geometry.Size
) {
    val checkerSize = min(boardSize.width, boardSize.height) * 0.065f
    val w = boardSize.width
    val h = boardSize.height

    // هایلایت نقاط مجاز حرکت (پیش‌نمایش)
    for (validMoveTo in validMoves) {
        if (validMoveTo in 1..24) {
            val pos = getPointCoordinates(validMoveTo, w, h)
            drawCircle(color = Color(0x80FFD700), radius = checkerSize * 0.9f, center = pos)
            drawCircle(color = Color(0xFFFFD700), radius = checkerSize * 0.9f, center = pos, style = Stroke(width = 4.dp.toPx()))
        } else if (validMoveTo == 25 && playerColor != null) {
            val bearOffPos = if (playerColor == BackgammonColor.WHITE) Offset(w * 0.95f, h * 0.25f) else Offset(w * 0.05f, h * 0.75f)
            drawCircle(color = Color(0x80FFD700), radius = checkerSize * 1.2f, center = bearOffPos)
            drawCircle(color = Color(0xFFFFD700), radius = checkerSize * 1.2f, center = bearOffPos, style = Stroke(width = 4.dp.toPx()))
        }
    }

    // رسم مهره‌های نقاط (۱ تا ۲۴)
    for (pointIndex in 1..24) {
        val point = state.points[pointIndex]
        val pointColor = point.owner ?: continue
        val count = point.checkers.size
        val isSelected = pointIndex == selectedPoint
        val basePos = getPointCoordinates(pointIndex, w, h)
        val isTop = pointIndex >= 13

        for (i in 0 until count) {
            val yOffset = if (isTop) i * (checkerSize * 0.85f) else -i * (checkerSize * 0.85f)
            val pos = Offset(basePos.x, basePos.y + yOffset)

            // اگر این مهره در حال انیمیشن باشد، آن را در جای خود رسم نکن (بعداً جدا رسم می‌شود)
            if (animatingChecker != null && animatingChecker.second == pointIndex && animatingChecker.first == pointColor && i == 0) continue

            drawChecker(pos, checkerSize / 2f, pointColor, isSelected && i == 0, false, shouldEnableHeavyEffects)
        }
    }

    // رسم مهره‌های Bar
    val barX = w / 2f
    val barYTop = h * 0.35f
    val barYBottom = h * 0.65f

    for (i in 0 until state.barWhite) {
        val y = barYBottom + i * (checkerSize * 0.9f)
        drawChecker(Offset(barX, y), checkerSize / 2f, BackgammonColor.WHITE, selectedPoint == 0, false, shouldEnableHeavyEffects)
    }
    for (i in 0 until state.barBlack) {
        val y = barYTop - i * (checkerSize * 0.9f)
        drawChecker(Offset(barX, y), checkerSize / 2f, BackgammonColor.BLACK, selectedPoint == 0, false, shouldEnableHeavyEffects)
    }

    // رسم مهره در حال انیمیشن (اگر وجود داشته باشد)
    if (animatingChecker != null && animatedOffset != Offset.Zero) {
        drawChecker(animatedOffset, checkerSize / 2f, animatingChecker.first, false, false, shouldEnableHeavyEffects)
    }
}

// ---------- توابع کمکی برای تشخیص لمس و هندلینگ ----------
private data class CheckerHitInfo(
    val pointIndex: Int,
    val color: BackgammonColor? = null,
    val isBar: Boolean = false,
    val isValidMove: Boolean = false
)

private fun findTappedPoint(
    tapOffset: Offset,
    boardWidth: Float,
    boardHeight: Float,
    state: BackgammonState,
    validMoves: List<Pair<Int, Int>>,
    validMovesForSelection: List<Int>,
    selectedPoint: Int?,
    playerColor: BackgammonColor?
): CheckerHitInfo? {
    val checkerSize = min(boardWidth, boardHeight) * 0.065f
    val w = boardWidth
    val h = boardHeight

    // 1. بررسی نقاط مجاز حرکت (برای پایان حرکت)
    for (validMoveTo in validMovesForSelection) {
        if (validMoveTo in 1..24) {
            val pos = getPointCoordinates(validMoveTo, w, h)
            val dx = tapOffset.x - pos.x
            val dy = tapOffset.y - pos.y
            if (dx * dx + dy * dy < (checkerSize * 0.9f) * (checkerSize * 0.9f)) {
                return CheckerHitInfo(pointIndex = validMoveTo, isValidMove = true)
            }
        } else if (validMoveTo == 25 && playerColor != null) {
            val bearOffPos = if (playerColor == BackgammonColor.WHITE) Offset(w * 0.95f, h * 0.25f) else Offset(w * 0.05f, h * 0.75f)
            val dx = tapOffset.x - bearOffPos.x
            val dy = tapOffset.y - bearOffPos.y
            if (dx * dx + dy * dy < (checkerSize * 1.2f) * (checkerSize * 1.2f)) {
                return CheckerHitInfo(pointIndex = validMoveTo, isValidMove = true)
            }
        }
    }

    // 2. بررسی Bar
    val barX = w / 2f
    val barYTop = h * 0.35f
    val barYBottom = h * 0.65f

    for (i in 0 until state.barWhite) {
        val y = barYBottom + i * (checkerSize * 0.9f)
        val dx = tapOffset.x - barX
        val dy = tapOffset.y - y
        if (dx * dx + dy * dy < checkerSize * checkerSize) {
            return CheckerHitInfo(pointIndex = 0, color = BackgammonColor.WHITE, isBar = true, isValidMove = false)
        }
    }
    for (i in 0 until state.barBlack) {
        val y = barYTop - i * (checkerSize * 0.9f)
        val dx = tapOffset.x - barX
        val dy = tapOffset.y - y
        if (dx * dx + dy * dy < checkerSize * checkerSize) {
            return CheckerHitInfo(pointIndex = 0, color = BackgammonColor.BLACK, isBar = true, isValidMove = false)
        }
    }

    // 3. بررسی نقاط عادی (۱ تا ۲۴)
    for (pointIndex in 1..24) {
        val point = state.points[pointIndex]
        val pointColor = point.owner ?: continue
        val count = point.checkers.size
        val basePos = getPointCoordinates(pointIndex, w, h)
        val isTop = pointIndex >= 13

        for (i in 0 until count) {
            val yOffset = if (isTop) i * (checkerSize * 0.85f) else -i * (checkerSize * 0.85f)
            val pos = Offset(basePos.x, basePos.y + yOffset)
            val dx = tapOffset.x - pos.x
            val dy = tapOffset.y - pos.y
            if (dx * dx + dy * dy < checkerSize * checkerSize) {
                return CheckerHitInfo(pointIndex = pointIndex, color = pointColor, isBar = false, isValidMove = false)
            }
        }
    }

    return null
}

private fun handleTap(
    hitInfo: CheckerHitInfo,
    state: BackgammonState,
    selectedPoint: Int?,
    selectedDieIndex: Int?,
    onSelectPoint: (Int) -> Unit,
    onClearSelection: () -> Unit,
    validMovesForSelection: List<Int>,
    onMakeMove: (Int, Int, Int) -> Unit,
    playerColor: BackgammonColor?
) {
    if (playerColor == null) return
    val hasCheckersOnBar = if (playerColor == BackgammonColor.WHITE) state.barWhite > 0 else state.barBlack > 0

    when {
        // حرکت مجاز (نقطه مقصد) و مبدأ و تاس انتخاب شده
        hitInfo.isValidMove && selectedPoint != null && selectedDieIndex != null -> {
            val die = state.dice[selectedDieIndex]
            onMakeMove(selectedPoint, hitInfo.pointIndex, die)
        }
        // لمس مهره خودی (برای انتخاب مبدأ)
        !hitInfo.isValidMove && hitInfo.color == playerColor -> {
            // اگر مهره‌ای در Bar داریم، فقط اجازه انتخاب Bar را بده
            if (hasCheckersOnBar && !hitInfo.isBar) return
            if (selectedPoint == hitInfo.pointIndex) {
                onClearSelection()
            } else {
                onSelectPoint(hitInfo.pointIndex)
            }
        }
        // در غیر این صورت انتخاب را لغو کن
        else -> onClearSelection()
    }
}