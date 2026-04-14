package com.example.barrioburritopos.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.barrioburritopos.feature.history.HistoryScreen
import com.example.barrioburritopos.feature.inventory.InventoryScreen
import com.example.barrioburritopos.feature.pos.PosScreen
import com.example.barrioburritopos.feature.pos.PosViewModel
import com.example.barrioburritopos.feature.reports.ReportsScreen
import com.example.barrioburritopos.feature.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Pos : Screen("pos", "POS", Icons.Default.Home)
    object History : Screen("history", "History", Icons.Default.Notifications)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.List)
    object Reports : Screen("reports", "Reports", Icons.Default.Star)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val screens = listOf(
    Screen.Pos,
    Screen.History,
    Screen.Inventory,
    Screen.Reports,
    Screen.Settings
)

val navBarColor = Color(0xFFFFF8F0)
val navSelectedColor = Color(0xFFC94F2D)
val navUnselectedColor = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    posViewModel: PosViewModel,
    inventoryViewModel: com.example.barrioburritopos.feature.inventory.InventoryViewModel,
    historyViewModel: com.example.barrioburritopos.feature.history.HistoryViewModel,
    reportsViewModel: com.example.barrioburritopos.feature.reports.ReportsViewModel,
    settingsViewModel: com.example.barrioburritopos.feature.settings.SettingsViewModel,
    currency: String
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = navSelectedColor,
                            selectedTextColor = navSelectedColor,
                            unselectedIconColor = navUnselectedColor,
                            unselectedTextColor = navUnselectedColor,
                            indicatorColor = Color(0xFFFFE8CC)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Pos.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Pos.route) {
                val products by posViewModel.products.collectAsStateWithLifecycle()
                val cart by posViewModel.cart.collectAsStateWithLifecycle()
                val total by posViewModel.total.collectAsStateWithLifecycle()
                val cashReceived by posViewModel.cashReceived.collectAsStateWithLifecycle()
                val change by posViewModel.change.collectAsStateWithLifecycle()
                val checkoutStatus by posViewModel.checkoutStatus.collectAsStateWithLifecycle()

                PosScreen(
                    products = products,
                    cart = cart,
                    total = total,
                    cashReceived = cashReceived,
                    change = change,
                    checkoutStatus = checkoutStatus,
                    currency = currency,
                    onAddToCart = posViewModel::addToCart,
                    onIncreaseQty = posViewModel::increaseQuantity,
                    onDecreaseQty = posViewModel::decreaseQuantity,
                    onRemoveFromCart = posViewModel::removeFromCart,
                    onClearCart = posViewModel::clearCart,
                    onCashChange = posViewModel::onCashReceivedChange,
                    onCheckout = posViewModel::checkout,
                    onResetCheckout = posViewModel::resetCheckoutStatus
                )
            }
            composable(Screen.History.route) {
                val transactions by historyViewModel.filteredTransactions.collectAsStateWithLifecycle()
                val searchQuery by historyViewModel.searchQuery.collectAsStateWithLifecycle()

                HistoryScreen(
                    transactions = transactions,
                    searchQuery = searchQuery,
                    currency = currency,
                    onSearchChange = historyViewModel::onSearchChange
                )
            }
            composable(Screen.Inventory.route) {
                val products by inventoryViewModel.products.collectAsStateWithLifecycle()
                val lowStock by inventoryViewModel.lowStockProducts.collectAsStateWithLifecycle()

                InventoryScreen(
                    products = products,
                    lowStockProducts = lowStock,
                    currency = currency,
                    onAddProduct = inventoryViewModel::addProduct,
                    onToggleAvailability = inventoryViewModel::toggleAvailability,
                    onRestock = inventoryViewModel::restock,
                    onDelete = inventoryViewModel::deleteProduct
                )
            }
            composable(Screen.Reports.route) {
                val report by reportsViewModel.todayReport.collectAsStateWithLifecycle()
                val transactions by reportsViewModel.todayTransactions.collectAsStateWithLifecycle()

                ReportsScreen(
                    report = report,
                    transactions = transactions,
                    currency = currency
                )
            }
            composable(Screen.Settings.route) {
                val businessName by settingsViewModel.businessName.collectAsStateWithLifecycle()
                val currencySetting by settingsViewModel.currency.collectAsStateWithLifecycle()
                val lowStockThreshold by settingsViewModel.lowStockThreshold.collectAsStateWithLifecycle()

                val scope = androidx.compose.runtime.rememberCoroutineScope()
                SettingsScreen(
                    businessName = businessName,
                    currency = currencySetting,
                    lowStockThreshold = lowStockThreshold,
                    onBusinessNameChange = settingsViewModel::setBusinessName,
                    onCurrencyChange = settingsViewModel::setCurrency,
                    onLowStockChange = settingsViewModel::setLowStockThreshold,
                    onClearTransactions = {
                        scope.launch {
                            settingsViewModel.clearAllTransactions()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> StateFlow<T>.collectAsStateWithLifecycle() = collectAsState()
