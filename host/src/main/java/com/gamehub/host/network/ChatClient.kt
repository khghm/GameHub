package com.gamehub.host.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class ReceivedChatMessage(
    val type: String = "chat",
    val from: String,
    val to: String = "all",
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val gameId: String? = null
)

class ChatClient {
    private val client = HttpClient(OkHttp) {
        install(WebSockets)
    }
    private var session: WebSocketSession? = null
    private val _messages = MutableSharedFlow<ReceivedChatMessage>(replay = 50)
    val messages: SharedFlow<ReceivedChatMessage> = _messages
    private val _onlineUsers = MutableSharedFlow<List<String>>(replay = 1)
    val onlineUsers: SharedFlow<List<String>> = _onlineUsers
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var playerName = ""

    suspend fun connect(serverUrl: String, name: String) {
        playerName = name
        println("ChatClient: Connecting to $serverUrl as $name")
        session = client.webSocketSession(serverUrl)

        val registerMsg = """{"type":"register","playerName":"$name"}"""
        session?.send(Frame.Text(registerMsg))

        scope.launch {
            try {
                for (frame in session!!.incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("ChatClient: Received: $text")
                        try {
                            val json = Json.parseToJsonElement(text).jsonObject
                            val type = json["type"]?.jsonPrimitive?.content
                            when (type) {
                                "userList" -> {
                                    val users = json["users"]?.jsonArray?.map {
                                        it.jsonPrimitive.content
                                    } ?: emptyList()
                                    _onlineUsers.emit(users)
                                }
                                else -> {
                                    val msg = ReceivedChatMessage(
                                        type = type ?: "chat",
                                        from = json["from"]?.jsonPrimitive?.content ?: "unknown",
                                        to = json["to"]?.jsonPrimitive?.content ?: "all",
                                        message = json["message"]?.jsonPrimitive?.content ?: "",
                                        timestamp = json["timestamp"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                                        gameId = json["gameId"]?.jsonPrimitive?.contentOrNull
                                    )
                                    _messages.emit(msg)
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            } catch (e: Exception) {
                println("ChatClient: Disconnected: ${e.message}")
            }
        }
    }

    suspend fun sendMessage(message: String, to: String = "all", gameId: String? = null) {
        val gameIdJson = if (gameId != null) """, "gameId":"$gameId"""" else ""
        val msg = """{"type":"chat","from":"$playerName","to":"$to","message":"$message","timestamp":${System.currentTimeMillis()}$gameIdJson}"""
        println("ChatClient: Sending: $msg")
        session?.send(Frame.Text(msg))
    }

    suspend fun disconnect() {
        session?.close()
        client.close()
    }
}