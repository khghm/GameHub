package com.gamehub.host.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit

class CustomLobbyClient {
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

    suspend fun createLobby(gameType: String, maxPlayers: Int = 2, isPrivate: Boolean = false): LobbyInfo? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/lobby/create") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"gameType":"$gameType","maxPlayers":$maxPlayers,"isPrivate":$isPrivate}""")
            }
            val body = response.bodyAsText()
            val json = Json.parseToJsonElement(body).jsonObject
            LobbyInfo.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPublicLobbies(gameType: String? = null): List<LobbyInfo> {
        return try {
            val url = if (gameType != null) "$baseUrl/api/lobby/public?gameType=$gameType" else "$baseUrl/api/lobby/public"
            val response: HttpResponse = client.get(url) {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { LobbyInfo.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun joinLobby(lobbyId: String): LobbyInfo? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/lobby/join") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"lobbyId":"$lobbyId"}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            LobbyInfo.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinLobbyByInviteCode(inviteCode: String): LobbyInfo? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/lobby/join") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"inviteCode":"$inviteCode"}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            LobbyInfo.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun leaveLobby(lobbyId: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/lobby/leave?lobbyId=$lobbyId") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun startGame(lobbyId: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/lobby/start?lobbyId=$lobbyId") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    data class LobbyInfo(
        val lobbyId: String,
        val gameType: String,
        val hostUserId: String,
        val players: List<String>,
        val maxPlayers: Int,
        val isPrivate: Boolean,
        val inviteCode: String,
        val isGameStarted: Boolean
    ) {
        companion object {
            fun fromJson(json: JsonObject): LobbyInfo {
                return LobbyInfo(
                    lobbyId = json["lobbyId"]?.jsonPrimitive?.content ?: "",
                    gameType = json["gameType"]?.jsonPrimitive?.content ?: "",
                    hostUserId = json["hostUserId"]?.jsonPrimitive?.content ?: "",
                    players = json["players"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    maxPlayers = json["maxPlayers"]?.jsonPrimitive?.int ?: 2,
                    isPrivate = json["isPrivate"]?.jsonPrimitive?.boolean ?: false,
                    inviteCode = json["inviteCode"]?.jsonPrimitive?.content ?: "",
                    isGameStarted = json["isGameStarted"]?.jsonPrimitive?.boolean ?: false
                )
            }
        }
    }
}
