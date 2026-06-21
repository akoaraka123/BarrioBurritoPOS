package com.example.barrioburritopos.domain.model

import com.example.barrioburritopos.data.local.entity.ProductEntity

data class CartItem(
    val lineId: String,
    val productId: Long,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val details: String? = null
) {
    val subtotal: Double get() = price * quantity

    companion object {
        fun fromProduct(product: ProductEntity): CartItem = CartItem(
            lineId = "p_${product.id}",
            productId = product.id,
            name = product.name,
            price = product.price,
            quantity = 1
        )
    }
}
