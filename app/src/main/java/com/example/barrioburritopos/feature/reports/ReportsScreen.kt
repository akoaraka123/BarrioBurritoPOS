package com.example.barrioburritopos.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barrioburritopos.data.local.entity.OrderEntity
import com.example.barrioburritopos.data.local.entity.OrderItemEntity
import com.example.barrioburritopos.domain.model.DailyReport
import java.text.SimpleDateFormat
import java.util.*

val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val accentYellow = Color(0xFFFFC857)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    dailyReports: List<DailyReportWithDate>,
    selectedTransactions: List<OrderEntity>,
    currency: String = "₱",
    onViewTransactions: (Long) -> Unit = {},
    onGetOrderItems: (Long) -> List<OrderItemEntity> = { emptyList() }
) {
    var showTransactionsDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var viewingOrderId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Sales Report", fontWeight = FontWeight.Bold, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
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
            if (dailyReports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sales data yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dailyReports) { dailyReport ->
                        DailyReportCard(
                            dailyReport = dailyReport,
                            currency = currency,
                            onViewTransactions = {
                                selectedDate = dailyReport.date
                                showTransactionsDialog = true
                                onViewTransactions(dailyReport.dateMillis)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showTransactionsDialog) {
        TransactionsDialog(
            date = selectedDate,
            transactions = selectedTransactions,
            currency = currency,
            onDismiss = { showTransactionsDialog = false },
            onViewOrder = { viewingOrderId = it },
            onGetOrderItems = onGetOrderItems
        )
    }

    viewingOrderId?.let { orderId ->
        val order = selectedTransactions.find { it.id == orderId }
        order?.let {
            val items = onGetOrderItems(orderId)
            ReportOrderDetailsDialog(
                order = order,
                items = items,
                currency = currency,
                onDismiss = { viewingOrderId = null }
            )
        }
    }
}

@Composable
fun DailyReportCard(
    dailyReport: DailyReportWithDate,
    currency: String,
    onViewTransactions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dailyReport.date,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = darkText
                )
                Button(
                    onClick = onViewTransactions,
                    colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                ) {
                    Text("View Transactions")
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniSummaryCard(
                    title = "Total Sales",
                    value = currency + "%.2f".format(dailyReport.report.totalSales),
                    color = accentRed,
                    modifier = Modifier.weight(1f)
                )
                MiniSummaryCard(
                    title = "Orders",
                    value = "${dailyReport.report.totalOrders}",
                    color = accentYellow,
                    modifier = Modifier.weight(1f)
                )
                MiniSummaryCard(
                    title = "Items Sold",
                    value = "${dailyReport.report.totalItemsSold}",
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniSummaryCard(
                    title = "Cash Sales",
                    value = currency + "%.2f".format(dailyReport.report.cashSales),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                MiniSummaryCard(
                    title = "GCash Sales",
                    value = currency + "%.2f".format(dailyReport.report.gcashSales),
                    color = Color(0xFF0077B6),
                    modifier = Modifier.weight(1f)
                )
            }

            if (dailyReport.report.bestSellers.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Best Sellers",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = darkText
                )
                Spacer(Modifier.height(4.dp))
                dailyReport.report.bestSellers.take(3).forEach { seller ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(seller.productName, style = MaterialTheme.typography.bodySmall, color = Color.Red)
                        Text("${seller.totalQty} sold", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsDialog(
    date: String,
    transactions: List<OrderEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onViewOrder: (Long) -> Unit = {},
    onGetOrderItems: (Long) -> List<OrderItemEntity> = { emptyList() }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transactions - $date", color = Color.Black) },
        text = {
            if (transactions.isEmpty()) {
                Text("No transactions", color = Color.Black)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactions) { order ->
                        TransactionRow(order, currency, onViewOrder)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Black)
            }
        },
        containerColor = Color.White
    )
}

@Composable
fun MiniSummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
        }
    }
}

@Composable
fun TransactionRow(order: OrderEntity, currency: String, onViewOrder: (Long) -> Unit = {}) {
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.dateTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewOrder(order.id) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Order #${order.id}",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "$time • ${order.totalItems} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "$currency${"%.2f".format(order.totalAmount)}",
                fontWeight = FontWeight.Bold,
                color = accentRed,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ReportOrderDetailsDialog(
    order: OrderEntity,
    items: List<OrderItemEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(order.dateTime))
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.dateTime))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Color.Black) }
        },
        title = {
            Column {
                Text("Order #${order.id}", fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    text = "$date at $time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Items
                items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "$currency${"%.2f".format(item.subtotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentRed
                                )
                            }
                            if (!item.itemDetails.isNullOrBlank()) {
                                Text(
                                    text = item.itemDetails,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF555555),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Payment details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Payment Method:", color = Color.Black)
                    Text(order.paymentMethod, color = Color.Black)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Amount Received:", color = Color.Black)
                    Text("$currency${"%.2f".format(order.amountReceived ?: order.totalAmount)}", color = Color.Black)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Change:", color = Color.Black)
                    Text("$currency${"%.2f".format(order.changeAmount)}", color = Color.Black)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total (${order.totalItems} items):",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "$currency${"%.2f".format(order.totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = accentRed,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        containerColor = Color.White
    )
}
