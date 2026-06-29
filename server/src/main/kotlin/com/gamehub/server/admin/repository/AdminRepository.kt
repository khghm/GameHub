package com.gamehub.server.admin.repository

import com.gamehub.server.admin.domain.AdminUser
import com.gamehub.server.admin.domain.AuditLogEntry
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object AdminUsersTable : Table("admin_users") {
    val id = uuid("ID").default(UUID.randomUUID())
    val username = varchar("USERNAME", 50)
    val passwordHash = varchar("PASSWORD_HASH", 255)
    val role = varchar("ROLE", 20)
    val twoFactorSecret = varchar("TWO_FACTOR_SECRET", 255).nullable()
    val isActive = bool("IS_ACTIVE").default(true)
    val createdAt = timestamp("CREATED_AT").default(Instant.now())
    val lastLogin = timestamp("LAST_LOGIN").nullable()
    override val primaryKey = PrimaryKey(id)
}

object AuditLogTable : Table("audit_log") {
    val id = long("ID").autoIncrement()
    val adminId = uuid("ADMIN_ID").nullable()
    val adminUsername = varchar("ADMIN_USERNAME", 50).nullable()
    val action = varchar("ACTION", 100)
    val targetUserId = text("TARGET_USER_ID").nullable()
    val targetType = varchar("TARGET_TYPE", 50).nullable()
    val details = text("DETAILS").nullable()
    val ipAddress = varchar("IP_ADDRESS", 45).nullable()
    val userAgent = text("USER_AGENT").nullable()
    val createdAt = timestamp("CREATED_AT").default(Instant.now())
    override val primaryKey = PrimaryKey(id)
}

class AdminRepository {
    suspend fun findByUsername(username: String): AdminUser? = dbQuery {
        AdminUsersTable.select { AdminUsersTable.username eq username }
            .singleOrNull()?.let {
                AdminUser(
                    id = it[AdminUsersTable.id],
                    username = it[AdminUsersTable.username],
                    passwordHash = it[AdminUsersTable.passwordHash],
                    role = it[AdminUsersTable.role],
                    permissions = emptyList(),     // برای سازگاری، لیست خالی
                    twoFactorSecret = it[AdminUsersTable.twoFactorSecret],
                    isActive = it[AdminUsersTable.isActive],
                    createdAt = it[AdminUsersTable.createdAt],
                    lastLogin = it[AdminUsersTable.lastLogin]
                )
            }
    }

    suspend fun createAdmin(username: String, passwordHash: String, role: String): AdminUser = dbQuery {
        val id = UUID.randomUUID()
        AdminUsersTable.insert {
            it[this.id] = id
            it[this.username] = username
            it[this.passwordHash] = passwordHash
            it[this.role] = role
            it[createdAt] = Instant.now()
        }
        AdminUser(id, username, passwordHash, role, emptyList(), null, true, Instant.now(), null)
    }

    suspend fun updateLastLogin(id: UUID, time: Instant): Unit = dbQuery {
        AdminUsersTable.update({ AdminUsersTable.id eq id }) {
            it[lastLogin] = time
        }
    }

    suspend fun log(entry: AuditLogEntry): Unit = dbQuery {
        AuditLogTable.insert {
            it[adminId] = entry.adminId
            it[adminUsername] = entry.adminUsername
            it[action] = entry.action
            it[targetUserId] = entry.targetUserId
            it[targetType] = entry.targetType
            it[details] = entry.details
            it[ipAddress] = entry.ipAddress
            it[userAgent] = entry.userAgent
            it[createdAt] = entry.createdAt
        }
    }

    suspend fun getAuditLog(limit: Int = 100, offset: Int = 0): List<AuditLogEntry> = dbQuery {
        AuditLogTable.selectAll()
            .orderBy(AuditLogTable.createdAt to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map {
                AuditLogEntry(
                    id = it[AuditLogTable.id],
                    adminId = it[AuditLogTable.adminId],
                    adminUsername = it[AuditLogTable.adminUsername],
                    action = it[AuditLogTable.action],
                    targetUserId = it[AuditLogTable.targetUserId],
                    targetType = it[AuditLogTable.targetType],
                    details = it[AuditLogTable.details],
                    ipAddress = it[AuditLogTable.ipAddress],
                    userAgent = it[AuditLogTable.userAgent],
                    createdAt = it[AuditLogTable.createdAt]
                )
            }
    }
}