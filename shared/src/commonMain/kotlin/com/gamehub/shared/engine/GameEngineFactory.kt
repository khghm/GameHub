package com.gamehub.shared.engine

import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameResult
import com.gamehub.shared.core.GameState
import kotlinx.serialization.json.Json

/**
 * کارخانه تولید موتور بازی – از الگوی Registry استفاده می‌کند.
 * ماژول host پس از راه‌اندازی، موتورهای واقعی را ثبت می‌کند.
 */
class GameEngineFactory(private val json: Json) {

    companion object {
        private val registry = mutableMapOf<String, (Json) -> GameEngine<GameState, GameAction, GameResult>>()

        /**
         * ثبت یک موتور بازی جدید (از ماژول host فراخوانی می‌شود)
         */
        fun register(gameType: String, engineProvider: (Json) -> GameEngine<GameState, GameAction, GameResult>) {
            registry[gameType] = engineProvider
        }
    }

    fun create(gameType: String): GameEngine<GameState, GameAction, GameResult> {
        val provider = registry[gameType] ?: throw IllegalArgumentException("Unknown game type: $gameType")
        return provider(json)
    }
}