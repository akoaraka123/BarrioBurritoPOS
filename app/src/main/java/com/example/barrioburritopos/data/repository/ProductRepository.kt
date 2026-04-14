package com.example.barrioburritopos.data.repository

import com.example.barrioburritopos.data.local.dao.ProductDao
import com.example.barrioburritopos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {
    fun getAllAvailable(): Flow<List<ProductEntity>> = productDao.getAllAvailable()
    fun getAll(): Flow<List<ProductEntity>> = productDao.getAll()
    suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)
    suspend fun add(product: ProductEntity) = productDao.insert(product)
    suspend fun update(product: ProductEntity) = productDao.update(product)
    suspend fun delete(product: ProductEntity) = productDao.delete(product)
    suspend fun decrementStock(productId: Long, quantity: Int) = productDao.decrementStock(productId, quantity)
    fun getLowStock(threshold: Int): Flow<List<ProductEntity>> = productDao.getLowStock(threshold)
}
