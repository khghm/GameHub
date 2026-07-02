
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.modules.FriendsModule
import com.gamehub.server.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.friendsRoutes(
    authModule: AuthModule,
    friendsModule: FriendsModule,
    userRepository: UserRepository
) {
    route("/api/friends") {
        post("/request") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val friendUsername = params["friendUsername"] ?: ""
            val result = friendsModule.sendRequest(java.util.UUID.fromString(currentUser.id), friendUsername)
            call.respondText(kotlinx.serialization.json.Json.encodeToString(result), ContentType.Application.Json)
        }

        post("/accept") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val requestId = java.util.UUID.fromString(params["requestId"] ?: "")
            val result = friendsModule.acceptRequest(java.util.UUID.fromString(currentUser.id), requestId)
            call.respondText(kotlinx.serialization.json.Json.encodeToString(result), ContentType.Application.Json)
        }

        post("/reject") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val requestId = java.util.UUID.fromString(params["requestId"] ?: "")
            val result = friendsModule.rejectRequest(java.util.UUID.fromString(currentUser.id), requestId)
            call.respondText(kotlinx.serialization.json.Json.encodeToString(result), ContentType.Application.Json)
        }

        get {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val friends = friendsModule.getFriends(java.util.UUID.fromString(currentUser.id))
            val friendsJson = kotlinx.serialization.json.Json.encodeToString(friends)
            call.respondText("""{"friends":$friendsJson}""", ContentType.Application.Json)
        }

        get("/pending") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val pending = friendsModule.getPendingRequests(java.util.UUID.fromString(currentUser.id))
            call.respondText("""{"pending":${kotlinx.serialization.json.Json.encodeToString(pending)}}""", ContentType.Application.Json)
        }

        delete("/remove") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUser = authModule.validateToken(token) ?: return@delete call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val params = call.receive<Map<String, String>>()
            val friendUsername = params["friendUsername"] ?: ""

            val friendUser = userRepository.findByUsername(friendUsername)
            if (friendUser == null) {
                return@delete call.respondText("{\"success\":false,\"message\":\"Friend not found\"}", ContentType.Application.Json)
            }

            val result = friendsModule.unfriend(java.util.UUID.fromString(currentUser.id), friendUser.id!!)
            call.respondText(kotlinx.serialization.json.Json.encodeToString(result), ContentType.Application.Json)
        }
    }
}

