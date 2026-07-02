
package com.gamehub.server.routes

import com.gamehub.server.modules.AuthModule
import com.gamehub.server.settings.SettingsService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

fun Route.settingsRoutes(
    authModule: AuthModule,
    settingsService: SettingsService
) {
    route("/api/settings") {
        get {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val settings = settingsService.getUserSettings(user.id)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(settings), ContentType.Application.Json)
        }

        patch {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val user = authModule.validateToken(token) ?: return@patch call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
            val patch = call.receive<JsonObject>()
            val newSettings = settingsService.updateUserSettings(user.id, patch)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(newSettings), ContentType.Application.Json)
        }
    }
}

