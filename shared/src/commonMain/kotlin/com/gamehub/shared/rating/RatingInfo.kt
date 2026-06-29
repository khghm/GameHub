// shared/src/commonMain/kotlin/com/gamehub/shared/rating/RatingInfo.kt
package com.gamehub.shared.rating

import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.serialization.Serializable

@Serializable
data class RatingInfo(
    val rating: Int,
    val skillRating: SkillRating = SkillRating(rating.toDouble(), 350.0),
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val tier: String,
    val division: Int
)

@Serializable
data class RatingChange(
    val userId: String,
    val gameId: String,
    val matchId: String,
    val oldRating: Int,
    val newRating: Int,
    val change: Int,
    val reason: String
)