package com.gamehub.games.backgammon

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import com.gamehub.shared.dice.DiceEngine
import com.gamehub.shared.dice.createProfileForGame

class BackgammonEngine : GameDefinition<BackgammonState, BackgammonAction, GameResult> {
    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "backgammon",
        name = "تخته‌نرد (Backgammon)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک تخته‌نرد!"
    )

    private fun rollDice(diceEngine: DiceEngine): List<Int> {
        val rolls = diceEngine.roll(2)
        val d1 = rolls[0]
        val d2 = rolls[1]
        return if (d1 == d2) {
            listOf(d1, d1, d1, d1)
        } else {
            listOf(d1, d2)
        }
    }

    override fun applyAction(
        state: BackgammonState,
        action: BackgammonAction,
        playerId: PlayerId
    ): GameUpdateResult<BackgammonState, GameResult> {
        if (state.gameOver) return GameUpdateResult(state)
        val playerColor = BackgammonRules.getPlayerColor(state, playerId)
        if (state.turn != playerColor) return GameUpdateResult(state)

        return when (action) {
            BackgammonAction.RollDice -> {
                if (state.diceRolled) return GameUpdateResult(state)
                val diceEngine = state.diceEngines[playerId] ?: throw IllegalStateException("DiceEngine not found for player $playerId")
                val newDice = rollDice(diceEngine)
                GameUpdateResult(
                    state.copy(
                        dice = newDice,
                        diceRolled = true,
                        canOfferDouble = false,
                        diceEngines = state.diceEngines
                    )
                )
            }

            BackgammonAction.OfferDouble -> {
                if (!state.canOfferDouble) return GameUpdateResult(state)
                val cubeOwner = state.doublingCube.owner
                if (cubeOwner != null && cubeOwner != playerId) return GameUpdateResult(state)
                GameUpdateResult(state)
            }

            BackgammonAction.AcceptDouble -> {
                val newCube = state.doublingCube.copy(
                    value = state.doublingCube.value * 2,
                    owner = playerId
                )
                GameUpdateResult(
                    state.copy(
                        doublingCube = newCube,
                        canOfferDouble = false,
                        diceEngines = state.diceEngines
                    )
                )
            }

            BackgammonAction.DeclineDouble -> {
                val winnerId = state.players.firstOrNull { it != playerId } ?: playerId
                GameUpdateResult(
                    state.copy(gameOver = true, diceEngines = state.diceEngines),
                    GameResult.Win(winnerId)
                )
            }

            is BackgammonAction.Move -> {
                if (!state.diceRolled) return GameUpdateResult(state)
                val validMoves = BackgammonRules.getValidMoves(state, playerColor, state.dice)
                val move = Pair(action.from, action.to)
                if (move !in validMoves) return GameUpdateResult(state)
                if (action.die !in state.dice) return GameUpdateResult(state)

                var newState = BackgammonRules.applyMove(state, playerColor, action.from, action.to, action.die)
                val newDice = state.dice.toMutableList()
                newDice.remove(action.die)
                newState = newState.copy(dice = newDice, diceEngines = state.diceEngines)

                if (newState.borneOffWhite == 15) {
                    return GameUpdateResult(
                        newState.copy(gameOver = true, diceEngines = state.diceEngines),
                        GameResult.Win(BackgammonRules.getColorPlayer(newState, BackgammonColor.WHITE))
                    )
                }
                if (newState.borneOffBlack == 15) {
                    return GameUpdateResult(
                        newState.copy(gameOver = true, diceEngines = state.diceEngines),
                        GameResult.Win(BackgammonRules.getColorPlayer(newState, BackgammonColor.BLACK))
                    )
                }
                GameUpdateResult(newState)
            }

            BackgammonAction.EndTurn -> {
                val nextColor = state.turn.opponent()
                val nextPlayerId = BackgammonRules.getColorPlayer(state, nextColor)
                GameUpdateResult(
                    state.copy(
                        turn = nextColor,
                        currentPlayer = nextPlayerId,
                        diceRolled = false,
                        dice = emptyList(),
                        canOfferDouble = true,
                        diceEngines = state.diceEngines
                    )
                )
            }
        }
    }

    override fun validateAction(
        state: BackgammonState,
        action: BackgammonAction,
        playerId: PlayerId
    ): Boolean {
        if (state.gameOver) return false
        val playerColor = BackgammonRules.getPlayerColor(state, playerId)
        if (state.turn != playerColor) return false

        return when (action) {
            BackgammonAction.RollDice -> !state.diceRolled
            BackgammonAction.OfferDouble -> {
                state.canOfferDouble &&
                        (state.doublingCube.owner == null || state.doublingCube.owner == playerId)
            }
            BackgammonAction.AcceptDouble,
            BackgammonAction.DeclineDouble -> true
            is BackgammonAction.Move -> {
                state.diceRolled &&
                        action.die in state.dice &&
                        BackgammonRules.getValidMoves(state, playerColor, listOf(action.die)).contains(Pair(action.from, action.to))
            }
            BackgammonAction.EndTurn -> state.diceRolled
        }
    }

    override fun isTerminal(state: BackgammonState): Boolean = state.gameOver

    override fun getResult(state: BackgammonState): GameResult? {
        if (!state.gameOver) return null
        return when {
            state.borneOffWhite == 15 -> GameResult.Win(BackgammonRules.getColorPlayer(state, BackgammonColor.WHITE))
            state.borneOffBlack == 15 -> GameResult.Win(BackgammonRules.getColorPlayer(state, BackgammonColor.BLACK))
            else -> null
        }
    }

    override fun getPlayers(state: BackgammonState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: BackgammonState, playerId: PlayerId): BackgammonState =
        state.copy(currentPlayer = playerId, turn = BackgammonRules.getPlayerColor(state, playerId), diceEngines = state.diceEngines)

    override fun createInitialState(players: List<PlayerId>): BackgammonState {
        val diceEngines = players.associateWith { player ->
            DiceEngine(createProfileForGame("backgammon"), player.value)
        }
        return BackgammonState(
            players = players,
            currentPlayer = players.firstOrNull(),
            diceEngines = diceEngines
        )
    }

    override fun skipTurn(state: BackgammonState, playerId: PlayerId): GameUpdateResult<BackgammonState, GameResult> {
        val nextColor = state.turn.opponent()
        val nextPlayerId = BackgammonRules.getColorPlayer(state, nextColor)
        return GameUpdateResult(
            state.copy(
                turn = nextColor,
                currentPlayer = nextPlayerId,
                diceRolled = false,
                dice = emptyList(),
                canOfferDouble = true,
                diceEngines = state.diceEngines
            )
        )
    }
}
