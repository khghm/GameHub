package com.gamehub.server.society

import com.gamehub.server.modules.ChatServer
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.server.repository.BehaviorRepository
import com.gamehub.server.repository.RatingRepository
import com.gamehub.server.repository.UserRepository
import com.gamehub.shared.behavior.BehaviorInfo
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.rating.RatingInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.selectAll


@Serializable
data class Society(
    val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val memberCount: Int = 0,
    val maxMembers: Int = 50000,
    val membershipType: String = "open", // open, approval, condition
    val membershipCondition: String? = null, // JSON query builder
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SocietyMember(
    val userId: String,
    val role: String, // owner, admin, member
    val joinedAt: Long,
    val status: String // active, pending
)

@Serializable
data class MembershipCondition(
    val minLevel: Int? = null,
    val minElo: Int? = null,
    val allowedBehaviorBands: List<String>? = null,
    val minGamesPlayed: Int? = null,
    val minWinRate: Double? = null,
    val gameId: String? = "tictactoe" // Allow specifying game ID for conditions
)

@Serializable
data class SocietyOperationResult(
    val success: Boolean,
    val message: String,
    val society: Society? = null
)

object SocietyTables {
    object Societies : Table("societies") {
        val id = varchar("id", 20)
        val name = varchar("name", 100)
        val description = text("description").nullable()
        val ownerId = varchar("owner_id", 255)
        val memberCount = integer("member_count").default(0)
        val maxMembers = integer("max_members").default(50000)
        val membershipType = varchar("membership_type", 20).default("open")
        val membershipCondition = text("membership_condition").nullable()
        val createdAt = timestamp("created_at").default(Instant.now())
        val updatedAt = timestamp("updated_at").default(Instant.now())
        override val primaryKey = PrimaryKey(id)
    }

    object SocietyMembers : Table("society_members") {
        val societyId = varchar("society_id", 20)
        val userId = varchar("user_id", 255)
        val role = varchar("role", 20).default("member")
        val joinedAt = timestamp("joined_at").default(Instant.now())
        val status = varchar("status", 20).default("active")
        override val primaryKey = PrimaryKey(societyId, userId)
    }
}

class SocietyService(
    private val cache: CacheProvider,
    private val userRepository: UserRepository,
    private val ratingRepository: RatingRepository,
    private val behaviorRepository: BehaviorRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ========== Creation & Management ==========
    suspend fun createSociety(
        name: String,
        description: String?,
        ownerId: String,
        maxMembers: Int = 50000,
        membershipType: String = "open",
        membershipCondition: MembershipCondition? = null
    ): SocietyOperationResult {
        if (name.isBlank() || name.length > 100) {
            return SocietyOperationResult(false, "نام انجمن باید بین 1 تا 100 کاراکتر باشد")
        }

        // Check uniqueness
        val existing = dbQuery {
            SocietyTables.Societies.selectAll().where { SocietyTables.Societies.name eq name }.singleOrNull()
        }
        if (existing != null) {
            return SocietyOperationResult(false, "نام انجمن قبلاً استفاده شده")
        }

        val id = UUID.randomUUID().toString()
        val conditionJson = membershipCondition?.let { json.encodeToString(it) }

        dbQuery {
            SocietyTables.Societies.insert {
                it[SocietyTables.Societies.id] = id
                it[SocietyTables.Societies.name] = name
                it[SocietyTables.Societies.description] = description
                it[SocietyTables.Societies.ownerId] = ownerId
                it[SocietyTables.Societies.maxMembers] = maxMembers
                it[SocietyTables.Societies.membershipType] = membershipType
                it[SocietyTables.Societies.membershipCondition] = conditionJson
            }
            SocietyTables.SocietyMembers.insert {
                it[SocietyTables.SocietyMembers.societyId] = id
                it[SocietyTables.SocietyMembers.userId] = ownerId
                it[SocietyTables.SocietyMembers.role] = "owner"
            }
        }
        val society = getSociety(id)
        return SocietyOperationResult(true, "انجمن با موفقیت ایجاد شد", society)
    }

    suspend fun getSociety(id: String): Society? {
        val row = dbQuery {
            SocietyTables.Societies.selectAll().where { SocietyTables.Societies.id eq id }.singleOrNull()
        } ?: return null
        return Society(
            id = row[SocietyTables.Societies.id],
            name = row[SocietyTables.Societies.name],
            description = row[SocietyTables.Societies.description],
            ownerId = row[SocietyTables.Societies.ownerId],
            memberCount = row[SocietyTables.Societies.memberCount],
            maxMembers = row[SocietyTables.Societies.maxMembers],
            membershipType = row[SocietyTables.Societies.membershipType],
            membershipCondition = row[SocietyTables.Societies.membershipCondition],
            createdAt = row[SocietyTables.Societies.createdAt].toEpochMilli(),
            updatedAt = row[SocietyTables.Societies.updatedAt].toEpochMilli()
        )
    }

    suspend fun getAllSocieties(): List<Society> {
        return dbQuery {
            SocietyTables.Societies.selectAll().map { row ->
                Society(
                    id = row[SocietyTables.Societies.id],
                    name = row[SocietyTables.Societies.name],
                    description = row[SocietyTables.Societies.description],
                    ownerId = row[SocietyTables.Societies.ownerId],
                    memberCount = row[SocietyTables.Societies.memberCount],
                    maxMembers = row[SocietyTables.Societies.maxMembers],
                    membershipType = row[SocietyTables.Societies.membershipType],
                    membershipCondition = row[SocietyTables.Societies.membershipCondition],
                    createdAt = row[SocietyTables.Societies.createdAt].toEpochMilli(),
                    updatedAt = row[SocietyTables.Societies.updatedAt].toEpochMilli()
                )
            }
        }
    }

    // ========== Membership ==========
    suspend fun requestJoin(userId: String, societyId: String): SocietyOperationResult {
        // Check existing membership
        val existing = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId) }
                .singleOrNull()
        }
        if (existing != null) {
            return SocietyOperationResult(false, "شما قبلاً در این انجمن هستید")
        }

        val society = getSociety(societyId) ?: return SocietyOperationResult(false, "انجمن یافت نشد")

        // Check capacity
        if (society.memberCount >= society.maxMembers) {
            return SocietyOperationResult(false, "ظرفیت انجمن تکمیل است")
        }

        // Check membership conditions if needed
        val canJoin = checkMembershipCondition(userId, society)
        if (!canJoin) {
            return SocietyOperationResult(false, "شما شرایط عضویت را ندارید")
        }

        val membershipType = society.membershipType
        val status = when (membershipType) {
            "open" -> "active"
            "approval" -> "pending"
            "condition" -> "active"
            else -> "pending"
        }

        dbQuery {
            SocietyTables.SocietyMembers.insert {
                it[SocietyTables.SocietyMembers.societyId] = societyId
                it[SocietyTables.SocietyMembers.userId] = userId
                it[SocietyTables.SocietyMembers.role] = "member"
                it[SocietyTables.SocietyMembers.status] = status
            }
            if (status == "active") {
                SocietyTables.Societies.update({ SocietyTables.Societies.id eq societyId }) {
                    it[SocietyTables.Societies.memberCount] = SocietyTables.Societies.memberCount.plus(1)
                    it[SocietyTables.Societies.updatedAt] = Instant.now()
                }
            }
        }
        val updatedSociety = getSociety(societyId)
        val message = if (status == "active") "با موفقیت به انجمن پیوستید" else "درخواست عضویت شما ارسال شد"
        return SocietyOperationResult(true, message, updatedSociety)
    }

    suspend fun approveMember(adminId: String, societyId: String, userId: String): SocietyOperationResult {
        // Check admin permission
        val adminRole = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq adminId) }
                .singleOrNull()?.get(SocietyTables.SocietyMembers.role)
        }
        if (adminRole != "owner" && adminRole != "admin") {
            return SocietyOperationResult(false, "شما مجاز به انجام این کار نیستید")
        }

        val society = getSociety(societyId) ?: return SocietyOperationResult(false, "انجمن یافت نشد")
        if (society.memberCount >= society.maxMembers) {
            return SocietyOperationResult(false, "ظرفیت انجمن تکمیل است")
        }

        val member = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId) }
                .singleOrNull()
        } ?: return SocietyOperationResult(false, "کاربر یافت نشد")

        if (member[SocietyTables.SocietyMembers.status] != "pending") {
            return SocietyOperationResult(false, "این درخواست قبلاً بررسی شده")
        }

        dbQuery {
            SocietyTables.SocietyMembers.update(
                { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId) }
            ) {
                it[SocietyTables.SocietyMembers.status] = "active"
            }
            SocietyTables.Societies.update({ SocietyTables.Societies.id eq societyId }) {
                it[SocietyTables.Societies.memberCount] = SocietyTables.Societies.memberCount + 1
                it[SocietyTables.Societies.updatedAt] = Instant.now()
            }
        }
        val updatedSociety = getSociety(societyId)
        return SocietyOperationResult(true, "عضویت کاربر تایید شد", updatedSociety)
    }

    suspend fun rejectMember(adminId: String, societyId: String, userId: String): SocietyOperationResult {
        val adminRole = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq adminId) }
                .singleOrNull()?.get(SocietyTables.SocietyMembers.role)
        }
        if (adminRole != "owner" && adminRole != "admin") {
            return SocietyOperationResult(false, "شما مجاز به انجام این کار نیستید")
        }

        dbQuery {
            SocietyTables.SocietyMembers.deleteWhere {
                (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId)
            }
        }
        return SocietyOperationResult(true, "درخواست عضویت رد شد")
    }

    suspend fun leaveSociety(userId: String, societyId: String): SocietyOperationResult {
        val member = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId) }
                .singleOrNull()
        } ?: return SocietyOperationResult(false, "شما در این انجمن عضو نیستید")

        val role = member[SocietyTables.SocietyMembers.role]
        val status = member[SocietyTables.SocietyMembers.status]

        if (role == "owner") {
            val nextOwner = dbQuery {
                SocietyTables.SocietyMembers.selectAll()
                    .where { (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.role neq "owner") }
                    .limit(1).singleOrNull()
            }
            if (nextOwner == null) {
                // Delete society completely
                dbQuery {
                    SocietyTables.SocietyMembers.deleteWhere { SocietyTables.SocietyMembers.societyId eq societyId }
                    SocietyTables.Societies.deleteWhere { SocietyTables.Societies.id eq societyId }
                }
                return SocietyOperationResult(true, "انجمن با موفقیت حذف شد")
            } else {
                // Transfer ownership
                val nextOwnerId = nextOwner[SocietyTables.SocietyMembers.userId]
                dbQuery {
                    SocietyTables.SocietyMembers.update({ (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq nextOwnerId) }) {
                        it[SocietyTables.SocietyMembers.role] = "owner"
                    }
                    SocietyTables.Societies.update({ SocietyTables.Societies.id eq societyId }) {
                        it[SocietyTables.Societies.ownerId] = nextOwnerId
                        it[SocietyTables.Societies.updatedAt] = Instant.now()
                    }
                }
            }
        }

        // Leave the society
        dbQuery {
            SocietyTables.SocietyMembers.deleteWhere {
                (SocietyTables.SocietyMembers.societyId eq societyId) and (SocietyTables.SocietyMembers.userId eq userId)
            }
            if (status == "active") {
                SocietyTables.Societies.update({ SocietyTables.Societies.id eq societyId }) {
                    it[SocietyTables.Societies.memberCount] = SocietyTables.Societies.memberCount.minus(1)
                    it[SocietyTables.Societies.updatedAt] = Instant.now()
                }
            }
        }
        return SocietyOperationResult(true, "با موفقیت از انجمن خارج شدید")
    }

    // ========== Helpers ==========
    private suspend fun checkMembershipCondition(userId: String, society: Society): Boolean {
        val conditionJson = society.membershipCondition ?: return true
        val condition = try {
            json.decodeFromString<MembershipCondition>(conditionJson)
        } catch (e: Exception) {
            return true
        }

        val gameId = condition.gameId ?: "tictactoe"
        val userRating = ratingRepository.getOrCreate(userId, gameId)
        val userBehavior = behaviorRepository.getOrCreate(userId)

        if (condition.minLevel != null && (userRating.gamesPlayed / 10) < condition.minLevel) return false
        if (condition.minElo != null && userRating.rating < condition.minElo) return false
        if (condition.minGamesPlayed != null && userRating.gamesPlayed < condition.minGamesPlayed) return false
        if (condition.minWinRate != null) {
            val winRate = if (userRating.gamesPlayed > 0) {
                userRating.wins.toDouble() / userRating.gamesPlayed.toDouble()
            } else 0.0
            if (winRate < condition.minWinRate) return false
        }
        if (condition.allowedBehaviorBands != null && condition.allowedBehaviorBands.isNotEmpty()) {
            if (!condition.allowedBehaviorBands.contains(userBehavior.band)) return false
        }
        return true
    }

    suspend fun getSocietyMembers(societyId: String): List<SocietyMember> {
        return dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { SocietyTables.SocietyMembers.societyId eq societyId }
                .map {
                    SocietyMember(
                        userId = it[SocietyTables.SocietyMembers.userId],
                        role = it[SocietyTables.SocietyMembers.role],
                        joinedAt = it[SocietyTables.SocietyMembers.joinedAt].toEpochMilli(),
                        status = it[SocietyTables.SocietyMembers.status]
                    )
                }
        }
    }

    suspend fun getUserSocieties(userId: String): List<Society> {
        val societyIds = dbQuery {
            SocietyTables.SocietyMembers.selectAll()
                .where { SocietyTables.SocietyMembers.userId eq userId }
                .map { it[SocietyTables.SocietyMembers.societyId] }
        }
        return societyIds.mapNotNull { getSociety(it) }
    }

    suspend fun getSocietyChannel(societyId: String): String = "society:$societyId"

    suspend fun subscribeUserToSocietyChat(userId: String, societyId: String) {
        val channel = getSocietyChannel(societyId)
        ChatServer.subscribeToChannel(userId, channel)
    }

    suspend fun unsubscribeUserFromSocietyChat(userId: String, societyId: String) {
        val channel = getSocietyChannel(societyId)
        ChatServer.unsubscribeFromChannel(userId, channel)
    }
}
