package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object RolesTable : Table("roles") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 50)
    val description = varchar("description", 255).nullable()
    val isDefault = bool("is_default").default(false)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}