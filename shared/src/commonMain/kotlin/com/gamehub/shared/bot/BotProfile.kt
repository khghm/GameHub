package com.gamehub.shared.bot

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.serialization.Serializable

@Serializable
data class BotProfile(
    val botId: PlayerId,
    val username: String,
    val avatarId: String,
    val gameId: String,
    val difficultyLevel: Int,
    val rating: SkillRating,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val isActive: Boolean = true,
    val isTutorial: Boolean = false,
    val isShadow: Boolean = true,
    val lastRotation: Long? = null,
    val lastGameAt: Long? = null,
    val totalGamesPlayed: Int = 0,
    val winCount: Int = 0,
    val lossCount: Int = 0
) {
    val winRate: Double get() = if (totalGames > 0) wins.toDouble() / totalGames else 0.0
}