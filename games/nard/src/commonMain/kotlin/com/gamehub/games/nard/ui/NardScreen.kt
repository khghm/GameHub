package com.gamehub.games.nard.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.nard.*
import kotlin.math.min

@Composable
fun NardScreen(
    state: NardState,
    onAction: (NardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMyTurn by remember { derivedStateOf { state.currentPlayer != null && !state.gameOver } }
    val playerColor by remember(state.currentPlayer) {
        derivedStateOf {
            state.currentPlayer?.let { NardRules.getPlayerColor(state, it) }
        }
    }
    val validMoves by remember(state, state.dice, state.currentPlayer) {
        derivedStateOf {
            playerColor?.let {
                NardRules.getValidMoves(state, it, state.dice)
            } ?: emptyList()
        }
    }

    var selectedPoint by remember { mutableStateOf<Int?>(null) }
    var selectedDieIndex by remember { mutableStateOf<Int?>(null) }
    val validMovesForSelection by remember(selectedPoint, selectedDieIndex, validMoves) {
        derivedStateOf {
            validMoves
                .filter { it.first == selectedPoint }
                .map { it.second }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4E342E),
                        Color(0xFF5D4037)
                    )
                )
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.gameOver) Color(0xFF6A1B9A)
                else if (isMyTurn) Color(0xFF2E7D32) else Color(0xFF616161)
            )
        ) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            state.gameOver -> {
                                val winText = when (state.winType) {
                                    WinType.BACKGAMMON -> "🏆 بک‌گامون! (برد ۳ برابری)"
                                    WinType.GAMMON -> "🏅 گام‌برگ! (برد ۲ برابری)"
                                    else -> "🎮 بازی تمام شد!"
                                }
                                winText
                            }
                            isMyTurn -> "⚫ نوبت شما!"
                            else -> "⏳ منتظر حریف..."
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "مهره‌های بیرون: ⚪ ${state.borneOffWhite} | ⚫ ${state.borneOffBlack} | ضریب: ${state.doublingCube.value}x",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (playerColor == NardColor.WHITE)
                                    Color.White else Color.Black
                            )
                            .border(
                                3.dp,
                                if (isMyTurn) Color(0xFFFFD700) else Color.Gray,
                                CircleShape
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8D6E63))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(
                            isMyTurn,
                            state,
                            selectedPoint,
                            selectedDieIndex,
                            validMoves,
                            validMovesForSelection
                        ) {
                            detectTapGestures { tapOffset ->
                                if (!isMyTurn) return@detectTapGestures

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
                                        onClearSelection = {
                                            selectedPoint = null
                                            selectedDieIndex = null
                                        },
                                        validMovesForSelection = validMovesForSelection,
                                        onMakeMove = { from, to, die ->
                                            onAction(NardAction.Move(from, to, die))
                                            selectedPoint = null
                                            selectedDieIndex = null
                                        },
                                        playerColor = playerColor
                                    )
                                }
                            }
                        }
                ) {
                    drawBoardBackground()
                    drawBoardPoints()
                    drawAllCheckers(
                        state = state,
                        selectedPoint = selectedPoint,
                        validMoves = validMovesForSelection,
                        playerColor = playerColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.doubleOffered && state.doubleOfferedBy != null) {
                    Text(
                        text = "پیشنهاد دوبرابر!",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { onAction(NardAction.DeclineDouble) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("رد کردن", fontSize = 16.sp, color = Color.White)
                        }
                        Button(
                            onClick = { onAction(NardAction.AcceptDouble) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("قبول کردن", fontSize = 16.sp, color = Color.White)
                        }
                    }
                } else if (state.diceRolled) {
                    Text(
                        "🎲 تاس‌ها:",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.dice.forEachIndexed { index, die ->
                            val isSelected = selectedDieIndex == index
                            val dieScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.25f else 1f,
                                animationSpec = spring(stiffness = 400f),
                                label = "dieScale"
                            )

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(60.dp)
                                    .scale(dieScale)
                                    .shadow(
                                        elevation = if (isSelected) 12.dp else 6.dp,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 4.dp else 2.dp,
                                        color = if (isSelected) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedDieIndex = if (isSelected) null else index
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFF9C4) else Color.White
                                )
                            ) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = die.toString(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedPoint = null
                                selectedDieIndex = null
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF757575)
                            )
                        ) {
                            Text("❌ لغو", fontSize = 16.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onAction(NardAction.EndTurn)
                                selectedPoint = null
                                selectedDieIndex = null
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text(
                                "✓ پایان نوبت",
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isMyTurn) {
                    if (state.canOfferDouble && !state.isCrawfordGame && (state.doublingCube.owner == null || state.doublingCube.owner == state.currentPlayer)) {
                        Button(
                            onClick = { onAction(NardAction.OfferDouble) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("2️⃣ پیشنهاد دوبرابر", fontSize = 16.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Button(
                        onClick = { onAction(NardAction.RollDice) },
                        Modifier.size(80.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        elevation = ButtonDefaults.buttonElevation(12.dp)
                    ) {
                        Text("🎲", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "پرتاب تاس",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun getPointCoordinates(pointIndex: Int, boardWidth: Float, boardHeight: Float): Offset {
    val margin = boardWidth * 0.05f
    val usableWidth = boardWidth - 2 * margin
    val pointWidth = usableWidth / 12f
    val pointHeight = boardHeight * 0.42f
    val topY = boardHeight * 0.04f
    val bottomY = boardHeight * 0.96f

    val x: Float
    val y: Float

    when (pointIndex) {
        // Top Row (Left to Right): 13,14,15,16,17,18
        13,14,15,16,17,18 -> {
            x = margin + (pointIndex -13) * pointWidth + pointWidth / 2f
            y = topY + pointHeight / 2f
        }
        // Top Row (Right Half): 19,20,21,22,23,24
        19,20,21,22,23,24 -> {
            x = margin + 6 * pointWidth + (pointIndex -19) * pointWidth + pointWidth / 2f
            y = topY + pointHeight / 2f
        }
        // Bottom Row (Left to Right):12,11,10,9,8,7
        12,11,10,9,8,7 -> {
            x = margin + (12 - pointIndex) * pointWidth + pointWidth / 2f
            y = bottomY - pointHeight / 2f
        }
        // Bottom Row (Right Half): 6,5,4,3,2,1
        6,5,4,3,2,1 -> {
            x = margin + 6 * pointWidth + (6 - pointIndex) * pointWidth + pointWidth / 2f
            y = bottomY - pointHeight / 2f
        }
        else -> {
            x = boardWidth / 2f
            y = boardHeight / 2f
        }
    }
    return Offset(x, y)
}

private fun DrawScope.drawBoardBackground() {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFA1887F),
                Color(0xFF8D6E63),
                Color(0xFFA1887F)
            )
        )
    )

    drawRect(
        color = Color(0xFF6D4C41),
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
        val color = if (i % 2 == 0) Color(0xFF4E342E) else Color(0xFFD7CCC8)

        drawTriangle(
            color = color,
            topLeft = Offset(x, topY),
            topRight = Offset(x + pointWidth, topY),
            bottom = Offset(x + pointWidth / 2f, topY + pointHeight)
        )

        drawTriangle(
            color = color,
            bottomLeft = Offset(x, size.height - topY),
            bottomRight = Offset(x + pointWidth, size.height - topY),
            top = Offset(x + pointWidth / 2f, size.height - topY - pointHeight)
        )
    }
}

private fun DrawScope.drawTriangle(
    color: Color,
    topLeft: Offset? = null,
    topRight: Offset? = null,
    bottomLeft: Offset? = null,
    bottomRight: Offset? = null,
    top: Offset? = null,
    bottom: Offset? = null
) {
    val path = Path()
    if (topLeft != null && topRight != null && bottom != null) {
        path.moveTo(topLeft.x, topLeft.y)
        path.lineTo(topRight.x, topRight.y)
        path.lineTo(bottom.x, bottom.y)
    } else if (bottomLeft != null && bottomRight != null && top != null) {
        path.moveTo(bottomLeft.x, bottomLeft.y)
        path.lineTo(bottomRight.x, bottomRight.y)
        path.lineTo(top.x, top.y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawAllCheckers(
    state: NardState,
    selectedPoint: Int?,
    validMoves: List<Int>,
    playerColor: NardColor?
) {
    val checkerSize = min(size.width, size.height) * 0.065f

    for (validMove in validMoves) {
        if (validMove in 1..24) {
            val pos = getPointCoordinates(validMove, size.width, size.height)
            drawCircle(
                color = Color(0x80FFD700),
                radius = checkerSize * 0.9f,
                center = pos
            )
            drawCircle(
                color = Color(0xFFFFD700),
                radius = checkerSize * 0.9f,
                center = pos,
                style = Stroke(width = 4.dp.toPx())
            )
        } else if (validMove == 25 && playerColor != null) {
            val bearOffX = size.width * 0.95f
            val bearOffY = if (playerColor == NardColor.WHITE) {
                // White's home is 19-24 (Top)
                size.height * 0.25f
            } else {
                // Black's home is 1-6 (Bottom)
                size.height * 0.75f
            }
            drawCircle(
                color = Color(0x80FFD700),
                radius = checkerSize * 1.2f,
                center = Offset(bearOffX, bearOffY)
            )
            drawCircle(
                color = Color(0xFFFFD700),
                radius = checkerSize * 1.2f,
                center = Offset(bearOffX, bearOffY),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }

    for (pointIndex in 1..24) {
        val point = state.points[pointIndex]
        val color = point.owner ?: continue
        val count = point.checkers.size
        val isSelected = pointIndex == selectedPoint

        val basePos = getPointCoordinates(pointIndex, size.width, size.height)
        val isTop = pointIndex >= 13

        for (i in 0 until count) {
            val yOffset = if (isTop) i * (checkerSize * 0.85f) else -i * (checkerSize * 0.85f)
            val pos = Offset(basePos.x, basePos.y + yOffset)

            drawChecker(
                pos = pos,
                radius = checkerSize / 2f,
                color = color,
                isSelected = isSelected && i == 0,
                isValidTarget = false
            )
        }
    }

    val barX = size.width / 2f
    val barYTop = size.height * 0.35f
    val barYBottom = size.height * 0.65f

    for (i in 0 until state.barWhite) {
        val y = barYBottom + i * (checkerSize * 0.9f)
        drawChecker(
            pos = Offset(barX, y),
            radius = checkerSize / 2f,
            color = NardColor.WHITE,
            isSelected = selectedPoint == 0,
            isValidTarget = false
        )
    }

    for (i in 0 until state.barBlack) {
        val y = barYTop - i * (checkerSize * 0.9f)
        drawChecker(
            pos = Offset(barX, y),
            radius = checkerSize / 2f,
            color = NardColor.BLACK,
            isSelected = selectedPoint == 0,
            isValidTarget = false
        )
    }
}

private fun DrawScope.drawChecker(
    pos: Offset,
    radius: Float,
    color: NardColor,
    isSelected: Boolean,
    isValidTarget: Boolean
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius,
        center = Offset(pos.x + 2f, pos.y + 2f)
    )

    drawCircle(
        brush = if (color == NardColor.WHITE) {
            Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFE0E0E0))
            )
        } else {
            Brush.radialGradient(
                colors = listOf(Color(0xFF424242), Color.Black)
            )
        },
        radius = radius,
        center = pos
    )

    drawCircle(
        color = if (color == NardColor.WHITE) Color(0xFFBDBDBD) else Color(0xFF212121),
        radius = radius,
        center = pos,
        style = Stroke(width = 2.dp.toPx())
    )

    if (isSelected) {
        drawCircle(
            color = Color(0xFFFFD700),
            radius = radius + 6.dp.toPx(),
            center = pos,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

private data class CheckerHitInfo(
    val pointIndex: Int,
    val color: NardColor? = null,
    val isBar: Boolean = false,
    val isValidMove: Boolean = false
)

private fun findTappedPoint(
    tapOffset: Offset,
    boardWidth: Float,
    boardHeight: Float,
    state: NardState,
    validMoves: List<Pair<Int, Int>>,
    validMovesForSelection: List<Int>,
    selectedPoint: Int?,
    playerColor: NardColor?
): CheckerHitInfo? {
    val checkerSize = min(boardWidth, boardHeight) * 0.065f

    for (validMoveTo in validMovesForSelection) {
        if (validMoveTo in 1..24) {
            val pos = getPointCoordinates(validMoveTo, boardWidth, boardHeight)
            val dx = tapOffset.x - pos.x
            val dy = tapOffset.y - pos.y
            val threshold = checkerSize * 0.9f
            if (dx * dx + dy * dy < threshold * threshold) {
                return CheckerHitInfo(
                    pointIndex = validMoveTo,
                    color = state.points[validMoveTo].owner,
                    isBar = false,
                    isValidMove = true
                )
            }
        } else if (validMoveTo == 25 && playerColor != null) {
            val bearOffX = boardWidth * 0.95f
            val bearOffY = if (playerColor == NardColor.WHITE) {
                // White's home is 19-24 (Top)
                boardHeight * 0.25f
            } else {
                // Black's home is 1-6 (Bottom)
                boardHeight * 0.75f
            }
            val dx = tapOffset.x - bearOffX
            val dy = tapOffset.y - bearOffY
            val threshold = checkerSize * 1.2f
            if (dx * dx + dy * dy < threshold * threshold) {
                return CheckerHitInfo(
                    pointIndex = validMoveTo,
                    color = null,
                    isBar = false,
                    isValidMove = true
                )
            }
        }
    }

    val barX = boardWidth / 2f
    val barYTop = boardHeight * 0.35f
    val barYBottom = boardHeight * 0.65f

    for (i in 0 until state.barWhite) {
        val y = barYBottom + i * (checkerSize * 0.9f)
        val dx = tapOffset.x - barX
        val dy = tapOffset.y - y
        if (dx * dx + dy * dy < checkerSize * checkerSize) {
            return CheckerHitInfo(pointIndex = 0, color = NardColor.WHITE, isBar = true, isValidMove = false)
        }
    }

    for (i in 0 until state.barBlack) {
        val y = barYTop - i * (checkerSize * 0.9f)
        val dx = tapOffset.x - barX
        val dy = tapOffset.y - y
        if (dx * dx + dy * dy < checkerSize * checkerSize) {
            return CheckerHitInfo(pointIndex = 0, color = NardColor.BLACK, isBar = true, isValidMove = false)
        }
    }

    for (pointIndex in 1..24) {
        val point = state.points[pointIndex]
        val color = point.owner ?: continue
        val count = point.checkers.size

        val basePos = getPointCoordinates(pointIndex, boardWidth, boardHeight)
        val isTop = pointIndex >= 13

        for (i in 0 until count) {
            val yOffset = if (isTop) i * (checkerSize * 0.85f) else -i * (checkerSize * 0.85f)
            val pos = Offset(basePos.x, basePos.y + yOffset)
            val dx = tapOffset.x - pos.x
            val dy = tapOffset.y - pos.y
            if (dx * dx + dy * dy < checkerSize * checkerSize) {
                return CheckerHitInfo(pointIndex = pointIndex, color = color, isBar = false, isValidMove = false)
            }
        }
    }

    return null
}

private fun handleTap(
    hitInfo: CheckerHitInfo,
    state: NardState,
    selectedPoint: Int?,
    selectedDieIndex: Int?,
    onSelectPoint: (Int) -> Unit,
    onClearSelection: () -> Unit,
    validMovesForSelection: List<Int>,
    onMakeMove: (Int, Int, Int) -> Unit,
    playerColor: NardColor?
) {
    if (playerColor == null) return
    val hasCheckersOnBar = if (playerColor == NardColor.WHITE) state.barWhite > 0 else state.barBlack > 0

    when {
        hitInfo.isValidMove && selectedPoint != null && selectedDieIndex != null -> {
            val die = state.dice[selectedDieIndex]
            onMakeMove(selectedPoint, hitInfo.pointIndex, die)
        }

        !hitInfo.isValidMove && hitInfo.color == playerColor -> {
            if (hasCheckersOnBar && !hitInfo.isBar) {
                return
            }
            if (selectedPoint == hitInfo.pointIndex) {
                onClearSelection()
            } else {
                onSelectPoint(hitInfo.pointIndex)
            }
        }

        else -> {
            onClearSelection()
        }
    }
}
