package com.gamehub.server.economy

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object EconomyAutoSinkLogTable : Table("economy_auto_sink_log") {
    val id = long("id").autoIncrement()
    val executionTimeCol = timestamp("execution_time")
    val inflationRateBefore = double("inflation_rate_before")
    val totalSupplyBefore = long("total_supply_before")
    val affectedUsers = integer("affected_users")
    val totalCoinsRemoved = long("total_coins_removed")
    val statusCol = varchar("status", 20)
    val errorMessage = text("error_message").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())
    override val primaryKey = PrimaryKey(id)
}