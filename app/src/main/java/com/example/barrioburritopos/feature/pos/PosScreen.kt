package com.example.barrioburritopos.feature.pos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.barrioburritopos.R
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.domain.model.CartItem
import com.example.barrioburritopos.feature.pos.PaymentMethod
import com.example.barrioburritopos.printing.BluetoothReceiptPrinter
import com.example.barrioburritopos.printing.PrintResult
import com.example.barrioburritopos.ui.responsive.rememberResponsiveInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Warm Mexican street food colors
val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val accentYellow = Color(0xFFFFC857)
val orderPanelColor = Color(0xFFFFF1DD)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    products: List<ProductEntity>,
    cart: List<CartItem>,
    total: Double,
    paymentMethod: PaymentMethod,
    cashReceived: String,
    change: Double,
    isCheckoutAllowed: Boolean,
    checkoutStatus: CheckoutStatus?,
    selectedPrinterAddress: String? = null,
    currency: String = "₱",
    showNavBar: Boolean = true,
    onToggleNavBar: () -> Unit = {},
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddToCart: (ProductEntity) -> Unit,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashChange: (String) -> Unit,
    onCheckout: () -> Unit,
    onResetCheckout: () -> Unit
) {
    var swapLayout by remember { mutableStateOf(false) }
    val responsiveInfo = rememberResponsiveInfo()
    val compact = responsiveInfo.isPhone
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val printer = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }
    var pendingPrintReceipt by remember { mutableStateOf<ReceiptData?>(null) }
    fun showTimedSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val snackbarJob = launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Indefinite
                )
            }
            delay(3_000)
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob.cancel()
        }
    }

    val printReceipt: (ReceiptData) -> Unit = { receipt ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPrintReceipt = receipt
        } else {
            scope.launch {
                when (val result = printer.print(receipt, currency, selectedPrinterAddress)) {
                    PrintResult.Success -> showTimedSnackbar("Receipt printed successfully.")
                    PrintResult.PrinterNotConnected -> showTimedSnackbar("Printer not connected")
                    is PrintResult.Error -> showTimedSnackbar(result.message)
                }
                onResetCheckout()
            }
        }
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val receipt = pendingPrintReceipt
        pendingPrintReceipt = null
        if (granted && receipt != null) {
            scope.launch {
                when (val result = printer.print(receipt, currency, selectedPrinterAddress)) {
                    PrintResult.Success -> showTimedSnackbar("Receipt printed successfully.")
                    PrintResult.PrinterNotConnected -> showTimedSnackbar("Printer not connected")
                    is PrintResult.Error -> showTimedSnackbar(result.message)
                }
                onResetCheckout()
            }
        } else {
            showTimedSnackbar("Bluetooth permission not granted")
            onResetCheckout()
        }
    }

    LaunchedEffect(pendingPrintReceipt) {
        if (pendingPrintReceipt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Barrio Burrito Logo",
                            modifier = Modifier
                                .size(if (compact) 32.dp else 40.dp)
                                .padding(end = if (compact) 6.dp else 8.dp)
                        )
                        Text(
                            "Barrio Burrito POS",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleNavBar) {
                        Icon(
                            imageVector = if (showNavBar) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showNavBar) "Hide navigation bar" else "Show navigation bar",
                            tint = accentRed
                        )
                    }
                    IconButton(onClick = { swapLayout = !swapLayout }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = if (swapLayout) "Reset layout" else "Swap layout",
                            tint = if (swapLayout) accentRed else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (swapLayout) {
                    MenuPanel(
                        products = products,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onAddToCart = onAddToCart,
                        compact = true,
                        modifier = Modifier.weight(0.85f)
                    )

                    OrderPanel(
                        cart = cart,
                        total = total,
                        paymentMethod = paymentMethod,
                        cashReceived = cashReceived,
                        change = change,
                        isCheckoutAllowed = isCheckoutAllowed,
                        checkoutStatus = checkoutStatus,
                        currency = currency,
                        onIncreaseQty = onIncreaseQty,
                        onDecreaseQty = onDecreaseQty,
                        onRemove = onRemoveFromCart,
                        onClear = onClearCart,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onCashChange = onCashChange,
                        onCheckout = onCheckout,
                        onResetCheckout = onResetCheckout,
                        onPrintReceipt = printReceipt,
                        onCheckoutSuccessMessage = { showTimedSnackbar("Checkout successful.") },
                        compact = true,
                        modifier = Modifier.weight(1.55f)
                    )
                } else {
                    OrderPanel(
                        cart = cart,
                        total = total,
                        paymentMethod = paymentMethod,
                        cashReceived = cashReceived,
                        change = change,
                        isCheckoutAllowed = isCheckoutAllowed,
                        checkoutStatus = checkoutStatus,
                        currency = currency,
                        onIncreaseQty = onIncreaseQty,
                        onDecreaseQty = onDecreaseQty,
                        onRemove = onRemoveFromCart,
                        onClear = onClearCart,
                        onPaymentMethodChange = onPaymentMethodChange,
                        onCashChange = onCashChange,
                        onCheckout = onCheckout,
                        onResetCheckout = onResetCheckout,
                        onPrintReceipt = printReceipt,
                        onCheckoutSuccessMessage = { showTimedSnackbar("Checkout successful.") },
                        compact = true,
                        modifier = Modifier.weight(1.55f)
                    )

                    MenuPanel(
                        products = products,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        onAddToCart = onAddToCart,
                        compact = true,
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }
        } else {
            Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (swapLayout) {
                // Swapped: Left: Order/Cart, Right: Menu
                OrderPanel(
                    cart = cart,
                    total = total,
                    paymentMethod = paymentMethod,
                    cashReceived = cashReceived,
                    change = change,
                    isCheckoutAllowed = isCheckoutAllowed,
                    checkoutStatus = checkoutStatus,
                    currency = currency,
                    onIncreaseQty = onIncreaseQty,
                    onDecreaseQty = onDecreaseQty,
                    onRemove = onRemoveFromCart,
                    onClear = onClearCart,
                    onPaymentMethodChange = onPaymentMethodChange,
                    onCashChange = onCashChange,
                    onCheckout = onCheckout,
                    onResetCheckout = onResetCheckout,
                    onPrintReceipt = printReceipt,
                    onCheckoutSuccessMessage = { showTimedSnackbar("Checkout successful.") },
                    modifier = Modifier.weight(1.2f)
                )

                MenuPanel(
                    products = products,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onAddToCart = onAddToCart,
                    modifier = Modifier.weight(1.5f)
                )
            } else {
                // Default: Left: Menu, Right: Order/Cart
                MenuPanel(
                    products = products,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onAddToCart = onAddToCart,
                    modifier = Modifier.weight(1.5f)
                )

                OrderPanel(
                    cart = cart,
                    total = total,
                    paymentMethod = paymentMethod,
                    cashReceived = cashReceived,
                    change = change,
                    isCheckoutAllowed = isCheckoutAllowed,
                    checkoutStatus = checkoutStatus,
                    currency = currency,
                    onIncreaseQty = onIncreaseQty,
                    onDecreaseQty = onDecreaseQty,
                    onRemove = onRemoveFromCart,
                    onClear = onClearCart,
                    onPaymentMethodChange = onPaymentMethodChange,
                    onCashChange = onCashChange,
                    onCheckout = onCheckout,
                    onResetCheckout = onResetCheckout,
                    onPrintReceipt = printReceipt,
                    onCheckoutSuccessMessage = { showTimedSnackbar("Checkout successful.") },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
        }
    }
}
@Composable
private fun CartItemText(item: CartItem, currency: String) {
    Text(
        text = item.name,
        fontWeight = FontWeight.Bold,
        color = darkText,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    if (!item.details.isNullOrBlank()) {
        Text(
            text = item.details,
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
    Text(
        text = "$currency${String.format("%,.2f", item.subtotal)}",
        color = accentRed,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun CompactCartButton(text: String, onClick: () -> Unit, color: Color) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
            color = color
        )
    }
}

@Composable
fun MenuPanel(
    products: List<ProductEntity>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddToCart: (ProductEntity) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            text = "Menu",
            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = darkText
        )
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = accentRed)
            },
            singleLine = true,
            shape = RoundedCornerShape(if (compact) 10.dp else 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentRed,
                unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onAdd = { onAddToCart(product) },
                    compact = compact
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAdd: () -> Unit,
    compact: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(if (compact) 12.dp else 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "₱${product.price}",
                    color = accentRed,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (product.stockQuantity <= 10) {
                    Text(
                        text = "Low stock: ${product.stockQuantity}",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.height(if (compact) 36.dp else 40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                shape = RoundedCornerShape(if (compact) 10.dp else 12.dp)
            ) {
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderPanel(
    cart: List<CartItem>,
    total: Double,
    paymentMethod: PaymentMethod,
    cashReceived: String,
    change: Double,
    isCheckoutAllowed: Boolean,
    checkoutStatus: CheckoutStatus?,
    currency: String,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashChange: (String) -> Unit,
    onCheckout: () -> Unit,
    onResetCheckout: () -> Unit,
    onPrintReceipt: (ReceiptData) -> Unit,
    onCheckoutSuccessMessage: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<CartItem?>(null) }
    
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                OrderPanelContent(
                    cart = cart,
                    total = total,
                    paymentMethod = paymentMethod,
                    cashReceived = cashReceived,
                    change = change,
                    isCheckoutAllowed = isCheckoutAllowed,
                    currency = currency,
                    onIncreaseQty = onIncreaseQty,
                    onDecreaseQty = onDecreaseQty,
                    onRemove = onRemove,
                    onClear = onClear,
                    onPaymentMethodChange = onPaymentMethodChange,
                    onCashChange = onCashChange,
                    onCheckout = onCheckout,
                    onItemClick = { selectedItem = it },
                    compact = true,
                    itemListModifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 130.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OrderPanelContent(
                    cart = cart,
                    total = total,
                    paymentMethod = paymentMethod,
                    cashReceived = cashReceived,
                    change = change,
                    isCheckoutAllowed = isCheckoutAllowed,
                    currency = currency,
                    onIncreaseQty = onIncreaseQty,
                    onDecreaseQty = onDecreaseQty,
                    onRemove = onRemove,
                    onClear = onClear,
                    onPaymentMethodChange = onPaymentMethodChange,
                    onCashChange = onCashChange,
                    onCheckout = onCheckout,
                    onItemClick = { selectedItem = it },
                    compact = false,
                    itemListModifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }
    }

    // Checkout status dialogs
    when (checkoutStatus) {
        is CheckoutStatus.Success -> {
            PrintReceiptDialog(
                onPrint = {
                    val receipt = checkoutStatus.receipt
                    onResetCheckout()
                    onCheckoutSuccessMessage()
                    onPrintReceipt(receipt)
                },
                onSkip = {
                    onResetCheckout()
                    onCheckoutSuccessMessage()
                }
            )
        }
        is CheckoutStatus.Error -> {
            AlertDialog(
                onDismissRequest = onResetCheckout,
                confirmButton = {
                    Button(onClick = onResetCheckout) { Text("OK") }
                },
                title = { Text("Checkout Error") },
                text = { Text(checkoutStatus.message) }
            )
        }
        null -> {}
    }

    // Cart item details dialog
    selectedItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            confirmButton = {
                TextButton(onClick = { selectedItem = null }) { Text("Close", color = Color.Black) }
            },
            title = {
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(
                        text = "$currency${String.format("%,.2f", item.price)} x ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentRed
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.details.isNullOrBlank()) {
                        Text("No customizations", color = Color.Black)
                    } else {
                        // Parse details and display them nicely
                        val detailsLines = item.details.split(", ")
                        detailsLines.forEach { detail ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0))
                            ) {
                                Text(
                                    text = detail.trim(),
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Subtotal: $currency${String.format("%,.2f", item.subtotal)}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun OrderPanelContent(
    cart: List<CartItem>,
    total: Double,
    paymentMethod: PaymentMethod,
    cashReceived: String,
    change: Double,
    isCheckoutAllowed: Boolean,
    currency: String,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onCashChange: (String) -> Unit,
    onCheckout: () -> Unit,
    onItemClick: (CartItem) -> Unit,
    compact: Boolean,
    itemListModifier: Modifier
) {
    Text(
        text = "Current Order",
        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = darkText
    )
    Spacer(Modifier.height(8.dp))

    // Top section: Scrollable items list
    Column(
        modifier = itemListModifier
    ) {
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items added yet",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                cart.forEach { item ->
                    CartItemRow(
                        item = item,
                        currency = currency,
                        onIncrease = { onIncreaseQty(item.lineId) },
                        onDecrease = { onDecreaseQty(item.lineId) },
                        onRemove = { onRemove(item.lineId) },
                        onClick = { onItemClick(item) },
                        compact = compact
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))

    // Bottom section: Fixed summary/actions
    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 4.dp)
    ) {
        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total:", fontWeight = FontWeight.Bold, color = darkText)
            Text(
                "$currency${String.format("%,.2f", total)}",
                fontWeight = FontWeight.Bold,
                color = accentRed,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
            )
        }

        // Payment method selector
        Text(
            text = "Payment Method",
            fontWeight = FontWeight.Bold,
            color = darkText
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = paymentMethod == PaymentMethod.CASH,
                onClick = { onPaymentMethodChange(PaymentMethod.CASH) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.height(if (compact) 36.dp else 32.dp)
            ) {
                Text("Cash", style = MaterialTheme.typography.bodyMedium)
            }
            SegmentedButton(
                selected = paymentMethod == PaymentMethod.GCASH,
                onClick = { onPaymentMethodChange(PaymentMethod.GCASH) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.height(if (compact) 36.dp else 32.dp)
            ) {
                Text("GCash", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Amount received (Cash only)
        if (paymentMethod == PaymentMethod.CASH) {
            val received = cashReceived.toDoubleOrNull()
            val showInsufficient = cart.isNotEmpty() && (received == null || received < total)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                OutlinedTextField(
                    value = cashReceived,
                    onValueChange = { if (it.length <= 5) onCashChange(it) },
                    label = { Text("Amount Received", color = Color.Black) },
                    prefix = { Text(currency, color = Color.Black) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    singleLine = true,
                    isError = showInsufficient,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = darkText,
                        unfocusedTextColor = darkText,
                        cursorColor = darkText,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = accentRed,
                        unfocusedIndicatorColor = Color.Gray
                    )
                )
            }

            if (showInsufficient) {
                Text(
                    text = "Insufficient payment",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Change:", fontWeight = FontWeight.Bold, color = darkText)
                Text(
                    "$currency${String.format("%,.2f", change)}",
                    fontWeight = FontWeight.Bold,
                    color = if (change > 0) Color(0xFF2E7D32) else Color.Gray,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Text(
                text = "Cashless payment - no change needed",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 44.dp else 40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Clear")
            }
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 44.dp else 40.dp),
                enabled = isCheckoutAllowed,
                colors = ButtonDefaults.buttonColors(containerColor = accentYellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Checkout", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PrintReceiptDialog(
    onPrint: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Print Receipt?", fontWeight = FontWeight.Bold) },
        text = { Text("Do you want to print the receipt?") },
        confirmButton = {
            Button(
                onClick = onPrint,
                colors = ButtonDefaults.buttonColors(containerColor = accentRed)
            ) {
                Text("Yes", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("No", color = Color.Gray)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun CartItemRow(
    item: CartItem,
    currency: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit = {},
    compact: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CartItemText(item = item, currency = currency)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactCartButton(text = "âˆ’", onClick = onDecrease, color = darkText)
                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        color = darkText,
                        modifier = Modifier.width(28.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    CompactCartButton(text = "+", onClick = onIncrease, color = darkText)
                    CompactCartButton(text = "Ã—", onClick = onRemove, color = Color.Red)
                }
            }
        } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = darkText
                )
                if (!item.details.isNullOrBlank()) {
                    Text(
                        text = item.details,
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 3
                    )
                }
                Text(
                    text = "$currency${String.format("%,.2f", item.subtotal)}",
                    color = accentRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onDecrease) {
                    Text("−", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize, color = darkText)
                }
                Text(
                    text = "${item.quantity}",
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    modifier = Modifier.width(32.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = onIncrease) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize, color = darkText)
                }
                IconButton(onClick = onRemove) {
                    Text("×", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                }
            }
        }
    }
}
}
