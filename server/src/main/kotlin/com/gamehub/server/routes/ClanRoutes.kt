
package com.gamehub.server.routes

import com.gamehub.server.clan.ClanService
import com.gamehub.server.modules.AuthModule
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import com.gamehub.server.modules.GameSessionManager


fun Route.clanRoutes(
    authModule: AuthModule,
    clanService: ClanService
) {
    route("/api/clans") {
        post("/create") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val name = params["name"] ?: return@post call.respondText("{\"error\":\"Missing name\"}", ContentType.Application.Json)
            val tag = params["tag"] ?: return@post call.respondText("{\"error\":\"Missing tag\"}", ContentType.Application.Json)
            val clan = clanService.createClan(user.id, name, tag)
            if (clan != null) {
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(clan), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Clan creation failed\"}", ContentType.Application.Json)
            }
        }

        post("{id}/join") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val success = clanService.joinClan(user.id, clanId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/leave") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val success = clanService.leaveClan(user.id, clanId)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/upgrade") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val success = clanService.upgradeClan(clanId, user.id)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        post("{id}/contribute") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val amount = params["amount"]?.toLongOrNull() ?: return@post call.respondText("{\"error\":\"Missing amount\"}", ContentType.Application.Json)
            val success = clanService.contributeCoins(user.id, clanId, amount)
            call.respondText("""{"success":$success}""", ContentType.Application.Json)
        }

        get("{id}") {
            val clanId = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val clan = clanService.getClanInfo(clanId)
            if (clan != null) {
                call.respondText(com.gamehub.server.serverGameJson.encodeToString(clan), ContentType.Application.Json)
            } else {
                call.respondText("{\"error\":\"Clan not found\"}", ContentType.Application.Json)
            }
        }

        get("{id}/members") {
            val clanId = call.parameters["id"] ?: return@get call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val members = clanService.getClanMembers(clanId)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(members), ContentType.Application.Json)
        }

        post("{id}/game") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val clanId = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing clan id\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val gameType = params["gameType"] ?: "tictactoe"

            val userClan = clanService.getUserClan(user.id)
            if (userClan?.id != clanId) {
                return@post call.respondText("{\"error\":\"Not a member of this clan\"}", ContentType.Application.Json)
            }

            val gameId = com.gamehub.server.modules.GameSessionManager.createSession(gameType, listOf(user.id))
            call.respondText("""{"gameId":"$gameId"}""", ContentType.Application.Json)
        }
    }

    get("/api/user/clan") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val clan = clanService.getUserClan(user.id)
        if (clan != null) {
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(clan), ContentType.Application.Json)
        } else {
            call.respondText("{\"clan\":null}", ContentType.Application.Json)
        }
    }
}

