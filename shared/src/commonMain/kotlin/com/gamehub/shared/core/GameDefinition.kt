package com.gamehub.shared.core

import com.gamehub.shared.engine.GameUpdateResult

interface GameDefinition<State : GameState, Action : GameAction, Result : GameResult> {
    val metadata: GameMetadata
    fun createInitialState(players: List<PlayerId>): State
    fun validateAction(state: State, action: Action, player: PlayerId): Boolean
    fun applyAction(state: State, action: Action, player: PlayerId): GameUpdateResult<State, Result>
    fun isTerminal(state: State): Boolean
    fun getResult(state: State): Result?

    /**
     * رد کردن نوبت بازیکن (به دلیل قطعی یا timeout)
     * پیاده‌سازی پیش‌فرض: نوبت را به بازیکن بعدی منتقل می‌کند.
     * بازی‌ها می‌توانند این متد را override کنند.
     */
    fun skipTurn(state: State, playerId: PlayerId): GameUpdateResult<State, Result> {
        val players = getPlayers(state)
        // اگر تعداد بازیکنان کمتر از 2 است، نوبت را تغییر نده (بازی احتمالاً تمام شده)
        if (players.size < 2) {
            return GameUpdateResult(state, null)
        }
        val currentIdx = players.indexOf(playerId)
        if (currentIdx == -1) {
            // بازیکن در لیست نیست (احتمالاً حذف شده)، نوبت را تغییر نده
            return GameUpdateResult(state, null)
        }
        val nextPlayer = players[(currentIdx + 1) % players.size]
        val newState = setCurrentPlayer(state, nextPlayer)
        return GameUpdateResult(newState, null)
    }

    /**
     * دریافت لیست بازیکنان از وضعیت بازی.
     * بازی‌ها باید این متد را override کنند اگر `skipTurn` پیش‌فرض را استفاده می‌کنند.
     */
    fun getPlayers(state: State): List<PlayerId> = emptyList()

    /**
     * تنظیم بازیکن فعلی در وضعیت بازی.
     * بازی‌ها باید این متد را override کنند اگر `skipTurn` پیش‌فرض را استفاده می‌کنند.
     */
    fun setCurrentPlayer(state: State, playerId: PlayerId): State = state
}