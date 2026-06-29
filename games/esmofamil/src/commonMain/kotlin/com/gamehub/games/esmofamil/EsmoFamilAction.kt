package com.gamehub.games.esmofamil

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class EsmoFamilAction : GameAction() {
    @Serializable
    data class SubmitAnswers(val answers: Map<Int, String?>) : EsmoFamilAction()
}
