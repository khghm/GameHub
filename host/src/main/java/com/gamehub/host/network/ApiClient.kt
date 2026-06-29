package com.gamehub.host.network

import com.gamehub.host.viewmodel.UserProfile
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.concurrent.TimeUnit

object GlobalAuth {
    var token: String? = null
        set(value) {
            println("🔐 GlobalAuth: Token set!")
            field = value
        }
}

class ApiClient {
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

    fun setToken(token: String) {
        GlobalAuth.token = token
    }

    suspend fun register(username: String, password: String, displayName: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","password":"$password","displayName":"$displayName"}""")
            }
            val body = response.bodyAsText()
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}","token":""}""").jsonObject
        }
    }

    suspend fun guestLogin(): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/auth/guest")
            val body = response.bodyAsText()
            println("🌐 Guest login response: $body")
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    suspend fun login(username: String, password: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"username":"$username","password":"$password"}""")
            }
            val body = response.bodyAsText()
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}","token":""}""").jsonObject
        }
    }

    suspend fun getProfile(): JsonObject {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/profile") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val body = response.bodyAsText()
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"error":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    suspend fun updateProfile(displayName: String, avatar: String): JsonObject {
        return try {
            val response: HttpResponse = client.put("$baseUrl/api/profile") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"displayName":"$displayName","avatar":"$avatar"}""")
            }
            val body = response.bodyAsText()
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"error":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    // Friend APIs
    suspend fun addFriend(friendUsername: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/friends/request") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"friendUsername":"$friendUsername"}""")
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    suspend fun getFriends(): JsonObject {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/friends") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"error":"Connection failed: ${e.message}","friends":[]}""").jsonObject
        }
    }

    suspend fun removeFriend(friendUsername: String): JsonObject {
        return try {
            val response: HttpResponse = client.delete("$baseUrl/api/friends/remove") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"friendUsername":"$friendUsername"}""")
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    suspend fun getLeaderboard(): String {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/leaderboard")
            response.bodyAsText()
        } catch (e: Exception) {
            "[]"
        }
    }

    suspend fun getUserProfile(username: String): UserProfile? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/user/$username") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val obj = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            if (obj.containsKey("error")) null
            else UserProfile(
                userId = obj["userId"]?.jsonPrimitive?.content ?: "",
                username = obj["username"]?.jsonPrimitive?.content ?: "",
                displayName = obj["displayName"]?.jsonPrimitive?.content ?: "",
                avatar = obj["avatar"]?.jsonPrimitive?.content ?: "",
                wins = obj["wins"]?.jsonPrimitive?.int ?: 0,
                losses = obj["losses"]?.jsonPrimitive?.int ?: 0,
                draws = obj["draws"]?.jsonPrimitive?.int ?: 0,
                friends = emptyList()
            )
        } catch (e: Exception) { null }
    }

    suspend fun getMatchHistory(username: String): List<MatchRecordUI> {
        println("📱 ApiClient.getMatchHistory() called for username: $username")
        println("📱 Global Auth token: ${GlobalAuth.token?.take(10)}...")
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/matchhistory/$username") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val body = response.bodyAsText()
            println("📱 API Response: $body")
            val jsonArray = Json.parseToJsonElement(body).jsonArray
            val records = jsonArray.map { element ->
                val obj = element.jsonObject
                println("📱 Parsing obj: ${obj}")
                println("📱 obj[\"gameSessionId\"]: ${obj["gameSessionId"]}")
                val record = MatchRecordUI(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    gameType = obj["gameType"]?.jsonPrimitive?.content ?: "",
                    players = obj["players"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    winner = obj["winner"]?.jsonPrimitive?.contentOrNull,
                    draw = obj["draw"]?.jsonPrimitive?.boolean ?: false,
                    timestamp = obj["timestamp"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                    gameSessionId = obj["gameSessionId"]?.jsonPrimitive?.contentOrNull
                )
                println("📱 Parsed record: $record")
                record
            }
            println("📱 Parsed ${records.size} records!")
            records
        } catch (e: Exception) {
            println("📱 ERROR in getMatchHistory(): ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPendingRequests(): JsonObject {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/friends/pending") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"pending":[]}""").jsonObject
        }
    }

    suspend fun rejectFriendRequest(requestId: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/friends/reject") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"requestId":"$requestId"}""")
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed"}""").jsonObject
        }
    }

    suspend fun acceptFriendRequest(requestId: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/friends/accept") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"requestId":"$requestId"}""")
            }
            Json.parseToJsonElement(response.bodyAsText()).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed"}""").jsonObject
        }
    }

    suspend fun guestLogin(deviceId: String): JsonObject {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/auth/guest") {
                contentType(ContentType.Application.Json)
                setBody("""{"deviceId":"$deviceId"}""")
            }
            val body = response.bodyAsText()
            println("🌐 Guest login with deviceId: $body")
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            Json.parseToJsonElement("""{"success":false,"message":"Connection failed: ${e.message}"}""").jsonObject
        }
    }

    data class MatchRecordUI(
        val id: String,
        val gameType: String,
        val players: List<String>,
        val winner: String?,
        val draw: Boolean,
        val timestamp: Long,
        val gameSessionId: String? = null
    )
}
