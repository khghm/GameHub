package com.gamehub.server.economy

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

@Serializable
data class PurchaseResult(
    val success: Boolean,
    val itemId: String,
    val quantity: Int,
    val pricePaid: Long,
    val purchaseId: String,
    val message: String = ""
)

// StockData برای موجودی جهانی آیتم محدود
@Serializable
private data class StockData(
    val sold: Int,
    val max: Int
)

class ShopService(
    private val cache: CacheProvider,
    private val economyService: EconomyService,
    private val idempotencyManager: IdempotencyManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val itemLocks = mutableMapOf<String, Mutex>()

    companion object {
        private const val CACHE_ITEM_PREFIX = "shop:item:"
        private const val CACHE_STOCK_PREFIX = "shop:stock:"
    }

    suspend fun getItemDefinition(itemId: String): ItemDefinition? {
        val cached = cache.get("$CACHE_ITEM_PREFIX$itemId")
        if (cached != null) {
            return json.decodeFromString(cached)
        }
        val item = when (itemId) {
            "test_item" -> ItemDefinition(
                itemId = itemId,
                name = "Test Item",
                description = "A test item",
                type = "consumable",
                priceSoft = 100L,
                refundableMinutes = 5,
                dailyPurchaseLimit = 1
            )
            else -> null
        }
        if (item != null) {
            cache.set("$CACHE_ITEM_PREFIX$itemId", json.encodeToString(item), 3600)
        }
        return item
    }

    suspend fun getDynamicPrice(itemId: String, basePrice: Long, maxQuantity: Int, sold: Int): Long {
        if (maxQuantity <= 0 || sold <= 0) return basePrice
        val soldRatio = sold.toDouble() / maxQuantity.toDouble()
        val multiplier = 1.0 + (soldRatio * 0.5)
        return (basePrice * multiplier).toLong().coerceAtLeast(basePrice)
    }

    suspend fun purchaseItem(
        userId: String,
        itemId: String,
        quantity: Int,
        idempotencyKey: String
    ): PurchaseResult {
        if (quantity != 1) {
            return PurchaseResult(false, itemId, quantity, 0, "", "Only quantity 1 supported")
        }

        val item = getItemDefinition(itemId) ?: return PurchaseResult(false, itemId, quantity, 0, "", "Item not found")

        val today = LocalDate.now(ZoneOffset.UTC).toString()
        val dailyKey = "shop:daily:$userId:$itemId:$today"
        val dailyCount = cache.incr(dailyKey)
        if (dailyCount == 1L) cache.expire(dailyKey, 86400)
        if (item.dailyPurchaseLimit != null && dailyCount > item.dailyPurchaseLimit) {
            return PurchaseResult(false, itemId, quantity, 0, "", "Daily purchase limit exceeded")
        }

        val lock = itemLocks.getOrPut(itemId) { Mutex() }
        return lock.withLock {
            var finalPrice = item.priceSoft
            var currentSold = 0

            if (item.globalMaxQuantity != null) {
                val stockKey = "$CACHE_STOCK_PREFIX$itemId"
                val stockJson = cache.get(stockKey)
                if (stockJson != null) {
                    val stockData = json.decodeFromString<StockData>(stockJson)
                    currentSold = stockData.sold
                }
                if (currentSold >= item.globalMaxQuantity) {
                    return@withLock PurchaseResult(false, itemId, quantity, 0, "", "Item sold out")
                }
                finalPrice = getDynamicPrice(itemId, item.priceSoft, item.globalMaxQuantity, currentSold)
            }

            val totalPrice = finalPrice * quantity
            val wallet = try {
                economyService.deductCoins(userId, totalPrice, "purchase:$itemId", idempotencyKey)
            } catch (e: Exception) {
                return PurchaseResult(false, itemId, quantity, 0, "", e.message ?: "Insufficient balance")
            }

            if (item.globalMaxQuantity != null) {
                val newSold = currentSold + quantity
                val stockData = StockData(newSold, item.globalMaxQuantity)
                cache.set("$CACHE_STOCK_PREFIX$itemId", json.encodeToString(stockData), 86400)
            }

            val purchaseId = UUID.randomUUID().toString()
            addToInventory(userId, itemId, quantity, purchaseId)

            // ذخیره اطلاعات بازپرداخت با کلاس RefundData که در EconomyService تعریف شده
            val refundData = RefundData(userId, totalPrice)
            val refundInfoJson = json.encodeToString(refundData)
            cache.set("refund:window:$purchaseId", refundInfoJson, item.refundableMinutes * 60L)

            return PurchaseResult(true, itemId, quantity, totalPrice, purchaseId, "Purchase successful")
        }
    }

    suspend fun refundPurchase(userId: String, purchaseId: String, idempotencyKey: String): PurchaseResult {
        return try {
            economyService.refundPurchase(userId, purchaseId, idempotencyKey)
            PurchaseResult(
                success = true,
                itemId = "",
                quantity = 0,
                pricePaid = 0,
                purchaseId = purchaseId,
                message = "Refund successful"
            )
        } catch (e: IllegalArgumentException) {
            PurchaseResult(false, "", 0, 0, purchaseId, e.message ?: "Invalid request")
        } catch (e: IllegalStateException) {
            PurchaseResult(false, "", 0, 0, purchaseId, e.message ?: "Refund not allowed")
        }
    }

    private suspend fun addToInventory(userId: String, itemId: String, quantity: Int, purchaseId: String) {
        println("💰 Adding $quantity x $itemId to inventory of $userId (purchase: $purchaseId)")
    }
}