package com.gamehub.host.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MatchmakingClient {
    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private var session: WebSocketSession? = null

    var authToken: String? = null

    suspend fun connect(url: String, gameType: String, playerName: String): String? {
        val token = authToken ?: ""
        val fullUrl = "$url?token=$token"
        try {
            session = client.webSocketSession(fullUrl)
        } catch (e: Exception) {
            return null
        }

        val joinMsg = """{"type":"join","gameType":"$gameType","playerName":"$playerName"}"""
        session?.send(Frame.Text(joinMsg))

        var matchedGameId: String? = null
        try {
            for (frame in session!!.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val json = Json.parseToJsonElement(text).jsonObject
                    if (json["type"]?.jsonPrimitive?.content == "MatchFound") {
                        matchedGameId = json["gameId"]?.jsonPrimitive?.content
                        // پیام Close را از سرور دریافت کن (اختیاری)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // اگر EOFException رخ داد، آن را نادیده بگیر و matchedGameId را برگردان
            if (e is java.io.EOFException) {
                // اشکالی ندارد، سرور اتصال را بست
            }
        } finally {
            disconnect()
        }

        return matchedGameId
    }

    suspend fun disconnect() {
        try { session?.close() } catch (_: Exception) {}
        try { client.close() } catch (_: Exception) {}
    }
}