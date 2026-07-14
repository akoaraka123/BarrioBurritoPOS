package com.example.barrioburritopos.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.barrioburritopos.feature.pos.ReceiptData
import com.example.barrioburritopos.feature.pos.ReceiptLineItem
import com.example.barrioburritopos.printing.BluetoothPrinterDevice
import com.example.barrioburritopos.printing.BluetoothReceiptPrinter
import com.example.barrioburritopos.printing.PrintResult
import com.example.barrioburritopos.ui.responsive.rememberResponsiveInfo
import kotlinx.coroutines.launch

val backgroundColor = Color(0xFFFFF8F0)
val cardColor = Color(0xFFFFE8CC)
val accentRed = Color(0xFFC94F2D)
val darkText = Color(0xFF3D3D3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    businessName: String,
    currency: String,
    selectedPrinterName: String?,
    selectedPrinterAddress: String?,
    onBusinessNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSelectedPrinterChange: (String, String) -> Unit,
    hasPin: Boolean = false,
    onSetPin: (String) -> Unit = {},
    onClearPin: () -> Unit = {}
) {
    var showSetupPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearPinDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothPrinterDevice>>(emptyList()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val printer = remember(context) { BluetoothReceiptPrinter(context.applicationContext) }
    val responsiveInfo = rememberResponsiveInfo()
    val compact = responsiveInfo.isPhone
    fun printResultMessage(result: PrintResult, successMessage: String): String = when (result) {
        PrintResult.Success -> successMessage
        PrintResult.PrinterNotConnected -> "Printer not connected"
        is PrintResult.Error -> result.message
    }
    val testReceipt = remember {
        ReceiptData(
            orderId = 1,
            dateTime = System.currentTimeMillis(),
            cashierName = "Soykier",
            paymentMethod = "TEST",
            amountReceived = 0.0,
            changeAmount = 0.0,
            totalAmount = 0.0,
            items = listOf(
                ReceiptLineItem(
                    name = "Printer Test",
                    quantity = 1,
                    unitPrice = 0.0,
                    subtotal = 0.0,
                    details = "PT-210 connection test"
                )
            )
        )
    }
    val loadPrinters = {
        pairedDevices = printer.getPairedDevices()
        showPrinterDialog = true
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val connectGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            grants[Manifest.permission.BLUETOOTH_CONNECT] == true
        if (connectGranted) {
            loadPrinters()
        } else {
            scope.launch { snackbarHostState.showSnackbar("Bluetooth permission denied") }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(if (compact) 10.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
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
                        label = { Text("Business Name", color = darkText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText,
                            focusedLabelColor = darkText,
                            unfocusedLabelColor = Color(0xFF666666)
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = currency,
                        onValueChange = onCurrencyChange,
                        label = { Text("Currency Symbol", color = darkText) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = darkText,
                            unfocusedTextColor = darkText,
                            focusedLabelColor = darkText,
                            unfocusedLabelColor = Color(0xFF666666)
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Printer Settings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Receipt Printer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = darkText
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = selectedPrinterName?.let { "$it\n$selectedPrinterAddress" } ?: "No printer selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = darkText
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    bluetoothPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        )
                                    )
                                } else {
                                    loadPrinters()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                        ) {
                            Text("Select / Connect Printer", color = Color.White)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val message = printResultMessage(
                                            printer.testConnection(selectedPrinterAddress),
                                            "Printer connected"
                                        )
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                enabled = !selectedPrinterAddress.isNullOrBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reconnect")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val message = printResultMessage(
                                            printer.print(testReceipt, currency, selectedPrinterAddress),
                                            "Test receipt printed"
                                        )
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                enabled = !selectedPrinterAddress.isNullOrBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Print")
                            }
                        }
                    }
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
                            if (compact) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "PIN is set",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Green
                                    )
                                    Button(
                                        onClick = { showChangePinDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentRed)
                                    ) {
                                        Text("Change PIN", color = Color.White)
                                    }
                                    Button(
                                        onClick = { showClearPinDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Remove PIN", color = Color.White)
                                    }
                                }
                            } else {
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

                Spacer(Modifier.height(16.dp))

                // Developer Credit
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Info",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = darkText
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Developed by: Marco A. Batiller",
                            style = MaterialTheme.typography.bodyMedium,
                            color = darkText
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Version: 2.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
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

    if (showPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Select Printer", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text(
                        "No paired Bluetooth devices found. Pair PT-210_BB73 in Android Bluetooth settings first.",
                        color = Color.Black
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pairedDevices) { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectedPrinterChange(device.name, device.address)
                                        showPrinterDialog = false
                                        scope.launch {
                                            val message = printResultMessage(
                                                printer.testConnection(device.address),
                                                "Printer connected"
                                            )
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.address == selectedPrinterAddress) Color(0xFFFFF1DD) else Color.White
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(device.name, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterDialog = false }) {
                    Text("Close", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }
}
