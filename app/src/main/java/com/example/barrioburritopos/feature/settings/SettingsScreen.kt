package com.example.barrioburritopos.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    businessName: String,
    currency: String,
    onBusinessNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Business settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Business Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = darkText
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = onBusinessNameChange,
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = currency,
                        onValueChange = onCurrencyChange,
                        label = { Text("Currency Symbol") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}
