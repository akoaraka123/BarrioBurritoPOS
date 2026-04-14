package com.example.barrioburritopos.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.barrioburritopos.data.local.entity.OrderEntity
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
    report: DailyReport?,
    transactions: List<OrderEntity>,
    currency: String = "₱"
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Sales Report", fontWeight = FontWeight.Bold) },
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
            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total Sales",
                    value = report?.let { currency + "%.2f".format(it.totalSales) } ?: (currency + "0.00"),
                    color = accentRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Orders",
                    value = "${report?.totalOrders ?: 0}",
                    color = accentYellow,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Items Sold",
                    value = "${report?.totalItemsSold ?: 0}",
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Best sellers
            Text(
                text = "Best Sellers Today",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = darkText
            )
            Spacer(Modifier.height(8.dp))

            if (report?.bestSellers.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No sales data yet",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                report?.bestSellers?.forEach { seller ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(seller.productName, fontWeight = FontWeight.Bold)
                            Text("${seller.totalQty} sold", color = accentRed)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Today's transactions
            Text(
                text = "Today's Transactions",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = darkText
            )
            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions today", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(transactions) { order ->
                        TransactionRow(order, currency)
                    }
                }
            }
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
fun TransactionRow(order: OrderEntity, currency: String) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.dateTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    color = darkText
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
