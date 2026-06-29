package com.gamehub.server.clan

import com.gamehub.server.economy.EconomyService
import com.gamehub.server.modules.ChatServer
import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.cache.CacheProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.selectAll

@Serializable
data class Clan(
    val id: String,
    val name: String,
    val tag: String,
    val ownerId: String,
    val level: Int = 1,
    val memberCount: Int = 1,
    val maxMembers: Int = 50,
    val coinsRequiredForNextLevel: Long = 0,
    val totalCoinsContributed: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ClanMember(
    val userId: String,
    val role: String,
    val joinedAt: Long,
    val coinsContributed: Long
)

@Serializable
data class ClanLevelInfo(
    val level: Int,
    val maxMembers: Int,
    val upgradeCost: Long,
    val features: String
)

@Serializable
data class ClanOperationResult(
    val success: Boolean,
    val message: String,
    val clan: Clan? = null
)

object ClanTables {
    object Clans : Table("clans") {
        val id = varchar("id", 20)
        val name = varchar("name", 50)
        val tag = varchar("tag", 10)
        val ownerId = varchar("owner_id", 255)
        val level = integer("level").default(1)
        val memberCount = integer("member_count").default(1)
        val maxMembers = integer("max_members").default(50)
        val coinsRequiredForNextLevel = long("coins_required_for_next_level").default(0)
        val totalCoinsContributed = long("total_coins_contributed").default(0)
        val createdAt = timestamp("created_at").default(Instant.now())
        val updatedAt = timestamp("updated_at").default(Instant.now())
        override val primaryKey = PrimaryKey(id)
    }

    object ClanMembers : Table("clan_members") {
        val clanId = varchar("clan_id", 20)
        val userId = varchar("user_id", 255)
        val role = varchar("role", 20).default("member")
        val joinedAt = timestamp("joined_at").default(Instant.now())
        val coinsContributed = long("coins_contributed").default(0)
        override val primaryKey = PrimaryKey(clanId, userId)
    }

    object ClanLevels : Table("clan_levels") {
        val level = integer("level")
        val maxMembers = integer("max_members")
        val upgradeCost = long("upgrade_cost_coins")
        val features = text("features")
        override val primaryKey = PrimaryKey(level)
    }
}

class ClanService(
    private val cache: CacheProvider,
    private val economyService: EconomyService
) {
    private val json = Json { ignoreUnknownKeys = true }

    // ========== Clan Creation ==========
    suspend fun createClan(userId: String, name: String, tag: String): ClanOperationResult {
        // Validate inputs
        if (name.isBlank() || name.length > 50) {
            return ClanOperationResult(false, "نام کلن باید بین 1 تا 50 کاراکتر باشد")
        }
        if (tag.isBlank() || tag.length > 10) {
            return ClanOperationResult(false, "تگ کلن باید بین 1 تا 10 کاراکتر باشد")
        }

        // Check uniqueness
        val existing = dbQuery {
            ClanTables.Clans.selectAll()
                .where { (ClanTables.Clans.name eq name) or (ClanTables.Clans.tag eq tag) }
                .singleOrNull()
        }
        if (existing != null) {
            return ClanOperationResult(false, "نام یا تگ کلن قبلاً استفاده شده")
        }

        // Check if user already in a clan
        val userClan = dbQuery {
            ClanTables.ClanMembers.select { ClanTables.ClanMembers.userId eq userId }.singleOrNull()
        }
        if (userClan != null) {
            return ClanOperationResult(false, "شما قبلاً در یک کلن هستید")
        }

        val clanId = UUID.randomUUID().toString() // Use full UUID for safety
        val clan = Clan(
            id = clanId,
            name = name,
            tag = tag,
            ownerId = userId,
            maxMembers = 50,
            coinsRequiredForNextLevel = 0
        )

        dbQuery {
            ClanTables.Clans.insert {
                it[ClanTables.Clans.id] = clan.id
                it[ClanTables.Clans.name] = clan.name
                it[ClanTables.Clans.tag] = clan.tag
                it[ClanTables.Clans.ownerId] = clan.ownerId
                it[ClanTables.Clans.level] = clan.level
                it[ClanTables.Clans.memberCount] = clan.memberCount
                it[ClanTables.Clans.maxMembers] = clan.maxMembers
                it[ClanTables.Clans.coinsRequiredForNextLevel] = clan.coinsRequiredForNextLevel
                it[ClanTables.Clans.totalCoinsContributed] = clan.totalCoinsContributed
            }
            ClanTables.ClanMembers.insert {
                it[ClanTables.ClanMembers.clanId] = clan.id
                it[ClanTables.ClanMembers.userId] = userId
                it[ClanTables.ClanMembers.role] = "owner"
            }
        }
        return ClanOperationResult(true, "کلن با موفقیت ایجاد شد", clan)
    }

    // ========== Join / Leave ==========
    suspend fun joinClan(userId: String, clanId: String): ClanOperationResult {
        // Check if user already in a clan
        val userClan = dbQuery {
            ClanTables.ClanMembers.selectAll().where { ClanTables.ClanMembers.userId eq userId }.singleOrNull()
        }
        if (userClan != null) {
            return ClanOperationResult(false, "شما قبلاً در یک کلن هستید")
        }

        val clanRow = dbQuery {
            ClanTables.Clans.selectAll().where { ClanTables.Clans.id eq clanId }.singleOrNull()
        } ?: return ClanOperationResult(false, "کلن یافت نشد")

        val currentMemberCount = clanRow[ClanTables.Clans.memberCount]
        val maxMembers = clanRow[ClanTables.Clans.maxMembers]
        if (currentMemberCount >= maxMembers) {
            return ClanOperationResult(false, "کلن تکمیل است")
        }

        dbQuery {
            ClanTables.ClanMembers.insert {
                it[ClanTables.ClanMembers.clanId] = clanId
                it[ClanTables.ClanMembers.userId] = userId
                it[ClanTables.ClanMembers.role] = "member"
            }
            ClanTables.Clans.update({ ClanTables.Clans.id eq clanId }) {
                it[ClanTables.Clans.memberCount] = currentMemberCount + 1
                it[ClanTables.Clans.updatedAt] = Instant.now()
            }
        }
        val updatedClan = getClanInfo(clanId)
        return ClanOperationResult(true, "با موفقیت به کلن پیوستید", updatedClan)
    }

    suspend fun leaveClan(userId: String, clanId: String): ClanOperationResult {
        val memberRow = dbQuery {
            ClanTables.ClanMembers.selectAll()
                .where { (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.userId eq userId) }
                .singleOrNull()
        } ?: return ClanOperationResult(false, "عضو کلن نیستید")

        val clanRow = dbQuery {
            ClanTables.Clans.selectAll().where { ClanTables.Clans.id eq clanId }.singleOrNull()
        } ?: return ClanOperationResult(false, "کلن یافت نشد")

        // If owner, transfer ownership or delete
        if (memberRow[ClanTables.ClanMembers.role] == "owner") {
            val nextOwner = dbQuery {
                ClanTables.ClanMembers.selectAll()
                    .where { (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.role neq "owner") }
                    .limit(1).singleOrNull()
            }
            if (nextOwner == null) {
                // Delete clan
                dbQuery {
                    ClanTables.ClanMembers.deleteWhere { ClanTables.ClanMembers.clanId eq clanId }
                    ClanTables.Clans.deleteWhere { ClanTables.Clans.id eq clanId }
                }
                return ClanOperationResult(true, "کلن با موفقیت حذف شد")
            } else {
                // Transfer ownership
                val nextOwnerId = nextOwner[ClanTables.ClanMembers.userId]
                dbQuery {
                    ClanTables.ClanMembers.update({ (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.userId eq nextOwnerId) }) {
                        it[ClanTables.ClanMembers.role] = "owner"
                    }
                    ClanTables.Clans.update({ ClanTables.Clans.id eq clanId }) {
                        it[ClanTables.Clans.ownerId] = nextOwnerId
                        it[ClanTables.Clans.updatedAt] = Instant.now()
                    }
                }
            }
        }

        dbQuery {
            ClanTables.ClanMembers.deleteWhere { (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.userId eq userId) }
            ClanTables.Clans.update({ ClanTables.Clans.id eq clanId }) {
                it[ClanTables.Clans.memberCount] = clanRow[ClanTables.Clans.memberCount] - 1
                it[ClanTables.Clans.updatedAt] = Instant.now()
            }
        }
        return ClanOperationResult(true, "با موفقیت از کلن خارج شدید")
    }

    // ========== Contribution & Upgrade ==========
    suspend fun contributeCoins(userId: String, clanId: String, amount: Long): ClanOperationResult {
        if (amount <= 0) {
            return ClanOperationResult(false, "مقدار سکه باید مثبت باشد")
        }

        // Check membership
        val memberRow = dbQuery {
            ClanTables.ClanMembers.selectAll()
                .where { (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.userId eq userId) }
                .singleOrNull()
        } ?: return ClanOperationResult(false, "عضو کلن نیستید")

        // Check user balance first (suspend)
        val userCanAfford = try {
            economyService.deductCoins(userId, amount, "clan_contribution", null)
            true
        } catch (e: Exception) {
            false
        }

        if (!userCanAfford) {
            return ClanOperationResult(false, "موجودی سکه کافی نیست")
        }

        // Update database
        dbQuery {
            ClanTables.ClanMembers.update({ (ClanTables.ClanMembers.clanId eq clanId) and (ClanTables.ClanMembers.userId eq userId) }) {
                it[ClanTables.ClanMembers.coinsContributed] = memberRow[ClanTables.ClanMembers.coinsContributed] + amount
            }
            ClanTables.Clans.update({ ClanTables.Clans.id eq clanId }) {
                it[ClanTables.Clans.totalCoinsContributed] = ClanTables.Clans.totalCoinsContributed.plus(amount)
                it[ClanTables.Clans.updatedAt] = Instant.now()
            }
        }
        val updatedClan = getClanInfo(clanId)
        return ClanOperationResult(true, "$amount سکه با موفقیت کمک شد", updatedClan)
    }

    suspend fun upgradeClan(clanId: String, userId: String): ClanOperationResult {
        val clanRow = dbQuery {
            ClanTables.Clans.selectAll().where { ClanTables.Clans.id eq clanId }.singleOrNull()
        } ?: return ClanOperationResult(false, "کلن یافت نشد")

        // Only owner can upgrade
        if (clanRow[ClanTables.Clans.ownerId] != userId) {
            return ClanOperationResult(false, "فقط مالک کلن می‌تواند ارتقا دهد")
        }

        val currentLevel = clanRow[ClanTables.Clans.level]
        val nextLevelInfo = dbQuery {
            ClanTables.ClanLevels.selectAll()
                .where { ClanTables.ClanLevels.level eq currentLevel + 1 }
                .singleOrNull()
        } ?: return ClanOperationResult(false, "کلن در حداکثر سطح است")

        val upgradeCost = nextLevelInfo[ClanTables.ClanLevels.upgradeCost]
        val totalContributed = clanRow[ClanTables.Clans.totalCoinsContributed]
        if (totalContributed < upgradeCost) {
            return ClanOperationResult(false, "سکه کافی برای ارتقا وجود ندارد")
        }

        // Get next upgrade cost
        val nextNextLevelInfo = dbQuery {
            ClanTables.ClanLevels.selectAll()
                .where { ClanTables.ClanLevels.level eq currentLevel + 2 }
                .singleOrNull()
        }
        val nextUpgradeCost = nextNextLevelInfo?.get(ClanTables.ClanLevels.upgradeCost) ?: 0L

        dbQuery {
            ClanTables.Clans.update({ ClanTables.Clans.id eq clanId }) {
                it[ClanTables.Clans.level] = currentLevel + 1
                it[ClanTables.Clans.maxMembers] = nextLevelInfo[ClanTables.ClanLevels.maxMembers]
                it[ClanTables.Clans.coinsRequiredForNextLevel] = nextUpgradeCost
                it[ClanTables.Clans.updatedAt] = Instant.now()
            }
        }
        val updatedClan = getClanInfo(clanId)
        return ClanOperationResult(true, "کلن با موفقیت ارتقا یافت", updatedClan)
    }

    // ========== Queries ==========
    suspend fun getClanInfo(clanId: String): Clan? {
        val row = dbQuery {
            ClanTables.Clans.selectAll().where { ClanTables.Clans.id eq clanId }.singleOrNull()
        } ?: return null
        return Clan(
            id = row[ClanTables.Clans.id],
            name = row[ClanTables.Clans.name],
            tag = row[ClanTables.Clans.tag],
            ownerId = row[ClanTables.Clans.ownerId],
            level = row[ClanTables.Clans.level],
            memberCount = row[ClanTables.Clans.memberCount],
            maxMembers = row[ClanTables.Clans.maxMembers],
            coinsRequiredForNextLevel = row[ClanTables.Clans.coinsRequiredForNextLevel],
            totalCoinsContributed = row[ClanTables.Clans.totalCoinsContributed],
            createdAt = row[ClanTables.Clans.createdAt].toEpochMilli(),
            updatedAt = row[ClanTables.Clans.updatedAt].toEpochMilli()
        )
    }

    suspend fun getClanMembers(clanId: String): List<ClanMember> {
        return dbQuery {
            ClanTables.ClanMembers.selectAll().where { ClanTables.ClanMembers.clanId eq clanId }
                .map {
                    ClanMember(
                        userId = it[ClanTables.ClanMembers.userId],
                        role = it[ClanTables.ClanMembers.role],
                        joinedAt = it[ClanTables.ClanMembers.joinedAt].toEpochMilli(),
                        coinsContributed = it[ClanTables.ClanMembers.coinsContributed]
                    )
                }
        }
    }

    suspend fun getUserClan(userId: String): Clan? {
        val member = dbQuery {
            ClanTables.ClanMembers.selectAll().where { ClanTables.ClanMembers.userId eq userId }.singleOrNull()
        } ?: return null
        return getClanInfo(member[ClanTables.ClanMembers.clanId])
    }

    suspend fun getAllClans(): List<Clan> {
        return dbQuery {
            ClanTables.Clans.selectAll().map { row ->
                Clan(
                    id = row[ClanTables.Clans.id],
                    name = row[ClanTables.Clans.name],
                    tag = row[ClanTables.Clans.tag],
                    ownerId = row[ClanTables.Clans.ownerId],
                    level = row[ClanTables.Clans.level],
                    memberCount = row[ClanTables.Clans.memberCount],
                    maxMembers = row[ClanTables.Clans.maxMembers],
                    coinsRequiredForNextLevel = row[ClanTables.Clans.coinsRequiredForNextLevel],
                    totalCoinsContributed = row[ClanTables.Clans.totalCoinsContributed],
                    createdAt = row[ClanTables.Clans.createdAt].toEpochMilli(),
                    updatedAt = row[ClanTables.Clans.updatedAt].toEpochMilli()
                )
            }
        }
    }

    suspend fun getClanChannel(clanId: String): String = "clan:$clanId"

    suspend fun subscribeUserToClanChat(userId: String, clanId: String) {
        val channel = getClanChannel(clanId)
        ChatServer.subscribeToChannel(userId, channel)
    }

    suspend fun unsubscribeUserFromClanChat(userId: String, clanId: String) {
        val channel = getClanChannel(clanId)
        ChatServer.unsubscribeFromChannel(userId, channel)
    }
}