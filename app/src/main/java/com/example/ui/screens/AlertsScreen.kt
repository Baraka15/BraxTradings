package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.PriceAlert
import com.example.ui.TradingViewModel
import com.example.ui.components.CreatePriceAlertDialog
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AlertsScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val quotes by viewModel.quotes.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val currentQuote = quotes.find { it.symbol == selectedSymbol } ?: quotes.first()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = BlueAccent,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Active & Triggered Alerts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }

            if (alerts.isEmpty()) {
                item {
                    Text("No alerts created yet. Tap + to set one!", color = TextSecondary, fontSize = 12.sp)
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alert.triggered) SurfaceDark else CardBg
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (alert.triggered) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (alert.triggered) GoldWarning else BlueAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(alert.symbol, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    val cond = if (alert.isAbove) "≥" else "≤"
                                    Text(
                                        text = "Target $cond ${String.format(Locale.US, "$%.2f", alert.targetPrice)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = if (alert.triggered) GoldWarning else TextSecondary
                                    )
                                    if (alert.note.isNotEmpty()) {
                                        Text(alert.note, fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                            IconButton(onClick = { viewModel.deleteAlert(alert.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedBearish, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreatePriceAlertDialog(
            symbol = selectedSymbol,
            currentPrice = currentQuote.currentPrice,
            onDismiss = { showDialog = false },
            onConfirm = { sym, price, isAbove, note ->
                viewModel.createAlert(sym, price, isAbove, note)
                showDialog = false
            }
        )
    }
}
