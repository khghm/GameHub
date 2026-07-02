
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.security.JwtService
import com.gamehub.server.society.MembershipCondition
import com.gamehub.server.society.SocietyService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import com.gamehub.server.modules.GameSessionManager

fun Route.societyRoutes(
    authModule: AuthModule,
    societyService: SocietyService,
    jwtService: JwtService
) {
    route("/api/societies") {
        get {
            val societies = societyService.getAllSocieties()
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(societies), ContentType.Application.Json)
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            val society = societyService.getSociety(id)
            if (society != null) {
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(society), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Not found\"}", ContentType.Application.Json)
            }
        }

        post("{id}/join") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            val success = societyService.requestJoin(user.id, id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/leave") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            val success = societyService.leaveSociety(user.id, id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/approve/{userId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val admin = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}", ContentType.Application.Json)
            val userId = call.parameters["userId"] ?: return@post call.respondText("{\"error\":\"Missing user id\"}", ContentType.Application.Json)
            val success = societyService.approveMember(admin.id, id, userId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/reject/{userId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val admin = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}", ContentType.Application.Json)
            val userId = call.parameters["userId"] ?: return@post call.respondText("{\"error\":\"Missing user id\"}", ContentType.Application.Json)
            val success = societyService.rejectMember(admin.id, id, userId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        get("{id}/members") {
            val id = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            val members = societyService.getSocietyMembers(id)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(members), ContentType.Application.Json)
        }

        post("{id}/game") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val societyId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing society id\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val gameType = params["gameType"] ?: "tictactoe"

            val userSocieties = societyService.getUserSocieties(user.id)
            if (!userSocieties.any { it.id == societyId }) {
                return@post call.respondText("{\"error\":\"Not a member of this society\"}", ContentType.Application.Json)
            }

            val gameId = com.gamehub.server.modules.GameSessionManager.createSession(gameType, listOf(user.id))
            call.respondText("""{"gameId":"$gameId"}""", ContentType.Application.Json)
        }
    }

    post("/api/admin/societies") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val claims = jwtService.verifyToken(token)
        if (claims == null || claims.type != "admin") {
            call.respondText("""{"error":"Unauthorized"}""", ContentType.Application.Json)
            return@post
        }
        val params = call.receive<Map<String, Any>>()
        val name = params["name"] as? String ?: return@post call.respondText("{\"error\":\"Missing name\"}", ContentType.Application.Json)
        val description = params["description"] as? String
        val maxMembers = (params["maxMembers"] as? Number)?.toInt() ?: 50000
        val membershipType = params["membershipType"] as? String ?: "open"
        val conditionMap = params["membershipCondition"] as? Map<*, *>
        val condition = conditionMap?.let {
            MembershipCondition(
                minLevel = it["minLevel"] as? Int,
                minElo = it["minElo"] as? Int,
                allowedBehaviorBands = it["allowedBehaviorBands"] as? List<String>,
                minGamesPlayed = it["minGamesPlayed"] as? Int,
                minWinRate = it["minWinRate"] as? Double
            )
        }
        val society = societyService.createSociety(name, description, claims.userId, maxMembers, membershipType, condition)
        if (society != null) {
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(society), ContentType.Application.Json)
        } else {
            call.respondText("{\"error\":\"Creation failed\"}", ContentType.Application.Json)
        }
    }
}

