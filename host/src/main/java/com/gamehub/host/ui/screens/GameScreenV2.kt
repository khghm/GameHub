package com.gamehub.host.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.games.chess.ChessAction
import com.gamehub.games.chess.ChessState
import com.gamehub.games.chess.ui.ChessScreen
import com.gamehub.games.farkle.FarkleState
import com.gamehub.games.farkle.ui.FarkleScreen
import com.gamehub.games.esmofamil.EsmoFamilState
import com.gamehub.games.esmofamil.ui.EsmoFamilScreen
import com.gamehub.games.backgammon.BackgammonAction
import com.gamehub.games.backgammon.BackgammonState
import com.gamehub.games.backgammon.ui.BackgammonScreen
import com.gamehub.games.nard.NardState
import com.gamehub.games.nard.NardAction
import com.gamehub.games.nard.ui.NardScreen
import com.gamehub.games.connectfour.ConnectFourState
import com.gamehub.games.connectfour.ui.ConnectFourScreen
import com.gamehub.games.connectfour.ui.ConnectFourGuide
import com.gamehub.games.ludo.LudoAction
import com.gamehub.games.ludo.LudoState
import com.gamehub.games.ludo.ui.LudoScreen
import com.gamehub.games.ludo.ui.LudoGuide
import com.gamehub.games.tictactoe.TicTacToeState
import com.gamehub.games.tictactoe.ui.TicTacToeScreen
import com.gamehub.games.tictactoe.ui.TicTacToeGuide
import com.gamehub.games.uno.UnoState
import com.gamehub.games.uno.ui.UnoScreen
import com.gamehub.games.uno.ui.UnoGuide
import com.gamehub.host.network.gameJson
import com.gamehub.host.viewmodel.AuthViewModel
import com.gamehub.host.viewmodel.GameViewModel
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.card.CardAction
import com.gamehub.shared.engines.card.CardColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.gamehub.games.backgammon.BackgammonColor
import com.gamehub.games.monopoly.MonopolyState
import com.gamehub.games.monopoly.ui.MonopolyScreen
import com.gamehub.games.abalone.AbaloneState
import com.gamehub.games.abalone.ui.AbaloneScreen
import com.gamehub.games.abalone.ui.AbaloneGuide
import com.gamehub.games.spadesbaloot.SpadesBalootState
import com.gamehub.games.spadesbaloot.ui.SpadesBalootScreen
import com.gamehub.games.spadesbaloot.ui.SpadesBalootGuide
import com.gamehub.games.othello.OthelloState
import com.gamehub.games.othello.OthelloAction
import com.gamehub.games.othello.ui.OthelloScreen
import com.gamehub.games.othello.ui.OthelloGuide
import com.gamehub.games.baltazar.BaltazarState
import com.gamehub.games.baltazar.BaltazarAction
import com.gamehub.games.baltazar.ui.BaltazarScreen
import com.gamehub.games.baltazar.ui.BaltazarGuide
import com.gamehub.games.bridge.BridgeState
import com.gamehub.games.bridge.ui.BridgeScreen
import com.gamehub.games.checkers.CheckersState
import com.gamehub.games.checkers.CheckersAction
import com.gamehub.games.checkers.ui.CheckersScreen
import com.gamehub.games.checkers.ui.CheckersGuide
import com.gamehub.games.blokus.BlokusState
import com.gamehub.games.blokus.ui.BlokusScreen
import com.gamehub.games.blokus.ui.BlokusGuide
import com.gamehub.games.yahtzee.YahtzeeState
import com.gamehub.games.yahtzee.ui.YahtzeeScreen
import com.gamehub.games.yahtzee.ui.YahtzeeGuide
import com.gamehub.games.bridge.ui.BridgeGuide
import com.gamehub.games.nard.ui.NardGuide
import com.gamehub.games.hex.HexState
import com.gamehub.games.hex.ui.HexScreen
import com.gamehub.games.hex.ui.HexGuide
import com.gamehub.games.battleship.BattleshipState
import com.gamehub.games.battleship.BattleshipAction
import com.gamehub.games.battleship.ui.BattleshipScreen
import com.gamehub.games.battleship.ui.BattleshipGuide
import com.gamehub.games.matchmonster.MatchMonsterState
import com.gamehub.games.matchmonster.MatchMonsterAction
import com.gamehub.games.matchmonster.ui.MatchMonsterScreen
import com.gamehub.games.matchmonster.ui.MatchMonsterGuide
import com.gamehub.games.soccerstriker.SoccerStrikerState
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.games.soccerstriker.ui.SoccerStrikerScreen
import com.gamehub.games.soccerstriker.ui.SoccerStrikerGuide
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * تلاش برای پارس GameState با استفاده از کتابخانهٔ استاندارد چندریختی.
 * اگر با موفقیت انجام شود، کلاس دقیق بازی برگردانده می‌شود.
 */
fun parseGameState(json: String): GameState? {
    if (json.isBlank() || json.startsWith("winner=")) return null
    return try {
        gameJson.decodeFromString(GameState.serializer(), json)
    } catch (e: Exception) {
        Log.e("GameScreen", "خطا در پارس GameState: ${e.message}")
        null
    }
}

/**
 * تلاش برای پارس GameResult (Win/Draw) با کتابخانهٔ استاندارد.
 */
// در GameScreenV2.kt، جایگزین تابع parseGameResult قدیمی
fun parseGameResult(stateJson: String): GameResult? {
    // حالت اول: اگر رشته ساده "winner=..." باشد
    if (stateJson.startsWith("winner=")) {
        val winnerId = stateJson.substringAfter("winner=")
        if (winnerId.isNotEmpty() && winnerId != "draw") {
            return GameResult.Win(PlayerId(winnerId))
        } else if (winnerId == "draw") {
            return GameResult.Draw
        }
        return null
    }

    // حالت دوم: اگر JSON کامل GameOverMsg باشد (با winnerId)
    return try {
        val obj = gameJson.parseToJsonElement(stateJson).jsonObject
        val winnerId = obj["winnerId"]?.jsonPrimitive?.contentOrNull
        if (winnerId != null) {
            GameResult.Win(PlayerId(winnerId))
        } else {
            val resultPayload = obj["resultPayload"]?.toString()
            if (resultPayload != null) {
                gameJson.decodeFromString(GameResult.serializer(), resultPayload)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun GameScreen(
    gameId: String,
    playerId: String,
    sessionId: String = "",
    onPlayAgain: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val stateJson by viewModel.state.collectAsState()
    var showGuide by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var lastTurnStartTime by remember { mutableStateOf(0L) }
    val activity = LocalContext.current as Activity
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = activity as ViewModelStoreOwner)
    val currentUserId = authViewModel.currentUserId.collectAsState().value
    val currentUser = authViewModel.currentUser.collectAsState().value
    val finalPlayerName = currentUser?.username?.takeIf { it.isNotBlank() } ?: "Player_${(1000..9999).random()}"

    LaunchedEffect(gameId, finalPlayerName, sessionId, currentUserId) {
        viewModel.authToken = com.gamehub.host.network.GlobalAuth.token
        viewModel.startGame(gameId, finalPlayerName, sessionId, currentUserId)
    }
    LaunchedEffect(stateJson) {
        val state = parseGameState(stateJson)
        val currentPlayerId = when (state) {
            is TicTacToeState -> state.currentPlayer?.value
            is ConnectFourState -> state.currentPlayer?.value
            is UnoState -> state.currentPlayer?.value
            is LudoState -> state.currentPlayer?.value
            is MonopolyState -> state.currentPlayer?.value
            is ChessState -> state.currentPlayer?.value
            is FarkleState -> state.currentPlayer?.value
            is EsmoFamilState -> state.currentPlayer?.value
            is BackgammonState -> state.currentPlayer?.value
            is NardState -> state.currentPlayer?.value
            is AbaloneState -> state.currentPlayer?.value
            is BaltazarState -> state.currentPlayer?.value
            is OthelloState -> state.currentPlayer?.value
            is SpadesBalootState -> state.currentPlayer?.value
            is BridgeState -> state.currentPlayer?.value
            is CheckersState -> state.currentPlayer?.value
            is BlokusState -> state.currentPlayer?.value
            is YahtzeeState -> state.currentPlayer?.value
            is HexState -> state.currentPlayer?.value
            is BattleshipState -> state.currentPlayer?.value
            is MatchMonsterState -> state.currentPlayer?.value
            is SoccerStrikerState -> state.currentPlayer?.value
            else -> null
        }
        if (currentPlayerId == playerId && lastTurnStartTime == 0L) {
            lastTurnStartTime = System.currentTimeMillis()
            viewModel.onTurnStart()  // باید در GameViewModel اضافه شود
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disconnect() }
    }

    if (stateJson.isBlank()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1A237E)),
            contentAlignment = Alignment.Center
        ) {
            Text("در انتظار حریف...", color = Color.White)
        }
        return
    }

    val gameState = remember(stateJson) { parseGameState(stateJson) }
    val gameResult = remember(stateJson) { parseGameResult(stateJson) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            gameResult != null -> {
                // پایان بازی
                val msg = when (gameResult) {
                    is GameResult.Win -> "برنده: ${(gameResult as GameResult.Win).winner.value}"
                    is GameResult.Draw -> "بازی مساوی شد"
                }
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF1A237E)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🏁 بازی تمام شد!", color = Color.White, fontSize = 24.sp)
                    Text(msg, color = Color(0xFFFFD700), fontSize = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onPlayAgain) { Text("🎮 بازی دوباره") }
                }
            }
            gameState != null -> {
                // انتخاب درست UI بر اساس نوع GameState
                when (gameState) {
                    is TicTacToeState -> TicTacToeScreen(
                        state = gameState,
                        onCellClick = { row, col -> viewModel.sendAction(row, col) },
                        modifier = Modifier.fillMaxSize()
                    )
                    is ConnectFourState -> ConnectFourScreen(
                        state = gameState,
                        onColumnClick = { viewModel.sendConnectFourAction(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                    is UnoState -> UnoScreen(
                        state = gameState,
                        localPlayerId = viewModel.playerId.value,
                        onAction = { action ->
                            when (action) {
                                is CardAction.PlayCard -> {
                                    val hand = gameState.hands[PlayerId(viewModel.playerId.value)]?.cards
                                    val idx = hand?.indexOf(action.card) ?: -1
                                    if (idx >= 0) {
                                        val chosen = if (action.card.color == CardColor.WILD) "RED" else null
                                        viewModel.sendUnoAction("play", idx, chosen)
                                    }
                                }
                                is CardAction.DrawCard -> viewModel.sendUnoAction("draw", -1)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // درون GameScreenV2، بخش Ludo
                    is LudoState -> {
                        val context = LocalContext.current
                        val boardBitmap = remember {
                            try {
                                context.assets.open("ludo.jpg").use { input ->
                                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                                }
                            } catch (e: Exception) { null }
                        }

                        // بارگذاری یک تصویر برای هر رنگ
                        val pieceColorImages = remember {
                            val map = mutableMapOf<String, ImageBitmap>()
                            val colorMap = mapOf(
                                "blue" to "b",   // پیشوند فایل
                                "red" to "r",
                                "green" to "g",
                                "yellow" to "y"
                            )
                            for ((color, prefix) in colorMap) {
                                try {
                                    context.assets.open("pieces/m_${prefix}0.png").use { input ->
                                        BitmapFactory.decodeStream(input)?.asImageBitmap()?.let { map[color] = it }
                                    }
                                } catch (e: Exception) { /* ignore */ }
                            }
                            map
                        }

                        LudoScreen(
                            state = gameState,
                            localPlayerId = viewModel.playerId,
                            onAction = { action ->
                                when (action) {
                                    is LudoAction.RollDice -> viewModel.sendLudoAction()
                                    is LudoAction.MovePiece -> viewModel.sendLudoMoveAction(action.pieceIndex)
                                }
                            },
                            boardImage = boardBitmap,
                            pieceColorImages = pieceColorImages,
                        )
                    }
                    is MonopolyState -> {
                        MonopolyScreen(
                            state = gameState,
                            localPlayerId = viewModel.playerId,   // پاس دادن شناسه کاربر
                            onAction = { action -> viewModel.sendMonopolyAction(action) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is ChessState -> {
                        ChessScreen(
                            state = gameState,
                            onMove = { from, to, promotion ->
                                viewModel.sendChessAction(ChessAction.Move(from, to, promotion))
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is FarkleState -> {
                        FarkleScreen(
                            state = gameState,
                            onAction = { viewModel.sendFarkleAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is EsmoFamilState -> {
                        EsmoFamilScreen(
                            state = gameState,
                            currentPlayerId = viewModel.playerId.value,
                            onAction = { viewModel.sendEsmoFamilAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is BackgammonState -> {
                        BackgammonScreen(
                            state = gameState,
                            onAction = { viewModel.sendBackgammonAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is NardState -> {
                        NardScreen(
                            state = gameState,
                            onAction = { viewModel.sendNardAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is AbaloneState -> {
                        AbaloneScreen(
                            state = gameState,
                            onAction = { viewModel.sendAbaloneAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is SpadesBalootState -> {
                        SpadesBalootScreen(
                            state = gameState,
                            onAction = { viewModel.sendSpadesBalootAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is OthelloState -> {
                        OthelloScreen(
                            state = gameState,
                            onAction = { viewModel.sendOthelloAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is BaltazarState -> {
                        BaltazarScreen(
                            state = gameState,
                            onAction = { viewModel.sendBaltazarAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is BridgeState -> {
                        BridgeScreen(
                            state = gameState,
                            onAction = { viewModel.sendBridgeAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is CheckersState -> {
                        CheckersScreen(
                            state = gameState,
                            onAction = { viewModel.sendCheckersAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is BlokusState -> {
                        BlokusScreen(
                            state = gameState,
                            onAction = { action -> viewModel.sendBlokusAction(action) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is YahtzeeState -> {
                        YahtzeeScreen(
                            state = gameState,
                            onAction = { action -> viewModel.sendYahtzeeAction(action) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is HexState -> {
                        HexScreen(
                            state = gameState,
                            onCellClick = { row, col -> viewModel.sendAction(row, col) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is BattleshipState -> {
                        BattleshipScreen(
                            state = gameState,
                            onAction = { viewModel.sendBattleshipAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is MatchMonsterState -> {
                        MatchMonsterScreen(
                            state = gameState,
                            currentPlayerId = PlayerId(viewModel.playerId.value),
                            onAction = { viewModel.sendMatchMonsterAction(it) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is SoccerStrikerState -> {
                        SoccerStrikerScreen(
                            state = gameState,
                            onAction = { action -> viewModel.sendSoccerStrikerAction(action) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> Box(
                        Modifier.fillMaxSize().background(Color(0xFF1A237E)),
                        contentAlignment = Alignment.Center
                    ) { Text("نوع بازی ناشناخته", color = Color.White) }
                }
            }
            else -> {
                // نه State معتبر است و نه Result
                Box(
                    Modifier.fillMaxSize().background(Color(0xFF1A237E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("خطا در بارگذاری بازی", color = Color.White)
                }
            }
        }

        // دکمه‌های شناور راهنما و چت
        FloatingActionButton(
            onClick = { showGuide = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Text("؟") }

        FloatingActionButton(
            onClick = { showChat = !showChat },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) { Text("💬") }

        if (showChat) {
            InGameChatBar(
                viewModel = viewModel,
                localPlayerName = finalPlayerName,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            )
        }
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            confirmButton = { TextButton(onClick = { showGuide = false }) { Text("متوجه شدم") } },
            title = { Text("راهنمای بازی") },
            text = {
                Box(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    when (gameId) {
                        "tictactoe" -> TicTacToeGuide()
                        "uno" -> UnoGuide()
                        "connectfour" -> ConnectFourGuide()
                        "ludo" -> LudoGuide()
                        "backgammon" -> Text("راهنمایی برای تخته نرد به زودی اضافه می‌شود!")
                        "nard" -> NardGuide()
                        "abalone" -> AbaloneGuide()
                        "spades-baloot" -> SpadesBalootGuide()
                        "othello" -> OthelloGuide()
                        "baltazar" -> BaltazarGuide()
                        "bridge" -> BridgeGuide()
                        "checkers" -> CheckersGuide()
                        "blokus" -> BlokusGuide()
                        "yahtzee" -> YahtzeeGuide()
                        "hex" -> HexGuide()
                        "battleship" -> BattleshipGuide()
                        "match-monster" -> MatchMonsterGuide()
                        "soccer-striker" -> SoccerStrikerGuide()
                        else -> Text("راهنمایی موجود نیست.")
                    }
                }
            }
        )
    }
}