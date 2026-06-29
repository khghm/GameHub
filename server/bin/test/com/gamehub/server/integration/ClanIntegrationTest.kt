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
class ClanIntegrationTest {

    private lateinit var client: HttpClient
    private var authToken: String = ""

    @BeforeAll
    fun setup() {
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.encodedPath) {
                        "/api/auth/guest" -> {
                            respond(content = """{"success":true,"token":"clan-token","guestId":"clan-user"}""", status = HttpStatusCode.OK)
                        }
                        "/api/clans/create" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (token == "clan-token") {
                                respond(content = """{"id":"clan-001","name":"TestClan","tag":"TST","ownerId":"clan-user","level":1,"memberCount":1,"maxMembers":50}""", status = HttpStatusCode.OK)
                            } else {
                                respond(content = """{"error":"Unauthorized"}""", status = HttpStatusCode.Unauthorized)
                            }
                        }
                        "/api/user/clan" -> {
                            val token = request.headers["Authorization"]?.removePrefix("Bearer ")
                            if (token == "clan-token") {
                                respond(content = """{"id":"clan-001","name":"TestClan","tag":"TST","level":1,"memberCount":1}""", status = HttpStatusCode.OK)
                            } else {
                                respond(content = """{"clan":null}""", status = HttpStatusCode.OK)
                            }
                        }
                        else -> error("Unhandled ${request.url.encodedPath}")
                    }
                }
            }
        }
        runBlocking {
            val resp = client.post("/api/auth/guest") { setBody("""{"deviceId":"clan"}""") }
            val json = Json.parseToJsonElement(resp.bodyAsText()).jsonObject
            authToken = json["token"]?.jsonPrimitive?.content ?: ""
        }
    }

    @Test
    fun `create clan should return clan object`() = runBlocking {
        val response = client.post("/api/clans/create") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"TestClan","tag":"TST"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("clan-001", json["id"]?.jsonPrimitive?.content)
        assertEquals("TestClan", json["name"]?.jsonPrimitive?.content)
    }
}