
package com.gamehub.server.routes

import com.gamehub.server.anticheat.AntiCheatService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Route.testAntiCheatRoutes(
    antiCheatService: AntiCheatService
) {
    post("/api/test/cheat") {
        val params = call.receive<Map<String, String>>()
        val userId = params["userId"] ?: return@post call.respondText("{\"error\":\"Missing userId\"}", ContentType.Application.Json)
        val cheatType = params["cheatType"] ?: return@post call.respondText("{\"error\":\"Missing cheatType\"}", ContentType.Application.Json)
        val gameId = params["gameId"] ?: "test_game"
        val matchId = params["matchId"] ?: "test_match"

        when (cheatType) {
            "speed" -> {
                val clientSendTime = System.currentTimeMillis()
                val serverRecvTime = clientSendTime + 10
                antiCheatService.checkMove(
                    userId = userId,
                    sessionId = "test_session",
                    gameId = gameId,
                    matchId = matchId,
                    moveType = "normal",
                    clientSendTime = clientSendTime,
                    serverRecvTime = serverRecvTime,
                    signature = "",
                    reactionMs = 5
                )
            }
            "lag" -> {
                val rtts = listOf(50L, 300L, 50L, 350L, 50L)
                for (rtt in rtts) {
                    antiCheatService.checkLagSwitch(userId, matchId, rtt)
                }
            }
            "macro" -> {
                repeat(10) {
                    antiCheatService.checkMove(
                        userId = userId,
                        sessionId = "test_session",
                        gameId = gameId,
                        matchId = matchId,
                        moveType = "normal",
                        clientSendTime = System.currentTimeMillis(),
                        serverRecvTime = System.currentTimeMillis(),
                        signature = "",
                        reactionMs = 50
                    )
                }
            }
            "collusion" -> {
                val otherUserId = params["otherUserId"] ?: return@post call.respondText("{\"error\":\"Missing otherUserId\"}", ContentType.Application.Json)
                antiCheatService.checkCollusion(userId, otherUserId, gameId, matchId)
            }
            else -> return@post call.respondText("{\"error\":\"Unknown cheatType\"}", ContentType.Application.Json)
        }
        call.respondText("{\"success\":true,\"message\":\"Cheat simulation triggered\"}", ContentType.Application.Json)
    }
}

