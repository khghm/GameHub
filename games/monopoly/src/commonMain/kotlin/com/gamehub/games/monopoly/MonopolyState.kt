package com.gamehub.games.monopoly

import com.gamehub.shared.core.GameState
import com.gamehub.shared.core.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class Loan(
    val amount: Int,
    var remaining: Int,
    val interestRate: Double = 0.3
)

@Serializable
data class Investment(
    val amount: Int,
    var roundsLeft: Int,
    val returnAmount: Int
)

@Serializable
data class PlayerState(
    val playerId: PlayerId,
    val name: String,
    var cash: Int = 1000,
    var position: Int = 0,
    var jailTurns: Int = 0,
    var innateShieldUsed: Boolean = false,
    val ownedProperties: MutableList<Int> = mutableListOf(),
    var helperCards: MutableList<HelperCard> = mutableListOf(),
    var loan: Loan? = null,
    var investments: MutableList<Investment> = mutableListOf(),
    var isBankrupt: Boolean = false,
    var firstMoveDone: Boolean = false,
    var jailFirstTime: Boolean = true,
    // کارت‌های شانس موقت
    var extraDiceNext: Boolean = false,          // شانس بیشتر - دور بعد 2 تاس و انتخاب یکی
    var banTradeUntilStart: Boolean = false,     // ممنوع المعامله تا رد شدن از شروع
    var banBuyUntilStart: Boolean = false,       // مسدودی تا رد شدن از شروع
    var freePropertyNext: Boolean = false,       // ملک مجانی در حرکت بعدی
    var rentBoostPercent: Int = 0,               // درصد افزایش اجاره (مثبت) یا کاهش (منفی)
    var startMultiplier: Int = 1,                // ضریب شروع (برای نوروز)
    var bankPaysRent: Boolean = false,           // ماه ثروت - بانک اجاره می‌دهد
    var payRentToBank: Boolean = false,          // ماه فقر - اجاره به بانک پرداخت می‌شود
    var reservedPropertyIndex: Int = -1,         // نشان (ملک رزرو شده برای 2 دور)
    var reservedRoundsLeft: Int = 0,
    var sharedRentPartner: PlayerId? = null,     // شراکت اجباری
    var sharedRentRoundsLeft: Int = 0,
    var strategicDiceOptions: List<Int> = emptyList(),  // سه تاس پیشنهادی
    var wasSentByDoubleSix: Boolean = false,    // برای پاداش زندان
    var shieldActiveUntilStart: Boolean = false, // برای کارت سپر
)

@Serializable
enum class HelperCard { SHIELD, GET_OUT_OF_JAIL }

@Serializable
data class PropertyState(
    var ownerId: PlayerId? = null,
    var houses: Int = 0,
    var mortgaged: Boolean = false,
    var temporaryRoundsLeft: Int = 0,
    var improvementValue: Int = 0
)

@Serializable
data class MissionState(
    val id: Int,
    val name: String,
    val description: String,
    val maxRounds: Int = 5,
    var currentRound: Int = 0,
    val params: MissionParams,
    val playersData: MutableMap<PlayerId, MissionPlayerData> = mutableMapOf(),
    val completedPlayers: MutableSet<PlayerId> = mutableSetOf(),
    val rewardAmount: Int = 0,
    val penaltyAmount: Int = 0,
    val isGlobalReward: Boolean = false,   // true=پاداش به برندگان, false=جریمه به بازندگان
)

@Serializable
data class MissionParams(
    val targetPropertyIndex: Int? = null,
    val targetGroup: String? = null,
    val minAmount: Int = 0,
    val maxAmount: Int = 0,
    val percent: Int = 0,
    val loanAmount: Int = 0,
    val extraData: Map<String, Int> = emptyMap(),
)

@Serializable
data class MissionPlayerData(
    var buyCount: Int = 0,
    var sellCount: Int = 0,
    var helperCardUsed: Int = 0,
    var passedStart: Boolean = false,
    var loanTaken: Int = 0,
    var totalCashAtStart: Int = 0,
    var totalAssetsAtStart: Int = 0,
    var customData: MutableMap<String, Int> = mutableMapOf(),
)

enum class MissionEvent {
    MISSION_START, MISSION_END, ROUND_END,
    BUY_PROPERTY, SELL_PROPERTY, TRADE_COMPLETED,
    PASS_START, LAND_ON_PROPERTY, LAND_ON_CHANCE,
    GO_TO_JAIL, USE_HELPER_CARD, GAME_START,
}

@Serializable
data class TradeProposal(
    val id: String,
    val fromPlayer: PlayerId,
    val toPlayer: PlayerId,
    val offeredCash: Int,
    val offeredProperties: List<Int>,
    var requestedCash: Int,          // تغییر به var
    var requestedProperties: List<Int>, // تغییر به var
    var status: TradeStatus
)

@Serializable
enum class TradeStatus { PENDING, ACCEPTED, REJECTED }

@Serializable
enum class TurnPhase {
    WAITING_FOR_ROLL,          // قبل از تاس (امکان خرید/فروش/ساخت)
    ROLLING_DICE,              // انیمیشن تاس
    AWAITING_DICE_SELECTION,   // بعد از نمایش دو تاس، منتظر انتخاب و حرکت
    MOVING,                    // در حال حرکت مهره
    AWAITING_DECISION,         // بعد از فرود روی خانه (خرید، بانک، ...)
    AWAITING_BUILD,            // بعد از فرود روی خانه خود و امکان ساخت
    AWAITING_JAIL_DECISION,    // تصمیم در زندان
    AWAITING_TRADE,            // منتظر معامله
    GAME_OVER,
    AWAITING_TRADE_PROPOSALS,
    STRATEGIC_DICE  // مرحله انتخاب تاس استراتژیک
}

@Serializable
data class AuctionData(
    val propertyIndex: Int,
    var currentBid: Int,
    val highestBidder: PlayerId?,
    val remainingPlayers: List<PlayerId>
)

@Serializable
data class GameSettings(
    var maxRounds: Int = 50,
    var winTarget: Int = 5000,
    var startCash: Int = 1000,
    var startSalary: Int = 200,
    var strategicDiceEnabled: Boolean = true,
    var chanceCardsEnabled: Boolean = true,
    var missionsEnabled: Boolean = true,
    var hideCash: Boolean = false,
    var teamMode: Boolean = false,
    var gameMode: GameMode = GameMode.CLASSIC,
    var buildTax: Int = 30
)

@Serializable
enum class GameMode { CLASSIC, BLACK_MARKET, MONOPOLISTS, ECONOMIC_COLLAPSE, TYCOON, SPEED_DUEL }

@Serializable
data class MonopolyState(
    val players: List<PlayerId>,
    val playerStates: Map<PlayerId, PlayerState>,
    var board: List<PropertyState> = List(MonopolyBoardData.CELL_COUNT) { PropertyState() },
    var currentPlayer: PlayerId?,
    var turnPhase: TurnPhase = TurnPhase.WAITING_FOR_ROLL,
    var diceResult: List<Int> = emptyList(),
    var selectedDice: List<Int> = emptyList(),
    var doubleCount: Int = 0,
    var roundCount: Int = 0,
    var winner: PlayerId? = null,
    var gameOver: Boolean = false,
    var message: String = "",
    var activeMission: MissionState? = null,
    var pendingTrade: TradeProposal? = null,
    var auctionData: AuctionData? = null,
    val settings: GameSettings = GameSettings(),
    val playerNames: Map<PlayerId, String> = emptyMap(),
    var jailDialogShown: Boolean = false,
    val chanceDeck: List<ChanceCard> = emptyList(),  // دسته باقی‌مانده (از بالا کشیده می‌شود)
    val chanceDiscard: List<ChanceCard> = emptyList(), // کارت‌های استفاده‌شده (برای برگشت به ته دسته)
    var tradeProposals: Map<PlayerId, TradeProposal> = emptyMap(), // پیشنهادهای دریافتی از سایر بازیکنان
    var selectedProposalId: String? = null,  // پیشنهاد انتخاب شده توسط A
    var tradeStep: TradeStep = TradeStep.IDLE,
    var selectedProposalFrom: PlayerId? = null,
    var isTradeActive: Boolean = false,
) : GameState()
enum class TradeStep {
    IDLE,
    AWAITING_PROPOSALS,   // منتظر پیشنهاد از سایرین
    AWAITING_SELECTION,   // A در حال انتخاب پیشنهاد
    AWAITING_COUNTER,     // A در حال نوشتن ضدپیشنهاد
    AWAITING_RESPONSE     // منتظر قبول/رد ضدپیشنهاد توسط طرف مقابل
}

@Serializable
data class ChanceCard(
    val id: Int,
    val name: String,
    val description: String,
    val weight: Int,           // وزن برای تولید دسته (تعداد تکرار)
    val effect: String,        // نوع اثر: "MONEY", "JAIL", "MOVE", "TRADE", "CARD", "SPECIAL"
    val amount: Int = 0,       // مقدار پول یا تعداد دور
    val condition: String = "", // شرط: "cash<400", "cash>700", "anyPlayerInJail", "noPlayerOutsideJail", "hasProperties", "cash<0"
    val target: String = "",    // "self", "allOthers", "randomPlayer", "richest", "poorest"
    val keepCard: Boolean = false, // آیا کارت به عنوان کارت کمکی نگهداری می‌شود (مانند فرار از زندان)
    val effectParam: String = ""
)



