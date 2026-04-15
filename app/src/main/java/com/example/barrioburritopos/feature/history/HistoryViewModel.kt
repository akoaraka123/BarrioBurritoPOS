package com.example.barrioburritopos.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.data.repository.ProductRepository
import com.example.barrioburritopos.domain.model.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(
    private val orderRepo: OrderRepository,
    private val productRepo: ProductRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = productRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> = combine(_transactions, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.items.any { item -> item.productName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAllTransactions()
    }

    private fun loadAllTransactions() {
        viewModelScope.launch {
            orderRepo.getAll().collect { orders ->
                val txs = orders.map { order ->
                    Transaction(
                        id = order.id,
                        dateTimeFormatted = formatDateTime(order.dateTime),
                        items = orderRepo.getItems(order.id),
                        totalAmount = order.totalAmount,
                        totalItems = order.totalItems,
                        paymentMethod = order.paymentMethod,
                        amountReceived = order.amountReceived,
                        changeAmount = order.changeAmount
                    )
                }
                _transactions.value = txs
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    fun editOrder(orderId: Long, newItems: List<OrderItemEntity>, priceDifference: Double, additionalPayment: Double) {
        viewModelScope.launch {
            val order = orderRepo.getById(orderId) ?: return@launch
            val newTotalAmount = newItems.sumOf { it.subtotal }
            val newTotalItems = newItems.sumOf { it.quantity }
            
            val originalAmountReceived = order.amountReceived ?: order.totalAmount
            val newAmountReceived = originalAmountReceived + additionalPayment
            val newChangeAmount = newAmountReceived - newTotalAmount
            
            val updatedOrder = order.copy(
                totalAmount = newTotalAmount,
                totalItems = newTotalItems,
                amountReceived = newAmountReceived,
                changeAmount = newChangeAmount
            )
            
            orderRepo.updateOrder(updatedOrder, newItems)
            
            // Add a small delay to ensure database transaction completes
            kotlinx.coroutines.delay(100)
            
            loadAllTransactions()
        }
    }

    fun deleteOrder(orderId: Long) {
        viewModelScope.launch {
            orderRepo.delete(orderId)
            loadAllTransactions()
        }
    }

    private fun formatDateTime(epoch: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return sdf.format(Date(epoch))
    }

    companion object {
        fun factory(orderRepo: OrderRepository, productRepo: ProductRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(orderRepo, productRepo) as T
                }
            }
        }
    }
}
