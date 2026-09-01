package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.domain.trading.*
import com.example.ui.TradingViewModel
import com.example.ui.components.CreatePriceAlertDialog
import com.example.ui.components.OrderBookAndTapeView
import com.example.ui.components.RealTimePriceTickerRibbon
import com.example.ui.components.TradingViewAdvancedSuite
import com.example.ui.components.TradingViewInteractiveChart
import com.example.ui.components.SparklineView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TradingViewModel,
    onNavigateToAlerts: () -> Unit
) {
    val instruments by viewModel.instruments.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val candleHistory by viewModel.candleHistory.collectAsState()
    val orderBook by viewModel.orderBook.collectAsState()
    val recentTrades by viewModel.recentTrades.collectAsState()
    val timeFrame by viewModel.selectedTimeFrame.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val priceDirections by viewModel.priceDirections.collectAsState()
    val streamInfo by viewModel.tickerStreamInfo.collectAsState()


    var selectedTab by remember { mutableStateOf("ALL") }
    var orderQuantity by remember { mutableStateOf("1.0") }
    var leverage by remember { mutableStateOf(1.0) }

    var showAlertDialogForInstrument by remember { mutableStateOf<Instrument?>(null) }

    val activeInstrument = instruments.find { it.symbol == selectedSymbol }
    val currentCandles = candleHistory[selectedSymbol]?.get(timeFrame) ?: emptyList()
    val activeSymbolAlerts = alerts.filter { it.symbol == selectedSymbol && it.isEnabled && !it.isTriggered }
    val alertTargets = activeSymbolAlerts.map { it.targetPrice }

    val filteredInstruments = when (selectedTab) {
        "STOCKS" -> instruments.filter { it.category == "STOCKS" }
        "CRYPTO" -> instruments.filter { it.category == "CRYPTO" }
        else -> instruments
    }

    if (showAlertDialogForInstrument != null) {
        CreatePriceAlertDialog(
            instrument = showAlertDialogForInstrument!!,
            onDismiss = { showAlertDialogForInstrument = null },
            onConfirm = { symbol, targetPrice, condition, note ->
                viewModel.createPriceAlert(symbol, targetPrice, condition, note)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 84.dp, top = 8.dp)
    ) {
        // Portfolio Balance & Live Stream Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Portfolio Margin Balance", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            "\$${String.format("%,.2f", balance)}",
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isStreaming) BullishGreen.copy(alpha = 0.15f) else DarkSurface,
                            modifier = Modifier.clickable { viewModel.toggleStreaming() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isStreaming) BullishGreen else TextMuted, RoundedCornerShape(4.dp))
                                )
                                Text(
                                    if (isStreaming) "LIVE FEED" else "PAUSED",
                                    color = if (isStreaming) BullishGreen else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-Time Dynamic Price Ticker Component (WebSocket / Polling)
        item {
            RealTimePriceTickerRibbon(
                instruments = instruments,
                selectedSymbol = selectedSymbol,
                priceDirections = priceDirections,
                streamInfo = streamInfo,
                onSelectSymbol = { sym -> viewModel.selectSymbol(sym) },
                onToggleStreaming = { viewModel.toggleStreaming() },
                onSetInterval = { interval -> viewModel.setStreamInterval(interval) },
                onSetTransportMode = { mode -> viewModel.setTransportMode(mode) },
                onManualRefreshTick = { viewModel.refreshTickNow() },
                onOpenCreateAlert = { inst -> showAlertDialogForInstrument = inst }
            )
        }

        // TradingView Interactive Chart Suite
        item {
            TradingViewInteractiveChart(
                symbol = activeInstrument?.symbol ?: "NVDA",
                instrumentName = activeInstrument?.name ?: "NVIDIA Corporation",
                currentPrice = activeInstrument?.currentPrice ?: 124.50,
                change24h = activeInstrument?.change24h ?: 0.0,
                candles = currentCandles,
                selectedTimeFrame = timeFrame,
                onSelectTimeFrame = { tf -> viewModel.selectTimeFrame(tf) },
                alertPriceTargets = alertTargets
            )
        }

        // TradingView Pro Advanced Suite (DOM Level 2 Ladder, Technical Screener, Whale Radar)
        item {
            activeInstrument?.let { inst ->
                TradingViewAdvancedSuite(
                    instruments = instruments,
                    selectedInstrument = inst,
                    orderBook = orderBook,
                    recentTrades = recentTrades,
                    onSelectSymbol = { sym -> viewModel.selectSymbol(sym) },
                    onPlaceLimitOrder = { side, price, qty ->
                        viewModel.placeOrder(inst.symbol, side, OrderType.LIMIT, qty, price = price, leverage = leverage)
                    }
                )
            }
        }

        // Active Alerts summary for selected symbol if any
        if (activeSymbolAlerts.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAlerts() },
                    shape = RoundedCornerShape(10.dp),
                    color = AccentGold.copy(alpha = 0.1f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentGold.copy(alpha = 0.4f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
                            Text(
                                "${activeSymbolAlerts.size} active price alert(s) on $selectedSymbol",
                                color = AccentGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("View >", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Instant Market Execution Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Instant Order Execution", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = orderQuantity,
                            onValueChange = { orderQuantity = it },
                            label = { Text("Shares / Units", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = DarkBorder
                            )
                        )

                        // Leverage Selectors
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1.0, 5.0, 10.0).forEach { lev ->
                                val isSelected = leverage == lev
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clickable { leverage = lev },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentGold else DarkSurface
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${lev.toInt()}x",
                                            color = if (isSelected) DarkBackground else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val qty = orderQuantity.toDoubleOrNull() ?: 1.0
                                viewModel.placeOrder(selectedSymbol, OrderSide.BUY, OrderType.MARKET, qty, leverage = leverage)
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BullishGreen)
                        ) {
                            Text("BUY / LONG", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val qty = orderQuantity.toDoubleOrNull() ?: 1.0
                                viewModel.placeOrder(selectedSymbol, OrderSide.SELL, OrderType.MARKET, qty, leverage = leverage)
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BearishRed)
                        ) {
                            Text("SELL / SHORT", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Order Book & Live Trades
        item {
            OrderBookAndTapeView(
                orderBook = orderBook,
                recentTrades = recentTrades
            )
        }

        // Watchlist Categories & Fast Alert Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Markets & Stock Watchlist", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ALL", "STOCKS", "CRYPTO")) { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            modifier = Modifier
                                .clickable { selectedTab = tab }
                                .height(26.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) DarkSurfaceElevated else Color.Transparent
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    tab,
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

        // Watchlist Items
        items(filteredInstruments) { inst ->
            val isSelected = inst.symbol == selectedSymbol
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectSymbol(inst.symbol) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(AccentCyan)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(inst.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(inst.name, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    }

                    // Sparkline
                    SparklineView(
                        data = inst.sparkline,
                        isPositive = inst.change24h >= 0,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(24.dp)
                            .padding(horizontal = 4.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "\$${String.format("%.2f", inst.currentPrice)}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        val isPositive = inst.change24h >= 0
                        Text(
                            "${if (isPositive) "+" else ""}${String.format("%.2f", inst.change24h)}%",
                            color = if (isPositive) BullishGreen else BearishRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Set Alert Icon on each item
                    IconButton(
                        onClick = { showAlertDialogForInstrument = inst },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AddAlert, contentDescription = "Add Price Alert", tint = AccentGold, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
