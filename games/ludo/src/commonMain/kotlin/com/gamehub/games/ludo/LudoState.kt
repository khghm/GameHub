// games/ludo/src/commonMain/kotlin/com/gamehub/games/ludo/LudoState.kt
package com.gamehub.games.ludo

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.dice.DiceEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class LudoPiece(
    val id: String,
    val color: String,
    val state: String = "IN_BASE", // IN_BASE, ON_TRACK, HOME_COLUMN, FINISHED
    val pathIndex: Int = -1,
    val homeColumnIndex: Int = -1 // 0-5 for home column, -1 otherwise
)

@Serializable
data class LudoState(
    val players: List<PlayerId>,
    val currentPlayer: PlayerId?,
    val pieces: Map<String, List<LudoPiece>>,
    val diceValue: Int = 0,
    val winner: PlayerId? = null,
    val gameOver: Boolean = false,
    val message: String = "",
    val canRollAgain: Boolean = true,
    val rolloutAvailable: List<Int> = emptyList(),
    val consecutiveSixes: Int = 0,
    @Transient
    val diceEngines: Map<PlayerId, DiceEngine> = emptyMap() // موتور تاس هر بازیکن – transient برای سریال‌سازی
) : GameState()