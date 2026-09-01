package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.OrderSide
import com.example.domain.trading.PositionRiskAssessment
import com.example.ui.theme.*

/**
 * Position Risk Heatmap & Liquidation Distance Card
 */
@Composable
fun PositionRiskTableCard(
    positions: List<PositionRiskAssessment>,
    onReduceExposure: (positionId: String, reductionPct: Double) -> Unit,
    onClosePosition: (positionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Position Risk & Liquidation Monitor", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Real-time liquidation buffer & margin exposure", color = TextSecondary, fontSize = 11.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurface
                ) {
                    Text(
                        "${positions.size} Active",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (positions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active positions requiring risk management.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    positions.forEach { item ->
                        val pos = item.position
                        val isBuy = pos.side == OrderSide.BUY

                        val (distColor, distLabel) = when {
                            item.distToLiquidationPct <= 5.0 -> BearishRed to "CRITICAL"
                            item.distToLiquidationPct <= 12.0 -> AccentOrange to "WARNING"
                            else -> BullishGreen to "SAFE"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, if (item.distToLiquidationPct <= 5.0) BearishRed.copy(alpha = 0.5f) else DarkBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Top row: Symbol, Side, Leverage, Liq Distance badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(pos.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isBuy) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                if (isBuy) "LONG ${pos.leverage.toInt()}x" else "SHORT ${pos.leverage.toInt()}x",
                                                color = if (isBuy) BullishGreen else BearishRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = distColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "Liq Dist: ${String.format("%.1f", item.distToLiquidationPct)}% ($distLabel)",
                                            color = distColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Key metrics grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Notional Value", color = TextMuted, fontSize = 10.sp)
                                        Text("\$${String.format("%,.2f", item.notionalExposure)}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Est. Liq Price", color = TextMuted, fontSize = 10.sp)
                                        Text("\$${String.format("%.2f", item.liquidationPrice)}", color = distColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PnL (ROE)", color = TextMuted, fontSize = 10.sp)
                                        Text(
                                            "${if (pos.pnl >= 0) "+" else ""}\$${String.format("%.2f", pos.pnl)} (${String.format("%.1f", pos.pnlPercentage)}%)",
                                            color = if (pos.pnl >= 0) BullishGreen else BearishRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Action Buttons: De-Risk 50% & Close
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onReduceExposure(pos.id, 0.5) },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                                    ) {
                                        Icon(Icons.Default.Shield, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Cut 50% Size", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onClosePosition(pos.id) },
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BearishRed.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Close Position", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive What-If Market Drop Stress Tester
 */
@Composable
fun MarketStressTestCard(
    grossExposure: Double,
    usedMargin: Double,
    totalEquity: Double,
    modifier: Modifier = Modifier
) {
    var simulatedDropPct by remember { mutableStateOf(5.0f) }

    val simulatedLoss = grossExposure * (simulatedDropPct / 100.0)
    val projectedEquity = (totalEquity - simulatedLoss).coerceAtLeast(0.0)
    val projectedMarginUtil = if (projectedEquity > 0) (usedMargin / projectedEquity * 100.0).coerceIn(0.0, 150.0) else 150.0
    val isLiquidationRisk = projectedMarginUtil >= 90.0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Market Crash Stress Tester (What-If)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Simulate adverse volatility before execution", color = TextSecondary, fontSize = 11.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        "-${simulatedDropPct.toInt()}% Shock",
                        color = AccentGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Slider
            Slider(
                value = simulatedDropPct,
                onValueChange = { simulatedDropPct = it },
                valueRange = 1f..25f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = AccentGold,
                    activeTrackColor = AccentGold,
                    inactiveTrackColor = DarkSurface
                )
            )

            // Results of simulation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Projected Drawdown", color = TextMuted, fontSize = 10.sp)
                    Text("-\$${String.format("%,.2f", simulatedLoss)}", color = BearishRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Projected Equity", color = TextMuted, fontSize = 10.sp)
                    Text("\$${String.format("%,.2f", projectedEquity)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Projected Margin %", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "${String.format("%.1f", projectedMarginUtil)}%",
                        color = if (isLiquidationRisk) BearishRed else if (projectedMarginUtil > 75) AccentOrange else BullishGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isLiquidationRisk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BearishRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed, modifier = Modifier.size(16.dp))
                    Text(
                        "WARNING: A ${simulatedDropPct.toInt()}% move triggers a Margin Call! Consider cutting leverage.",
                        color = BearishRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Edit Day Trader Risk Guardrails Dialog
 */
@Composable
fun EditRiskGuardrailsDialog(
    currentDailyLossLimit: Double,
    onSave: (newLimit: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var limitInput by remember { mutableStateOf(currentDailyLossLimit.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentCyan)
                Text("Day Trader Risk Guardrails", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Set a strict daily maximum drawdown cap. Once this threshold is reached, trading alerts and circuit breakers will trigger to protect your capital.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Daily Loss Limit ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Quick presets
                Text("Quick Presets", color = TextMuted, fontSize = 11.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500.0, 1000.0, 2500.0, 5000.0).forEach { preset ->
                        AssistChip(
                            onClick = { limitInput = preset.toInt().toString() },
                            label = { Text("\$${preset.toInt()}", fontSize = 11.sp, color = TextPrimary) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = limitInput.toDoubleOrNull() ?: currentDailyLossLimit
                    onSave(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Save Guardrails", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

/**
 * Confirmation dialog for 1-Tap Emergency De-Risk
 */
@Composable
fun EmergencyDeRiskConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = BearishRed)
                Text("Emergency De-Risk (-50%)", color = BearishRed, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "This action will immediately reduce all open positions by 50% to rapidly recover free margin and neutralize liquidation risk.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
            ) {
                Text("Confirm 50% De-Risk", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
