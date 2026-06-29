package com.gamehub.games.blokus

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class BlokusAction : GameAction() {
    @Serializable
    @SerialName("BlokusAction.Place")
    data class Place(
        val pieceId: Int,
        val rotation: Int,
        val anchorRow: Int,
        val anchorCol: Int
    ) : BlokusAction()

    @Serializable
    @SerialName("BlokusAction.Pass")
    object Pass : BlokusAction()
}
