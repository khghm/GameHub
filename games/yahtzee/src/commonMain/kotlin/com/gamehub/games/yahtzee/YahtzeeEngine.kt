package com.gamehub.games.yahtzee

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult
import com.gamehub.shared.dice.DiceEngine
import com.gamehub.shared.dice.createProfileForGame

class YahtzeeEngine : GameDefinition<YahtzeeState, YahtzeeAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "yahtzee",
        name = "Yahtzee (یاتزی)",
        minPlayers = 2,
        maxPlayers = 4,
        description = "بازی استراتژی و شانس با ۵ تاس"
    )

    override fun createInitialState(players: List<PlayerId>): YahtzeeState {
        val diceEngines = players.associateWith { player ->
            DiceEngine(createProfileForGame("yahtzee"), player.value)
        }
        return YahtzeeState.initial(players, diceEngines)
    }

    override fun validateAction(
        state: YahtzeeState,
        action: YahtzeeAction,
        playerId: PlayerId
    ): Boolean {
        if (state.currentPlayer != playerId || state.gameOver) return false

        return when (action) {
            is YahtzeeAction.Roll -> state.rollsRemaining > 0
            is YahtzeeAction.HoldDice -> state.rollsRemaining in 1..2
            is YahtzeeAction.ScoreCategory -> {
                val playerData = state.playerData[playerId] ?: return false
                val category = action.category
                playerData.scores[category] == null
            }
        }
    }

    override fun applyAction(
        state: YahtzeeState,
        action: YahtzeeAction,
        playerId: PlayerId
    ): GameUpdateResult<YahtzeeState, GameResult> {
        val newState = when (action) {
            is YahtzeeAction.Roll -> rollDice(state)
            is YahtzeeAction.HoldDice -> state.copy(heldDice = action.heldIndices, diceEngines = state.diceEngines)
            is YahtzeeAction.ScoreCategory -> scoreCategory(state, playerId, action.category)
        }

        val result = getResult(newState)
        return GameUpdateResult(newState, result)
    }

    private fun rollDice(state: YahtzeeState): YahtzeeState {
        val currentPlayer = state.currentPlayer ?: return state
        val diceEngine = state.diceEngines[currentPlayer] ?: throw IllegalStateException("DiceEngine not found for player $currentPlayer")
        val idleDiceCount = 5 - state.heldDice.size
        val rolls = diceEngine.roll(idleDiceCount)
        var rollIndex = 0
        val newDice = state.dice.mapIndexed { index, value ->
            if (state.heldDice.contains(index)) {
                value
            } else {
                val rollValue = rolls[rollIndex]
                rollIndex++
                rollValue
            }
        }
        return state.copy(
            dice = newDice,
            rollsRemaining = state.rollsRemaining - 1,
            diceEngines = state.diceEngines
        )
    }

    private fun scoreCategory(
        state: YahtzeeState,
        playerId: PlayerId,
        category: YahtzeeCategory
    ): YahtzeeState {
        val playerData = state.playerData[playerId] ?: return state
        val score = calculateScore(state.dice, category)

        val isYahtzee = state.dice.all { it == state.dice[0] }
        val currentYahtzeeScore = playerData.scores[YahtzeeCategory.YAHTZEE]

        var newYahtzeeBonuses = playerData.yahtzeeBonuses
        var newHasHadYahtzee = playerData.hasHadYahtzee
        if (isYahtzee && currentYahtzeeScore != null) {
            newYahtzeeBonuses += 1
        }
        if (isYahtzee) {
            newHasHadYahtzee = true
        }

        val newScores = playerData.scores.toMutableMap().apply {
            this[category] = score
        }

        val upperTotal = listOf(
            YahtzeeCategory.ONES, YahtzeeCategory.TWOS, YahtzeeCategory.THREES,
            YahtzeeCategory.FOURS, YahtzeeCategory.FIVES, YahtzeeCategory.SIXES
        ).sumOf { newScores[it] ?: 0 }

        val newUpperBonus = if (upperTotal >= 63 && playerData.upperBonus == 0) 35 else playerData.upperBonus

        val newPlayerData = playerData.copy(
            scores = newScores,
            upperBonus = newUpperBonus,
            yahtzeeBonuses = newYahtzeeBonuses,
            hasHadYahtzee = newHasHadYahtzee
        )

        val newPlayerDataMap = state.playerData.toMutableMap().apply {
            this[playerId] = newPlayerData
        }

        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size

        return state.copy(
            playerData = newPlayerDataMap,
            currentPlayerIndex = nextPlayerIndex,
            dice = listOf(0, 0, 0, 0, 0),
            heldDice = emptySet(),
            rollsRemaining = 3,
            diceEngines = state.diceEngines
        )
    }

    fun calculateScore(dice: List<Int>, category: YahtzeeCategory): Int {
        val sorted = dice.sorted()
        val counts = sorted.groupBy { it }.mapValues { it.value.size }

        return when (category) {
            YahtzeeCategory.ONES -> dice.count { it == 1 } * 1
            YahtzeeCategory.TWOS -> dice.count { it == 2 } * 2
            YahtzeeCategory.THREES -> dice.count { it == 3 } * 3
            YahtzeeCategory.FOURS -> dice.count { it == 4 } * 4
            YahtzeeCategory.FIVES -> dice.count { it == 5 } * 5
            YahtzeeCategory.SIXES -> dice.count { it == 6 } * 6

            YahtzeeCategory.THREE_OF_A_KIND ->
                if (counts.values.any { it >= 3 }) dice.sum() else 0

            YahtzeeCategory.FOUR_OF_A_KIND ->
                if (counts.values.any { it >= 4 }) dice.sum() else 0

            YahtzeeCategory.FULL_HOUSE -> {
                val values = counts.values.sorted()
                if (values == listOf(2, 3) || counts.values.any { it == 5 }) 25 else 0
            }

            YahtzeeCategory.SMALL_STRAIGHT -> {
                val uniqueSorted = sorted.distinct()
                val sequences = listOf(
                    listOf(1, 2, 3, 4),
                    listOf(2, 3, 4, 5),
                    listOf(3, 4, 5, 6)
                )
                if (sequences.any { uniqueSorted.containsAll(it) }) 30 else 0
            }

            YahtzeeCategory.LARGE_STRAIGHT -> {
                val uniqueSorted = sorted.distinct()
                if (uniqueSorted == listOf(1, 2, 3, 4, 5) || uniqueSorted == listOf(2, 3, 4, 5, 6)) 40 else 0
            }

            YahtzeeCategory.YAHTZEE -> if (counts.values.any { it == 5 }) 50 else 0
            YahtzeeCategory.CHANCE -> dice.sum()
        }
    }

    override fun isTerminal(state: YahtzeeState): Boolean =
        state.playerData.values.all { it.scores.values.all { score -> score != null } }

    override fun getResult(state: YahtzeeState): GameResult? {
        if (!isTerminal(state)) return null
        val winner = state.playerData.maxByOrNull { it.value.totalScore }?.key
        return if (winner != null) GameResult.Win(winner) else GameResult.Draw
    }

    override fun getPlayers(state: YahtzeeState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: YahtzeeState, playerId: PlayerId): YahtzeeState {
        val index = state.players.indexOf(playerId)
        return state.copy(currentPlayerIndex = if (index >=0 ) index else state.currentPlayerIndex, diceEngines = state.diceEngines)
    }

    override fun skipTurn(state: YahtzeeState, playerId: PlayerId): GameUpdateResult<YahtzeeState, GameResult> {
        val playerData = state.playerData[playerId] ?: return GameUpdateResult(state, null)
        val emptyCategory = playerData.scores.entries.firstOrNull { it.value == null }?.key
        return if (emptyCategory != null) {
            applyAction(state, YahtzeeAction.ScoreCategory(emptyCategory), playerId)
        } else {
            GameUpdateResult(state, null)
        }
    }
}
