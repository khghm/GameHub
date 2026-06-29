package com.gamehub.games.monopoly

import com.gamehub.shared.core.GameAction
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
sealed class MonopolyAction : GameAction() {
    // عملیات تاس
    @Serializable data object RollDice : MonopolyAction()                     // درخواست انداختن دو تاس
    @Serializable data class SelectDiceAndMove(val selectedSteps: Int) : MonopolyAction() // انتخاب مجموع تاس‌ها و حرکت

    // عملیات خرید/فروش/ساخت (قبل از تاس یا بعد از فرود)
    @Serializable data object BuyProperty : MonopolyAction()
    @Serializable data object PassProperty : MonopolyAction()
    @Serializable data class SellProperty(val propertyIndex: Int) : MonopolyAction()
    @Serializable data class BuildHouse(val propertyIndex: Int) : MonopolyAction()
    @Serializable data class SellHouse(val propertyIndex: Int) : MonopolyAction()

    // عملیات بانک
    @Serializable data class TakeLoan(val amount: Int) : MonopolyAction()
    @Serializable data class MortgageProperty(val propertyIndex: Int) : MonopolyAction()
    @Serializable data class UnmortgageProperty(val propertyIndex: Int) : MonopolyAction()
    @Serializable data class MakeInvestment(val amount: Int) : MonopolyAction()

    // حمل و نقل
    @Serializable data class UseTransport(val destinationIndex: Int) : MonopolyAction()

    // معامله
    @Serializable data class ProposeTrade(
        val targetPlayerId: PlayerId,
        val offeredCash: Int,
        val offeredProperties: List<Int>
    ) : MonopolyAction()
    @Serializable data class CounterTrade(
        val tradeId: String,
        val requestedCash: Int,
        val requestedProperties: List<Int>
    ) : MonopolyAction()
    @Serializable data class AcceptTrade(val tradeId: String) : MonopolyAction()
    @Serializable data class RejectTrade(val tradeId: String) : MonopolyAction()

    // کارت‌های کمکی و سپر
    @Serializable data class UseHelperCard(val card: HelperCard) : MonopolyAction()
    @Serializable data object UseInnateShield : MonopolyAction()

    // تصمیمات زندان
    @Serializable data object PayJailFine : MonopolyAction()    // پرداخت 150 دلار و آزادی فوری
    @Serializable data object StayInJail : MonopolyAction()     // ماندن در زندان (تعداد دور مشخص)

    // مأموریت (در صورت نیاز)
    @Serializable data object SkipMission : MonopolyAction()

    @Serializable data class SubmitTradeProposal(
        val offeredCash: Int,
        val offeredProperties: List<Int>
    ) : MonopolyAction()

    @Serializable data class SelectTradeProposal(val proposerId: PlayerId) : MonopolyAction()
    @Serializable data object CancelTrade : MonopolyAction()

    @Serializable data class MakeCounterOffer(
        val targetPlayerId: PlayerId,
        val requestedCash: Int,        // در عمل، این مقدار پیشنهاد نقدی بازیکن فعلی است (آنچه می‌دهد)
        val requestedProperties: List<Int> // املاکی که بازیکن فعلی می‌دهد
    ) : MonopolyAction()
    @Serializable data object RollStrategicDice : MonopolyAction()   // درخواست تاس استراتژیک
    @Serializable data class SelectStrategicDice(val chosenValue: Int) : MonopolyAction() // انتخاب از سه
}