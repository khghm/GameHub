package com.gamehub.games.soccerstriker

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class DiscData(
    val id: String,
    val team: String, // "red" or "blue"
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val angle: Float = 0f,
    val angularVelocity: Float = 0f
)

@Serializable
data class BallData(
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f
)

@Serializable
data class SoccerStrikerState(
    val players: List<PlayerId>,
    val currentPlayerIndex: Int,
    val scoreRed: Int = 0,
    val scoreBlue: Int = 0,
    val discs: List<DiscData> = emptyList(),
    val ball: BallData = BallData(0f, 0f),
    val selectedDiscId: String? = null,
    val isSimulating: Boolean = false,
    val gameOver: Boolean = false,
    val winner: PlayerId? = null,
    val isKickOff: Boolean = true,
    val physicsConfig: PhysicsConfig = PhysicsConfig(),
    val animationFrames: List<AnimationFrame> = emptyList(),
    val currentAnimationFrame: Int = 0
) : GameState() {

    val currentPlayer: PlayerId?
        get() = if (gameOver) null else players.getOrNull(currentPlayerIndex)

    companion object {
        const val FIELD_WIDTH = 800f
        const val FIELD_HEIGHT = 1422f
        const val DISC_RADIUS = 38f
        const val BALL_RADIUS = 25f
        const val GOAL_WIDTH = 150f
        const val GOAL_DEPTH = 20f
        const val WIN_SCORE = 5  // تغییر از ۳ به ۵ (همانند HTML)

        fun initial(players: List<PlayerId>): SoccerStrikerState {
            val redDiscs = listOf(
                DiscData("red-1", "red", FIELD_WIDTH / 2, 200f - 100f),
                DiscData("red-2", "red", FIELD_WIDTH / 2 - 150f, 200f),
                DiscData("red-3", "red", FIELD_WIDTH / 2 + 150f, 200f),
                DiscData("red-4", "red", FIELD_WIDTH / 2 - 100f, 200f + 100f),
                DiscData("red-5", "red", FIELD_WIDTH / 2 + 100f, 200f + 100f)
            )
            val blueDiscs = listOf(
                DiscData("blue-1", "blue", FIELD_WIDTH / 2, FIELD_HEIGHT - 200f + 100f),
                DiscData("blue-2", "blue", FIELD_WIDTH / 2 - 150f, FIELD_HEIGHT - 200f),
                DiscData("blue-3", "blue", FIELD_WIDTH / 2 + 150f, FIELD_HEIGHT - 200f),
                DiscData("blue-4", "blue", FIELD_WIDTH / 2 - 100f, FIELD_HEIGHT - 200f - 100f),
                DiscData("blue-5", "blue", FIELD_WIDTH / 2 + 100f, FIELD_HEIGHT - 200f - 100f)
            )
            return SoccerStrikerState(
                players = players,
                currentPlayerIndex = 0,
                discs = redDiscs + blueDiscs,
                ball = BallData(FIELD_WIDTH / 2, FIELD_HEIGHT / 2)
            )
        }
    }
}

@Serializable
data class AnimationFrame(
    val discs: List<DiscData>,
    val ball: BallData
)

@Serializable
data class PhysicsConfig(
    // ======== تنظیمات واقعی برای فوتبال دستی ========
    val discRestitution: Float = 0.6f,        // برخوردهای کمی نرم‌تر
    val discFriction: Float = 0.5f,           // اصطکاک سطح میز (واقعی‌تر)
    val discDensity: Float = 1.2f,            // جرم بیشتر برای احساس محکم
    val discLinearDamping: Float = 0.8f,      // میرایی مناسب (مانند میز)
    val discAngularDamping: Float = 0.5f,
    val ballRestitution: Float = 0.55f,
    val ballFriction: Float = 0.4f,
    val ballDensity: Float = 0.7f,
    val ballLinearDamping: Float = 0.7f,
    val ballAngularDamping: Float = 0.4f,
    val maxSpeed: Float = 1800f,              // سرعت معقول‌تر
    val flickPowerFactor: Float = 1800f       // افزایش قدرت ضربه
)