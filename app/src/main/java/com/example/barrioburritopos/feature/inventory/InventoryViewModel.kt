package com.example.barrioburritopos.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(private val productRepo: ProductRepository) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _lowStockProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val lowStockProducts: StateFlow<List<ProductEntity>> = _lowStockProducts.asStateFlow()

    val lowStockThreshold = MutableStateFlow(10)

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            productRepo.getAll().collect { list ->
                _products.value = list
            }
        }
        viewModelScope.launch {
            productRepo.getLowStock(lowStockThreshold.value).collect { list ->
                _lowStockProducts.value = list
            }
        }
    }

    fun addProduct(name: String, price: Double, stock: Int, category: String?) {
        viewModelScope.launch {
            val product = ProductEntity(
                name = name,
                price = price,
                stockQuantity = stock,
                category = category,
                isAvailable = true
            )
            productRepo.add(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepo.update(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productRepo.delete(product)
        }
    }

    fun toggleAvailability(product: ProductEntity) {
        viewModelScope.launch {
            productRepo.update(product.copy(isAvailable = !product.isAvailable))
        }
    }

    fun restock(productId: Long, amount: Int) {
        viewModelScope.launch {
            val product = productRepo.getById(productId) ?: return@launch
            productRepo.update(product.copy(stockQuantity = product.stockQuantity + amount))
        }
    }

    companion object {
        fun factory(productRepo: ProductRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InventoryViewModel(productRepo) as T
                }
            }
        }
    }
}
