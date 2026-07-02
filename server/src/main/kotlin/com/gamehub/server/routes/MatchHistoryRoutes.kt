
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.modules.MatchHistoryModule
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.matchHistoryRoutes(
    authModule: AuthModule
) {
    get("/api/matchhistory/{username}") {
        try {
            println("🌐 /api/matchhistory/{username} CALLED!")
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            println("🌐 Got token: ${if (token.isNotEmpty()) "****" else "empty"}")
            val user = authModule.validateToken(token)
            println("🌐 authModule.validateToken returned: $user")
            if (user == null) {
                return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            }
            val username = call.parameters["username"] ?: ""
            println("🌐 Requested username: $username, User from token: ${user.username}, ${user.id}")
            val history = MatchHistoryModule.getHistoryForUser(user.id)
            println("🌐 Returning ${history.size} matches!")
            val json = com.gamehub.server.serverGameJson.encodeToString(history)
            println("🌐 JSON response: $json")
            call.respondText(json, ContentType.Application.Json)
        } catch (e: Exception) {
            println("🌐 ERROR IN API: ${e.message}")
            e.printStackTrace()
            call.respondText("{\"error\":\"Internal server error\",\"message\":\"${e.message}\"}", status = io.ktor.http.HttpStatusCode.InternalServerError, contentType = ContentType.Application.Json)
        }
    }
}

