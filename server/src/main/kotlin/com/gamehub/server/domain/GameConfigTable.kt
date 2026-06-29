package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object GameConfigTable : Table("game_configs") {
    val id = long("id").autoIncrement()
    val gameId = varchar("game_id", 50)
    val mode = varchar("mode", 20)
    val configJson = text("config_json")
    val version = integer("version").default(1)
    val isActive = bool("is_active").default(true)
    val createdBy = varchar("created_by", 255).nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
    override val primaryKey = PrimaryKey(id)
}