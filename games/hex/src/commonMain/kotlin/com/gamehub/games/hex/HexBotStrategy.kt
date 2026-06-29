package com.gamehub.games.hex

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardAction
import kotlin.random.Random

class HexBotStrategy : BotStrategy<HexState, BoardAction> {
    override val gameId: String = "hex"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: HexState, botPlayerId: PlayerId, difficultyLevel: Int): BoardAction? {
        val emptyCells = mutableListOf<BoardAction>()
        for (row in 0 until 11) {
            for (col in 0 until 11) {
                if (state.grid[row][col] == null) {
                    emptyCells.add(BoardAction(row, col))
                }
            }
        }
        return if (emptyCells.isEmpty()) null else emptyCells.random(Random)
    }
}
