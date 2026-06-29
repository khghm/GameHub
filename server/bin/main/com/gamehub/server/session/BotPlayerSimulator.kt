// server/src/main/kotlin/com/gamehub/server/session/BotPlayerSimulator.kt
package com.gamehub.server.session

import com.gamehub.games.ludo.LudoState
import com.gamehub.games.monopoly.MonopolyState
import com.gamehub.games.monopoly.TradeStep
import com.gamehub.games.monopoly.TurnPhase
import com.gamehub.server.bot.BotStrategyRegistry
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engine.GameServerContract
import kotlinx.coroutines.*

class BotPlayerSimulator<State : GameState, Action : GameAction>(
    private val gameSession: GameServerContract<State, Action, *>,
    private val botPlayerId: PlayerId,
    private val difficultyLevel: Int,
    private val scope: CoroutineScope,
    private val moveDelayMs: Long = 800L
) {
    private var isActive = true
    private var isProcessing = false

    fun start() {
        println("🤖 BotPlayerSimulator started for bot $botPlayerId (difficulty=$difficultyLevel)")
        scope.launch {
            while (isActive && !gameSession.isTerminal()) {
                delay(500)
                val currentPlayer = getCurrentPlayer(gameSession.currentState)
                val state = gameSession.currentState
                val isSimultaneousGame =
                    gameSession.definition.metadata.id == "match-monster" ||
                            gameSession.definition.metadata.id == "esmofamil"

                val shouldAct =
                    isSimultaneousGame ||
                            currentPlayer == botPlayerId ||
                            shouldActInTradeState(state) ||
                            shouldActInEsmoFamilState(state, botPlayerId)

                if (shouldAct && !isProcessing) {
                    isProcessing = true
                    try {
                        val strategy = BotStrategyRegistry.get<State, Action>(gameSession.definition.metadata.id)
                        if (strategy != null) {
                            delay(moveDelayMs)
                            val action = strategy.getNextMove(state, botPlayerId, difficultyLevel)
                            if (action != null) {
                                println("🤖 Bot $botPlayerId executing: ${action::class.simpleName}")
                                gameSession.submitAction(botPlayerId, action)
                            } else {
                                println("⚠️ Bot $botPlayerId has no valid move")
                            }
                        } else {
                            println("⚠️ No bot strategy found for game ${gameSession.definition.metadata.id}")
                        }
                    } catch (e: Exception) {
                        println("❌ Bot error: ${e.message}")
                        e.printStackTrace()
                    } finally {
                        isProcessing = false
                    }
                }
                if (gameSession.isTerminal()) break
            }
            println("🤖 Bot $botPlayerId stopped (game ended)")
        }
    }

    private fun shouldActInTradeState(state: GameState): Boolean {
        if (state is MonopolyState) {
            // در هر مرحله از معامله، اگر ربات مخاطب عمل است (پیشنهاد دهنده یا پاسخ‌دهنده)
            return state.turnPhase == TurnPhase.AWAITING_TRADE &&
                    (state.tradeStep == TradeStep.AWAITING_PROPOSALS ||
                            state.tradeStep == TradeStep.AWAITING_COUNTER ||
                            (state.tradeStep == TradeStep.AWAITING_RESPONSE && state.pendingTrade?.toPlayer == botPlayerId))
        }
        return false
    }

    private fun shouldActInEsmoFamilState(state: GameState, botPlayerId: PlayerId): Boolean {
        return if (state is com.gamehub.games.esmofamil.EsmoFamilState) {
            state.phase == com.gamehub.games.esmofamil.EsmoFamilPhase.ANSWERING &&
                    !(state.playerAnswers.containsKey(botPlayerId) &&
                            state.playerAnswers[botPlayerId]?.answers?.isNotEmpty() == true)
        } else {
            false
        }
    }

    private fun getCurrentPlayer(state: GameState): PlayerId? {
        return when (state) {
            is com.gamehub.shared.engines.board.BoardState -> state.currentPlayer
            is com.gamehub.shared.engines.card.CardGameState -> state.currentPlayer
            is MonopolyState -> state.currentPlayer
            is LudoState -> state.currentPlayer
            is com.gamehub.games.blokus.BlokusState -> state.currentPlayer
            is com.gamehub.games.soccerstriker.SoccerStrikerState -> state.currentPlayer
            else -> try {
                val field = state.javaClass.getDeclaredField("currentPlayer")
                field.isAccessible = true
                field.get(state) as? PlayerId
            } catch (e: Exception) {
                null
            }
        }
    }

    fun stop() {
        isActive = false
    }
}