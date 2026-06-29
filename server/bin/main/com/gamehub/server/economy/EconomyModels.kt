package com.gamehub.server.economy

import kotlinx.serialization.Serializable

@Serializable
data class ItemDefinition(
    val itemId: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val priceSoft: Long,
    val globalMaxQuantity: Int? = null,
    val currentSold: Int = 0,
    val dailyPurchaseLimit: Int? = null,
    val minLevel: Int = 0,
    val refundableMinutes: Int = 5,
    val isGiftable: Boolean = true,
    val expirationDays: Int? = null
)
data class SinkRollbackItem(
    val userId: String,
    val oldBalance: Long,
    val taxAmount: Long
)