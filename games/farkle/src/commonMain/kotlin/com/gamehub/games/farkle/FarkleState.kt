package com.gamehub.games.farkle

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable
import com.gamehub.shared.dice.DiceEngine
import kotlinx.serialization.Transient

@Serializable
enum class FarkleDiceState {
    IDLE,
    ROLLED,
    SELECTED
}

@Serializable
data class FarkleDice(
    val id: Int,
    val value: Int = 0,
    val state: FarkleDiceState = FarkleDiceState.IDLE
)

@Serializable
data class FarklePlayerStats(
    val playerId: PlayerId,
    val totalScore: Int = 0,
    val hasEnteredGame: Boolean = false
)

@Serializable
data class FarkleState(
    val players: List<PlayerId>,
    val stats: Map<PlayerId, FarklePlayerStats>,
    val currentPlayer: PlayerId?,
    val turnScore: Int = 0,
    val dice: List<FarkleDice>,
    val selectedDiceIds: List<Int> = emptyList(),
    val isFinalRound: Boolean = false,
    val playersWhoHadFinalTurn: List<PlayerId> = emptyList(),
    val targetScore: Int = 10000,
    val entryThreshold: Int = 500,
    @Transient
    val diceEngines: Map<PlayerId, DiceEngine> = emptyMap() // موتور تاس هر بازیکن
) : GameState()
