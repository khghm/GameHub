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
import kotlinx.serialization.json.long

class SocietyClient {
    private val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
    private val baseUrl = "http://$serverIp:8080"
    private val client = HttpClient(OkHttp)

    suspend fun createSociety(name: String, description: String?): SocietyOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/societies/create") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                val descJson = description?.let { """"$it"""" } ?: "null"
                setBody("""{"name":"$name","description":$descJson}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            SocietyOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllSocieties(): List<Society> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/societies") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { Society.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSociety(id: String): Society? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/societies/$id") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            Society.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinSociety(id: String): SocietyOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/societies/$id/join") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            SocietyOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun leaveSociety(id: String): SocietyOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/societies/$id/leave") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            SocietyOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSocietyMembers(id: String): List<SocietyMember> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/societies/$id/members") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { SocietyMember.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun approveMember(id: String, userId: String): SocietyOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/societies/$id/approve") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"userId":"$userId"}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            SocietyOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun rejectMember(id: String, userId: String): SocietyOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/societies/$id/reject") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"userId":"$userId"}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            SocietyOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserSocieties(): List<Society> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/user/societies") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { Society.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
