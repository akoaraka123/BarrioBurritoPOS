package com.example.barrioburritopos.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barrioburritopos.data.local.entity.ProductEntity

val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val accentYellow = Color(0xFFFFC857)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<ProductEntity>,
    lowStockProducts: List<ProductEntity>,
    currency: String = "₱",
    onAddProduct: (String, Double, Int, String?) -> Unit,
    onToggleAvailability: (ProductEntity) -> Unit,
    onRestock: (Long, Int) -> Unit,
    onDelete: (ProductEntity) -> Unit,
    onUpdateProduct: (ProductEntity) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
                actions = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = accentRed,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
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
            // Low stock warning
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠️ Low Stock Alert",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = lowStockProducts.joinToString { "${it.name} (${it.stockQuantity})" },
                            color = Color.DarkGray
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Products list
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No products", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductInventoryCard(
                            product = product,
                            currency = currency,
                            isLowStock = lowStockProducts.any { it.id == product.id },
                            onToggle = { onToggleAvailability(product) },
                            onRestock = { onRestock(product.id, it) },
                            onDelete = { onDelete(product) },
                            onEdit = { showEditDialog = product }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, price, stock, cat ->
                onAddProduct(name, price, stock, cat)
                showAddDialog = false
            }
        )
    }

    showEditDialog?.let { product ->
        EditProductDialog(
            product = product,
            onDismiss = { showEditDialog = null },
            onUpdate = { updatedProduct ->
                onUpdateProduct(updatedProduct)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun ProductInventoryCard(
    product: ProductEntity,
    currency: String,
    isLowStock: Boolean,
    onToggle: () -> Unit,
    onRestock: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showRestock by remember { mutableStateOf(false) }
    var restockAmount by remember { mutableStateOf("10") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isAvailable) cardColor else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isAvailable) darkText else Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$currency${product.price}",
                        color = accentRed
                    )
                    if (isLowStock) {
                        Text(
                            text = "Low stock: ${product.stockQuantity}",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Stock: ${product.stockQuantity}",
                            color = Color.Gray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = product.isAvailable,
                        onCheckedChange = { onToggle() }
                    )
                    if (product.isAvailable) {
                        TextButton(onClick = { showRestock = true }) {
                            Text("Restock", color = accentRed)
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accentRed)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }

    if (showRestock) {
        AlertDialog(
            onDismissRequest = { showRestock = false },
            title = { Text("Restock ${product.name}") },
            text = {
                OutlinedTextField(
                    value = restockAmount,
                    onValueChange = { restockAmount = it },
                    label = { Text("Amount to add") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRestock(restockAmount.toIntOrNull() ?: 0)
                    showRestock = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestock = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete ${product.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
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
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Initial Stock") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        name,
                        price.toDoubleOrNull() ?: 0.0,
                        stock.toIntOrNull() ?: 0,
                        category.takeIf { it.isNotBlank() }
                    )
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onUpdate: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var price by remember { mutableStateOf(product.price.toString()) }
    var stock by remember { mutableStateOf(product.stockQuantity.toString()) }
    var category by remember { mutableStateOf(product.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stock") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedProduct = product.copy(
                        name = name,
                        price = price.toDoubleOrNull() ?: product.price,
                        stockQuantity = stock.toIntOrNull() ?: product.stockQuantity,
                        category = category.takeIf { it.isNotBlank() }
                    )
                    onUpdate(updatedProduct)
                },
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
