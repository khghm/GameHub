// games/monopoly/src/commonMain/kotlin/com/gamehub/games/monopoly/MonopolyBotStrategy.kt
package com.gamehub.games.monopoly

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import kotlin.random.Random

class MonopolyBotStrategy : BotStrategy<MonopolyState, MonopolyAction> {
    override val gameId: String = "monopoly"
    override val supportedDifficultyLevels: IntRange = 1..10

    // Cache برای ارسال فقط یک پیشنهاد در هر دور معامله
    private val tradeProposedThisRound = mutableSetOf<String>()
    private val tradeCancelCounter = mutableMapOf<String, Int>()

    override suspend fun getNextMove(state: MonopolyState, botPlayerId: PlayerId, difficultyLevel: Int): MonopolyAction? {
        val ps = state.playerStates[botPlayerId] ?: return null
        val currentPlayer = state.currentPlayer
        val turnPhase = state.turnPhase

        // ========== 1. مدیریت معاملات (حتی اگر نوبت ربات نباشد) ==========
        if (turnPhase == TurnPhase.AWAITING_TRADE) {
            val isBotCurrentPlayer = (currentPlayer == botPlayerId)
            return when (state.tradeStep) {
                TradeStep.AWAITING_PROPOSALS -> {
                    if (isBotCurrentPlayer) {
                        // ربات روی خانه معامله ایستاده → نباید پیشنهاد بدهد، بلکه بعد از چند بار لغو کند
                        if (shouldCancelTrade(state, botPlayerId)) {
                            println("🤖 Bot $botPlayerId is cancelling trade (no proposals)")
                            MonopolyAction.CancelTrade
                        } else {
                            // هنوز منتظر بماند
                            null
                        }
                    } else {
                        // ربات یکی از سایر بازیکنان است → یک بار پیشنهاد بدهد
                        val roundKey = "${state.roundCount}_${state.tradeProposals.size}"
                        if (!tradeProposedThisRound.contains(roundKey)) {
                            tradeProposedThisRound.add(roundKey)
                            println("🤖 Bot $botPlayerId sending trade proposal")
                            val (offerCash, offerProperties) = generateTradeOffer(state, botPlayerId, difficultyLevel)
                            MonopolyAction.SubmitTradeProposal(offerCash, offerProperties)
                        } else {
                            null
                        }
                    }
                }
                TradeStep.AWAITING_COUNTER -> {
                    if (isBotCurrentPlayer) {
                        // ربات باید ضدپیشنهاد بدهد
                        val selectedFrom = state.selectedProposalFrom
                        if (selectedFrom != null) {
                            println("🤖 Bot $botPlayerId making counter-offer")
                            val (requestedCash, requestedProps) = generateCounterOffer(state, botPlayerId, selectedFrom, difficultyLevel)
                            MonopolyAction.MakeCounterOffer(selectedFrom, requestedCash, requestedProps)
                        } else null
                    } else {
                        null
                    }
                }
                TradeStep.AWAITING_RESPONSE -> {
                    // ربات باید به ضدپیشنهاد پاسخ دهد (اگر طرف مقابل است)
                    val pendingTrade = state.pendingTrade
                    if (pendingTrade != null && pendingTrade.toPlayer == botPlayerId) {
                        println("🤖 Bot $botPlayerId evaluating counter-offer")
                        val accept = evaluateTradeOffer(pendingTrade, ps, state, difficultyLevel)
                        if (accept) {
                            MonopolyAction.AcceptTrade(pendingTrade.id)
                        } else {
                            MonopolyAction.RejectTrade(pendingTrade.id)
                        }
                    } else null
                }
                else -> null
            }
        } else {
            // پاک کردن کش پیشنهادات معامله و شمارنده لغو وقتی دور معامله تمام شد
            tradeProposedThisRound.clear()
            tradeCancelCounter.clear()
        }

        // اگر نوبت ربات نیست، در بقیه موارد کاری انجام نده
        if (currentPlayer != botPlayerId) return null

        println("🤖 Bot $botPlayerId turnPhase=$turnPhase, cash=${ps.cash}, owned=${ps.ownedProperties.size}")

        // ========== 2. مدیریت زندان ==========
        if (ps.jailTurns > 0) {
            return when (turnPhase) {
                TurnPhase.AWAITING_JAIL_DECISION -> {
                    if (ps.cash >= 150 && (difficultyLevel >= 5 || hasGetOutOfJailCard(ps))) {
                        MonopolyAction.PayJailFine
                    } else {
                        MonopolyAction.StayInJail
                    }
                }
                else -> null
            }
        }

        // ========== 3. مدیریت فازهای نوبتی ==========
        return when (turnPhase) {
            TurnPhase.WAITING_FOR_ROLL -> handleWaitingForRoll(state, botPlayerId, difficultyLevel)
            TurnPhase.STRATEGIC_DICE -> handleStrategicDice(state, botPlayerId)
            TurnPhase.AWAITING_DICE_SELECTION -> handleDiceSelection(state, botPlayerId)
            TurnPhase.AWAITING_DECISION -> handleDecision(state, botPlayerId, difficultyLevel)
            TurnPhase.AWAITING_BUILD -> handleBuild(state, botPlayerId, difficultyLevel)
            TurnPhase.AWAITING_TRADE -> null // قبلاً مدیریت شد
            TurnPhase.AWAITING_JAIL_DECISION -> null // قبلاً مدیریت شد
            else -> null
        }
    }

    // ==================== توابع کمکی ====================

    private fun hasGetOutOfJailCard(ps: PlayerState): Boolean {
        return ps.helperCards.contains(HelperCard.GET_OUT_OF_JAIL)
    }

    private fun shouldCancelTrade(state: MonopolyState, botId: PlayerId): Boolean {
        // استفاده از botId و roundCount به عنوان کلید یکتا (به جای gameId که در MonopolyState وجود ندارد)
        val key = "${botId}_${state.roundCount}"
        val count = tradeCancelCounter.getOrDefault(key, 0)
        if (count >= 3) { // بعد از 3 بار تلاش (هر بار حدود 0.5 ثانیه) لغو کن
            tradeCancelCounter.remove(key)
            return true
        }
        tradeCancelCounter[key] = count + 1
        return false
    }

    private fun handleWaitingForRoll(state: MonopolyState, botPlayerId: PlayerId, difficulty: Int): MonopolyAction? {
        val ps = state.playerStates[botPlayerId]!!
        if (ps.helperCards.isNotEmpty() && difficulty >= 7 && Random.nextFloat() < 0.3f) {
            val card = ps.helperCards.random()
            if (card != HelperCard.GET_OUT_OF_JAIL || ps.jailTurns > 0) {
                return MonopolyAction.UseHelperCard(card)
            }
        }
        return MonopolyAction.RollDice
    }

    private fun handleStrategicDice(state: MonopolyState, botPlayerId: PlayerId): MonopolyAction? {
        val ps = state.playerStates[botPlayerId]!!
        if (ps.strategicDiceOptions.isNotEmpty()) {
            val best = ps.strategicDiceOptions.maxOrNull() ?: ps.strategicDiceOptions.first()
            return MonopolyAction.SelectStrategicDice(best)
        }
        return null
    }

    private fun handleDiceSelection(state: MonopolyState, botPlayerId: PlayerId): MonopolyAction? {
        val dice = state.diceResult
        if (dice.size < 2) return null
        val ps = state.playerStates[botPlayerId]!!
        val currentPos = ps.position
        val optionSum = dice[0] + dice[1]
        val optionFirst = dice[0]
        val optionSecond = dice[1]

        val targetSum = (currentPos + optionSum) % MonopolyBoardData.CELL_COUNT
        val targetFirst = (currentPos + optionFirst) % MonopolyBoardData.CELL_COUNT
        val targetSecond = (currentPos + optionSecond) % MonopolyBoardData.CELL_COUNT

        val scoreSum = evaluateLandingCell(state, targetSum, botPlayerId)
        val scoreFirst = evaluateLandingCell(state, targetFirst, botPlayerId)
        val scoreSecond = evaluateLandingCell(state, targetSecond, botPlayerId)

        val bestOption = when {
            scoreSum >= scoreFirst && scoreSum >= scoreSecond -> optionSum
            scoreFirst >= scoreSecond -> optionFirst
            else -> optionSecond
        }
        return MonopolyAction.SelectDiceAndMove(bestOption)
    }

    private fun evaluateLandingCell(state: MonopolyState, cellIndex: Int, botId: PlayerId): Int {
        val cell = MonopolyBoardData.cells[cellIndex]
        val prop = state.board[cellIndex]
        var score = 0
        when (cell.type) {
            MonopolyBoardData.CellType.START -> score = 50
            MonopolyBoardData.CellType.PROPERTY, MonopolyBoardData.CellType.TRANSPORT, MonopolyBoardData.CellType.TEMPORARY -> {
                if (prop.ownerId == null) {
                    val affordability = if (state.playerStates[botId]!!.cash >= cell.price) 100 else 0
                    score = (cell.price / 10) + (cell.rentBase * 5) + affordability
                    val group = cell.group
                    if (group != null) {
                        val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
                        val ownedByBot = groupIndices.count { state.board[it].ownerId == botId }
                        if (ownedByBot == groupIndices.size - 1) score += 200
                    }
                } else if (prop.ownerId == botId) {
                    score = 10
                } else {
                    val rent = MonopolyBoardData.calculateRent(cell, prop.houses, hasFullGroup(state, cellIndex, prop.ownerId!!))
                    score = -rent
                }
            }
            MonopolyBoardData.CellType.CHANCE -> score = 20
            MonopolyBoardData.CellType.BANK -> score = 30
            MonopolyBoardData.CellType.JAIL -> score = -50
            MonopolyBoardData.CellType.DEAL -> score = 40
            MonopolyBoardData.CellType.MISSION -> score = 25
            else -> score = 0
        }
        return score
    }

    private fun hasFullGroup(state: MonopolyState, cellIndex: Int, ownerId: PlayerId): Boolean {
        val cell = MonopolyBoardData.cells[cellIndex]
        val group = cell.group ?: return false
        val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
        return groupIndices.all { state.board[it].ownerId == ownerId }
    }

    private fun handleDecision(state: MonopolyState, botPlayerId: PlayerId, difficulty: Int): MonopolyAction? {
        val ps = state.playerStates[botPlayerId]!!
        val pos = ps.position
        val cell = MonopolyBoardData.cells[pos]
        val prop = state.board[pos]

        return when (cell.type) {
            MonopolyBoardData.CellType.PROPERTY,
            MonopolyBoardData.CellType.TRANSPORT,
            MonopolyBoardData.CellType.TEMPORARY -> {
                if (prop.ownerId == null) {
                    val shouldBuy = evaluatePurchase(state, botPlayerId, pos, difficulty)
                    if (shouldBuy && ps.cash >= cell.price) {
                        MonopolyAction.BuyProperty
                    } else {
                        MonopolyAction.PassProperty
                    }
                } else if (prop.ownerId == botPlayerId) {
                    MonopolyAction.PassProperty
                } else {
                    MonopolyAction.PassProperty
                }
            }
            MonopolyBoardData.CellType.BANK -> handleBankAction(state, botPlayerId, difficulty)
            MonopolyBoardData.CellType.TRANSPORT -> {
                if (prop.ownerId == botPlayerId) {
                    handleTransport(state, botPlayerId, difficulty)
                } else {
                    MonopolyAction.PassProperty
                }
            }
            MonopolyBoardData.CellType.DEAL -> {
                MonopolyAction.PassProperty
            }
            else -> MonopolyAction.PassProperty
        }
    }

    private fun evaluatePurchase(state: MonopolyState, botId: PlayerId, propertyIndex: Int, difficulty: Int): Boolean {
        val ps = state.playerStates[botId]!!
        val cell = MonopolyBoardData.cells[propertyIndex]
        val price = cell.price
        val cash = ps.cash
        if (cash < price) return false

        var value = cell.rentBase * 10 + price / 10
        val group = cell.group
        if (group != null) {
            val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
            val ownedByBot = groupIndices.count { state.board[it].ownerId == botId }
            if (ownedByBot == groupIndices.size - 1) {
                value += price / 2
            }
        }
        val affordability = cash.toDouble() / price.toDouble()
        var threshold = when (difficulty) {
            1, 2 -> 1.2
            3, 4 -> 1.5
            5, 6 -> 1.8
            7, 8 -> 2.2
            else -> 2.5
        }
        if (price < 200) threshold *= 0.8
        return value >= price && affordability >= threshold
    }

    private fun handleBankAction(state: MonopolyState, botId: PlayerId, difficulty: Int): MonopolyAction? {
        val ps = state.playerStates[botId]!!
        if (ps.cash < 200 && difficulty >= 5) {
            val mortgagable = ps.ownedProperties.filter { !state.board[it].mortgaged }
            if (mortgagable.isNotEmpty()) {
                val propIndex = mortgagable.minByOrNull { MonopolyBoardData.cells[it].price }
                if (propIndex != null) return MonopolyAction.MortgageProperty(propIndex)
            }
        }
        if (difficulty >= 6 && ps.loan == null && ps.cash < 500) {
            val loanAmount = when {
                ps.cash < 200 -> 500
                ps.cash < 400 -> 300
                else -> 100
            }
            return MonopolyAction.TakeLoan(loanAmount)
        }
        if (difficulty >= 8 && ps.cash > 800 && ps.investments.isEmpty()) {
            val investAmount = minOf(200, ps.cash / 4)
            if (investAmount >= 50) return MonopolyAction.MakeInvestment(investAmount)
        }
        return MonopolyAction.PassProperty
    }

    private fun handleTransport(state: MonopolyState, botId: PlayerId, difficulty: Int): MonopolyAction? {
        val ps = state.playerStates[botId]!!
        val fromPos = ps.position
        val destinations = MonopolyBoardData.cells.indices.filter { idx ->
            MonopolyBoardData.cells[idx].type == MonopolyBoardData.CellType.TRANSPORT &&
                    idx != fromPos &&
                    state.board[idx].ownerId != null
        }
        if (destinations.isEmpty()) return MonopolyAction.PassProperty
        var bestDest = -1
        var bestScore = -1
        for (dest in destinations) {
            val score = evaluateLandingCell(state, dest, botId)
            val cost = 50 + kotlin.math.abs(fromPos - dest) * 5
            if (score - cost > bestScore) {
                bestScore = score - cost
                bestDest = dest
            }
        }
        return if (bestDest != -1 && ps.cash >= 50 + kotlin.math.abs(fromPos - bestDest) * 5) {
            MonopolyAction.UseTransport(bestDest)
        } else {
            MonopolyAction.PassProperty
        }
    }

    private fun handleBuild(state: MonopolyState, botId: PlayerId, difficulty: Int): MonopolyAction? {
        val ps = state.playerStates[botId]!!
        val pos = ps.position
        val cell = MonopolyBoardData.cells[pos]
        if (cell.type != MonopolyBoardData.CellType.PROPERTY) return MonopolyAction.PassProperty
        if (ps.cash < cell.buildCost) return MonopolyAction.PassProperty
        val group = cell.group ?: return MonopolyAction.PassProperty
        val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
        val currentHouses = state.board[pos].houses
        if (currentHouses >= 4) return MonopolyAction.PassProperty
        val minHousesInGroup = groupIndices.minOf { state.board[it].houses }
        if (currentHouses > minHousesInGroup) return MonopolyAction.PassProperty
        val shouldBuild = when (difficulty) {
            1, 2 -> false
            3, 4 -> ps.cash > cell.buildCost * 3
            5, 6 -> ps.cash > cell.buildCost * 2
            else -> ps.cash > cell.buildCost
        }
        return if (shouldBuild) MonopolyAction.BuildHouse(pos) else MonopolyAction.PassProperty
    }

    // ==================== توابع معامله پیشرفته ====================

    private fun generateTradeOffer(state: MonopolyState, botId: PlayerId, difficulty: Int): Pair<Int, List<Int>> {
        val ps = state.playerStates[botId]!!
        var offerCash = (ps.cash * 0.2).toInt().coerceIn(20, 300)
        val offerProperties = mutableListOf<Int>()
        if (difficulty >= 7 && ps.ownedProperties.size > 3) {
            val candidates = ps.ownedProperties.filter { propIdx ->
                val cell = MonopolyBoardData.cells[propIdx]
                val group = cell.group
                if (group != null) {
                    val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
                    val ownedCount = groupIndices.count { state.board[it].ownerId == botId }
                    ownedCount < groupIndices.size
                } else true
            }
            if (candidates.isNotEmpty()) {
                val worstProp = candidates.minByOrNull { MonopolyBoardData.cells[it].price }
                if (worstProp != null) offerProperties.add(worstProp)
                offerCash = (offerCash * 0.7).toInt()
            }
        }
        return Pair(offerCash, offerProperties)
    }

    private fun generateCounterOffer(state: MonopolyState, botId: PlayerId, proposerId: PlayerId, difficulty: Int): Pair<Int, List<Int>> {
        val ps = state.playerStates[botId]!!
        val originalProposal = state.tradeProposals[proposerId] ?: return Pair(0, emptyList())
        var requestedCash = (originalProposal.offeredCash * 1.2).toInt().coerceIn(30, 400)
        val requestedProperties = mutableListOf<Int>()
        if (difficulty >= 8 && originalProposal.offeredProperties.isNotEmpty()) {
            val targetPlayer = proposerId
            val targetPs = state.playerStates[targetPlayer]!!
            if (targetPs.ownedProperties.isNotEmpty()) {
                val bestProp = targetPs.ownedProperties.maxByOrNull { MonopolyBoardData.cells[it].price }
                if (bestProp != null) requestedProperties.add(bestProp)
                requestedCash = (requestedCash * 1.3).toInt()
            }
        }
        return Pair(requestedCash, requestedProperties)
    }

    private fun evaluateTradeOffer(trade: TradeProposal, botState: PlayerState, state: MonopolyState, difficulty: Int): Boolean {
        val requestedCash = trade.requestedCash
        val requestedProps = trade.requestedProperties
        val offeredCash = trade.offeredCash
        val offeredProps = trade.offeredProperties

        var valueForBot = offeredCash - requestedCash
        for (propIdx in offeredProps) {
            val cell = MonopolyBoardData.cells[propIdx]
            valueForBot += cell.price / 2
        }
        for (propIdx in requestedProps) {
            val cell = MonopolyBoardData.cells[propIdx]
            valueForBot -= cell.price / 2
        }
        val threshold = when (difficulty) {
            1, 2 -> 0
            3, 4 -> 50
            5, 6 -> 100
            else -> 200
        }
        val cashNeeded = botState.cash < 300
        val accept = valueForBot >= threshold || (cashNeeded && offeredCash > requestedCash + 50)
        println("🤖 Bot evaluates trade: value=$valueForBot, accept=$accept")
        return accept
    }
}