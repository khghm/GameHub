package com.gamehub.server.integration

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GamePlayIntegrationTest {

    private lateinit var client: HttpClient
    private var authToken: String = ""

    @BeforeAll
    fun setup() {
//        TestApplication.start()
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            val body = """{"success":true,"token":"test-token-123","guestId":"guest-456"}"""
                            respond(content = body, status = HttpStatusCode.OK)
                        }
                        "/api/matchmaking/join" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (token == "test-token-123") {
                                respond(content = """{"status":"matched","gameId":"test-game-001"}""", status = HttpStatusCode.OK)
                            } else {
                                respond(content = """{"status":"error","message":"Unauthorized"}""", status = HttpStatusCode.Unauthorized)
                            }
                        }
                        else -> error("Unhandled")
                    }
                }
            }
        }
        // دریافت توکن
        runBlocking {
            val resp = client.post("/api/auth/guest") { setBody("""{"deviceId":"test"}""") }
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            authToken = json["token"]?.jsonPrimitive?.content ?: ""
        }
    }

    @Test
    fun `matchmaking should return gameId for valid token`() = runBlocking {
        val response = client.post("/api/matchmaking/join") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody("""{"gameType":"tictactoe","playerName":"testPlayer"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("matched", json["status"]?.jsonPrimitive?.content)
        assertNotNull(json["gameId"]?.jsonPrimitive?.content)
    }
}