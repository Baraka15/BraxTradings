package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.AlertCondition
import com.example.domain.trading.Instrument
import com.example.ui.TradingViewModel
import com.example.ui.components.CreatePriceAlertDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: TradingViewModel,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val alerts by viewModel.alerts.collectAsState()
    val instruments by viewModel.instruments.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()

    var showCreateDialogForInstrument by remember { mutableStateOf<Instrument?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val activeInstrument = instruments.find { it.symbol == selectedSymbol } ?: instruments.firstOrNull()

    if (showCreateDialogForInstrument != null) {
        CreatePriceAlertDialog(
            instrument = showCreateDialogForInstrument!!,
            onDismiss = { showCreateDialogForInstrument = null },
            onConfirm = { symbol, targetPrice, condition, note ->
                viewModel.createPriceAlert(symbol, targetPrice, condition, note)
            }
        )
    }

    val filteredAlerts = when (selectedFilter) {
        "ACTIVE" -> alerts.filter { it.isEnabled && !it.isTriggered }
        "TRIGGERED" -> alerts.filter { it.isTriggered }
        else -> alerts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 84.dp, top = 8.dp)
    ) {
        // System Notification Permission Card (if not granted on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentOrange))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable System Notifications", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Allow BraxTradings to post status bar alerts when your price targets hit.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestNotificationPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Allow", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Fast Header with Action Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Price Alerts Engine", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Local system notifications on target hit", color = TextSecondary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (activeInstrument != null) {
                                    showCreateDialogForInstrument = activeInstrument
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Alert", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Test notification & testing buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val target = (activeInstrument?.currentPrice ?: 125.0) * 1.01
                                viewModel.sendTestNotification(activeInstrument?.symbol ?: "NVDA", target)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test System Alarm", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Jump current active instrument price to trigger alerts
                                val activeAlert = alerts.firstOrNull { it.symbol == selectedSymbol && it.isEnabled && !it.isTriggered }
                                if (activeAlert != null) {
                                    val triggerPrice = if (activeAlert.condition == AlertCondition.CROSSES_ABOVE) {
                                        activeAlert.targetPrice + 0.50
                                    } else {
                                        activeAlert.targetPrice - 0.50
                                    }
                                    viewModel.forceTriggerPrice(selectedSymbol, triggerPrice)
                                } else {
                                    val curPrice = activeInstrument?.currentPrice ?: 125.0
                                    viewModel.createPriceAlert(selectedSymbol, curPrice + 0.10, AlertCondition.CROSSES_ABOVE, "Simulated instant hit")
                                    viewModel.forceTriggerPrice(selectedSymbol, curPrice + 0.20)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BullishGreen),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Target Hit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Price Triggers (${alerts.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "ACTIVE", "TRIGGERED").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            modifier = Modifier
                                .clickable { selectedFilter = filter }
                                .height(26.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) DarkSurfaceElevated else Color.Transparent
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    filter,
                                    color = if (isSelected) AccentCyan else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alerts List
        if (filteredAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(DarkSurface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Text("No price alerts in this filter.", color = TextMuted, fontSize = 13.sp)
                        Text("Tap '+ New Alert' to create one for any stock or crypto.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(filteredAlerts) { alert ->
                val inst = instruments.find { it.symbol == alert.symbol }
                val currentLivePrice = inst?.currentPrice ?: alert.initialPriceAtCreation
                val isBullish = alert.condition == AlertCondition.CROSSES_ABOVE

                // Distance calculation
                val priceDiff = alert.targetPrice - currentLivePrice
                val pctDistance = (priceDiff / currentLivePrice) * 100.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.isTriggered) DarkSurfaceElevated else DarkSurface
                    ),
                    border = if (alert.isTriggered) {
                        CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(if (isBullish) BullishGreen else BearishRed))
                    } else null
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(alert.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isBullish) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        if (isBullish) "≥ ABOVE" else "≤ BELOW",
                                        color = if (isBullish) BullishGreen else BearishRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (alert.isTriggered) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "TRIGGERED",
                                            color = AccentGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!alert.isTriggered) {
                                    Switch(
                                        checked = alert.isEnabled,
                                        onCheckedChange = { viewModel.toggleAlertEnabled(alert.id) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = DarkBackground,
                                            checkedTrackColor = BullishGreen,
                                            uncheckedTrackColor = DarkBorder
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.resetAlert(alert.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reactivate Alert", tint = AccentCyan)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteAlert(alert.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Alert", tint = BearishRed)
                                }
                            }
                        }

                        // Target and Live Price Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Target Price", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "\$${String.format("%.2f", alert.targetPrice)}",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (alert.isTriggered) "Triggered At Price" else "Distance",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                if (alert.isTriggered) {
                                    Text(
                                        "\$${String.format("%.2f", alert.triggerPrice ?: alert.targetPrice)}",
                                        color = if (isBullish) BullishGreen else BearishRed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Text(
                                        "${if (pctDistance > 0) "+" else ""}${String.format("%.2f", pctDistance)}%",
                                        color = if (Math.abs(pctDistance) < 1.0) AccentGold else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Live Market Price", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    "\$${String.format("%.2f", currentLivePrice)}",
                                    color = AccentCyan,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (alert.note.isNotBlank()) {
                            Text("Note: ${alert.note}", color = TextSecondary, fontSize = 11.sp)
                        }

                        if (alert.triggeredAt != null) {
                            val timeStr = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(alert.triggeredAt))
                            Text("Triggered on $timeStr", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
