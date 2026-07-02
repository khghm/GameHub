
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.rating.RatingService
import com.gamehub.server.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.profileRoutes(
    authModule: AuthModule,
    userRepository: UserRepository,
    ratingService: RatingService,
    behaviorService: BehaviorService
) {
    get("/api/profile") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val profile = userRepository.findById(java.util.UUID.fromString(user.id))
        if (profile != null) {
            call.respondText(
                """{"username":"${profile.username}","displayName":"${profile.displayName ?: profile.username}","avatar":"${profile.avatarUrl ?: ""}","wins":0,"losses":0,"draws":0}""",
                ContentType.Application.Json
            )
        } else {
            call.respondText("{\"error\":\"User not found\"}", ContentType.Application.Json)
        }
    }

    get("/api/user/rating") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val gameId = call.request.queryParameters["gameId"] ?: "tictactoe"
        val rating = ratingService.getRating(user.id, gameId)
        call.respondText(com.gamehub.server.serverGameJson.encodeToString(rating), ContentType.Application.Json)
    }

    get("/api/user/behavior") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val behavior = behaviorService.getBehavior(user.id)
        call.respondText(com.gamehub.server.serverGameJson.encodeToString(behavior), ContentType.Application.Json)
    }
}

