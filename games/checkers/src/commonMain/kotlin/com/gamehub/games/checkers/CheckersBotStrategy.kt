package com.gamehub.games.checkers

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId

class CheckersBotStrategy : BotStrategy<CheckersState, CheckersAction> {
    override val gameId: String = "checkers"
    override val supportedDifficultyLevels: IntRange = 1..5

    override suspend fun getNextMove(state: CheckersState, botPlayerId: PlayerId, difficultyLevel: Int): CheckersAction? {
        val validActions = CheckersEngine().getValidActions(state, botPlayerId)
        if (validActions.isEmpty()) return null
        // Simple strategy: pick first valid action!
        return validActions.firstOrNull()
    }
}
