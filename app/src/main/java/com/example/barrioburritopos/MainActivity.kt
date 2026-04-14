package com.example.barrioburritopos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.barrioburritopos.data.local.db.BarrioBurritoDatabase
import com.example.barrioburritopos.data.repository.OrderRepository
import com.example.barrioburritopos.data.repository.ProductRepository
import com.example.barrioburritopos.feature.history.HistoryViewModel
import com.example.barrioburritopos.feature.inventory.InventoryViewModel
import com.example.barrioburritopos.feature.pos.PosViewModel
import com.example.barrioburritopos.feature.reports.ReportsViewModel
import com.example.barrioburritopos.feature.settings.SettingsViewModel
import com.example.barrioburritopos.navigation.AppNavigation
import com.example.barrioburritopos.ui.theme.BarrioBurritoPOSTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val database by lazy { BarrioBurritoDatabase.getDatabase(this) }
    private val productRepo by lazy { ProductRepository(database.productDao()) }
    private val orderRepo by lazy { OrderRepository(database.orderDao()) }

    private val posViewModel by viewModels<PosViewModel> {
        PosViewModel.factory(productRepo, orderRepo)
    }
    private val inventoryViewModel by viewModels<InventoryViewModel> {
        InventoryViewModel.factory(productRepo)
    }
    private val historyViewModel by viewModels<HistoryViewModel> {
        HistoryViewModel.factory(orderRepo)
    }
    private val reportsViewModel by viewModels<ReportsViewModel> {
        ReportsViewModel.factory(orderRepo)
    }
    private val settingsViewModel by viewModels<SettingsViewModel> {
        SettingsViewModel.factory(this, orderRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe settings for currency
        var currency = "₱"
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.currency.collectLatest { curr ->
                    currency = curr
                }
            }
        }

        setContent {
            BarrioBurritoPOSTheme {
                AppNavigation(
                    posViewModel = posViewModel,
                    inventoryViewModel = inventoryViewModel,
                    historyViewModel = historyViewModel,
                    reportsViewModel = reportsViewModel,
                    settingsViewModel = settingsViewModel,
                    currency = currency
                )
            }
        }
    }
}