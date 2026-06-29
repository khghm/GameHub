// shared/src/commonMain/kotlin/com/gamehub/shared/core/GameMode.kt
package com.gamehub.shared.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class GameMode {
    @SerialName("casual")
    CASUAL,

    @SerialName("ranked")
    RANKED,

    @SerialName("tournament")
    TOURNAMENT,

    @SerialName("private")
    PRIVATE,

    @SerialName("tutorial")
    TUTORIAL
}