package com.gamehub.games.baltazar

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * کلاس داده برای ذخیره اطلاعات یک حرکت بالقوه
 */
data class CandidateMove(
    val word: String,
    val cells: List<Pair<Int, Int>>,
    val score: Double,
    val capturesTerritory: Boolean,
    val threatensEnemyBase: Boolean
)

class BaltazarBotStrategy : BotStrategy<BaltazarState, BaltazarAction> {
    override val gameId: String = "baltazar"
    override val supportedDifficultyLevels: IntRange = 1..10

    // داده‌های موقت برای کلمات در حال ساخت
    private val buildingWords = mutableMapOf<PlayerId, List<Pair<Int, Int>>>()
    private val failedAttempts = mutableMapOf<PlayerId, Int>()

    override suspend fun getNextMove(
        state: BaltazarState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): BaltazarAction? {
        if (state.currentPlayer != botPlayerId) return null

        val delayMs = when {
            difficultyLevel <= 3 -> 40L
            difficultyLevel <= 6 -> 60L
            else -> 80L
        }
        delay(delayMs)

        val currentSelected = state.selectedCells
        
        // اگر در حال ساخت یک کلمه هستیم، ادامه دهیم
        if (currentSelected.isNotEmpty()) {
            return continueBuildingWord(state, botPlayerId, currentSelected, difficultyLevel)
        }

        // پاک کردن اطلاعات قبلی
        buildingWords.remove(botPlayerId)
        failedAttempts.remove(botPlayerId)

        // پیدا کردن بهترین حرکت ممکن
        val bestMove = findBestMove(state, botPlayerId, difficultyLevel)
        
        return when {
            bestMove != null -> {
                // اولین خانه کلمه را انتخاب کنیم
                buildingWords[botPlayerId] = bestMove.cells
                BaltazarAction.SelectCell(bestMove.cells[0].first, bestMove.cells[0].second)
            }
            else -> {
                // کلمه‌ای پیدا نشد، یک خانه تصادفی انتخاب کنیم
                findRandomNearTerritory(state, botPlayerId)
            }
        }
    }

    /**
     * ادامه ساخت کلمه‌ای که شروع شده است
     */
    private suspend fun continueBuildingWord(
        state: BaltazarState,
        botPlayerId: PlayerId,
        currentSelected: List<Pair<Int, Int>>,
        difficultyLevel: Int
    ): BaltazarAction {
        val currentWord = currentSelected
            .mapNotNull { (r, c) -> state.getCell(r, c)?.letter }
            .joinToString("")

        // اگر کلمه کامل و معتبر است، ثبت کنیم
        if (currentWord.length >= 3 && BALTAR_VOCAB.contains(currentWord)) {
            buildingWords.remove(botPlayerId)
            failedAttempts.remove(botPlayerId)
            return BaltazarAction.SubmitWord
        }

        // اگر قبلا یک کلمه هدف انتخاب کرده‌ایم، آن را دنبال کنیم
        val targetWord = buildingWords[botPlayerId]
        if (targetWord != null && targetWord.size > currentSelected.size) {
            val nextCell = targetWord[currentSelected.size]
            
            // بررسی اینکه آیا خانه بعدی همچنان در دسترس است
            val cell = state.getCell(nextCell.first, nextCell.second)
            if (cell != null && cell.state == CellState.OpenNeutral) {
                return BaltazarAction.SelectCell(nextCell.first, nextCell.second)
            }
        }

        // اگر نتوانستیم ادامه کلمه را پیدا کنیم، باقی تلاش کنیم
        val attempts = failedAttempts.getOrDefault(botPlayerId, 0) + 1
        failedAttempts[botPlayerId] = attempts

        // اگر زیاد تلاش کردیم و نتیجه نگرفتیم، کلمه را پاک و دوباره شروع کنیم
        if (attempts > 2) {
            return if (currentSelected.size > 1) {
                BaltazarAction.DeselectLast
            } else {
                // اگر فقط یک خانه انتخاب شده، یک کلمه جدید پیدا کنیم
                buildingWords.remove(botPlayerId)
                failedAttempts.remove(botPlayerId)
                
                val openCells = state.board
                    .filter { 
                        it.state == CellState.OpenNeutral && !currentSelected.contains(it.row to it.col) }
                if (openCells.isNotEmpty()) {
                    val randomCell = openCells.random(Random)
                    buildingWords[botPlayerId] = listOf(randomCell.row to randomCell.col)
                    BaltazarAction.DeselectLast
                } else {
                    BaltazarAction.DeselectLast
                }
            }
        }

        // در غیر این صورت، یک حرف جدید برای ادامه پیدا کنیم
        val openCells = state.board.filter { 
            it.state == CellState.OpenNeutral && !currentSelected.contains(it.row to it.col) }
        
        for (cell in openCells) {
            val testWord = currentWord + cell.letter
            if (BALTAR_VOCAB.any { it.startsWith(testWord) }) {
                return BaltazarAction.SelectCell(cell.row, cell.col)
            }
        }

        // اگر نتوانستیم ادامه دهیم و کلمه فعلی معتبر است، ثبت کنیم
        if (currentWord.length >= 3 && BALTAR_VOCAB.contains(currentWord)) {
            buildingWords.remove(botPlayerId)
            failedAttempts.remove(botPlayerId)
            return BaltazarAction.SubmitWord
        }

        // در غیر این صورت، آخرین انتخاب را حذف کنیم
        return BaltazarAction.DeselectLast
    }

    /**
     * پیدا کردن بهترین حرکت ممکن با استفاده از ارزیابی Heuristic
     */
    private fun findBestMove(
        state: BaltazarState,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): CandidateMove? {
        val openCells = state.board
            .filter { it.state == CellState.OpenNeutral }
            .map { it.row to it.col to it.letter!! }
            .toMap()

        val territory = state.getTerritory(botPlayerId).toSet()
        val opponentId = if (botPlayerId == state.players[0]) state.players[1] else state.players[0]
        val opponentHome = if (botPlayerId == state.players[0]) (6 to 5) else (0 to 1)

        val candidates = mutableListOf<CandidateMove>()

        // پیدا کردن تمام کلمات معتبر ممکن
        for (word in BALTAR_VOCAB) {
            if (word.length < 3) continue

            val cellSequences = findCellSequencesForWord(word, openCells)
            
            for (cells in cellSequences) {
                val (score, captures, threatens) = evaluateMove(
                    state, 
                    cells, 
                    territory, 
                    opponentHome, 
                    botPlayerId, 
                    difficultyLevel
                )
                
                candidates.add(CandidateMove(
                    word = word,
                    cells = cells,
                    score = score,
                    capturesTerritory = captures,
                    threatensEnemyBase = threatens
                ))
            }
        }

        if (candidates.isEmpty()) return null

        // مرتب‌سازی بر اساس امتیاز
        candidates.sortByDescending { it.score }

        // انتخاب بهترین حرکت با احتمال مناسب
        val selectionRange = when {
            difficultyLevel <= 3 -> minOf(5, candidates.size)
            difficultyLevel <= 6 -> minOf(3, candidates.size)
            else -> 1
        }

        val selectedIndex = Random.nextInt(selectionRange)
        return candidates[selectedIndex]
    }

    /**
     * پیدا کردن دنباله‌های سلولی برای یک کلمه
     */
    private fun findCellSequencesForWord(
        word: String,
        openCells: Map<Pair<Int, Int>, Char>
    ): List<List<Pair<Int, Int>>> {
        val results = mutableListOf<List<Pair<Int, Int>>>()
        
        fun backtrack(
            remainingWord: String,
            usedCells: Set<Pair<Int, Int>>,
            currentSequence: List<Pair<Int, Int>>
        ) {
            if (remainingWord.isEmpty()) {
                results.add(currentSequence)
                return
            }

            val targetChar = remainingWord.first()
            val availableCells = openCells.entries
                .filter { it.value == targetChar && !usedCells.contains(it.key) }

            for ((cell, _) in availableCells) {
                backtrack(
                    remainingWord.substring(1),
                    usedCells + cell,
                    currentSequence + cell
                )
            }
        }

        backtrack(word, emptySet(), emptyList())
        return results
    }

    /**
     * ارزیابی یک حرکت با استفاده از Heuristic
     */
    private fun evaluateMove(
        state: BaltazarState,
        cells: List<Pair<Int, Int>>,
        initialTerritory: Set<Pair<Int, Int>>,
        opponentHome: Pair<Int, Int>,
        botPlayerId: PlayerId,
        difficultyLevel: Int
    ): Triple<Double, Boolean, Boolean> {
        var territory = initialTerritory.toMutableSet()
        var capturesAny = false
        var threatensEnemyBase = false

        // شبیه‌سازی تصرف
        for ((r, c) in cells) {
            val cell = state.getCell(r, c) ?: continue
            
            val isAdjacent = state.getValidNeighbors(r, c).any { territory.contains(it) }
            
            if (isAdjacent) {
                territory.add(r to c)
                capturesAny = true

                // بررسی تهدید خانه اصلی حریف
                val neighbors = state.getValidNeighbors(r, c)
                if (neighbors.contains(opponentHome)) {
                    threatensEnemyBase = true
                }
            }
        }

        // محاسبه امتیاز
        var score = 0.0

        // وزن‌ها بر اساس سطح سختی
        val weights = when {
            difficultyLevel <= 3 -> mapOf(
                "territory" to 1.0,
                "distance" to 0.2,
                "threat" to 2.0,
                "wordLength" to 0.5
            )
            difficultyLevel <= 6 -> mapOf(
                "territory" to 2.0,
                "distance" to 0.5,
                "threat" to 4.0,
                "wordLength" to 0.3
            )
            else -> mapOf(
                "territory" to 3.0,
                "distance" to 1.0,
                "threat" to 10.0,
                "wordLength" to 0.2
            )
        }

        // 1. امتیاز تعداد خانه‌های تصرف‌شده
        val territoryGain = territory.size - initialTerritory.size
        score += territoryGain * weights["territory"]!!

        // 2. تهدید خانه اصلی حریف (بسیار مهم)
        if (threatensEnemyBase) {
            score += weights["threat"]!!
        }

        // 3. فاصله تا خانه اصلی حریف (هرچه کمتر بهتر)
        val avgDistance = calculateAverageDistance(territory, opponentHome, state)
        score -= avgDistance * weights["distance"]!!

        // 4. طول کلمه (کمتر اولویت‌دار)
        score += cells.size * weights["wordLength"]!!

        // 5. اولویت به حرکت‌هایی که قلمرو می‌گیرند
        if (!capturesAny) {
            score *= 0.1  // بسیار کم‌اهمیت می‌شود
        }

        return Triple(score, capturesAny, threatensEnemyBase)
    }

    /**
     * محاسبه متوسط فاصله از قلمرو تا خانه اصلی حریف
     */
    private fun calculateAverageDistance(
        territory: Set<Pair<Int, Int>>,
        target: Pair<Int, Int>,
        state: BaltazarState
    ): Double {
        if (territory.isEmpty()) return Double.MAX_VALUE

        var totalDistance = 0.0
        for (cell in territory) {
            totalDistance += calculateDistance(cell, target, state)
        }
        return totalDistance / territory.size
    }

    /**
     * محاسبه فاصله بین دو خانه با BFS
     */
    private fun calculateDistance(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        state: BaltazarState
    ): Int {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Pair<Int, Int>, Int>>()
        queue.add(from to 0)
        visited.add(from)

        while (queue.isNotEmpty()) {
            val (current, distance) = queue.removeFirst()
            if (current == to) return distance

            for (neighbor in state.getValidNeighbors(current.first, current.second)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor)
                    queue.add(neighbor to distance + 1)
                }
            }
        }
        return Int.MAX_VALUE
    }

    /**
     * انتخاب یک خانه تصادفی نزدیک قلمرو
     */
    private fun findRandomNearTerritory(
        state: BaltazarState,
        botPlayerId: PlayerId
    ): BaltazarAction? {
        val territory = state.getTerritory(botPlayerId).toSet()
        val openCells = state.board
            .filter { it.state == CellState.OpenNeutral }
        
        val nearTerritory = openCells.filter { cell ->
            state.getValidNeighbors(cell.row, cell.col).any { territory.contains(it) }
        }

        val selectedCell = if (nearTerritory.isNotEmpty()) {
            nearTerritory.random(Random)
        } else if (openCells.isNotEmpty()) {
            openCells.random(Random)
        } else {
            return null
        }

        return BaltazarAction.SelectCell(selectedCell.row, selectedCell.col)
    }
}
