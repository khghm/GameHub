package com.gamehub.server.modules

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.*
import kotlinx.serialization.json.*

class ChatWebSocketHandler {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        var playerName: String? = null

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Chat received: $text")

                    try {
                        val json = Json.parseToJsonElement(text).jsonObject
                        val type = json["type"]?.jsonPrimitive?.content

                        when (type) {
                            "register" -> {
                                playerName = json["playerName"]?.jsonPrimitive?.content ?: continue
                                ChatServer.registerUser(playerName!!, session)
                                println("User registered for chat: $playerName")
                            }
                            "chat" -> {
                                val from = json["from"]?.jsonPrimitive?.content ?: playerName ?: continue
                                val to = json["to"]?.jsonPrimitive?.content ?: "all"
                                val message = json["message"]?.jsonPrimitive?.content ?: continue
                                val gameId = json["gameId"]?.jsonPrimitive?.contentOrNull

                                ChatServer.sendMessage(
                                    ChatMessage(
                                        from = from,
                                        to = to,
                                        message = message,
                                        gameId = gameId
                                    )
                                )
                                println("Chat message from $from: $message")
                            }
                        }
                    } catch (e: Exception) {
                        println("Chat error: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Chat disconnected: ${e.message}")
        } finally {
            if (playerName != null) {
                ChatServer.unregisterUser(playerName!!)
                println("User unregistered from chat: $playerName")
            }
        }
    }
}