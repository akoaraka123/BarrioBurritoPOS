package com.example.barrioburritopos.domain.model

import com.example.barrioburritopos.data.local.entity.OrderItemEntity

// UI model for Order History
data class Transaction(
    val id: Long,
    val dateTimeFormatted: String,
    val items: List<OrderItemEntity>,
    val totalAmount: Double,
    val totalItems: Int,
    val paymentMethod: String,
    val amountReceived: Double?,
    val changeAmount: Double
)
