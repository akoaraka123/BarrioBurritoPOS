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
    onCurrencyChange: (String) -> Unit,
    hasPin: Boolean = false,
    onSetPin: (String) -> Unit = {},
    onClearPin: () -> Unit = {}
) {
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearPinDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.Black) },
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
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = currency,
                        onValueChange = onCurrencyChange,
                        label = { Text("Currency Symbol") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // PIN Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PIN Security",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = darkText
                        )
                        Spacer(Modifier.height(12.dp))

                        if (hasPin) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PIN is set",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Green
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showChangePinDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                                    ) {
                                        Text("Change PIN", color = Color.White)
                                    }
                                    Button(
                                        onClick = { showClearPinDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Remove PIN", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { showSetupPinDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                            ) {
                                Text("Setup PIN", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Setup PIN Dialog
    if (showSetupPinDialog) {
        PinDialog(
            onDismiss = { showSetupPinDialog = false },
            onPinConfirm = { pin ->
                onSetPin(pin)
                showSetupPinDialog = false
            },
            title = "Setup PIN",
            isSetupMode = true
        )
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        PinDialog(
            onDismiss = { showChangePinDialog = false },
            onPinConfirm = { pin ->
                onSetPin(pin)
                showChangePinDialog = false
            },
            title = "Change PIN",
            isSetupMode = true
        )
    }

    // Clear PIN Confirmation Dialog
    if (showClearPinDialog) {
        AlertDialog(
            onDismissRequest = { showClearPinDialog = false },
            title = { Text("Remove PIN", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Are you sure you want to remove the PIN security? Anyone will be able to access the app without a PIN.", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPin()
                        showClearPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPinDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
