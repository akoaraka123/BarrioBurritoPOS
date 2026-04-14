package com.example.barrioburritopos.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.domain.model.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistoryViewModel(private val orderRepo: OrderRepository) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
                        totalItems = order.totalItems
                    )
                }
                _transactions.value = txs
            }
        }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    private fun formatDateTime(epoch: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(epoch))
    }

    companion object {
        fun factory(orderRepo: OrderRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(orderRepo) as T
                }
            }
        }
    }
}
