package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.trading.AlertCondition
import com.example.domain.trading.Instrument
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePriceAlertDialog(
    instrument: Instrument,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, targetPrice: Double, condition: AlertCondition, note: String) -> Unit
) {
    var targetPriceText by remember { mutableStateOf(String.format("%.2f", instrument.currentPrice * 1.02)) }
    var selectedCondition by remember { mutableStateOf(AlertCondition.CROSSES_ABOVE) }
    var noteText by remember { mutableStateOf("") }

    val currentPrice = instrument.currentPrice

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold)
                        Text(
                            "Set Price Alert",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // Asset info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(instrument.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(instrument.name, color = TextSecondary, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Current Price", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "\$${String.format("%.2f", currentPrice)}",
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Condition Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Trigger Condition", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    selectedCondition = AlertCondition.CROSSES_ABOVE
                                    if ((targetPriceText.toDoubleOrNull() ?: 0.0) <= currentPrice) {
                                        targetPriceText = String.format("%.2f", currentPrice * 1.02)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCondition == AlertCondition.CROSSES_ABOVE) BullishGreen.copy(alpha = 0.2f) else DarkSurface,
                            border = if (selectedCondition == AlertCondition.CROSSES_ABOVE) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BullishGreen)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "📈 Above (≥)",
                                    color = if (selectedCondition == AlertCondition.CROSSES_ABOVE) BullishGreen else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    selectedCondition = AlertCondition.CROSSES_BELOW
                                    if ((targetPriceText.toDoubleOrNull() ?: 0.0) >= currentPrice) {
                                        targetPriceText = String.format("%.2f", currentPrice * 0.98)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedCondition == AlertCondition.CROSSES_BELOW) BearishRed.copy(alpha = 0.2f) else DarkSurface,
                            border = if (selectedCondition == AlertCondition.CROSSES_BELOW) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BearishRed)) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "📉 Below (≤)",
                                    color = if (selectedCondition == AlertCondition.CROSSES_BELOW) BearishRed else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Quick Percentage Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = if (selectedCondition == AlertCondition.CROSSES_ABOVE) {
                        listOf(0.01 to "+1%", 0.02 to "+2%", 0.05 to "+5%", 0.10 to "+10%")
                    } else {
                        listOf(-0.01 to "-1%", -0.02 to "-2%", -0.05 to "-5%", -0.10 to "-10%")
                    }

                    presets.forEach { (factor, label) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clickable {
                                    val calc = currentPrice * (1.0 + factor)
                                    targetPriceText = String.format("%.2f", calc)
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = DarkSurface
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Target Price Input
                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = { targetPriceText = it },
                    label = { Text("Target Price ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AccentCyan
                    )
                )

                // Optional note input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Strategy (Optional)") },
                    placeholder = { Text("e.g., Take profit on resistance level", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkBorder
                    )
                )

                // Confirm CTA
                Button(
                    onClick = {
                        val parsed = targetPriceText.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onConfirm(instrument.symbol, parsed, selectedCondition, noteText)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text("Create Local Notification Alert", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
