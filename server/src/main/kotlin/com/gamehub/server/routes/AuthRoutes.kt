
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.origin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

fun Route.authRoutes(
    authModule: AuthModule
) {
    route("/api/auth") {
        post("/guest") {
            val params = call.receive<Map<String, String>>()
            val deviceId = params["deviceId"] ?: ""
            val ip = call.request.headers["X-Forwarded-For"] ?: call.request.origin.remoteHost
            val result = authModule.guestLogin(deviceId, ip)
            call.respondText(result, ContentType.Application.Json)
        }

        post("/attest/request") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val challengeId = authModule.requestAttestationChallenge(user.id)
            call.respondText("""{"challengeId":"$challengeId"}""", ContentType.Application.Json)
        }

        post("/attest/verify") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val challengeId = params["challengeId"] ?: return@post call.respondText("{\"error\":\"Missing challengeId\"}", ContentType.Application.Json)
            val signature = params["signature"] ?: return@post call.respondText("{\"error\":\"Missing signature\"}", ContentType.Application.Json)
            val publicKey = params["publicKey"] ?: return@post call.respondText("{\"error\":\"Missing publicKey\"}", ContentType.Application.Json)
            val verified = authModule.verifyAttestation(user.id, challengeId, signature, publicKey)
            if (verified) {
                call.respondText("{\"success\":true,\"message\":\"Device attested\"}", ContentType.Application.Json)
            } else {
                call.respondText("{\"success\":false,\"message\":\"Verification failed\"}", ContentType.Application.Json)
            }
        }
    }

    post("/api/register") {
        val params = call.receive<Map<String, String>>()
        val username = params["username"] ?: ""
        val password = params["password"] ?: ""
        val displayName = params["displayName"] ?: username
        val result = authModule.register(username, password, displayName)
        call.respondText(result, ContentType.Application.Json)
    }

    post("/api/login") {
        val params = call.receive<Map<String, String>>()
        val username = params["username"] ?: ""
        val password = params["password"] ?: ""
        val result = authModule.login(username, password)
        call.respondText(result, ContentType.Application.Json)
    }
}

