package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.market.AssetType
import com.example.data.market.Quote
import com.example.data.market.TimeFrame
import com.example.domain.trading.OrderRequest
import com.example.domain.trading.OrderSide
import com.example.domain.trading.OrderType
import com.example.ui.TradingViewModel
import com.example.ui.components.InteractiveCandlestickChart
import com.example.ui.theme.*

enum class TickerSortOption(val label: String) {
    GAINERS("Top Gainers"),
    LOSERS("Top Losers"),
    VOLUME("Most Active"),
    ALPHABETICAL("A - Z")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TradingViewModel,
    onNavigateToTradingView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quotesMap by viewModel.marketEngine.quotes.collectAsStateWithLifecycle()
    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val currentCandles by viewModel.currentCandles.collectAsStateWithLifecycle()
    val indicatorToggles by viewModel.indicatorToggles.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val isStreaming by viewModel.marketEngine.isStreaming.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedAssetFilter by remember { mutableStateOf<AssetType?>(null) }
    var selectedSortOption by remember { mutableStateOf(TickerSortOption.GAINERS) }
    var showQuickTradeDialog by remember { mutableStateOf(false) }
    var quickTradeSide by remember { mutableStateOf(OrderSide.BUY) }

    val watchlistSymbols = remember(watchlist) { watchlist.map { it.symbol }.toSet() }

    // Filtered & Sorted Ticker List
    val displayedQuotes = remember(quotesMap, searchQuery, selectedAssetFilter, selectedSortOption) {
        var list = quotesMap.values.toList()

        if (selectedAssetFilter != null) {
            list = list.filter { it.assetType == selectedAssetFilter }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        when (selectedSortOption) {
            TickerSortOption.GAINERS -> list.sortedByDescending { it.changePercent }
            TickerSortOption.LOSERS -> list.sortedBy { it.changePercent }
            TickerSortOption.VOLUME -> list.sortedByDescending { it.volume }
            TickerSortOption.ALPHABETICAL -> list.sortedBy { it.symbol }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // 1. Dashboard Header & Market Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isStreaming) BullGreen else BearRed)
                            )
                            Text(
                                text = if (isStreaming) "LIVE MARKET DATA FEED" else "MARKET FEED PAUSED",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp
                            )
                        }

                        // Day Trader Speed Control / Streaming Toggle
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceContainerHigh)
                                .clickable { viewModel.toggleStreaming() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle stream",
                                tint = M3Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isStreaming) "Active" else "Resume",
                                color = M3Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Real-Time Quick Stats Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickMetric(label = "ACTIVE SYMBOLS", value = "${quotesMap.size}")
                        val gainersCount = quotesMap.values.count { it.isPositive }
                        QuickMetric(label = "BULL/BEAR", value = "$gainersCount / ${quotesMap.size - gainersCount}", valueColor = BullGreen)
                        val totalVol = quotesMap.values.sumOf { it.volume }
                        QuickMetric(label = "TOTAL VOLUME", value = "${totalVol / 1_000_000}M")
                    }
                }
            }
        }

        // 2. Selected Symbol Hero View & Chart Section
        item {
            if (currentQuote != null) {
                val quote = currentQuote!!
                val isBull = quote.isPositive
                val changeColor = if (isBull) BullGreen else BearRed
                val isFavorite = watchlistSymbols.contains(quote.symbol)

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, M3Primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_selected_hero_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Symbol Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = quote.symbol,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Surface(
                                            color = M3PrimaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = quote.assetType.label,
                                                color = M3OnPrimaryContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = quote.name,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Watchlist Star Button
                                IconButton(
                                    onClick = { viewModel.toggleWatchlist(quote.symbol, quote.name, quote.assetType) },
                                    modifier = Modifier.size(36.dp).testTag("dash_watchlist_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color(0xFFFFD700) else TextSecondary
                                    )
                                }

                                // Open TradingView CTA button
                                Button(
                                    onClick = onNavigateToTradingView,
                                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dash_open_tradingview_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("TradingView Analysis", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Large Price & 24h Change Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "$${"%.2f".format(quote.price)}",
                                    color = TextPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${if (isBull) "+" else ""}${"%.2f".format(quote.change)} (${if (isBull) "+" else ""}${"%.2f".format(quote.changePercent)}%)",
                                        color = changeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("24h", color = TextSecondary, fontSize = 12.sp)
                                }
                            }

                            // Quick Trade Actions (Buy / Sell)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        quickTradeSide = OrderSide.BUY
                                        showQuickTradeDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BullGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dash_buy_btn")
                                ) {
                                    Text("BUY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        quickTradeSide = OrderSide.SELL
                                        showQuickTradeDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dash_sell_btn")
                                ) {
                                    Text("SELL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Timeframe Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TimeFrame.values().forEach { tf ->
                                val isSelected = tf == selectedTimeFrame
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) M3PrimaryContainer else SurfaceCard,
                                    modifier = Modifier
                                        .clickable { viewModel.selectTimeFrame(tf) }
                                        .testTag("dash_tf_${tf.label}")
                                ) {
                                    Text(
                                        text = tf.label,
                                        color = if (isSelected) M3OnPrimaryContainer else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Chart Viewport for Selected Symbol
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CanvasDark)
                        ) {
                            InteractiveCandlestickChart(
                                candles = currentCandles,
                                indicatorToggles = indicatorToggles,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Key Indicators Summary Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            KeyStat(label = "24h High", value = "$${"%.2f".format(quote.high)}")
                            KeyStat(label = "24h Low", value = "$${"%.2f".format(quote.low)}")
                            KeyStat(label = "VWAP", value = "$${"%.2f".format(quote.vwap)}")
                            KeyStat(label = "RSI (14)", value = "%.1f".format(quote.rsi14), isWarning = quote.rsi14 >= 70 || quote.rsi14 <= 30)
                        }
                    }
                }
            }
        }

        // 3. Live Price Ticker List Header & Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PRICE TICKERS",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${displayedQuotes.size} markets active",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tickers (e.g., NVDA, BTC, AAPL)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dash_ticker_search_input")
                )

                // Asset Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedAssetFilter == null,
                        onClick = { selectedAssetFilter = null },
                        label = { Text("All Markets") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = M3PrimaryContainer,
                            selectedLabelColor = M3OnPrimaryContainer
                        )
                    )
                    AssetType.values().forEach { asset ->
                        FilterChip(
                            selected = selectedAssetFilter == asset,
                            onClick = { selectedAssetFilter = if (selectedAssetFilter == asset) null else asset },
                            label = { Text(asset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = M3PrimaryContainer,
                                selectedLabelColor = M3OnPrimaryContainer
                            )
                        )
                    }
                }

                // Sort Option Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TickerSortOption.values().forEach { sortOpt ->
                        val isSel = selectedSortOption == sortOpt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) SurfaceContainerHigh else SurfaceCard,
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, M3Primary) else null,
                            modifier = Modifier.clickable { selectedSortOption = sortOpt }
                        ) {
                            Text(
                                text = sortOpt.label,
                                color = if (isSel) M3Primary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Live Ticker List Items
        items(displayedQuotes, key = { it.symbol }) { quote ->
            val isSelected = quote.symbol == selectedSymbol
            val isFavorite = watchlistSymbols.contains(quote.symbol)

            DashboardTickerItem(
                quote = quote,
                isSelected = isSelected,
                isFavorite = isFavorite,
                onSelect = { viewModel.selectSymbol(quote.symbol) },
                onToggleWatchlist = { viewModel.toggleWatchlist(quote.symbol, quote.name, quote.assetType) },
                onOpenTradingView = {
                    viewModel.selectSymbol(quote.symbol)
                    onNavigateToTradingView()
                }
            )
        }
    }

    // Quick Trade Placement Dialog
    if (showQuickTradeDialog && currentQuote != null) {
        var sharesText by remember { mutableStateOf("10") }
        val price = currentQuote!!.price

        AlertDialog(
            onDismissRequest = { showQuickTradeDialog = false },
            containerColor = SurfaceElevated,
            title = {
                Text(
                    "Order: ${quickTradeSide.name} $selectedSymbol",
                    color = if (quickTradeSide == OrderSide.BUY) BullGreen else BearRed,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Market Price: $${"%.2f".format(price)}",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = sharesText,
                        onValueChange = { sharesText = it.filter { c -> c.isDigit() } },
                        label = { Text("Shares / Quantity") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (quickTradeSide == OrderSide.BUY) BullGreen else BearRed,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("dash_trade_shares_input")
                    )
                    val qty = sharesText.toDoubleOrNull() ?: 0.0
                    val totalNotional = qty * price
                    Text(
                        "Estimated Order Value: $${"%,.2f".format(totalNotional)}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = sharesText.toDoubleOrNull() ?: 10.0
                        if (qty > 0) {
                            viewModel.placeOrder(
                                OrderRequest(
                                    symbol = selectedSymbol,
                                    side = quickTradeSide,
                                    type = OrderType.MARKET,
                                    quantity = qty
                                )
                            )
                        }
                        showQuickTradeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (quickTradeSide == OrderSide.BUY) BullGreen else BearRed
                    ),
                    modifier = Modifier.testTag("dash_trade_confirm_btn")
                ) {
                    Text("Submit ${quickTradeSide.name}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTradeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun DashboardTickerItem(
    quote: Quote,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onOpenTradingView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBull = quote.isPositive
    val changeColor = if (isBull) BullGreen else BearRed

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) M3PrimaryContainer.copy(alpha = 0.4f) else SurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, M3Primary) else androidx.compose.foundation.BorderStroke(0.5.dp, BorderDark),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("ticker_item_${quote.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symbol & Name & Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1.2f)
            ) {
                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFFD700) else TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = quote.symbol,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = quote.assetType.label.take(1),
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = quote.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Sparkline Mini Chart
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .padding(horizontal = 6.dp)
            ) {
                if (quote.sparkline.size >= 2) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val points = quote.sparkline
                        val minVal = points.minOrNull() ?: 0.0
                        val maxVal = points.maxOrNull() ?: 1.0
                        val range = maxOf(0.001, maxVal - minVal)

                        val path = Path()
                        val stepX = size.width / (points.size - 1)

                        points.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height * (1f - ((v - minVal) / range).toFloat())
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = changeColor,
                            style = Stroke(width = 1.8f)
                        )
                    }
                }
            }

            // Price & Change Pill
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$${"%.2f".format(quote.price)}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = if (isBull) BullGreenBg else BearRedBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${if (isBull) "+" else ""}${"%.2f".format(quote.changePercent)}%",
                        color = changeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Arrow to TradingView
            IconButton(
                onClick = onOpenTradingView,
                modifier = Modifier.size(32.dp).testTag("open_tv_item_${quote.symbol}")
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open chart",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickMetric(label: String, value: String, valueColor: Color = TextPrimary) {
    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun KeyStat(label: String, value: String, isWarning: Boolean = false) {
    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(
            text = value,
            color = if (isWarning) AlertOrange else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
