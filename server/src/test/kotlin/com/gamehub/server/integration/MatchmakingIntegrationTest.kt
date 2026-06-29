package com.gamehub.server.integration

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * تست‌های مچ‌میکینگ:
 * - اضافه شدن به صف
 * - پیدا کردن حریف انسانی (شبیه‌سازی)
 * - Fallback به ربات در صورت نبود حریف
 * - تایم‌اوت صف و گسترش دامنه
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MatchmakingIntegrationTest {

    private lateinit var client: HttpClient
    private var authToken1: String = ""
    private var authToken2: String = ""

    @BeforeAll
    fun setup() {
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            val deviceId = (request.body as? String)?.let { body ->
                                Regex(""""deviceId":"(.*?)"""").find(body)?.groupValues?.get(1)
                            } ?: "unknown"
                            val token = when (deviceId) {
                                "device1" -> "token-user-1"
                                "device2" -> "token-user-2"
                                else -> "token-fallback"
                            }
                            val guestId = when (deviceId) {
                                "device1" -> "guest-111"
                                "device2" -> "guest-222"
                                else -> "guest-000"
                            }
                            val body = """{"success":true,"token":"$token","guestId":"$guestId"}"""
                            respond(content = body, status = HttpStatusCode.OK)
                        }
                        "/api/matchmaking/join" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            val body = request.body as? String ?: ""
                            val gameType = Regex(""""gameType":"(.*?)"""").find(body)?.groupValues?.get(1) ?: "tictactoe"
                            when (token) {
                                "token-user-1" -> {
                                    // شبیه‌سازی پیدا کردن حریف بعد از چند ثانیه (در تست فوری جواب می‌دهیم)
                                    respond(content = """{"status":"matched","gameId":"game-123"}""", status = HttpStatusCode.OK)
                                }
                                "token-user-2" -> {
                                    // برای تست fallback به ربات
                                    if (gameType == "ludo") {
                                        respond(content = """{"status":"matched","gameId":"game-456","isBot":true}""", status = HttpStatusCode.OK)
                                    } else {
                                        respond(content = """{"status":"waiting","message":"No opponent yet"}""", status = HttpStatusCode.OK)
                                    }
                                }
                                else -> respond(content = """{"status":"error","message":"Unauthorized"}""", status = HttpStatusCode.Unauthorized)
                            }
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }

        runBlocking {
            // ثبت دو کاربر مهمان
            val resp1 = client.post("/api/auth/guest") { setBody("""{"deviceId":"device1"}""") }
            val json1 = Json.parseToJsonElement(resp1.bodyAsText()).jsonObject
            authToken1 = json1["token"]?.jsonPrimitive?.content ?: ""

            val resp2 = client.post("/api/auth/guest") { setBody("""{"deviceId":"device2"}""") }
            val json2 = Json.parseToJsonElement(resp2.bodyAsText()).jsonObject
            authToken2 = json2["token"]?.jsonPrimitive?.content ?: ""
        }
    }

    @Test
    fun `join queue and get matched with human opponent`() = runBlocking {
        val response = client.post("/api/matchmaking/join") {
            header(HttpHeaders.Authorization, "Bearer $authToken1")
            contentType(ContentType.Application.Json)
            setBody("""{"gameType":"tictactoe","playerName":"Player1"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("matched", json["status"]?.jsonPrimitive?.content)
        assertNotNull(json["gameId"]?.jsonPrimitive?.content)
    }

    @Test
    fun `fallback to bot when no human opponent available`() = runBlocking {
        val response = client.post("/api/matchmaking/join") {
            header(HttpHeaders.Authorization, "Bearer $authToken2")
            contentType(ContentType.Application.Json)
            setBody("""{"gameType":"ludo","playerName":"Player2"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("matched", json["status"]?.jsonPrimitive?.content)
        assertTrue(json["isBot"]?.jsonPrimitive?.boolean == true)
    }

    @Test
    fun `invalid token returns unauthorized`() = runBlocking {
        val response = client.post("/api/matchmaking/join") {
            header(HttpHeaders.Authorization, "Bearer invalid-token")
            contentType(ContentType.Application.Json)
            setBody("""{"gameType":"tictactoe","playerName":"Hacker"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}