package com.gamehub.games.farkle

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

class FarkleBotStrategy : BotStrategy<FarkleState, FarkleAction> {
    override val gameId: String = "farkle"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: FarkleState, botPlayerId: PlayerId, difficultyLevel: Int): FarkleAction? {
        val engine = FarkleEngine()
        val stats = state.stats[botPlayerId] ?: return null

        val delayMs = when {
            difficultyLevel <= 3 -> 100L
            difficultyLevel <= 6 -> 200L
            else -> 300L
        }
        delay(delayMs)

        val hasIdleDice = state.dice.any { it.state == FarkleDiceState.IDLE }
        val hasRolledDice = state.dice.any { it.state == FarkleDiceState.ROLLED }
        val allSelected = state.dice.all { it.state == FarkleDiceState.SELECTED }
        val hasSomeSelected = state.selectedDiceIds.isNotEmpty()

        // 1. First, if there are rolled dice: we must select some!
        if (hasRolledDice) {
            val rolled = state.dice.filter { it.state == FarkleDiceState.ROLLED }

            // Try to pick 3 of a kind first!
            val counts = rolled.groupBy { it.value }.mapValues { it.value.size }
            for (num in counts.keys) {
                if (counts[num]!! >= 3) {
                    val groupIds = rolled.filter { it.value == num }.map { it.id }
                    if (engine.validateAction(state, FarkleAction.SelectDice(groupIds), botPlayerId)) {
                        return FarkleAction.SelectDice(groupIds)
                    }
                }
            }

            // If not, try to select all 1s and 5s!
            val scoringIds = rolled.filter { it.value == 1 || it.value == 5 }.map { it.id }
            if (scoringIds.isNotEmpty() && engine.validateAction(state, FarkleAction.SelectDice(scoringIds), botPlayerId)) {
                return FarkleAction.SelectDice(scoringIds)
            }

            // Fallback: pick at least one scoring die!
            val anyScoring = rolled.find { it.value == 1 || it.value == 5 }
            if (anyScoring != null && engine.validateAction(state, FarkleAction.SelectDice(listOf(anyScoring.id)), botPlayerId)) {
                return FarkleAction.SelectDice(listOf(anyScoring.id))
            }
        }

        // 2. Now check if we can bank!
        val canBank = engine.validateAction(state, FarkleAction.BankScore, botPlayerId)
        if (canBank) {
            val shouldBank = when {
                difficultyLevel <= 3 -> state.turnScore >= 200
                difficultyLevel <= 6 -> state.turnScore >= 350
                else -> state.turnScore >= 450
            }
            // If we have no idle dice, or should bank, then bank!
            if (!hasIdleDice || shouldBank) {
                return FarkleAction.BankScore
            }
        }

        // 3. If all selected, check if we can continue hot dice!
        if (allSelected) {
            val canContinue = engine.validateAction(state, FarkleAction.ContinueHotDice, botPlayerId)
            if (canContinue && difficultyLevel > 4 && state.turnScore < 500) {
                return FarkleAction.ContinueHotDice
            } else if (canBank) {
                return FarkleAction.BankScore
            }
        }

        // 4. If we have idle dice and shouldn't bank, roll!
        if (hasIdleDice && engine.validateAction(state, FarkleAction.RollDice, botPlayerId)) {
            val shouldBankInstead = when {
                difficultyLevel <= 3 -> state.turnScore >= 250
                difficultyLevel <= 6 -> state.turnScore >= 350
                else -> state.turnScore >= 450
            }
            if (canBank && shouldBankInstead) {
                return FarkleAction.BankScore
            } else {
                return FarkleAction.RollDice
            }
        }

        // Last resort: bank if possible!
        if (canBank) {
            return FarkleAction.BankScore
        }

        return null
    }
}
