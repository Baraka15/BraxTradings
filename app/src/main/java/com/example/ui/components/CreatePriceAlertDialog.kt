package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CreatePriceAlertDialog(
    symbol: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, targetPrice: Double, isAbove: Boolean, note: String) -> Unit
) {
    var targetText by remember { mutableStateOf(String.format("%.2f", currentPrice * 1.05)) }
    var isAbove by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text("Set Price Alert: $symbol", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Price: $${String.format("%.2f", currentPrice)}", color = TextSecondary, fontSize = 12.sp)

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isAbove = true },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isAbove) GreenBullish else SurfaceDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rise Above", color = if (isAbove) DarkBg else TextPrimary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { isAbove = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (!isAbove) RedBearish else SurfaceDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Drop Below", color = if (!isAbove) DarkBg else TextPrimary, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Strategy (Optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = targetText.toDoubleOrNull() ?: currentPrice
                    onConfirm(symbol, p, isAbove, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) {
                Text("Create Alert", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
