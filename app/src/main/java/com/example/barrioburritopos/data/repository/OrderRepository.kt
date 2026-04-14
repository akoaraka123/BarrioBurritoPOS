package com.example.barrioburritopos.data.repository

import com.example.barrioburritopos.data.local.dao.BestSeller
import com.example.barrioburritopos.data.local.dao.OrderDao
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

class OrderRepository(private val orderDao: OrderDao) {
    fun getAll(): Flow<List<OrderEntity>> = orderDao.getAll()
    fun getTodayOrders(startOfDay: Long, endOfDay: Long): Flow<List<OrderEntity>> = orderDao.getTodayOrders(startOfDay, endOfDay)
    suspend fun getById(id: Long): OrderEntity? = orderDao.getById(id)
    suspend fun getItems(orderId: Long): List<OrderItemEntity> = orderDao.getItemsForOrder(orderId)
    suspend fun createOrder(order: OrderEntity, items: List<OrderItemEntity>): Long {
        val orderId = orderDao.insert(order)
        val itemsWithId = items.map { it.copy(orderId = orderId) }
        orderDao.insertItems(itemsWithId)
        return orderId
    }
    suspend fun getTodaySalesTotal(start: Long, end: Long): Double = orderDao.getTodaySalesTotal(start, end) ?: 0.0
    suspend fun getTodayOrderCount(start: Long, end: Long): Int = orderDao.getTodayOrderCount(start, end)
    suspend fun getTodayItemsSold(start: Long, end: Long): Int = orderDao.getTodayItemsSold(start, end) ?: 0
    suspend fun getBestSellingItems(start: Long, end: Long, limit: Int): List<BestSeller> = orderDao.getBestSellingItems(start, end, limit)
    suspend fun deleteAll() = orderDao.deleteAll()
}
