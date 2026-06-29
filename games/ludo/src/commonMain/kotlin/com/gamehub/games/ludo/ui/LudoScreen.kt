package com.gamehub.games.ludo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.ludo.LudoAction
import com.gamehub.games.ludo.LudoBoardData
import com.gamehub.games.ludo.LudoState
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val playerColors = listOf(
    Color(0xFF1E88E5), // آبی
    Color(0xFFE53935), // قرمز
    Color(0xFF43A047), // سبز
    Color(0xFFFBC02D)  // زرد
)

private data class PieceHitInfo(
    val pieceIndex: Int,
    val color: String,
    val rect: Rect
)

@Composable
fun LudoScreen(
    state: LudoState,
    localPlayerId: PlayerId,
    onAction: (LudoAction) -> Unit,
    modifier: Modifier = Modifier,
    boardImage: ImageBitmap? = null,
    pieceColorImages: Map<String, ImageBitmap> = emptyMap()
) {
    val myIndex = state.players.indexOf(localPlayerId)
    val myColor = LudoBoardData.playerColors.getOrElse(myIndex) { "blue" }
    val isMyTurn = state.currentPlayer == localPlayerId && !state.gameOver

    val rotationAngle = if (state.players.size >= 2) myIndex * 360f / state.players.size else 0f

    LaunchedEffect(state.rolloutAvailable, isMyTurn) {
        if (isMyTurn && state.rolloutAvailable.size == 1) {
            delay(600)
            onAction(LudoAction.MovePiece(state.rolloutAvailable.first()))
        }
    }

    var pieceHitboxes by remember { mutableStateOf<List<PieceHitInfo>>(emptyList()) }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF5F5DC)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🎲 منچ", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (state.winner != null) Text("برنده: ${state.winner!!.value}", color = Color.Red, fontWeight = FontWeight.Bold)
                else if (isMyTurn) Text("نوبت شماست!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                else Text("منتظر حریف...", color = Color.Gray)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(state.message, fontSize = 16.sp, color = Color.DarkGray)
        Spacer(Modifier.height(8.dp))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (boardImage != null) {
                Image(boardImage, "Board", Modifier.fillMaxSize().rotate(rotationAngle), contentScale = ContentScale.Fit)
            } else {
                Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFFFFF8E1)) }
            }

            Canvas(Modifier.fillMaxSize().pointerInput(isMyTurn, state.rolloutAvailable, myColor) {
                if (!isMyTurn || state.rolloutAvailable.size <= 1) return@pointerInput
                detectTapGestures { tapOffset ->
                    val hit = pieceHitboxes.firstOrNull { info ->
                        info.rect.contains(tapOffset) && info.color == myColor && state.rolloutAvailable.contains(info.pieceIndex)
                    }
                    if (hit != null) onAction(LudoAction.MovePiece(hit.pieceIndex))
                }
            }) {
                val imgAspect = boardImage?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f
                val canvasAspect = size.width / size.height
                val boardW: Float; val boardH: Float; val offX: Float; val offY: Float
                if (canvasAspect > imgAspect) {
                    boardH = size.height; boardW = boardH * imgAspect
                    offX = (size.width - boardW) / 2; offY = 0f
                } else {
                    boardW = size.width; boardH = boardW / imgAspect
                    offX = 0f; offY = (size.height - boardH) / 2
                }
                pieceHitboxes = drawPieces(state, boardW, boardH, offX, offY, pieceColorImages, rotationAngle)
            }
        }

        Spacer(Modifier.height(12.dp))
        if (isMyTurn && state.canRollAgain && state.rolloutAvailable.isEmpty()) {
            Button(onClick = { onAction(LudoAction.RollDice) }, Modifier.size(80.dp), shape =  CircleShape,
                colors = ButtonDefaults.buttonColors(Color(0xFF1565C0))) {
                Text("🎲", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun DrawScope.drawPieces(
    state: LudoState, boardW: Float, boardH: Float, offX: Float, offY: Float,
    pieceColorImages: Map<String, ImageBitmap>, rotationAngle: Float
): List<PieceHitInfo> {
    val basePieceSize = boardW / 11f
    val cx = offX + boardW / 2
    val cy = offY + boardH / 2
    val rad = Math.toRadians(rotationAngle.toDouble())
    val hits = mutableListOf<PieceHitInfo>()

    // شمارش تعداد مهره‌ها در هر خانه برای تعیین مقیاس
    val cellCount = mutableMapOf<String, Int>()
    state.pieces.forEach { (color, pieces) ->
        pieces.forEach { piece ->
            val cellId = pieceToCellId(piece, color) ?: return@forEach
            cellCount[cellId] = (cellCount[cellId] ?: 0) + 1
        }
    }

    // رسم تک تک مهره‌ها
    state.pieces.forEach { (color, pieces) ->
        val colorIndex = LudoBoardData.playerColors.indexOf(color)
        val fallbackColor = playerColors.getOrElse(colorIndex) { Color.Gray }
        val colorImage = pieceColorImages[color]
        val needFlip = (colorIndex * 360f / state.players.size % 180f != 0f)

        pieces.forEachIndexed { idx, piece ->
            val cellId = pieceToCellId(piece, color) ?: return@forEachIndexed
            val coord = LudoBoardData.cellCoords[cellId] ?: return@forEachIndexed

            val origX = offX + coord.xPercent / 100f * boardW
            val origY = offY + coord.yPercent / 100f * boardH
            val dx = origX - cx
            val dy = origY - cy
            val baseX = (cx + dx * cos(rad) - dy * sin(rad)).toFloat()
            val baseY = (cy + dx * sin(rad) + dy * cos(rad)).toFloat()

            val count = cellCount[cellId] ?: 1
            val scale = if (count > 1) (1f - (count - 1) * 0.1f).coerceAtLeast(0.7f) else 1f
            val pieceSize = basePieceSize * scale

            // جابجایی ساده: فقط در راستای عمودی (بالا) برای دیده شدن همه‌ی مهره‌ها
            val sameCellPieces = state.pieces.flatMap { (c, ps) ->
                ps.filter { pieceToCellId(it, c) == cellId }.map { c to it }
            }
            val localIndex = sameCellPieces.indexOfFirst { it.first == color && it.second.id == piece.id }
            val offsetY_local = if (sameCellPieces.size > 1) {
                (localIndex - (sameCellPieces.size - 1) / 2f) * pieceSize * 0.3f
            } else 0f

            val posX = baseX
            val posY = baseY + offsetY_local

            val left = posX - pieceSize / 2
            val top = posY - pieceSize   // پایین تصویر روی مختصات

            hits.add(PieceHitInfo(idx, color, Rect(left, top, left + pieceSize, top + pieceSize)))

            if (colorImage != null) {
                withTransform({
                    if (needFlip) rotate(180f, Offset(posX, posY))
                }) {
                    drawImage(
                        image = colorImage,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(pieceSize.roundToInt(), pieceSize.roundToInt())
                    )
                }
            } else {
                drawCircle(fallbackColor, pieceSize / 2, Offset(posX, posY - pieceSize / 2))
            }
        }
    }
    return hits
}

private fun pieceToCellId(piece: com.gamehub.games.ludo.LudoPiece, color: String): String? = when (piece.state) {
    "IN_BASE" -> "${color.take(1)}p${piece.id.last().digitToInt() + 1}"
    "ON_TRACK" -> LudoBoardData.paths[color]?.getOrNull(piece.pathIndex)
    "HOME_COLUMN" -> "${color.take(1)}e${piece.homeColumnIndex + 1}"
    "FINISHED" -> "${color.take(1)}e6"
    else -> null
}