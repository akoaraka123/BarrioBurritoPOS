package com.example.barrioburritopos.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.domain.model.DailyReport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ReportsViewModel(private val orderRepo: OrderRepository) : ViewModel() {

    private val _todayReport = MutableStateFlow<DailyReport?>(null)
    val todayReport: StateFlow<DailyReport?> = _todayReport.asStateFlow()

    private val _todayTransactions = MutableStateFlow<List<OrderEntity>>(emptyList())
    val todayTransactions: StateFlow<List<OrderEntity>> = _todayTransactions.asStateFlow()

    init {
        loadTodayReport()
    }

    private fun loadTodayReport() {
        viewModelScope.launch {
            val (start, end) = getTodayRange()

            val sales = orderRepo.getTodaySalesTotal(start, end)
            val count = orderRepo.getTodayOrderCount(start, end)
            val items = orderRepo.getTodayItemsSold(start, end)
            val best = orderRepo.getBestSellingItems(start, end, 5)

            _todayReport.value = DailyReport(
                totalSales = sales,
                totalOrders = count,
                totalItemsSold = items,
                bestSellers = best
            )

            orderRepo.getTodayOrders(start, end).collect { orders ->
                _todayTransactions.value = orders
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        return start to end
    }

    companion object {
        fun factory(orderRepo: OrderRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReportsViewModel(orderRepo) as T
                }
            }
        }
    }
}
