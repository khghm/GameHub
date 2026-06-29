// server/src/main/kotlin/com/gamehub/server/persistence/DatabaseFactory.kt
package com.gamehub.server.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

data class DatabaseConfig(
    val driver: String,
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)

object DatabaseFactory {
    internal var db: Database? = null

    fun init(config: DatabaseConfig) {
        val hikari = HikariDataSource(HikariConfig().apply {
            driverClassName = config.driver
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
        })
        db = Database.connect(hikari)
    }

    suspend fun <T> dbQuery(block: () -> T): T {
        return transaction(db!!) {
            block()
        }
    }
}