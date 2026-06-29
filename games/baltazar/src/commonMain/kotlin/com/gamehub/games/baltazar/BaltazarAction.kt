package com.gamehub.games.baltazar

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class BaltazarAction : GameAction() {
    @Serializable
    @SerialName("BaltazarAction.SelectCell")
    data class SelectCell(val row: Int, val col: Int) : BaltazarAction()

    @Serializable
    @SerialName("BaltazarAction.DeselectLast")
    data object DeselectLast : BaltazarAction()

    @Serializable
    @SerialName("BaltazarAction.SubmitWord")
    data object SubmitWord : BaltazarAction()
}
