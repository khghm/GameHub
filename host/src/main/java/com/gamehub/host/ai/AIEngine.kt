package com.gamehub.host.ai

import com.gamehub.shared.core.*
import com.gamehub.shared.engines.board.*
import com.gamehub.shared.engines.card.*
import com.gamehub.games.uno.UnoState
import com.gamehub.games.ludo.LudoState
import com.gamehub.games.battleship.*
import com.gamehub.games.soccerstriker.SoccerStrikerAction
import com.gamehub.games.soccerstriker.SoccerStrikerBotStrategy
import com.gamehub.games.soccerstriker.SoccerStrikerState
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AIEngine(
    private val definition: GameDefinition<*, *, *>,
    private val difficulty: Int = 2 // 1=easy, 2=medium, 3=hard
) {
    fun getBestAction(state: GameState, player: PlayerId): GameAction? {
        return when (definition.metadata.id) {
            "tictactoe" -> getTicTacToeAction(state as BoardState, player)
            "connectfour" -> getConnectFourAction(state as BoardState, player)
            "uno" -> getUnoAction(state as UnoState, player)
            "ludo" -> getLudoAction(state as LudoState, player)
            "battleship" -> runBlocking {
                BattleshipBotStrategy().getNextMove(state as BattleshipState, player, difficulty)
            }
//            "soccer-striker" -> runBlocking {
//                SoccerStrikerBotStrategy().getNextMove(state as SoccerStrikerState, player, difficulty)
//            }
            else -> null
        }
    }

    // ===== Tic-Tac-Toe: Minimax کامل =====
    private fun getTicTacToeAction(state: BoardState, player: PlayerId): BoardAction? {
        val emptyCells = mutableListOf<BoardAction>()
        for (r in 0 until 3) for (c in 0 until 3) {
            if (state.grid[r][c] == null) emptyCells.add(BoardAction(r, c))
        }
        if (emptyCells.isEmpty()) return null

        // برای سطوح آسان: حرکت تصادفی با احتمال ۴۰٪
        if (difficulty == 1 && Random.nextFloat() < 0.4f) {
            return emptyCells.random()
        }

        // Minimax با هرس آلفا-بتا
        var bestScore = Int.MIN_VALUE
        var bestMove = emptyCells[0]
        val opponent = state.players.first { it != player }

        for (move in emptyCells) {
            val simState = simulateBoardMove(state, move, player)
            val score = minimaxTicTacToe(simState, 0, false, player, opponent, Int.MIN_VALUE, Int.MAX_VALUE)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minimaxTicTacToe(
        state: BoardState, depth: Int, isMaximizing: Boolean,
        player: PlayerId, opponent: PlayerId, alpha: Int, beta: Int
    ): Int {
        // Check terminal
        val winner = checkTicTacToeWinner(state)
        if (winner == player) return 10 - depth
        if (winner == opponent) return depth - 10
        if (state.grid.all { row -> row.all { it != null } }) return 0

        val currentPlayer = if (isMaximizing) player else opponent
        val emptyCells = mutableListOf<Pair<Int,Int>>()
        for (r in 0 until 3) for (c in 0 until 3) {
            if (state.grid[r][c] == null) emptyCells.add(r to c)
        }

        var alphaVar = alpha
        var betaVar = beta
        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            for ((r,c) in emptyCells) {
                val sim = simulateBoardMove(state, BoardAction(r,c), currentPlayer)
                val score = minimaxTicTacToe(sim, depth+1, false, player, opponent, alphaVar, betaVar)
                maxScore = max(maxScore, score)
                alphaVar = max(alphaVar, score)
                if (betaVar <= alphaVar) break
            }
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            for ((r,c) in emptyCells) {
                val sim = simulateBoardMove(state, BoardAction(r,c), currentPlayer)
                val score = minimaxTicTacToe(sim, depth+1, true, player, opponent, alphaVar, betaVar)
                minScore = min(minScore, score)
                betaVar = min(betaVar, score)
                if (betaVar <= alphaVar) break
            }
            return minScore
        }
    }

    private fun checkTicTacToeWinner(state: BoardState): PlayerId? {
        val b = state.grid
        for (p in state.players) {
            for (i in 0..2) {
                if (b[i][0] == p && b[i][1] == p && b[i][2] == p) return p
                if (b[0][i] == p && b[1][i] == p && b[2][i] == p) return p
            }
            if (b[0][0] == p && b[1][1] == p && b[2][2] == p) return p
            if (b[0][2] == p && b[1][1] == p && b[2][0] == p) return p
        }
        return null
    }

    // ===== Connect Four: Minimax با عمق ۵ =====
    private fun getConnectFourAction(state: BoardState, player: PlayerId): BoardAction? {
        val validCols = (0 until 7).filter { col -> (0 until 6).any { state.grid[it][col] == null } }
        if (validCols.isEmpty()) return null

        if (difficulty == 1 && Random.nextFloat() < 0.5f) {
            return BoardAction(0, validCols.random())
        }

        var bestScore = Int.MIN_VALUE
        var bestCol = validCols[0]
        val opponent = state.players.first { it != player }

        for (col in validCols) {
            val row = (5 downTo 0).first { state.grid[it][col] == null }
            val sim = simulateBoardMove(state, BoardAction(row, col), player)
            val score = minimaxConnectFour(sim, 4, false, player, opponent, Int.MIN_VALUE, Int.MAX_VALUE)
            if (score > bestScore) {
                bestScore = score
                bestCol = col
            }
        }
        return BoardAction(0, bestCol)
    }

    private fun minimaxConnectFour(
        state: BoardState, depth: Int, isMaximizing: Boolean,
        player: PlayerId, opponent: PlayerId, alpha: Int, beta: Int
    ): Int {
        val winner = checkConnectFourWinner(state)
        if (winner == player) return 100 + depth
        if (winner == opponent) return -100 - depth
        if (depth == 0 || state.grid.all { row -> row.all { it != null } }) return evaluateBoard(state, player)

        val validCols = (0 until 7).filter { col -> (0 until 6).any { state.grid[it][col] == null } }
        val currentPlayer = if (isMaximizing) player else opponent
        var alphaVar = alpha; var betaVar = beta

        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            for (col in validCols) {
                val row = (5 downTo 0).first { state.grid[it][col] == null }
                val sim = simulateBoardMove(state, BoardAction(row, col), currentPlayer)
                val score = minimaxConnectFour(sim, depth-1, false, player, opponent, alphaVar, betaVar)
                maxScore = max(maxScore, score)
                alphaVar = max(alphaVar, score)
                if (betaVar <= alphaVar) break
            }
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            for (col in validCols) {
                val row = (5 downTo 0).first { state.grid[it][col] == null }
                val sim = simulateBoardMove(state, BoardAction(row, col), currentPlayer)
                val score = minimaxConnectFour(sim, depth-1, true, player, opponent, alphaVar, betaVar)
                minScore = min(minScore, score)
                betaVar = min(betaVar, score)
                if (betaVar <= alphaVar) break
            }
            return minScore
        }
    }

    private fun checkConnectFourWinner(state: BoardState): PlayerId? {
        val b = state.grid
        for (p in state.players) {
            for (r in 0..5) for (c in 0..3) if (b[r][c]==p&&b[r][c+1]==p&&b[r][c+2]==p&&b[r][c+3]==p) return p
            for (c in 0..6) for (r in 0..2) if (b[r][c]==p&&b[r+1][c]==p&&b[r+2][c]==p&&b[r+3][c]==p) return p
            for (r in 0..2) for (c in 0..3) if (b[r][c]==p&&b[r+1][c+1]==p&&b[r+2][c+2]==p&&b[r+3][c+3]==p) return p
            for (r in 3..5) for (c in 0..3) if (b[r][c]==p&&b[r-1][c+1]==p&&b[r-2][c+2]==p&&b[r-3][c+3]==p) return p
        }
        return null
    }

    private fun evaluateBoard(state: BoardState, player: PlayerId): Int {
        // Heuristic ساده: تعداد سه‌تایی‌های باز
        var score = 0
        val opponent = state.players.first { it != player }
        for (r in 0..5) for (c in 0..6) {
            if (state.grid[r][c] == player) score += getPositionalValue(r, c)
            if (state.grid[r][c] == opponent) score -= getPositionalValue(r, c)
        }
        return score
    }

    private fun getPositionalValue(row: Int, col: Int): Int {
        val centerBonus = 3 - abs(col - 3)
        return centerBonus + 1
    }

    private fun simulateBoardMove(state: BoardState, action: BoardAction, player: PlayerId): BoardState {
        val newGrid = state.grid.map { it.toMutableList() }
        newGrid[action.row][action.col] = player
        return BoardState(newGrid, state.players.first { it != player }, state.players)
    }

    // ===== UNO: قانون‌محور =====
    private fun getUnoAction(state: UnoState, player: PlayerId): CardAction? {
        val hand = state.hands[player] ?: return CardAction.DrawCard
        val top = state.discardPile.lastOrNull() ?: return CardAction.DrawCard

        val playable = hand.cards.filter { canPlayUno(it, top) }
        if (playable.isEmpty()) return CardAction.DrawCard

        // اولویت: کارت‌های عددی معمولی، سپس wildها
        val normal = playable.filter { it.color != CardColor.WILD && it.value !is CardValue.Wild && it.value !is CardValue.WildDrawFour }
        val chosen = if (normal.isNotEmpty()) normal.random() else playable.random()

        return CardAction.PlayCard(
            card = chosen,
            chosenColor = if (chosen.color == CardColor.WILD) getMostFrequentColor(hand) else null
        )
    }

    private fun getMostFrequentColor(hand: Hand): CardColor {
        val colors = hand.cards.map { it.color }.filter { it != CardColor.WILD }
        return if (colors.isNotEmpty()) colors.groupBy { it }.maxBy { it.value.size }.key else CardColor.RED
    }

    private fun canPlayUno(card: Card, top: Card): Boolean {
        if (card.color == CardColor.WILD || top.color == CardColor.WILD) return true
        if (card.color == top.color) return true
        if (card.value == top.value) return true
        return false
    }

    // ===== LUDO: انتخاب تصادفی =====
    private fun getLudoAction(state: LudoState, player: PlayerId): GameAction {
        return com.gamehub.games.ludo.LudoAction.RollDice
    }
}