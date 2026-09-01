package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.market.*
import com.example.domain.trading.OrderRequest
import com.example.domain.trading.OrderSide
import com.example.domain.trading.OrderType
import com.example.ui.IndicatorToggles
import com.example.ui.SubIndicator
import com.example.ui.TradingViewModel
import com.example.ui.components.TradingViewCanvas
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingViewScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val selectedTimeFrame by viewModel.selectedTimeFrame.collectAsStateWithLifecycle()
    val chartStyle by viewModel.chartStyle.collectAsStateWithLifecycle()
    val activeDrawingTool by viewModel.activeDrawingTool.collectAsStateWithLifecycle()
    val currentDrawings by viewModel.currentDrawings.collectAsStateWithLifecycle()
    val indicatorToggles by viewModel.indicatorToggles.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val currentCandles by viewModel.currentCandles.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val pendingOrders by viewModel.pendingOrders.collectAsStateWithLifecycle()
    val quotesMap by viewModel.marketEngine.quotes.collectAsStateWithLifecycle()

    var showSymbolSelector by remember { mutableStateOf(false) }
    var showChartStyleMenu by remember { mutableStateOf(false) }
    var showIndicatorsDialog by remember { mutableStateOf(false) }
    var showQuickTradeDialog by remember { mutableStateOf(false) }
    var showDrawingToolbar by remember { mutableStateOf(true) }
    var quickTradeSide by remember { mutableStateOf(OrderSide.BUY) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasDark,
        topBar = {
            // TradingView Pro Top Bar
            Surface(
                color = SurfaceElevated,
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Symbol Pill & Asset Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceContainerHigh)
                                .clickable { showSymbolSelector = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("tv_symbol_picker_btn")
                        ) {
                            Text(
                                text = selectedSymbol,
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change symbol",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (currentQuote != null) {
                                Spacer(Modifier.width(4.dp))
                                val isBull = currentQuote!!.isPositive
                                Text(
                                    text = "$${"%.2f".format(currentQuote!!.price)}",
                                    color = if (isBull) BullGreen else BearRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Timeframes Scrollable Group
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimeFrame.values().forEach { tf ->
                                val isSelected = tf == selectedTimeFrame
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) M3PrimaryContainer else Color.Transparent)
                                        .clickable { viewModel.selectTimeFrame(tf) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("tv_tf_${tf.label}")
                                ) {
                                    Text(
                                        text = tf.label,
                                        color = if (isSelected) M3OnPrimaryContainer else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Chart Style, Indicators, and Tools Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chart Style Selector
                            IconButton(
                                onClick = { showChartStyleMenu = true },
                                modifier = Modifier.size(32.dp).testTag("tv_chart_style_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "Chart Style",
                                    tint = M3Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Indicators Button
                            IconButton(
                                onClick = { showIndicatorsDialog = true },
                                modifier = Modifier.size(32.dp).testTag("tv_indicators_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = "Indicators",
                                    tint = M3Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Toggle Drawing Toolbar
                            IconButton(
                                onClick = { showDrawingToolbar = !showDrawingToolbar },
                                modifier = Modifier.size(32.dp).testTag("tv_drawings_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Drawing Tools",
                                    tint = if (showDrawingToolbar) M3Primary else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick Buy / Sell Pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BullGreen.copy(alpha = 0.15f))
                                    .clickable {
                                        quickTradeSide = OrderSide.BUY
                                        showQuickTradeDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("tv_quick_buy_btn")
                            ) {
                                Text("TRADE", color = BullGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Drawing Tools Rail (TradingView style)
            AnimatedVisibility(
                visible = showDrawingToolbar,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(46.dp),
                    color = SurfaceElevated,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DrawingToolType.values().forEach { tool ->
                            val isSelected = activeDrawingTool == tool
                            IconButton(
                                onClick = { viewModel.setActiveDrawingTool(tool) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) M3PrimaryContainer else Color.Transparent)
                                    .testTag("drawing_tool_${tool.name}")
                            ) {
                                val iconVec = when (tool) {
                                    DrawingToolType.CURSOR -> Icons.Default.ControlCamera
                                    DrawingToolType.TRENDLINE -> Icons.Default.TrendingUp
                                    DrawingToolType.HORIZONTAL_RAY -> Icons.Default.HorizontalRule
                                    DrawingToolType.FIBONACCI -> Icons.Default.Calculate
                                    DrawingToolType.PRICE_RULER -> Icons.Default.Straighten
                                    DrawingToolType.LONG_POSITION -> Icons.Default.ArrowUpward
                                    DrawingToolType.SHORT_POSITION -> Icons.Default.ArrowDownward
                                    DrawingToolType.RECTANGLE_ZONE -> Icons.Default.CropSquare
                                }
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = tool.label,
                                    tint = if (isSelected) M3OnPrimaryContainer else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = BorderDark
                        )

                        // Clear All Drawings Button
                        IconButton(
                            onClick = { viewModel.clearDrawingsForCurrentSymbol() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("tv_clear_drawings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear drawings",
                                tint = AlertOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Central TradingView Interactive Chart
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Main Chart Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    TradingViewCanvas(
                        candles = currentCandles,
                        chartStyle = chartStyle,
                        activeTool = activeDrawingTool,
                        drawings = currentDrawings,
                        indicatorToggles = indicatorToggles,
                        currentPosition = currentPosition,
                        pendingOrders = pendingOrders,
                        onAddDrawing = { viewModel.addDrawing(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Active Tool Hint Pill at top-center
                    if (activeDrawingTool != DrawingToolType.CURSOR) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            color = SurfaceElevated.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, M3Primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Tool: ${activeDrawingTool.label} (Tap 2 points on chart)",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Tool",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { viewModel.setActiveDrawingTool(DrawingToolType.CURSOR) }
                                )
                            }
                        }
                    }
                }

                // TradingView Bottom Info Bar
                Surface(
                    color = SurfaceElevated,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentQuote != null) {
                            val q = currentQuote!!
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("24H HIGH", color = TextSecondary, fontSize = 9.sp)
                                    Text("$${"%.2f".format(q.high)}", color = BullGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("24H LOW", color = TextSecondary, fontSize = 9.sp)
                                    Text("$${"%.2f".format(q.low)}", color = BearRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("VWAP", color = TextSecondary, fontSize = 9.sp)
                                    Text("$${"%.2f".format(q.vwap)}", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Column {
                                    Text("RSI (14)", color = TextSecondary, fontSize = 9.sp)
                                    val rsiColor = when {
                                        q.rsi14 >= 70 -> AlertOrange
                                        q.rsi14 <= 30 -> BullGreen
                                        else -> TextPrimary
                                    }
                                    Text("${"%.1f".format(q.rsi14)}", color = rsiColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Order placement shortcuts
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    quickTradeSide = OrderSide.BUY
                                    showQuickTradeDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BullGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp).testTag("tv_btn_buy")
                            ) {
                                Text("BUY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    quickTradeSide = OrderSide.SELL
                                    showQuickTradeDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BearRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp).testTag("tv_btn_sell")
                            ) {
                                Text("SELL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Chart Style Selector Dialog / Menu
    if (showChartStyleMenu) {
        AlertDialog(
            onDismissRequest = { showChartStyleMenu = false },
            containerColor = SurfaceElevated,
            title = { Text("Select Chart Style", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartStyle.values().forEach { style ->
                        val isSelected = style == chartStyle
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) M3PrimaryContainer else SurfaceCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setChartStyle(style)
                                    showChartStyleMenu = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = style.label,
                                    color = if (isSelected) M3OnPrimaryContainer else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = M3OnPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChartStyleMenu = false }) {
                    Text("Close", color = M3Primary)
                }
            }
        )
    }

    // Indicators Configuration Dialog
    if (showIndicatorsDialog) {
        AlertDialog(
            onDismissRequest = { showIndicatorsDialog = false },
            containerColor = SurfaceElevated,
            title = { Text("TradingView Technical Indicators", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IndicatorToggleRow(
                        title = "EMA 9 (Fast Trend)",
                        subtitle = "Blue exponential moving average",
                        checked = indicatorToggles.showEMA,
                        onCheckedChange = { viewModel.toggleEMA() }
                    )
                    IndicatorToggleRow(
                        title = "EMA 21 (Medium Trend)",
                        subtitle = "Orange exponential moving average",
                        checked = indicatorToggles.showEMA21,
                        onCheckedChange = { viewModel.toggleEMA21() }
                    )
                    IndicatorToggleRow(
                        title = "SMA 50 (Major Baseline)",
                        subtitle = "Purple simple moving average",
                        checked = indicatorToggles.showSMA50,
                        onCheckedChange = { viewModel.toggleSMA50() }
                    )
                    IndicatorToggleRow(
                        title = "Bollinger Bands (20, 2)",
                        subtitle = "Volatility envelope & channel shade",
                        checked = indicatorToggles.showBollinger,
                        onCheckedChange = { viewModel.toggleBollinger() }
                    )
                    IndicatorToggleRow(
                        title = "VWAP (Session Volume Average)",
                        subtitle = "Yellow benchmark line for institutional liquidity",
                        checked = indicatorToggles.showVWAP,
                        onCheckedChange = { viewModel.toggleVWAP() }
                    )
                    IndicatorToggleRow(
                        title = "Open Orders & Position Lines",
                        subtitle = "Display buy/sell limits directly on canvas",
                        checked = indicatorToggles.showOrderLines,
                        onCheckedChange = { viewModel.toggleOrderLines() }
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 4.dp))

                    Text("Oscillator / Sub-Indicator Pane", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubIndicator.values().forEach { sub ->
                            val isSel = indicatorToggles.subIndicator == sub
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) M3PrimaryContainer else SurfaceCard,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setSubIndicator(sub) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sub.label.split(" ")[0],
                                        color = if (isSel) M3OnPrimaryContainer else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showIndicatorsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = M3Primary)
                ) {
                    Text("Apply & Done", color = Color.White)
                }
            }
        )
    }

    // Symbol Selector Dialog
    if (showSymbolSelector) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredQuotes = remember(searchQuery, quotesMap) {
            quotesMap.values.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showSymbolSelector = false },
            containerColor = SurfaceElevated,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Search Symbol", color = TextPrimary, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter NVDA, BTC, SPY...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = M3Primary,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tv_symbol_search_input")
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filteredQuotes.forEach { q ->
                        val isSelected = q.symbol == selectedSymbol
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) M3PrimaryContainer else SurfaceCard,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSymbol(q.symbol)
                                    showSymbolSelector = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = q.symbol,
                                        color = if (isSelected) M3OnPrimaryContainer else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = q.name,
                                        color = if (isSelected) M3OnPrimaryContainer.copy(alpha = 0.8f) else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${"%.2f".format(q.price)}",
                                        color = if (isSelected) M3OnPrimaryContainer else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${if (q.changePercent >= 0) "+" else ""}${"%.2f".format(q.changePercent)}%",
                                        color = if (q.isPositive) BullGreen else BearRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSymbolSelector = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
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
                    "Quick Market ${quickTradeSide.name}: $selectedSymbol",
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
                        label = { Text("Shares / Contracts") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (quickTradeSide == OrderSide.BUY) BullGreen else BearRed,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("tv_shares_input")
                    )
                    val qty = sharesText.toDoubleOrNull() ?: 0.0
                    val totalNotional = qty * price
                    Text(
                        "Estimated Notional: $${"%,.2f".format(totalNotional)}",
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
                    modifier = Modifier.testTag("tv_confirm_trade_btn")
                ) {
                    Text("Submit ${quickTradeSide.name} Order", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun IndicatorToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = M3Primary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceElevated
                )
            )
        }
    }
}
