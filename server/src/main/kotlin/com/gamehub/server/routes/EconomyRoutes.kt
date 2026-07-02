
package com.gamehub.server.routes

import com.gamehub.server.economy.EconomyService
import com.gamehub.server.economy.ShopService
import com.gamehub.server.modules.AuthModule
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

fun Route.economyRoutes(
    authModule: AuthModule,
    economyService: EconomyService,
    shopService: ShopService
) {
    get("/api/wallet") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@get call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val wallet = economyService.getWallet(user.id)
        call.respondText(com.gamehub.server.serverGameJson.encodeToString(wallet), ContentType.Application.Json)
    }

    post("/api/shop/buy") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val params = call.receive<Map<String, String>>()
        val itemId = params["itemId"] ?: return@post call.respondText("{\"error\":\"Missing itemId\"}", ContentType.Application.Json)
        val quantity = params["quantity"]?.toIntOrNull() ?: 1
        val idempotencyKey = params["idempotencyKey"] ?: java.util.UUID.randomUUID().toString()
        try {
            val result = shopService.purchaseItem(user.id, itemId, quantity, idempotencyKey)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(result), ContentType.Application.Json)
        } catch (e: IllegalArgumentException) {
            call.respondText(
                text = "{\"error\":\"${e.message}\"}",
                status = HttpStatusCode.BadRequest,
                contentType = ContentType.Application.Json
            )
        } catch (e: IllegalStateException) {
            call.respondText(
                text = "{\"error\":\"${e.message}\"}",
                status = HttpStatusCode.BadRequest,
                contentType = ContentType.Application.Json
            )
        }
    }

    post("/api/refund") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val params = call.receive<Map<String, String>>()
        val purchaseId = params["purchaseId"] ?: return@post call.respondText("{\"error\":\"Missing purchaseId\"}", ContentType.Application.Json)
        val idempotencyKey = params["idempotencyKey"] ?: java.util.UUID.randomUUID().toString()
        try {
            val result = shopService.refundPurchase(user.id, purchaseId, idempotencyKey)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(result), ContentType.Application.Json)
        } catch (e: IllegalStateException) {
            call.respondText(
                text = "{\"error\":\"${e.message}\"}",
                status = HttpStatusCode.BadRequest,
                contentType = ContentType.Application.Json
            )
        }
    }

    post("/api/gift/coin") {
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
        val user = authModule.validateToken(token) ?: return@post call.respondText("{\"error\":\"Unauthorized\"}", ContentType.Application.Json)
        val params = call.receive<Map<String, String>>()
        val toUserId = params["toUserId"] ?: return@post call.respondText("{\"error\":\"Missing toUserId\"}", ContentType.Application.Json)
        val amount = params["amount"]?.toLongOrNull() ?: return@post call.respondText("{\"error\":\"Missing amount\"}", ContentType.Application.Json)
        val message = params["message"]
        val idempotencyKey = params["idempotencyKey"] ?: java.util.UUID.randomUUID().toString()
        try {
            val result = economyService.giftCoins(user.id, toUserId, amount, message, idempotencyKey)
            call.respondText(com.gamehub.server.serverGameJson.encodeToString(result), ContentType.Application.Json)
        } catch (e: IllegalArgumentException) {
            call.respondText(
                text = "{\"error\":\"${e.message}\"}",
                status = HttpStatusCode.BadRequest,
                contentType = ContentType.Application.Json
            )
        } catch (e: IllegalStateException) {
            call.respondText(
                text = "{\"error\":\"${e.message}\"}",
                status = HttpStatusCode.BadRequest,
                contentType = ContentType.Application.Json
            )
        }
    }
}

