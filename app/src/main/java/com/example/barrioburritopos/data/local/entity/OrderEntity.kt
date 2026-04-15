package com.example.barrioburritopos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTime: Long, // epoch millis
    val totalAmount: Double,
    val totalItems: Int,
    val paymentMethod: String = "CASH",
    val amountReceived: Double? = null,
    val changeAmount: Double = 0.0
)
