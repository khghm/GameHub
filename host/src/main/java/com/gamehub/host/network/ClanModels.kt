package com.gamehub.host.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

data class Clan(
    val id: String,
    val name: String,
    val tag: String,
    val ownerId: String,
    val level: Int = 1,
    val memberCount: Int = 1,
    val maxMembers: Int = 50,
    val coinsRequiredForNextLevel: Long = 0,
    val totalCoinsContributed: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(json: JsonObject): Clan {
            return Clan(
                id = json["id"]?.jsonPrimitive?.content ?: "",
                name = json["name"]?.jsonPrimitive?.content ?: "",
                tag = json["tag"]?.jsonPrimitive?.content ?: "",
                ownerId = json["ownerId"]?.jsonPrimitive?.content ?: "",
                level = json["level"]?.jsonPrimitive?.int ?: 1,
                memberCount = json["memberCount"]?.jsonPrimitive?.int ?: 1,
                maxMembers = json["maxMembers"]?.jsonPrimitive?.int ?: 50,
                coinsRequiredForNextLevel = json["coinsRequiredForNextLevel"]?.jsonPrimitive?.long ?: 0,
                totalCoinsContributed = json["totalCoinsContributed"]?.jsonPrimitive?.long ?: 0,
                createdAt = json["createdAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                updatedAt = json["updatedAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis()
            )
        }
    }
}

data class ClanMember(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val coinsContributed: Long
) {
    companion object {
        fun fromJson(json: JsonObject): ClanMember {
            return ClanMember(
                userId = json["userId"]?.jsonPrimitive?.content ?: "",
                role = json["role"]?.jsonPrimitive?.content ?: "",
                joinedAt = json["joinedAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                coinsContributed = json["coinsContributed"]?.jsonPrimitive?.long ?: 0
            )
        }
    }
}

data class ClanOperationResult(
    val success: Boolean,
    val message: String,
    val clan: Clan? = null
) {
    companion object {
        fun fromJson(json: JsonObject): ClanOperationResult {
            return ClanOperationResult(
                success = json["success"]?.jsonPrimitive?.boolean ?: false,
                message = json["message"]?.jsonPrimitive?.content ?: "",
                clan = json["clan"]?.jsonObject?.let { Clan.fromJson(it) }
            )
        }
    }
}
