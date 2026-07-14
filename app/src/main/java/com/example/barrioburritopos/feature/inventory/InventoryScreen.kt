package com.example.barrioburritopos.feature.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.barrioburritopos.data.local.entity.CustomizeOptionEntity
import com.example.barrioburritopos.data.local.entity.ProductEntity
import com.example.barrioburritopos.ui.responsive.rememberResponsiveInfo

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
    riceOptions: List<CustomizeOptionEntity> = emptyList(),
    mainOptions: List<CustomizeOptionEntity> = emptyList(),
    baseOptions: List<CustomizeOptionEntity> = emptyList(),
    toppingOptions: List<CustomizeOptionEntity> = emptyList(),
    sauceOptions: List<CustomizeOptionEntity> = emptyList(),
    addOnOptions: List<CustomizeOptionEntity> = emptyList(),
    currency: String = "₱",
    customBurritoBasePrice: Double = 130.0,
    onAddProduct: (String, Double, Int, String?) -> Unit,
    onToggleAvailability: (ProductEntity) -> Unit,
    onRestock: (Long, Int) -> Unit,
    onDelete: (ProductEntity) -> Unit,
    onUpdateProduct: (ProductEntity) -> Unit = {},
    onAddCustomizeOption: (String, String, Double, String?) -> Unit = { _, _, _, _ -> },
    onUpdateCustomizeOption: (String, String, String, Double, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteCustomizeOption: (String, String) -> Unit = { _, _ -> },
    onUpdateBasePrice: (Double) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    val responsiveInfo = rememberResponsiveInfo()
    val compact = responsiveInfo.isPhone

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
                actions = {
                    IconButton(
                        onClick = { showCustomizeDialog = true }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize", tint = accentRed)
                    }
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
                .padding(if (compact) 10.dp else 16.dp)
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
                            onEdit = { showEditDialog = product },
                            compact = compact
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

    if (showCustomizeDialog) {
        CustomizeDialog(
            riceOptions = riceOptions,
            mainOptions = mainOptions,
            baseOptions = baseOptions,
            toppingOptions = toppingOptions,
            sauceOptions = sauceOptions,
            addOnOptions = addOnOptions,
            currency = currency,
            customBurritoBasePrice = customBurritoBasePrice,
            onDismiss = { showCustomizeDialog = false },
            onAddOption = onAddCustomizeOption,
            onUpdateOption = onUpdateCustomizeOption,
            onDeleteOption = onDeleteCustomizeOption,
            onUpdateBasePrice = onUpdateBasePrice
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
    onEdit: () -> Unit,
    compact: Boolean = false
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
        Column(
            modifier = Modifier.padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 0.dp)
        ) {
            if (compact) {
                ProductInventoryInfo(
                    product = product,
                    currency = currency,
                    isLowStock = isLowStock,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = product.isAvailable,
                        onCheckedChange = { onToggle() }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductInventoryInfo(
                        product = product,
                        currency = currency,
                        isLowStock = isLowStock
                    )

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
private fun ProductInventoryInfo(
    product: ProductEntity,
    currency: String,
    isLowStock: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = product.name,
            fontWeight = FontWeight.Bold,
            color = if (product.isAvailable) darkText else Color.Gray,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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

@Composable
fun StepCard(
    stepNumber: Int,
    stepName: String,
    options: List<String>,
    currency: String,
    onEditOption: (String) -> Unit = {},
    onDeleteOption: (String) -> Unit = {},
    onAddOption: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Step Badge
            Surface(
                color = accentRed,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP $stepNumber",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = stepName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = darkText
            )
            
            Spacer(Modifier.height(12.dp))
            
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(accentRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = option,
                            color = darkText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onEditOption(option) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = accentRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteOption(option) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddOption,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Option", color = Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
            }
        }
    }
}

@Composable
fun AddOnItem(
    name: String,
    price: String,
    currency: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accentRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                color = darkText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = price,
                color = accentRed,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = accentRed,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CustomizeDialog(
    riceOptions: List<CustomizeOptionEntity>,
    mainOptions: List<CustomizeOptionEntity>,
    baseOptions: List<CustomizeOptionEntity>,
    toppingOptions: List<CustomizeOptionEntity>,
    sauceOptions: List<CustomizeOptionEntity>,
    addOnOptions: List<CustomizeOptionEntity>,
    currency: String,
    customBurritoBasePrice: Double = 130.0,
    onDismiss: () -> Unit,
    onAddOption: (String, String, Double, String?) -> Unit = { _, _, _, _ -> },
    onUpdateOption: (String, String, String, Double, String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteOption: (String, String) -> Unit = { _, _ -> },
    onUpdateBasePrice: (Double) -> Unit = {}
) {
    var editingOption by remember { mutableStateOf<Pair<String, String>?>(null) } // (stepName, optionName)
    var showEditPriceDialog by remember { mutableStateOf(false) }
    val responsiveInfo = rememberResponsiveInfo()
    val compact = responsiveInfo.isPhone
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = { })
                .background(Color.White),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentRed)
                        .padding(if (compact) 12.dp else 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Customize Products",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                
                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (compact) 12.dp else 24.dp,
                            vertical = if (compact) 12.dp else 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
                ) {
                    item {
                        Text(
                            text = "Build Your Perfect Burrito",
                            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Manage customization options",
                            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                            color = accentRed,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Custom Burrito Base Price Card (Left side, smaller)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = accentYellow),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Base Price",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = darkText
                                    )
                                    Text(
                                        text = "$currency${"%.2f".format(customBurritoBasePrice)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentRed
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { showEditPriceDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit", color = Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                    }
                                }
                            }

                            // Info card (Right side)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Customize Options",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = darkText
                                    )
                                    Text(
                                        text = "Manage burrito customization steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Step 1 - Rice
                        StepCard(
                            stepNumber = 1,
                            stepName = "PICK YOUR RICE",
                            options = riceOptions.map { it.name },
                            currency = currency,
                            onEditOption = { option -> editingOption = Pair("PICK YOUR RICE", option) },
                            onDeleteOption = { option -> onDeleteOption("PICK YOUR RICE", option) },
                            onAddOption = { editingOption = Pair("PICK YOUR RICE", "") }
                        )
                    }
                    
                    item {
                        // Step 2 - Main
                        StepCard(
                            stepNumber = 2,
                            stepName = "PICK YOUR MAIN",
                            options = mainOptions.map { it.name },
                            currency = currency,
                            onEditOption = { option -> editingOption = Pair("PICK YOUR MAIN", option) },
                            onDeleteOption = { option -> onDeleteOption("PICK YOUR MAIN", option) },
                            onAddOption = { editingOption = Pair("PICK YOUR MAIN", "") }
                        )
                    }
                    
                    item {
                        // Step 3 - Base
                        StepCard(
                            stepNumber = 3,
                            stepName = "PICK YOUR BASE",
                            options = baseOptions.map { it.name },
                            currency = currency,
                            onEditOption = { option -> editingOption = Pair("PICK YOUR BASE", option) },
                            onDeleteOption = { option -> onDeleteOption("PICK YOUR BASE", option) },
                            onAddOption = { editingOption = Pair("PICK YOUR BASE", "") }
                        )
                    }
                    
                    item {
                        // Step 4 - Topping
                        StepCard(
                            stepNumber = 4,
                            stepName = "PICK YOUR TOPPING",
                            options = toppingOptions.map { it.name },
                            currency = currency,
                            onEditOption = { option -> editingOption = Pair("PICK YOUR TOPPING", option) },
                            onDeleteOption = { option -> onDeleteOption("PICK YOUR TOPPING", option) },
                            onAddOption = { editingOption = Pair("PICK YOUR TOPPING", "") }
                        )
                    }
                    
                    item {
                        // Step 5 - Sauce
                        StepCard(
                            stepNumber = 5,
                            stepName = "PICK YOUR SAUCE",
                            options = sauceOptions.map { it.name },
                            currency = currency,
                            onEditOption = { option -> editingOption = Pair("PICK YOUR SAUCE", option) },
                            onDeleteOption = { option -> onDeleteOption("PICK YOUR SAUCE", option) },
                            onAddOption = { editingOption = Pair("PICK YOUR SAUCE", "") }
                        )
                    }
                    
                    item {
                        // Add-ons
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = accentYellow),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Step Badge
                                Surface(
                                    color = accentRed,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ADD-ONS",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                Text(
                                    text = "Optional extras",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = darkText
                                )
                                
                                Spacer(Modifier.height(12.dp))
                                
                                addOnOptions.forEach { option ->
                                    AddOnItem(
                                        option.name,
                                        "$currency${"%.2f".format(option.price)}",
                                        currency,
                                        onEdit = { editingOption = Pair("Add-ons", option.name) },
                                        onDelete = { onDeleteOption("Add-ons", option.name) }
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { editingOption = Pair("Add-ons", "") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Option", color = Color.White, fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit/Add Option Dialog
        editingOption?.let { (stepName, optionName) ->
            var editedName by remember { mutableStateOf(optionName) }
            var editedPrice by remember { mutableStateOf("0.0") }
            var selectedDrawable by remember { mutableStateOf<String?>(null) }
            var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
            val isAdding = optionName.isEmpty()
            val isAddOn = stepName == "Add-ons"
            
            // Gallery image picker
            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    selectedGalleryUri = uri
                    selectedDrawable = null
                }
            }
            
            // Available drawable images
            val drawableImages = listOf(
                "beef_chorizo", "bistek", "cabbage", "chicken_tenders", "classic_beef",
                "corn", "cucumber", "garlic_rice", "mexican_rice", "onion", "tomato",
                "bbq", "cheese_sauce", "honey_mustard", "honey_sriracha", "shredded_cheese",
                "sour_cream", "sweet_chili", "tomato_salsa"
            )
            
            // Get existing price and image if editing
            LaunchedEffect(optionName) {
                if (!isAdding) {
                    val option = when (stepName) {
                        "PICK YOUR RICE" -> riceOptions.find { it.name == optionName }
                        "PICK YOUR MAIN" -> mainOptions.find { it.name == optionName }
                        "PICK YOUR BASE" -> baseOptions.find { it.name == optionName }
                        "PICK YOUR TOPPING" -> toppingOptions.find { it.name == optionName }
                        "PICK YOUR SAUCE" -> sauceOptions.find { it.name == optionName }
                        "Add-ons" -> addOnOptions.find { it.name == optionName }
                        else -> null
                    }
                    if (isAddOn) {
                        editedPrice = option?.price?.toString() ?: "0.0"
                    }
                    // Extract drawable name from URI if it's a drawable resource
                    if (option?.imageUri?.startsWith("drawable://") == true) {
                        selectedDrawable = option.imageUri.removePrefix("drawable://")
                    } else if (option?.imageUri != null) {
                        selectedGalleryUri = Uri.parse(option.imageUri)
                    }
                }
            }
            
            var expanded by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { editingOption = null },
                title = { Text(if (isAdding) "Add Option" else "Edit Option") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Step: $stepName")
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Option Name") },
                            singleLine = true
                        )
                        if (isAddOn) {
                            OutlinedTextField(
                                value = editedPrice,
                                onValueChange = { editedPrice = it },
                                label = { Text("Price") },
                                singleLine = true
                            )
                        }
                        
                        // Image selector
                        Text("Select Image:", style = MaterialTheme.typography.labelMedium)
                        var showImageDialog by remember { mutableStateOf(false) }
                        
                        // Display selected image info
                        val imageDisplayText = when {
                            selectedGalleryUri != null -> "Gallery image selected"
                            selectedDrawable != null -> selectedDrawable
                            else -> "No image selected"
                        }
                        
                        OutlinedButton(
                            onClick = { showImageDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(imageDisplayText ?: "No image selected")
                        }
                        
                        // Image preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFEFEF)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedGalleryUri != null) {
                                AsyncImage(
                                    model = selectedGalleryUri,
                                    contentDescription = "Selected image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (selectedDrawable != null) {
                                val context = LocalContext.current
                                val resourceId = context.resources.getIdentifier(
                                    selectedDrawable,
                                    "drawable",
                                    context.packageName
                                )
                                if (resourceId != 0) {
                                    Image(
                                        painter = painterResource(id = resourceId),
                                        contentDescription = "Selected image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text("Image not found", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text("No image selected", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        if (showImageDialog) {
                            AlertDialog(
                                onDismissRequest = { showImageDialog = false },
                                title = { Text("Select Image") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Gallery option
                                        Button(
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                                showImageDialog = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Pick from Gallery", color = Color.White)
                                        }
                                        
                                        HorizontalDivider()
                                        
                                        Text("Or select from presets:", style = MaterialTheme.typography.labelSmall)
                                        
                                        LazyColumn(
                                            modifier = Modifier.heightIn(max = 300.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            item {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().clickable {
                                                        selectedDrawable = null
                                                        selectedGalleryUri = null
                                                        showImageDialog = false
                                                    }
                                                ) {
                                                    Text("No image", modifier = Modifier.padding(12.dp))
                                                }
                                            }
                                            items(drawableImages) { imageName ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().clickable {
                                                        selectedDrawable = imageName
                                                        selectedGalleryUri = null
                                                        showImageDialog = false
                                                    }
                                                ) {
                                                    Text(imageName, modifier = Modifier.padding(12.dp))
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showImageDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                val price = if (isAddOn) editedPrice.toDoubleOrNull() ?: 0.0 else 0.0
                                val imageUri = when {
                                    selectedGalleryUri != null -> selectedGalleryUri.toString()
                                    selectedDrawable != null -> "drawable://$selectedDrawable"
                                    else -> null
                                }
                                if (isAdding) {
                                    onAddOption(stepName, editedName, price, imageUri)
                                } else {
                                    onUpdateOption(stepName, optionName, editedName, price, imageUri)
                                }
                                editingOption = null
                            }
                        },
                        enabled = editedName.isNotBlank()
                    ) {
                        Text(if (isAdding) "Add" else "Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingOption = null }) { Text("Cancel") }
                }
            )
        }
    }

    if (showEditPriceDialog) {
        var newPrice by remember { mutableStateOf(customBurritoBasePrice.toString()) }
        AlertDialog(
            onDismissRequest = { showEditPriceDialog = false },
            title = { Text("Edit Custom Burrito Base Price", color = Color.Black) },
            text = {
                Column {
                    Text("Current base price: $currency${"%.2f".format(customBurritoBasePrice)}", color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = it },
                        label = { Text("New Base Price", color = Color.Black) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newPrice.toDoubleOrNull() ?: customBurritoBasePrice
                        if (price > 0) {
                            onUpdateBasePrice(price)
                            showEditPriceDialog = false
                        }
                    },
                    enabled = newPrice.toDoubleOrNull() ?: 0.0 > 0
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPriceDialog = false }) { Text("Cancel", color = Color.Black) }
            },
            containerColor = Color.White
        )
    }
}
