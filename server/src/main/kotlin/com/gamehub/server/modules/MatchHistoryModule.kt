package com.gamehub.server.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class MatchRecord(
    val id: String,
    val gameType: String,
    val players: List<String>,
    val winner: String?,
    val draw: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val gameSessionId: String? = null
)

object MatchHistoryModule {
    private val history = mutableListOf<MatchRecord>()
    private val lock = Any()

    fun addMatch(gameType: String, players: List<String>, winner: String?, draw: Boolean, gameSessionId: String? = null) {
        println("📝 MatchHistoryModule.addMatch() CALLED with gameType=$gameType, players=$players, winner=$winner, draw=$draw, gameSessionId=$gameSessionId")
        val record = MatchRecord(
            id = java.util.UUID.randomUUID().toString().take(8),
            gameType = gameType,
            players = players,
            winner = winner,
            draw = draw,
            timestamp = System.currentTimeMillis(),
            gameSessionId = gameSessionId
        )
        println("📝 Created record: $record")
        synchronized(lock) {
            history.add(record)
            println("📝 History now has ${history.size} entries!")
        }
    }

    fun getHistoryForUser(userId: String): List<MatchRecord> {
        println("🔍 getHistoryForUser() CALLED for userId: $userId")
        println("🔍 Looking in all ${history.size} history entries")
        synchronized(lock) {
            history.forEachIndexed { index, record ->
                println("🔍 Entry $index: players=${record.players}, checking if contains '$userId'")
            }
            
            val matches = history.filter { record ->
                record.players.contains(userId)
            }.sortedByDescending { it.timestamp }
            
            println("🔍 Found ${matches.size} matches for $userId!")
            return matches
        }
    }

    fun getAllHistory(): List<MatchRecord> {
        synchronized(lock) {
            return history.sortedByDescending { it.timestamp }
        }
    }

    fun historyToJson(userId: String): String {
        return Json.encodeToString(getHistoryForUser(userId))
    }

    fun allHistoryToJson(): String {
        return Json.encodeToString(getAllHistory())
    }
}