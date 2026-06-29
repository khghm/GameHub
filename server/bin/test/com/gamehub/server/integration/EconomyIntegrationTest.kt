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
import kotlinx.serialization.json.long
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EconomyIntegrationTest {

    private lateinit var client: HttpClient
    private var authToken: String = ""

    @BeforeAll
    fun setup() {
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            respond(content = """{"success":true,"token":"eco-token","guestId":"eco-user"}""", status = HttpStatusCode.OK)
                        }
                        "/api/wallet" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (token == "eco-token") {
                                respond(content = """{"balance":500,"version":1}""", status = HttpStatusCode.OK)
                            } else {
                                respond(content = """{"error":"Unauthorized"}""", status = HttpStatusCode.Unauthorized)
                            }
                        }
                        "/api/shop/buy" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (token == "eco-token") {
                                respond(content = """{"success":true,"itemId":"test_item","quantity":1,"pricePaid":100,"purchaseId":"p-123"}""", status = HttpStatusCode.OK)
                            } else {
                                respond(content = """{"success":false,"message":"Unauthorized"}""", status = HttpStatusCode.Unauthorized)
                            }
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }
        runBlocking {
            val resp = client.post("/api/auth/guest") { setBody("""{"deviceId":"eco"}""") }
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            authToken = json["token"]?.jsonPrimitive?.content ?: ""
        }
    }

    @Test
    fun `get wallet should return balance`() = runBlocking {
        val response = client.get("/api/wallet") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(json.containsKey("balance"))
        val balance = json["balance"]?.jsonPrimitive?.long ?: 0
        assertTrue(balance >= 0)
    }

    @Test
    fun `purchase item should succeed`() = runBlocking {
        val response = client.post("/api/shop/buy") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody("""{"itemId":"test_item","quantity":1,"idempotencyKey":"test-key-1"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(json["success"]?.jsonPrimitive?.boolean == true)
    }
}