package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
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
import com.example.data.local.AccountEntity
import com.example.data.local.PositionEntity
import com.example.data.market.*
import com.example.domain.trading.OrderRequest
import com.example.ui.IndicatorToggles
import com.example.ui.SubIndicator
import com.example.ui.TradingViewModel
import com.example.ui.components.InteractiveCandlestickChart
import com.example.ui.components.OrderBookView
import com.example.ui.components.OrderEntryForm
import com.example.ui.components.TradeTapeView
import com.example.ui.theme.*

@Composable
fun TerminalScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsState()
    val indicatorToggles by viewModel.indicatorToggles.collectAsState()
    val quote by viewModel.currentQuote.collectAsState()
    val candles by viewModel.currentCandles.collectAsState()
    val orderBook by viewModel.currentOrderBook.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val accountState by viewModel.accountState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val quotesMap by viewModel.marketEngine.quotes.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = L2 Depth, 1 = Trade Tape
    var selectedLadderPrice by remember { mutableStateOf<Double?>(null) }

    val allSymbols = quotesMap.keys.toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 12.dp)
            .testTag("terminal_screen")
    ) {
        // 1. Quick Symbol Carousel Switcher
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allSymbols) { sym ->
                    val q = quotesMap[sym]
                    val isSel = sym == selectedSymbol
                    val isPos = (q?.change ?: 0.0) >= 0

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) M3PrimaryContainer else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) M3Primary else BorderDark),
                        modifier = Modifier
                            .clickable { viewModel.selectSymbol(sym) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = sym,
                                color = if (isSel) M3OnPrimaryContainer else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (q != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPos) BullGreenBg else BearRedBg
                                ) {
                                    Text(
                                        text = "${if (isPos) "+" else ""}${"%.1f".format(q.changePercent)}%",
                                        color = if (isPos) BullGreen else BearRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Symbol Hero Card (Matching Material 3 design mockup)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedSymbol,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = quote?.assetType?.label ?: "Stock",
                                    color = M3Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(M3SecondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = quote?.name ?: "",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (quote != null) {
                            val isPos = quote!!.isPositive
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isPos) BullGreenBg else BearRedBg
                            ) {
                                Text(
                                    text = "${if (isPos) "+" else ""}${"%.2f".format(quote!!.changePercent)}%",
                                    color = if (isPos) BullGreen else BearRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Price Display
                    if (quote != null) {
                        val isPos = quote!!.isPositive
                        val priceColor = if (isPos) BullGreen else BearRed
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$%,.2f".format(quote!!.price),
                                color = TextPrimary,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${if (isPos) "▲" else "▼"} $${"%.2f".format(quote!!.change)}",
                                color = priceColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary stats row
                    if (quote != null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatItem("24H HIGH", "$%.2f".format(quote!!.high))
                                StatItem("24H LOW", "$%.2f".format(quote!!.low))
                                StatItem("VWAP", "$%.2f".format(quote!!.vwap))
                                StatItem("VOLUME", "${quote!!.volume / 1000}k")
                            }
                        }
                    }
                }
            }
        }

        // 3. Technical Stats Grid (Volatility & RSI cards matching M3 design)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Volatility Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "VOLATILITY",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "High",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { 0.75f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = BearRed,
                                trackColor = BorderDark
                            )
                        }
                    }
                }

                // RSI (14) Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "RSI (14)",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "68.4",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BullGreenBg
                            ) {
                                Text(
                                    text = "STABLE",
                                    color = BullGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. M3 Alert Banner Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = M3Primary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PRICE ALERT ACTIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${selectedSymbol} tracking target threshold",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "LIVE",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Timeframe & Technical Indicator Toolbar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe buttons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevated)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TimeFrame.values().forEach { tf ->
                        val isSel = selectedTimeFrame == tf
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) M3Primary else Color.Transparent)
                                .clickable { viewModel.selectTimeFrame(tf) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf.label,
                                color = if (isSel) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Indicator Toggle Pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IndicatorPill(
                        label = "EMA",
                        isActive = indicatorToggles.showEMA,
                        onClick = { viewModel.toggleEMA() }
                    )
                    IndicatorPill(
                        label = "BB",
                        isActive = indicatorToggles.showBollinger,
                        onClick = { viewModel.toggleBollinger() }
                    )
                    IndicatorPill(
                        label = "VWAP",
                        isActive = indicatorToggles.showVWAP,
                        onClick = { viewModel.toggleVWAP() }
                    )
                    IndicatorPill(
                        label = indicatorToggles.subIndicator.label,
                        isActive = indicatorToggles.subIndicator != SubIndicator.NONE,
                        onClick = { viewModel.cycleSubIndicator() }
                    )
                }
            }
        }

        // 6. Interactive Candlestick Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                InteractiveCandlestickChart(
                    candles = candles,
                    indicatorToggles = indicatorToggles
                )
            }
        }

        // 7. Active Position Bar (if user holds current symbol)
        if (currentPosition != null && currentPosition!!.shares > 0) {
            item {
                val pos = currentPosition!!
                val currentP = quote?.price ?: pos.averagePrice
                val marketVal = pos.shares * currentP
                val uPnL = marketVal - pos.totalCost
                val uPnLPct = if (pos.totalCost > 0) (uPnL / pos.totalCost) * 100.0 else 0.0
                val isGain = uPnL >= 0

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isGain) BullGreenBg else BearRedBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isGain) BullGreen else BearRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ACTIVE POSITION: ${"%.2f".format(pos.shares)} Shares",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Avg: $${"%.2f".format(pos.averagePrice)} | Value: $${"%,.2f".format(marketVal)}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isGain) "+" else ""}$${"%,.2f".format(uPnL)}",
                                color = if (isGain) BullGreen else BearRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${if (isGain) "+" else ""}${"%.2f".format(uPnLPct)}%)",
                                color = if (isGain) BullGreen else BearRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = { viewModel.marketClosePosition(pos.symbol) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BearRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Close", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 8. Split Tabs: Level 2 Market Depth vs Live Trade Tape
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TabHeader(
                            title = "Level 2 Order Book",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        TabHeader(
                            title = "Time & Sales Tape",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }

                    if (selectedTab == 0) {
                        OrderBookView(
                            orderBook = orderBook,
                            onPriceSelected = { selectedLadderPrice = it }
                        )
                    } else {
                        TradeTapeView(
                            tapeStream = viewModel.marketEngine.tradeTape,
                            symbolFilter = selectedSymbol
                        )
                    }
                }
            }
        }

        // 9. Day Trader Order Entry Form
        item {
            Text(
                text = "EXECUTE ORDER",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            OrderEntryForm(
                quote = quote,
                account = accountState,
                currentPosition = currentPosition,
                selectedFillPrice = selectedLadderPrice,
                onOrderSubmit = { request -> viewModel.placeOrder(request) }
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IndicatorPill(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) M3SecondaryContainer else SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) M3Primary else BorderDark),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isActive) M3OnSecondaryContainer else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TabHeader(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SurfaceCard else Color.Transparent,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) M3Primary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}
