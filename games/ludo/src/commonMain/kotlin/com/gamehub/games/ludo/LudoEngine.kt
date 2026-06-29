package com.gamehub.games.ludo

import com.gamehub.shared.core.*
import kotlinx.serialization.Serializable
import kotlin.random.Random
import com.gamehub.shared.engine.GameUpdateResult
import com.gamehub.shared.dice.DiceEngine
import com.gamehub.shared.dice.createProfileForGame

@Serializable
sealed class LudoAction : GameAction() {
    @Serializable data object RollDice : LudoAction()
    @Serializable data class MovePiece(val pieceIndex: Int) : LudoAction()
}

class LudoEngine : GameDefinition<LudoState, LudoAction, GameResult> {

    override val metadata = GameMetadata(
        id = "ludo", name = "منچ (Ludo)", minPlayers = 2, maxPlayers = 4,
        description = "بازی رومیزی کلاسیک ایرانی"
    )

    private val colors = LudoBoardData.playerColors
    private val paths = LudoBoardData.paths
    private val safeCells = setOf("bs","rs","gs","ys","b8","r8","g8","y8") +
            (1..6).flatMap { listOf("be$it","re$it","ge$it","ye$it") }

    override fun createInitialState(players: List<PlayerId>): LudoState {
        val pieces = players.mapIndexed { idx, player ->
            val color = colors[idx % colors.size]
            color to (0..3).map { LudoPiece("${color.take(1)}$it", color) }
        }.toMap()
        val diceEngines = players.associateWith { player ->
            DiceEngine(createProfileForGame("ludo"), player.value)
        }
        return LudoState(players = players, currentPlayer = players.first(), pieces = pieces,
            message = "نوبت ${players.first().value} - تاس بنداز 🎲", diceEngines = diceEngines)
    }

    override fun validateAction(state: LudoState, action: LudoAction, player: PlayerId): Boolean {
        if (state.gameOver || state.currentPlayer == null || state.currentPlayer != player) return false
        return when (action) {
            is LudoAction.RollDice -> state.canRollAgain
            is LudoAction.MovePiece -> state.diceValue > 0 && state.rolloutAvailable.contains(action.pieceIndex)
        }
    }

    override fun applyAction(state: LudoState, action: LudoAction, player: PlayerId): GameUpdateResult<LudoState, GameResult> {
        require(validateAction(state, action, player))
        val playerColor = colors[state.players.indexOf(player)]
        return when (action) {
            is LudoAction.RollDice -> handleRollDice(state, player, playerColor)
            is LudoAction.MovePiece -> handleMovePiece(state, player, playerColor, action.pieceIndex)
        }
    }

    private fun handleRollDice(state: LudoState, player: PlayerId, color: String): GameUpdateResult<LudoState, GameResult> {
        val diceEngine = state.diceEngines[player] ?: throw IllegalStateException("DiceEngine not found for player $player")
        val rolls = diceEngine.roll(1) // لودو از یک تاس استفاده می‌کند
        val dice = rolls.first()
        val pieces = state.pieces[color]!!

        // مدیریت تاس‌های متوالی ۶
        val newConsecutiveSixes = if (dice == 6) state.consecutiveSixes + 1 else 0
        val skipTurn = newConsecutiveSixes >= 3

        val movable = pieces.mapIndexedNotNull { idx, piece ->
            when {
                skipTurn -> null
                piece.state == "IN_BASE" && dice == 6 -> idx
                piece.state == "ON_TRACK" -> {
                    val newIdx = piece.pathIndex + dice
                    if (newIdx < (paths[color]?.size ?: 0)) idx else null
                }
                piece.state == "HOME_COLUMN" -> {
                    if (dice <= 5 - piece.homeColumnIndex) idx else null
                }
                else -> null
            }
        }

        if (skipTurn) {
            val next = getNextPlayer(state, player)
            return GameUpdateResult(state.copy(
                currentPlayer = next,
                diceValue = 0,
                message = "🎲 تاس $dice - سه تاس ۶ متوالی! نوبت به حریف می‌رود!",
                canRollAgain = true,
                rolloutAvailable = emptyList(),
                consecutiveSixes = 0,
                diceEngines = state.diceEngines
            ), null)
        }

        if (movable.isEmpty()) {
            val next = getNextPlayer(state, player)
            return GameUpdateResult(state.copy(
                currentPlayer = next,
                diceValue = 0,
                message = "🎲 تاس $dice - هیچ حرکتی ممکن نیست!",
                canRollAgain = true,
                rolloutAvailable = emptyList(),
                consecutiveSixes = 0,
                diceEngines = state.diceEngines
            ), null)
        }

        return GameUpdateResult(state.copy(
            currentPlayer = player,
            diceValue = dice,
            message = "🎲 تاس $dice - یک مهره انتخاب کن",
            canRollAgain = false,
            rolloutAvailable = movable,
            consecutiveSixes = newConsecutiveSixes,
            diceEngines = state.diceEngines
        ), null)
    }

    private fun handleMovePiece(state: LudoState, player: PlayerId, color: String, idx: Int): GameUpdateResult<LudoState, GameResult> {
        val currentPiece = state.pieces[color]!![idx]
        val dice = state.diceValue
        require(dice > 0)
        require(idx in state.rolloutAvailable)

        var extraTurn = false
        var message = ""

        println("🔄 قبل از حرکت: بازیکن=$player, مهره=$currentPiece, تاس=$dice")

        val updatedPiece = when (currentPiece.state) {
            "IN_BASE" -> {
                require(dice == 6)
                extraTurn = true
                message = "✅ مهره وارد بازی شد!"
                currentPiece.copy(state = "ON_TRACK", pathIndex = 0)
            }
            "ON_TRACK" -> {
                val path = paths[color]!!
                val newIdx = currentPiece.pathIndex + dice
                val targetCell = path[newIdx]
                println("🎯 حرکت ON_TRACK: targetCell=$targetCell, newIdx=$newIdx")

                // ** اولویت ۱: رسیدن مستقیم به خانه‌ی e6 **
                if (targetCell.endsWith("e6")) {
                    extraTurn = true
                    message = "🏁 مهره به پایان رسید! 🎁 تاس جایزه! "
                    currentPiece.copy(state = "FINISHED", homeColumnIndex = 5, pathIndex = -1)
                }
                // ** اولویت ۲: ورود به ستون خانه (اگر خانه‌ی بعدی e1 باشد) **
                else {
                    val nextCellId = path.getOrNull(newIdx + 1)
                    println("🔍 nextCellId=$nextCellId")
                    if (nextCellId != null && nextCellId.endsWith("e1")) {
                        extraTurn = (dice == 6)
                        message = "🏠 وارد ستون خانه شدید! "
                        currentPiece.copy(state = "HOME_COLUMN", pathIndex = -1, homeColumnIndex = 0)
                    }
                    // ** اولویت ۳: رسیدن به انتهای مسیر (بدون e1) -> ورود اضطراری به ستون **
                    else if (newIdx == path.size - 1 && nextCellId == null) {
                        extraTurn = (dice == 6)
                        message = "🏠 وارد ستون خانه شدید! (انتهای مسیر)"
                        currentPiece.copy(state = "HOME_COLUMN", pathIndex = -1, homeColumnIndex = 0)
                    }
                    // ** حرکت عادی **
                    else {
                        extraTurn = (dice == 6)
                        if (message.isEmpty()) message = "حرکت انجام شد"
                        currentPiece.copy(state = "ON_TRACK", pathIndex = newIdx)
                    }
                }
            }
            "HOME_COLUMN" -> {
                val remaining = 5 - currentPiece.homeColumnIndex
                require(dice <= remaining) { "عدد تاس بیشتر از خانه‌های باقی‌مانده است" }
                val newHomeIdx = currentPiece.homeColumnIndex + dice
                if (newHomeIdx == 5) {
                    extraTurn = true
                    message = "🏁 مهره به پایان رسید! 🎁 تاس جایزه! "
                    currentPiece.copy(state = "FINISHED", homeColumnIndex = 5)
                } else {
                    extraTurn = (dice == 6)
                    message = "حرکت در ستون خانه"
                    currentPiece.copy(state = "HOME_COLUMN", homeColumnIndex = newHomeIdx)
                }
            }
            else -> currentPiece
        }

        // اعمال تغییرات روی یک کپی از وضعیت کلی
        val allPieces = state.pieces.mapValues { it.value.toMutableList() }.toMutableMap()
        allPieces[color] = allPieces[color]!!.toMutableList().apply { this[idx] = updatedPiece }

        // مدیریت Capture
        if (currentPiece.state == "ON_TRACK") {
            val path = paths[color]!!
            val targetCell = path[currentPiece.pathIndex + dice]
            if (targetCell !in safeCells) {
                allPieces.forEach { (otherColor, list) ->
                    if (otherColor != color) {
                        val newList = list.toMutableList()
                        for (i in newList.indices) {
                            val other = newList[i]
                            if (other.state == "ON_TRACK" && paths[otherColor]?.getOrNull(other.pathIndex) == targetCell) {
                                newList[i] = other.copy(state = "IN_BASE", pathIndex = -1)
                                extraTurn = true
                                message += "💥 مهره حریف خورده شد! 🎁 "
                            }
                        }
                        allPieces[otherColor] = newList
                    }
                }
            }
        }

        println("🔄 بعد از حرکت: مهره به‌روز شده=$updatedPiece, extraTurn=$extraTurn, پیام=$message")
        println("🔄 وضعیت تمام مهره‌ها: $allPieces")

        // بررسی برد
        if (updatedPiece.state == "FINISHED") {
            val myPieces = allPieces[color]!!
            val finishedCount = myPieces.count { it.state == "FINISHED" }
            println("🔄 بررسی برد: تعداد مهره‌های تمام‌شده برای $color = $finishedCount")
            if (finishedCount == 4) {
                println("🎉 برنده: $player")
                return GameUpdateResult(state.copy(
                    winner = player, gameOver = true,
                    message = "🎉 ${player.value} برنده شد!",
                    canRollAgain = false, rolloutAvailable = emptyList(),
                    pieces = allPieces,
                    diceEngines = state.diceEngines
                ), GameResult.Win(player))
            }
        }

        val nextPlayer = if (extraTurn) player else getNextPlayer(state, player)
        val newConsecutiveSixes = if (extraTurn && state.diceValue == 6) state.consecutiveSixes else 0
        return GameUpdateResult(state.copy(
            currentPlayer = nextPlayer, diceValue = 0, message = message,
            canRollAgain = true, rolloutAvailable = emptyList(), pieces = allPieces,
            consecutiveSixes = newConsecutiveSixes,
            diceEngines = state.diceEngines
        ), null)
    }

    override fun isTerminal(state: LudoState) = state.gameOver
    override fun getResult(state: LudoState) = if (state.winner != null) GameResult.Win(state.winner!!) else null

    private fun getNextPlayer(state: LudoState, current: PlayerId): PlayerId {
        val idx = state.players.indexOf(current)
        return state.players[(idx + 1) % state.players.size]
    }
}