package com.example.barrioburritopos.feature.pos

data class ReceiptData(
    val orderId: Long,
    val dateTime: Long,
    val cashierName: String,
    val paymentMethod: String,
    val amountReceived: Double,
    val changeAmount: Double,
    val totalAmount: Double,
    val items: List<ReceiptLineItem>
)

data class ReceiptLineItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val details: String?
)
