package com.gamehub.games.farkle

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import kotlinx.serialization.Serializable
import com.gamehub.shared.dice.DiceEngine
import com.gamehub.shared.dice.createProfileForGame

class FarkleEngine : GameDefinition<FarkleState, FarkleAction, GameResult> {
    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "farkle",
        name = "فارکل (Farkle)",
        minPlayers = 2,
        maxPlayers = 8,
        description = "بازی شانسی و استراتژیک فارکل با ۶ تاس!"
    )

    fun calculateScore(diceValues: List<Int>): Int {
        if (diceValues.isEmpty()) return 0
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }
        var score = 0
        val usedDice = mutableSetOf<Int>()
        val sortedValues = diceValues.sorted()

        if (diceValues.size == 6) {
            if (sortedValues == listOf(1,2,3,4,5,6)) {
                return 1500
            }

            if (counts.values.all { it == 2 } && counts.size == 3) {
                return 1500
            }
        }

        val diceList = diceValues.toMutableList()

        for (num in 1..6) {
            val count = counts[num] ?: continue
            if (count >= 3) {
                val baseScore = when(num) {
                    1 -> 1000
                    else -> num * 100
                }
                val bonus = (count - 3) * baseScore
                score += baseScore + bonus
                repeat(count) {
                    val idx = diceList.indexOf(num)
                    if (idx != -1) {
                        diceList.removeAt(idx)
                    }
                }
            }
        }

        for (die in diceList) {
            when(die) {
                1 -> score += 100
                5 -> score +=50
            }
        }

        return score
    }

    fun isScoringCombination(diceValues: List<Int>): Boolean {
        if (diceValues.isEmpty()) return false
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }
        val sorted = diceValues.sorted()
        if (diceValues.size == 6) {
            if (sorted == listOf(1,2,3,4,5,6)) return true
            if (counts.values.all { it == 2 } && counts.size == 3) return true
        }

        for ((num, count) in counts) {
            if (count >=3) return true
            if (num == 1 || num == 5) return true
        }
        return diceValues.any { it == 1 || it ==5 }
    }

    fun isFarkle(diceValues: List<Int>): Boolean {
        return !isScoringCombination(diceValues)
    }

    fun createInitialDice(): List<FarkleDice> {
        return (0..5).map { idx -> FarkleDice(id=idx, value=0, state=FarkleDiceState.IDLE) }
    }

    fun rollDice(state: FarkleState, diceEngine: DiceEngine): FarkleState {
        val idleDiceCount = state.dice.count { it.state == FarkleDiceState.IDLE }
        val rolls = diceEngine.roll(idleDiceCount)
        var rollIndex = 0
        val newDice = state.dice.map { die ->
            if (die.state == FarkleDiceState.IDLE) {
                val value = rolls[rollIndex]
                rollIndex++
                die.copy(value = value, state=FarkleDiceState.ROLLED)
            } else {
                die
            }
        }

        val rolledDiceValues = newDice.filter { it.state == FarkleDiceState.ROLLED }.map { it.value }
        if (isFarkle(rolledDiceValues)) {
            return state.copy(
                dice = newDice,
                turnScore = 0,
                selectedDiceIds = emptyList(),
                diceEngines = state.diceEngines
            )
        }

        return state.copy(dice = newDice, diceEngines = state.diceEngines)
    }

    fun selectDice(state: FarkleState, diceIds: List<Int>): FarkleState? {
        val validIds = diceIds.filter { id ->
            state.dice.find { it.id == id }?.state == FarkleDiceState.ROLLED
        }
        val selectedValues = validIds.mapNotNull { id ->
            state.dice.find { it.id == id }?.value
        }
        if (!isScoringCombination(selectedValues)) {
            return null
        }
        val newScore = state.turnScore + calculateScore(selectedValues)
        val newDice = state.dice.map { die ->
            when {
                validIds.contains(die.id) -> {
                    die.copy(state = FarkleDiceState.SELECTED)
                }
                die.state == FarkleDiceState.ROLLED -> {
                    die.copy(state = FarkleDiceState.IDLE)
                }
                else -> {
                    die
                }
            }
        }

        return state.copy(
            dice = newDice,
            selectedDiceIds = state.selectedDiceIds + validIds,
            turnScore = newScore,
            diceEngines = state.diceEngines
        )
    }

    fun bankScore(state: FarkleState): GameUpdateResult<FarkleState, GameResult>? {
        val currentPlayer = state.currentPlayer ?: return null
        val currentStats = state.stats[currentPlayer] ?: return null

        if (!currentStats.hasEnteredGame && state.turnScore < state.entryThreshold) {
            return null
        }

        val newTotalScore = currentStats.totalScore + state.turnScore
        val newHasEntered = currentStats.hasEnteredGame || state.turnScore >= state.entryThreshold

        val newStats = state.stats.toMutableMap()
        newStats[currentPlayer] = currentStats.copy(
            totalScore = newTotalScore,
            hasEnteredGame = newHasEntered
        )

        var isFinalRound = state.isFinalRound
        var finalRoundPlayers = state.playersWhoHadFinalTurn.toMutableList()

        if (!isFinalRound && newTotalScore >= state.targetScore) {
            isFinalRound = true
            finalRoundPlayers.add(currentPlayer)
        }

        val currentIdx = state.players.indexOf(currentPlayer)
        var nextPlayerIdx = (currentIdx + 1) % state.players.size
        var nextPlayer = state.players[nextPlayerIdx]

        var gameEnded = false
        if (isFinalRound) {
            if (finalRoundPlayers.containsAll(state.players)) {
                gameEnded = true
            } else {
                while (finalRoundPlayers.contains(nextPlayer) && nextPlayerIdx != currentIdx) {
                    nextPlayerIdx = (nextPlayerIdx + 1) % state.players.size
                    nextPlayer = state.players[nextPlayerIdx]
                }
                if (!finalRoundPlayers.contains(nextPlayer)) {
                    finalRoundPlayers.add(nextPlayer)
                }
            }
        }

        val newDice = createInitialDice()

        val newState = state.copy(
            stats = newStats,
            currentPlayer = if (gameEnded) null else nextPlayer,
            turnScore = 0,
            dice = newDice,
            selectedDiceIds = emptyList(),
            isFinalRound = isFinalRound,
            playersWhoHadFinalTurn = finalRoundPlayers,
            diceEngines = state.diceEngines
        )

        val result = if (gameEnded) {
            val maxScore = newStats.values.maxOf { it.totalScore }
            val winners = newStats.filterValues { it.totalScore == maxScore }.keys.toList()
            if (winners.size == 1) {
                GameResult.Win(winners[0])
            } else {
                GameResult.Draw
            }
        } else {
            null
        }

        return GameUpdateResult(newState, result)
    }

    fun continueHotDice(state: FarkleState): FarkleState? {
        val allSelected = state.dice.all { it.state == FarkleDiceState.SELECTED }
        if (!allSelected) {
            return null
        }
        return state.copy(
            dice = createInitialDice(),
            selectedDiceIds = emptyList(),
            diceEngines = state.diceEngines
        )
    }

    override fun applyAction(
        state: FarkleState,
        action: FarkleAction,
        playerId: PlayerId
    ): GameUpdateResult<FarkleState, GameResult> {
        if (state.currentPlayer != playerId) {
            return GameUpdateResult(state)
        }

        return when (action) {
            is FarkleAction.RollDice -> {
                val diceEngine = state.diceEngines[playerId] ?: throw IllegalStateException("DiceEngine not found for player $playerId")
                val newState = rollDice(state, diceEngine)
                val farkle = newState.dice.filter { it.state == FarkleDiceState.ROLLED }.map { it.value }.let { isFarkle(it) }
                if (farkle) {
                    val currentIdx = state.players.indexOf(playerId)
                    val nextIdx = (currentIdx + 1) % state.players.size
                    var nextPlayer = state.players[nextIdx]
                    var finalRoundPlayers = state.playersWhoHadFinalTurn.toMutableList()

                    if (state.isFinalRound && !finalRoundPlayers.contains(nextPlayer)) {
                        finalRoundPlayers.add(nextPlayer)
                    }

                    val gameEnded = state.isFinalRound && finalRoundPlayers.containsAll(state.players)

                    val result = if (gameEnded) {
                        val maxScore = state.stats.values.maxOf { it.totalScore }
                        val winners = state.stats.filterValues { it.totalScore == maxScore }.keys.toList()
                        if (winners.size ==1) GameResult.Win(winners[0]) else GameResult.Draw
                    } else null

                    GameUpdateResult(
                        newState.copy(
                            currentPlayer = if (gameEnded) null else nextPlayer,
                            turnScore = 0,
                            dice = createInitialDice(),
                            selectedDiceIds = emptyList(),
                            playersWhoHadFinalTurn = finalRoundPlayers,
                            diceEngines = state.diceEngines
                        ),
                        result
                    )
                } else {
                    GameUpdateResult(newState)
                }
            }
            is FarkleAction.SelectDice -> {
                val newState = selectDice(state, action.diceIds)
                GameUpdateResult(newState ?: state)
            }
            is FarkleAction.BankScore -> {
                bankScore(state) ?: GameUpdateResult(state)
            }
            is FarkleAction.ContinueHotDice -> {
                val newState = continueHotDice(state)
                GameUpdateResult(newState ?: state)
            }
        }
    }

    override fun validateAction(state: FarkleState, action: FarkleAction, playerId: PlayerId): Boolean {
        if (state.currentPlayer != playerId) return false
        return when (action) {
            is FarkleAction.RollDice -> {
                state.dice.any { it.state == FarkleDiceState.IDLE }
            }
            is FarkleAction.SelectDice -> {
                val validIds = action.diceIds.filter { id ->
                    state.dice.find { it.id == id }?.state == FarkleDiceState.ROLLED
                }
                val values = validIds.mapNotNull { id -> state.dice.find { it.id == id }?.value }
                isScoringCombination(values)
            }
            is FarkleAction.BankScore -> {
                val currentStats = state.stats[playerId]
                val turnScoreValid = currentStats?.hasEnteredGame == true || state.turnScore >= state.entryThreshold
                state.selectedDiceIds.isNotEmpty() && turnScoreValid
            }
            is FarkleAction.ContinueHotDice -> {
                state.dice.all { it.state == FarkleDiceState.SELECTED }
            }
        }
    }

    override fun isTerminal(state: FarkleState): Boolean =
        state.isFinalRound && state.playersWhoHadFinalTurn.containsAll(state.players)

    override fun getResult(state: FarkleState): GameResult? {
        if (!isTerminal(state)) return null
        val maxScore = state.stats.values.maxOf { it.totalScore }
        val winners = state.stats.filterValues { it.totalScore == maxScore }.keys.toList()
        return if (winners.size ==1) GameResult.Win(winners[0]) else GameResult.Draw
    }

    override fun getPlayers(state: FarkleState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: FarkleState, playerId: PlayerId): FarkleState =
        state.copy(currentPlayer = playerId, diceEngines = state.diceEngines)

    override fun createInitialState(players: List<PlayerId>): FarkleState {
        val stats = players.associateWith { playerId ->
            FarklePlayerStats(playerId = playerId)
        }
        val diceEngines = players.associateWith { player ->
            DiceEngine(createProfileForGame("farkle"), player.value)
        }
        return FarkleState(
            players = players,
            stats = stats,
            currentPlayer = players.firstOrNull(),
            dice = createInitialDice(),
            targetScore = 10000,
            entryThreshold = 500,
            diceEngines = diceEngines
        )
    }
}
