package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object PermissionsTable : Table("permissions") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 100)
    val resource = varchar("resource", 50)
    val action = varchar("action", 50)
    val description = varchar("description", 255).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}