package com.example.barrioburritopos.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.CustomizeStepType
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.data.repository.CustomizeOptionRepository
import com.example.barrioburritopos.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val productRepo: ProductRepository,
    private val customizeOptionRepo: CustomizeOptionRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _lowStockProducts = MutableStateFlow<List<ProductEntity>>(emptyList())
    val lowStockProducts: StateFlow<List<ProductEntity>> = _lowStockProducts.asStateFlow()

    private val _riceOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val riceOptions: StateFlow<List<CustomizeOptionEntity>> = _riceOptions.asStateFlow()

    private val _mainOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val mainOptions: StateFlow<List<CustomizeOptionEntity>> = _mainOptions.asStateFlow()

    private val _baseOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val baseOptions: StateFlow<List<CustomizeOptionEntity>> = _baseOptions.asStateFlow()

    private val _toppingOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val toppingOptions: StateFlow<List<CustomizeOptionEntity>> = _toppingOptions.asStateFlow()

    private val _sauceOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val sauceOptions: StateFlow<List<CustomizeOptionEntity>> = _sauceOptions.asStateFlow()

    private val _addOnOptions = MutableStateFlow<List<CustomizeOptionEntity>>(emptyList())
    val addOnOptions: StateFlow<List<CustomizeOptionEntity>> = _addOnOptions.asStateFlow()

    val lowStockThreshold = MutableStateFlow(10)

    init {
        loadProducts()
        loadCustomizeOptions()
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

    private fun loadCustomizeOptions() {
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.RICE).collect { list ->
                _riceOptions.value = list
            }
        }
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.MAIN).collect { list ->
                _mainOptions.value = list
            }
        }
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.BASE).collect { list ->
                _baseOptions.value = list
            }
        }
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.TOPPING).collect { list ->
                _toppingOptions.value = list
            }
        }
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.SAUCE).collect { list ->
                _sauceOptions.value = list
            }
        }
        viewModelScope.launch {
            customizeOptionRepo.getByStepType(CustomizeStepType.ADDON).collect { list ->
                _addOnOptions.value = list
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

    fun addCustomizeOption(stepType: CustomizeStepType, name: String, price: Double = 0.0, imageUri: String? = null) {
        viewModelScope.launch {
            // If it's a drawable resource, store as string with drawable:// prefix
            // Otherwise, convert to Uri for file picker
            val uri = if (imageUri?.startsWith("drawable://") == true) {
                imageUri
            } else {
                imageUri?.let { android.net.Uri.parse(it).toString() }
            }
            customizeOptionRepo.addOption(stepType, name, price, uri)
        }
    }

    fun updateCustomizeOption(option: CustomizeOptionEntity) {
        viewModelScope.launch {
            customizeOptionRepo.updateOption(option)
        }
    }

    fun deleteCustomizeOption(stepName: String, optionName: String) {
        viewModelScope.launch {
            val stepType = when (stepName) {
                "PICK YOUR RICE" -> CustomizeStepType.RICE
                "PICK YOUR MAIN" -> CustomizeStepType.MAIN
                "PICK YOUR BASE" -> CustomizeStepType.BASE
                "PICK YOUR TOPPING" -> CustomizeStepType.TOPPING
                "PICK YOUR SAUCE" -> CustomizeStepType.SAUCE
                "Add-ons" -> CustomizeStepType.ADDON
                else -> return@launch
            }
            val option = customizeOptionRepo.getOptionByName(optionName, stepType) ?: return@launch
            customizeOptionRepo.deleteOption(option)
        }
    }

    companion object {
        fun factory(productRepo: ProductRepository, customizeOptionRepo: CustomizeOptionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InventoryViewModel(productRepo, customizeOptionRepo) as T
                }
            }
        }
    }
}
