package com.gamehub.server.wal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.UUID

/**
 * مثال استفاده از سیستم Write-Ahead Log در بازی
 */
class WalExampleUsage(
    private val walService: WriteAheadLogService
) {
    
    suspend fun exampleGameSession() {
        val gameSessionId = UUID.randomUUID()
        val playerId = UUID.randomUUID()
        val gameType = "tictactoe"
        
        // مثال 1: ثبت حرکت بازیکن
        val movePayload = buildJsonObject {
            put("x", 1)
            put("y", 2)
            put("player", "X")
        }
        
        val moveEvent = walService.logEvent(
            gameSessionId = gameSessionId,
            gameType = gameType,
            eventType = "player_move",
            playerId = playerId,
            payload = movePayload
        )
        
        println("ثیت حرکت: ${moveEvent.eventId}")
        
        // مثال 2: ثبت شروع بازی
        val startPayload = buildJsonObject {
            putJsonArray("players") {
                add(kotlinx.serialization.json.JsonPrimitive("player1"))
                add(kotlinx.serialization.json.JsonPrimitive("player2"))
            }
            put("startingPlayer", "X")
        }
        
        val startEvent = walService.logEvent(
            gameSessionId = gameSessionId,
            gameType = gameType,
            eventType = "game_start",
            playerId = null,
            payload = startPayload
        )
        
        println("ثیت شروع بازی: ${startEvent.eventId}")
        
        // مثال 3: بازیابی رویدادها برای ریکاوری
        val events = walService.getEventsForSession(gameSessionId)
        println("تعداد رویدادهای بازی: ${events.size}")
        
        // مثال 4: علامت‌گذاری رویدادها به عنوان اعمال‌شده
        walService.markEventsAsApplied(events.map { it.eventId })
        
        // مثال 5: ری‌پلای رویدادها
        walService.replayEvents(gameSessionId) { event ->
            println("ری‌پلای رویداد: ${event.eventType} - Seq: ${event.sequenceNumber}")
        }
    }
}
