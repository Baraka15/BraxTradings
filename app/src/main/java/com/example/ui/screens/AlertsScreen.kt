package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlertEntity
import com.example.ui.TradingViewModel
import com.example.ui.components.CreateAlertDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: TradingViewModel,
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val quotesMap by viewModel.marketEngine.quotes.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    val activeAlerts = alerts.filter { !it.isTriggered }
    val triggeredAlerts = alerts.filter { it.isTriggered }

    Scaffold(
        containerColor = CanvasDark,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = M3Primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = { Icon(Icons.Default.AddAlert, contentDescription = "Add Alert") },
                text = { Text("Create Alert", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("create_alert_fab")
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .testTag("alerts_screen"),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header Info Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = M3PrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = M3OnPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "Personalized Market Triggers",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Monitors price breakouts, volume surges, RSI extremes, and MACD crosses in real-time.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Active Alerts Section
            item {
                Text(
                    text = "ACTIVE ALERTS (${activeAlerts.size})",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            if (activeAlerts.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active alerts. Tap '+ Create Alert' below to set your first technical trigger.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(activeAlerts, key = { it.alertId }) { alert ->
                    val quote = quotesMap[alert.symbol]
                    val currentPrice = quote?.price ?: 0.0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(alert.symbol, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = M3SecondaryContainer
                                    ) {
                                        Text(
                                            formatConditionLabel(alert.conditionType, alert.targetValue),
                                            color = M3OnSecondaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    alert.note.ifBlank { "Monitoring live tick feed" },
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Current: $${"%.2f".format(currentPrice)}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { viewModel.deleteAlert(alert.alertId) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Alert", tint = BearRed)
                                }
                            }
                        }
                    }
                }
            }

            // Triggered History Section
            if (triggeredAlerts.isNotEmpty()) {
                item {
                    Text(
                        text = "TRIGGERED HISTORY (${triggeredAlerts.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }

                items(triggeredAlerts, key = { it.alertId }) { alert ->
                    val timeStr = alert.triggeredAt?.let {
                        SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()).format(Date(it))
                    } ?: "Recently"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BullGreen.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(alert.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BullGreenBg
                                    ) {
                                        Text(
                                            "TRIGGERED",
                                            color = BullGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Triggered @ $${"%.2f".format(alert.targetValue)} on $timeStr",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.selectSymbol(alert.symbol)
                                        onNavigateToTerminal()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = M3PrimaryContainer,
                                        contentColor = M3OnPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(onClick = { viewModel.deleteAlert(alert.alertId) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateAlertDialog(
                initialSymbol = selectedSymbol,
                quotes = quotesMap,
                onDismiss = { showCreateDialog = false },
                onConfirm = { sym, cond, targetVal, note ->
                    viewModel.createAlert(sym, cond, targetVal, note)
                    showCreateDialog = false
                }
            )
        }
    }
}

private fun formatConditionLabel(conditionType: String, targetVal: Double): String {
    return when (conditionType) {
        "PRICE_ABOVE" -> "Price ≥ $${"%.2f".format(targetVal)}"
        "PRICE_BELOW" -> "Price ≤ $${"%.2f".format(targetVal)}"
        "PCT_GAIN" -> "Gain ≥ +${"%.1f".format(targetVal)}%"
        "PCT_DROP" -> "Drop ≥ -${"%.1f".format(targetVal)}%"
        "RSI_OVERBOUGHT" -> "RSI ≥ ${"%.0f".format(targetVal)}"
        "RSI_OVERSOLD" -> "RSI ≤ ${"%.0f".format(targetVal)}"
        "MACD_CROSSOVER" -> "MACD Golden Cross"
        "VOL_SPIKE" -> "Vol Surge"
        else -> conditionType
    }
}
