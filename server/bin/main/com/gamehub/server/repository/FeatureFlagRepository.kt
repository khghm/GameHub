package com.gamehub.server.repository

import com.gamehub.server.domain.FeatureFlagsTable
import com.gamehub.server.featureflags.FeatureFlag
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class FeatureFlagRepository {

    suspend fun getAll(environment: String? = null): List<FeatureFlag> = dbQuery {
        var query = FeatureFlagsTable.selectAll()
        environment?.let {
            query = query.where { FeatureFlagsTable.environment eq it or (FeatureFlagsTable.environment.isNull()) }
        }
        query.orderBy(FeatureFlagsTable.flagKey to SortOrder.ASC)
            .map { rowToFeatureFlag(it) }
    }

    suspend fun getById(id: UUID): FeatureFlag? = dbQuery {
        FeatureFlagsTable.select { FeatureFlagsTable.id eq id }
            .singleOrNull()?.let { rowToFeatureFlag(it) }
    }

    suspend fun getByKey(key: String): FeatureFlag? = dbQuery {
        FeatureFlagsTable.select { FeatureFlagsTable.flagKey eq key }
            .singleOrNull()?.let { rowToFeatureFlag(it) }
    }

    private fun Instant.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)
    private fun OffsetDateTime.toInstantCompat(): Instant = toInstant()

    suspend fun create(
        key: String,
        name: String,
        description: String?,
        isEnabled: Boolean,
        environment: String?
    ): FeatureFlag = dbQuery {
        val id = UUID.randomUUID()
        val now = Instant.now()
        FeatureFlagsTable.insert {
            it[this.id] = id
            it[this.flagKey] = key
            it[this.name] = name
            it[this.description] = description
            it[this.isEnabled] = isEnabled
            it[this.environment] = environment
            it[this.createdAt] = now.toOffsetDateTime()
            it[this.updatedAt] = now.toOffsetDateTime()
        }
        FeatureFlag(
            id = id,
            key = key,
            name = name,
            description = description,
            isEnabled = isEnabled,
            environment = environment,
            createdAt = now,
            updatedAt = now
        )
    }

    suspend fun update(
        id: UUID,
        name: String?,
        description: String?,
        isEnabled: Boolean?,
        environment: String?
    ): FeatureFlag? = dbQuery {
        val now = Instant.now()
        val updatedRows = FeatureFlagsTable.update({ FeatureFlagsTable.id eq id }) { updateStmt ->
            name?.let { updateStmt[this.name] = it }
            description?.let { updateStmt[this.description] = it }
            isEnabled?.let { updateStmt[this.isEnabled] = it }
            environment?.let { updateStmt[this.environment] = it }
            updateStmt[this.updatedAt] = now.toOffsetDateTime()
        }
        if (updatedRows > 0) {
            FeatureFlagsTable.select { FeatureFlagsTable.id eq id }
                .singleOrNull()?.let { rowToFeatureFlag(it) }
        } else {
            null
        }
    }

    suspend fun delete(id: UUID): Boolean = dbQuery {
        FeatureFlagsTable.deleteWhere { FeatureFlagsTable.id eq id } > 0
    }

    private fun rowToFeatureFlag(row: ResultRow): FeatureFlag {
        return FeatureFlag(
            id = row[FeatureFlagsTable.id],
            key = row[FeatureFlagsTable.flagKey],
            name = row[FeatureFlagsTable.name],
            description = row[FeatureFlagsTable.description],
            isEnabled = row[FeatureFlagsTable.isEnabled],
            environment = row[FeatureFlagsTable.environment],
            createdAt = row[FeatureFlagsTable.createdAt].toInstantCompat(),
            updatedAt = row[FeatureFlagsTable.updatedAt].toInstantCompat()
        )
    }
}
