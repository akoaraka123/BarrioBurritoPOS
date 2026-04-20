package com.example.barrioburritopos.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.domain.model.Transaction

val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    searchQuery: String,
    currency: String = "₱",
    onSearchChange: (String) -> Unit,
    onEditOrder: (Long, List<OrderItemEntity>, Double, Double) -> Unit = { _, _, _, _ -> },
    onDeleteOrder: (Long) -> Unit = {},
    products: List<com.example.barrioburritopos.data.local.entity.ProductEntity> = emptyList()
) {
    var editingOrderId by remember { mutableStateOf<Long?>(null) }
    var editingItems by remember { mutableStateOf<List<OrderItemEntity>>(emptyList()) }
    var isLocked by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order History", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
                actions = {
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = if (isLocked) "Unlock" else "Lock",
                            tint = if (isLocked) Color.Red else Color.Green
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search orders...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Complete a checkout to see history here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions) { tx ->
                        TransactionCard(
                            tx,
                            currency,
                            isLocked,
                            onEditOrder = {
                                editingOrderId = tx.id
                                editingItems = tx.items
                            },
                            onDeleteOrder = { onDeleteOrder(tx.id) }
                        )
                    }
                }
            }
        }
    }

    editingOrderId?.let { orderId ->
        val order = transactions.find { it.id == orderId }
        order?.let {
            EditOrderDialog(
                orderId = orderId,
                items = editingItems,
                originalTotal = order.totalAmount,
                products = products,
                onDismiss = { editingOrderId = null },
                onUpdate = { id, items, priceDiff, additionalPayment -> onEditOrder(id, items, priceDiff, additionalPayment) }
            )
        }
    }
}

@Composable
fun TransactionCard(
    tx: Transaction,
    currency: String,
    isLocked: Boolean,
    onEditOrder: () -> Unit = {},
    onDeleteOrder: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${tx.id}",
                    fontWeight = FontWeight.Bold,
                    color = darkText,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.dateTimeFormatted,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onEditOrder) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accentRed)
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !isLocked
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (isLocked) Color.Gray else Color.Red
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            tx.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.productName} × ${item.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentRed
                    )
                    Text(
                        text = "$currency${"%.2f".format(item.subtotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentRed
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Payment: ${tx.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Received: $currency${"%.2f".format(tx.amountReceived ?: tx.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "$currency${"%.2f".format(tx.changeAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${tx.totalItems} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Total: $currency${"%.2f".format(tx.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    color = accentRed,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Order") },
            text = { Text("Are you sure you want to delete Order #${tx.id}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrder()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditOrderDialog(
    orderId: Long,
    items: List<OrderItemEntity>,
    originalTotal: Double,
    products: List<com.example.barrioburritopos.data.local.entity.ProductEntity>,
    onDismiss: () -> Unit,
    onUpdate: (Long, List<OrderItemEntity>, Double, Double) -> Unit
) {
    var editedItems by remember { mutableStateOf(items.map { it.copy() }) }
    var showAdditionalPaymentDialog by remember { mutableStateOf(false) }
    var showRefundDialog by remember { mutableStateOf(false) }
    var additionalPaymentAmount by remember { mutableStateOf("") }

    val newTotal = editedItems.sumOf { it.subtotal }
    val priceDifference = newTotal - originalTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Order #$orderId") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(editedItems.size) { index ->
                    val item = editedItems[index]
                    var expanded by remember { mutableStateOf(false) }
                    var selectedProduct by remember { mutableStateOf(products.find { it.name == item.productName }) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Product",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Box {
                                        OutlinedButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(item.productName)
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            products.forEach { product ->
                                                DropdownMenuItem(
                                                    text = { Text(product.name) },
                                                    onClick = {
                                                        selectedProduct = product
                                                        editedItems = editedItems.toMutableList().apply {
                                                            set(index, item.copy(
                                                                productId = product.id,
                                                                productName = product.name,
                                                                itemPrice = product.price,
                                                                subtotal = product.price * item.quantity
                                                            ))
                                                        }
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Price: ₱${"%.2f".format(item.itemPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        if (item.quantity > 1) {
                                            editedItems = editedItems.toMutableList().apply {
                                                set(index, item.copy(
                                                    quantity = item.quantity - 1,
                                                    subtotal = item.itemPrice * (item.quantity - 1)
                                                ))
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "${item.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    IconButton(onClick = {
                                        editedItems = editedItems.toMutableList().apply {
                                            set(index, item.copy(
                                                quantity = item.quantity + 1,
                                                subtotal = item.itemPrice * (item.quantity + 1)
                                            ))
                                        }
                                    }) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Subtotal: ₱${"%.2f".format(item.subtotal)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentRed
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (priceDifference > 0) {
                    showAdditionalPaymentDialog = true
                } else if (priceDifference < 0) {
                    showRefundDialog = true
                } else {
                    onUpdate(orderId, editedItems, priceDifference, 0.0)
                    onDismiss()
                }
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAdditionalPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showAdditionalPaymentDialog = false },
            title = { Text("Additional Payment Required") },
            text = {
                Column {
                    Text("Order total increased by: ₱${"%.2f".format(priceDifference)}")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = additionalPaymentAmount,
                        onValueChange = { additionalPaymentAmount = it },
                        label = { Text("Additional Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = additionalPaymentAmount.toDoubleOrNull() ?: 0.0
                    onUpdate(orderId, editedItems, priceDifference, amount)
                    showAdditionalPaymentDialog = false
                    onDismiss()
                }) {
                    Text("Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdditionalPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRefundDialog) {
        AlertDialog(
            onDismissRequest = { showRefundDialog = false },
            title = { Text("Refund Required") },
            text = {
                Column {
                    Text("Order total decreased by: ₱${"%.2f".format(-priceDifference)}")
                    Text(
                        "Customer should receive a refund of: ₱${"%.2f".format(-priceDifference)}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdate(orderId, editedItems, priceDifference, 0.0)
                    showRefundDialog = false
                    onDismiss()
                }) {
                    Text("Confirm Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
