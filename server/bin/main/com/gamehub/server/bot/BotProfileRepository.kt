package com.gamehub.server.bot

import com.gamehub.server.persistence.DatabaseFactory.dbQuery
import com.gamehub.shared.bot.BotProfile
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.SkillRating
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit

object BotProfilesTable : Table("bot_profiles") {
    val botId = varchar("bot_id", 255)
    val username = varchar("username", 100)
    val avatarId = varchar("avatar_id", 100)
    val gameId = varchar("game_id", 50)
    val difficultyLevel = integer("difficulty_level")
    val eloMean = double("elo_mean")
    val eloSigma = double("elo_sigma")
    val totalGames = integer("total_games").default(0)
    val wins = integer("wins").default(0)
    val losses = integer("losses").default(0)
    val isActive = bool("is_active").default(true)
    val isTutorial = bool("is_tutorial").default(false)
    val isShadow = bool("is_shadow").default(true)
    val createdAt = timestamp("created_at").default(Instant.now())
    val updatedAt = timestamp("updated_at").default(Instant.now())
    val lastRotation = timestamp("last_rotation").nullable()
    val lastGameAt = timestamp("last_game_at").nullable()
    val totalGamesPlayed = integer("total_games_played").default(0)
    val winCount = integer("win_count").default(0)
    val lossCount = integer("loss_count").default(0)

    override val primaryKey = PrimaryKey(botId)
}

class BotProfileRepository {

    suspend fun findById(botId: String): BotProfile? = dbQuery {
        BotProfilesTable.select { BotProfilesTable.botId eq botId }
            .singleOrNull()
            ?.let { rowToProfile(it) }
    }

    suspend fun findAllActive(gameId: String? = null): List<BotProfile> = dbQuery {
        var query = BotProfilesTable.select { BotProfilesTable.isActive eq true }
        if (gameId != null) {
            query = query.andWhere { BotProfilesTable.gameId eq gameId }
        }
        query.map { rowToProfile(it) }
    }

    suspend fun findInactiveBots(daysInactive: Int): List<BotProfile> = dbQuery {
        val cutoff = Instant.now().minus(daysInactive.toLong(), ChronoUnit.DAYS)
        BotProfilesTable.select {
            (BotProfilesTable.isActive eq true) and
                    (BotProfilesTable.lastGameAt lessEq cutoff) and
                    (BotProfilesTable.isTutorial eq false)
        }.map { rowToProfile(it) }
    }

    suspend fun save(profile: BotProfile): Unit = dbQuery {
        val exists = BotProfilesTable.select { BotProfilesTable.botId eq profile.botId.value }.singleOrNull() != null
        if (!exists) {
            BotProfilesTable.insert {
                it[botId] = profile.botId.value
                it[username] = profile.username
                it[avatarId] = profile.avatarId
                it[gameId] = profile.gameId
                it[difficultyLevel] = profile.difficultyLevel
                it[eloMean] = profile.rating.mean
                it[eloSigma] = profile.rating.standardDeviation
                it[totalGames] = profile.totalGames
                it[wins] = profile.wins
                it[losses] = profile.losses
                it[isActive] = profile.isActive
                it[isTutorial] = profile.isTutorial
                it[isShadow] = profile.isShadow
                it[lastRotation] = profile.lastRotation?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
                it[lastGameAt] = profile.lastGameAt?.let { Instant.ofEpochMilli(it) } ?: Instant.now()
                it[totalGamesPlayed] = profile.totalGamesPlayed
                it[winCount] = profile.winCount
                it[lossCount] = profile.lossCount
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        }
    }

    suspend fun updateStats(botId: String, totalGames: Int, wins: Int, losses: Int, rating: SkillRating): Unit = dbQuery {
        BotProfilesTable.update({ BotProfilesTable.botId eq botId }) {
            it[this.totalGames] = totalGames
            it[this.wins] = wins
            it[this.losses] = losses
            it[eloMean] = rating.mean
            it[eloSigma] = rating.standardDeviation
            it[updatedAt] = Instant.now()
            it[lastGameAt] = Instant.now()
        }
    }

    suspend fun updateLastGame(botId: String) = dbQuery {
        BotProfilesTable.update({ BotProfilesTable.botId eq botId }) {
            it[lastGameAt] = Instant.now()
        }
    }

    suspend fun rotateProfile(botId: String, newUsername: String, newAvatarId: String): Unit = dbQuery {
        BotProfilesTable.update({ BotProfilesTable.botId eq botId }) {
            it[username] = newUsername
            it[avatarId] = newAvatarId
            it[lastRotation] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun deactivateBot(botId: String): Unit = dbQuery {
        BotProfilesTable.update({ BotProfilesTable.botId eq botId }) {
            it[isActive] = false
            it[updatedAt] = Instant.now()
        }
    }

    private fun rowToProfile(row: ResultRow): BotProfile = BotProfile(
        botId = PlayerId(row[BotProfilesTable.botId]),
        username = row[BotProfilesTable.username],
        avatarId = row[BotProfilesTable.avatarId],
        gameId = row[BotProfilesTable.gameId],
        difficultyLevel = row[BotProfilesTable.difficultyLevel],
        rating = SkillRating(
            mean = row[BotProfilesTable.eloMean],
            standardDeviation = row[BotProfilesTable.eloSigma]
        ),
        totalGames = row[BotProfilesTable.totalGames],
        wins = row[BotProfilesTable.wins],
        losses = row[BotProfilesTable.losses],
        isActive = row[BotProfilesTable.isActive],
        isTutorial = row[BotProfilesTable.isTutorial],
        isShadow = row[BotProfilesTable.isShadow],
        lastRotation = row[BotProfilesTable.lastRotation]?.toEpochMilli(),
        lastGameAt = row[BotProfilesTable.lastGameAt]?.toEpochMilli(),
        totalGamesPlayed = row[BotProfilesTable.totalGamesPlayed],
        winCount = row[BotProfilesTable.winCount],
        lossCount = row[BotProfilesTable.lossCount]
    )
}