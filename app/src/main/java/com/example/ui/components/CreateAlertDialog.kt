package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.market.Quote
import com.example.ui.theme.*

data class AlertConditionOption(
    val typeKey: String,
    val title: String,
    val defaultVal: Double,
    val unit: String
)

@Composable
fun CreateAlertDialog(
    initialSymbol: String,
    quotes: Map<String, Quote>,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, conditionType: String, targetValue: Double, note: String) -> Unit
) {
    val symbolList = quotes.keys.toList().ifEmpty { listOf("NVDA", "TSLA", "AAPL", "BTC/USD") }
    var selectedSymbol by remember { mutableStateOf(if (initialSymbol in symbolList) initialSymbol else symbolList.first()) }
    val quote = quotes[selectedSymbol]
    val currentPrice = quote?.price ?: 100.0

    val conditionOptions = listOf(
        AlertConditionOption("PRICE_ABOVE", "Price Above", currentPrice * 1.03, "$"),
        AlertConditionOption("PRICE_BELOW", "Price Below", currentPrice * 0.97, "$"),
        AlertConditionOption("PCT_GAIN", "Day Gain %", 3.0, "%"),
        AlertConditionOption("PCT_DROP", "Day Drop %", 3.0, "%"),
        AlertConditionOption("RSI_OVERBOUGHT", "RSI Overbought", 70.0, "RSI"),
        AlertConditionOption("RSI_OVERSOLD", "RSI Oversold", 30.0, "RSI"),
        AlertConditionOption("MACD_CROSSOVER", "MACD Golden Cross", 0.0, ""),
        AlertConditionOption("VOL_SPIKE", "Volume Surge", (quote?.volume ?: 1000000L) * 1.5, "Vol")
    )

    var selectedOption by remember { mutableStateOf(conditionOptions.first()) }
    var targetValueText by remember { mutableStateOf("%.2f".format(selectedOption.defaultVal)) }
    var noteText by remember { mutableStateOf("") }

    LaunchedEffect(selectedOption, selectedSymbol) {
        val q = quotes[selectedSymbol]
        val p = q?.price ?: 100.0
        val def = when (selectedOption.typeKey) {
            "PRICE_ABOVE" -> p * 1.03
            "PRICE_BELOW" -> p * 0.97
            "PCT_GAIN" -> 3.5
            "PCT_DROP" -> 3.5
            "RSI_OVERBOUGHT" -> 70.0
            "RSI_OVERSOLD" -> 30.0
            "MACD_CROSSOVER" -> 0.0
            "VOL_SPIKE" -> (q?.volume ?: 1000000L) * 1.5
            else -> 0.0
        }
        targetValueText = if (selectedOption.typeKey.contains("RSI") || selectedOption.typeKey.contains("PCT")) {
            "%.1f".format(def)
        } else {
            "%.2f".format(def)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("create_alert_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Dialog Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = M3PrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alert Icon",
                                tint = M3OnPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "New Market Alert",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Get real-time notifications when technical parameters trigger.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )

                // Symbol Selector
                Text("Select Asset / Symbol", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(symbolList) { sym ->
                        val isSel = sym == selectedSymbol
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) M3PrimaryContainer else SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) M3Primary else BorderDark),
                            modifier = Modifier.clickable { selectedSymbol = sym }
                        ) {
                            Text(
                                text = sym,
                                color = if (isSel) M3OnPrimaryContainer else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Condition Types
                Text("Trigger Condition", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chunked = conditionOptions.chunked(2)
                    chunked.forEach { rowOptions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowOptions.forEach { opt ->
                                val isSelected = selectedOption.typeKey == opt.typeKey
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) M3SecondaryContainer else SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) M3Primary else BorderDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedOption = opt }
                                ) {
                                    Text(
                                        text = opt.title,
                                        color = if (isSelected) M3OnSecondaryContainer else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Value Input
                if (selectedOption.typeKey != "MACD_CROSSOVER") {
                    OutlinedTextField(
                        value = targetValueText,
                        onValueChange = { targetValueText = it },
                        label = { Text("Target Threshold (${selectedOption.unit})", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = M3Primary,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alert_target_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Memo / Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Strategy Note / Memo (Optional)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = BorderDark,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val targetVal = targetValueText.toDoubleOrNull() ?: 0.0
                            val finalNote = noteText.ifBlank { "${selectedOption.title} for $selectedSymbol" }
                            onConfirm(selectedSymbol, selectedOption.typeKey, targetVal, finalNote)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = M3Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_create_alert_button")
                    ) {
                        Text("Set Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
