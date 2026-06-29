package com.gamehub.games.monopoly.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.gamehub.games.monopoly.*
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val playerColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFBC02D),
    Color(0xFF8E24AA), Color(0xFF00ACC1)
)

private val playerAvatars = listOf("🚗", "🎩", "🐕", "🚢", "🐱", "🐧")

private val groupColors = mapOf(
    "brown" to Color(0xFF795548), "lightBlue" to Color(0xFF81D4FA), "pink" to Color(0xFFF48FB1),
    "orange" to Color(0xFFFFB74D), "red" to Color(0xFFE57373), "yellow" to Color(0xFFFFF176),
    "green" to Color(0xFF81C784), "darkBlue" to Color(0xFF1565C0)
)

private fun calculateActualRent(
    cell: MonopolyBoardData.CellInfo,
    houses: Int,
    fullGroup: Boolean
): Int {
    var rent = when (houses) {
        0 -> cell.rentBase
        1 -> cell.rentBase * 2
        2 -> cell.rentBase * 3
        3 -> cell.rentBase * 4
        4 -> cell.rentBase * 5
        else -> cell.rentBase * 6
    }
    if (fullGroup && houses == 0) rent *= 2
    return rent
}

data class TooltipData(val cellIndex: Int, val screenPos: Offset)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonopolyScreen(
    state: MonopolyState,
    onAction: (MonopolyAction) -> Unit,
    modifier: Modifier = Modifier,
    localPlayerId: PlayerId = PlayerId("")
) {
    val myPlayerState = state.playerStates[localPlayerId]
    val isMyTurn = state.currentPlayer == localPlayerId && !state.gameOver
    val textMeasurer = rememberTextMeasurer()
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }
    var showManagePanel by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDarkTheme = false

    // انیمیشن حرکت
    var animatedLocalPosition by remember { mutableStateOf<Int?>(null) }
    var isMoving by remember { mutableStateOf(false) }
    var animationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    LaunchedEffect(myPlayerState?.position, state.turnPhase) {
        val newPos = myPlayerState?.position ?: return@LaunchedEffect
        val oldPos = animatedLocalPosition ?: newPos
        if (oldPos != newPos && !isMoving && animationJob?.isActive != true) {
            val steps = mutableListOf<Int>()
            var current = oldPos
            while (current != newPos) {
                current = (current + 1) % MonopolyBoardData.CELL_COUNT
                steps.add(current)
            }
            if (steps.isNotEmpty()) {
                isMoving = true
                animationJob = scope.launch {
                    for (step in steps) {
                        animatedLocalPosition = step
                        delay(100L)
                    }
                    animatedLocalPosition = newPos
                    isMoving = false
                    animationJob = null
                }
            } else {
                animatedLocalPosition = newPos
            }
        } else if (animatedLocalPosition == null) {
            animatedLocalPosition = newPos
        }
    }

    val effectiveLocalPosition = if (isMoving && animatedLocalPosition != null) animatedLocalPosition!!
    else myPlayerState?.position ?: 0

    // انیمیشن تاس
    var diceAnimationVisible by remember { mutableStateOf(false) }
    var animatedDice1 by remember { mutableStateOf(1) }
    var animatedDice2 by remember { mutableStateOf(1) }

    LaunchedEffect(state.diceResult, state.turnPhase) {
        if (state.turnPhase == TurnPhase.AWAITING_DICE_SELECTION && state.diceResult.size == 2 && !diceAnimationVisible) {
            diceAnimationVisible = true
            repeat(10) {
                animatedDice1 = Random.nextInt(1, 7)
                animatedDice2 = Random.nextInt(1, 7)
                delay(50L)
            }
            animatedDice1 = state.diceResult[0]
            animatedDice2 = state.diceResult[1]
            delay(200L)
            diceAnimationVisible = false
        }
    }

    // کارت شانس با BottomSheet
    var showChanceBottomSheet by remember { mutableStateOf(false) }
    var chanceCardMessage by remember { mutableStateOf("") }

    LaunchedEffect(state.message) {
        if (state.message.contains("شانس:") && !showChanceBottomSheet) {
            chanceCardMessage = state.message.substringAfter("شانس:").trim()
            showChanceBottomSheet = true
            delay(3000L)
            showChanceBottomSheet = false
        }
    }

    val animatedPositions = remember { mutableStateMapOf<PlayerId, Int>() }
    animatedPositions[localPlayerId] = effectiveLocalPosition
    val finalPositions = state.playerStates.mapValues { (pid, ps) ->
        if (pid == localPlayerId && animatedPositions.containsKey(pid)) animatedPositions[pid]!!
        else ps.position
    }

    // پنل وضعیت - گزینه‌های عملی
    val availableActions = remember(state.turnPhase, isMyTurn, myPlayerState) {
        mutableListOf<Pair<String, () -> Unit>>().apply {
            if (!isMyTurn || state.gameOver) return@apply
            when (state.turnPhase) {
                TurnPhase.WAITING_FOR_ROLL -> {
                    if (myPlayerState?.ownedProperties?.isNotEmpty() == true) {
                        add("🏘️ مدیریت املاک" to { showManagePanel = true })
                    }
                    add("🎲 انداختن تاس" to { onAction(MonopolyAction.RollDice) })
                    if (myPlayerState?.helperCards?.isNotEmpty() == true) {
                        myPlayerState.helperCards.forEach { card ->
                            add((if (card == HelperCard.SHIELD) "🛡️ سپر" else "🔓 فرار از زندان") to { onAction(MonopolyAction.UseHelperCard(card)) })
                        }
                    }
                }
                TurnPhase.AWAITING_DICE_SELECTION -> {
                    // در دیالوگ مدیریت می‌شود
                }
                TurnPhase.AWAITING_DECISION -> {
                    val pos = myPlayerState?.position ?: 0
                    val cell = MonopolyBoardData.cells[pos]
                    when (cell.type) {
                        MonopolyBoardData.CellType.PROPERTY,
                        MonopolyBoardData.CellType.TRANSPORT,
                        MonopolyBoardData.CellType.TEMPORARY -> {
                            if (state.board[pos].ownerId == null) {
                                add("💰 خرید ملک (${cell.price}$)" to { onAction(MonopolyAction.BuyProperty) })
                                add("❌ رد خرید" to { onAction(MonopolyAction.PassProperty) })
                            }
                        }
                        MonopolyBoardData.CellType.BANK -> {
                            add("🏦 بانک" to {}) // دیالوگ جدا
                        }
                        else -> { /* سایر خانه‌ها نیازی به دکمه ندارند */ }
                    }
                }
                TurnPhase.AWAITING_BUILD -> {
                    add("🏗️ ساخت خانه" to { onAction(MonopolyAction.BuildHouse(myPlayerState?.position ?: 0)) })
                    add("⏭️ رد" to { onAction(MonopolyAction.PassProperty) })
                }
                TurnPhase.AWAITING_JAIL_DECISION -> {
                    add("💰 پرداخت ۱۵۰$" to { onAction(MonopolyAction.PayJailFine) })
                    add("⛓️ ماندن در زندان" to { onAction(MonopolyAction.StayInJail) })
                }
                else -> { }
            }
        }
    }

    // Tooltip
    var tooltipState by remember { mutableStateOf<TooltipData?>(null) }

    // تخته بازی
    var cellBounds by remember { mutableStateOf<List<Pair<Int, Rect>>>(emptyList()) }
    var boardSize by remember { mutableStateOf(Size.Zero) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFF1A237E)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🏦 بانک‌رول", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        if (!state.gameOver) {
                            val currentName = state.playerNames[state.currentPlayer] ?: ""
                            Text("نوبت: $currentName", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                    if (state.winner != null) {
                        Text("🎉 برنده: ${state.winner!!.value}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    } else if (isMyTurn) {
                        Text("🎯 نوبت شماست!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                    myPlayerState?.let { ps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Text("💰 نقدینگی: ${ps.cash}$", color = Color(0xFFC8E6C9), fontWeight = FontWeight.Bold)
                            val assetValue = ps.cash + ps.ownedProperties.sumOf { (MonopolyBoardData.cells[it].price * 0.85).toInt() }
                            Text("💎 دارایی: $assetValue$", color = Color(0xFFBBDEFB), fontWeight = FontWeight.Bold)
                            Text("🏠 املاک: ${ps.ownedProperties.size}", color = Color(0xFFFFCC80), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (diceAnimationVisible) {
                        Text("🎲 $animatedDice1 + $animatedDice2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    } else if (state.diceResult.isNotEmpty() && state.turnPhase != TurnPhase.AWAITING_DICE_SELECTION) {
                        Text("🎲 ${state.diceResult.joinToString(" + ")}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(state.message, fontSize = 13.sp, color = Color.LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFF0D47A1)
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        text = "مرحله: ${when (state.turnPhase) {
                            TurnPhase.WAITING_FOR_ROLL -> "در انتظار تاس 🎲"
                            TurnPhase.AWAITING_DICE_SELECTION -> "انتخاب تاس 🎲"
                            TurnPhase.AWAITING_DECISION -> "تصمیم‌گیری 🧠"
                            TurnPhase.AWAITING_BUILD -> "ساخت خانه 🏗️"
                            TurnPhase.AWAITING_JAIL_DECISION -> "تصمیم زندان ⛓️"
                            TurnPhase.AWAITING_TRADE -> "معامله 🤝"
                            else -> "در حال حرکت"
                        }}",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    if (availableActions.isNotEmpty() && isMyTurn && state.turnPhase != TurnPhase.AWAITING_DICE_SELECTION) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            availableActions.forEach { (label, action) ->
                                Button(
                                    onClick = action,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (state.activeMission != null) {
                        Spacer(Modifier.height(4.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                            Text(
                                text = "📜 مأموریت: ${state.activeMission!!.description} (دور ${state.activeMission!!.currentRound}/${state.activeMission!!.maxRounds})",
                                modifier = Modifier.padding(6.dp),
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5DC), RoundedCornerShape(16.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val hit = cellBounds.firstOrNull { (_, rect) -> rect.contains(tapOffset) }
                                    if (hit != null) {
                                        selectedCellIndex = hit.first
                                        showManagePanel = true
                                    }
                                },
                                onLongPress = { longPressOffset ->
                                    val hit = cellBounds.firstOrNull { (_, rect) -> rect.contains(longPressOffset) }
                                    if (hit != null) {
                                        tooltipState = TooltipData(hit.first, longPressOffset)
                                    }
                                }
                            )
                        }
                        .onGloballyPositioned { coordinates ->
                            boardSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
                        }
                ) {
                    cellBounds = drawMonopolyBoard(
                        state = state,
                        textMeasurer = textMeasurer,
                        canvasW = size.width,
                        canvasH = size.height,
                        overridePositions = finalPositions,
                        isDark = isDarkTheme
                    )
                }
            }

            // نمایش Tooltip
            tooltipState?.let { tooltip ->
                val cellInfo = MonopolyBoardData.cells[tooltip.cellIndex]
                val prop = state.board[tooltip.cellIndex]
                val ownerName = prop.ownerId?.let { state.playerNames[it] } ?: "بانک"
                val fullGroup = cellInfo.group?.let { grp ->
                    val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == grp }
                    groupIndices.all { state.board[it].ownerId == prop.ownerId }
                } ?: false
                val actualRent = if (cellInfo.price > 0) calculateActualRent(cellInfo, prop.houses, fullGroup) else 0
                Popup(
                    alignment = Alignment.TopStart,
                    offset = androidx.compose.ui.unit.IntOffset(tooltip.screenPos.x.toInt() + 20, tooltip.screenPos.y.toInt() - 20),
                    onDismissRequest = { tooltipState = null },
                    properties = PopupProperties(focusable = false)
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF333333) else Color.White),
                        modifier = Modifier.width(200.dp).shadow(4.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(cellInfo.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (cellInfo.type == MonopolyBoardData.CellType.PROPERTY) {
                                Text("قیمت: ${cellInfo.price}$", fontSize = 12.sp)
                                Text("اجاره: $actualRent$", fontSize = 12.sp)
                                Text("خانه‌ها: ${prop.houses}", fontSize = 12.sp)
                                Text("مالک: $ownerName", fontSize = 12.sp)
                                if (prop.houses < 4 && prop.ownerId == localPlayerId) {
                                    Text("🏗️ هزینه ساخت: ${cellInfo.buildCost}$", fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                            } else {
                                Text("نوع: ${cellInfo.type}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // دیالوگ مدیریت املاک
            if (showManagePanel && myPlayerState != null && state.turnPhase == TurnPhase.WAITING_FOR_ROLL) {
                val cellIdx = selectedCellIndex ?: myPlayerState.position
                val cell = MonopolyBoardData.cells[cellIdx]
                val prop = state.board[cellIdx]
                val ownerName = prop.ownerId?.let { state.playerNames[it] } ?: "بانک"
                val fullGroup = cell.group?.let { grp ->
                    val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == grp }
                    groupIndices.all { state.board[it].ownerId == prop.ownerId }
                } ?: false
                val currentRent = if (cell.price > 0) calculateActualRent(cell, prop.houses, fullGroup) else 0
                AlertDialog(
                    onDismissRequest = { showManagePanel = false },
                    title = { Text(cell.name) },
                    text = {
                        Column {
                            Text("نوع: ${cell.type}")
                            if (cell.price > 0) Text("قیمت خرید: ${cell.price}$")
                            Text("اجاره فعلی: ${if (cell.price > 0) "$currentRent$" else "---"}")
                            Text("مالک: $ownerName")
                            if (prop.houses > 0) Text("خانه‌ها: ${prop.houses}")
                            if (prop.mortgaged) Text("رهن شده")
                            if (prop.ownerId == localPlayerId) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (cell.type != MonopolyBoardData.CellType.TEMPORARY) {
                                        Button(onClick = { onAction(MonopolyAction.SellProperty(cellIdx)); showManagePanel = false }) {
                                            Text("بفروش")
                                        }
                                    }
                                    if (cell.type == MonopolyBoardData.CellType.PROPERTY && prop.houses < 4 && !prop.mortgaged) {
                                        Button(onClick = { onAction(MonopolyAction.BuildHouse(cellIdx)); showManagePanel = false }) {
                                            Text("+🏠 ساخت (${cell.buildCost}$)")
                                        }
                                    }
                                    if (prop.houses > 0) {
                                        Button(onClick = { onAction(MonopolyAction.SellHouse(cellIdx)); showManagePanel = false }) {
                                            Text("-🏠 فروش خانه")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showManagePanel = false }) { Text("بستن") } }
                )
            }

            // دیالوگ کارت شانس (BottomSheet)
            if (showChanceBottomSheet) {
                ModalBottomSheet(onDismissRequest = { showChanceBottomSheet = false }) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🎴 کارت شانس", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(chanceCardMessage)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showChanceBottomSheet = false }) {
                            Text("باشه")
                        }
                    }
                }
            }

            // دیالوگ‌های تاس و معامله (با پیش‌نمایش)
            when {
                state.turnPhase == TurnPhase.STRATEGIC_DICE && isMyTurn -> {
                    StrategicDiceDialog(state.diceResult, onAction, myPlayerState?.position ?: 0)
                }
                state.tradeStep == TradeStep.AWAITING_PROPOSALS && !isMyTurn && state.currentPlayer != localPlayerId -> {
                    SubmitTradeProposalDialog(state, localPlayerId, onAction)
                }
                state.tradeStep == TradeStep.AWAITING_PROPOSALS && isMyTurn && state.currentPlayer == localPlayerId -> {
                    key(state.tradeProposals.size) {
                        if (state.tradeProposals.isNotEmpty()) {
                            SelectProposalDialog(state, onAction)
                        } else {
                            Card(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                                Column {
                                    Text("منتظر پیشنهاد سایر بازیکنان...", modifier = Modifier.padding(16.dp))
                                    Button(onClick = { onAction(MonopolyAction.CancelTrade) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                        Text("لغو معامله")
                                    }
                                }
                            }
                        }
                    }
                }
                state.tradeStep == TradeStep.AWAITING_COUNTER && isMyTurn -> {
                    MakeCounterOfferDialog(state, onAction)
                }
                state.tradeStep == TradeStep.AWAITING_RESPONSE && state.pendingTrade?.toPlayer == localPlayerId -> {
                    TradeResponseDialog(state.pendingTrade!!, state.playerNames, onAction)
                }
                state.turnPhase == TurnPhase.AWAITING_DICE_SELECTION && isMyTurn -> {
                    DiceSelectionDialog(state.diceResult, onAction, myPlayerState?.position ?: 0)
                }
                state.turnPhase == TurnPhase.AWAITING_DECISION && isMyTurn -> {
                    val pos = myPlayerState?.position ?: 0
                    val cell = MonopolyBoardData.cells[pos]
                    when (cell.type) {
                        MonopolyBoardData.CellType.PROPERTY, MonopolyBoardData.CellType.TRANSPORT, MonopolyBoardData.CellType.TEMPORARY -> {
                            if (state.board[pos].ownerId == null) {
                                BuyPropertyDialog(cell, onAction)
                            } else if (cell.type == MonopolyBoardData.CellType.TRANSPORT && state.board[pos].ownerId == localPlayerId) {
                                TransportDialog(state, pos, onAction)
                            }
                        }
                        MonopolyBoardData.CellType.BANK -> BankDialog(state, localPlayerId, onAction)
                        MonopolyBoardData.CellType.DEAL -> { /* handled by trade step */ }
                        else -> {}
                    }
                }
                state.turnPhase == TurnPhase.AWAITING_BUILD && isMyTurn -> {
                    val pos = myPlayerState?.position ?: 0
                    BuildHouseDialog(state, pos, onAction)
                }
                state.turnPhase == TurnPhase.AWAITING_JAIL_DECISION && isMyTurn -> {
                    JailDecisionDialog(myPlayerState!!, onAction)
                }
            }
        }
    }
}

// ========================== دیالوگ‌های تاس با پیش‌نمایش ==========================
@Composable
fun StrategicDiceDialog(options: List<Int>, onAction: (MonopolyAction) -> Unit, currentPos: Int) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = {},
        title = { Text("تاس استراتژیک") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("یکی از تاس‌های زیر را انتخاب کنید:")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    options.forEach { value ->
                        val newPos = (currentPos + value) % MonopolyBoardData.CELL_COUNT
                        Button(
                            onClick = { selectedOption = value },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selectedOption == value) Color.Yellow else Color.White)
                        ) {
                            Text(value.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        if (selectedOption == value) {
                            Text("↓ به ${MonopolyBoardData.cells[newPos].name}", fontSize = 12.sp)
                        }
                    }
                }
                if (selectedOption != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onAction(MonopolyAction.SelectStrategicDice(selectedOption!!)) }) {
                        Text("حرکت کن")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun DiceSelectionDialog(dice: List<Int>, onAction: (MonopolyAction) -> Unit, currentPos: Int) {
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var isMoving by remember { mutableStateOf(false) }
    val dice1 = dice.getOrNull(0) ?: 1
    val dice2 = dice.getOrNull(1) ?: 1
    val totalSteps = selectedIndices.sumOf { dice[it] }
    val targetPos = (currentPos + totalSteps) % MonopolyBoardData.CELL_COUNT
    val targetName = MonopolyBoardData.cells[targetPos].name

    AlertDialog(
        onDismissRequest = {},
        title = { Text("انتخاب تاس") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("تاس‌ها: $dice1 و $dice2")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    DiceButton(value = dice1, isSelected = selectedIndices.contains(0)) {
                        if (!isMoving) {
                            selectedIndices = if (selectedIndices.contains(0)) selectedIndices - 0 else selectedIndices + 0
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    DiceButton(value = dice2, isSelected = selectedIndices.contains(1)) {
                        if (!isMoving) {
                            selectedIndices = if (selectedIndices.contains(1)) selectedIndices - 1 else selectedIndices + 1
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("مجموع انتخاب‌شده: $totalSteps")
                if (totalSteps > 0) {
                    Text("📍 مقصد: $targetName", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedIndices.isNotEmpty() && !isMoving) {
                        isMoving = true
                        onAction(MonopolyAction.SelectDiceAndMove(totalSteps))
                    }
                },
                enabled = selectedIndices.isNotEmpty() && !isMoving
            ) {
                Text(if (isMoving) "در حال حرکت..." else "حرکت به $targetName")
            }
        }
    )
}

@Composable
fun DiceButton(value: Int, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Yellow else Color.White,
            contentColor = Color.Black
        ),
        border = if (isSelected) BorderStroke(2.dp, Color.Black) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text = value.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

// توابع دیالوگ دیگر (همان پیاده‌سازی اصلی) - برای اختصار فقط امضاها آورده شده
@Composable
fun BuyPropertyDialog(cell: MonopolyBoardData.CellInfo, onAction: (MonopolyAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(MonopolyAction.PassProperty) },
        title = { Text(cell.name) },
        text = { Text("قیمت: ${cell.price}$\nاجاره پایه: ${cell.rentBase}$\nمی‌خواهید بخرید؟") },
        confirmButton = { Button(onClick = { onAction(MonopolyAction.BuyProperty) }) { Text("خرید") } },
        dismissButton = { Button(onClick = { onAction(MonopolyAction.PassProperty) }) { Text("رد") } }
    )
}

@Composable
fun TransportDialog(state: MonopolyState, fromPos: Int, onAction: (MonopolyAction) -> Unit) {
    val destinations = MonopolyBoardData.cells.indices.filter {
        MonopolyBoardData.cells[it].type == MonopolyBoardData.CellType.TRANSPORT && it != fromPos && state.board[it].ownerId != null
    }
    if (destinations.isEmpty()) {
        AlertDialog(
            onDismissRequest = { onAction(MonopolyAction.PassProperty) },
            title = { Text("حمل و نقل") },
            text = { Text("هیچ مقصد قابل سفر دیگری وجود ندارد.") },
            confirmButton = { Button(onClick = { onAction(MonopolyAction.PassProperty) }) { Text("باشه") } }
        )
        return
    }
    AlertDialog(
        onDismissRequest = { onAction(MonopolyAction.PassProperty) },
        title = { Text("انتخاب مقصد") },
        text = {
            Column {
                destinations.forEach { dest ->
                    val cost = 50 + kotlin.math.abs(fromPos - dest) * 5
                    Button(onClick = { onAction(MonopolyAction.UseTransport(dest)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${MonopolyBoardData.cells[dest].name} - هزینه $cost$")
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Button(onClick = { onAction(MonopolyAction.PassProperty) }) { Text("انصراف") }
            }
        },
        confirmButton = { TextButton(onClick = {}) { Text("") } }
    )
}

@Composable
fun BankDialog(state: MonopolyState, playerId: PlayerId, onAction: (MonopolyAction) -> Unit) {
    val ps = state.playerStates[playerId]!!
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onAction(MonopolyAction.PassProperty) },
        title = { Text("بانک") },
        text = {
            Column {
                if (ps.loan == null) {
                    Text("وام نقدی:")
                    Row {
                        Button(onClick = { onAction(MonopolyAction.TakeLoan(100)) }) { Text("100$") }
                        Button(onClick = { onAction(MonopolyAction.TakeLoan(300)) }) { Text("300$") }
                        Button(onClick = { onAction(MonopolyAction.TakeLoan(500)) }) { Text("500$") }
                    }
                } else {
                    Text("شما وام دارید: باقیمانده ${ps.loan!!.remaining}$")
                }
                Spacer(Modifier.height(8.dp))
                Text("رهن ملک:")
                ps.ownedProperties.filter { !state.board[it].mortgaged }.forEach { idx ->
                    Button(onClick = { onAction(MonopolyAction.MortgageProperty(idx)) }) {
                        Text("رهن ${MonopolyBoardData.cells[idx].name}")
                    }
                }
                Text("آزادسازی رهن:")
                ps.ownedProperties.filter { state.board[it].mortgaged }.forEach { idx ->
                    Button(onClick = { onAction(MonopolyAction.UnmortgageProperty(idx)) }) {
                        Text("آزادسازی ${MonopolyBoardData.cells[idx].name}")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("سرمایه‌گذاری (حداقل 50$):")
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("مبلغ") })
                Button(onClick = { amountText.toIntOrNull()?.let { onAction(MonopolyAction.MakeInvestment(it)) } }) {
                    Text("سرمایه‌گذاری برای ۲ دور (سود ۱۵٪)")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAction(MonopolyAction.PassProperty) }) { Text("بستن") } }
    )
}

@Composable
fun BuildHouseDialog(state: MonopolyState, propIndex: Int, onAction: (MonopolyAction) -> Unit) {
    val cell = MonopolyBoardData.cells[propIndex]
    AlertDialog(
        onDismissRequest = { onAction(MonopolyAction.PassProperty) },
        title = { Text("ساخت خانه") },
        text = { Text("روی ملک خود ایستاده‌اید: ${cell.name}\nهزینه ساخت: ${cell.buildCost}$\nمی‌خواهید خانه بسازید؟") },
        confirmButton = { Button(onClick = { onAction(MonopolyAction.BuildHouse(propIndex)) }) { Text("ساخت") } },
        dismissButton = { Button(onClick = { onAction(MonopolyAction.PassProperty) }) { Text("نه، بعداً") } }
    )
}

@Composable
fun JailDecisionDialog(ps: PlayerState, onAction: (MonopolyAction) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("زندان") },
        text = { Text("به زندان افتادی! می‌خواهی 150 دلار جریمه پرداخت کنی یا ${if (ps.jailFirstTime) "1 دور" else "3 دور"} در زندان بمانی؟") },
        confirmButton = { Button(onClick = { onAction(MonopolyAction.PayJailFine) }) { Text("پرداخت 150 دلار") } },
        dismissButton = { Button(onClick = { onAction(MonopolyAction.StayInJail) }) { Text("ماندن در زندان") } }
    )
}

@Composable
fun SubmitTradeProposalDialog(state: MonopolyState, playerId: PlayerId, onAction: (MonopolyAction) -> Unit) {
    var cashOffer by remember { mutableStateOf("") }
    var selectedProperties by remember { mutableStateOf(setOf<Int>()) }
    val myPs = state.playerStates[playerId]!!
    val currentTraderName = state.playerNames[state.currentPlayer] ?: ""

    AlertDialog(
        onDismissRequest = { onAction(MonopolyAction.CancelTrade) },
        title = { Text("ارسال پیشنهاد معامله") },
        text = {
            Column {
                Text("بازیکن $currentTraderName روی خانه معامله ایستاده است.")
                Text("شما می‌توانید پول و/یا املاک خود را پیشنهاد دهید.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = cashOffer,
                    onValueChange = { cashOffer = it },
                    label = { Text("پیشنهاد نقدی (دلار)") },
                    placeholder = { Text("مثلاً 200") }
                )
                Spacer(Modifier.height(8.dp))
                Text("املاک پیشنهادی (شما می‌دهید):")
                if (myPs.ownedProperties.isEmpty()) {
                    Text("شما هیچ ملکی ندارید", color = Color.Gray)
                } else {
                    myPs.ownedProperties.forEach { propIdx ->
                        val cell = MonopolyBoardData.cells[propIdx]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedProperties.contains(propIdx),
                                onCheckedChange = { isChecked ->
                                    selectedProperties = if (isChecked) selectedProperties + propIdx else selectedProperties - propIdx
                                }
                            )
                            Text("${cell.name} (ارزش خرید ${cell.price}$)", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { onAction(MonopolyAction.CancelTrade) }) { Text("لغو") } },
        confirmButton = {
            Button(onClick = {
                val cash = cashOffer.toIntOrNull() ?: 0
                onAction(MonopolyAction.SubmitTradeProposal(cash, selectedProperties.toList()))
            }) { Text("ارسال پیشنهاد") }
        }
    )
}

@Composable
fun SelectProposalDialog(state: MonopolyState, onAction: (MonopolyAction) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("انتخاب پیشنهاد معامله") },
        text = {
            Column {
                Text("سایر بازیکنان پیشنهادهای زیر را ارسال کرده‌اند. یکی را انتخاب کنید:")
                state.tradeProposals.values.forEach { proposal ->
                    val proposerName = state.playerNames[proposal.fromPlayer] ?: ""
                    val cashStr = if (proposal.offeredCash > 0) "${proposal.offeredCash}$" else "بدون پول"
                    val propStr = if (proposal.offeredProperties.isNotEmpty()) {
                        proposal.offeredProperties.joinToString(", ") { MonopolyBoardData.cells[it].name }
                    } else "بدون ملک"
                    Button(
                        onClick = { onAction(MonopolyAction.SelectTradeProposal(proposal.fromPlayer)) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$proposerName: $cashStr + [$propStr]")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = {}) { Text("") } }
    )
}

@Composable
fun MakeCounterOfferDialog(state: MonopolyState, onAction: (MonopolyAction) -> Unit) {
    var offeredCash by remember { mutableStateOf("") }
    var offeredProperties by remember { mutableStateOf(setOf<Int>()) }
    val targetPlayerId = state.selectedProposalFrom ?: return
    val targetName = state.playerNames[targetPlayerId] ?: ""
    val myPs = state.playerStates[state.currentPlayer] ?: return

    val originalProposal = state.tradeProposals[targetPlayerId] ?: return

    AlertDialog(
        onDismissRequest = {},
        title = { Text("ضدپیشنهاد به $targetName") },
        text = {
            Column {
                Text("پیشنهاد ${state.playerNames[targetPlayerId]}:")
                if (originalProposal.offeredCash > 0) Text("💰 پول: ${originalProposal.offeredCash}$")
                originalProposal.offeredProperties.forEach { prop ->
                    Text("🏠 ملک: ${MonopolyBoardData.cells[prop].name}")
                }
                Spacer(Modifier.height(12.dp))
                Text("پیشنهاد شما (آنچه می‌دهید):")
                OutlinedTextField(
                    value = offeredCash,
                    onValueChange = { offeredCash = it },
                    label = { Text("پیشنهاد نقدی (دلار)") },
                    placeholder = { Text("مثلاً 50") }
                )
                Spacer(Modifier.height(8.dp))
                Text("املاک پیشنهادی شما (از املاک خودتان):")
                if (myPs.ownedProperties.isEmpty()) {
                    Text("شما هیچ ملکی ندارید", color = Color.Gray)
                } else {
                    myPs.ownedProperties.forEach { propIdx ->
                        val cell = MonopolyBoardData.cells[propIdx]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = offeredProperties.contains(propIdx),
                                onCheckedChange = { isChecked ->
                                    offeredProperties = if (isChecked) offeredProperties + propIdx else offeredProperties - propIdx
                                }
                            )
                            Text("${cell.name} (ارزش خرید ${cell.price}$)", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = { onAction(MonopolyAction.CancelTrade) }) { Text("لغو معامله") } },
        confirmButton = {
            Button(onClick = {
                val cash = offeredCash.toIntOrNull() ?: 0
                onAction(MonopolyAction.MakeCounterOffer(targetPlayerId, cash, offeredProperties.toList()))
            }) { Text("ارسال ضدپیشنهاد") }
        }
    )
}

@Composable
fun TradeResponseDialog(trade: TradeProposal, playerNames: Map<PlayerId, String>, onAction: (MonopolyAction) -> Unit) {
    var decisionMade by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!decisionMade) {
                decisionMade = true
                onAction(MonopolyAction.RejectTrade(trade.id))
            }
        },
        title = { Text("پیشنهاد معامله") },
        text = {
            Column {
                Text("بازیکن ${playerNames[trade.fromPlayer]} پیشنهاد می‌دهد:")
                if (trade.offeredCash > 0) Text("💰 پول: ${trade.offeredCash}$")
                trade.offeredProperties.forEach { prop ->
                    Text("🏠 ملک: ${MonopolyBoardData.cells[prop].name}")
                }
                Text("در مقابل درخواست دارد:")
                if (trade.requestedCash > 0) Text("💰 پول: ${trade.requestedCash}$")
                trade.requestedProperties.forEach { prop ->
                    Text("🏠 ملک: ${MonopolyBoardData.cells[prop].name}")
                }
                Text("آیا این معامله را قبول می‌کنید؟")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!decisionMade) {
                        decisionMade = true
                        onAction(MonopolyAction.AcceptTrade(trade.id))
                    }
                },
                enabled = !decisionMade
            ) { Text("قبول") }
        },
        dismissButton = {
            Button(
                onClick = {
                    if (!decisionMade) {
                        decisionMade = true
                        onAction(MonopolyAction.RejectTrade(trade.id))
                    }
                },
                enabled = !decisionMade
            ) { Text("رد") }
        }
    )
}

// ========================== تابع رسم تخته با آواتار و مقیاس‌پذیری ==========================
private fun DrawScope.drawMonopolyBoard(
    state: MonopolyState,
    textMeasurer: TextMeasurer,
    canvasW: Float,
    canvasH: Float,
    overridePositions: Map<PlayerId, Int>,
    isDark: Boolean
): List<Pair<Int, Rect>> {
    val boardW = canvasW
    val boardH = canvasH
    val offX = 0f
    val offY = 0f

    drawRect(if (isDark) Color(0xFF2E2E2E) else Color(0xFFF5F5DC), Offset(offX, offY), Size(boardW, boardH))
    drawRect(Color.Black, Offset(offX, offY), Size(boardW, boardH), style = Stroke(4f))

    val cellSize = boardW / 12f
    val bounds = mutableListOf<Pair<Int, Rect>>()

    for (i in 0 until MonopolyBoardData.CELL_COUNT) {
        val (px, py) = MonopolyBoardData.getCellCoords(i)
        val cx = offX + px / 100f * boardW
        val cy = offY + py / 100f * boardH
        val cell = MonopolyBoardData.cells[i]

        val bgColor = when (cell.type) {
            MonopolyBoardData.CellType.START -> Color(0xFF4CAF50)
            MonopolyBoardData.CellType.JAIL -> Color(0xFFFF9800)
            MonopolyBoardData.CellType.CHANCE -> Color(0xFF9C27B0)
            MonopolyBoardData.CellType.BANK -> Color(0xFF2196F3)
            MonopolyBoardData.CellType.TRANSPORT -> Color(0xFF795548)
            MonopolyBoardData.CellType.TEMPORARY -> Color(0xFFFF5722)
            MonopolyBoardData.CellType.DEAL -> Color(0xFFE91E63)
            MonopolyBoardData.CellType.MISSION -> Color(0xFF00BCD4)
            MonopolyBoardData.CellType.PROPERTY -> groupColors[cell.group] ?: Color.Gray
        }

        val rectSize = cellSize * 1.3f
        val half = rectSize / 2
        val left = cx - half
        val top = cy - half
        val right = cx + half
        val bottom = cy + half

        drawRoundRect(bgColor, Offset(left, top), Size(rectSize, rectSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f))
        drawRoundRect(Color.Black, Offset(left, top), Size(rectSize, rectSize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f), style = Stroke(1.5f))

        bounds.add(i to Rect(left, top, right, bottom))

        val nameStyle = TextStyle(fontSize = 10.sp, color = if (isDark) Color.White else Color.Black, textAlign = TextAlign.Center)
        val nameResult = textMeasurer.measure(cell.name.take(10), nameStyle)
        drawText(nameResult, topLeft = Offset(cx - nameResult.size.width / 2, top + 4))

        if (cell.price > 0) {
            val priceStyle = TextStyle(fontSize = 8.sp, color = Color.Yellow, textAlign = TextAlign.Center)
            val priceResult = textMeasurer.measure("${cell.price}$", priceStyle)
            drawText(priceResult, topLeft = Offset(cx - priceResult.size.width / 2, bottom - 12))
        }

        val prop = state.board[i]
        if (prop.ownerId != null) {
            val ownerIdx = state.players.indexOf(prop.ownerId)
            if (ownerIdx >= 0) {
                drawCircle(playerColors[ownerIdx % playerColors.size], radius = 5f, center = Offset(cx + half - 8, cy - half + 8))
            }
            if (cell.price > 0) {
                val fullGroup = cell.group?.let { grp ->
                    val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == grp }
                    groupIndices.all { state.board[it].ownerId == prop.ownerId }
                } ?: false
                val actualRent = calculateActualRent(cell, prop.houses, fullGroup)
                val rentText = "اجاره:$actualRent"
                val rentStyle = TextStyle(fontSize = 7.sp, color = if (isDark) Color.Black else Color.White, background = Color.White.copy(alpha = 0.8f))
                val rentResult = textMeasurer.measure(rentText, rentStyle)
                drawText(rentResult, topLeft = Offset(cx - rentResult.size.width / 2, bottom - rentResult.size.height - 2))
            }
        }

        if (prop.houses > 0) {
            val houseText = if (prop.houses >= 5) "🏨" else "🏠".repeat(prop.houses)
            val houseStyle = TextStyle(fontSize = 12.sp)
            val houseResult = textMeasurer.measure(houseText, houseStyle)
            drawText(houseResult, topLeft = Offset(cx - houseResult.size.width / 2, bottom - houseResult.size.height - (if (cell.price > 0) 12 else 2)))
        }
    }

    // رسم مهره‌ها با آواتار
    state.playerStates.forEach { (playerId, pState) ->
        if (pState.isBankrupt) return@forEach
        val pos = overridePositions[playerId] ?: pState.position
        val (px, py) = MonopolyBoardData.getCellCoords(pos)
        val cx = offX + px / 100f * boardW
        val cy = offY + py / 100f * boardH
        val idx = state.players.indexOf(playerId)
        val color = playerColors[idx % playerColors.size]
        val avatar = playerAvatars[idx % playerAvatars.size]

        val sameCellPlayers = state.playerStates.filter { it.value.position == pos && !it.value.isBankrupt }.keys.toList()
        val offsetX = when (sameCellPlayers.size) {
            1 -> 0f
            2 -> if (sameCellPlayers[0] == playerId) -10f else 10f
            else -> (sameCellPlayers.indexOf(playerId) - (sameCellPlayers.size - 1) / 2f) * 12f
        }

        drawCircle(color, radius = 16f, center = Offset(cx + offsetX, cy))
        drawCircle(Color.White, radius = 16f, center = Offset(cx + offsetX, cy), style = Stroke(2f))
        val avatarStyle = TextStyle(fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
        val avatarResult = textMeasurer.measure(avatar, avatarStyle)
        drawText(avatarResult, topLeft = Offset(cx + offsetX - avatarResult.size.width / 2, cy - avatarResult.size.height / 2))
    }

    return bounds
}