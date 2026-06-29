// shared/src/commonMain/kotlin/com/gamehub/shared/engine/GameServerContract.kt
package com.gamehub.shared.engine

import com.gamehub.shared.core.*
import kotlinx.serialization.Serializable

/**
 * قرارداد استاندارد برای یک GameServer که روی سرور اجرا می‌شود.
 * هر بازی (Chess, Uno, etc.) از این اینترفیس برای مدیریت چرخه‌ی حیات استفاده می‌کند.
 */
interface GameServerContract<State : GameState, Action : GameAction, Result : GameResult> {

    val gameId: String
    val definition: GameDefinition<State, Action, Result>

    /** بازیکنان حاضر در بازی (شامل ربات‌ها) */
    val players: List<PlayerId>

    /** وضعیت فعلی بازی */
    var currentState: State

    /** ارسال یک Action از طرف یک بازیکن */
    suspend fun submitAction(playerId: PlayerId, action: Action): GameUpdateResult<State, Result>

    /** گرفتن Snapshot از وضعیت فعلی برای ذخیره‌سازی */
    fun takeSnapshot(): GameSnapshot

    /** بازیابی وضعیت از یک Snapshot */
    suspend fun restoreFromSnapshot(snapshot: GameSnapshot): State

    /** بررسی اینکه آیا بازی تمام شده است */
    fun isTerminal(): Boolean = definition.isTerminal(currentState)

    /** گرفتن نتیجه نهایی (در صورت اتمام) */
    fun getResult(): Result? = definition.getResult(currentState)
}

/**
 * یک Snapshot ساده از وضعیت بازی برای ذخیره‌سازی و بازیابی.
 * در آینده می‌توان آن را با Event Sourcing پیشرفته جایگزین کرد.
 */
@Serializable
data class GameSnapshot(
    val gameId: String,
    val gameType: String,
    val stateJson: String,
    val players: List<String>,
    val version: Long = 0,
    // فیلدهای جدید برای Grace Period
    val turnDeadline: Long? = null,           // زمان مطلق پایان نوبت (میلی‌ثانیه)
    val isTurnTimerPaused: Boolean = false,   // آیا تایمر متوقف شده؟
    val pausedRemainingMs: Long = 0,          // زمان باقیمانده هنگام توقف
    val graceUsedCount: Map<String, Int> = emptyMap(), // userId -> تعداد دفعات استفاده از Grace
    val missedTurnsCount: Map<String, Int> = emptyMap(), // userId -> تعداد نوبت‌های از دست رفته
    val lastActivePlayerId: String? = null    // آخرین بازیکنی که نوبت داشت
)