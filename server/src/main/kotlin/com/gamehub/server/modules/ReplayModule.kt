package com.gamehub.server.modules

import com.gamehub.server.replay.ReplayService
import com.gamehub.server.serverGameJson
import com.gamehub.server.wal.GameEvent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import java.util.UUID

fun Application.replayModule(replayService: ReplayService, authModule: AuthModule) {
    routing {
        route("/api/replay") {
            get("/{gameSessionId}") {
                println("🎬 Replay endpoint called!")
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val user = authModule.validateToken(token) ?: return@get call.respondText(
                    """{"error":"Unauthorized"}""",
                    ContentType.Application.Json
                )

                val gameSessionIdStr = call.parameters["gameSessionId"] ?: ""
                println("🎬 GameSessionId from param: $gameSessionIdStr")
                val gameSessionId = try {
                    UUID.fromString(gameSessionIdStr)
                } catch (e: Exception) {
                    println("❌ Invalid UUID: ${e.message}")
                    return@get call.respondText(
                        """{"error":"Invalid game session ID"}""",
                        ContentType.Application.Json
                    )
                }

                val events = replayService.getReplayEvents(gameSessionId)
                println("🎬 Found ${events.size} events for session $gameSessionId")
                val json = serverGameJson.encodeToString(events)
                println("🎬 JSON response: $json")
                call.respondText(json, ContentType.Application.Json)
            }

            get("/{gameSessionId}/from/{sequenceNumber}") {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val user = authModule.validateToken(token) ?: return@get call.respondText(
                    """{"error":"Unauthorized"}""",
                    ContentType.Application.Json
                )

                val gameSessionId = try {
                    UUID.fromString(call.parameters["gameSessionId"])
                } catch (e: Exception) {
                    return@get call.respondText(
                        """{"error":"Invalid game session ID"}""",
                        ContentType.Application.Json
                    )
                }

                val sequenceNumber = call.parameters["sequenceNumber"]?.toLongOrNull() ?: 0L

                val events = replayService.getReplayEventsFromSequence(gameSessionId, sequenceNumber)
                call.respondText(serverGameJson.encodeToString(events), ContentType.Application.Json)
            }
        }
    }
}
