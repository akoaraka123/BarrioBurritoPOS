package com.example.barrioburritopos.domain.model

data class CartItem(
    val productId: Long,
    val name: String,
    val price: Double,
    val quantity: Int = 1
) {
    val subtotal: Double get() = price * quantity
}
