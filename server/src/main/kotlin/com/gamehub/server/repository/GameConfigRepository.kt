package com.gamehub.server.repository

import com.gamehub.server.domain.GameConfigTable
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.game.GameConfig
import com.gamehub.shared.game.GameParameters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.OffsetDateTime

class GameConfigRepository {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getActiveConfig(gameId: String, mode: String): GameConfig? = dbQuery {
        GameConfigTable.select {
            (GameConfigTable.gameId eq gameId) and
                    (GameConfigTable.mode eq mode) and
                    (GameConfigTable.isActive eq true)
        }.singleOrNull()?.let { rowToConfig(it) }
    }

    suspend fun getAllConfigs(gameId: String? = null): List<GameConfig> = dbQuery {
        var query = GameConfigTable.selectAll()
        if (gameId != null) {
            query = query.where { GameConfigTable.gameId eq gameId }
        }
        query.orderBy(GameConfigTable.gameId to SortOrder.ASC, GameConfigTable.mode to SortOrder.ASC)
            .map { rowToConfig(it) }
    }

    suspend fun createConfig(
        gameId: String,
        mode: String,
        config: GameParameters,
        createdBy: String?
    ): GameConfig = dbQuery {
        val configJson = json.encodeToString(config)
        val now = OffsetDateTime.now()
        val newVersion = 1
        GameConfigTable.insert {
            it[this.gameId] = gameId
            it[this.mode] = mode
            it[this.configJson] = configJson
            it[this.version] = newVersion
            it[this.isActive] = true
            it[this.createdBy] = createdBy
            it[this.createdAt] = now
            it[this.updatedAt] = now
        }.resultedValues?.single()?.let { rowToConfig(it) }
            ?: throw IllegalStateException("Failed to create config")
    }

    suspend fun updateConfig(
        id: Long,
        newConfig: GameParameters,
        newVersion: Int,
        changedBy: String?
    ): Boolean = dbQuery {
        val configJson = json.encodeToString(newConfig)
        val updatedRows = GameConfigTable.update({ GameConfigTable.id eq id }) {
            it[this.configJson] = configJson
            it[this.version] = newVersion
            it[this.updatedAt] = OffsetDateTime.now()
        }
        updatedRows > 0
    }

    suspend fun deactivateOldConfig(gameId: String, mode: String, excludeId: Long): Boolean = dbQuery {
        val updatedRows = GameConfigTable.update({
            (GameConfigTable.gameId eq gameId) and
                    (GameConfigTable.mode eq mode) and
                    (GameConfigTable.isActive eq true) and
                    (GameConfigTable.id neq excludeId)
        }) {
            it[isActive] = false
        }
        updatedRows > 0
    }

    private fun rowToConfig(row: ResultRow): GameConfig = GameConfig(
        id = row[GameConfigTable.id],
        gameId = row[GameConfigTable.gameId],
        mode = row[GameConfigTable.mode],
        config = json.decodeFromString(row[GameConfigTable.configJson]),
        version = row[GameConfigTable.version],
        isActive = row[GameConfigTable.isActive],
        createdAt = row[GameConfigTable.createdAt].toInstant().toEpochMilli(),
        updatedAt = row[GameConfigTable.updatedAt].toInstant().toEpochMilli()
    )
}