package com.gamehub.server.bot

import com.gamehub.games.connectfour.ConnectFourBotStrategy
import com.gamehub.games.ludo.LudoBotStrategy
import com.gamehub.games.monopoly.MonopolyBotStrategy
import com.gamehub.games.tictactoe.TicTacToeBotStrategy
import com.gamehub.games.uno.UnoBotStrategy
import com.gamehub.games.chess.ChessBotStrategy
import com.gamehub.games.farkle.FarkleBotStrategy
import com.gamehub.games.esmofamil.EsmoFamilBotStrategy
import com.gamehub.games.backgammon.BackgammonBotStrategy
import com.gamehub.games.abalone.AbaloneBotStrategy
import com.gamehub.games.spadesbaloot.SpadesBalootBotStrategy
import com.gamehub.games.othello.OthelloBotStrategy
import com.gamehub.games.baltazar.BaltazarBotStrategy
import com.gamehub.games.bridge.BridgeBotStrategy
import com.gamehub.games.checkers.CheckersBotStrategy
import com.gamehub.games.blokus.BlokusBotStrategy
import com.gamehub.games.yahtzee.YahtzeeBotStrategy
import com.gamehub.games.nard.NardBotStrategy
import com.gamehub.games.hex.HexBotStrategy
import com.gamehub.games.battleship.BattleshipBotStrategy
import com.gamehub.games.matchmonster.MatchMonsterBotStrategy
import com.gamehub.games.soccerstriker.SoccerStrikerBotStrategy
import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameState

object BotStrategyRegistry {
    private val strategies = mutableMapOf<String, BotStrategy<out GameState, out GameAction>>()

    init {
        register(TicTacToeBotStrategy())
        register(ConnectFourBotStrategy())
        register(UnoBotStrategy())
        register(LudoBotStrategy())
        register(MonopolyBotStrategy())
        register(ChessBotStrategy())
        register(FarkleBotStrategy())
        register(EsmoFamilBotStrategy())
        register(BackgammonBotStrategy())
        register(AbaloneBotStrategy())
        register(SpadesBalootBotStrategy())
        register(OthelloBotStrategy())
        register(BaltazarBotStrategy())
        register(BridgeBotStrategy())
        register(CheckersBotStrategy())
        register(BlokusBotStrategy())
        register(YahtzeeBotStrategy())
        register(NardBotStrategy())
        register(HexBotStrategy())
        register(BattleshipBotStrategy())
        register(MatchMonsterBotStrategy())
        register(SoccerStrikerBotStrategy())
    }

    fun register(strategy: BotStrategy<*, *>) {
        strategies[strategy.gameId] = strategy
    }

    @Suppress("UNCHECKED_CAST")
    fun <State : GameState, Action : GameAction> get(gameId: String): BotStrategy<State, Action>? {
        return strategies[gameId] as? BotStrategy<State, Action>
    }
}