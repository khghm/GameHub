// games/uno/src/commonMain/kotlin/com/gamehub/games/uno/UnoBotStrategy.kt
package com.gamehub.games.uno

import com.gamehub.shared.bot.BotStrategy
import com.gamehub.shared.core.PlayerId
import com.gamehub.shared.engines.card.CardAction
import com.gamehub.shared.engines.card.CardColor
import com.gamehub.shared.engines.card.Hand
import kotlin.random.Random

class UnoBotStrategy : BotStrategy<UnoState, CardAction> {
    override val gameId: String = "uno"
    override val supportedDifficultyLevels: IntRange = 1..10

    override suspend fun getNextMove(state: UnoState, botPlayerId: PlayerId, difficultyLevel: Int): CardAction? {
        val hand = state.hands[botPlayerId] ?: return CardAction.DrawCard
        val topCard = state.discardPile.lastOrNull() ?: return CardAction.DrawCard

        val playableCards = hand.cards.filter { canPlay(it, topCard) }

        // سطح 1-2: حرکت تصادفی
        if (difficultyLevel <= 2) {
            return if (playableCards.isNotEmpty()) {
                val card = playableCards.random()
                CardAction.PlayCard(card, chooseBestColor(hand, card))
            } else {
                CardAction.DrawCard
            }
        }

        // سطح 3-4: اولویت کارت‌های عددی (نگه‌داشتن اکشن برای بعد)
        if (difficultyLevel <= 4) {
            val numberCards = playableCards.filter { it.value is com.gamehub.shared.engines.card.CardValue.Number }
            if (numberCards.isNotEmpty()) {
                return CardAction.PlayCard(numberCards.first(), null)
            }
        }

        // سطح 5-7: اولویت کارت‌های اکشن (Skip, Reverse, DrawTwo)
        if (difficultyLevel <= 7) {
            val actionCards = playableCards.filter {
                it.value is com.gamehub.shared.engines.card.CardValue.Skip ||
                        it.value is com.gamehub.shared.engines.card.CardValue.Reverse ||
                        it.value is com.gamehub.shared.engines.card.CardValue.DrawTwo
            }
            if (actionCards.isNotEmpty()) {
                return CardAction.PlayCard(actionCards.first(), null)
            }
        }

        // سطح 8-10: استراتژی هوشمند – اولویت Wild و WildDrawFour اگر تعداد کارت‌ها زیاد است
        if (difficultyLevel >= 8) {
            // اگر بیش از 5 کارت داریم، سعی کنیم Wild بزنیم
            if (hand.cards.size > 5) {
                val wildCards = playableCards.filter { it.color == CardColor.WILD }
                if (wildCards.isNotEmpty()) {
                    val bestColor = getMostFrequentColor(hand)
                    return CardAction.PlayCard(wildCards.first(), bestColor)
                }
            }
            // اگر یک کارت داریم (UNO)، سعی کنیم با هر کارت ممکن بازی کنیم
            if (hand.cards.size == 1 && playableCards.isNotEmpty()) {
                return CardAction.PlayCard(playableCards.first(), chooseBestColor(hand, playableCards.first()))
            }
        }

        return if (playableCards.isNotEmpty()) {
            val card = playableCards.first()
            CardAction.PlayCard(card, chooseBestColor(hand, card))
        } else {
            CardAction.DrawCard
        }
    }

    private fun canPlay(card: com.gamehub.shared.engines.card.Card, top: com.gamehub.shared.engines.card.Card): Boolean {
        if (card.color == CardColor.WILD || top.color == CardColor.WILD) return true
        if (card.color == top.color) return true
        if (card.value == top.value) return true
        return false
    }

    private fun getMostFrequentColor(hand: Hand): CardColor {
        val colorCounts = hand.cards.filter { it.color != CardColor.WILD }
            .groupBy { it.color }
            .mapValues { it.value.size }
        return colorCounts.maxByOrNull { it.value }?.key ?: CardColor.RED
    }

    private fun chooseBestColor(hand: Hand, playedCard: com.gamehub.shared.engines.card.Card): CardColor? {
        if (playedCard.color != CardColor.WILD) return null
        return getMostFrequentColor(hand)
    }
}