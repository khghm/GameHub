// shared/src/commonMain/kotlin/com/gamehub/shared/dice/DiceEngine.kt
package com.gamehub.shared.dice

import kotlin.math.abs
import kotlin.random.Random

/**
 * موتور تاس هوشمند «ارکستر نامرئی جمعی»
 * پشتیبانی از تعداد تاس پویا (۱ تا ۶)، کمک خشکسالی، تزریق تصادف خالص، تعادل بلندمدت، و کمک عقب‌ماندگی.
 * هر بازیکن یک نمونه مستقل از این کلاس دارد.
 */
class DiceEngine(
    private val sessionProfile: DiceSessionProfile,
    private val playerId: String,
    private val rngSeed: Long = Random.nextLong()
) {
    // ========================== PRNG پایه (Xorshift) ==========================
    private var state = rngSeed xor playerId.hashCode().toLong()
    private fun nextInt(bound: Int): Int {
        state = state xor (state shl 13)
        state = state xor (state shr 7)
        state = state xor (state shl 17)
        return ((state and 0x7FFFFFFF) % bound).toInt()
    }

    // PRNG تزریق (کاملاً مجزا)
    private var injectionState = rngSeed xor 0x9E3779B97F4A7C15UL.toLong()
    private fun injectionNextInt(): Int {
        injectionState = injectionState xor (injectionState shl 13)
        injectionState = injectionState xor (injectionState shr 7)
        injectionState = injectionState xor (injectionState shl 17)
        return ((injectionState and 0x7FFFFFFF) % 6).toInt()
    }

    // ========================== وضعیت جاری ==========================
    private var droughtCounter = 0
    private var totalDiceProduced = 0L
    private var doubleRollCount = 0L
    private var lastPairCheckRoll = 0L
    private var currentPairBoostProbability = sessionProfile.pairBoostProbability
    private val biases = DoubleArray(7) { 1.0 / 6.0 }
    private val numberCounts = LongArray(7)

    private var lastComebackCheckTurn = 0
    private var boostedInjectionUntilTurn = -1
    private var originalInjectionRate = sessionProfile.injectionRate

    // بازخورد از بازی (باید قبل از هر پرتاب توسط بازی تنظیم شود)
    var lastMovePossible: Boolean = true
    var lastTotalProgress: Double = 0.0
    var lastActivePieces: Int = 0
    var lastCurrentScore: Int = 0
    var lastGameCompletionRatio: Double = 0.0

    fun roll(diceCount: Int, requireDistinct: Boolean = false): List<Int> {
        var finalCount = diceCount.coerceIn(1, sessionProfile.maxDice)
        if (requireDistinct && finalCount > 6) finalCount = 6

        totalDiceProduced += finalCount
        if (finalCount == 2) doubleRollCount++

        updateDroughtCounter()

        val totalTurns = (totalDiceProduced / sessionProfile.defaultDiceCount).toInt()
        if (totalTurns - lastComebackCheckTurn >= 20) {
            applyComebackBoost()
            lastComebackCheckTurn = totalTurns
        }

        val result = if (requireDistinct) {
            rollDistinct(finalCount)
        } else {
            rollNormal(finalCount)
        }

        result.forEach { numberCounts[it]++ }

        if (totalDiceProduced % 20 == 0L && Random.nextDouble() < 1.0 / sessionProfile.balancerCheckMeanInterval) {
            adjustBiases()
        }

        if (finalCount == 2) {
            val rollsSinceLastCheck = doubleRollCount - lastPairCheckRoll
            if (rollsSinceLastCheck in sessionProfile.pairCheckIntervalMin..sessionProfile.pairCheckIntervalMax) {
                adjustPairBoost()
                lastPairCheckRoll = doubleRollCount
            }
        }

        return result
    }

    fun feedback(
        movePossible: Boolean,
        totalProgress: Double = 0.0,
        activePieces: Int = 0,
        currentScore: Int = 0,
        gameCompletionRatio: Double = 0.0
    ) {
        lastMovePossible = movePossible
        lastTotalProgress = totalProgress
        lastActivePieces = activePieces
        lastCurrentScore = currentScore
        lastGameCompletionRatio = gameCompletionRatio
    }

    private fun updateDroughtCounter() {
        if (lastMovePossible) {
            droughtCounter = 0
        } else {
            droughtCounter++
        }
    }

    private fun applyComebackBoost() {
        if (sessionProfile.comebackMode == "none") return
        if (lastGameCompletionRatio < sessionProfile.comebackDelay) return

        val ratio = when (sessionProfile.comebackMode) {
            "progress" -> if (lastTotalProgress > 0) 1.0 else 1.0
            "score" -> if (lastCurrentScore > 0) 1.0 else 1.0
            else -> 1.0
        }
        val threshold = when (sessionProfile.comebackMode) {
            "progress" -> sessionProfile.progressComebackThreshold
            "score" -> sessionProfile.scoreComebackThreshold
            else -> 0.5
        }
        if (ratio < threshold) {
            originalInjectionRate = sessionProfile.injectionRate
            boostedInjectionUntilTurn = (totalDiceProduced / sessionProfile.defaultDiceCount).toInt() + 20
        }
    }

    private fun rollDistinct(count: Int): List<Int> {
        val pool = mutableListOf(1, 2, 3, 4, 5, 6)
        val result = mutableListOf<Int>()
        repeat(count) {
            val idx = nextInt(pool.size)
            result.add(pool[idx])
            pool.removeAt(idx)
        }
        return result
    }

    private fun rollNormal(count: Int): List<Int> {
        if (count == 2 && currentPairBoostProbability > 0.0 && Random.nextDouble() < currentPairBoostProbability) {
            val outcome = Random.nextDouble()
            when {
                outcome < 0.5 -> {
                    val pairVal = 1 + nextInt(6)
                    return listOf(pairVal, pairVal)
                }
                outcome < 0.8 -> {
                    val v1 = 1 + nextInt(6)
                    var v2 = v1 + if (Random.nextBoolean()) 1 else -1
                    if (v2 < 1) v2 = 2
                    if (v2 > 6) v2 = 5
                    return listOf(v1, v2)
                }
            }
        }

        val currentTurn = (totalDiceProduced / sessionProfile.defaultDiceCount).toInt()
        val effectiveInjectionRate = if (boostedInjectionUntilTurn > currentTurn) {
            originalInjectionRate + sessionProfile.comebackInjectionBoost
        } else {
            originalInjectionRate
        }
        if (Random.nextDouble() < effectiveInjectionRate) {
            return List(count) { 1 + injectionNextInt() }
        }

        if (droughtCounter >= sessionProfile.droughtThreshold && Random.nextDouble() < sessionProfile.varianceBoostProbability) {
            droughtCounter = 0
            return rollHighVariance(count)
        }

        return List(count) { biasedNextInt() }
    }

    private fun rollHighVariance(count: Int): List<Int> {
        val types = arrayOf("A", "B", "C", "D")
        val selectedType = types[Random.nextInt(types.size)]
        val result = mutableListOf<Int>()
        val highVarCount = minOf(2, count)
        repeat(highVarCount) {
            result.add(sampleHighVariance(selectedType))
        }
        repeat(count - highVarCount) {
            result.add(biasedNextInt())
        }
        return result
    }

    private fun sampleHighVariance(type: String): Int {
        val r = Random.nextDouble()
        return when (type) {
            "A" -> when {
                r < 2.0/9 -> 1
                r < 4.0/9 -> 6
                r < 5.0/9 -> 2
                r < 6.0/9 -> 3
                r < 7.0/9 -> 4
                else -> 5
            }
            "B" -> if (r < 1.0/3) 6 else (1 + (r - 1.0/3) / (2.0/3) * 5).toInt().coerceIn(1, 5)
            "C" -> when {
                r < 2.0/9 -> 5
                r < 4.0/9 -> 6
                r < 5.0/9 -> 1
                r < 6.0/9 -> 2
                r < 7.0/9 -> 3
                else -> 4
            }
            else -> {
                val epsilon = Random.nextDouble(-0.03, 0.03)
                val p1 = 0.15 + epsilon
                val p6 = 0.15 - epsilon
                when {
                    r < p1 -> 1
                    r < p1 + p6 -> 6
                    else -> 1 + nextInt(6)
                }
            }
        }
    }

    private fun biasedNextInt(): Int {
        val r = Random.nextDouble()
        var cum = 0.0
        for (i in 1..6) {
            cum += biases[i]
            if (r < cum) return i
        }
        return 1
    }

    private fun adjustBiases() {
        val expected = totalDiceProduced.toDouble() / 6.0
        var maxDev = 0.0
        var maxNum = 1
        for (i in 1..6) {
            val dev = abs(numberCounts[i] - expected) / expected
            if (dev > maxDev) {
                maxDev = dev
                maxNum = i
            }
        }
        if (maxDev > sessionProfile.balancerThreshold) {
            val correction = minOf(0.005, maxDev / 500.0)
            for (i in 1..6) {
                biases[i] += if (i == maxNum) -correction / 5.0 else correction / 5.0
            }
            val sum = biases.sum()
            for (i in 1..6) biases[i] /= sum
        }
    }

    private fun adjustPairBoost() {
        // در صورت نیاز می‌توان پیاده‌سازی کرد – فعلاً غیرفعال
    }
}

data class DiceSessionProfile(
    val maxDice: Int,
    val defaultDiceCount: Int,
    val droughtThreshold: Int,
    val varianceBoostProbability: Double,
    val injectionRate: Double,
    val balancerCheckMeanInterval: Double,
    val balancerThreshold: Double,
    val pairCheckIntervalMin: Int,
    val pairCheckIntervalMax: Int,
    val pairRateUpperThreshold: Double,
    val comebackInjectionBoost: Double,
    val comebackMode: String,
    val progressComebackThreshold: Double,
    val piecesComebackThreshold: Double,
    val scoreComebackThreshold: Double,
    val comebackDelay: Double,
    val pairBoostProbability: Double
)

private data class ProfileRanges(
    val drought: IntRange,
    val varBoost: ClosedFloatingPointRange<Double>,
    val inject: ClosedFloatingPointRange<Double>,
    val balancerInterval: ClosedFloatingPointRange<Double>,
    val balancerThresh: ClosedFloatingPointRange<Double>,
    val comebackThreshold: ClosedFloatingPointRange<Double>,
    val comebackDelay: Double,
    val pairBoost: Double
)

fun createProfileForGame(gameId: String, isRanked: Boolean = false): DiceSessionProfile {
    val rand = Random
    val ranges = when (gameId) {
        "ludo" -> ProfileRanges(
            drought = 4..8, varBoost = 0.20..0.35, inject = 0.04..0.10,
            balancerInterval = 250.0..400.0, balancerThresh = 2.5..3.5,
            comebackThreshold = 0.45..0.55, comebackDelay = 0.0, pairBoost = 0.03
        )
        "backgammon" -> ProfileRanges(
            drought = 5..8, varBoost = 0.15..0.25, inject = 0.08..0.15,
            balancerInterval = 250.0..400.0, balancerThresh = 2.8..3.8,
            comebackThreshold = 1.2..1.8, comebackDelay = 0.0, pairBoost = 0.03
        )
        "yahtzee" -> ProfileRanges(
            drought = 10..10, varBoost = 0.0..0.0, inject = 0.05..0.10,
            balancerInterval = 350.0..550.0, balancerThresh = 2.5..3.5,
            comebackThreshold = 0.45..0.55, comebackDelay = 0.3, pairBoost = 0.0
        )
        "farkle" -> ProfileRanges(
            drought = 10..10, varBoost = 0.0..0.0, inject = 0.10..0.20,
            balancerInterval = 350.0..550.0, balancerThresh = 2.5..3.5,
            comebackThreshold = 0.45..0.55, comebackDelay = 0.25, pairBoost = 0.0
        )
        "monopoly" -> ProfileRanges(
            drought = 10..10, varBoost = 0.0..0.0, inject = 0.10..0.20,
            balancerInterval = 350.0..550.0, balancerThresh = 2.5..3.5,
            comebackThreshold = 0.45..0.55, comebackDelay = 0.2, pairBoost = 0.02
        )
        else -> throw IllegalArgumentException("بازی $gameId پشتیبانی نمی‌شود")
    }

    fun safeNextDouble(range: ClosedFloatingPointRange<Double>): Double {
        return if (range.start == range.endInclusive) range.start
        else rand.nextDouble(range.start, range.endInclusive)
    }

    fun safeNextInt(range: IntRange): Int {
        return if (range.start == range.endInclusive) range.start
        else rand.nextInt(range.start, range.endInclusive + 1)
    }

    return DiceSessionProfile(
        maxDice = when (gameId) {
            "yahtzee" -> 5
            "farkle" -> 6
            else -> 2
        },
        defaultDiceCount = when (gameId) {
            "yahtzee" -> 5
            "farkle" -> 6
            else -> 2
        },
        droughtThreshold = safeNextInt(ranges.drought),
        varianceBoostProbability = safeNextDouble(ranges.varBoost),
        injectionRate = safeNextDouble(ranges.inject),
        balancerCheckMeanInterval = safeNextDouble(ranges.balancerInterval),
        balancerThreshold = safeNextDouble(ranges.balancerThresh),
        pairCheckIntervalMin = 55,
        pairCheckIntervalMax = 85,
        pairRateUpperThreshold = if (gameId == "backgammon") 0.175 else 0.178,
        comebackInjectionBoost = rand.nextDouble(0.01, 0.03),
        comebackMode = when (gameId) {
            "ludo" -> "progress"
            "backgammon" -> "piecesAdvantage"
            "yahtzee", "farkle" -> "score"
            "monopoly" -> "hybrid"
            else -> "none"
        },
        progressComebackThreshold = if (gameId == "ludo") safeNextDouble(ranges.comebackThreshold) else 0.5,
        piecesComebackThreshold = if (gameId == "backgammon") safeNextDouble(ranges.comebackThreshold) else 1.5,
        scoreComebackThreshold = if (gameId in listOf("yahtzee", "farkle")) safeNextDouble(ranges.comebackThreshold) else 0.5,
        comebackDelay = ranges.comebackDelay,
        pairBoostProbability = ranges.pairBoost
    )
}