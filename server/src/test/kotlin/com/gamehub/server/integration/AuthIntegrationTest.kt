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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    private lateinit var client: HttpClient

    @BeforeAll
    fun setup() {
//        TestApplication.start() // راه‌اندازی سرور تست (در صورت نیاز واقعی)
        // برای تست بدون سرور واقعی، از MockEngine استفاده می‌کنیم
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            val body = """{"success":true,"token":"fake-jwt-token","guestId":"guest-123","user":{"id":"guest-123","username":"Guest_ABC","displayName":"مهمان","isGuest":true}}"""
                            respond(content = body, status = HttpStatusCode.OK)
                        }
                        "/api/login" -> {
                            respond(content = """{"success":false,"message":"Not implemented"}""", status = HttpStatusCode.Unauthorized)
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }
    }

    @Test
    fun `guest login should return token and guestId`() = runBlocking {
        val response = client.post("/api/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"test-device"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
        assertNotNull(json["token"]?.jsonPrimitive?.content)
        assertNotNull(json["guestId"]?.jsonPrimitive?.content)
    }
}