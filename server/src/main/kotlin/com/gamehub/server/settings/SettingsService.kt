// server/src/main/kotlin/com/gamehub/server/settings/SettingsService.kt
package com.gamehub.server.settings

import com.gamehub.shared.cache.CacheProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class UserSettings(
    val language: String = "fa",
    val theme: String = "dark",
    val soundEnabled: Boolean = true,
    val musicVolume: Int = 80,
    val sfxVolume: Int = 100,
    val notificationsEnabled: Boolean = true,
    val privateProfile: Boolean = false
)

class SettingsService(private val cache: CacheProvider) {
    private val json = Json { ignoreUnknownKeys = true }
    private val ttlSeconds = 3600L // 1 hour cache

    suspend fun getUserSettings(userId: String): UserSettings {
        val cached = cache.get("settings:$userId")
        if (cached != null) {
            return json.decodeFromString(cached)
        }
        // In real DB, load from PostgreSQL. For now, return default.
        val default = UserSettings()
        cache.set("settings:$userId", json.encodeToString(default), ttlSeconds)
        return default
    }

    suspend fun updateUserSettings(userId: String, patch: JsonObject): UserSettings {
        val current = getUserSettings(userId)
        val updated = applyPatch(current, patch)
        cache.set("settings:$userId", json.encodeToString(updated), ttlSeconds)
        // TODO: save to PostgreSQL as well
        return updated
    }

    private fun applyPatch(current: UserSettings, patch: JsonObject): UserSettings {
        var result = current
        patch.forEach { (key, value) ->
            when (key) {
                "language" -> result = result.copy(language = value.jsonPrimitive.content)
                "theme" -> result = result.copy(theme = value.jsonPrimitive.content)
                "soundEnabled" -> result = result.copy(soundEnabled = value.jsonPrimitive.boolean)
                "musicVolume" -> result = result.copy(musicVolume = value.jsonPrimitive.int)
                "sfxVolume" -> result = result.copy(sfxVolume = value.jsonPrimitive.int)
                "notificationsEnabled" -> result = result.copy(notificationsEnabled = value.jsonPrimitive.boolean)
                "privateProfile" -> result = result.copy(privateProfile = value.jsonPrimitive.boolean)
            }
        }
        return result
    }
}