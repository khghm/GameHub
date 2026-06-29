package com.gamehub.games.battleship.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.battleship.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BattleshipScreen(
    state: BattleshipState,
    onAction: (BattleshipAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayerId = state.players.firstOrNull() ?: return
    val playerData = state.playerData[currentPlayerId] ?: return

    var selectedShipType by remember { mutableStateOf<ShipType?>(null) }
    var selectedDirection by remember { mutableStateOf(Direction.HORIZONTAL) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020617),
                        Color(0xFF0F172A),
                        Color(0xFF1E3A5F),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        if (state.phase == GamePhase.PLACEMENT) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Header(state, currentPlayerId)
                PlacementPhaseUI(
                    playerData = playerData,
                    selectedShipType = selectedShipType,
                    onSelectShip = { selectedShipType = it },
                    selectedDirection = selectedDirection,
                    onToggleDirection = { selectedDirection = if (it == Direction.HORIZONTAL) Direction.VERTICAL else Direction.HORIZONTAL },
                    onPlaceShip = { type, row, col, dir ->
                        onAction(BattleshipAction.PlaceShip(type, row, col, dir))
                    },
                    onReady = { onAction(BattleshipAction.MarkReady) },
                    onAction = onAction
                )
            }
        } else {
            BattlePhaseUI(
                state = state,
                currentPlayerId = currentPlayerId,
                onShoot = { row, col -> onAction(BattleshipAction.Shoot(row, col)) }
            )
        }
    }
}

@Composable
private fun Header(
    state: BattleshipState,
    currentPlayerId: com.gamehub.shared.core.PlayerId
) {
    val phaseText = if (state.phase == GamePhase.PLACEMENT) "مرحله جانمایی کشتی‌ها" else "مرحله نبرد"
    val isYourTurn = state.currentPlayer == currentPlayerId
    val currentPlayerText = if (state.currentPlayer != null) {
        "نوبت: " + if (isYourTurn) "شما" else "حریف"
    } else {
        "در انتظار حریف..."
    }
    val headerColor by animateColorAsState(
        targetValue = if (isYourTurn) Color(0xFF22C55E) else Color(0xFF3B82F6),
        label = "header color",
        animationSpec = tween(600, easing = EaseInOut)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = headerColor.copy(alpha = 0.4f)
            )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚓ نبردناو ⚓",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    headerColor.copy(alpha = 0.9f),
                                    headerColor
                                )
                            )
                        )
                        .shadow(4.dp, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = phaseText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE2E8F0)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedContent(
                targetState = currentPlayerText,
                label = "turn text animation",
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) + slideInVertically { -it / 2 } togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutVertically { it / 2 }
                }
            ) { text ->
                Text(
                    text = text,
                    fontSize = 18.sp,
                    color = headerColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OpponentInfoBar(
    remainingShips: Int = 6,
    isYourTurn: Boolean = false
) {
    val turnIndicatorColor by animateColorAsState(
        targetValue = if (isYourTurn) Color(0xFF22C55E) else Color(0xFFDC2626),
        label = "turn indicator color"
    )
    
    var timeLeft by remember { mutableStateOf(30) }
    LaunchedEffect(isYourTurn) {
        if (isYourTurn) {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .shadow(6.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 26.sp
                    )
                }
                // Name and Rank
                Column {
                    Text(
                        text = "حریف هوشمند",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "رتبه: نقره‌ای",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            
            // Remaining Ships Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..remainingShips) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚓",
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Turn Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    turnIndicatorColor.copy(alpha = 0.3f),
                                    turnIndicatorColor.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = turnIndicatorColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isYourTurn) timeLeft.toString() else "⌛",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = turnIndicatorColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isYourTurn) "نوبت شما" else "نوبت حریف",
                    fontSize = 11.sp,
                    color = turnIndicatorColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlacementPhaseUI(
    playerData: PlayerGameData,
    selectedShipType: ShipType?,
    onSelectShip: (ShipType) -> Unit,
    selectedDirection: Direction,
    onToggleDirection: (Direction) -> Unit,
    onPlaceShip: (ShipType, Int, Int, Direction) -> Unit,
    onReady: () -> Unit,
    onAction: (BattleshipAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = "انتخاب کشتی:",
            color = Color(0xFFF1F5F9),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(ShipType.entries) { ship ->
                val isSelected = ship == selectedShipType
                val isPlaced = playerData.ships[ship] != null
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.93f else 1f,
                    label = "ship card scale"
                )

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isPlaced -> Color(0xFF16A34A).copy(alpha = 0.35f)
                        isSelected -> Color(0xFF6366F1).copy(alpha = 0.35f)
                        else -> Color(0xFF1E293B).copy(alpha = 0.95f)
                    },
                    label = "ship bg color"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isPlaced) Color(0xFF4ADE80) else Color.White,
                    label = "ship text color"
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .scale(scale)
                        .shadow(
                            elevation = if (isSelected) 12.dp else 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isSelected) Color(0xFF6366F1).copy(alpha = 0.4f) else Color.Transparent
                        )
                        .let {
                            if (!isPlaced) {
                                it.clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) { onSelectShip(ship) }
                            } else it
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ship.displayName,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${ship.length} خانه",
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = 14.sp
                        )
                        if (isPlaced) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "✓",
                                color = Color(0xFF22C55E),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        if (selectedShipType != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جهت:",
                    color = Color(0xFFF1F5F9),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 14.dp)
                )
                FilterChip(
                    selected = selectedDirection == Direction.HORIZONTAL,
                    onClick = { onToggleDirection(Direction.HORIZONTAL) },
                    label = {
                        Text(
                            "افقی",
                            fontWeight = if (selectedDirection == Direction.HORIZONTAL) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                FilterChip(
                    selected = selectedDirection == Direction.VERTICAL,
                    onClick = { onToggleDirection(Direction.VERTICAL) },
                    label = {
                        Text(
                            "عمودی",
                            fontWeight = if (selectedDirection == Direction.VERTICAL) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        Text(
            text = "دریای شما:",
            color = Color(0xFFF1F5F9),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        ShipGrid(
            grid = playerData.shipGrid,
            isPlacementMode = true,
            showCoordinates = true,
            selectedShipType = selectedShipType,
            selectedDirection = selectedDirection,
            onCellClick = { row, col ->
                if (selectedShipType != null) {
                    onPlaceShip(selectedShipType, row, col, selectedDirection)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = { onAction(BattleshipAction.RandomPlaceAll) },
                enabled = !playerData.isReady,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = "🎲 تصادفی",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            val allShipsPlaced = playerData.ships.values.all { it != null }
            val readyButtonColor by animateColorAsState(
                targetValue = if (allShipsPlaced) Color(0xFF16A34A) else Color(0xFF475569),
                label = "ready button color"
            )

            Button(
                onClick = onReady,
                enabled = allShipsPlaced && !playerData.isReady,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = readyButtonColor,
                    disabledContainerColor = Color(0xFF475569)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = if (playerData.isReady) "آماده هستید" else "آماده شدن ✓",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

data class ActiveAnimation(
    val type: AnimationType,
    val row: Int,
    val col: Int
)

enum class AnimationType {
    SHOOT, HIT, MISS, SINK
}

@Composable
private fun BattlePhaseUI(
    state: BattleshipState,
    currentPlayerId: com.gamehub.shared.core.PlayerId,
    onShoot: (Int, Int) -> Unit
) {
    val playerData = state.playerData[currentPlayerId] ?: return
    val opponentId = state.players.firstOrNull { it != currentPlayerId }
    val opponentData = opponentId?.let { state.playerData[it] }
    val isYourTurn = state.currentPlayer == currentPlayerId

    var activeAnimations by remember { mutableStateOf<List<ActiveAnimation>>(emptyList()) }
    var lastShot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Track target grid changes to trigger animations
    val currentTargetGrid = playerData.targetGrid
    val previousTargetGrid = remember { mutableStateOf(currentTargetGrid) }

    LaunchedEffect(currentTargetGrid) {
        for (row in 0 until 10) {
            for (col in 0 until 10) {
                if (currentTargetGrid[row][col] != previousTargetGrid.value[row][col]) {
                    val newState = currentTargetGrid[row][col]
                    val newAnimationType = when (newState) {
                        CellState.HIT -> AnimationType.HIT
                        CellState.MISS -> AnimationType.MISS
                        else -> null
                    }
                    if (newAnimationType != null) {
                        lastShot = row to col
                        activeAnimations = activeAnimations + ActiveAnimation(newAnimationType, row, col)
                        delay(1000)
                        activeAnimations = activeAnimations.filterNot { it.row == row && it.col == col }
                    }
                }
            }
        }
        previousTargetGrid.value = currentTargetGrid
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020617),
                        Color(0xFF0F172A),
                        Color(0xFF1E3A5F),
                        Color(0xFF0F172A)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top: Opponent Info Bar
        OpponentInfoBar(
            remainingShips = 6 - (opponentData?.shipsSunk?.size ?: 0),
            isYourTurn = isYourTurn
        )

        // Middle (~60%): Tracking Grid (Enemy Sea)
        Column(
            modifier = Modifier
                .weight(6f)
        ) {
            Text(
                text = "دریای حریف (حمله کنید):",
                color = Color(0xFFF1F5F9),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ShipGrid(
                    grid = playerData.targetGrid,
                    isPlacementMode = false,
                    showCoordinates = true,
                    isClickable = isYourTurn,
                    opponentShips = opponentData?.ships,
                    opponentShipsSunk = opponentData?.shipsSunk,
                    activeAnimations = activeAnimations,
                    onCellClick = { row, col ->
                        if (isYourTurn && playerData.targetGrid[row][col] == CellState.EMPTY) {
                            onShoot(row, col)
                        }
                    }
                )

                // Overlay for shooting animations
                activeAnimations.firstOrNull { it.type == AnimationType.HIT || it.type == AnimationType.MISS }
                    ?.let { animation ->
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val cellSize = maxWidth / 12f
                            val xOffset = (cellSize * 1.5f) + (cellSize * animation.col)
                            val yOffset = (cellSize * 1f) + (cellSize * animation.row)

                            if (animation.type == AnimationType.HIT) {
                                ExplosionAnimation(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = xOffset.toPx().roundToInt(),
                                                y = yOffset.toPx().roundToInt()
                                            )
                                        }
                                )
                            } else {
                                WaterWaveAnimation(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = xOffset.toPx().roundToInt(),
                                                y = yOffset.toPx().roundToInt()
                                            )
                                        }
                                )
                            }
                        }
                    }
            }
        }

        // Bottom (~40%): Own Fleet Grid
        Column(
            modifier = Modifier
                .weight(4f)
        ) {
            Text(
                text = "ناوگان شما:",
                color = Color(0xFFF1F5F9),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ShipGrid(
                    grid = playerData.shipGrid,
                    isPlacementMode = false,
                    showCoordinates = true,
                    isClickable = false,
                    onCellClick = null
                )
            }
        }

        // Bottommost: Toolbar
        BottomToolbar()
    }
}

@Composable
private fun ExplosionAnimation(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 2.5f,
            animationSpec = tween(600, easing = EaseOutBack)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(400, delayMillis = 300)
        )
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF7D6),
                            Color(0xFFFCA5A5),
                            Color(0xFFDC2626),
                            Color(0xFF7F1D1D)
                        )
                    )
                )
        )
    }
}

@Composable
private fun WaterWaveAnimation(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 2f,
            animationSpec = tween(700, easing = EaseOut)
        )
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(500, delayMillis = 200)
        )
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFDBEAFE),
                            Color(0xFF93C5FD),
                            Color(0xFF3B82F6),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun BottomToolbar() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings Button
            Button(
                onClick = {},
                modifier = Modifier
                    .height(50.dp)
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    text = "⚙️",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تنظیمات",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Surrender Button
            Button(
                onClick = {},
                modifier = Modifier
                    .height(50.dp)
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(
                    text = "🏳️",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تسلیم",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShipGrid(
    grid: List<List<CellState>>,
    isPlacementMode: Boolean,
    showCoordinates: Boolean = false,
    isClickable: Boolean = true,
    selectedShipType: ShipType? = null,
    selectedDirection: Direction = Direction.HORIZONTAL,
    opponentShips: Map<ShipType, ShipPlacement?>? = null,
    opponentShipsSunk: Set<ShipType>? = null,
    activeAnimations: List<ActiveAnimation> = emptyList(),
    onCellClick: ((Int, Int) -> Unit)?
) {
    val rowLabels = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J")
    val colLabels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")

    val sunkOpponentCells = remember(opponentShips, opponentShipsSunk) {
        val cells = mutableSetOf<Pair<Int, Int>>()
        opponentShipsSunk?.forEach { shipType ->
            opponentShips?.get(shipType)?.let { placement ->
                cells.addAll(placement.getOccupiedCells())
            }
        }
        cells
    }

    val shakeOffset = remember { Animatable(0f) }
    val hasHitAnimation = activeAnimations.any { it.type == AnimationType.HIT }

    LaunchedEffect(hasHitAnimation) {
        if (hasHitAnimation) {
            shakeOffset.animateTo(
                targetValue = 8f,
                animationSpec = repeatable(
                    iterations = 6,
                    animation = tween(50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            shakeOffset.snapTo(0f)
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                translationX = shakeOffset.value
            }
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF6366F1).copy(alpha = 0.3f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCoordinates) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rowLabels.forEach { label ->
                        Text(
                            text = label,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showCoordinates) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colLabels.forEach { label ->
                            Text(
                                text = label,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                for (row in 0 until 10) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 10) {
                            val cellState = grid[row][col]
                            val isSunkCell = sunkOpponentCells.contains(row to col)
                            val isAnimating = activeAnimations.any { it.row == row && it.col == col }
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val cellColor by animateColorAsState(
                                targetValue = when {
                                    cellState == CellState.HIT -> Color(0xFFDC2626)
                                    cellState == CellState.MISS -> Color(0xFF64748B)
                                    cellState == CellState.SHIP -> Color(0xFF3B82F6)
                                    isSunkCell -> Color(0xFF1D4ED8).copy(alpha = 0.85f)
                                    else -> Color(0xFF38BDF8).copy(alpha = 0.18f)
                                },
                                label = "cell color"
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.88f else if (isAnimating) 1.15f else 1f,
                                label = "cell scale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(scale)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (cellState == CellState.SHIP || isSunkCell) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF60A5FA),
                                                    cellColor
                                                )
                                            )
                                        } else if (cellState == CellState.HIT) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFCA5A5),
                                                    cellColor
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    cellColor,
                                                    cellColor.copy(alpha = 0.65f)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.8.dp,
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .let {
                                        if (onCellClick != null && isClickable) {
                                            it.clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) { onCellClick(row, col) }
                                        } else it
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = cellState,
                                    label = "cell icon animation",
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)) togetherWith
                                                fadeOut(animationSpec = tween(200))
                                    }
                                ) { state ->
                                    when (state) {
                                        CellState.HIT -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .shadow(6.dp, CircleShape)
                                            )
                                        }
                                        CellState.MISS -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF334155))
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
}
