
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.notifications.NotificationService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.notificationRoutes(
    authModule: AuthModule,
    notificationService: NotificationService
) {
    route("/api/notifications") {
        get("/poll") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val notifications = notificationService.getNotificationsSince(user.id, since)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(notifications), ContentType.Application.Json)
        }

        post("/read/{id}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val id = call.parameters["id"] ?: return@post call.respondText("{\"error\":\"Missing id\"}", ContentType.Application.Json)
            notificationService.markAsRead(user.id, id)
            call.respondText("{\"success\":true}", ContentType.Application.Json)
        }

        get("/unread") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val count = notificationService.getUnreadCount(user.id)
            call.respondText("{\"count\":$count}", ContentType.Application.Json)
        }
    }
}

