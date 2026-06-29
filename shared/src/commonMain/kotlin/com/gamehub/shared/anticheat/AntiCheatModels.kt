// shared/src/commonMain/kotlin/com/gamehub/shared/anticheat/AntiCheatModels.kt
package com.gamehub.shared.anticheat

import kotlinx.serialization.Serializable

@Serializable
enum class ViolationType {
    SPEED_HACK,
    LAG_SWITCH,
    MACRO,
    COLLUSION,
    MULTI_DEVICE
}

@Serializable
data class CheatAttempt(
    val userId: String,
    val gameId: String,
    val matchId: String,
    val violationType: ViolationType,
    val confidenceScore: Double,
    val details: Map<String, String> = emptyMap()
)

@Serializable
data class Penalty(
    val userId: String,
    val trustScoreDelta: Int,
    val eloDelta: Int,
    val coinDelta: Int,
    val suspensionHours: Int,
    val shadowPool: Boolean,
    val reason: String
)