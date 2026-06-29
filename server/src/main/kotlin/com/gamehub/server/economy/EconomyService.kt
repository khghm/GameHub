// server/src/main/kotlin/com/gamehub/server/economy/EconomyService.kt
package com.gamehub.server.economy

import com.gamehub.server.repository.UserRepository
import com.gamehub.shared.cache.CacheProvider
import com.gamehub.shared.idempotency.IdempotencyManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class RefundData(
    val userId: String,
    val amount: Long
)
@Serializable
data class WalletInfo(val balance: Long, val version: Long)

@Serializable
data class InventoryItem(
    val itemId: String,
    val quantity: Int,
    val expiresAt: Long? = null,
    val equipped: Boolean = false,
    val obtainedAt: Long,
    val source: String
)

class EconomyService(
    private val userRepository: UserRepository,
    private val cache: CacheProvider,
    private val idempotencyManager: IdempotencyManager,
    private val economyLoopService: EconomyLoopService
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val balanceLocks = mutableMapOf<String, Mutex>()
    private val itemDefinitionsCache = ConcurrentHashMap<String, ItemDefinition>()
    private val transactionLog = mutableListOf<Map<String, Any>>()
    private val giftLog = mutableListOf<Map<String, Any>>()

    // ==================== کیف پول ====================

    suspend fun getWallet(userId: String): WalletInfo {
        val cached = cache.get("wallet:$userId")
        if (cached != null) return json.decodeFromString(cached)
        val user = userRepository.findById(UUID.fromString(userId)) ?: throw IllegalArgumentException("User not found")
        val info = WalletInfo(user.softCurrency, user.walletVersion)
        cache.set("wallet:$userId", json.encodeToString(info), 3600)
        return info
    }

    suspend fun addCoins(userId: String, amount: Long, source: String, idempotencyKey: String? = null): WalletInfo {
        val effectiveKey = idempotencyKey ?: UUID.randomUUID().toString()
        return idempotencyManager.execute(
            userId = userId, actionType = "add_coins", idempotencyKey = effectiveKey,
            serializer = WalletInfo.serializer()
        ) {
            val lock = balanceLocks.getOrPut(userId) { Mutex() }
            lock.withLock {
                val user = userRepository.findById(UUID.fromString(userId)) ?: throw IllegalArgumentException("User not found")
                val newBalance = user.softCurrency + amount
                val newVersion = user.walletVersion + 1
                userRepository.updateBalance(userId, newBalance, newVersion)
                val info = WalletInfo(newBalance, newVersion)
                cache.set("wallet:$userId", json.encodeToString(info), 3600)
                recordTransaction(userId, "add_coin", amount, null, null, newBalance, source, effectiveKey)
                economyLoopService.recordFlow(amount)
                info
            }
        }
    }

    suspend fun deductCoins(userId: String, amount: Long, source: String, idempotencyKey: String? = null): WalletInfo {
        val effectiveKey = idempotencyKey ?: UUID.randomUUID().toString()
        return idempotencyManager.execute(
            userId = userId, actionType = "deduct_coins", idempotencyKey = effectiveKey,
            serializer = WalletInfo.serializer()
        ) {
            val lock = balanceLocks.getOrPut(userId) { Mutex() }
            lock.withLock {
                val user = userRepository.findById(UUID.fromString(userId)) ?: throw IllegalArgumentException("User not found")
                if (user.softCurrency < amount) throw IllegalArgumentException("Insufficient balance")
                val newBalance = user.softCurrency - amount
                val newVersion = user.walletVersion + 1
                userRepository.updateBalance(userId, newBalance, newVersion)
                val info = WalletInfo(newBalance, newVersion)
                cache.set("wallet:$userId", json.encodeToString(info), 3600)
                recordTransaction(userId, "deduct_coin", -amount, null, null, newBalance, source, effectiveKey)
                economyLoopService.recordFlow(-amount)
                info
            }
        }
    }

    // ==================== بازپرداخت (فقط بر اساس cache و بدون inventory) ====================

    suspend fun refundPurchase(userId: String, purchaseId: String, idempotencyKey: String): WalletInfo {
        return idempotencyManager.execute(
            userId = userId, actionType = "refund", idempotencyKey = idempotencyKey,
            serializer = WalletInfo.serializer()
        ) {
            val refundInfoJson = cache.get("refund:window:$purchaseId")
                ?: throw IllegalStateException("Refund window expired or invalid")
            val refundData = json.decodeFromString<RefundData>(refundInfoJson)
            if (refundData.userId != userId) throw IllegalArgumentException("Purchase not owned by user")

            val today = LocalDate.now(ZoneOffset.UTC).toString()
            val refundCountKey = "refund:daily:$userId:$today"
            val refundCount = cache.incr(refundCountKey)
            if (refundCount == 1L) cache.expire(refundCountKey, 86400)
            if (refundCount > 3L) throw IllegalStateException("Daily refund limit exceeded")

            val wallet = addCoins(userId, refundData.amount, "refund:$purchaseId", idempotencyKey)
            cache.delete("refund:window:$purchaseId")
            recordTransaction(userId, "refund", refundData.amount, null, null, wallet.balance, "refund", idempotencyKey)
            wallet
        }
    }

    // ==================== هدیه سکه (نسخه نهایی با cooldown ۶۰ ثانیه) ====================

    suspend fun giftCoins(fromUserId: String, toUserId: String, amount: Long, message: String?, idempotencyKey: String): WalletInfo {
        if (amount <= 0) throw IllegalArgumentException("Amount must be positive")

        val fromLock = balanceLocks.getOrPut(fromUserId) { Mutex() }
        val toLock = balanceLocks.getOrPut(toUserId) { Mutex() }
        val firstLock = if (fromUserId.hashCode() < toUserId.hashCode()) fromLock else toLock
        val secondLock = if (fromUserId.hashCode() < toUserId.hashCode()) toLock else fromLock

        return idempotencyManager.execute(
            userId = fromUserId, actionType = "gift_coin", idempotencyKey = idempotencyKey,
            serializer = WalletInfo.serializer()
        ) {
            firstLock.withLock {
                secondLock.withLock {
                    val fromUser = userRepository.findById(UUID.fromString(fromUserId))
                        ?: throw IllegalArgumentException("Sender not found")
                    val toUser = userRepository.findById(UUID.fromString(toUserId))
                        ?: throw IllegalArgumentException("Recipient not found")

                    val today = LocalDate.now(ZoneOffset.UTC).toString()
                    val senderLevel = fromUser.userLevel

                    // 1. محدودیت مبلغ هر هدیه (۲۰۰ سکه)
                    if (amount > 200L) {
                        throw IllegalStateException("Maximum gift amount per transaction is 200 coins")
                    }

                    // 2. Cooldown ۶۰ ثانیه
                    val cooldownKey = "gift:cooldown:$fromUserId"
                    val lastGift = cache.get(cooldownKey)?.toLongOrNull()
                    if (lastGift != null && System.currentTimeMillis() - lastGift < 60_000L) {
                        throw IllegalStateException("Please wait 1 minute between gifts")
                    }
                    cache.set(cooldownKey, System.currentTimeMillis().toString(), 60)

                    // 3. تعداد هدیه فرستنده در روز (حداکثر ۲۰)
                    val senderDailyCountKey = "gift:daily:sent:$fromUserId:$today"
                    val senderCount = cache.incr(senderDailyCountKey)
                    if (senderCount == 1L) cache.expire(senderDailyCountKey, 86400)
                    if (senderCount > 20) {
                        throw IllegalStateException("Daily send gift limit exceeded (max 20 gifts per day)")
                    }

                    // 4. تعداد هدیه گیرنده در روز (حداکثر ۲۰)
                    val receiverDailyCountKey = "gift:daily:received:$toUserId:$today"
                    val receiverCount = cache.incr(receiverDailyCountKey)
                    if (receiverCount == 1L) cache.expire(receiverDailyCountKey, 86400)
                    if (receiverCount > 20) {
                        throw IllegalStateException("Daily receive gift limit exceeded (max 20 gifts per day)")
                    }

                    // 5. محدودیت هدیه به یک گیرنده خاص (حداکثر ۳ بار در روز)
                    val specificPairKey = "gift:pair:$fromUserId:$toUserId:$today"
                    val pairCount = cache.incr(specificPairKey)
                    if (pairCount == 1L) cache.expire(specificPairKey, 86400)
                    if (pairCount > 3) {
                        throw IllegalStateException("You can only gift this user 3 times per day")
                    }

                    // 6. محدودیت حجم کل هدیه فرستنده بر اساس سطح
                    val maxDailyVolumeByLevel = when {
                        senderLevel <= 5 -> 50L
                        senderLevel <= 10 -> 200L
                        else -> 500L
                    }
                    val senderTotalSentKey = "gift:volume:sent:$fromUserId:$today"
                    var totalSentToday = cache.get(senderTotalSentKey)?.toLongOrNull() ?: 0L
                    if (totalSentToday + amount > maxDailyVolumeByLevel) {
                        throw IllegalStateException("Your daily gift volume limit (based on level $senderLevel) is $maxDailyVolumeByLevel coins")
                    }

                    // 7. کارمزد ۵٪ (حداقل ۱ سکه)
                    val fee = (amount * 0.05).toLong().coerceAtLeast(1)
                    val netAmount = amount - fee

                    if (fromUser.softCurrency < amount) {
                        throw IllegalStateException("Insufficient balance")
                    }

                    val newFromBalance = fromUser.softCurrency - amount
                    val newFromVersion = fromUser.walletVersion + 1
                    userRepository.updateBalance(fromUserId, newFromBalance, newFromVersion)

                    val newToBalance = toUser.softCurrency + netAmount
                    val newToVersion = toUser.walletVersion + 1
                    userRepository.updateBalance(toUserId, newToBalance, newToVersion)

                    economyLoopService.recordFlow(-fee)

                    val fromWallet = WalletInfo(newFromBalance, newFromVersion)
                    val toWallet = WalletInfo(newToBalance, newToVersion)
                    cache.set("wallet:$fromUserId", json.encodeToString(fromWallet), 3600)
                    cache.set("wallet:$toUserId", json.encodeToString(toWallet), 3600)

                    cache.set(senderTotalSentKey, (totalSentToday + amount).toString(), 86400)

                    val correlationId = UUID.randomUUID().toString()
                    recordGift(fromUserId, toUserId, "coin", amount, null, null, message, correlationId)
                    recordTransaction(fromUserId, "gift_sent", -amount, null, null, newFromBalance, "gift", idempotencyKey)
                    recordTransaction(toUserId, "gift_received", netAmount, null, null, newToBalance, "gift", correlationId)
                    recordTransaction("system_sink", "gift_fee", -fee, null, null, 0, "gift", correlationId)

                    fromWallet
                }
            }
        }
    }

    // ==================== توابع کمکی ====================

    private suspend fun getItemDefinition(itemId: String): ItemDefinition? {
        return itemDefinitionsCache[itemId] ?: run {
            if (itemId == "test_item") {
                ItemDefinition(
                    itemId = itemId, name = "Test Item", description = "A test item",
                    type = "consumable", priceSoft = 100L, refundableMinutes = 5
                ).also { itemDefinitionsCache[itemId] = it }
            } else null
        }
    }

    private suspend fun recordTransaction(
        userId: String, type: String, amount: Long, itemId: String?, quantity: Int?,
        balanceAfter: Long, source: String, idempotencyKey: String?
    ) {
        transactionLog.add(mapOf(
            "userId" to userId, "type" to type, "amount" to amount, "itemId" to (itemId ?: ""),
            "quantity" to (quantity ?: 0), "balanceAfter" to balanceAfter, "source" to source,
            "idempotencyKey" to (idempotencyKey ?: ""), "timestamp" to System.currentTimeMillis()
        ))
    }

    private suspend fun recordGift(
        from: String, to: String, type: String, amount: Long?, itemId: String?,
        quantity: Int?, message: String?, correlationId: String
    ) {
        giftLog.add(mapOf(
            "from" to from, "to" to to, "type" to type, "amount" to (amount ?: 0L),
            "itemId" to (itemId ?: ""), "quantity" to (quantity ?: 1),
            "message" to (message ?: ""), "correlationId" to correlationId,
            "timestamp" to System.currentTimeMillis()
        ))
    }
}