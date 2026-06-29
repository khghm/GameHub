// shared/src/commonMain/kotlin/com/gamehub/shared/core/GameResult.kt
package com.gamehub.shared.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class GameResult {
    @Serializable
    @SerialName("win")
    data class Win(val winner: PlayerId) : GameResult()

    @Serializable
    @SerialName("draw")
    data object Draw : GameResult()
}