package com.gamehub.games.abalone

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class AbaloneBotStrategy : BotStrategy<AbaloneState, AbaloneAction> {
    override val gameId: String = "abalone"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(
        state: AbaloneState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): AbaloneAction? {
        // Simple random bot: pick a random marble and random direction
        val botColor = if (botPlayerId == state.blackPlayerId) AbaloneColor.BLACK else AbaloneColor.WHITE
        val ourMarbles = state.marbles.filter { it.color == botColor }

        if (ourMarbles.isEmpty()) return null

        // Delay based on difficulty
        val delayMs = when {
            difficultyLevel <= 3 -> 500L
            difficultyLevel <= 6 -> 1000L
            else -> 1500L
        }
        delay(delayMs)

        // Try to pick a random valid move
        val dirs = AbaloneDirection.entries
        for (i in 0 until 100) {
            val randomMarble = ourMarbles.random()
            val randomDir = dirs.random()
            // Just a simple move for now
            return AbaloneAction.Move(listOf(randomMarble.pos), randomDir)
        }
        return null
    }
}
