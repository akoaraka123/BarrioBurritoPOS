package com.example.barrioburritopos.data.local.dao

import androidx.room.*
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY dateTime DESC")
    fun getAll(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE dateTime >= :startOfDay AND dateTime < :endOfDay ORDER BY dateTime DESC")
    fun getTodayOrders(startOfDay: Long, endOfDay: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getById(orderId: Long): OrderEntity?

    @Insert
    suspend fun insert(order: OrderEntity): Long

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Update
    suspend fun updateOrderItem(item: OrderItemEntity)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: Long)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT SUM(totalAmount) FROM orders WHERE dateTime >= :startOfDay AND dateTime < :endOfDay")
    suspend fun getTodaySalesTotal(startOfDay: Long, endOfDay: Long): Double?

    @Query(
        "SELECT SUM(totalAmount) FROM orders WHERE dateTime >= :startOfDay AND dateTime < :endOfDay AND paymentMethod = :paymentMethod"
    )
    suspend fun getTodaySalesTotalByPaymentMethod(
        startOfDay: Long,
        endOfDay: Long,
        paymentMethod: String
    ): Double?

    @Query("SELECT COUNT(*) FROM orders WHERE dateTime >= :startOfDay AND dateTime < :endOfDay")
    suspend fun getTodayOrderCount(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT SUM(totalItems) FROM orders WHERE dateTime >= :startOfDay AND dateTime < :endOfDay")
    suspend fun getTodayItemsSold(startOfDay: Long, endOfDay: Long): Int?

    @Query("""
        SELECT productName, SUM(quantity) as totalQty 
        FROM order_items 
        JOIN orders ON orders.id = order_items.orderId 
        WHERE orders.dateTime >= :startOfDay AND orders.dateTime < :endOfDay 
        GROUP BY productName 
        ORDER BY totalQty DESC 
        LIMIT :limit
    """)
    suspend fun getBestSellingItems(startOfDay: Long, endOfDay: Long, limit: Int): List<BestSeller>

    @Query("DELETE FROM orders")
    suspend fun deleteAll()

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteById(orderId: Long)
}

data class BestSeller(
    val productName: String,
    val totalQty: Int
)
