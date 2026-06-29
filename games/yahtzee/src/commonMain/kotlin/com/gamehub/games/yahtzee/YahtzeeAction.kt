package com.gamehub.games.yahtzee

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
sealed class YahtzeeAction : GameAction() {
    @Serializable
    data object Roll : YahtzeeAction()

    @Serializable
    data class HoldDice(val heldIndices: Set<Int>) : YahtzeeAction()

    @Serializable
    data class ScoreCategory(val category: YahtzeeCategory) : YahtzeeAction()
}

@Serializable
enum class YahtzeeCategory {
    ONES,
    TWOS,
    THREES,
    FOURS,
    FIVES,
    SIXES,
    THREE_OF_A_KIND,
    FOUR_OF_A_KIND,
    FULL_HOUSE,
    SMALL_STRAIGHT,
    LARGE_STRAIGHT,
    YAHTZEE,
    CHANCE
}
