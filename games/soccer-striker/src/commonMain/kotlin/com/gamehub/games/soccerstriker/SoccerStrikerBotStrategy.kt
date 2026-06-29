package com.gamehub.games.soccerstriker

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlin.math.*
import kotlin.random.Random

class SoccerStrikerBotStrategy : BotStrategy<SoccerStrikerState, SoccerStrikerAction> {
    override val gameId: String = "soccer-striker"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(
        state: SoccerStrikerState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): SoccerStrikerAction? {
        val botPlayerIndex = state.players.indexOf(botPlayerId)
        val botTeam = if (botPlayerIndex == 0) "red" else "blue"
        val myDiscs = state.discs.filter { it.team == botTeam }

        if (myDiscs.isEmpty()) return SoccerStrikerAction.SkipTurn

        val goalCenter = if (botTeam == "red") {
            Pair(SoccerStrikerState.FIELD_WIDTH / 2, SoccerStrikerState.FIELD_HEIGHT)
        } else {
            Pair(SoccerStrikerState.FIELD_WIDTH / 2, 0f)
        }

        var bestMove: SoccerStrikerAction? = null
        var bestScore = Float.MIN_VALUE

        for (disc in myDiscs) {
            val (angle, power) = calculateBestAngleAndPower(disc, state.ball, goalCenter, difficultyLevel)
            val score = calculateMoveScore(disc, state.ball, goalCenter, angle, power, difficultyLevel)
            if (score > bestScore) {
                bestScore = score
                bestMove = SoccerStrikerAction.AutoFlick(disc.id, angle, power)
            }
        }

        if (bestMove == null) {
            val randomDisc = myDiscs.random()
            val randomAngle = Random.nextFloat() * 2 * PI.toFloat()
            val randomPower = 0.3f + Random.nextFloat() * 0.7f
            bestMove = SoccerStrikerAction.AutoFlick(randomDisc.id, randomAngle, randomPower)
        }

        return bestMove
    }

    private fun calculateBestAngleAndPower(
        disc: DiscData,
        ball: BallData,
        goalCenter: Pair<Float, Float>,
        difficultyLevel: Int
    ): Pair<Float, Float> {
        // بردار از دیسک به توپ
        val discToBallDx = ball.x - disc.x
        val discToBallDy = ball.y - disc.y
        val distanceToBall = sqrt(discToBallDx * discToBallDx + discToBallDy * discToBallDy)

        // اگر توپ خیلی دور است، به سمت توپ برو
        if (distanceToBall > 250f) {
            val angle = atan2(discToBallDy, discToBallDx)
            return Pair(angle, 0.6f)
        }

        // محاسبه جهت ضربه به سمت دروازه (مانند HTML)
        val ballToGoalDx = goalCenter.first - ball.x
        val ballToGoalDy = goalCenter.second - ball.y
        // موقعیت هدف برای دیسک: پشت توپ نسبت به دروازه
        val targetDiscPositionX = ball.x - ballToGoalDx * 0.1f
        val targetDiscPositionY = ball.y - ballToGoalDy * 0.1f
        val moveDx = targetDiscPositionX - disc.x
        val moveDy = targetDiscPositionY - disc.y
        val angle = atan2(moveDy, moveDx)

        // توان بر اساس difficulty و فاصله
        val power = when {
            difficultyLevel <= 2 -> 0.2f + Random.nextFloat() * 0.5f
            difficultyLevel <= 5 -> 0.4f + Random.nextFloat() * 0.4f
            difficultyLevel <= 8 -> 0.6f + Random.nextFloat() * 0.3f
            else -> 0.7f + Random.nextFloat() * 0.3f
        }

        return Pair(angle, power)
    }

    private fun calculateMoveScore(
        disc: DiscData,
        ball: BallData,
        goalCenter: Pair<Float, Float>,
        angle: Float,
        power: Float,
        difficultyLevel: Int
    ): Float {
        var score = 0f

        // نزدیکی به توپ
        val dx = ball.x - disc.x
        val dy = ball.y - disc.y
        val distanceToBall = sqrt(dx*dx + dy*dy)
        score -= distanceToBall * 0.01f

        // هم‌راستایی با دروازه
        val ballToGoalDx = goalCenter.first - ball.x
        val ballToGoalDy = goalCenter.second - ball.y
        val discToBallAngle = atan2(dy, dx)
        val ballToGoalAngle = atan2(ballToGoalDy, ballToGoalDx)
        val alignmentDiff = abs(discToBallAngle - (ballToGoalAngle + PI.toFloat()))
        score -= alignmentDiff * 5f

        // وزن difficulty
        score *= (1f + difficultyLevel * 0.05f)

        return score
    }
}