package com.gamehub.server.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PlayerStats(
    val username: String,
    var wins: Int = 0,
    var losses: Int = 0,
    var draws: Int = 0,
    var points: Int = 0
) {
    val gamesPlayed: Int get() = wins + losses + draws
}

object LeaderboardModule {
    private val stats = ConcurrentHashMap<String, PlayerStats>()

    fun recordResult(username: String, result: String) {
        val playerStats = stats.getOrPut(username) { PlayerStats(username) }
        when (result) {
            "win" -> {
                playerStats.wins++
                playerStats.points += 3
            }
            "loss" -> {
                playerStats.losses++
            }
            "draw" -> {
                playerStats.draws++
                playerStats.points += 1
            }
        }
    }

    fun getLeaderboard(): List<PlayerStats> {
        return stats.values.toList().sortedByDescending { it.points }
    }

    fun getPlayerStats(username: String): PlayerStats? {
        return stats[username]
    }

    fun leaderboardToJson(): String {
        return Json.encodeToString(getLeaderboard())
    }

    fun statsToJson(username: String): String {
        return Json.encodeToString(getPlayerStats(username) ?: PlayerStats(username))
    }
}