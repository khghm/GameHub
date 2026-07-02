
package com.gamehub.server.routes

import com.gamehub.server.behavior.BehaviorService
import com.gamehub.server.economy.EconomyService
import com.gamehub.server.modules.AuthModule
import com.gamehub.server.modules.TournamentModule
import com.gamehub.server.rating.RatingService
import com.gamehub.server.security.JwtService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.tournamentRoutes(
    authModule: AuthModule,
    tournamentModule: TournamentModule,
    ratingService: RatingService,
    behaviorService: BehaviorService,
    economyService: EconomyService,
    jwtService: JwtService
) {
    route("/api/tournaments") {
        get {
            val tournaments = tournamentModule.getAllTournaments()
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(tournaments), ContentType.Application.Json)
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            val tournament = tournamentModule.getTournament(id)
            if (tournament != null) {
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(tournament), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Tournament not found\"}", ContentType.Application.Json)
            }
        }

        post("{id}/register") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)

            val tournamentId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing tournament id\"}", ContentType.Application.Json)

            val userRating = ratingService.getRating(user.id, "tictactoe")
            val userBehavior = behaviorService.getBehavior(user.id)

            val result = tournamentModule.registerUser(
                tournamentId = tournamentId,
                userId = user.id,
                userLevel = userRating.gamesPlayed / 10 + 1,
                userElo = userRating.rating,
                userBehaviorBand = userBehavior.band,
                economyService = economyService
            )

            call.respondText(com.gamehub.server.serverGameJson.encodeToString(result), ContentType.Application.Json)
        }

        delete("{id}/register") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@delete call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)

            val tournamentId = call.parameters["id"] ?: return@delete call.respondText("{\"error\":\"Missing tournament id\"}", ContentType.Application.Json)

            val success = tournamentModule.cancelRegistration(tournamentId, user.id, economyService)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }
    }

    post("/api/admin/tournaments/{id}/distribute-prizes") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val claims = jwtService.verifyToken(token)
        if (claims == null || claims.type != "admin") {
            call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
            return@post
        }
        val tournamentId = call.parameters["id"] ?: return@post call.respondText("""{"error":"Missing id\"}""", ContentType.Application.Json)
        val awarded = tournamentModule.distributePrizes(tournamentId, economyService)
        call.respondText(com.gamehub.server.serverGameJson.encodeToString(mapOf("awarded" to awarded)), ContentType.Application.Json)
    }
}

