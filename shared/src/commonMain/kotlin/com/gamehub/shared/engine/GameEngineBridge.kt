package com.gamehub.shared.engine

import com.gamehub.shared.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * پل بین GameDefinition (موتور واقعی بازی) و اینترفیس GameEngine
 */
class GameEngineBridge<State : GameState, Action : GameAction, Result : GameResult>(
    private val definition: GameDefinition<State, Action, Result>,
    private val json: Json
) : GameEngine<State, Action, Result> {

    override fun validateAction(state: State, action: Action, playerId: PlayerId): Boolean {
        return definition.validateAction(state, action, playerId)
    }

    override fun applyAction(state: State, action: Action, playerId: PlayerId): GameUpdateResult<State, Result> {
        return definition.applyAction(state, action, playerId)
    }

    @Suppress("UNCHECKED_CAST")
    override fun serializeAction(action: Action): JsonElement {
        // استفاده از serializer پایه GameAction (چون Action زیرنوع آن است)
        return json.encodeToJsonElement(GameAction.serializer(), action as GameAction)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserializeState(serialized: String): State {
        return json.decodeFromString(GameState.serializer(), serialized) as State
    }

    override fun restoreState(state: State) {
        // بدون نیاز
    }

    override fun serializeState(state: State): String {
        return json.encodeToString(GameState.serializer(), state)
    }

    override fun parseToJsonElement(str: String): JsonElement {
        return json.parseToJsonElement(str)
    }

    override fun getConfig(): JsonElement {
        // فعلاً یک آبجکت خالی برمی‌گردانیم
        return json.parseToJsonElement("{}")
    }
}