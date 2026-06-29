package com.gamehub.shared.core

data class GameUpdateResult<State : GameState, Result : GameResult>(
    val newState: State,
    val result: Result?
)