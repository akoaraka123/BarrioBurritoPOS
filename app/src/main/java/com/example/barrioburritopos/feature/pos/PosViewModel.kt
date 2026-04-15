package com.example.barrioburritopos.feature.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.data.repository.ProductRepository
import com.example.barrioburritopos.domain.model.CartItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

enum class PaymentMethod {
    CASH,
    GCASH
}

class PosViewModel(
    private val productRepo: ProductRepository,
    private val orderRepo: OrderRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(_products, _searchQuery) { products, query ->
        if (query.isBlank()) {
            products
        } else {
            products.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                (product.category?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cashReceived = MutableStateFlow("")
    val cashReceived: StateFlow<String> = _cashReceived.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod: StateFlow<PaymentMethod> = _paymentMethod.asStateFlow()

    private val _checkoutStatus = MutableStateFlow<CheckoutStatus?>(null)
    val checkoutStatus: StateFlow<CheckoutStatus?> = _checkoutStatus.asStateFlow()

    val total: StateFlow<Double> = _cart.map { items -> items.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val amountReceived: StateFlow<Double?> = combine(paymentMethod, total, cashReceived) { method, t, cash ->
        when (method) {
            PaymentMethod.CASH -> cash.toDoubleOrNull()
            PaymentMethod.GCASH -> t
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val change: StateFlow<Double> = combine(paymentMethod, total, cashReceived) { method, t, cash ->
        when (method) {
            PaymentMethod.CASH -> {
                val c = cash.toDoubleOrNull() ?: 0.0
                if (c >= t) c - t else 0.0
            }
            PaymentMethod.GCASH -> 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val isCheckoutAllowed: StateFlow<Boolean> = combine(cart, paymentMethod, total, cashReceived) { items, method, t, cash ->
        if (items.isEmpty()) return@combine false
        when (method) {
            PaymentMethod.CASH -> {
                val c = cash.toDoubleOrNull() ?: 0.0
                c >= t
            }
            PaymentMethod.GCASH -> true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepo.getAllAvailable().collect { list ->
                _products.value = list
            }
        }
    }

    fun addToCart(product: ProductEntity) {
        val current = _cart.value.toMutableList()
        val existing = current.find { it.productId == product.id }
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + 1)
            current[current.indexOf(existing)] = updated
        } else {
            current.add(CartItem(product.id, product.name, product.price, 1))
        }
        _cart.value = current
    }

    fun increaseQuantity(productId: Long) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.productId == productId }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
            _cart.value = current
        }
    }

    fun decreaseQuantity(productId: Long) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.productId == productId }
        if (index != -1) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.productId != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cashReceived.value = ""
        _paymentMethod.value = PaymentMethod.CASH
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _paymentMethod.value = method
        if (method == PaymentMethod.GCASH) {
            _cashReceived.value = ""
        }
    }

    fun onCashReceivedChange(value: String) {
        // Allow only valid decimal input
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _cashReceived.value = value
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun checkout() {
        viewModelScope.launch {
            val items = _cart.value
            if (items.isEmpty()) {
                _checkoutStatus.value = CheckoutStatus.Error("Cart is empty")
                return@launch
            }

            val totalAmount = items.sumOf { it.subtotal }
            val method = paymentMethod.value
            val receivedAmount: Double? = when (method) {
                PaymentMethod.CASH -> cashReceived.value.toDoubleOrNull()
                PaymentMethod.GCASH -> totalAmount
            }
            val changeAmount: Double = when (method) {
                PaymentMethod.CASH -> {
                    val cash = receivedAmount ?: 0.0
                    if (cash < totalAmount) {
                        _checkoutStatus.value = CheckoutStatus.Error("Insufficient payment")
                        return@launch
                    }
                    cash - totalAmount
                }
                PaymentMethod.GCASH -> 0.0
            }

            // Check stock availability
            for (item in items) {
                val product = productRepo.getById(item.productId)
                if (product == null || product.stockQuantity < item.quantity) {
                    _checkoutStatus.value = CheckoutStatus.Error("Insufficient stock for ${item.name}")
                    return@launch
                }
            }

            // Create order
            val now = System.currentTimeMillis()
            val order = OrderEntity(
                dateTime = now,
                totalAmount = totalAmount,
                totalItems = items.sumOf { it.quantity },
                paymentMethod = method.name,
                amountReceived = receivedAmount,
                changeAmount = changeAmount
            )

            val orderItems = items.map {
                OrderItemEntity(
                    orderId = 0, // will be set by DAO
                    productId = it.productId,
                    productName = it.name,
                    itemPrice = it.price,
                    quantity = it.quantity,
                    subtotal = it.subtotal
                )
            }

            try {
                // Save order and items
                orderRepo.createOrder(order, orderItems)

                // Decrease stock
                for (item in items) {
                    productRepo.decrementStock(item.productId, item.quantity)
                }

                // Clear cart
                clearCart()
                _checkoutStatus.value = CheckoutStatus.Success(changeAmount)
            } catch (e: Exception) {
                _checkoutStatus.value = CheckoutStatus.Error(e.message ?: "Checkout failed")
            }
        }
    }

    fun resetCheckoutStatus() {
        _checkoutStatus.value = null
    }

    companion object {
        fun factory(productRepo: ProductRepository, orderRepo: OrderRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PosViewModel(productRepo, orderRepo) as T
                }
            }
        }
    }
}

sealed class CheckoutStatus {
    data class Success(val change: Double) : CheckoutStatus()
    data class Error(val message: String) : CheckoutStatus()
}
