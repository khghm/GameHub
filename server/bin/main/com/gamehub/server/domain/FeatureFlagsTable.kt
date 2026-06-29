package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

object FeatureFlagsTable : Table("feature_flags") {
    val id = uuid("id").default(UUID.randomUUID())
    val flagKey = varchar("flag_key", 100).uniqueIndex() // Unique identifier for the flag, e.g., "new_game_mode"
    val name = varchar("name", 100) // Human-readable name
    val description = text("description").nullable()
    val isEnabled = bool("is_enabled").default(false)
    val environment = varchar("environment", 50).nullable() // e.g., "prod", "staging"
    val createdAt = timestampWithTimeZone("created_at").default(Instant.now().atOffset(ZoneOffset.UTC))
    val updatedAt = timestampWithTimeZone("updated_at").default(Instant.now().atOffset(ZoneOffset.UTC))

    override val primaryKey = PrimaryKey(id)
}
