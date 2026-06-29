package com.gamehub.shared.game

import kotlinx.serialization.Serializable

@Serializable
data class GameConfig(
    val id: Long,
    val gameId: String,
    val mode: String,
    val config: GameParameters,
    val version: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class GameParameters(
    // General
    val minPlayers: Int = 2,
    val maxPlayers: Int = 4,
    val turnTimeoutSeconds: Int = 30,
    val disconnectGraceSeconds: Int = 20,
    val maxGracePeriodsPerPlayer: Int = 2,
    val maxMissedTurnsBeforeRemoval: Int = 3,
    val allowSpectator: Boolean = true,
    // Ranked specific
    val initialElo: Int = 1200,
    val kFactorBase: Int = 60,
    val kFactorNormal: Int = 32,
    val kFactorHigh: Int = 16,
    val upsetBonusEnabled: Boolean = true,
    val upsetThreshold: Double = 0.3,
    val upsetAlpha: Double = 0.25,
    val demotionShieldLosses: Int = 3,
    val recoveryMultiplier: Double = 1.5,
    val minLevelRequired: Int = 3,
    val rankedSeasonDurationDays: Int = 60,
    val softResetFactor: Double = 0.5,
    val maxPartySkillSpread: Int = 300,
    // Tournament specific
    val entryFeeCoins: Int = 0,
    val entryFeeDiamonds: Int = 0,
    val maxParticipants: Int = 16,
    val bracketType: String = "single_elimination", // single_elimination, group_stage_elimination
    val autoStartThreshold: Int = 4,
    val prizePoolDistribution: Map<String, Double> = emptyMap(), // e.g., {"1":0.5,"2":0.3,"3":0.2}
    // Casual specific
    val botEnabled: Boolean = true,
    val botDifficultyMin: Int = 1,
    val botDifficultyMax: Int = 10,
    val quickPlayEnabled: Boolean = true,
    val backfillEnabled: Boolean = true,
    val allowBotBackfill: Boolean = true
)

@Serializable
data class GameConfigCreateRequest(
    val gameId: String,
    val mode: String,
    val config: GameParameters
)

@Serializable
data class GameConfigUpdateRequest(
    val config: GameParameters,
    val version: Int
)