// host/src/main/java/com/gamehub/host/network/GameClient.kt
package com.gamehub.host.network

import com.gamehub.shared.core.GameMode
import com.gamehub.shared.networking.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonElement
import java.util.UUID

class GameClient {
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: WebSocketSession? = null
    var authToken: String? = null
    private var lastTurnStartTime: Long = 0L
    private val _incomingMessages = MutableSharedFlow<WsMessage>(replay = 1)
    val incomingMessages: SharedFlow<WsMessage> = _incomingMessages

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onTurnStart() {
        lastTurnStartTime = System.currentTimeMillis()
    }
    suspend fun connect(url: String, gameType: String) {
        val token = authToken ?: ""
        val fullUrl = "$url?token=$token"
        session = client.webSocketSession(fullUrl)

        // استفاده از MatchmakingRequestMsg برای درخواست بازی جدید
        val request = SoloRequest(userId = "", gameId = gameType, mode = GameMode.CASUAL)
        val msg = MatchmakingRequestMsg(request = request)
        val json = gameJson.encodeToString(WsMessage.serializer(), msg)
        session?.send(Frame.Text(json))

        startListening()
    }

    suspend fun joinGame(url: String, gameId: String) {
        val token = authToken ?: ""
        val fullUrl = "$url?token=$token"
        session = client.webSocketSession(fullUrl)

        // استفاده از ResumeGameMsg برای پیوستن به بازی موجود
        val msg = ResumeGameMsg(gameId = gameId, reconnectToken = null)
        val json = gameJson.encodeToString(WsMessage.serializer(), msg)
        session?.send(Frame.Text(json))

        startListening()
    }

    private fun startListening() {
        scope.launch {
            try {
                for (frame in session!!.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val msg = gameJson.decodeFromString(WsMessage.serializer(), text)
                            _incomingMessages.emit(msg)
                        } catch (e: Exception) {
                            // خطای پارس – نادیده گرفته شود
                        }
                    }
                }
            } catch (e: Exception) {
                // قطع ارتباط
            }
        }
    }

    suspend fun sendMove(gameId: String, movePayload: String) {
        val reactionMs = if (lastTurnStartTime > 0) {
            System.currentTimeMillis() - lastTurnStartTime
        } else null
        val payloadJson = gameJson.parseToJsonElement(movePayload)
        val moveMsg = SubmitMoveMsg(
            msgId = null,
            gameId = gameId,
            movePayload = payloadJson,
            clientMoveId = null,
            nonce = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            reactionMs = reactionMs
        )
        val json = gameJson.encodeToString(WsMessage.serializer(), moveMsg)
        session?.send(Frame.Text(json))
    }

    suspend fun sendChat(channelType: String, channelId: String, body: String, senderId: String) {
        val chatMsg = ChatMessageMsg(
            msgId = null,
            channelType = channelType,
            channelId = channelId,
            senderId = senderId,
            content = ChatContent(type = "text", body = body, metadata = emptyMap())
        )
        val json = gameJson.encodeToString(WsMessage.serializer(), chatMsg)
        session?.send(Frame.Text(json))
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}