package com.example.barrioburritopos.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.barrioburritopos.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsViewModel(
    context: Context,
    private val orderRepo: OrderRepository
) : ViewModel() {

    private val dataStore = context.dataStore

    private val BUSINESS_NAME = stringPreferencesKey("business_name")
    private val CURRENCY = stringPreferencesKey("currency")
    private val LOW_STOCK_THRESHOLD = intPreferencesKey("low_stock_threshold")
    private val PIN = stringPreferencesKey("pin")

    val businessName: StateFlow<String> = dataStore.data
        .map { it[BUSINESS_NAME] ?: "Barrio Burrito" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Barrio Burrito")

    val currency: StateFlow<String> = dataStore.data
        .map { it[CURRENCY] ?: "₱" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₱")

    val lowStockThreshold: StateFlow<Int> = dataStore.data
        .map { it[LOW_STOCK_THRESHOLD] ?: 10 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val pin: StateFlow<String?> = dataStore.data
        .map { it[PIN] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setBusinessName(name: String) {
        viewModelScope.launch {
            dataStore.edit { it[BUSINESS_NAME] = name }
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            dataStore.edit { it[CURRENCY] = currency }
        }
    }

    fun setLowStockThreshold(threshold: Int) {
        viewModelScope.launch {
            dataStore.edit { it[LOW_STOCK_THRESHOLD] = threshold }
        }
    }

    fun setPin(newPin: String) {
        viewModelScope.launch {
            dataStore.edit { it[PIN] = newPin }
        }
    }

    fun clearPin() {
        viewModelScope.launch {
            dataStore.edit { it.remove(PIN) }
        }
    }

    fun validatePin(inputPin: String): Boolean {
        return pin.value == inputPin
    }

    suspend fun clearAllTransactions() {
        orderRepo.deleteAll()
    }

    companion object {
        fun factory(context: Context, orderRepo: OrderRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(context, orderRepo) as T
                }
            }
        }
    }
}
