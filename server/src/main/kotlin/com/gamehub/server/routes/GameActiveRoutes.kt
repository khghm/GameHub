
package com.gamehub.server.routes

import com.gamehub.server.cache.CircuitBreakerCacheProvider
import com.gamehub.server.modules.AuthModule
import com.gamehub.shared.engine.GameSnapshot
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.gameActiveRoutes(
    authModule: AuthModule,
    cacheProvider: CircuitBreakerCacheProvider
) {
    get("/api/games/active") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val activeGamesSet = cacheProvider.smembers("user:active_games:${user.id}")
        val gameInfos = mutableListOf<Map<String, Any>>()
        for (gameId in activeGamesSet) {
            val snapshotJson = cacheProvider.get("snapshot:$gameId")
            if (snapshotJson != null) {
                val snapshot = com.gamehub.server.serverGameJson.decodeFromString(GameSnapshot.serializer(), snapshotJson)
                gameInfos.add(mapOf(
                    "gameId" to gameId,
                    "gameType" to snapshot.gameType,
                    "players" to snapshot.players,
                    "lastUpdate" to snapshot.version
                ))
            }
        }
        call.respondText(com.gamehub.server.serverGameJson.encodeToString(gameInfos), ContentType.Application.Json)
    }
}

