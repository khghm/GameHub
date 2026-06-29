// server/src/main/kotlin/com/gamehub/server/repository/UserRepository.kt
package com.gamehub.server.repository

import com.gamehub.server.domain.User
import com.gamehub.server.domain.UsersTable
import com.gamehub.server.domain.UsersTable.softCurrency
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID


@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String?,
    val softCurrency: Long,
    @Contextual val createdAt: Instant
)

class UserRepository {
    fun findById(id: UUID?): User? {
        if (id == null) return null
        return transaction {
            UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
        }
    }

    fun findByUsername(username: String): User? = transaction {
        UsersTable.selectAll().where { UsersTable.username eq username }.singleOrNull()?.toUser()
    }

    fun create(user: User): User = transaction {
        UsersTable.insert {
            it[id] = user.id ?: UUID.randomUUID()
            it[username] = user.username
            it[passwordHash] = user.passwordHash
            it[displayName] = user.displayName
            it[avatarUrl] = user.avatarUrl
            it[softCurrency] = user.softCurrency
            it[hardCurrency] = user.hardCurrency
            it[walletVersion] = user.walletVersion
            it[xp] = user.xp
            it[userLevel] = user.userLevel
            it[settings] = user.settings
            it[createdAt] = user.createdAt
            it[lastLogin] = user.lastLogin
            it[isGuest] = user.isGuest
            it[deviceIdHash] = user.deviceIdHash
            it[ipHash] = user.ipHash
            it[isMigrated] = user.isMigrated
            it[isActive] = user.isActive
        }.resultedValues?.single()?.toUser() ?: throw IllegalStateException("ایجاد کاربر ناموفق بود")
    }

    fun update(id: UUID, block: (UpdateBuilder<Int>) -> Unit) = transaction {
        UsersTable.update({ UsersTable.id eq id }) { block(it) }
    }

    suspend fun getAllUsers(offset: Int, limit: Int, search: String?): List<UserDto> = dbQuery {
        var query = UsersTable.selectAll()
        if (!search.isNullOrBlank()) {
            query = query.where { UsersTable.username like "%$search%" }
        }
        query
            .orderBy(UsersTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                UserDto(
                    id = row[UsersTable.id].toString(),
                    username = row[UsersTable.username],
                    displayName = row[UsersTable.displayName],
                    softCurrency = row[UsersTable.softCurrency],
                    createdAt = row[UsersTable.createdAt].toInstant()
                )
            }
    }

    suspend fun getTotalUsersCount(search: String?): Int = dbQuery {
        var query = UsersTable.selectAll()
        if (!search.isNullOrBlank()) {
            query = query.where { UsersTable.username like "%$search%" }
        }
        query.count().toInt()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        username = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        displayName = this[UsersTable.displayName],
        avatarUrl = this[UsersTable.avatarUrl],
        softCurrency = this[UsersTable.softCurrency],
        hardCurrency = this[UsersTable.hardCurrency],
        walletVersion = this[UsersTable.walletVersion],
        xp = this[UsersTable.xp],
        userLevel = this[UsersTable.userLevel],
        settings = this[UsersTable.settings],
        createdAt = this[UsersTable.createdAt],
        lastLogin = this[UsersTable.lastLogin],
        isGuest = this[UsersTable.isGuest],
        deviceIdHash = this[UsersTable.deviceIdHash],
        ipHash = this[UsersTable.ipHash],
        isMigrated = this[UsersTable.isMigrated],
        isActive = this[UsersTable.isActive],
    )

    suspend fun saveBackupCode(guestId: String, bcryptHash: String) = dbQuery {
        // TODO: پیاده‌سازی در فاز بعدی
    }

    suspend fun findGuestIdByBackupCode(bcryptHash: String): String? = dbQuery {
        // TODO: پیاده‌سازی در فاز بعدی
        null
    }

    suspend fun updateGuestDevice(guestId: String, deviceIdHash: String, ipHash: String, fingerprint: String) = dbQuery {
        // TODO: پیاده‌سازی در فاز بعدی
    }

    suspend fun updateBalance(userId: String, newBalance: Long, newVersion: Long) = dbQuery {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[softCurrency] = newBalance
            it[walletVersion] = newVersion
        }
    }

    suspend fun getUsersRegisteredAfter(after: OffsetDateTime): Int = dbQuery {
        UsersTable.select { UsersTable.createdAt greaterEq after }.count().toInt()
    }

    suspend fun getTotalSoftCurrency(): Long = dbQuery {
        UsersTable.selectAll().sumOf { it[softCurrency] }
    }

    suspend fun getFilteredUsers(
        search: String?,
        status: String?,
        fromDate: OffsetDateTime?,
        toDate: OffsetDateTime?,
        offset: Int,
        limit: Int
    ): List<UserDto> = dbQuery {
        var query = UsersTable.selectAll()
        if (!search.isNullOrBlank()) {
            query = query.where { UsersTable.username like "%$search%" }
        }
        if (status != null) {
            val isActive = when (status) {
                "active" -> true
                "banned" -> false
                else -> null
            }
            if (isActive != null) {
                query = query.where { UsersTable.isActive eq isActive }
            }
        }
        fromDate?.let {
            query = query.where { UsersTable.createdAt greaterEq it }
        }
        toDate?.let {
            query = query.where { UsersTable.createdAt lessEq it }
        }
        query
            .orderBy(UsersTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { row ->
                UserDto(
                    id = row[UsersTable.id].toString(),
                    username = row[UsersTable.username],
                    displayName = row[UsersTable.displayName],
                    softCurrency = row[UsersTable.softCurrency],
                    createdAt = row[UsersTable.createdAt].toInstant()
                )
            }
    }

    suspend fun getFilteredUsersCount(
        search: String?,
        status: String?,
        fromDate: OffsetDateTime?,
        toDate: OffsetDateTime?
    ): Int = dbQuery {
        var query = UsersTable.selectAll()
        if (!search.isNullOrBlank()) {
            query = query.where { UsersTable.username like "%$search%" }
        }
        if (status != null) {
            val isActive = when (status) {
                "active" -> true
                "banned" -> false
                else -> null
            }
            if (isActive != null) {
                query = query.where { UsersTable.isActive eq isActive }
            }
        }
        fromDate?.let {
            query = query.where { UsersTable.createdAt greaterEq it }
        }
        toDate?.let {
            query = query.where { UsersTable.createdAt lessEq it }
        }
        query.count().toInt()
    }

    suspend fun getTopUsersByCoins(percent: Int): List<User> = dbQuery {
        UsersTable.selectAll()
            .orderBy(UsersTable.softCurrency to SortOrder.DESC)
            .limit((UsersTable.selectAll().count() * percent / 100).toInt())
            .map { it.toUser() }
    }

}