package com.gamehub.server.admin

import com.gamehub.server.repository.GameConfigRepository
import com.gamehub.shared.game.GameConfig
import com.gamehub.shared.game.GameParameters

class GameConfigService(
    private val configRepository: GameConfigRepository
) {

    suspend fun getConfig(gameId: String, mode: String): GameConfig? {
        return configRepository.getActiveConfig(gameId, mode)
    }

    suspend fun getAllConfigs(gameId: String? = null): List<GameConfig> {
        return configRepository.getAllConfigs(gameId)
    }

    suspend fun createConfig(
        gameId: String,
        mode: String,
        config: GameParameters,
        createdBy: String?
    ): GameConfig {
        // First, check if there is already an active config
        val existing = configRepository.getActiveConfig(gameId, mode)
        if (existing != null) {
            // Deactivate it before creating new one
            configRepository.deactivateOldConfig(gameId, mode, existing.id)
        }
        return configRepository.createConfig(gameId, mode, config, createdBy)
    }

    suspend fun updateConfig(
        id: Long,
        newConfig: GameParameters,
        version: Int,
        changedBy: String?
    ): Boolean {
        return configRepository.updateConfig(id, newConfig, version, changedBy)
    }

    suspend fun getConfigHistory(gameId: String, mode: String): List<GameConfig> {
        // For simplicity, we can return all configs (active and inactive) for that game/mode
        // We need a separate method in repository; for now we can use getAllConfigs and filter
        return configRepository.getAllConfigs(gameId).filter { it.mode == mode }
    }
}