package com.example.barrioburritopos.domain.model

import com.example.barrioburritopos.data.local.dao.BestSeller

data class DailyReport(
    val totalSales: Double,
    val totalOrders: Int,
    val totalItemsSold: Int,
    val bestSellers: List<BestSeller>
)
