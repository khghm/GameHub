package com.gamehub.server.modules

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.*
import kotlinx.serialization.json.*

// Temporary placeholder until WebSocket integration with new MatchmakingService is complete
class MatchmakingWebSocketHandler {
    suspend fun handle(session: DefaultWebSocketServerSession) {
        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Matchmaking WebSocket received: $text (placeholder - use REST API for now)")
                }
            }
        } catch (e: Exception) {
            println("Matchmaking WebSocket disconnected: ${e.message}")
        }
    }
}