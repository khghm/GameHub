package com.gamehub.games.uno.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.gamehub.games.uno.UnoState
import com.gamehub.shared.engines.card.*
import com.gamehub.shared.graphics.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

object UnoGraphicsSpec : GraphicsSpec() {
    override val primaryColor: Color = Color(0xFF2E7D32)
    override val secondaryColor: Color = Color(0xFF1B5E20)
    override val accentColor: Color = Color(0xFFFFD700)
    override val backgroundColor: Color = Color(0xFF0A2E0A)
    override val surfaceColor: Color = Color(0xFF1C3A1C)
    override val surfaceVariantColor: Color = Color(0xFF0F280F)
    override val textColor: Color = Color.White
    override val textSecondaryColor: Color = Color(0xFFA5A5A5)
    override val errorColor: Color = Color(0xFFB00020)
    override val shadowElevation: androidx.compose.ui.unit.Dp = 16.dp
    override val cornerRadius: androidx.compose.ui.unit.Dp = 12.dp
    override val borderWidth: androidx.compose.ui.unit.Dp = 2.dp
    override val animationDurationMs: Int = 400
    override val particleEnabled: Boolean = DeviceTierDetector.shouldEnableHeavyEffects()
    override val effectEnabled: Boolean = DeviceTierDetector.shouldEnableHeavyEffects()
}

@Composable
fun UnoScreen(
    state: UnoState,
    localPlayerId: String,
    onAction: (CardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMyTurn = state.currentPlayer?.value == localPlayerId && !state.gameOver
    val myHand = state.hands[com.gamehub.shared.core.PlayerId(localPlayerId)] ?: Hand(emptyList())
    val discardTop = state.discardPile.lastOrNull()

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val coroutineScope = rememberCoroutineScope()

    var discardCenter by remember { mutableStateOf(Offset.Zero) }
    var handAreaWidth by remember { mutableStateOf(0f) }
    var handAreaHeight by remember { mutableStateOf(0f) }
    var handAreaOrigin by remember { mutableStateOf(Offset.Zero) }

    var flyingCard by remember { mutableStateOf<Card?>(null) }
    val flyingPos = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val flyingScale = remember { Animatable(1f) }

    val particleSystem = remember { ParticleSystem() }
    var showFireworks by remember { mutableStateOf(false) }

    LaunchedEffect(state.winner) {
        if (state.winner != null && UnoGraphicsSpec.particleEnabled) {
            showFireworks = true
            repeat(DeviceTierDetector.getMaxParticleCount()) {
                val angle = Random.nextFloat() * 2 * PI.toFloat()
                val speed = Random.nextFloat() * 400f + 200f
                val colors = listOf(
                    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047),
                    Color(0xFFFDD835), Color(0xFF8E24AA), UnoGraphicsSpec.accentColor
                )
                particleSystem.emitBurst(
                    centerX = 0.5f,
                    centerY = 0.5f,
                    count = 1,
                    colors = colors,
                    minSpeed = 0.5f,
                    maxSpeed = 1f,
                    minLife = 1f,
                    maxLife = 3f,
                    isNormalized = true
                )
            }
            delay(3500)
            showFireworks = false
            particleSystem.clear()
        }
    }

    fun launchCard(card: Card, startOffset: Offset) {
        flyingCard = card
        coroutineScope.launch {
            flyingPos.snapTo(startOffset)
            flyingScale.snapTo(1f)
            val j1 = launch { flyingPos.animateTo(discardCenter, tween(400, easing = FastOutSlowInEasing)) }
            val j2 = launch { flyingScale.animateTo(0.7f, tween(400, easing = FastOutSlowInEasing)) }
            j1.join(); j2.join()
            flyingCard = null
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(UnoGraphicsSpec.primaryColor, UnoGraphicsSpec.secondaryColor, UnoGraphicsSpec.backgroundColor), center = Offset(0.5f, 0.6f), radius = 1.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            UnoHeader(state, isMyTurn)

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val sz = coords.size.toSize()
                        discardCenter = Offset(pos.x + sz.width/2, pos.y + sz.height/2)
                    },
                contentAlignment = Alignment.Center
            ) {
                DiscardSection(state = state, isMyTurn = isMyTurn, textMeasurer = textMeasurer, onDrawCard = { onAction(CardAction.DrawCard) })
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp)
                    .onGloballyPositioned { coords ->
                        handAreaOrigin = coords.positionInRoot()
                        handAreaWidth = coords.size.toSize().width
                        handAreaHeight = coords.size.toSize().height
                    }
            ) {
                if (myHand.cards.isNotEmpty()) {
                    CanvasFanHand(
                        hand = myHand, discardTop = discardTop, isMyTurn = isMyTurn,
                        areaWidth = handAreaWidth, areaHeight = handAreaHeight,
                        originInRoot = handAreaOrigin, density = density,
                        textMeasurer = textMeasurer,
                        onCardClick = { card, _, cardCenterInRoot ->
                            if (isMyTurn) {
                                launchCard(card, cardCenterInRoot)
                                onAction(CardAction.PlayCard(card = card, chosenColor = if (card.color == CardColor.WILD) CardColor.RED else null))
                            }
                        }
                    )
                }
            }
        }

        flyingCard?.let { card ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCard(card, center = flyingPos.value, scale = flyingScale.value, textMeasurer = textMeasurer, drawShadow = false)
            }
        }

        if (UnoGraphicsSpec.particleEnabled) {
            ParticleEffect(
                modifier = Modifier.fillMaxSize(),
                particleSystem = particleSystem,
                isActive = showFireworks
            )
        }

        if (state.winner != null) WinnerBanner(winner = state.winner!!.value)
    }
}

@Composable
fun UnoHeader(state: UnoState, isMyTurn: Boolean) = Card(
    modifier = Modifier.fillMaxWidth().shadow(UnoGraphicsSpec.shadowElevation, RoundedCornerShape(20.dp)).slideIn(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = UnoGraphicsSpec.surfaceColor.copy(alpha = 0.9f))
) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        HeaderItem("🃏", "${state.deck.size}", "دسته")
        HeaderItem(if (state.direction == 1) "↻" else "↺", "جهت", "")
        HeaderItem(if (isMyTurn) "🎯" else "⏳", state.currentPlayer?.value ?: "?", if (isMyTurn) "نوبت شما" else "منتظر", isMyTurn)
    }
}

@Composable
fun HeaderItem(icon: String, value: String, label: String, isPulsing: Boolean = false) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    val modifier = if (isPulsing) Modifier.pulse() else Modifier
    Text(icon, fontSize = 26.sp, modifier = modifier)
    Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
    Text(label, fontSize = 10.sp, color = Color(0xFFB0BEC5))
}

@Composable
fun DiscardSection(state: UnoState, isMyTurn: Boolean, textMeasurer: TextMeasurer, onDrawCard: () -> Unit) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    val card = state.discardPile.lastOrNull()
    if (card != null) Canvas(modifier = Modifier.size(120.dp, 170.dp)) { drawCard(card, center = Offset(size.width/2, size.height/2), scale = 1f, textMeasurer = textMeasurer) }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onDrawCard,
        enabled = isMyTurn,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
    ) { Text("🃏 کشیدن", color = Color.White) }
}

@Composable
fun CanvasFanHand(
    hand: Hand, discardTop: Card?, isMyTurn: Boolean,
    areaWidth: Float, areaHeight: Float, originInRoot: Offset,
    density: Density, textMeasurer: TextMeasurer,
    onCardClick: (Card, Int, Offset) -> Unit
) {
    val cards = hand.cards; if (cards.isEmpty() || areaWidth <= 0f) return
    val cardW = with(density) { 65.dp.toPx() }; val cardH = with(density) { 95.dp.toPx() }
    val lift = with(density) { 8.dp.toPx() }; val margin = with(density) { 8.dp.toPx() }
    val centerY = areaHeight - with(density) { 20.dp.toPx() }

    val availableWidth = areaWidth - cardW - margin * 2
    val spacing = if (cards.size > 1) (availableWidth / (cards.size - 1)).coerceAtMost(cardW * 0.9f).coerceAtLeast(cardW * 0.2f) else 0f
    val fanAngle = (30f - (cards.size - 4).coerceIn(0, 10) * 1.5f).coerceAtLeast(15f)

    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(cards, isMyTurn, discardTop, cardW, cardH, lift, spacing, centerY) {
            detectTapGestures { tapOffset ->
                val idx = findClickedCardIndexDynamic(tapOffset, cards.size, areaWidth, spacing, cardW)
                if (idx >= 0) {
                    val cardCenter = calculateCardCenterInRoot(idx, cards.size, areaWidth, originInRoot, cardW, cardH, lift, centerY, spacing)
                    onCardClick(cards[idx], idx, cardCenter)
                }
            }
        }
    ) {
        cards.forEachIndexed { index, card ->
            val liftPx = if (isMyTurn && discardTop != null && canPlay(card, discardTop)) lift else 0f
            val angle = if (cards.size == 1) 0f else (index - (cards.size - 1) / 2f) * (fanAngle / (cards.size - 1))
            val cardCenterX = areaWidth / 2 + (index - (cards.size - 1) / 2f) * spacing

            rotate(degrees = angle, pivot = Offset(cardCenterX, centerY + cardH / 2)) {
                translate(left = cardCenterX - cardW / 2, top = centerY - cardH / 2 - liftPx) {
                    drawCard(card, Offset(cardW / 2, cardH / 2), 1f, drawShadow = true, textMeasurer = textMeasurer)
                }
            }
        }
    }
}

private fun findClickedCardIndexDynamic(tap: Offset, n: Int, areaW: Float, spacing: Float, cardW: Float): Int {
    if (n == 1) return if (tap.x in (areaW/2 - cardW/2)..(areaW/2 + cardW/2)) 0 else -1
    val dx = tap.x - areaW / 2
    return ((dx / spacing) + (n - 1) / 2f).roundToInt().coerceIn(0, n - 1)
}

private fun calculateCardCenterInRoot(
    index: Int, n: Int, areaW: Float, origin: Offset,
    cardW: Float, cardH: Float, lift: Float, centerY: Float, spacing: Float
): Offset {
    val cardCenterX = areaW / 2 + (index - (n - 1) / 2f) * spacing
    return Offset(origin.x + cardCenterX, origin.y + centerY - cardH / 2 - (if (lift > 0) lift else 0f))
}

private fun DrawScope.drawCard(card: Card, center: Offset, scale: Float = 1f, drawShadow: Boolean = false, textMeasurer: TextMeasurer) {
    val w = 60.dp.toPx() * scale; val h = 88.dp.toPx() * scale
    val corner = 12.dp.toPx() * scale
    val bg = cardColorToColor(card.color)
    val left = center.x - w/2; val top = center.y - h/2

    if (drawShadow) drawRoundRect(Color.Black.copy(alpha=0.3f), Offset(left+4.dp.toPx(), top+4.dp.toPx()), Size(w,h), CornerRadius(corner))
    drawRoundRect(bg, Offset(left,top), Size(w,h), CornerRadius(corner))
    drawRoundRect(Color.White.copy(alpha=0.3f), Offset(left+2.dp.toPx(), top+2.dp.toPx()), Size(w-4.dp.toPx(), h-4.dp.toPx()), CornerRadius(corner-2.dp.toPx()), style = Stroke(width = 1.dp.toPx()))

    val txt = valueToText(card.value)
    val txtColor = if (card.color == CardColor.YELLOW || card.color == CardColor.WILD) Color.Black else Color.White
    val textLayoutResult = textMeasurer.measure(text = txt, style = TextStyle(fontSize = (24.sp).value.sp, fontWeight = FontWeight.Bold, color = txtColor))
    drawText(textLayoutResult = textLayoutResult, topLeft = Offset(center.x - textLayoutResult.size.width/2, center.y - textLayoutResult.size.height/2 + 10.dp.toPx() * scale))
}

private fun cardColorToColor(c: CardColor) = when(c) {
    CardColor.RED -> Color(0xFFE53935); CardColor.BLUE -> Color(0xFF1E88E5); CardColor.GREEN -> Color(0xFF43A047)
    CardColor.YELLOW -> Color(0xFFFDD835); CardColor.WILD -> Color(0xFF8E24AA)
}

private fun valueToText(v: CardValue) = when(v) {
    is CardValue.Number -> v.number.toString(); is CardValue.Skip -> "⊘"; is CardValue.Reverse -> "↺"
    is CardValue.DrawTwo -> "+2"; is CardValue.Wild -> "W"; is CardValue.WildDrawFour -> "+4"
}

private fun canPlay(card: Card, top: Card) = when {
    card.color == CardColor.WILD || top.color == CardColor.WILD -> true
    card.color == top.color -> true
    card.value == top.value -> true
    else -> false
}

@Composable
fun WinnerBanner(winner: String) = Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)), contentAlignment = Alignment.Center) {
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = UnoGraphicsSpec.accentColor),
        elevation = CardDefaults.cardElevation(24.dp),
        modifier = Modifier.scaleIn()
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏆", fontSize = 64.sp, modifier = Modifier.pulse())
            Text("برنده", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = UnoGraphicsSpec.secondaryColor)
            Text(winner, fontSize = 36.sp, fontWeight = FontWeight.Black, color = UnoGraphicsSpec.backgroundColor)
        }
    }
}