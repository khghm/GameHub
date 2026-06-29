package com.gamehub.games.bridge

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId

class BridgeBotStrategy : BotStrategy<BridgeState, BridgeAction> {
    override val gameId: String = "bridge"
    override val supportedDifficultyLevels: IntRange = 1..5

    override suspend fun getNextMove(state: BridgeState, botPlayerId: PlayerId, difficultyLevel: Int): BridgeAction? {
        val actingSeat = state.seatPlayers.entries.find { it.value == botPlayerId }?.key ?: return null
        
        val validActions = getValidActions(state, actingSeat)
        
        if (validActions.isEmpty()) {
            return null
        }
        
        // Simple strategy: pick first valid action
        return validActions.firstOrNull()
    }
}