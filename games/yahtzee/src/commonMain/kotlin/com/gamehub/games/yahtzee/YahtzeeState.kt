package com.gamehub.games.yahtzee

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable
import com.gamehub.shared.dice.DiceEngine
import kotlinx.serialization.Transient

@Serializable
data class YahtzeePlayerData(
    val playerId: PlayerId,
    val scores: Map<YahtzeeCategory, Int?> = YahtzeeCategory.entries.associateWith { null },
    val upperBonus: Int = 0,
    val yahtzeeBonuses: Int = 0,
    val hasHadYahtzee: Boolean = false
) {
    val totalScore: Int
        get() = scores.values.filterNotNull().sum() + upperBonus + yahtzeeBonuses * 100
}

@Serializable
data class YahtzeeState(
    val players: List<PlayerId>,
    val currentPlayerIndex: Int,
    val dice: List<Int>,
    val heldDice: Set<Int>,
    val rollsRemaining: Int,
    val playerData: Map<PlayerId, YahtzeePlayerData>,
    val gameOver: Boolean = false,
    @Transient
    val diceEngines: Map<PlayerId, DiceEngine> = emptyMap() // موتور تاس هر بازیکن
) : GameState() {
    val currentPlayer: PlayerId?
        get() = if (gameOver) null else players.getOrNull(currentPlayerIndex)

    companion object {
        fun initial(players: List<PlayerId>, diceEngines: Map<PlayerId, DiceEngine> = emptyMap()): YahtzeeState {
            val playerDataMap = players.associateWith {
                YahtzeePlayerData(playerId = it)
            }
            return YahtzeeState(
                players = players,
                currentPlayerIndex = 0,
                dice = listOf(0, 0, 0, 0, 0),
                heldDice = emptySet(),
                rollsRemaining = 3,
                playerData = playerDataMap,
                diceEngines = diceEngines
            )
        }
    }
}
