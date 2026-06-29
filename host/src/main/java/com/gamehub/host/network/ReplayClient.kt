package com.gamehub.host.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.concurrent.TimeUnit

class ReplayClient {
    private val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
    private val baseUrl = "http://$serverIp:8080"
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(10, TimeUnit.SECONDS)
                writeTimeout(10, TimeUnit.SECONDS)
            }
        }
    }

    suspend fun getGameEvents(gameSessionId: String): List<GameEvent> {
        println("📱 ReplayClient: Calling getGameEvents for sessionId: $gameSessionId")
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/replay/$gameSessionId") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val responseBody = response.bodyAsText()
            println("📱 ReplayClient: Response status: ${response.status}")
            println("📱 ReplayClient: Response body: $responseBody")
            val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
            val events = jsonArray.mapNotNull { GameEvent.fromJson(it.jsonObject) }
            println("📱 ReplayClient: Parsed ${events.size} events")
            events
        } catch (e: Exception) {
            println("📱 ReplayClient: Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    data class GameEvent(
        val eventId: String,
        val gameSessionId: String,
        val gameType: String,
        val eventType: String,
        val playerId: String?,
        val timestamp: String,
        val sequenceNumber: Long,
        val payload: JsonObject,
        val isApplied: Boolean,
        val appliedAt: String?
    ) {
        companion object {
            fun fromJson(json: JsonObject): GameEvent {
                val timestampVal = json["timestamp"]?.jsonPrimitive
                val timestampStr = timestampVal?.let {
                    try {
                        it.long.toString()
                    } catch (e: Exception) {
                        it.content
                    }
                } ?: ""
                val appliedAtVal = json["appliedAt"]?.jsonPrimitive
                val appliedAtStr = appliedAtVal?.let {
                    try {
                        it.long.toString()
                    } catch (e: Exception) {
                        it.content
                    }
                }
                return GameEvent(
                    eventId = json["eventId"]?.jsonPrimitive?.content ?: "",
                    gameSessionId = json["gameSessionId"]?.jsonPrimitive?.content ?: "",
                    gameType = json["gameType"]?.jsonPrimitive?.content ?: "",
                    eventType = json["eventType"]?.jsonPrimitive?.content ?: "",
                    playerId = json["playerId"]?.jsonPrimitive?.contentOrNull,
                    timestamp = timestampStr,
                    sequenceNumber = json["sequenceNumber"]?.jsonPrimitive?.long ?: 0L,
                    payload = json["payload"]?.jsonObject ?: JsonObject(emptyMap()),
                    isApplied = json["isApplied"]?.jsonPrimitive?.boolean ?: false,
                    appliedAt = appliedAtStr
                )
            }
        }
    }
}
