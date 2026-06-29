// shared/src/commonMain/kotlin/com/gamehub/shared/behavior/BehaviorInfo.kt
package com.gamehub.shared.behavior

import kotlinx.serialization.Serializable

@Serializable
data class BehaviorInfo(
    val userId: String,
    val score: Int,
    val band: String,
    val lastBandChange: Long? = null
)