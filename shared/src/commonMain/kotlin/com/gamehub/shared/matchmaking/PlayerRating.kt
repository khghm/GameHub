// shared/src/commonMain/kotlin/com/gamehub/shared/matchmaking/PlayerRating.kt
package com.gamehub.shared.matchmaking

import kotlinx.serialization.Serializable

@Serializable
data class PlayerRating(
    val userId: String,
    val gameType: String,
    val rating: SkillRating,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0
)