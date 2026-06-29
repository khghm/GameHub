package com.gamehub.games.monopoly

import com.gamehub.shared.core.*
import kotlin.random.Random
import com.gamehub.shared.engine.GameUpdateResult

private val random = Random

private data class Tuple4<A, B, C, D, E>(
    val first: A, val second: B, val third: C, val fourth: D, val fifth: E
)

class MonopolyEngine : GameDefinition<MonopolyState, MonopolyAction, GameResult> {
    override val metadata = GameMetadata(
        id = "monopoly", name = "Bank Roll", minPlayers = 2, maxPlayers = 6,
        description = "Economic strategy game with chance, missions, transport, and bank services"
    )

    // ========================== Helper Functions ==========================

    private fun calculateRent(
        cell: MonopolyBoardData.CellInfo,
        houses: Int,
        fullGroup: Boolean
    ): Int {
        var rent = when (houses) {
            0 -> cell.rentBase
            1 -> cell.rentBase * 2
            2 -> cell.rentBase * 3
            3 -> cell.rentBase * 4
            4 -> cell.rentBase * 5
            else -> cell.rentBase * 6
        }
        if (fullGroup && houses == 0) rent *= 2
        return rent
    }

    private fun getNextPlayer(state: MonopolyState, current: PlayerId?): PlayerId {
        val idx = state.players.indexOf(current)
        return state.players[(idx + 1) % state.players.size]
    }

    private fun canBuildHouseBalanced(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): Boolean {
        val cell = MonopolyBoardData.cells[propIndex]
        if (cell.type != MonopolyBoardData.CellType.PROPERTY) return false
        val group = cell.group ?: return false
        val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
        if (groupIndices.any { state.board[it].ownerId != player }) return false
        val currentHouses = state.board[propIndex].houses
        if (currentHouses >= 4) return false
        val minHouses = groupIndices.minOf { state.board[it].houses }
        if (currentHouses - minHouses >= 1) return false
        val buildCost = cell.buildCost
        val ps = state.playerStates[player] ?: return false
        return ps.cash >= buildCost && !state.board[propIndex].mortgaged
    }

    private fun canSellHouseBalanced(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): Boolean {
        val cell = MonopolyBoardData.cells[propIndex]
        if (cell.type != MonopolyBoardData.CellType.PROPERTY) return false
        val group = cell.group ?: return false
        val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == group }
        if (groupIndices.any { state.board[it].ownerId != player }) return false
        val currentHouses = state.board[propIndex].houses
        if (currentHouses == 0) return false
        val maxHouses = groupIndices.maxOf { state.board[it].houses }
        if (maxHouses - currentHouses >= 1) return false
        return true
    }

    private fun calculateTransportCost(fromIdx: Int, toIdx: Int): Int {
        val base = 50
        return base + kotlin.math.abs(fromIdx - toIdx) * 5
    }

    // ========================== Validation ==========================

    override fun validateAction(
        state: MonopolyState,
        action: MonopolyAction,
        player: PlayerId
    ): Boolean {
        if (state.gameOver) return false

        when (action) {
            is MonopolyAction.SubmitTradeProposal -> {
                return state.isTradeActive && player != state.currentPlayer
            }
            is MonopolyAction.AcceptTrade, is MonopolyAction.RejectTrade -> {
                return state.tradeStep == TradeStep.AWAITING_RESPONSE &&
                        state.pendingTrade?.toPlayer == player
            }
            is MonopolyAction.SelectTradeProposal -> {
                return state.tradeStep == TradeStep.AWAITING_PROPOSALS &&
                        state.currentPlayer == player &&
                        state.tradeProposals.containsKey(action.proposerId)
            }
            is MonopolyAction.MakeCounterOffer -> {
                return state.tradeStep == TradeStep.AWAITING_COUNTER &&
                        state.currentPlayer == player &&
                        state.selectedProposalFrom == action.targetPlayerId
            }
            is MonopolyAction.CancelTrade -> {
                if (!state.isTradeActive) return false
                return when (state.tradeStep) {
                    TradeStep.AWAITING_PROPOSALS -> state.currentPlayer == player
                    TradeStep.AWAITING_RESPONSE -> state.pendingTrade?.toPlayer == player
                    else -> false
                }
            }
            is MonopolyAction.RollStrategicDice -> {
                val ps = state.playerStates[player] ?: return false
                return state.currentPlayer == player &&
                        !ps.firstMoveDone &&
                        state.settings.strategicDiceEnabled &&
                        state.turnPhase == TurnPhase.WAITING_FOR_ROLL
            }
            is MonopolyAction.SelectStrategicDice -> {
                val ps = state.playerStates[player] ?: return false
                return state.currentPlayer == player &&
                        state.turnPhase == TurnPhase.STRATEGIC_DICE &&
                        ps.strategicDiceOptions.contains(action.chosenValue)
            }
            else -> {
                if (state.currentPlayer != player) return false
            }
        }

        val ps = state.playerStates[player] ?: return false

        if (ps.jailTurns > 0) {
            return when (action) {
                is MonopolyAction.RollDice,
                is MonopolyAction.PayJailFine,
                is MonopolyAction.StayInJail -> true
                else -> false
            }
        }

        return when (action) {
            is MonopolyAction.RollDice -> state.turnPhase == TurnPhase.WAITING_FOR_ROLL
            is MonopolyAction.SelectDiceAndMove -> {
                state.turnPhase == TurnPhase.AWAITING_DICE_SELECTION &&
                        action.selectedSteps in 1..12 &&
                        (action.selectedSteps == state.diceResult.sum() ||
                                action.selectedSteps == state.diceResult[0] ||
                                action.selectedSteps == state.diceResult[1])
            }
            is MonopolyAction.BuyProperty, is MonopolyAction.PassProperty -> state.turnPhase == TurnPhase.AWAITING_DECISION
            is MonopolyAction.SellProperty -> {
                (state.turnPhase == TurnPhase.WAITING_FOR_ROLL || state.turnPhase == TurnPhase.AWAITING_DECISION) &&
                        ps.ownedProperties.contains(action.propertyIndex)
            }
            is MonopolyAction.BuildHouse -> {
                (state.turnPhase == TurnPhase.WAITING_FOR_ROLL || state.turnPhase == TurnPhase.AWAITING_BUILD) &&
                        canBuildHouseBalanced(state, player, action.propertyIndex)
            }
            is MonopolyAction.SellHouse -> {
                (state.turnPhase == TurnPhase.WAITING_FOR_ROLL || state.turnPhase == TurnPhase.AWAITING_DECISION) &&
                        canSellHouseBalanced(state, player, action.propertyIndex)
            }
            is MonopolyAction.TakeLoan -> {
                state.turnPhase == TurnPhase.AWAITING_DECISION && ps.loan == null && action.amount in listOf(100, 300, 500) && ps.cash >= 0
            }
            is MonopolyAction.MortgageProperty -> {
                state.turnPhase == TurnPhase.AWAITING_DECISION && ps.ownedProperties.contains(action.propertyIndex) &&
                        !state.board[action.propertyIndex].mortgaged
            }
            is MonopolyAction.UnmortgageProperty -> {
                state.turnPhase == TurnPhase.AWAITING_DECISION && ps.ownedProperties.contains(action.propertyIndex) &&
                        state.board[action.propertyIndex].mortgaged
            }
            is MonopolyAction.MakeInvestment -> {
                state.turnPhase == TurnPhase.AWAITING_DECISION && action.amount >= 50 && ps.cash >= action.amount
            }
            is MonopolyAction.UseTransport -> {
                state.turnPhase == TurnPhase.AWAITING_DECISION && state.board[ps.position].ownerId == player &&
                        MonopolyBoardData.cells[ps.position].type == MonopolyBoardData.CellType.TRANSPORT
            }
            is MonopolyAction.ProposeTrade -> {
                state.turnPhase == TurnPhase.AWAITING_TRADE && action.targetPlayerId != player &&
                        action.offeredCash <= ps.cash && action.offeredProperties.all { ps.ownedProperties.contains(it) }
            }
            is MonopolyAction.CounterTrade -> {
                state.turnPhase == TurnPhase.AWAITING_TRADE && state.pendingTrade?.fromPlayer == player &&
                        state.pendingTrade?.status == TradeStatus.PENDING
            }
            is MonopolyAction.UseHelperCard -> {
                state.turnPhase == TurnPhase.WAITING_FOR_ROLL && ps.helperCards.contains(action.card)
            }
            is MonopolyAction.UseInnateShield -> {
                state.turnPhase == TurnPhase.WAITING_FOR_ROLL && !ps.innateShieldUsed && !ps.helperCards.contains(HelperCard.SHIELD)
            }
            is MonopolyAction.PayJailFine -> state.turnPhase == TurnPhase.AWAITING_JAIL_DECISION && ps.jailTurns > 0
            is MonopolyAction.StayInJail -> state.turnPhase == TurnPhase.AWAITING_JAIL_DECISION && ps.jailTurns > 0
            is MonopolyAction.SubmitTradeProposal,
            is MonopolyAction.SelectTradeProposal,
            is MonopolyAction.MakeCounterOffer,
            is MonopolyAction.AcceptTrade,
            is MonopolyAction.RejectTrade,
            is MonopolyAction.CancelTrade -> false
            else -> false
        }
    }

    // ========================== Apply Action ==========================

    override fun applyAction(
        state: MonopolyState,
        action: MonopolyAction,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        require(validateAction(state, action, player))
        return when (action) {
            is MonopolyAction.RollDice -> handleRollDice(state, player)
            is MonopolyAction.RollStrategicDice -> {
                val ps = state.playerStates[player]!!
                val options = generateStrategicDice()
                ps.strategicDiceOptions = options
                GameUpdateResult(
                    state.copy(
                        playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                        turnPhase = TurnPhase.STRATEGIC_DICE,
                        diceResult = options,
                        message = "تاس استراتژیک: یکی از سه تاس را انتخاب کنید."
                    ), null
                )
            }
            is MonopolyAction.SelectStrategicDice -> handleSelectStrategicDice(state, player, action.chosenValue)
            is MonopolyAction.SelectDiceAndMove -> {
                println("🎲 Applying SelectDiceAndMove with steps=${action.selectedSteps}")
                handleMoveWithSelectedDice(state, player, action.selectedSteps)
            }
            is MonopolyAction.BuyProperty -> handleBuyProperty(state, player)
            is MonopolyAction.PassProperty -> handlePassProperty(state, player)
            is MonopolyAction.SellProperty -> handleSellProperty(state, player, action.propertyIndex)
            is MonopolyAction.BuildHouse -> handleBuildHouse(state, player, action.propertyIndex)
            is MonopolyAction.SellHouse -> handleSellHouse(state, player, action.propertyIndex)
            is MonopolyAction.TakeLoan -> handleTakeLoan(state, player, action.amount)
            is MonopolyAction.MortgageProperty -> handleMortgage(state, player, action.propertyIndex)
            is MonopolyAction.UnmortgageProperty -> handleUnmortgage(state, player, action.propertyIndex)
            is MonopolyAction.MakeInvestment -> handleMakeInvestment(state, player, action.amount)
            is MonopolyAction.UseTransport -> handleUseTransport(state, player, action.destinationIndex)
            is MonopolyAction.ProposeTrade -> handleProposeTrade(state, player, action)
            is MonopolyAction.CounterTrade -> handleCounterTrade(state, player, action)
            is MonopolyAction.AcceptTrade -> handleAcceptTrade(state, player, action.tradeId)
            is MonopolyAction.RejectTrade -> handleRejectTrade(state, player, action.tradeId)
            is MonopolyAction.UseHelperCard -> handleUseHelperCard(state, player, action.card)
            is MonopolyAction.UseInnateShield -> handleUseInnateShield(state, player)
            is MonopolyAction.PayJailFine -> handlePayJailFine(state, player)
            is MonopolyAction.StayInJail -> handleStayInJail(state, player)
            is MonopolyAction.SubmitTradeProposal -> handleSubmitTradeProposal(state, player, action)
            is MonopolyAction.SelectTradeProposal -> handleSelectTradeProposal(state, player, action)
            is MonopolyAction.MakeCounterOffer -> handleMakeCounterOffer(state, player, action)
            is MonopolyAction.CancelTrade -> handleCancelTrade(state, player)
            else -> GameUpdateResult(state, null)
        }
    }

    // ========================== زندان ==========================

    private fun handlePayJailFine(state: MonopolyState, player: PlayerId): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val pName = state.playerNames[player] ?: player.value

        if (ps.cash < 0) {
            ps.jailTurns = 5
            ps.jailFirstTime = false
            val next = getNextPlayer(state, player)
            val newState = state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "$pName نقدینگی منفی دارد! ۵ دور در زندان می‌ماند."
            )
            val stateAfterMission = updateMissionProgress(newState, MissionEvent.GO_TO_JAIL, player, null)
            return GameUpdateResult(stateAfterMission, null)
        }

        if (ps.cash < 150) {
            ps.jailTurns = if (ps.jailFirstTime) 1 else 3
            ps.jailFirstTime = false
            val next = getNextPlayer(state, player)
            val newState = state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "$pName پول کافی برای جریمه ندارد، ${ps.jailTurns} دور می‌ماند."
            )
            val stateAfterMission = updateMissionProgress(newState, MissionEvent.GO_TO_JAIL, player, null)
            return GameUpdateResult(stateAfterMission, null)
        }

        ps.cash -= 150
        ps.jailTurns = 0
        ps.jailFirstTime = true

        val extraTurn = if (ps.wasSentByDoubleSix) {
            ps.wasSentByDoubleSix = false
            true
        } else false

        if (extraTurn) {
            val newState = state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = player,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "$pName جریمه پرداخت کرد و به دلیل جفت ۶، یک نوبت دیگر تاس می‌اندازد."
            )
            val stateAfterMission = updateMissionProgress(newState, MissionEvent.GO_TO_JAIL, player, null)
            return GameUpdateResult(stateAfterMission, null)
        }

        val next = getNextPlayer(state, player)
        val newState = state.copy(
            playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
            currentPlayer = next,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            message = "$pName جریمه ۱۵۰ دلاری پرداخت کرد و از زندان آزاد شد."
        )
        val stateAfterMission = updateMissionProgress(newState, MissionEvent.GO_TO_JAIL, player, null)
        return GameUpdateResult(stateAfterMission, null)
    }

    private fun handleStayInJail(state: MonopolyState, player: PlayerId): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        if (ps.cash < 0) {
            ps.jailTurns = 5
        } else {
            ps.jailTurns = if (ps.jailFirstTime) 1 else 3
        }
        ps.jailFirstTime = false
        val next = getNextPlayer(state, player)
        val newState = state.copy(
            playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
            currentPlayer = next,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            message = "${state.playerNames[player]} تصمیم گرفت در زندان بماند (${ps.jailTurns} دور)."
        )
        val stateAfterMission = updateMissionProgress(newState, MissionEvent.GO_TO_JAIL, player, null)
        return GameUpdateResult(stateAfterMission, null)
    }

    // ========================== Core Turn Flow ==========================

    private fun handleRollDice(state: MonopolyState, player: PlayerId): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val pName = state.playerNames[player] ?: player.value

        if (ps.jailTurns > 0) {
            if (ps.jailFirstTime) {
                return GameUpdateResult(state.copy(turnPhase = TurnPhase.AWAITING_JAIL_DECISION, message = "..."), null)
            } else {
                ps.jailTurns--
                if (ps.jailTurns == 0) {
                    ps.jailFirstTime = true
                    return GameUpdateResult(state.copy(
                        playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                        turnPhase = TurnPhase.WAITING_FOR_ROLL,
                        message = "$pName از زندان آزاد شد."
                    ), null)
                } else {
                    val next = getNextPlayer(state, player)
                    return GameUpdateResult(state.copy(
                        playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                        currentPlayer = next,
                        turnPhase = TurnPhase.WAITING_FOR_ROLL,
                        message = "$pName هنوز در زندان است (${ps.jailTurns} دور دیگر)."
                    ), null)
                }
            }
        }

        if (!ps.firstMoveDone && state.settings.strategicDiceEnabled) {
            val options = generateStrategicDice()
            ps.strategicDiceOptions = options
            return GameUpdateResult(
                state.copy(
                    playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                    turnPhase = TurnPhase.STRATEGIC_DICE,
                    diceResult = options,
                    message = "تاس استراتژیک: یکی از سه تاس را انتخاب کنید."
                ), null
            )
        }

        val dice1 = random.nextInt(6) + 1
        val dice2 = random.nextInt(6) + 1
        val isDouble = dice1 == dice2
        var doubleCount = if (isDouble) state.doubleCount + 1 else 0

        if (doubleCount >= 3) {
            ps.position = 10
            ps.jailFirstTime = true
            ps.jailTurns = 0
            ps.wasSentByDoubleSix = true
            return GameUpdateResult(
                state.copy(
                    playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                    currentPlayer = player,
                    turnPhase = TurnPhase.AWAITING_JAIL_DECISION,
                    message = "$pName سه جفت پشت سر هم آورد → به زندان رفت!",
                    doubleCount = 0,
                    diceResult = listOf(dice1, dice2)
                ), null
            )
        }

        return GameUpdateResult(
            state.copy(
                diceResult = listOf(dice1, dice2),
                turnPhase = TurnPhase.AWAITING_DICE_SELECTION,
                message = "تاس‌ها: $dice1 و $dice2. یک یا هر دو را انتخاب کن."
            ), null
        )
    }

    private fun handleMoveWithSelectedDice(
        state: MonopolyState,
        player: PlayerId,
        steps: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        println("🔹 handleMoveWithSelectedDice: player=$player, steps=$steps")
        var currentState = state
        val ps = state.playerStates[player]!!
        val pName = state.playerNames[player] ?: player.value

        // خواندن تاس‌ها (پشتیبانی از تاس استراتژیک)
        val diceValues = state.diceResult
        val dice1 = diceValues.getOrNull(0) ?: return GameUpdateResult(
            state.copy(message = "خطا: تاس موجود نیست"), null
        )
        val dice2 = if (diceValues.size >= 2) diceValues[1] else 0
        val isDouble = diceValues.size >= 2 && dice1 == dice2

        // اعتبارسنجی حرکت
        val isValid = when (diceValues.size) {
            1 -> steps == dice1
            2 -> steps == dice1 || steps == dice2 || steps == dice1 + dice2
            else -> false
        }
        if (!isValid) {
            return GameUpdateResult(state.copy(message = "حرکت نامعتبر: مجموع انتخاب شده با تاس‌ها همخوانی ندارد"), null)
        }

        // حرکت مهره
        val oldPos = ps.position
        var newPos = (oldPos + steps) % MonopolyBoardData.CELL_COUNT
        val passedStart = oldPos + steps >= MonopolyBoardData.CELL_COUNT
        var message = ""

        // ===== عبور از شروع =====
        if (passedStart) {
            var salary = state.settings.startSalary
            if (ps.loan != null && ps.loan!!.remaining > 0) {
                val payment = minOf(salary, ps.loan!!.remaining)
                ps.loan!!.remaining -= payment
                salary -= payment
                if (ps.loan!!.remaining == 0) ps.loan = null
            }
            ps.cash += salary
            message += "از شروع عبور کردی و $salary$ حقوق دریافت کردی. "
            if (ps.innateShieldUsed) ps.innateShieldUsed = false

            // مدیریت سرمایه‌گذاری
            val investments = ps.investments.toMutableList()
            val iterInv = investments.iterator()
            while (iterInv.hasNext()) {
                val inv = iterInv.next()
                inv.roundsLeft--
                if (inv.roundsLeft <= 0) {
                    ps.cash += inv.returnAmount
                    iterInv.remove()
                    message += "سرمایه‌گذاری ${inv.amount}$ با سود ${inv.returnAmount - inv.amount}$ بازگشت. "
                }
            }
            ps.investments = investments

            // کاهش دور ملک موقت
            val newBoard = state.board.toMutableList()
            val ownedCopy = ps.ownedProperties.toList()
            for (idx in ownedCopy) {
                val cell = MonopolyBoardData.cells[idx]
                if (cell.type == MonopolyBoardData.CellType.TEMPORARY) {
                    val prop = newBoard[idx]
                    prop.temporaryRoundsLeft--
                    if (prop.temporaryRoundsLeft <= 0) {
                        ps.ownedProperties.remove(idx)
                        newBoard[idx] = PropertyState()
                        message += "ملک موقت ${cell.name} منقضی شد و به بانک بازگشت. "
                    } else {
                        newBoard[idx] = prop.copy(temporaryRoundsLeft = prop.temporaryRoundsLeft)
                    }
                }
            }
            currentState = currentState.copy(board = newBoard)

            // غیرفعال کردن سپر کارت کمکی
            if (ps.shieldActiveUntilStart) {
                ps.shieldActiveUntilStart = false
                message += "سپر محافظ غیرفعال شد. "
            }

            // ===== رویداد مأموریت: عبور از شروع =====
            currentState = updateMissionProgress(currentState, MissionEvent.PASS_START, player, null)
        }

        // به‌روزرسانی موقعیت
        ps.position = newPos
        message += "به ${MonopolyBoardData.cells[newPos].name} نقل مکان کردی. "
        println("🏃 Moved from $oldPos to $newPos (steps=$steps)")

        val cell = MonopolyBoardData.cells[newPos]
        val prop = currentState.board[newPos]
        var nextPhase = TurnPhase.AWAITING_DECISION
        var nextPlayer = player
        val newDoubleCount = if (isDouble) currentState.doubleCount + 1 else 0
        val shieldActive = ps.shieldActiveUntilStart || ps.helperCards.contains(HelperCard.SHIELD) || ps.innateShieldUsed

        // ===== رویداد مأموریت: فرود روی ملک (برای مأموریت ارث) =====
        currentState = updateMissionProgress(currentState, MissionEvent.LAND_ON_PROPERTY, player, newPos)

        // ===== خانه معامله (DEAL) =====
        if (cell.type == MonopolyBoardData.CellType.DEAL) {
            val newState = currentState.copy(
                playerStates = currentState.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = player,
                turnPhase = TurnPhase.AWAITING_TRADE,
                tradeStep = TradeStep.AWAITING_PROPOSALS,
                isTradeActive = true,
                tradeProposals = emptyMap(),
                selectedProposalFrom = null,
                pendingTrade = null,
                message = "بازیکن ${currentState.playerNames[player]} روی خانه معامله ایستاده است. سایر بازیکنان پیشنهاد خود را ارسال کنند."
            )
            return GameUpdateResult(newState, null)
        }

        // ===== سایر خانه‌ها =====
        when (cell.type) {
            MonopolyBoardData.CellType.PROPERTY, MonopolyBoardData.CellType.TRANSPORT, MonopolyBoardData.CellType.TEMPORARY -> {
                if (prop.ownerId == null) {
                    if (ps.cash >= cell.price) {
                        message += "این ملک قابل خرید است (${cell.price}$). خرید یا رد؟"
                        nextPhase = TurnPhase.AWAITING_DECISION
                        nextPlayer = player
                    } else {
                        message += "پول کافی برای خرید ندارید، رد می‌شود."
                        nextPhase = TurnPhase.WAITING_FOR_ROLL
                        nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                    }
                } else if (prop.ownerId == player) {
                    if (cell.type == MonopolyBoardData.CellType.PROPERTY && canBuildHouseBalanced(currentState, player, newPos)) {
                        nextPhase = TurnPhase.AWAITING_BUILD
                        message += "روی ملک خودت ایستادی. می‌توانی خانه بسازی."
                        nextPlayer = player
                    } else {
                        nextPhase = TurnPhase.WAITING_FOR_ROLL
                        nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                        message += "روی ملک خودت ایستادی اما نمی‌توانی بسازی."
                    }
                } else {
                    if (shieldActive) {
                        message += "سپر فعال است، اجاره پرداخت نمی‌شود."
                        nextPhase = TurnPhase.WAITING_FOR_ROLL
                        nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                    } else {
                        val fullGroup = cell.group?.let { grp ->
                            val groupIndices = MonopolyBoardData.cells.indices.filter { MonopolyBoardData.cells[it].group == grp }
                            groupIndices.all { currentState.board[it].ownerId == prop.ownerId }
                        } ?: false
                        val rent = calculateRent(cell, prop.houses, fullGroup)
                        if (ps.cash >= rent) {
                            ps.cash -= rent
                            val ownerState = currentState.playerStates[prop.ownerId]!!
                            ownerState.cash += rent
                            message += "اجاره $rent$ به ${currentState.playerNames[prop.ownerId]} پرداخت شد."
                            nextPhase = TurnPhase.WAITING_FOR_ROLL
                            nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                        } else {
                            return handleBankruptcy(currentState, player, "ناتوانی در پرداخت اجاره")
                        }
                    }
                }
            }
            MonopolyBoardData.CellType.BANK -> {
                message += "به بانک رسیدی. می‌توانی وام بگیری، رهن بگذاری یا سرمایه‌گذاری کنی."
                nextPhase = TurnPhase.AWAITING_DECISION
                nextPlayer = player
            }
            MonopolyBoardData.CellType.MISSION -> {
                if (currentState.settings.missionsEnabled) {
                    val (newStateAfterMission, missionMsg) = startRandomMission(currentState, player)
                    message += " مأموریت: $missionMsg"
                    val newPs = newStateAfterMission.playerStates[player]!!
                    if (newPs.cash < 0) return handleBankruptcy(newStateAfterMission, player, "نقدینگی منفی پس از مأموریت")
                    val finalNextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                    val finalState = newStateAfterMission.copy(
                        currentPlayer = finalNextPlayer,
                        turnPhase = TurnPhase.WAITING_FOR_ROLL,
                        message = message,
                        doubleCount = newDoubleCount,
                        diceResult = emptyList()
                    )
                    return GameUpdateResult(finalState, null)
                } else {
                    nextPhase = TurnPhase.WAITING_FOR_ROLL
                    nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                }
            }
            MonopolyBoardData.CellType.CHANCE -> {
                if (currentState.settings.chanceCardsEnabled) {
                    val (newStateAfterChance, chanceMsg) = applyChanceCardEffectAndGetState(currentState, player)
                    message += " کارت شانس: $chanceMsg"
                    val newPs = newStateAfterChance.playerStates[player]!!
                    if (newPs.cash < 0) return handleBankruptcy(newStateAfterChance, player, "نقدینگی منفی پس از کارت شانس")
                    val finalPos = newPs.position
                    if (finalPos != newPos) {
                        message += " و به ${MonopolyBoardData.cells[finalPos].name} منتقل شدی."
                    }
                    val finalNextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                    val finalState = newStateAfterChance.copy(
                        currentPlayer = finalNextPlayer,
                        turnPhase = TurnPhase.WAITING_FOR_ROLL,
                        message = message,
                        doubleCount = newDoubleCount,
                        diceResult = emptyList()
                    )
                    // ===== رویداد مأموریت: فرود روی شانس =====
                    val stateAfterMission = updateMissionProgress(finalState, MissionEvent.LAND_ON_CHANCE, player, null)
                    return GameUpdateResult(stateAfterMission, null)
                } else {
                    nextPhase = TurnPhase.WAITING_FOR_ROLL
                    nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
                }
            }
            MonopolyBoardData.CellType.JAIL -> {
                message += "فقط از زندان بازدید می‌کنی."
                nextPhase = TurnPhase.WAITING_FOR_ROLL
                nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
            }
            else -> {
                nextPhase = TurnPhase.WAITING_FOR_ROLL
                nextPlayer = if (isDouble && newDoubleCount < 3) player else getNextPlayer(currentState, player)
            }
        }

        // بررسی ورشکستگی
        if (ps.cash < 0) return handleBankruptcy(currentState, player, "نقدینگی منفی پس از حرکت")

        // بررسی شرط برد
        val totalAssets = ps.cash + ps.ownedProperties.sumOf { idx ->
            val propState = currentState.board[idx]
            (MonopolyBoardData.cells[idx].price * 0.85).toInt() + propState.improvementValue
        }
        if (totalAssets >= currentState.settings.winTarget) {
            return GameUpdateResult(
                currentState.copy(winner = player, gameOver = true, message = "$pName برنده شد! دارایی کل: $totalAssets$"),
                GameResult.Win(player)
            )
        }

        // ساخت وضعیت نهایی
        val finalState = currentState.copy(
            playerStates = currentState.playerStates.toMutableMap().apply { this[player] = ps },
            currentPlayer = nextPlayer,
            turnPhase = nextPhase,
            message = message,
            doubleCount = newDoubleCount,
            diceResult = emptyList()
        )

        // ===== رویداد مأموریت: پایان دور (ROUND_END) =====
        val stateAfterRound = updateMissionProgress(finalState, MissionEvent.ROUND_END, null, null)
        return GameUpdateResult(stateAfterRound, null)
    }

    // ========================== Property Transactions ==========================

    private fun handleBuyProperty(
        state: MonopolyState,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        println("🔹 handleBuyProperty: doubleCount=${state.doubleCount}, player=$player")
        val ps = state.playerStates[player]!!
        val pos = ps.position
        val cell = MonopolyBoardData.cells[pos]
        if (ps.cash < cell.price) {
            return GameUpdateResult(
                state.copy(message = "پول کافی برای خرید ${cell.name} نداری."),
                null
            )
        }
        ps.cash -= cell.price
        val newBoard = state.board.toMutableList()
        newBoard[pos] = state.board[pos].copy(ownerId = player)
        ps.ownedProperties.add(pos)

        if (cell.type == MonopolyBoardData.CellType.TEMPORARY) {
            newBoard[pos].temporaryRoundsLeft = 2
        }

        val next = if (state.doubleCount > 0) {
            println("👉 بازیکن به دلیل جفت بودن دوباره نوبت می‌گیرد")
            player
        } else {
            getNextPlayer(state, player)
        }
        val newState = state.copy(
            playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
            board = newBoard,
            currentPlayer = next,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            message = "${cell.name} به قیمت ${cell.price}$ خریداری شد."
        )

        // به‌روزرسانی مأموریت با رویداد خرید ملک
        val stateAfterMission = updateMissionProgress(newState, MissionEvent.BUY_PROPERTY, player, pos)
        return GameUpdateResult(stateAfterMission, null)
    }

    private fun handlePassProperty(
        state: MonopolyState,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "بازیکن ${state.playerNames[player]} از خرید صرف نظر کرد."
            ), null
        )
    }

    private fun handleSellProperty(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val cell = MonopolyBoardData.cells[propIndex]
        if (cell.type == MonopolyBoardData.CellType.TEMPORARY) {
            return GameUpdateResult(state.copy(message = "املاک موقت قابل فروش به بانک نیستند."), null)
        }
        val baseSell = (cell.price * 0.85).toInt()
        val improvement = state.board[propIndex].improvementValue
        val totalSell = baseSell + improvement
        ps.cash += totalSell
        ps.ownedProperties.remove(propIndex)
        val newBoard = state.board.toMutableList()
        newBoard[propIndex] = PropertyState()
        val newState = state.copy(
            playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
            board = newBoard,
            message = "${cell.name} به قیمت $totalSell$ فروخته شد (${baseSell}$ پایه + $improvement$ بهبود)."
        )
        // به‌روزرسانی مأموریت با رویداد فروش ملک
        val stateAfterMission = updateMissionProgress(newState, MissionEvent.SELL_PROPERTY, player, propIndex)
        return GameUpdateResult(stateAfterMission, null)
    }

    private fun handleBuildHouse(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val cell = MonopolyBoardData.cells[propIndex]
        val buildCost = cell.buildCost
        if (ps.cash < buildCost) {
            return GameUpdateResult(state.copy(message = "پول کافی برای ساخت خانه نداری."), null)
        }
        ps.cash -= buildCost
        val newBoard = state.board.toMutableList()
        val prop = newBoard[propIndex]
        prop.houses += 1
        val netIncrease = buildCost - state.settings.buildTax
        prop.improvementValue += netIncrease

        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                board = newBoard,
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "خانه‌ای روی ${cell.name} ساخته شد. ارزش ملک ${netIncrease}$ افزایش یافت."
            ), null
        )
    }

    private fun handleSellHouse(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val cell = MonopolyBoardData.cells[propIndex]
        val refund = cell.buildCost / 2
        ps.cash += refund
        val newBoard = state.board.toMutableList()
        val prop = newBoard[propIndex]
        val netDecrease = (cell.buildCost - state.settings.buildTax) / 2
        prop.improvementValue -= netDecrease
        if (prop.improvementValue < 0) prop.improvementValue = 0
        prop.houses -= 1

        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                board = newBoard,
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "خانه‌ای از ${cell.name} فروخته شد. ${refund}$ دریافت کردی."
            ), null
        )
    }

    // ========================== Bank Actions ==========================

    private fun handleTakeLoan(
        state: MonopolyState,
        player: PlayerId,
        amount: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val totalPayback = (amount * 1.3).toInt()
        ps.loan = Loan(amount, totalPayback, 0.3)
        ps.cash += amount
        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "Loan of $amount$ received. Payback $totalPayback$ from future salaries."
            ), null
        )
    }

    private fun handleMortgage(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val cell = MonopolyBoardData.cells[propIndex]
        val mortgageAmount = (cell.price * 0.425).toInt()
        ps.cash += mortgageAmount
        val newBoard = state.board.toMutableList()
        newBoard[propIndex] = newBoard[propIndex].copy(mortgaged = true)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                board = newBoard,
                message = "Mortgaged ${cell.name} for $mortgageAmount$"
            ), null
        )
    }

    private fun handleUnmortgage(
        state: MonopolyState,
        player: PlayerId,
        propIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val cell = MonopolyBoardData.cells[propIndex]
        val amount = (cell.price * 0.85).toInt() * 0.5
        val cost = (amount * 1.05).toInt()
        if (ps.cash < cost) {
            return GameUpdateResult(state.copy(message = "Not enough cash to unmortgage"), null)
        }
        ps.cash -= cost
        val newBoard = state.board.toMutableList()
        newBoard[propIndex] = newBoard[propIndex].copy(mortgaged = false)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                board = newBoard,
                message = "Unmortgaged ${cell.name} for $cost$"
            ), null
        )
    }

    private fun handleMakeInvestment(
        state: MonopolyState,
        player: PlayerId,
        amount: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        ps.cash -= amount
        val returnAmount = (amount * 1.15).toInt()
        ps.investments.add(Investment(amount, 2, returnAmount))
        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "Invested $amount$ for 2 rounds. Will return $returnAmount$ after two passes of Start."
            ), null
        )
    }

    // ========================== Transport ==========================

    private fun handleUseTransport(
        state: MonopolyState,
        player: PlayerId,
        destIndex: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        val fromIdx = ps.position
        val destCell = MonopolyBoardData.cells[destIndex]
        val fromCell = MonopolyBoardData.cells[fromIdx]
        if (state.board[destIndex].ownerId == null || state.board[fromIdx].ownerId == null) {
            return GameUpdateResult(state.copy(message = "Transport not available (no owner)"), null)
        }
        val cost = calculateTransportCost(fromIdx, destIndex)
        if (ps.cash < cost) {
            return GameUpdateResult(state.copy(message = "Insufficient cash for transport"), null)
        }
        ps.cash -= cost
        val commission = (cost * 0.2).toInt()
        val ownerFrom = state.board[fromIdx].ownerId!!
        val ownerTo = state.board[destIndex].ownerId!!
        val fromState = state.playerStates[ownerFrom]!!
        val toState = state.playerStates[ownerTo]!!
        fromState.cash += commission / 2
        toState.cash += commission / 2

        ps.position = destIndex
        val next = getNextPlayer(state, player)
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply {
                    this[player] = ps
                    this[ownerFrom] = fromState
                    this[ownerTo] = toState
                },
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "Traveled from ${fromCell.name} to ${destCell.name} for $cost$"
            ), null
        )
    }

    // ========================== Trade (Deal Property) ==========================

    private fun handleProposeTrade(
        state: MonopolyState,
        player: PlayerId,
        action: MonopolyAction.ProposeTrade
    ): GameUpdateResult<MonopolyState, GameResult> {
        if (state.pendingTrade != null) {
            return GameUpdateResult(state.copy(message = "در حال حاضر معامله دیگری در جریان است"), null)
        }
        val tradeId = "${player.value}_${action.targetPlayerId}_${System.currentTimeMillis()}"
        val proposal = TradeProposal(
            id = tradeId,
            fromPlayer = player,
            toPlayer = action.targetPlayerId,
            offeredCash = action.offeredCash,
            offeredProperties = action.offeredProperties,
            requestedCash = 0,
            requestedProperties = emptyList(),
            status = TradeStatus.PENDING
        )
        return GameUpdateResult(
            state.copy(
                pendingTrade = proposal,
                turnPhase = TurnPhase.AWAITING_TRADE,
                message = "Trade proposed to ${state.playerNames[action.targetPlayerId]}"
            ), null
        )
    }

    private fun handleCounterTrade(
        state: MonopolyState,
        player: PlayerId,
        action: MonopolyAction.CounterTrade
    ): GameUpdateResult<MonopolyState, GameResult> {
        val trade = state.pendingTrade ?: return GameUpdateResult(state, null)
        if (trade.fromPlayer != player) return GameUpdateResult(state, null)
        trade.requestedCash = action.requestedCash
        trade.requestedProperties = action.requestedProperties
        return GameUpdateResult(
            state.copy(pendingTrade = trade, message = "پیشنهاد متقابل ارسال شد. منتظر پاسخ حریف."),
            null
        )
    }

    private fun handleAcceptTrade(
        state: MonopolyState,
        player: PlayerId,
        tradeId: String
    ): GameUpdateResult<MonopolyState, GameResult> {
        val trade = state.pendingTrade ?: return GameUpdateResult(state.copy(message = "معامله‌ای در جریان نیست"), null)
        if (trade.toPlayer != player || trade.id != tradeId) return GameUpdateResult(state.copy(message = "معامله نامعتبر"), null)
        if (trade.status != TradeStatus.PENDING) return GameUpdateResult(state.copy(message = "معامله قبلاً انجام شده"), null)

        val fromPlayer = trade.fromPlayer
        val toPlayer = trade.toPlayer
        val fromState = state.playerStates[fromPlayer]!!
        val toState = state.playerStates[toPlayer]!!

        fromState.cash -= trade.offeredCash
        toState.cash += trade.offeredCash
        toState.cash -= trade.requestedCash
        fromState.cash += trade.requestedCash

        trade.offeredProperties.forEach { prop ->
            fromState.ownedProperties.remove(prop)
            toState.ownedProperties.add(prop)
            state.board[prop].ownerId = toPlayer
        }
        trade.requestedProperties.forEach { prop ->
            toState.ownedProperties.remove(prop)
            fromState.ownedProperties.add(prop)
            state.board[prop].ownerId = fromPlayer
        }

        val next = getNextPlayer(state, fromPlayer)

        val newState = state.copy(
            isTradeActive = false,
            pendingTrade = null,
            tradeProposals = emptyMap(),
            selectedProposalFrom = null,
            tradeStep = TradeStep.IDLE,
            turnPhase = TurnPhase.WAITING_FOR_ROLL,
            playerStates = state.playerStates.toMutableMap().apply {
                this[fromPlayer] = fromState
                this[toPlayer] = toState
            },
            currentPlayer = next,
            message = "معامله بین ${state.playerNames[fromPlayer]} و ${state.playerNames[toPlayer]} با موفقیت انجام شد."
        )

        // رویداد معامله کامل شده
        val tradePair = Pair(fromPlayer, toPlayer)
        val stateAfterMission = updateMissionProgress(newState, MissionEvent.TRADE_COMPLETED, null, tradePair)
        return GameUpdateResult(stateAfterMission, null)
    }

    private fun handleRejectTrade(
        state: MonopolyState,
        player: PlayerId,
        tradeId: String
    ): GameUpdateResult<MonopolyState, GameResult> {
        val trade = state.pendingTrade ?: return GameUpdateResult(state.copy(message = "معامله‌ای در جریان نیست"), null)
        if (trade.toPlayer != player || trade.id != tradeId) return GameUpdateResult(state.copy(message = "معامله نامعتبر"), null)
        if (trade.status != TradeStatus.PENDING) return GameUpdateResult(state.copy(message = "معامله قبلاً انجام شده"), null)

        val next = getNextPlayer(state, trade.fromPlayer)

        return GameUpdateResult(
            state.copy(
                isTradeActive = false,
                pendingTrade = null,
                tradeProposals = emptyMap(),
                selectedProposalFrom = null,
                currentPlayer = next,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                tradeStep = TradeStep.IDLE,
                message = "معامله توسط ${state.playerNames[player]} رد شد."
            ), null
        )
    }

    private fun handleSubmitTradeProposal(
        state: MonopolyState,
        player: PlayerId,
        action: MonopolyAction.SubmitTradeProposal
    ): GameUpdateResult<MonopolyState, GameResult> {
        if (state.tradeStep != TradeStep.AWAITING_PROPOSALS) {
            return GameUpdateResult(state.copy(message = "در حال حاضر در مرحله دریافت پیشنهاد نیستیم"), null)
        }
        val currentTrader = state.currentPlayer ?: return GameUpdateResult(state, null)
        if (player == currentTrader) {
            return GameUpdateResult(state.copy(message = "شما نمی‌توانید به خودتان پیشنهاد دهید"), null)
        }
        val ps = state.playerStates[player]!!
        if (ps.cash < action.offeredCash) {
            return GameUpdateResult(state.copy(message = "پول کافی ندارید"), null)
        }
        if (action.offeredProperties.any { !ps.ownedProperties.contains(it) }) {
            return GameUpdateResult(state.copy(message = "یکی از املاک پیشنهادی متعلق به شما نیست"), null)
        }
        val proposal = TradeProposal(
            id = "${player.value}_${System.currentTimeMillis()}",
            fromPlayer = player,
            toPlayer = currentTrader,
            offeredCash = action.offeredCash,
            offeredProperties = action.offeredProperties,
            requestedCash = 0,
            requestedProperties = emptyList(),
            status = TradeStatus.PENDING
        )
        val newProposals = state.tradeProposals.toMutableMap()
        newProposals[player] = proposal

        val newState = state.copy(
            tradeProposals = newProposals,
            message = "پیشنهاد شما به ${state.playerNames[currentTrader]} ارسال شد."
        )
        return GameUpdateResult(newState, null)
    }

    private fun handleSelectTradeProposal(
        state: MonopolyState,
        player: PlayerId,
        action: MonopolyAction.SelectTradeProposal
    ): GameUpdateResult<MonopolyState, GameResult> {
        if (state.currentPlayer != player) return GameUpdateResult(state, null)
        if (state.tradeStep != TradeStep.AWAITING_PROPOSALS) return GameUpdateResult(state, null)
        if (!state.tradeProposals.containsKey(action.proposerId)) return GameUpdateResult(state, null)
        return GameUpdateResult(
            state.copy(
                selectedProposalFrom = action.proposerId,
                tradeStep = TradeStep.AWAITING_COUNTER,
                message = "شما پیشنهاد ${state.playerNames[action.proposerId]} را انتخاب کردید. حالا ضدپیشنهاد خود را بنویسید."
            ), null
        )
    }

    private fun handleMakeCounterOffer(
        state: MonopolyState,
        player: PlayerId,
        action: MonopolyAction.MakeCounterOffer
    ): GameUpdateResult<MonopolyState, GameResult> {
        if (state.currentPlayer != player) return GameUpdateResult(state, null)
        if (state.tradeStep != TradeStep.AWAITING_COUNTER) return GameUpdateResult(state, null)
        val targetId = action.targetPlayerId
        val originalProposal = state.tradeProposals[targetId] ?: return GameUpdateResult(state, null)

        val aState = state.playerStates[player]!!
        if (aState.cash < action.requestedCash) {
            return GameUpdateResult(state.copy(message = "پول کافی برای پیشنهاد نقدی ندارید."), null)
        }
        if (action.requestedProperties.any { !aState.ownedProperties.contains(it) }) {
            return GameUpdateResult(state.copy(message = "یکی از املاک پیشنهادی متعلق به شما نیست."), null)
        }

        val finalTrade = TradeProposal(
            id = originalProposal.id,
            fromPlayer = player,
            toPlayer = targetId,
            offeredCash = action.requestedCash,
            offeredProperties = action.requestedProperties,
            requestedCash = originalProposal.offeredCash,
            requestedProperties = originalProposal.offeredProperties,
            status = TradeStatus.PENDING
        )

        return GameUpdateResult(
            state.copy(
                pendingTrade = finalTrade,
                tradeStep = TradeStep.AWAITING_RESPONSE,
                message = "ضدپیشنهاد به ${state.playerNames[targetId]} ارسال شد. منتظر پاسخ بمانید."
            ), null
        )
    }

    // ========================== Helper Cards & Innate Shield ==========================

    private fun handleUseHelperCard(state: MonopolyState, player: PlayerId, card: HelperCard): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        if (!ps.helperCards.contains(card)) return GameUpdateResult(state, null)
        when (card) {
            HelperCard.GET_OUT_OF_JAIL -> {
                if (ps.jailTurns > 0) {
                    ps.jailTurns = 0
                    ps.helperCards.remove(card)
                    return GameUpdateResult(state.copy(
                        playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                        message = "کارت فرار از زندان استفاده شد. شما آزاد شدید."
                    ), null)
                }
            }
            HelperCard.SHIELD -> {
                ps.shieldActiveUntilStart = true
                ps.helperCards.remove(card)
                val newState = state.copy(
                    playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                    message = "سپر فعال شد. تا عبور از شروع از اجاره در امان خواهید بود."
                )
                val stateAfterMission = updateMissionProgress(newState, MissionEvent.USE_HELPER_CARD, player, null)
                return GameUpdateResult(stateAfterMission, null)
            }
        }
        return GameUpdateResult(state, null)
    }

    private fun handleUseInnateShield(
        state: MonopolyState,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        ps.innateShieldUsed = true
        return GameUpdateResult(
            state.copy(
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                message = "Innate shield activated for this round"
            ), null
        )
    }

    // ========================== Chance Cards ==========================

    private fun drawChanceCard(
        state: MonopolyState,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        var deck = state.chanceDeck.toMutableList()
        if (deck.isEmpty()) {
            deck = state.chanceDiscard.shuffled().toMutableList()
            val newState = state.copy(chanceDeck = deck, chanceDiscard = emptyList())
            return drawChanceCard(newState, player)
        }
        val card = deck.removeAt(0)
        val discard = state.chanceDiscard.toMutableList()

        val (intermediateState, effectMessage) = applyChanceCardEffect(state, player, card)

        val ps = intermediateState.playerStates[player]!!
        if (ps.cash < 0) {
            return handleBankruptcy(intermediateState, player, "ورشکستگی بر اثر کارت شانس")
        }

        if (card.keepCard) {
            return GameUpdateResult(
                intermediateState.copy(
                    chanceDeck = deck,
                    chanceDiscard = discard,
                    message = "شانس: ${card.description} - $effectMessage"
                ), null
            )
        } else {
            discard.add(card)
            return GameUpdateResult(
                intermediateState.copy(
                    chanceDeck = deck,
                    chanceDiscard = discard,
                    message = "شانس: ${card.description} - $effectMessage"
                ), null
            )
        }
    }

    private fun applyChanceCardEffectAndGetState(state: MonopolyState, player: PlayerId): Pair<MonopolyState, String> {
        var deck = state.chanceDeck.toMutableList()
        if (deck.isEmpty()) {
            deck = state.chanceDiscard.shuffled().toMutableList()
            val tempState = state.copy(chanceDeck = deck, chanceDiscard = emptyList())
            return applyChanceCardEffectAndGetState(tempState, player)
        }
        val card = deck.removeAt(0)
        val discard = state.chanceDiscard.toMutableList()
        val (newState, effectMessage) = applyChanceCardEffect(state, player, card)
        if (card.keepCard) {
            return Pair(newState.copy(chanceDeck = deck, chanceDiscard = discard), effectMessage)
        } else {
            discard.add(card)
            return Pair(newState.copy(chanceDeck = deck, chanceDiscard = discard), effectMessage)
        }
    }

    private fun applyChanceCardEffect(
        state: MonopolyState,
        player: PlayerId,
        card: ChanceCard
    ): Pair<MonopolyState, String> {
        val ps = state.playerStates[player]!!
        val pName = state.playerNames[player] ?: player.value
        var newState = state
        var effectMessage = ""

        when (card.condition) {
            "cash<400" -> if (ps.cash >= 400) return Pair(state, "شرایط کارت برآورده نشد (نقدینگی زیر 400 نیست)")
            "cash>700" -> if (ps.cash <= 700) return Pair(state, "شرایط کارت برآورده نشد (نقدینگی بالای 700 نیست)")
            "cash<0" -> if (ps.cash >= 0) return Pair(state, "شرایط کارت برآورده نشد (نقدینگی منفی نیست)")
            "cash>=100" -> if (ps.cash < 100) return Pair(state, "شرایط کارت برآورده نشد (نقدینگی کمتر از 100 است)")
            "hasProperties" -> if (ps.ownedProperties.isEmpty()) return Pair(state, "شرایط کارت برآورده نشد (ملکی ندارید)")
            "anyPlayerInJail" -> if (state.playerStates.none { it.value.jailTurns > 0 }) return Pair(state, "شرایط کارت برآورده نشد (هیچکس در زندان نیست)")
            "otherPlayerOutsideJail" -> {
                val othersOutside = state.players.filter { it != player && state.playerStates[it]!!.jailTurns == 0 }
                if (othersOutside.isEmpty()) return Pair(state, "شرایط کارت برآورده نشد (همه بازیکنان در زندان هستند)")
            }
            "lowestAssets" -> {
                val myAssets = ps.cash + ps.ownedProperties.sumOf { MonopolyBoardData.cells[it].price }
                val isLowest = state.playerStates.all { (id, p) ->
                    id == player || (p.cash + p.ownedProperties.sumOf { MonopolyBoardData.cells[it].price }) >= myAssets
                }
                if (!isLowest) return Pair(state, "شرایط کارت برآورده نشد (بازیکن با کمترین دارایی نیستید)")
            }
        }

        when (card.effect) {
            "MONEY" -> {
                ps.cash += card.amount
                effectMessage = "$pName ${card.amount}$ دریافت کرد."
            }
            "MONEY_RANDOM" -> {
                val amount = listOf(50, 100, 150, 200).random(random)
                ps.cash += amount
                effectMessage = "$pName $amount$ پاداش دریافت کرد."
            }
            "MONEY_RANDOM_NEG" -> {
                val amount = listOf(50, 100, 150, 200).random(random)
                ps.cash -= amount
                effectMessage = "$pName $amount$ جریمه پرداخت کرد."
            }
            "TAX_PERCENT" -> {
                val percent = random.nextInt(20, 31)
                val tax = (ps.cash * percent / 100).toInt()
                ps.cash -= tax
                effectMessage = "$pName $percent% از نقدینگی ($tax$) را جریمه پرداخت کرد."
            }
            "GO_TO_JAIL" -> {
                ps.jailTurns = if (ps.jailFirstTime) 1 else 3
                ps.jailFirstTime = false
                ps.position = 10
                effectMessage = "$pName به زندان رفت."
            }
            "SEND_TO_JAIL" -> {
                val turns = card.amount
                val candidates = state.players.filter { it != player && state.playerStates[it]!!.jailTurns == 0 }
                if (candidates.isNotEmpty()) {
                    val target = if (card.target == "randomPlayer") candidates.random(random) else candidates.first()
                    val targetPs = state.playerStates[target]!!
                    targetPs.jailTurns = turns
                    targetPs.jailFirstTime = false
                    targetPs.position = 10
                    effectMessage = "${state.playerNames[target]} به مدت $turns دور به زندان فرستاده شد."
                } else {
                    effectMessage = "هیچ بازیکن مناسبی برای زندانی کردن وجود ندارد."
                }
            }
            "ALL_GO_TO_JAIL" -> {
                val turns = card.amount
                state.players.filter { it != player }.forEach { p ->
                    val pPs = state.playerStates[p]!!
                    pPs.jailTurns = turns
                    pPs.jailFirstTime = false
                    pPs.position = 10
                }
                effectMessage = "همه بازیکنان دیگر به مدت $turns دور به زندان رفتند."
            }
            "RELEASE_ALL_JAIL" -> {
                state.playerStates.values.forEach { if (it.jailTurns > 0) it.jailTurns = 0 }
                effectMessage = "همه زندانیان آزاد شدند."
            }
            "MOVE_RANDOM" -> {
                val steps = random.nextInt(1, 13)
                var newPos = (ps.position + steps) % MonopolyBoardData.CELL_COUNT
                val passedStart = ps.position + steps >= MonopolyBoardData.CELL_COUNT
                if (passedStart) {
                    ps.cash += state.settings.startSalary
                    effectMessage = "از شروع عبور کردی و حقوق دریافت کردی. "
                }
                ps.position = newPos
                effectMessage += "به خانه ${MonopolyBoardData.cells[newPos].name} منتقل شدی."
            }
            "MOVE_TO_START" -> {
                ps.position = 0
                effectMessage = "به خانه شروع برگشتی."
            }
            "TELEPORT_TO_PLAYER" -> {
                val others = state.players.filter { it != player }
                if (others.isNotEmpty()) {
                    val target = others.random(random)
                    val targetPos = state.playerStates[target]!!.position
                    ps.position = targetPos
                    effectMessage = "به خانه ${state.playerNames[target]} (${MonopolyBoardData.cells[targetPos].name}) منتقل شدی."
                } else {
                    effectMessage = "هیچ بازیکن دیگری وجود ندارد."
                }
            }
            "FREE_TRADE" -> {
                newState = newState.copy(
                    turnPhase = TurnPhase.AWAITING_TRADE,
                    tradeStep = TradeStep.AWAITING_PROPOSALS,
                    isTradeActive = true,
                    tradeProposals = emptyMap(),
                    selectedProposalFrom = null,
                    pendingTrade = null,
                    message = "کارت معامله کن فعال شد."
                )
                effectMessage = "می‌توانی همین الان با یک بازیکن معامله کنی."
            }
            "RESERVE_PROPERTY" -> {
                val emptyProps = MonopolyBoardData.cells.indices.filter { state.board[it].ownerId == null }
                if (emptyProps.isNotEmpty()) {
                    val prop = emptyProps.random(random)
                    ps.reservedPropertyIndex = prop
                    ps.reservedRoundsLeft = card.amount
                    effectMessage = "ملک ${MonopolyBoardData.cells[prop].name} برای ${card.amount} دور رزرو شد (فقط تو می‌توانی بخری)."
                } else {
                    effectMessage = "هیچ ملک بی‌صاحبی برای رزرو وجود ندارد."
                }
            }
            "EXTRA_DICE_NEXT" -> {
                ps.extraDiceNext = true
                effectMessage = "در دور بعد، می‌توانی ۲ تاس بندازی و یکی را انتخاب کنی."
            }
            "BAN_TRADE" -> {
                ps.banTradeUntilStart = true
                effectMessage = "تا زمانی که از شروع عبور نکنی، نمی‌توانی معامله کنی."
            }
            "BAN_BUY" -> {
                ps.banBuyUntilStart = true
                effectMessage = "تا زمانی که از شروع عبور نکنی، نمی‌توانی ملک بخری."
            }
            "FREE_PROPERTY_NEXT" -> {
                ps.freePropertyNext = true
                effectMessage = "در حرکت بعدی، اگر روی ملک بی‌صاحب بایستی، رایگان صاحبش می‌شوی."
            }
            "RENT_BOOST" -> {
                ps.rentBoostPercent = card.amount
                effectMessage = "تا نوبت بعد، اجاره املاکت ${card.amount}% بیشتر می‌شود."
            }
            "RENT_REDUCE" -> {
                ps.rentBoostPercent = -card.amount
                effectMessage = "تا نوبت بعد، اجاره املاکت ${card.amount}% کمتر می‌شود."
            }
            "RENT_ZERO" -> {
                ps.rentBoostPercent = -100
                effectMessage = "تا نوبت بعد، اجاره املاکت صفر می‌شود."
            }
            "ALL_PAY_BANK" -> {
                val amount = listOf(50, 100, 150, 200).random(random)
                state.players.filter { it != player }.forEach { p ->
                    val pPs = state.playerStates[p]!!
                    pPs.cash = maxOf(pPs.cash - amount, -pPs.cash)
                }
                effectMessage = "همه بازیکنان دیگر $amount$ به بانک پرداخت کردند."
            }
            "COLLECT_FROM_ALL" -> {
                val amount = random.nextInt(10, 101)
                var totalCollected = 0
                state.players.filter { it != player }.forEach { p ->
                    val pPs = state.playerStates[p]!!
                    val deduct = minOf(amount, pPs.cash)
                    pPs.cash -= deduct
                    totalCollected += deduct
                }
                ps.cash += totalCollected
                effectMessage = "از هر بازیکن $amount$ گرفته شد و جمعاً $totalCollected$ به تو اضافه شد."
            }
            "TAKE_FROM_RICHEST" -> {
                val richest = state.playerStates.maxByOrNull { it.value.cash }?.key
                if (richest != null && richest != player) {
                    val amount = listOf(100, 150, 200).random(random)
                    val richestPs = state.playerStates[richest]!!
                    val actualTake = minOf(amount, richestPs.cash)
                    richestPs.cash -= actualTake
                    ps.cash += actualTake
                    effectMessage = "$actualTake$ از ${state.playerNames[richest]} گرفتی."
                } else {
                    effectMessage = "هیچ بازیکن دیگری وجود ندارد."
                }
            }
            "GIVE_TO_POOREST" -> {
                val poorest = state.playerStates.filter { it.key != player }.minByOrNull { it.value.cash }?.key
                if (poorest != null) {
                    val amount = card.amount
                    if (ps.cash >= amount) {
                        ps.cash -= amount
                        state.playerStates[poorest]!!.cash += amount
                        effectMessage = "$amount$ به ${state.playerNames[poorest]} دادی."
                    } else {
                        effectMessage = "پول کافی برای باج دادن نداری."
                    }
                } else {
                    effectMessage = "هیچ بازیکن دیگری وجود ندارد."
                }
            }
            "GIVE_TO_ALL" -> {
                val amount = random.nextInt(25, 76)
                var totalGiven = 0
                state.players.filter { it != player }.forEach { p ->
                    if (ps.cash >= amount) {
                        ps.cash -= amount
                        state.playerStates[p]!!.cash += amount
                        totalGiven += amount
                    } else {
                        effectMessage = "پول کافی برای پرداخت به همه نداری."
                    }
                }
                if (totalGiven > 0) effectMessage = "به هر بازیکن $amount$ دادی. جمعاً $totalGiven$ پرداخت شد."
            }
            "START_MULTIPLIER" -> {
                ps.startMultiplier = card.amount
                effectMessage = "دفعه بعد که به شروع برسی، ${card.amount} برابر جایزه می‌گیری."
            }
            "EXTRA_TURN" -> {
                newState = newState.copy(currentPlayer = player, turnPhase = TurnPhase.WAITING_FOR_ROLL)
                effectMessage = "یک نوبت دیگر داری (می‌توانی دوباره تاس بندازی)."
            }
            "BANK_PAYS_RENT" -> {
                ps.bankPaysRent = true
                effectMessage = "تا رسیدن به شروع، وقتی روی ملک خودت بایستی بانک به تو اجاره می‌دهد."
            }
            "PAY_RENT_TO_BANK" -> {
                ps.payRentToBank = true
                effectMessage = "تا رسیدن به شروع، وقتی روی ملک خودت بایستی به بانک اجاره می‌دهی."
            }
            "FORCE_TRANSPORT" -> {
                effectMessage = "باید در همین نوبت از یکی از وسایل نقلیه سفر کنی (هزینه پرداخت می‌کنی)."
            }
            "TAX_PER_PROPERTY" -> {
                val taxPerProp = card.amount
                val totalTax = ps.ownedProperties.size * taxPerProp
                if (ps.cash >= totalTax) {
                    ps.cash -= totalTax
                    effectMessage = "$totalTax$ (${ps.ownedProperties.size} ملک × $taxPerProp$) جریمه پرداخت شد."
                } else {
                    effectMessage = "پول کافی برای پرداخت مالیات ملکی نداری! ورشکست می‌شوی."
                    return Pair(state, effectMessage)
                }
            }
            "PAY_LOAN" -> {
                if (ps.loan != null && ps.loan!!.remaining > 0) {
                    if (ps.cash >= ps.loan!!.remaining) {
                        ps.cash -= ps.loan!!.remaining
                        ps.loan = null
                        effectMessage = "تمام وام خود را پرداخت کردی."
                    } else {
                        effectMessage = "پول کافی برای تسویه وام نداری!"
                    }
                } else {
                    effectMessage = "وام نداری."
                }
            }
            "SHARE_RENT" -> {
                val others = state.players.filter { it != player }
                if (others.isNotEmpty()) {
                    val partner = others.random(random)
                    ps.sharedRentPartner = partner
                    ps.sharedRentRoundsLeft = card.amount
                    effectMessage = "با ${state.playerNames[partner]} به مدت ${card.amount} دور شراکت اجاره داری."
                } else {
                    effectMessage = "هیچ بازیکن دیگری وجود ندارد."
                }
            }
            "SELL_ONE_PROPERTY" -> {
                if (ps.ownedProperties.isNotEmpty()) {
                    val propIdx = ps.ownedProperties.random(random)
                    val sellPrice = (MonopolyBoardData.cells[propIdx].price * 0.85).toInt()
                    ps.cash += sellPrice
                    ps.ownedProperties.remove(propIdx)
                    val newBoard = state.board.toMutableList()
                    newBoard[propIdx] = PropertyState()
                    newState = newState.copy(board = newBoard)
                    effectMessage = "ملک ${MonopolyBoardData.cells[propIdx].name} به قیمت $sellPrice$ فروخته شد."
                } else {
                    effectMessage = "ملکی برای فروش نداری."
                }
            }
            "GAIN_HELPER_CARD" -> {
                val cardType = when (card.effectParam) {
                    "SHIELD" -> HelperCard.SHIELD
                    "GET_OUT_OF_JAIL" -> HelperCard.GET_OUT_OF_JAIL
                    else -> return Pair(state, "کارت کمکی نامشخص")
                }
                ps.helperCards.add(cardType)
                effectMessage = "کارت ${cardType.name} به کارت‌های کمکی شما اضافه شد."
            }
            else -> effectMessage = "بدون اثر"
        }

        val newPlayerStates = newState.playerStates.toMutableMap()
        newPlayerStates[player] = ps
        newState = newState.copy(playerStates = newPlayerStates)

        return Pair(newState, effectMessage)
    }

    // ========================== Missions (Full Implementation) ==========================

    private fun startRandomMission(state: MonopolyState, activator: PlayerId): Pair<MonopolyState, String> {
        val missionId = random.nextInt(1, 25)
        val params = generateMissionParams(missionId, state)
        val (name, desc, reward, penalty, isGlobal) = getMissionDetails(missionId, params)
        val playersData = state.players.associateWith { MissionPlayerData() }.toMutableMap()
        val mission = MissionState(
            id = missionId,
            name = name,
            description = desc,
            maxRounds = 5,
            currentRound = 0,
            params = params,
            playersData = playersData,
            completedPlayers = mutableSetOf(),
            rewardAmount = reward,
            penaltyAmount = penalty,
            isGlobalReward = isGlobal
        )
        for (player in state.players) {
            val ps = state.playerStates[player]!!
            mission.playersData[player]!!.totalCashAtStart = ps.cash
            mission.playersData[player]!!.totalAssetsAtStart = ps.cash + ps.ownedProperties.sumOf { MonopolyBoardData.cells[it].price }
        }
        val (newState, startMsg) = applyMissionStartEffect(state, mission)
        return Pair(newState.copy(activeMission = mission), startMsg)
    }

    private fun generateMissionParams(missionId: Int, state: MonopolyState): MissionParams {
        val emptyProps = MonopolyBoardData.cells.indices.filter {
            state.board[it].ownerId == null && MonopolyBoardData.cells[it].price > 0
        }
        return when (missionId) {
            1 -> {
                val target = if (emptyProps.isNotEmpty()) emptyProps.random() else 0
                val fine = (random.nextInt(100, 301) / 50) * 50
                MissionParams(targetPropertyIndex = target, minAmount = fine, maxAmount = fine)
            }
            2 -> {
                val target = if (emptyProps.isNotEmpty()) emptyProps.random() else 0
                val reward = (random.nextInt(100, 301) / 50) * 50
                MissionParams(targetPropertyIndex = target, minAmount = reward, maxAmount = reward)
            }
            3 -> {
                val reward = (random.nextInt(200, 451) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            4 -> {
                val reward = (random.nextInt(100, 301) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            5 -> {
                val target = if (emptyProps.isNotEmpty()) emptyProps.random() else 0
                MissionParams(targetPropertyIndex = target)
            }
            6 -> MissionParams(minAmount = 100, maxAmount = 100)
            7 -> MissionParams()
            8 -> {
                val reward = (random.nextInt(50, 201) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            9 -> {
                val penalty = (random.nextInt(50, 251) / 50) * 50
                MissionParams(minAmount = penalty, maxAmount = penalty)
            }
            10 -> {
                val loan = (random.nextInt(150, 401) / 50) * 50
                MissionParams(loanAmount = loan)
            }
            11 -> {
                val reward = (random.nextInt(50, 151) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            12 -> MissionParams(minAmount = 400, maxAmount = 400)
            13 -> {
                val reward = (random.nextInt(100, 201) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            14 -> {
                val percent = (random.nextInt(20, 51) / 5) * 5
                MissionParams(percent = percent)
            }
            15 -> {
                val percent = (random.nextInt(20, 41) / 5) * 5
                MissionParams(percent = percent)
            }
            16 -> MissionParams()
            17 -> {
                val reward = (random.nextInt(200, 401) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            18 -> {
                val reward = (random.nextInt(100, 201) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            19 -> {
                val reward = (random.nextInt(50, 301) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            20 -> {
                val reward = (random.nextInt(100, 201) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            21 -> {
                val reward = (random.nextInt(100, 301) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            22 -> {
                val reward = (random.nextInt(50, 201) / 50) * 50
                MissionParams(minAmount = reward, maxAmount = reward)
            }
            23 -> {
                val percent = (random.nextInt(20, 41) / 5) * 5
                MissionParams(percent = percent)
            }
            24 -> {
                val percent = (random.nextInt(20, 41) / 5) * 5
                MissionParams(percent = percent)
            }
            else -> MissionParams()
        }
    }

    private fun getMissionDetails(id: Int, params: MissionParams): Tuple4<String, String, Int, Int, Boolean> {
        return when (id) {
            1 -> Tuple4("خرید ممنوع", "خرید ملک ${MonopolyBoardData.cells[params.targetPropertyIndex!!].name} ممنوع! جریمه ${params.minAmount}$", params.minAmount, 0, false)
            2 -> Tuple4("پاداش خرید", "هرکس ملک ${MonopolyBoardData.cells[params.targetPropertyIndex!!].name} را بخرد، ${params.minAmount}$ پاداش می‌گیرد", params.minAmount, 0, true)
            3 -> Tuple4("صبوری", "همه باید از شروع عبور کنند. آخرین نفر ${params.minAmount}$ دریافت می‌کند", params.minAmount, 0, true)
            4 -> Tuple4("سریع‌السیر", "اولین نفری که به شروع برسد، ${params.minAmount}$ دریافت می‌کند", params.minAmount, 0, true)
            5 -> Tuple4("ارث", "ملک ${MonopolyBoardData.cells[params.targetPropertyIndex!!].name} به اولین نفری که روی آن بیفتد تعلق می‌گیرد", 0, 0, true)
            6 -> Tuple4("سرقت", "در پایان، ثروتمندترین بازیکن به هر نفر 100$ می‌دهد", 0, 100, false)
            7 -> Tuple4("دور باطل", "هیچ اجاره‌ای پرداخت نمی‌شود", 0, 0, false)
            8 -> Tuple4("سود ملکی", "بیشترین خرید ملک، پاداش ${params.minAmount}$", params.minAmount, 0, true)
            9 -> Tuple4("مالیات ملکی", "بیشترین تعداد ملک، جریمه ${params.minAmount}$", 0, params.minAmount, false)
            10 -> Tuple4("وام اجباری", "همه ${params.loanAmount}$ دریافت می‌کنند. در پایان باید برگردانند +30% جریمه", 0, 0, false)
            11 -> Tuple4("معاملات جذاب", "هر معامله، طرفین ${params.minAmount}$ پاداش می‌گیرند", params.minAmount, 0, true)
            12 -> Tuple4("دلالی", "بیشترین فروش ملک، ${params.minAmount}$ بین برندگان تقسیم می‌شود", params.minAmount, 0, true)
            13 -> Tuple4("گل ریزون", "رأی‌گیری: برنده ${params.minAmount}$، در صورت تساوی همه -150$", params.minAmount, 150, false)
            14 -> Tuple4("تورم", "قیمت خرید املاک ${params.percent}% افزایش می‌یابد", 0, 0, false)
            15 -> Tuple4("سقوط مسکن", "قیمت خرید املاک ${params.percent}% کاهش می‌یابد", 0, 0, false)
            16 -> Tuple4("رکود معاملاتی", "هیچکس نمی‌تواند ملک بفروشد", 0, 0, false)
            17 -> Tuple4("استفاده از کارت", "بیشترین استفاده از کارت کمکی، پاداش ${params.minAmount}$ تقسیم", params.minAmount, 0, true)
            18 -> Tuple4("گردش مالی", "هرکس یک ملک بخرد و یک ملک بفروشد، ${params.minAmount}$ دریافت می‌کند", params.minAmount, 0, true)
            19 -> Tuple4("حقوق بیکاری", "کمترین نقدینگی، پاداش ${params.minAmount}$", params.minAmount, 0, true)
            20 -> Tuple4("وام خرید خانه", "کمترین ارزش دارایی، پاداش ${params.minAmount}$", params.minAmount, 0, true)
            21 -> Tuple4("مافیا در زندان", "هرکس وارد زندان شود، ${params.minAmount}$ دریافت می‌کند", params.minAmount, 0, true)
            22 -> Tuple4("به سوی شانس", "هرکس روی شانس بیفتد، ${params.minAmount}$ دریافت می‌کند", params.minAmount, 0, true)
            23 -> Tuple4("کاهش قیمت", "قیمت فروش املاک ${params.percent}% کمتر می‌شود", 0, 0, false)
            24 -> Tuple4("افزایش قیمت", "قیمت فروش املاک ${params.percent}% بیشتر می‌شود", 0, 0, false)
            else -> Tuple4("", "", 0, 0, false)
        }
    }

    private fun applyMissionStartEffect(state: MonopolyState, mission: MissionState): Pair<MonopolyState, String> {
        var newState = state
        var msg = ""
        when (mission.id) {
            10 -> {
                val loanAmount = mission.params.loanAmount
                for (player in state.players) {
                    val ps = newState.playerStates[player]!!
                    ps.cash += loanAmount
                    mission.playersData[player]!!.loanTaken = loanAmount
                }
                newState = newState.copy(playerStates = newState.playerStates.toMap())
                msg = "همه بازیکنان ${loanAmount}$ وام دریافت کردند. باید تا پایان مأموریت بازپرداخت کنند."
            }
            14,15 -> {
                msg = "قیمت خرید املاک ${mission.params.percent}% ${if (mission.id == 14) "افزایش" else "کاهش"} یافت."
            }
            7 -> {
                msg = "هیچ اجاره‌ای تا پایان مأموریت پرداخت نمی‌شود."
            }
        }
        return Pair(newState, msg)
    }

    private fun updateMissionProgress(
        state: MonopolyState,
        event: MissionEvent,
        playerId: PlayerId? = null,
        data: Any? = null
    ): MonopolyState {
        val mission = state.activeMission ?: return state
        var newState = state

        when (event) {
            MissionEvent.ROUND_END -> {
                mission.currentRound++
                if (mission.currentRound >= mission.maxRounds) {
                    newState = endMission(newState, mission)
                }
            }
            MissionEvent.BUY_PROPERTY -> {
                if (playerId == null) return state
                val propIndex = data as? Int ?: return state
                val ps = newState.playerStates[playerId]!!
                when (mission.id) {
                    1 -> {
                        if (propIndex == mission.params.targetPropertyIndex) {
                            ps.cash -= mission.params.minAmount
                            mission.completedPlayers.add(playerId)
                            newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps })
                        }
                    }
                    2 -> {
                        if (propIndex == mission.params.targetPropertyIndex) {
                            ps.cash += mission.params.minAmount
                            newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps })
                        }
                    }
                    8 -> {
                        mission.playersData[playerId]!!.buyCount++
                    }
                    18 -> {
                        mission.playersData[playerId]!!.customData["bought"] = 1
                    }
                }
            }
            MissionEvent.SELL_PROPERTY -> {
                if (playerId == null) return state
                when (mission.id) {
                    12 -> {
                        mission.playersData[playerId]!!.sellCount++
                    }
                    18 -> {
                        mission.playersData[playerId]!!.customData["sold"] = 1
                    }
                }
            }
            MissionEvent.TRADE_COMPLETED -> {
                if (data !is Pair<*, *>) return state
                val p1 = data.first as? PlayerId ?: return state
                val p2 = data.second as? PlayerId ?: return state
                when (mission.id) {
                    11 -> {
                        val reward = mission.params.minAmount
                        val ps1 = newState.playerStates[p1]!!
                        val ps2 = newState.playerStates[p2]!!
                        ps1.cash += reward
                        ps2.cash += reward
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply {
                            this[p1] = ps1
                            this[p2] = ps2
                        })
                    }
                }
            }
            MissionEvent.PASS_START -> {
                if (playerId == null) return state
                when (mission.id) {
                    3 -> {
                        mission.playersData[playerId]!!.passedStart = true
                    }
                    4 -> {
                        if (!mission.completedPlayers.contains(playerId)) {
                            mission.completedPlayers.add(playerId)
                            val ps = newState.playerStates[playerId]!!
                            ps.cash += mission.params.minAmount
                            newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps })
                            return newState.copy(activeMission = null)
                        }
                    }
                }
            }
            MissionEvent.LAND_ON_PROPERTY -> {
                if (playerId == null) return state
                val propIndex = data as? Int ?: return state
                if (mission.id == 5 && propIndex == mission.params.targetPropertyIndex) {
                    val board = newState.board.toMutableList()
                    if (board[propIndex].ownerId == null) {
                        val ps = newState.playerStates[playerId]!!
                        board[propIndex] = board[propIndex].copy(ownerId = playerId)
                        ps.ownedProperties.add(propIndex)
                        newState = newState.copy(
                            playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps },
                            board = board
                        )
                    }
                    return newState.copy(activeMission = null)
                }
            }
            MissionEvent.LAND_ON_CHANCE -> {
                if (playerId == null) return state
                if (mission.id == 22) {
                    val ps = newState.playerStates[playerId]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps })
                }
            }
            MissionEvent.GO_TO_JAIL -> {
                if (playerId == null) return state
                if (mission.id == 21) {
                    val ps = newState.playerStates[playerId]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[playerId] = ps })
                }
            }
            MissionEvent.USE_HELPER_CARD -> {
                if (playerId == null) return state
                if (mission.id == 17) {
                    mission.playersData[playerId]!!.helperCardUsed++
                }
            }
            else -> {}
        }
        return newState
    }

    private fun endMission(state: MonopolyState, mission: MissionState): MonopolyState {
        var newState = state
        val players = state.players.filter { !state.playerStates[it]!!.isBankrupt }

        when (mission.id) {
            3 -> {
                val notPassed = players.filter { !mission.playersData[it]!!.passedStart }
                if (notPassed.size == 1) {
                    val winner = notPassed.first()
                    val ps = newState.playerStates[winner]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[winner] = ps })
                }
            }
            6 -> {
                val richest = players.maxByOrNull { state.playerStates[it]!!.cash }
                if (richest != null) {
                    val richestPs = newState.playerStates[richest]!!
                    val others = players.filter { it != richest }
                    val total = 100 * others.size
                    if (richestPs.cash >= total) {
                        richestPs.cash -= total
                        others.forEach { p ->
                            val ps = newState.playerStates[p]!!
                            ps.cash += 100
                            newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[p] = ps })
                        }
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[richest] = richestPs })
                    }
                }
            }
            8 -> {
                val winner = players.maxByOrNull { mission.playersData[it]!!.buyCount }
                if (winner != null && mission.playersData[winner]!!.buyCount > 0) {
                    val ps = newState.playerStates[winner]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[winner] = ps })
                }
            }
            9 -> {
                val maxPropsPlayer = players.maxByOrNull { state.playerStates[it]!!.ownedProperties.size }
                if (maxPropsPlayer != null) {
                    val ps = newState.playerStates[maxPropsPlayer]!!
                    ps.cash -= mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[maxPropsPlayer] = ps })
                }
            }
            10 -> {
                for (player in players) {
                    val ps = newState.playerStates[player]!!
                    val borrowed = mission.playersData[player]!!.loanTaken
                    if (borrowed > 0) {
                        if (ps.cash >= borrowed) {
                            ps.cash -= borrowed
                        } else {
                            val penalty = (borrowed * 0.3).toInt()
                            ps.cash -= (borrowed + penalty)
                        }
                    }
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[player] = ps })
                }
            }
            12 -> {
                val maxSell = players.maxOfOrNull { mission.playersData[it]!!.sellCount } ?: 0
                if (maxSell > 0) {
                    val winners = players.filter { mission.playersData[it]!!.sellCount == maxSell }
                    val share = mission.params.minAmount / winners.size
                    winners.forEach { p ->
                        val ps = newState.playerStates[p]!!
                        ps.cash += share
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[p] = ps })
                    }
                }
            }
            13 -> {
                val randomWinner = players.randomOrNull()
                if (randomWinner != null) {
                    val ps = newState.playerStates[randomWinner]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[randomWinner] = ps })
                } else {
                    for (p in players) {
                        val ps = newState.playerStates[p]!!
                        ps.cash -= mission.penaltyAmount
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[p] = ps })
                    }
                }
            }
            17 -> {
                val maxUse = players.maxOfOrNull { mission.playersData[it]!!.helperCardUsed } ?: 0
                if (maxUse > 0) {
                    val winners = players.filter { mission.playersData[it]!!.helperCardUsed == maxUse }
                    val share = mission.params.minAmount / winners.size
                    winners.forEach { p ->
                        val ps = newState.playerStates[p]!!
                        ps.cash += share
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[p] = ps })
                    }
                }
            }
            18 -> {
                for (p in players) {
                    val data = mission.playersData[p]!!.customData
                    if (data["bought"] == 1 && data["sold"] == 1) {
                        val ps = newState.playerStates[p]!!
                        ps.cash += mission.params.minAmount
                        newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[p] = ps })
                    }
                }
            }
            19 -> {
                val poorest = players.minByOrNull { state.playerStates[it]!!.cash }
                if (poorest != null) {
                    val ps = newState.playerStates[poorest]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[poorest] = ps })
                }
            }
            20 -> {
                val minAsset = players.minByOrNull { p ->
                    val ps = state.playerStates[p]!!
                    ps.cash + ps.ownedProperties.sumOf { MonopolyBoardData.cells[it].price }
                }
                if (minAsset != null) {
                    val ps = newState.playerStates[minAsset]!!
                    ps.cash += mission.params.minAmount
                    newState = newState.copy(playerStates = newState.playerStates.toMutableMap().apply { this[minAsset] = ps })
                }
            }
        }
        return newState.copy(activeMission = null)
    }

    // ========================== Bankruptcy ==========================

    private fun handleBankruptcy(
        state: MonopolyState,
        player: PlayerId,
        reason: String
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        ps.isBankrupt = true
        ps.cash = 0
        val newBoard = state.board.toMutableList()
        for (idx in ps.ownedProperties) {
            newBoard[idx] = PropertyState()
        }
        ps.ownedProperties.clear()
        val newPlayers = state.players.filter { it != player }
        val nextPlayer = if (newPlayers.isNotEmpty()) newPlayers.firstOrNull() else null
        val gameOver = newPlayers.size < 2
        val winner = if (gameOver && newPlayers.isNotEmpty()) newPlayers.first() else null

        return GameUpdateResult(
            state.copy(
                players = newPlayers,
                playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
                board = newBoard,
                currentPlayer = nextPlayer,
                gameOver = gameOver,
                winner = winner,
                message = "بازیکن ${state.playerNames[player]} ورشکست شد و از بازی حذف گردید. دلیل: $reason"
            ), if (winner != null) GameResult.Win(winner) else null
        )
    }

    // ========================== Terminal & Result ==========================

    override fun isTerminal(state: MonopolyState) =
        state.gameOver || state.roundCount >= state.settings.maxRounds

    override fun getResult(state: MonopolyState): GameResult? {
        if (state.winner != null) return GameResult.Win(state.winner!!)
        if (state.roundCount >= state.settings.maxRounds) {
            val winner = state.playerStates.maxByOrNull {
                it.value.cash + it.value.ownedProperties.sumOf { idx ->
                    val prop = state.board[idx]
                    (MonopolyBoardData.cells[idx].price * 0.85).toInt() + prop.improvementValue
                }
            }?.key
            return if (winner != null) GameResult.Win(winner) else null
        }
        return null
    }

    override fun createInitialState(players: List<PlayerId>): MonopolyState {
        val shuffledPlayers = players.shuffled()
        val playerStates = shuffledPlayers.associateWith { PlayerState(playerId = it, name = it.value, cash = 1000) }
        val names = playerStates.mapValues { it.value.name }
        val chanceDeck = createChanceDeck()
        return MonopolyState(
            players = shuffledPlayers,
            playerStates = playerStates,
            playerNames = names,
            currentPlayer = shuffledPlayers.first(),
            message = "بازی شروع شد. نوبت ${names[shuffledPlayers.first()]}",
            settings = GameSettings(startCash = 1000),
            chanceDeck = chanceDeck,
            chanceDiscard = emptyList()
        )
    }

    private fun createChanceDeck(): List<ChanceCard> {
        val cards = listOf(
            ChanceCard(1, "فروش اجباری", "یکی از املاک خود را باید بفروشی", 1, "SELL_ONE_PROPERTY", condition = "hasProperties"),
            ChanceCard(2, "معامله کن", "بدون حرکت می‌توانی با یک نفر معامله کنی", 5, "FREE_TRADE"),
            ChanceCard(3, "برو زندان", "مستقیماً به زندان برو", 12, "GO_TO_JAIL"),
            ChanceCard(4, "جا به جایی", "به یک خانه تصادفی جلوتر منتقل شو", 10, "MOVE_RANDOM"),
            ChanceCard(5, "پاداش بزرگ", "500 دلار دریافت کن", 3, "MONEY", 500, condition = "cash<400"),
            ChanceCard(6, "پاداش", "50 تا 200 دلار پاداش", 10, "MONEY_RANDOM", 0),
            ChanceCard(7, "جریمه بزرگ", "20 تا 30 درصد نقدینگی را از دست بده", 3, "TAX_PERCENT", condition = "cash>700"),
            ChanceCard(8, "جریمه نقدی", "50 تا 200 دلار جریمه", 10, "MONEY_RANDOM_NEG", 0, condition = "cash>=100"),
            ChanceCard(9, "شکایت", "یک بازیکن را به زندان بفرست", 5, "SEND_TO_JAIL", 3, target = "randomPlayer", condition = "otherPlayerOutsideJail"),
            ChanceCard(10, "نشان", "یک ملک بی‌صاحب را برای 2 دور رزرو کن", 3, "RESERVE_PROPERTY", 2),
            ChanceCard(11, "شانس بیشتر", "دور بعد می‌توانی 2 تاس بزنی و یکی را انتخاب کنی", 5, "EXTRA_DICE_NEXT"),
            ChanceCard(12, "ممنوع المعامله", "تا عبور از شروع نمی‌توانی معامله کنی", 5, "BAN_TRADE"),
            ChanceCard(13, "مسدودی", "تا عبور از شروع نمی‌توانی ملک بخری", 2, "BAN_BUY"),
            ChanceCard(14, "ملک مجانی", "در حرکت بعدی اگر روی ملک بی‌صاحب بایستی، رایگان صاحبش می‌شوی", 1, "FREE_PROPERTY_NEXT"),
            ChanceCard(15, "افزایش اجاره", "تا نوبت بعد اجاره املاک شما 25% بیشتر می‌شود", 3, "RENT_BOOST", 25),
            ChanceCard(16, "کاهش اجاره", "تا نوبت بعد اجاره املاک شما 50% کمتر می‌شود", 3, "RENT_REDUCE", 50),
            ChanceCard(17, "بی اجاره", "تا نوبت بعد اجاره املاک شما صفر می‌شود", 1, "RENT_ZERO"),
            ChanceCard(18, "کومونیست", "همه بازیکنان جز شما بین 50 تا 200 دلار به بانک بدهند", 1, "ALL_PAY_BANK", target = "allOthers", condition = "lowestAssets"),
            ChanceCard(19, "باج گیری جمعی", "از هر بازیکن 10 تا 100 دلار بگیر", 2, "COLLECT_FROM_ALL", 0),
            ChanceCard(20, "باج گیری", "از بازیکن ثروتمندترین بین 100 تا 200 بگیر", 5, "TAKE_FROM_RICHEST", 0),
            ChanceCard(21, "باج دادن", "به بازیکن کمترین پول 200 دلار بده", 6, "GIVE_TO_POOREST", 200),
            ChanceCard(22, "مالیات فقرا", "به هر بازیکن 25 تا 75 دلار بده", 4, "GIVE_TO_ALL", 0),
            ChanceCard(23, "نوروز طلایی", "دفعه بعدی که به شروع برسی 3 برابر جایزه می‌گیری", 1, "START_MULTIPLIER", 3, condition = "cash<0"),
            ChanceCard(24, "نوروز", "دفعه بعدی که به شروع برسی 2 برابر جایزه می‌گیری", 3, "START_MULTIPLIER", 2),
            ChanceCard(25, "کودتا", "همه بازیکنان دیگر 3 دور به زندان می‌روند", 1, "ALL_GO_TO_JAIL", 3),
            ChanceCard(26, "تاس مجانی", "می‌توانی دوباره تاس بندازی", 5, "EXTRA_TURN"),
            ChanceCard(27, "ماه ثروت", "تا رسیدن به شروع، وقتی روی ملک خودت بیفتی بانک اجاره می‌دهد", 2, "BANK_PAYS_RENT"),
            ChanceCard(28, "ماه فقر", "تا رسیدن به شروع، روی ملک خودت اجاره به بانک می‌دهی", 2, "PAY_RENT_TO_BANK"),
            ChanceCard(29, "تبعید", "باید از یک وسیله نقلیه سفر کنی (هزینه پرداخت کنی)", 2, "FORCE_TRANSPORT"),
            ChanceCard(30, "شورش در زندان", "همه زندانی‌ها آزاد می‌شوند", 4, "RELEASE_ALL_JAIL", condition = "anyPlayerInJail"),
            ChanceCard(31, "تلپورت", "به خانه یکی از بازیکنان دیگر منتقل شو", 2, "TELEPORT_TO_PLAYER"),
            ChanceCard(32, "مالیات ملکی", "به ازای هر ملک 50 دلار به بانک بپرداز", 4, "TAX_PER_PROPERTY", 50),
            ChanceCard(33, "شروع دوباره", "به خانه شروع بازگرد", 6, "MOVE_TO_START"),
            ChanceCard(34, "شراکت اجباری", "با یک بازیکن به مدت 4 دور اجاره‌ها نصف می‌شود", 5, "SHARE_RENT", 4),
            ChanceCard(35, "تسویه حساب", "کل وام خود را فوراً پرداخت کن", 2, "PAY_LOAN"),
            ChanceCard(101, "سپر", "کارت سپر دریافت می‌کنید", 3, "GAIN_HELPER_CARD", keepCard = true, amount = 1, effectParam = "SHIELD"),
            ChanceCard(102, "فرار از زندان", "کارت فرار از زندان دریافت می‌کنید", 3, "GAIN_HELPER_CARD", keepCard = true, amount = 1, effectParam = "GET_OUT_OF_JAIL")
        )
        val deck = mutableListOf<ChanceCard>()
        cards.forEach { card ->
            repeat(card.weight) {
                deck.add(card.copy(weight = 1))
            }
        }
        return deck.shuffled()
    }

    // ========================== Strategic Dice Helpers ==========================

    private fun generateStrategicDice(): List<Int> = List(3) { random.nextInt(1, 7) }

    private fun handleSelectStrategicDice(
        state: MonopolyState,
        player: PlayerId,
        chosenValue: Int
    ): GameUpdateResult<MonopolyState, GameResult> {
        val ps = state.playerStates[player]!!
        if (!ps.strategicDiceOptions.contains(chosenValue)) {
            return GameUpdateResult(state.copy(message = "انتخاب نامعتبر"), null)
        }
        ps.firstMoveDone = true
        ps.strategicDiceOptions = emptyList()
        val newState = state.copy(
            playerStates = state.playerStates.toMutableMap().apply { this[player] = ps },
            diceResult = listOf(chosenValue)
        )
        return handleMoveWithSelectedDice(newState, player, chosenValue)
    }

    // ========================== Trade Cancel ==========================

    private fun handleCancelTrade(
        state: MonopolyState,
        player: PlayerId
    ): GameUpdateResult<MonopolyState, GameResult> {
        if (state.currentPlayer != player && state.tradeStep != TradeStep.AWAITING_PROPOSALS) {
            return GameUpdateResult(state.copy(message = "شما نمی‌توانید این معامله را لغو کنید."), null)
        }
        val nextPlayer = getNextPlayer(state, state.currentPlayer)
        return GameUpdateResult(
            state.copy(
                isTradeActive = false,
                tradeStep = TradeStep.IDLE,
                tradeProposals = emptyMap(),
                selectedProposalFrom = null,
                pendingTrade = null,
                currentPlayer = nextPlayer,
                turnPhase = TurnPhase.WAITING_FOR_ROLL,
                message = "معامله توسط ${state.playerNames[player]} لغو شد."
            ), null
        )
    }
}