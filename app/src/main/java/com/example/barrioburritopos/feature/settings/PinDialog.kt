package com.example.barrioburritopos.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberButton(
    number: String,
    onClick: () -> Unit,
    isEmpty: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .then(
                if (!isEmpty) {
                    Modifier
                        .background(Color(0xFFF5F5F5), shape = CircleShape)
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun PinDialog(
    onDismiss: () -> Unit,
    onPinConfirm: (String) -> Unit,
    title: String = "Enter PIN",
    isSetupMode: Boolean = false,
    errorMessage: String = ""
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf(errorMessage) }

    // Update local error message when errorMessage prop changes
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            localErrorMessage = errorMessage
        }
    }

    // Auto-submit when 4 digits are entered
    LaunchedEffect(pin.length) {
        if (pin.length == 4 && !showConfirm) {
            if (isSetupMode) {
                showConfirm = true
            } else {
                onPinConfirm(pin)
            }
        }
    }

    // Auto-submit when confirmation PIN is entered
    LaunchedEffect(confirmPin.length) {
        if (confirmPin.length == 4 && showConfirm) {
            if (confirmPin == pin) {
                onPinConfirm(pin)
            } else {
                localErrorMessage = "PINs do not match"
                showConfirm = false
                confirmPin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (!showConfirm) {
                        if (pin.length != 4) {
                            localErrorMessage = "PIN must be 4 digits"
                        } else {
                            onPinConfirm(pin)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC94F2D))
            ) {
                Text("Submit", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        title = { 
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (!showConfirm) "Enter 4-digit PIN" else "Confirm PIN",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(4) { index ->
                        val currentPin = if (showConfirm) confirmPin else pin
                        val isFilled = index < currentPin.length
                        
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (isFilled) Color(0xFFC94F2D) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            Color.White,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }
                    }
                }

                if (localErrorMessage.isNotEmpty()) {
                    Text(
                        text = localErrorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Numeric Keypad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rows 1-3 (1-9)
                    (1..9).chunked(3).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { number ->
                                NumberButton(
                                    number = number.toString(),
                                    onClick = {
                                        val currentPin = if (showConfirm) confirmPin else pin
                                        if (currentPin.length < 4) {
                                            if (showConfirm) {
                                                confirmPin += number.toString()
                                            } else {
                                                pin += number.toString()
                                            }
                                            localErrorMessage = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    // Row 4 (0 and backspace)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NumberButton(
                            number = "",
                            onClick = {},
                            isEmpty = true
                        )
                        NumberButton(
                            number = "0",
                            onClick = {
                                val currentPin = if (showConfirm) confirmPin else pin
                                if (currentPin.length < 4) {
                                    if (showConfirm) {
                                        confirmPin += "0"
                                    } else {
                                        pin += "0"
                                    }
                                    localErrorMessage = ""
                                }
                            }
                        )
                        NumberButton(
                            number = "⌫",
                            onClick = {
                                if (showConfirm) {
                                    if (confirmPin.isNotEmpty()) {
                                        confirmPin = confirmPin.dropLast(1)
                                    }
                                } else {
                                    if (pin.isNotEmpty()) {
                                        pin = pin.dropLast(1)
                                    }
                                }
                                localErrorMessage = ""
                            }
                        )
                    }
                }
            }
        }
    )
}
