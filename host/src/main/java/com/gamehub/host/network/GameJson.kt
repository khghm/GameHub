// host/src/main/java/com/gamehub/host/network/GameJson.kt
package com.gamehub.host.network

import com.gamehub.games.chess.ChessAction
import com.gamehub.games.chess.ChessState
import com.gamehub.games.farkle.FarkleState
import com.gamehub.games.farkle.FarkleAction
import com.gamehub.games.ludo.LudoAction
import com.gamehub.games.esmofamil.EsmoFamilState
import com.gamehub.games.backgammon.BackgammonState
import com.gamehub.games.backgammon.BackgammonAction
import com.gamehub.games.nard.NardState
import com.gamehub.games.nard.NardAction
import com.gamehub.games.connectfour.ConnectFourState
import com.gamehub.games.ludo.LudoState
import com.gamehub.games.monopoly.MonopolyState
import com.gamehub.games.monopoly.MonopolyAction
import com.gamehub.games.tictactoe.TicTacToeState
import com.gamehub.games.uno.UnoState
import com.gamehub.games.abalone.AbaloneAction
import com.gamehub.games.abalone.AbaloneState
import com.gamehub.games.spadesbaloot.SpadesBalootAction
import com.gamehub.games.spadesbaloot.SpadesBalootState
import com.gamehub.games.othello.OthelloAction
import com.gamehub.games.othello.OthelloState
import com.gamehub.games.baltazar.BaltazarAction
import com.gamehub.games.baltazar.BaltazarState
import com.gamehub.games.bridge.BridgeState
import com.gamehub.games.bridge.BridgeAction
import com.gamehub.games.checkers.CheckersState
import com.gamehub.games.checkers.CheckersAction
import com.gamehub.games.blokus.BlokusState
import com.gamehub.games.blokus.BlokusAction
import com.gamehub.games.yahtzee.YahtzeeState
import com.gamehub.games.yahtzee.YahtzeeAction
import com.gamehub.games.hex.HexState
import com.gamehub.games.battleship.BattleshipState
import com.gamehub.games.battleship.BattleshipAction
import com.gamehub.games.matchmonster.MatchMonsterState
import com.gamehub.games.matchmonster.MatchMonsterAction
import com.gamehub.games.soccerstriker.SoccerStrikerState
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.board.BoardState
import com.gamehub.shared.engines.board.BoardAction
import com.gamehub.shared.engines.card.CardAction
import com.gamehub.shared.networking.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val gameJson = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
    classDiscriminator = "messageType"
    serializersModule = SerializersModule {
        polymorphic(GameState::class) {
            subclass(BoardState::class, BoardState.serializer())
            subclass(TicTacToeState::class, TicTacToeState.serializer())
            subclass(ConnectFourState::class, ConnectFourState.serializer())
            subclass(UnoState::class, UnoState.serializer())
            subclass(LudoState::class, LudoState.serializer())
            subclass(MonopolyState::class, MonopolyState.serializer())
            subclass(ChessState::class, ChessState.serializer())
            subclass(FarkleState::class, FarkleState.serializer())
            subclass(EsmoFamilState::class, EsmoFamilState.serializer())
            subclass(BackgammonState::class, BackgammonState.serializer())
            subclass(NardState::class, NardState.serializer())
            subclass(AbaloneState::class, AbaloneState.serializer())
            subclass(SpadesBalootState::class, SpadesBalootState.serializer())
            subclass(OthelloState::class, OthelloState.serializer())
            subclass(BaltazarState::class, BaltazarState.serializer())
            subclass(BridgeState::class, BridgeState.serializer())
            subclass(CheckersState::class, CheckersState.serializer())
            subclass(BlokusState::class, BlokusState.serializer())
            subclass(YahtzeeState::class, YahtzeeState.serializer())
            subclass(HexState::class, HexState.serializer())
            subclass(BattleshipState::class, BattleshipState.serializer())
            subclass(MatchMonsterState::class, MatchMonsterState.serializer())
            subclass(SoccerStrikerState::class, SoccerStrikerState.serializer())
        }

        polymorphic(GameAction::class) {
            subclass(BoardAction::class, BoardAction.serializer())
            subclass(CardAction.PlayCard::class, CardAction.PlayCard.serializer())
            subclass(CardAction.DrawCard::class, CardAction.DrawCard.serializer())
            subclass(ChessAction.Move::class, ChessAction.Move.serializer())
            subclass(BackgammonAction.RollDice::class, BackgammonAction.RollDice.serializer())
            subclass(BackgammonAction.OfferDouble::class, BackgammonAction.OfferDouble.serializer())
            subclass(BackgammonAction.AcceptDouble::class, BackgammonAction.AcceptDouble.serializer())
            subclass(BackgammonAction.DeclineDouble::class, BackgammonAction.DeclineDouble.serializer())
            subclass(BackgammonAction.Move::class, BackgammonAction.Move.serializer())
            subclass(BackgammonAction.EndTurn::class, BackgammonAction.EndTurn.serializer())
            subclass(NardAction.RollDice::class, NardAction.RollDice.serializer())
            subclass(NardAction.OfferDouble::class, NardAction.OfferDouble.serializer())
            subclass(NardAction.AcceptDouble::class, NardAction.AcceptDouble.serializer())
            subclass(NardAction.DeclineDouble::class, NardAction.DeclineDouble.serializer())
            subclass(NardAction.Move::class, NardAction.Move.serializer())
            subclass(NardAction.EndTurn::class, NardAction.EndTurn.serializer())
            subclass(AbaloneAction.Move::class, AbaloneAction.Move.serializer())
            subclass(SpadesBalootAction.Bid::class, SpadesBalootAction.Bid.serializer())
            subclass(SpadesBalootAction.DeclareBaloot::class, SpadesBalootAction.DeclareBaloot.serializer())
            subclass(SpadesBalootAction.PlayCard::class, SpadesBalootAction.PlayCard.serializer())
            subclass(OthelloAction.Move::class, OthelloAction.Move.serializer())
            subclass(BaltazarAction.SelectCell::class, BaltazarAction.SelectCell.serializer())
            subclass(BaltazarAction.DeselectLast::class, BaltazarAction.DeselectLast.serializer())
            subclass(BaltazarAction.SubmitWord::class, BaltazarAction.SubmitWord.serializer())
            subclass(BridgeAction.MakeBid::class, BridgeAction.MakeBid.serializer())
            subclass(BridgeAction.PlayCard::class, BridgeAction.PlayCard.serializer())
            subclass(CheckersAction.Move::class, CheckersAction.Move.serializer())
            subclass(CheckersAction.Capture::class, CheckersAction.Capture.serializer())
            subclass(BlokusAction.Place::class, BlokusAction.Place.serializer())
            subclass(BlokusAction.Pass::class, BlokusAction.Pass.serializer())
            subclass(YahtzeeAction.Roll::class, YahtzeeAction.Roll.serializer())
            subclass(YahtzeeAction.HoldDice::class, YahtzeeAction.HoldDice.serializer())
            subclass(YahtzeeAction.ScoreCategory::class, YahtzeeAction.ScoreCategory.serializer())
            subclass(BattleshipAction.PlaceShip::class, BattleshipAction.PlaceShip.serializer())
            subclass(BattleshipAction.RandomPlaceAll::class, BattleshipAction.RandomPlaceAll.serializer())
            subclass(BattleshipAction.MarkReady::class, BattleshipAction.MarkReady.serializer())
            subclass(BattleshipAction.Shoot::class, BattleshipAction.Shoot.serializer())
            subclass(MatchMonsterAction.SelectPath::class, MatchMonsterAction.SelectPath.serializer())
            subclass(MatchMonsterAction.SwapTiles::class, MatchMonsterAction.SwapTiles.serializer())
            subclass(MatchMonsterAction.ActivateLightning::class, MatchMonsterAction.ActivateLightning.serializer())
            subclass(MatchMonsterAction.ActivateRainbow::class, MatchMonsterAction.ActivateRainbow.serializer())
            subclass(LudoAction.RollDice::class, LudoAction.RollDice.serializer())
            subclass(LudoAction.MovePiece::class, LudoAction.MovePiece.serializer())
            subclass(FarkleAction.RollDice::class, FarkleAction.RollDice.serializer())
            subclass(FarkleAction.SelectDice::class, FarkleAction.SelectDice.serializer())
            subclass(FarkleAction.BankScore::class, FarkleAction.BankScore.serializer())
            subclass(FarkleAction.ContinueHotDice::class, FarkleAction.ContinueHotDice.serializer())
            subclass(MonopolyAction.RollDice::class, MonopolyAction.RollDice.serializer())
            subclass(MonopolyAction.SelectDiceAndMove::class, MonopolyAction.SelectDiceAndMove.serializer())
            subclass(MonopolyAction.BuyProperty::class, MonopolyAction.BuyProperty.serializer())
            subclass(MonopolyAction.PassProperty::class, MonopolyAction.PassProperty.serializer())
            subclass(MonopolyAction.SellProperty::class, MonopolyAction.SellProperty.serializer())
            subclass(MonopolyAction.BuildHouse::class, MonopolyAction.BuildHouse.serializer())
            subclass(MonopolyAction.SellHouse::class, MonopolyAction.SellHouse.serializer())
            subclass(MonopolyAction.TakeLoan::class, MonopolyAction.TakeLoan.serializer())
            subclass(MonopolyAction.MortgageProperty::class, MonopolyAction.MortgageProperty.serializer())
            subclass(MonopolyAction.UnmortgageProperty::class, MonopolyAction.UnmortgageProperty.serializer())
            subclass(MonopolyAction.MakeInvestment::class, MonopolyAction.MakeInvestment.serializer())
            subclass(MonopolyAction.UseTransport::class, MonopolyAction.UseTransport.serializer())
            subclass(MonopolyAction.ProposeTrade::class, MonopolyAction.ProposeTrade.serializer())
            subclass(MonopolyAction.CounterTrade::class, MonopolyAction.CounterTrade.serializer())
            subclass(MonopolyAction.AcceptTrade::class, MonopolyAction.AcceptTrade.serializer())
            subclass(MonopolyAction.RejectTrade::class, MonopolyAction.RejectTrade.serializer())
            subclass(MonopolyAction.UseHelperCard::class, MonopolyAction.UseHelperCard.serializer())
            subclass(MonopolyAction.UseInnateShield::class, MonopolyAction.UseInnateShield.serializer())
            subclass(MonopolyAction.PayJailFine::class, MonopolyAction.PayJailFine.serializer())
            subclass(MonopolyAction.StayInJail::class, MonopolyAction.StayInJail.serializer())
            subclass(MonopolyAction.SkipMission::class, MonopolyAction.SkipMission.serializer())
            subclass(MonopolyAction.SubmitTradeProposal::class, MonopolyAction.SubmitTradeProposal.serializer())
            subclass(MonopolyAction.SelectTradeProposal::class, MonopolyAction.SelectTradeProposal.serializer())
            subclass(MonopolyAction.CancelTrade::class, MonopolyAction.CancelTrade.serializer())
            subclass(MonopolyAction.MakeCounterOffer::class, MonopolyAction.MakeCounterOffer.serializer())
            subclass(MonopolyAction.RollStrategicDice::class, MonopolyAction.RollStrategicDice.serializer())
            subclass(MonopolyAction.SelectStrategicDice::class, MonopolyAction.SelectStrategicDice.serializer())
            subclass(SoccerStrikerAction.Reset::class, SoccerStrikerAction.Reset.serializer())
            subclass(SoccerStrikerAction.SkipTurn::class, SoccerStrikerAction.SkipTurn.serializer())
            subclass(SoccerStrikerAction.SelectDisc::class, SoccerStrikerAction.SelectDisc.serializer())
            subclass(SoccerStrikerAction.FlickDisc::class, SoccerStrikerAction.FlickDisc.serializer())
            subclass(SoccerStrikerAction.AutoFlick::class, SoccerStrikerAction.AutoFlick.serializer())
            subclass(SoccerStrikerAction.AnimationComplete::class, SoccerStrikerAction.AnimationComplete.serializer())
        }

        polymorphic(GameResult::class) {
            subclass(GameResult.Win::class, GameResult.Win.serializer())
            subclass(GameResult.Draw::class, GameResult.Draw.serializer())
        }
        polymorphic(WsMessage::class) {
            subclass(MatchmakingRequestMsg::class, MatchmakingRequestMsg.serializer())
            subclass(SubmitMoveMsg::class, SubmitMoveMsg.serializer())
            subclass(ResumeGameMsg::class, ResumeGameMsg.serializer())
            subclass(SurrenderMsg::class, SurrenderMsg.serializer())
            subclass(ChatMessageMsg::class, ChatMessageMsg.serializer())
            subclass(TimeSyncRequest::class, TimeSyncRequest.serializer())
            subclass(TimeSyncPollResp::class, TimeSyncPollResp.serializer())
            subclass(GameEventMsg::class, GameEventMsg.serializer())
            subclass(MoveResultMsg::class, MoveResultMsg.serializer())
            subclass(GameStateUpdateMsg::class, GameStateUpdateMsg.serializer())
            subclass(GameOverMsg::class, GameOverMsg.serializer())
            subclass(MatchProposalMsg::class, MatchProposalMsg.serializer())
            subclass(PresenceUpdateMsg::class, PresenceUpdateMsg.serializer())
            subclass(TimeSyncResponse::class, TimeSyncResponse.serializer())
            subclass(TimeSyncPoll::class, TimeSyncPoll.serializer())
            subclass(ErrorMsg::class, ErrorMsg.serializer())
        }
        polymorphic(MatchmakingRequest::class) {
            subclass(SoloRequest::class, SoloRequest.serializer())
            subclass(PartyRequest::class, PartyRequest.serializer())
        }
        contextual(PlayerId::class, PlayerIdSerializer)
    }
}

object PlayerIdSerializer : kotlinx.serialization.KSerializer<PlayerId> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("PlayerId", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: PlayerId) {
        encoder.encodeString(value.value)
    }
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): PlayerId {
        return PlayerId(decoder.decodeString())
    }
}