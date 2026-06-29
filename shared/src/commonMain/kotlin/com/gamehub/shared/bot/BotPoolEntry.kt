package com.gamehub.shared.bot

import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.matchmaking.SkillRating
import kotlinx.serialization.Serializable

@Serializable
data class BotPoolEntry(
    val botId: PlayerId,
    val gameId: String,
    val difficultyLevel: Int,
    val rating: SkillRating
)