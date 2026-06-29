package com.gamehub.shared.bot

import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId

/**
 * استراتژی تصمیم‌گیری ربات برای یک بازی خاص
 */
interface BotStrategy<State : GameState, Action : GameAction> {
    val gameId: String
    val supportedDifficultyLevels: IntRange // 1..10

    /**
     * تولید حرکت بعدی ربات بر اساس وضعیت فعلی و سطح دشواری
     * @param state وضعیت فعلی بازی
     * @param botPlayerId شناسه ربات
     * @param difficultyLevel سطح دشواری (1 تا 10)
     * @return حرکت (Action) یا null اگر هیچ حرکت قانونی وجود نداشته باشد
     */
    suspend fun getNextMove(state: State, botPlayerId: PlayerId, difficultyLevel: Int): Action?

    /**
     * آیا ربات باید استیکر بفرستد؟ (اختیاری)
     */
    fun shouldSendSticker(state: State, event: String): Boolean = false

    /**
     * پیام متنی از پیش تعریف شده برای مناسبت خاص
     */
    fun getPredefinedMessage(occasion: String): String? = null
}