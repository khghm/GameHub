
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.modules.MatchmakingRestHandler
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Route.matchmakingRoutes(
    authModule: AuthModule,
    matchmakingRestHandler: MatchmakingRestHandler
) {
    post("/api/matchmaking/join") {
        println("📡 Matchmaking request received!")
        val params = call.receive<Map<String, String>>()
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@post call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
        val gameType = params["gameType"] ?: "tictactoe"
        val mode = params["mode"] ?: "casual"
        val result = matchmakingRestHandler.joinQueue(user.id, user.username, gameType, mode)
        call.respondText(result, ContentType.Application.Json)
    }
}

