package com.example.barrioburritopos.domain.model

import com.example.barrioburritopos.data.local.entity.OrderItemEntity

// UI model for Order History
data class Transaction(
    val id: Long,
    val dateTimeFormatted: String,
    val items: List<OrderItemEntity>,
    val totalAmount: Double,
    val totalItems: Int
)
