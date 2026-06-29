// host/src/main/java/com/gamehub/host/viewmodel/GameViewModel.kt
package com.gamehub.host.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamehub.games.chess.ChessAction
import com.gamehub.games.farkle.FarkleAction
import com.gamehub.games.esmofamil.EsmoFamilAction
import com.gamehub.games.monopoly.MonopolyAction
import com.gamehub.games.backgammon.BackgammonAction
import com.gamehub.games.nard.NardAction
import com.gamehub.games.abalone.AbaloneAction
import com.gamehub.games.spadesbaloot.SpadesBalootAction
import com.gamehub.games.othello.OthelloAction
import com.gamehub.games.baltazar.BaltazarAction
import com.gamehub.games.bridge.BridgeAction
import com.gamehub.games.checkers.CheckersAction
import com.gamehub.games.blokus.BlokusAction
import com.gamehub.games.yahtzee.YahtzeeAction
import com.gamehub.games.battleship.BattleshipAction
import com.gamehub.games.matchmonster.MatchMonsterAction
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.host.network.ApiClient
import com.gamehub.host.network.GameClient
import com.gamehub.host.network.gameJson
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.networking.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

class GameViewModel : ViewModel() {
    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state

    data class ChatMessageItem(
        val from: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessageItem>> = _chatMessages

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _replyTarget = MutableStateFlow<ChatMessageItem?>(null)
    val replyTarget: StateFlow<ChatMessageItem?> = _replyTarget

    private var currentGameId: String = ""
    var playerId: PlayerId = PlayerId("player1")
        private set

    private val client = GameClient()
    var authToken: String? = null

    fun startGame(selectedGameId: String, playerName: String = "player1", sessionId: String = "", userId: String = "") {
        if (_state.value.isNotEmpty()) disconnect()
        currentGameId = if (sessionId.isNotEmpty()) sessionId else selectedGameId
        playerId = PlayerId(if (userId.isNotEmpty()) userId else playerName)

        client.authToken = authToken
        val serverIp = com.gamehub.host.BuildConfig.SERVER_IP

        viewModelScope.launch {
            try {
                if (sessionId.isNotEmpty()) {
                    client.joinGame("ws://$serverIp:8080/game", sessionId)
                } else {
                    client.connect("ws://$serverIp:8080/game", selectedGameId)
                }
                client.incomingMessages.collect { msg ->
                    println("📥 GameViewModel received message type: ${msg::class.simpleName}, full msg: $msg")
                    when (msg) {
                        is GameStateUpdateMsg -> {
                            println("📥 GameStateUpdateMsg payload: ${msg.statePayload}")
                            _state.value = msg.statePayload.toString()
                        }
                        is GameOverMsg -> {
                            val winner = msg.winnerId ?: "draw"
                            _state.value = "winner=$winner"
                        }
                        is ChatMessageMsg -> {
                            val item = ChatMessageItem(
                                from = msg.senderId,
                                message = msg.content.body,
                                timestamp = System.currentTimeMillis()
                            )
                            _chatMessages.value = _chatMessages.value + item
                        }
                        else -> {}
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun sendAction(row: Int, col: Int) {
        viewModelScope.launch { client.sendMove(currentGameId, """{"row":$row,"col":$col}""") }
    }

    fun sendUnoAction(actionType: String, cardIndex: Int, chosenColor: String? = null) {
        viewModelScope.launch {
            val colorPart = if (chosenColor != null) """, "chosenColor":"$chosenColor"""" else ""
            client.sendMove(currentGameId, """{"actionType":"$actionType","cardIndex":$cardIndex$colorPart}""")
        }
    }

    fun sendConnectFourAction(col: Int) {
        viewModelScope.launch { client.sendMove(currentGameId, """{"col":$col}""") }
    }

    fun sendLudoAction() {
        viewModelScope.launch { client.sendMove(currentGameId, """{"actionType":"roll"}""") }
    }

    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            client.sendChat("game", currentGameId, message, playerId.value)
        }
    }

    fun toggleMute() { _isMuted.value = !_isMuted.value }
    fun setReplyTarget(msg: ChatMessageItem?) { _replyTarget.value = msg }

    fun getUserProfile(username: String, callback: (UserProfile?) -> Unit) {
        viewModelScope.launch {
            val profile = ApiClient().getUserProfile(username)
            callback(profile)
        }
    }

    fun disconnect() {
        viewModelScope.launch { client.disconnect() }
        currentGameId = ""
    }

    override fun onCleared() {
        viewModelScope.launch { client.disconnect() }
        super.onCleared()
    }

    fun sendLudoMoveAction(pieceIndex: Int) {
        viewModelScope.launch {
            client.sendMove(currentGameId, """{"actionType":"move","pieceIndex":$pieceIndex}""")
        }
    }

    fun sendMonopolyAction(action: MonopolyAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(MonopolyAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendChessAction(action: ChessAction) {
        viewModelScope.launch {
            val jsonStr = when (action) {
                is ChessAction.Move -> gameJson.encodeToString(ChessAction.Move.serializer(), action)
            }
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendFarkleAction(action: FarkleAction) {
        viewModelScope.launch {
            val jsonStr = when (action) {
                is FarkleAction.RollDice -> """{"actionType":"roll"}"""
                is FarkleAction.SelectDice -> """{"actionType":"select","diceIds":[${action.diceIds.joinToString(",")}]}"""
                is FarkleAction.BankScore -> """{"actionType":"bank"}"""
                is FarkleAction.ContinueHotDice -> """{"actionType":"continue"}"""
            }
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendEsmoFamilAction(action: EsmoFamilAction) {
        viewModelScope.launch {
            val jsonStr = when (action) {
                is EsmoFamilAction.SubmitAnswers -> {
                    // Build a JSON string for answers
                    val answersParts = action.answers.entries.joinToString(",") { (idx, ans) ->
                        val escapedAns = ans?.replace("\"", "\\\"") ?: ""
                        """"$idx":"$escapedAns""""
                    }
                    """{$answersParts}"""
                }
            }
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendBackgammonAction(action: BackgammonAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendNardAction(action: NardAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendAbaloneAction(action: AbaloneAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendSpadesBalootAction(action: SpadesBalootAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendOthelloAction(action: OthelloAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendBaltazarAction(action: BaltazarAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendBridgeAction(action: BridgeAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendCheckersAction(action: CheckersAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendBlokusAction(action: BlokusAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendYahtzeeAction(action: YahtzeeAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }



    fun sendBattleshipAction(action: BattleshipAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendMatchMonsterAction(action: MatchMonsterAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun sendSoccerStrikerAction(action: SoccerStrikerAction) {
        viewModelScope.launch {
            val jsonStr = gameJson.encodeToString(GameAction.serializer(), action)
            client.sendMove(currentGameId, jsonStr)
        }
    }

    fun onTurnStart() {
        client.onTurnStart()
    }
}