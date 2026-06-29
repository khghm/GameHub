package com.gamehub.games.nard

import com.gamehub.shared.core.GameDefinition
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameUpdateResult
import kotlin.random.Random

class NardEngine : GameDefinition<NardState, NardAction, GameResult> {
    override val metadata = com.gamehub.shared.core.GameMetadata(
        id = "nard",
        name = "تخته نرد شرقی (Nard)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی کلاسیک تخته نرد - یک بازی استراتژیک و هیجان‌انگیز",
    )

    private fun rollDice(): List<Int> {
        val d1 = Random.nextInt(1, 7)
        val d2 = Random.nextInt(1, 7)
        return if (d1 == d2) {
            listOf(d1, d1, d1, d1)
        } else {
            listOf(d1, d2)
        }
    }

    override fun applyAction(
        state: NardState,
        action: NardAction,
        playerId: PlayerId
    ): GameUpdateResult<NardState, GameResult> {
        if (state.gameOver) return GameUpdateResult(state)
        val playerColor = NardRules.getPlayerColor(state, playerId)
        if (state.turn != playerColor) return GameUpdateResult(state)

        return when (action) {
            NardAction.RollDice -> {
                if (state.diceRolled) return GameUpdateResult(state)
                val newDice = rollDice()
                GameUpdateResult(
                    state.copy(
                        dice = newDice,
                        diceRolled = true,
                        canOfferDouble = false
                    )
                )
            }

            NardAction.OfferDouble -> {
                if (!state.canOfferDouble) return GameUpdateResult(state)
                val cubeOwner = state.doublingCube.owner
                if (cubeOwner != null && cubeOwner != playerId) return GameUpdateResult(state)
                GameUpdateResult(
                    state.copy(
                        doubleOffered = true,
                        doubleOfferedBy = playerId,
                        canOfferDouble = false
                    )
                )
            }

            NardAction.AcceptDouble -> {
                if (!state.doubleOffered || state.doubleOfferedBy == playerId) return GameUpdateResult(state)
                val newCube = state.doublingCube.copy(
                    value = state.doublingCube.value * 2,
                    owner = playerId
                )
                GameUpdateResult(
                    state.copy(
                        doublingCube = newCube,
                        doubleOffered = false,
                        doubleOfferedBy = null,
                        canOfferDouble = false,
                        cubeHasBeenDoubled = true
                    )
                )
            }

            NardAction.DeclineDouble -> {
                if (!state.doubleOffered || state.doubleOfferedBy == playerId) return GameUpdateResult(state)
                val winnerId = state.doubleOfferedBy ?: state.players.firstOrNull { it != playerId } ?: playerId
                GameUpdateResult(
                    state.copy(gameOver = true),
                    GameResult.Win(winnerId)
                )
            }

            is NardAction.Move -> {
                if (!state.diceRolled) return GameUpdateResult(state)
                val validMoves = NardRules.getValidMoves(state, playerColor, state.dice)
                val move = Pair(action.from, action.to)
                if (move !in validMoves) return GameUpdateResult(state)
                if (action.die !in state.dice) return GameUpdateResult(state)

                var newState = NardRules.applyMove(state, playerColor, action.from, action.to, action.die)
                val newDice = state.dice.toMutableList()
                val dieIndex = newDice.indexOf(action.die)
                if (dieIndex != -1) {
                    newDice.removeAt(dieIndex)
                }
                newState = newState.copy(dice = newDice)

                // Check for win condition
                if (newState.borneOffWhite == 15) {
                    val winType = NardRules.getWinType(newState, NardColor.WHITE)
                    return GameUpdateResult(
                        newState.copy(gameOver = true, winType = winType),
                        GameResult.Win(NardRules.getColorPlayer(newState, NardColor.WHITE))
                    )
                }
                if (newState.borneOffBlack == 15) {
                    val winType = NardRules.getWinType(newState, NardColor.BLACK)
                    return GameUpdateResult(
                        newState.copy(gameOver = true, winType = winType),
                        GameResult.Win(NardRules.getColorPlayer(newState, NardColor.BLACK))
                    )
                }
                GameUpdateResult(newState)
            }

            NardAction.EndTurn -> {
                val nextColor = state.turn.opponent()
                val nextPlayerId = NardRules.getColorPlayer(state, nextColor)
                GameUpdateResult(
                    state.copy(
                        turn = nextColor,
                        currentPlayer = nextPlayerId,
                        diceRolled = false,
                        dice = emptyList(),
                        canOfferDouble = true
                    )
                )
            }
        }
    }

    override fun validateAction(
        state: NardState,
        action: NardAction,
        playerId: PlayerId
    ): Boolean {
        if (state.gameOver) return false

        // Double offer/accept/decline can be done by either player when double is offered
        if (state.doubleOffered && (action is NardAction.AcceptDouble || action is NardAction.DeclineDouble)) {
            return state.doubleOfferedBy != playerId
        }

        val playerColor = NardRules.getPlayerColor(state, playerId)
        if (state.turn != playerColor) return false

        return when (action) {
            NardAction.RollDice -> !state.diceRolled && !state.doubleOffered
            NardAction.OfferDouble -> {
                !state.doubleOffered &&
                        state.canOfferDouble &&
                        !state.isCrawfordGame &&
                        (state.doublingCube.owner == null || state.doublingCube.owner == playerId)
            }
            NardAction.AcceptDouble -> state.doubleOffered && state.doubleOfferedBy != playerId
            NardAction.DeclineDouble -> state.doubleOffered && state.doubleOfferedBy != playerId
            is NardAction.Move -> {
                !state.doubleOffered &&
                        state.diceRolled &&
                        action.die in state.dice &&
                        NardRules.getValidMoves(state, playerColor, listOf(action.die)).contains(Pair(action.from, action.to))
            }
            NardAction.EndTurn -> !state.doubleOffered && state.diceRolled
        }
    }

    override fun isTerminal(state: NardState): Boolean = state.gameOver

    override fun getResult(state: NardState): GameResult? {
        if (!state.gameOver) return null
        return when {
            state.borneOffWhite == 15 -> GameResult.Win(NardRules.getColorPlayer(state, NardColor.WHITE))
            state.borneOffBlack == 15 -> GameResult.Win(NardRules.getColorPlayer(state, NardColor.BLACK))
            else -> null
        }
    }

    override fun getPlayers(state: NardState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: NardState, playerId: PlayerId): NardState =
        state.copy(currentPlayer = playerId, turn = NardRules.getPlayerColor(state, playerId))

    override fun createInitialState(players: List<PlayerId>): NardState {
        return NardState(
            players = players,
            currentPlayer = players.firstOrNull()
        )
    }

    override fun skipTurn(state: NardState, playerId: PlayerId): GameUpdateResult<NardState, GameResult> {
        val nextColor = state.turn.opponent()
        val nextPlayerId = NardRules.getColorPlayer(state, nextColor)
        return GameUpdateResult(
            state.copy(
                turn = nextColor,
                currentPlayer = nextPlayerId,
                diceRolled = false,
                dice = emptyList(),
                canOfferDouble = true
            )
        )
    }
}
