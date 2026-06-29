package com.gamehub.server.admin

import com.gamehub.server.cache.PresenceCache
import com.gamehub.server.matchmaking.MatchmakingService
import com.gamehub.server.modules.GameSessionManager
import kotlinx.serialization.Serializable

@Serializable
data class MetricsSnapshot(
    val timestamp: Long,
    val onlineHubUsers: Int,
    val inGameUsers: Int,
    val activeGames: Int,
    val queueSizes: Map<String, Int>,
    val systemLoad: Double? = null,
    val memoryUsedMB: Long? = null
)

class MetricsService(
    private val matchmakingService: MatchmakingService? = null
) {
    suspend fun getCurrentMetrics(): MetricsSnapshot {
        val onlineHub = PresenceCache.getOnlineHubCount()
        val inGame = PresenceCache.getInGameCount()
        val activeGames = GameSessionManager.getActiveGamesCount()

        val queueSizes = mutableMapOf<String, Int>()
        val games = listOf("tictactoe", "connectfour", "uno", "ludo", "monopoly")
        val modes = listOf("casual", "ranked")

        for (game in games) {
            for (mode in modes) {
                // اصلاح: استفاده از queueSize به جای getQueueSize
                val size = matchmakingService?.queueSize(game, mode) ?: 0
                if (size > 0) {
                    queueSizes["$game:$mode"] = size
                }
            }
        }

        val runtime = Runtime.getRuntime()
        val memoryUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val systemLoad = try {
            val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean()
            osBean.systemLoadAverage
        } catch (e: Exception) { null }

        return MetricsSnapshot(
            timestamp = System.currentTimeMillis(),
            onlineHubUsers = onlineHub,
            inGameUsers = inGame,
            activeGames = activeGames,
            queueSizes = queueSizes,
            systemLoad = systemLoad,
            memoryUsedMB = memoryUsedMB
        )
    }
}