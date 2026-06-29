// games/ludo/src/commonMain/kotlin/com/gamehub/games/ludo/LudoBotStrategy.kt
package com.gamehub.games.ludo

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlin.random.Random

class LudoBotStrategy : BotStrategy<LudoState, LudoAction> {
    override val gameId: String = "ludo"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: LudoState, botPlayerId: PlayerId, difficultyLevel: Int): LudoAction? {
        val myColor = getBotColor(state, botPlayerId)

        // اگر نوبت ربات است و می‌تواند تاس بزند
        if (state.canRollAgain && state.rolloutAvailable.isEmpty()) {
            return LudoAction.RollDice
        }

        // اگر لیست مهره‌های قابل حرکت وجود دارد
        if (state.rolloutAvailable.isNotEmpty()) {
            val pieces = state.pieces[myColor] ?: return null

            // سطح 1-3: حرکت تصادفی
            if (difficultyLevel <= 3) {
                return LudoAction.MovePiece(state.rolloutAvailable.random())
            }

            // سطح 4-7: اولویت مهره‌هایی که نزدیک به پایان هستند
            val candidates = state.rolloutAvailable.mapNotNull { idx ->
                val piece = pieces[idx]
                val distance = when (piece.state) {
                    "ON_TRACK" -> {
                        val path = LudoBoardData.paths[myColor] ?: return@mapNotNull null
                        path.size - piece.pathIndex
                    }
                    "HOME_COLUMN" -> 5 - piece.homeColumnIndex
                    "IN_BASE" -> 1000
                    else -> 1000
                }
                idx to distance
            }
            if (candidates.isNotEmpty()) {
                // نزدیک‌ترین مهره به پایان را انتخاب کن
                val best = candidates.minByOrNull { it.second }
                if (best != null) return LudoAction.MovePiece(best.first)
            }

            // سطح 8-10: اولویت مهره‌های بیرون از خانه
            val outsidePieces = state.rolloutAvailable.filter { idx ->
                pieces[idx].state != "IN_BASE"
            }
            if (outsidePieces.isNotEmpty()) {
                return LudoAction.MovePiece(outsidePieces.first())
            }
        }

        return null
    }

    private fun getBotColor(state: LudoState, botId: PlayerId): String {
        val index = state.players.indexOf(botId)
        return LudoBoardData.playerColors.getOrElse(index) { "blue" }
    }
}