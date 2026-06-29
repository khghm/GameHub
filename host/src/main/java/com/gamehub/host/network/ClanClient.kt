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

class ClanClient {
    private val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
    private val baseUrl = "http://$serverIp:8080"
    private val client = HttpClient(OkHttp)

    suspend fun createClan(name: String, tag: String): ClanOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/clans/create") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"name":"$name","tag":"$tag"}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            ClanOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinClan(clanId: String): ClanOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/clans/$clanId/join") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            ClanOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun leaveClan(clanId: String): ClanOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/clans/$clanId/leave") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            ClanOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun upgradeClan(clanId: String): ClanOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/clans/$clanId/upgrade") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            ClanOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun contributeCoins(clanId: String, amount: Long): ClanOperationResult? {
        return try {
            val response: HttpResponse = client.post("$baseUrl/api/clans/$clanId/contribute") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
                contentType(ContentType.Application.Json)
                setBody("""{"amount":$amount}""")
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            ClanOperationResult.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getClanInfo(clanId: String): Clan? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/clans/$clanId") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            Clan.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMyClan(): Clan? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/user/clan") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val body = response.bodyAsText()
            if (body.contains("\"clan\":null")) {
                null
            } else {
                val json = Json.parseToJsonElement(body).jsonObject
                Clan.fromJson(json)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getClanMembers(clanId: String): List<ClanMember> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/clans/$clanId/members") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { ClanMember.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllClans(): List<Clan> {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/clans") {
                GlobalAuth.token?.let { header("Authorization", "Bearer $it") }
            }
            val jsonArray = Json.parseToJsonElement(response.bodyAsText()).jsonArray
            jsonArray.mapNotNull { Clan.fromJson(it.jsonObject) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
