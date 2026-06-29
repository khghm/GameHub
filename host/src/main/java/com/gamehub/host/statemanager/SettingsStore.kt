// host/src/main/java/com/gamehub/host/statemanager/SettingsStore.kt
package com.gamehub.host.statemanager

import com.gamehub.host.secure.SecureStorage
import com.gamehub.shared.state.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class SettingsStore(
    private val secureStorage: SecureStorage,
    private val userId: String
) : Store {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    override suspend fun start() {
        loadSettings()
    }

    override suspend fun stop() {}

    private suspend fun loadSettings() {
        val saved = secureStorage.load("settings_$userId")
        if (saved != null) {
            _settings.value = Json.decodeFromString(saved)
        } else {
            _settings.value = UserSettings()
        }
    }

    suspend fun updateSettings(update: UserSettings.() -> UserSettings) {
        val current = _settings.value ?: UserSettings()
        val newSettings = update(current)
        _settings.value = newSettings
        secureStorage.save("settings_$userId", Json.encodeToString(newSettings))
    }
}