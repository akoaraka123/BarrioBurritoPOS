package com.example.barrioburritopos.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.domain.model.DailyReport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DailyReportWithDate(
    val date: String,
    val dateMillis: Long,
    val report: DailyReport
)

class ReportsViewModel(private val orderRepo: OrderRepository) : ViewModel() {

    private val _dailyReports = MutableStateFlow<List<DailyReportWithDate>>(emptyList())
    val dailyReports: StateFlow<List<DailyReportWithDate>> = _dailyReports.asStateFlow()

    private val _selectedDayTransactions = MutableStateFlow<List<OrderEntity>>(emptyList())
    val selectedDayTransactions: StateFlow<List<OrderEntity>> = _selectedDayTransactions.asStateFlow()

    init {
        loadDailyReports()
    }

    private fun loadDailyReports() {
        viewModelScope.launch {
            // Get all orders and group by day
            orderRepo.getAll().collect { allOrders ->
                val groupedOrders = allOrders.groupBy { order ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
                    cal.timeInMillis = order.dateTime
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }

                val reports = groupedOrders.map { (dayMillis, orders) ->
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                    val date = sdf.format(Date(dayMillis))

                    val sales = orders.sumOf { it.totalAmount }
                    val cashSales = orders.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
                    val gcashSales = orders.filter { it.paymentMethod == "GCASH" }.sumOf { it.totalAmount }
                    val count = orders.size
                    val items = orders.sumOf { it.totalItems }

                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
                    cal.timeInMillis = dayMillis
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val endOfDay = cal.timeInMillis

                    val best = orderRepo.getBestSellingItems(dayMillis, endOfDay, 5)

                    DailyReportWithDate(
                        date = date,
                        dateMillis = dayMillis,
                        report = DailyReport(
                            totalSales = sales,
                            cashSales = cashSales,
                            gcashSales = gcashSales,
                            totalOrders = count,
                            totalItemsSold = items,
                            bestSellers = best
                        )
                    )
                }.sortedByDescending { it.dateMillis }

                _dailyReports.value = reports
            }
        }
    }

    fun loadTransactionsForDay(dateMillis: Long) {
        viewModelScope.launch {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
            cal.timeInMillis = dateMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = cal.timeInMillis

            orderRepo.getTodayOrders(dateMillis, endOfDay).collect { orders ->
                _selectedDayTransactions.value = orders
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
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
