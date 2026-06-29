package com.gamehub.games.yahtzee

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class YahtzeeBotStrategy : BotStrategy<YahtzeeState, YahtzeeAction> {
    override val gameId: String = "yahtzee"
    override val supportedDifficultyLevels: IntRange = 1..10

    private val engine = YahtzeeEngine()

    override suspend fun getNextMove(
        state: YahtzeeState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): YahtzeeAction? {
        println("🤖 YahtzeeBotStrategy called for $botPlayerId")

        val playerData = state.playerData[botPlayerId] ?: return null
        val delayMs = when {
            difficultyLevel <= 3 -> 50L
            difficultyLevel <= 6 -> 100L
            else -> 150L
        }
        delay(delayMs)

        if (state.rollsRemaining > 0 && state.rollsRemaining < 3) {
            return pickRandomEmptyCategory(playerData)
        }

        if (state.rollsRemaining == 3) {
            return YahtzeeAction.Roll
        }

        return pickRandomEmptyCategory(playerData)
    }

    private fun pickRandomEmptyCategory(playerData: YahtzeePlayerData): YahtzeeAction? {
        val emptyCategories = playerData.scores.filter { it.value == null }.keys.toList()
        if (emptyCategories.isEmpty()) return null
        return YahtzeeAction.ScoreCategory(emptyCategories.random(Random))
    }
}
