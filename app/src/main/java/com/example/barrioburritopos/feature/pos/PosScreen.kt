package com.example.barrioburritopos.feature.pos

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.barrioburritopos.R
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.domain.model.CartItem
import com.example.barrioburritopos.feature.pos.PaymentMethod

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
    Scaffold(
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
                                .height(40.dp)
                                .width(40.dp)
                                .padding(end = 8.dp)
                        )
                        Text("Barrio Burrito POS", fontWeight = FontWeight.Bold, color = Color.Black)
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
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
fun MenuPanel(
    products: List<ProductEntity>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            text = "Menu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = darkText
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search products...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = accentRed)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentRed,
                unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onAdd = { onAddToCart(product) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    style = MaterialTheme.typography.titleMedium
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
            Button(
                onClick = onAdd,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                shape = RoundedCornerShape(12.dp)
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
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<CartItem?>(null) }
    
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Current Order",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = darkText
            )
            Spacer(Modifier.height(8.dp))

            // Top section: Scrollable items list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (cart.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                onClick = { selectedItem = item }
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        style = MaterialTheme.typography.titleLarge
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
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Cash", style = MaterialTheme.typography.bodyMedium)
                    }
                    SegmentedButton(
                        selected = paymentMethod == PaymentMethod.GCASH,
                        onClick = { onPaymentMethodChange(PaymentMethod.GCASH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.height(32.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear")
                    }
                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.weight(1f),
                        enabled = isCheckoutAllowed,
                        colors = ButtonDefaults.buttonColors(containerColor = accentYellow),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Checkout", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Checkout status dialogs
    when (checkoutStatus) {
        is CheckoutStatus.Success -> {
            AlertDialog(
                onDismissRequest = onResetCheckout,
                confirmButton = {
                    Button(onClick = onResetCheckout) { Text("OK") }
                },
                title = { Text("Checkout Successful") },
                text = { Text("Change: $currency${"%.2f".format(checkoutStatus.change)}") }
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
fun CartItemRow(
    item: CartItem,
    currency: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
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
