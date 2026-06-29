// shared/src/commonMain/kotlin/com/gamehub/shared/engine/GameEngine.kt
package com.gamehub.shared.engine

import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.json.JsonElement

interface GameEngine<S : GameState, A : GameAction, R : GameResult> {
    fun validateAction(state: S, action: A, playerId: PlayerId): Boolean
    fun applyAction(state: S, action: A, playerId: PlayerId): GameUpdateResult<S, R>
    fun serializeAction(action: A): JsonElement
    fun deserializeState(serialized: String): S
    fun restoreState(state: S)
    fun serializeState(state: S): String
    fun parseToJsonElement(str: String): JsonElement
    fun getConfig(): JsonElement
}

data class GameUpdateResult<S : GameState, R : GameResult>(
    val newState: S,
    val result: R? = null,
    val events: List<JsonElement> = emptyList()
)