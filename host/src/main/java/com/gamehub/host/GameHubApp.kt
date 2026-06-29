package com.gamehub.host

import android.app.Application
import com.gamehub.games.connectfour.ConnectFourEngine
import com.gamehub.games.ludo.LudoEngine
import com.gamehub.games.monopoly.MonopolyEngine
import com.gamehub.games.tictactoe.TicTacToeEngine
import com.gamehub.games.chess.ChessEngine
import com.gamehub.games.farkle.FarkleEngine
import com.gamehub.games.farkle.FarkleModule
import com.gamehub.games.esmofamil.EsmoFamilEngine
import com.gamehub.games.esmofamil.EsmoFamilModule
import com.gamehub.games.backgammon.BackgammonEngine
import com.gamehub.games.backgammon.BackgammonModule
import com.gamehub.games.abalone.AbaloneEngine
import com.gamehub.games.abalone.AbaloneModule
import com.gamehub.games.spadesbaloot.SpadesBalootEngine
import com.gamehub.games.spadesbaloot.SpadesBalootModule
import com.gamehub.games.baltazar.BaltazarEngine
import com.gamehub.games.baltazar.BaltazarModule
import com.gamehub.games.othello.OthelloEngine
import com.gamehub.games.othello.OthelloModule
import com.gamehub.games.bridge.BridgeModule
import com.gamehub.games.bridge.BridgeEngine
import com.gamehub.games.checkers.CheckersModule
import com.gamehub.games.checkers.CheckersEngine
import com.gamehub.games.blokus.BlokusModule
import com.gamehub.games.blokus.BlokusEngine
import com.gamehub.games.yahtzee.YahtzeeModule
import com.gamehub.games.yahtzee.YahtzeeEngine
import com.gamehub.host.notifications.NotificationManager
import com.gamehub.host.util.PowerHelper
import com.gamehub.shared.engine.GameEngineBridge
import com.gamehub.shared.engine.GameEngineFactory
import com.gamehub.shared.registry.GameRegistry
import com.gamehub.games.tictactoe.TicTacToeModule
import com.gamehub.games.uno.UnoModule
import com.gamehub.games.connectfour.ConnectFourModule
import com.gamehub.games.ludo.LudoModule
import com.gamehub.games.monopoly.MonopolyModule
import com.gamehub.games.chess.ChessModule
import com.gamehub.games.nard.NardModule
import com.gamehub.games.nard.NardEngine
import com.gamehub.games.hex.HexModule
import com.gamehub.games.hex.HexEngine
import com.gamehub.games.battleship.BattleshipModule
import com.gamehub.games.battleship.BattleshipEngine
import com.gamehub.games.matchmonster.MatchMonsterModule
import com.gamehub.games.matchmonster.MatchMonsterEngine
import com.gamehub.games.uno.UnoEngine
import com.gamehub.games.soccerstriker.SoccerStrikerModule
import com.gamehub.games.soccerstriker.SoccerStrikerEngine
import com.gamehub.host.network.gameJson
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.engine.GameEngine

class GameHubApp : Application() {
    lateinit var notificationManager: NotificationManager
    lateinit var powerHelper: PowerHelper

    override fun onCreate() {
        super.onCreate()

        // Initialize power helper
        powerHelper = PowerHelper(this)

        // ثبت ماژول‌های بازی در رجیستری (برای LobbyScreen)
        GameRegistry.register(TicTacToeModule())
        GameRegistry.register(UnoModule())
        GameRegistry.register(ConnectFourModule())
        GameRegistry.register(LudoModule())
        GameRegistry.register(MonopolyModule())
        GameRegistry.register(ChessModule())
        GameRegistry.register(FarkleModule())
        GameRegistry.register(EsmoFamilModule())
        GameRegistry.register(BackgammonModule())
        GameRegistry.register(AbaloneModule())
        GameRegistry.register(SpadesBalootModule())
        GameRegistry.register(OthelloModule())
        GameRegistry.register(BaltazarModule())
        GameRegistry.register(BridgeModule())
        GameRegistry.register(CheckersModule())
        GameRegistry.register(BlokusModule())
        GameRegistry.register(YahtzeeModule())
        GameRegistry.register(NardModule())
        GameRegistry.register(HexModule())
        GameRegistry.register(BattleshipModule())
        GameRegistry.register(MatchMonsterModule())
        GameRegistry.register(SoccerStrikerModule())

        // ثبت موتورهای بازی در GameEngineFactory
        GameEngineFactory.register("tictactoe") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(TicTacToeEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("connectfour") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(ConnectFourEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("uno") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(UnoEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("ludo") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(LudoEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("monopoly") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(MonopolyEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("chess") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(ChessEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("farkle") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(FarkleEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("esmofamil") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(EsmoFamilEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("backgammon") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(BackgammonEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("abalone") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(AbaloneEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("spades-baloot") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(SpadesBalootEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("othello") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(OthelloEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("baltazar") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(BaltazarEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("bridge") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(BridgeEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("checkers") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(CheckersEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("blokus") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(BlokusEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("yahtzee") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(YahtzeeEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("nard") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(NardEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("hex") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(HexEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("battleship") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(BattleshipEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("match-monster") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(MatchMonsterEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
        GameEngineFactory.register("soccer-striker") { json ->
            @Suppress("UNCHECKED_CAST")
            GameEngineBridge(SoccerStrikerEngine(), json) as GameEngine<GameState, GameAction, GameResult>
        }
    }
}