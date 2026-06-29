package com.gamehub.host.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

val JsonPrimitive.contentOrNull: String?
    get() = if (isString) content else null

data class Society(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val memberCount: Int = 0,
    val maxMembers: Int = 50000,
    val membershipType: String = "open",
    val membershipCondition: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(json: JsonObject): Society {
            return Society(
                id = json["id"]?.jsonPrimitive?.content ?: "",
                name = json["name"]?.jsonPrimitive?.content ?: "",
                description = json["description"]?.jsonPrimitive?.contentOrNull,
                ownerId = json["ownerId"]?.jsonPrimitive?.content ?: "",
                memberCount = json["memberCount"]?.jsonPrimitive?.int ?: 0,
                maxMembers = json["maxMembers"]?.jsonPrimitive?.int ?: 50000,
                membershipType = json["membershipType"]?.jsonPrimitive?.content ?: "open",
                membershipCondition = json["membershipCondition"]?.jsonPrimitive?.contentOrNull,
                createdAt = json["createdAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                updatedAt = json["updatedAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis()
            )
        }
    }
}

data class SocietyMember(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val status: String
) {
    companion object {
        fun fromJson(json: JsonObject): SocietyMember {
            return SocietyMember(
                userId = json["userId"]?.jsonPrimitive?.content ?: "",
                role = json["role"]?.jsonPrimitive?.content ?: "",
                joinedAt = json["joinedAt"]?.jsonPrimitive?.long ?: System.currentTimeMillis(),
                status = json["status"]?.jsonPrimitive?.content ?: ""
            )
        }
    }
}

data class SocietyOperationResult(
    val success: Boolean,
    val message: String,
    val society: Society? = null
) {
    companion object {
        fun fromJson(json: JsonObject): SocietyOperationResult {
            return SocietyOperationResult(
                success = json["success"]?.jsonPrimitive?.boolean ?: false,
                message = json["message"]?.jsonPrimitive?.content ?: "",
                society = json["society"]?.jsonObject?.let { Society.fromJson(it) }
            )
        }
    }
}
