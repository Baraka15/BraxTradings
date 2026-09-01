package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Professional TradingView-grade interactive chart with multi-timeframe switching,
 * multi-indicator overlay (EMA 9/21/50, Bollinger Bands, VWAP, Supertrend),
 * sub-chart oscillators (Volume 20MA, RSI 14, MACD), dynamic drawing tools
 * (Trendlines, Horizontal rays, Fibonacci Retracements, Risk/Reward box, Ruler),
 * magnetic cursor crosshair, and real-time Technical Analysis Confluence Gauges.
 */
@Composable
fun TradingViewInteractiveChart(
    symbol: String,
    instrumentName: String,
    currentPrice: Double,
    change24h: Double,
    candles: List<CandleStick>,
    selectedTimeFrame: TimeFrame,
    onSelectTimeFrame: (TimeFrame) -> Unit,
    alertPriceTargets: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Chart Display State
    var chartStyle by remember { mutableStateOf(ChartStyle.CANDLESTICK) }
    var indicatorSettings by remember { mutableStateOf(IndicatorSettings()) }
    var activeDrawingTool by remember { mutableStateOf(DrawingTool.CURSOR) }
    var drawings by remember { mutableStateOf<List<ChartDrawing>>(emptyList()) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Dialog / Modal states
    var showIndicatorsMenu by remember { mutableStateOf(false) }
    var showTechnicalGaugeDialog by remember { mutableStateOf(false) }
    var showDrawingToolbar by remember { mutableStateOf(true) }

    // Crosshair State
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }
    var activeHoverIndex by remember { mutableStateOf<Int?>(null) }

    // Temporary drawing drag in progress
    var dragStartPoint by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPoint by remember { mutableStateOf<Offset?>(null) }

    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(DarkSurfaceElevated, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(32.dp))
                Text("Streaming Real-Time Candlestick Data...", color = TextSecondary, fontSize = 12.sp)
            }
        }
        return
    }

    // Prepare candles based on selected chart style
    val displayCandles = remember(candles, chartStyle) {
        if (chartStyle == ChartStyle.HEIKIN_ASHI) {
            TechnicalAnalysisEngine.convertToHeikinAshi(candles)
        } else {
            candles
        }
    }

    // Precalculate Technical Indicators
    val ema9List = remember(candles) { TechnicalAnalysisEngine.calculateEMA(candles, 9) }
    val ema21List = remember(candles) { TechnicalAnalysisEngine.calculateEMA(candles, 21) }
    val ema50List = remember(candles) { TechnicalAnalysisEngine.calculateEMA(candles, 50) }
    val bollingerBands = remember(candles) { TechnicalAnalysisEngine.calculateBollingerBands(candles, 20, 2.0) }
    val vwapList = remember(candles) { TechnicalAnalysisEngine.calculateVWAP(candles) }
    val supertrendList = remember(candles) { TechnicalAnalysisEngine.calculateSupertrend(candles, 10, 3.0) }
    val rsiList = remember(candles) { TechnicalAnalysisEngine.calculateRSI(candles, 14) }
    val macdList = remember(candles) { TechnicalAnalysisEngine.calculateMACD(candles) }
    val technicalGauge = remember(candles) { TechnicalAnalysisEngine.calculateTechnicalGauge(candles) }

    // Calculate Price Bounds
    val pricesToInclude = displayCandles.flatMap { listOf(it.high, it.low) } + alertPriceTargets
    val minPrice = (pricesToInclude.minOrNull() ?: currentPrice * 0.98) * 0.999
    val maxPrice = (pricesToInclude.maxOrNull() ?: currentPrice * 1.02) * 1.001
    val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0

    val heightDp = if (isFullscreen) 560.dp else 460.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            // --- 1. TradingView Main Header Bar ---
            TradingViewTopHeader(
                symbol = symbol,
                instrumentName = instrumentName,
                currentPrice = currentPrice,
                change24h = change24h,
                selectedTimeFrame = selectedTimeFrame,
                onSelectTimeFrame = onSelectTimeFrame,
                chartStyle = chartStyle,
                onSelectChartStyle = { chartStyle = it },
                onOpenIndicators = { showIndicatorsMenu = true },
                onOpenTechnicalGauge = { showTechnicalGaugeDialog = true },
                showDrawingToolbar = showDrawingToolbar,
                onToggleDrawingToolbar = { showDrawingToolbar = !showDrawingToolbar },
                isFullscreen = isFullscreen,
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                technicalRating = technicalGauge.overallRating
            )

            // --- 2. TradingView Drawing Toolbar (Expandable Horizontal Ribbon) ---
            AnimatedVisibility(
                visible = showDrawingToolbar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                TradingViewDrawingToolbar(
                    activeTool = activeDrawingTool,
                    onSelectTool = { activeDrawingTool = it },
                    drawingsCount = drawings.size,
                    onClearDrawings = {
                        drawings = emptyList()
                        activeDrawingTool = DrawingTool.CURSOR
                    }
                )
            }

            // --- 3. TradingView Real-Time Data Window / Legend Bar ---
            val hoveredCandle = activeHoverIndex?.let { idx -> displayCandles.getOrNull(idx) } ?: displayCandles.lastOrNull()
            val hoveredIdx = activeHoverIndex ?: (displayCandles.size - 1)

            TradingViewDataLegend(
                candle = hoveredCandle,
                ema9 = if (indicatorSettings.showEMA9) ema9List.getOrNull(hoveredIdx) else null,
                ema21 = if (indicatorSettings.showEMA21) ema21List.getOrNull(hoveredIdx) else null,
                ema50 = if (indicatorSettings.showEMA50) ema50List.getOrNull(hoveredIdx) else null,
                vwap = if (indicatorSettings.showVWAP) vwapList.getOrNull(hoveredIdx) else null,
                supertrend = if (indicatorSettings.showSupertrend) supertrendList.getOrNull(hoveredIdx) else null,
                subChartType = indicatorSettings.subChart,
                rsi = rsiList.getOrNull(hoveredIdx),
                macd = macdList.getOrNull(hoveredIdx)
            )

            // --- 4. TradingView Multi-Pane Chart Canvas ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayCandles.size, activeDrawingTool) {
                            detectTapGestures(
                                onTap = { tapOffset ->
                                    val count = displayCandles.size
                                    if (count > 0) {
                                        val spacing = size.width / count
                                        val tappedIdx = ((tapOffset.x / spacing).toInt()).coerceIn(0, count - 1)
                                        activeHoverIndex = tappedIdx
                                        crosshairOffset = tapOffset

                                        // If tool is Horizontal Ray, add a drawing instantly at tap level
                                        if (activeDrawingTool == DrawingTool.HORIZONTAL_RAY) {
                                            val mainHeight = size.height * 0.75f
                                            val tappedPrice = minPrice + ((mainHeight - tapOffset.y) / mainHeight) * priceRange
                                            val newDrawing = ChartDrawing(
                                                id = UUID.randomUUID().toString(),
                                                type = DrawingTool.HORIZONTAL_RAY,
                                                startIndex = 0,
                                                startPrice = tappedPrice,
                                                endIndex = count - 1,
                                                endPrice = tappedPrice,
                                                label = "Key Level $${String.format("%.2f", tappedPrice)}",
                                                colorHex = 0xFFFFD600
                                            )
                                            drawings = drawings + newDrawing
                                            activeDrawingTool = DrawingTool.CURSOR
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(displayCandles.size, activeDrawingTool) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    crosshairOffset = startOffset
                                    dragStartPoint = startOffset
                                    dragCurrentPoint = startOffset
                                    val count = displayCandles.size
                                    if (count > 0) {
                                        val spacing = size.width / count
                                        activeHoverIndex = ((startOffset.x / spacing).toInt()).coerceIn(0, count - 1)
                                    }
                                },
                                onDrag = { change, _ ->
                                    crosshairOffset = change.position
                                    dragCurrentPoint = change.position
                                    val count = displayCandles.size
                                    if (count > 0) {
                                        val spacing = size.width / count
                                        activeHoverIndex = ((change.position.x / spacing).toInt()).coerceIn(0, count - 1)
                                    }
                                },
                                onDragEnd = {
                                    val start = dragStartPoint
                                    val end = dragCurrentPoint
                                    val count = displayCandles.size
                                    if (start != null && end != null && count > 0 && activeDrawingTool != DrawingTool.CURSOR) {
                                        val spacing = size.width / count
                                        val mainHeight = size.height * 0.75f
                                        val startIdx = ((start.x / spacing).toInt()).coerceIn(0, count - 1)
                                        val endIdx = ((end.x / spacing).toInt()).coerceIn(0, count - 1)
                                        val sPrice = minPrice + ((mainHeight - start.y) / mainHeight) * priceRange
                                        val ePrice = minPrice + ((mainHeight - end.y) / mainHeight) * priceRange

                                        if (abs(start.x - end.x) > 15f || abs(start.y - end.y) > 15f) {
                                            val newDrawing = ChartDrawing(
                                                id = UUID.randomUUID().toString(),
                                                type = activeDrawingTool,
                                                startIndex = min(startIdx, endIdx),
                                                startPrice = sPrice,
                                                endIndex = max(startIdx, endIdx),
                                                endPrice = ePrice,
                                                label = when (activeDrawingTool) {
                                                    DrawingTool.TREND_LINE -> "Trendline"
                                                    DrawingTool.FIBONACCI -> "Fibonacci"
                                                    DrawingTool.RISK_REWARD -> "1:2 R:R Position"
                                                    DrawingTool.RULER -> "Δ ${(ePrice - sPrice) / sPrice * 100}%"
                                                    else -> ""
                                                },
                                                colorHex = when (activeDrawingTool) {
                                                    DrawingTool.TREND_LINE -> 0xFF00E5FF
                                                    DrawingTool.FIBONACCI -> 0xFFFFD600
                                                    DrawingTool.RISK_REWARD -> 0xFF00E676
                                                    DrawingTool.RULER -> 0xFFB388FF
                                                    else -> 0xFFFFFFFF
                                                }
                                            )
                                            drawings = drawings + newDrawing
                                        }
                                    }
                                    dragStartPoint = null
                                    dragCurrentPoint = null
                                }
                            )
                        }
                ) {
                    val fullWidth = size.width
                    val fullHeight = size.height
                    val hasSubChart = indicatorSettings.subChart != SubChartType.NONE
                    val mainHeight = if (hasSubChart) fullHeight * 0.72f else fullHeight - 20.dp.toPx()
                    val subHeight = if (hasSubChart) fullHeight * 0.24f else 0f
                    val subTopY = if (hasSubChart) fullHeight * 0.76f else fullHeight

                    val candleCount = displayCandles.size
                    val spacing = fullWidth / candleCount
                    val candleWidth = spacing * 0.72f

                    // --- Grid & Background ---
                    drawTradingViewGrid(fullWidth, mainHeight, 5)
                    if (hasSubChart) {
                        drawSubChartGrid(fullWidth, subTopY, subHeight)
                    }

                    // --- Bollinger Bands Shaded Area & Lines ---
                    if (indicatorSettings.showBollingerBands) {
                        drawBollingerBands(
                            bollingerBands = bollingerBands,
                            minPrice = minPrice,
                            priceRange = priceRange,
                            spacing = spacing,
                            mainHeight = mainHeight
                        )
                    }

                    // --- Render Chosen Chart Style (Candles, Heikin Ashi, Mountain Line) ---
                    when (chartStyle) {
                        ChartStyle.CANDLESTICK, ChartStyle.HEIKIN_ASHI -> {
                            drawCandlestickSeries(
                                candles = displayCandles,
                                minPrice = minPrice,
                                priceRange = priceRange,
                                spacing = spacing,
                                candleWidth = candleWidth,
                                mainHeight = mainHeight
                            )
                        }
                        ChartStyle.LINE_AREA -> {
                            drawLineAreaSeries(
                                candles = displayCandles,
                                minPrice = minPrice,
                                priceRange = priceRange,
                                spacing = spacing,
                                mainHeight = mainHeight
                            )
                        }
                        ChartStyle.HOLLOW_CANDLES -> {
                            drawHollowCandlesSeries(
                                candles = displayCandles,
                                minPrice = minPrice,
                                priceRange = priceRange,
                                spacing = spacing,
                                candleWidth = candleWidth,
                                mainHeight = mainHeight
                            )
                        }
                    }

                    // --- Indicator Overlays ---
                    if (indicatorSettings.showEMA9) {
                        drawIndicatorLine(ema9List, minPrice, priceRange, spacing, mainHeight, AccentCyan, 2f)
                    }
                    if (indicatorSettings.showEMA21) {
                        drawIndicatorLine(ema21List, minPrice, priceRange, spacing, mainHeight, AccentOrange, 2f)
                    }
                    if (indicatorSettings.showEMA50) {
                        drawIndicatorLine(ema50List, minPrice, priceRange, spacing, mainHeight, Color(0xFFB388FF), 2f)
                    }
                    if (indicatorSettings.showVWAP) {
                        drawIndicatorLine(vwapList, minPrice, priceRange, spacing, mainHeight, AccentGold, 2.2f, isDashed = true)
                    }
                    if (indicatorSettings.showSupertrend) {
                        drawSupertrendOverlay(supertrendList, minPrice, priceRange, spacing, mainHeight)
                    }

                    // --- Price Alerts Targets (Dashed Gold Lines) ---
                    alertPriceTargets.forEach { alertTarget ->
                        val alertY = mainHeight - ((alertTarget - minPrice) / priceRange * mainHeight).toFloat()
                        if (alertY in 0f..mainHeight) {
                            drawLine(
                                color = AccentGold,
                                start = Offset(0f, alertY),
                                end = Offset(fullWidth, alertY),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                        }
                    }

                    // --- User Drawings (Trendlines, Rays, Fibonacci, R:R Box, Ruler) ---
                    drawings.forEach { drawing ->
                        renderChartDrawing(drawing, minPrice, priceRange, spacing, mainHeight, fullWidth)
                    }

                    // --- Interactive In-Progress Drawing Drag Feedback ---
                    val dStart = dragStartPoint
                    val dCur = dragCurrentPoint
                    if (dStart != null && dCur != null && activeDrawingTool != DrawingTool.CURSOR) {
                        drawLine(
                            color = Color.White,
                            start = dStart,
                            end = dCur,
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    // --- Sub-Chart Oscillators Pane ---
                    if (hasSubChart) {
                        when (indicatorSettings.subChart) {
                            SubChartType.VOLUME -> {
                                drawVolumeSubChart(displayCandles, spacing, subTopY, subHeight)
                            }
                            SubChartType.RSI -> {
                                drawRsiSubChart(rsiList, spacing, subTopY, subHeight, fullWidth)
                            }
                            SubChartType.MACD -> {
                                drawMacdSubChart(macdList, spacing, subTopY, subHeight, fullWidth)
                            }
                            SubChartType.NONE -> Unit
                        }
                    }

                    // --- Magnetic Interactive Crosshair Overlay ---
                    val chOffset = crosshairOffset
                    if (chOffset != null && activeHoverIndex != null) {
                        val activeIdx = activeHoverIndex!!.coerceIn(0, candleCount - 1)
                        val snapX = activeIdx * spacing + (spacing / 2f)
                        val targetCandle = displayCandles[activeIdx]
                        val snapY = mainHeight - ((targetCandle.close - minPrice) / priceRange * mainHeight).toFloat()

                        // Vertical & Horizontal Dashed Crosshair Lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(snapX, 0f),
                            end = Offset(snapX, fullHeight),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.6f),
                            start = Offset(0f, chOffset.y.coerceIn(0f, mainHeight)),
                            end = Offset(fullWidth, chOffset.y.coerceIn(0f, mainHeight)),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )

                        // Highlight Active Candle Anchor
                        drawCircle(
                            color = AccentCyan,
                            radius = 4.dp.toPx(),
                            center = Offset(snapX, snapY)
                        )
                        drawCircle(
                            color = DarkBackground,
                            radius = 2.dp.toPx(),
                            center = Offset(snapX, snapY)
                        )
                    }
                }

                // Price Scale Labels along the right side of the canvas
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "$${String.format("%.2f", maxPrice)}",
                        color = BullishGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$${String.format("%.2f", minPrice + priceRange * 0.5)}",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "$${String.format("%.2f", minPrice)}",
                        color = BearishRed,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- 5. Bottom Sub-Chart Quick Toggle Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubChartType.values().forEach { subType ->
                        val isSelected = indicatorSettings.subChart == subType
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    indicatorSettings = indicatorSettings.copy(subChart = subType)
                                },
                            color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkSurface,
                            border = if (isSelected) BorderStroke(1.dp, AccentCyan) else BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text(
                                subType.shortName,
                                color = if (isSelected) AccentCyan else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    "⚡ Real-Time TradingView Engine",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // --- Indicator Overlay Configuration Sheet Modal ---
    if (showIndicatorsMenu) {
        AlertDialog(
            onDismissRequest = { showIndicatorsMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AccentCyan)
                    Text("TradingView Indicators & Overlays", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IndicatorToggleRow("EMA 9 (Fast Exponential)", indicatorSettings.showEMA9, AccentCyan) {
                        indicatorSettings = indicatorSettings.copy(showEMA9 = it)
                    }
                    IndicatorToggleRow("EMA 21 (Medium Trend)", indicatorSettings.showEMA21, AccentOrange) {
                        indicatorSettings = indicatorSettings.copy(showEMA21 = it)
                    }
                    IndicatorToggleRow("EMA 50 (Major Baseline)", indicatorSettings.showEMA50, Color(0xFFB388FF)) {
                        indicatorSettings = indicatorSettings.copy(showEMA50 = it)
                    }
                    IndicatorToggleRow("Bollinger Bands (20, 2.0)", indicatorSettings.showBollingerBands, Color(0xFF64B5F6)) {
                        indicatorSettings = indicatorSettings.copy(showBollingerBands = it)
                    }
                    IndicatorToggleRow("VWAP (Volume Weighted Avg)", indicatorSettings.showVWAP, AccentGold) {
                        indicatorSettings = indicatorSettings.copy(showVWAP = it)
                    }
                    IndicatorToggleRow("Supertrend (10, 3.0 ATR)", indicatorSettings.showSupertrend, BullishGreen) {
                        indicatorSettings = indicatorSettings.copy(showSupertrend = it)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIndicatorsMenu = false }) {
                    Text("Apply Indicators", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    // --- Technical Analysis Confluence Gauge Modal ---
    if (showTechnicalGaugeDialog) {
        TechnicalGaugeModal(
            gauge = technicalGauge,
            symbol = symbol,
            onDismiss = { showTechnicalGaugeDialog = false }
        )
    }
}

/**
 * Top Header with Symbol Info, Timeframe Switcher, Chart Styles, and Tool Actions
 */
@Composable
private fun TradingViewTopHeader(
    symbol: String,
    instrumentName: String,
    currentPrice: Double,
    change24h: Double,
    selectedTimeFrame: TimeFrame,
    onSelectTimeFrame: (TimeFrame) -> Unit,
    chartStyle: ChartStyle,
    onSelectChartStyle: (ChartStyle) -> Unit,
    onOpenIndicators: () -> Unit,
    onOpenTechnicalGauge: () -> Unit,
    showDrawingToolbar: Boolean,
    onToggleDrawingToolbar: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    technicalRating: TechnicalRating
) {
    val isPositive = change24h >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Symbol Name, Price, and Technical Rating Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(symbol, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(instrumentName, color = TextMuted, fontSize = 11.sp)
                }

                // Technical Rating Pill (e.g., STRONG BUY)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(technicalRating.colorHex).copy(alpha = 0.15f),
                    modifier = Modifier.clickable { onOpenTechnicalGauge() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(technicalRating.colorHex), CircleShape)
                        )
                        Text(
                            technicalRating.label,
                            color = Color(technicalRating.colorHex),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Technical Rating",
                            tint = Color(technicalRating.colorHex),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Real-Time Price Readout
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${if (currentPrice >= 1000) String.format("%,.2f", currentPrice) else String.format("%.2f", currentPrice)}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "${if (isPositive) "+" else ""}${String.format("%.2f", change24h)}%",
                    color = if (isPositive) BullishGreen else BearishRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Row 2: Timeframes & Chart Function Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframes (1m, 5m, 15m, 1h, 1D)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeFrame.values().forEach { tf ->
                    val isSelected = tf == selectedTimeFrame
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectTimeFrame(tf) },
                        color = if (isSelected) AccentCyan else DarkSurface,
                        border = if (!isSelected) BorderStroke(1.dp, DarkBorder) else null
                    ) {
                        Text(
                            tf.label,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Chart Styles & Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Chart Style Selector
                var styleMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { styleMenuExpanded = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            when (chartStyle) {
                                ChartStyle.CANDLESTICK -> Icons.Default.CandlestickChart
                                ChartStyle.HEIKIN_ASHI -> Icons.Default.ShowChart
                                ChartStyle.LINE_AREA -> Icons.Default.MultilineChart
                                ChartStyle.HOLLOW_CANDLES -> Icons.Default.BarChart
                            },
                            contentDescription = "Chart Style",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = styleMenuExpanded,
                        onDismissRequest = { styleMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        ChartStyle.values().forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style.label, color = if (style == chartStyle) AccentCyan else TextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    onSelectChartStyle(style)
                                    styleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Indicators Dialog Trigger
                IconButton(onClick = onOpenIndicators, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Analytics, contentDescription = "Indicators", tint = AccentGold, modifier = Modifier.size(18.dp))
                }

                // Drawing Tools Ribbon Toggle
                IconButton(onClick = onToggleDrawingToolbar, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = "Drawing Tools",
                        tint = if (showDrawingToolbar) AccentCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Fullscreen / Expand
                IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Floating/Horizontal Drawing Tools Toolbar
 */
@Composable
private fun TradingViewDrawingToolbar(
    activeTool: DrawingTool,
    onSelectTool: (DrawingTool) -> Unit,
    drawingsCount: Int,
    onClearDrawings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tools:", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            DrawingTool.values().forEach { tool ->
                val isSelected = activeTool == tool
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectTool(tool) },
                    color = if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    border = if (isSelected) BorderStroke(1.dp, AccentCyan) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            when (tool) {
                                DrawingTool.CURSOR -> Icons.Default.NearMe
                                DrawingTool.TREND_LINE -> Icons.Default.TrendingUp
                                DrawingTool.HORIZONTAL_RAY -> Icons.Default.HorizontalRule
                                DrawingTool.FIBONACCI -> Icons.Default.StackedLineChart
                                DrawingTool.RISK_REWARD -> Icons.Default.AspectRatio
                                DrawingTool.RULER -> Icons.Default.Straighten
                            },
                            contentDescription = tool.label,
                            tint = if (isSelected) AccentCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            tool.label,
                            color = if (isSelected) AccentCyan else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            if (drawingsCount > 0) {
                IconButton(onClick = onClearDrawings, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Drawings", tint = BearishRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Top Data Window & Indicator Legend (TradingView Style)
 */
@Composable
private fun TradingViewDataLegend(
    candle: CandleStick?,
    ema9: Double?,
    ema21: Double?,
    ema50: Double?,
    vwap: Double?,
    supertrend: SupertrendPoint?,
    subChartType: SubChartType,
    rsi: Double?,
    macd: MacdPoint?
) {
    if (candle == null) return

    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val isBullish = candle.close >= candle.open

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Line 1: OHLCV Values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(sdf.format(Date(candle.timestamp)), color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("O: ${String.format("%.2f", candle.open)}", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("H: ${String.format("%.2f", candle.high)}", color = BullishGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("L: ${String.format("%.2f", candle.low)}", color = BearishRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("C: ${String.format("%.2f", candle.close)}", color = if (isBullish) BullishGreen else BearishRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        // Line 2: Active Indicators Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ema9 != null) {
                Text("EMA 9: ${String.format("%.2f", ema9)}", color = AccentCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            if (ema21 != null) {
                Text("EMA 21: ${String.format("%.2f", ema21)}", color = AccentOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            if (ema50 != null) {
                Text("EMA 50: ${String.format("%.2f", ema50)}", color = Color(0xFFB388FF), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            if (vwap != null) {
                Text("VWAP: ${String.format("%.2f", vwap)}", color = AccentGold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            if (supertrend != null) {
                Text(
                    "Supertrend: ${String.format("%.2f", supertrend.value)} (${if (supertrend.isBullish) "BUY" else "SELL"})",
                    color = if (supertrend.isBullish) BullishGreen else BearishRed,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            when (subChartType) {
                SubChartType.RSI -> if (rsi != null) {
                    Text("RSI(14): ${String.format("%.1f", rsi)}", color = Color(0xFFE040FB), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                SubChartType.MACD -> if (macd != null) {
                    Text("MACD: ${String.format("%.2f", macd.macd)} Signal: ${String.format("%.2f", macd.signal)} Hist: ${String.format("%.2f", macd.histogram)}", color = AccentCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                SubChartType.VOLUME -> {
                    Text("Vol: ${String.format("%,.0f", candle.volume)}", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
                SubChartType.NONE -> Unit
            }
        }
    }
}

/**
 * Toggle Row for Indicator Configuration Sheet
 */
@Composable
private fun IndicatorToggleRow(
    title: String,
    checked: Boolean,
    colorAccent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(colorAccent, RoundedCornerShape(3.dp))
            )
            Text(title, color = TextPrimary, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * TradingView Speedometer Confluence Technical Rating Sheet
 */
@Composable
private fun TechnicalGaugeModal(
    gauge: TechnicalGauge,
    symbol: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(gauge.overallRating.colorHex))
                Text("$symbol Technical Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Big Summary Gauge Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(gauge.overallRating.colorHex).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(gauge.overallRating.colorHex))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            gauge.overallRating.label,
                            color = Color(gauge.overallRating.colorHex),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Score: ${if (gauge.summaryScore > 0) "+" else ""}${gauge.summaryScore} • Confluence Rating",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Moving Averages Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Moving Averages:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(gauge.movingAverageRating.label, color = Color(gauge.movingAverageRating.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Buy: ${gauge.movingAverageBuyCount}", color = BullishGreen, fontSize = 11.sp)
                        Text("Neutral: ${gauge.movingAverageNeutralCount}", color = TextMuted, fontSize = 11.sp)
                        Text("Sell: ${gauge.movingAverageSellCount}", color = BearishRed, fontSize = 11.sp)
                    }
                }

                Divider(color = DarkBorder)

                // Oscillators Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Oscillators (RSI / MACD):", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(gauge.oscillatorRating.label, color = Color(gauge.oscillatorRating.colorHex), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RSI(14): ${String.format("%.1f", gauge.rsiValue)}", color = Color(0xFFE040FB), fontSize = 11.sp)
                        Text("MACD: ${gauge.macdSignal}", color = AccentCyan, fontSize = 11.sp)
                        Text("Trend: ${gauge.adxStrength}", color = AccentGold, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

// -------------------------------------------------------------
// Canvas Drawing Helper Extensions
// -------------------------------------------------------------

private fun DrawScope.drawTradingViewGrid(width: Float, height: Float, gridLines: Int) {
    for (i in 0..gridLines) {
        val y = (height / gridLines) * i
        drawLine(
            color = DarkBorder.copy(alpha = 0.5f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawSubChartGrid(width: Float, subTopY: Float, subHeight: Float) {
    drawLine(
        color = DarkBorder,
        start = Offset(0f, subTopY),
        end = Offset(width, subTopY),
        strokeWidth = 1.5f
    )
    val midY = subTopY + (subHeight / 2f)
    drawLine(
        color = DarkBorder.copy(alpha = 0.4f),
        start = Offset(0f, midY),
        end = Offset(width, midY),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )
}

private fun DrawScope.drawCandlestickSeries(
    candles: List<CandleStick>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    candleWidth: Float,
    mainHeight: Float
) {
    candles.forEachIndexed { index, candle ->
        val centerX = index * spacing + (spacing / 2f)
        val isBullish = candle.close >= candle.open
        val color = if (isBullish) BullishGreen else BearishRed

        val openY = mainHeight - ((candle.open - minPrice) / priceRange * mainHeight).toFloat()
        val closeY = mainHeight - ((candle.close - minPrice) / priceRange * mainHeight).toFloat()
        val highY = mainHeight - ((candle.high - minPrice) / priceRange * mainHeight).toFloat()
        val lowY = mainHeight - ((candle.low - minPrice) / priceRange * mainHeight).toFloat()

        // High-Low Wick
        drawLine(
            color = color,
            start = Offset(centerX, highY),
            end = Offset(centerX, lowY),
            strokeWidth = 1.8f
        )

        // Body
        val topY = minOf(openY, closeY)
        val bodyHeight = max(abs(closeY - openY), 2f)
        drawRect(
            color = color,
            topLeft = Offset(centerX - (candleWidth / 2f), topY),
            size = Size(candleWidth, bodyHeight)
        )
    }
}

private fun DrawScope.drawLineAreaSeries(
    candles: List<CandleStick>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    mainHeight: Float
) {
    if (candles.size < 2) return
    val linePath = Path()
    val areaPath = Path()

    candles.forEachIndexed { index, candle ->
        val x = index * spacing + (spacing / 2f)
        val y = mainHeight - ((candle.close - minPrice) / priceRange * mainHeight).toFloat()
        if (index == 0) {
            linePath.moveTo(x, y)
            areaPath.moveTo(x, mainHeight)
            areaPath.lineTo(x, y)
        } else {
            linePath.lineTo(x, y)
            areaPath.lineTo(x, y)
        }
    }

    val lastX = (candles.size - 1) * spacing + (spacing / 2f)
    areaPath.lineTo(lastX, mainHeight)
    areaPath.close()

    // Glowing Gradient Fill
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(AccentCyan.copy(alpha = 0.35f), Color.Transparent),
            startY = 0f,
            endY = mainHeight
        )
    )

    // Vibrant Mountain Line
    drawPath(
        path = linePath,
        color = AccentCyan,
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawHollowCandlesSeries(
    candles: List<CandleStick>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    candleWidth: Float,
    mainHeight: Float
) {
    candles.forEachIndexed { index, candle ->
        val centerX = index * spacing + (spacing / 2f)
        val isBullish = candle.close >= candle.open
        val color = if (isBullish) BullishGreen else BearishRed

        val openY = mainHeight - ((candle.open - minPrice) / priceRange * mainHeight).toFloat()
        val closeY = mainHeight - ((candle.close - minPrice) / priceRange * mainHeight).toFloat()
        val highY = mainHeight - ((candle.high - minPrice) / priceRange * mainHeight).toFloat()
        val lowY = mainHeight - ((candle.low - minPrice) / priceRange * mainHeight).toFloat()

        drawLine(color = color, start = Offset(centerX, highY), end = Offset(centerX, lowY), strokeWidth = 1.8f)

        val topY = minOf(openY, closeY)
        val bodyHeight = max(abs(closeY - openY), 2f)

        if (isBullish) {
            drawRect(
                color = color,
                topLeft = Offset(centerX - (candleWidth / 2f), topY),
                size = Size(candleWidth, bodyHeight),
                style = Stroke(width = 1.8f)
            )
        } else {
            drawRect(
                color = color,
                topLeft = Offset(centerX - (candleWidth / 2f), topY),
                size = Size(candleWidth, bodyHeight)
            )
        }
    }
}

private fun DrawScope.drawIndicatorLine(
    values: List<Double?>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    mainHeight: Float,
    color: Color,
    strokeWidth: Float,
    isDashed: Boolean = false
) {
    val path = Path()
    var started = false

    values.forEachIndexed { index, value ->
        if (value != null) {
            val x = index * spacing + (spacing / 2f)
            val y = mainHeight - ((value - minPrice) / priceRange * mainHeight).toFloat()
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (started) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                pathEffect = if (isDashed) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null
            )
        )
    }
}

private fun DrawScope.drawBollingerBands(
    bollingerBands: List<BollingerBand?>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    mainHeight: Float
) {
    val upperPath = Path()
    val lowerPath = Path()
    val fillPath = Path()
    var started = false

    val validIndices = bollingerBands.mapIndexedNotNull { idx, b -> if (b != null) idx to b else null }
    if (validIndices.isEmpty()) return

    validIndices.forEachIndexed { i, (idx, bb) ->
        val x = idx * spacing + (spacing / 2f)
        val upperY = mainHeight - ((bb.upper - minPrice) / priceRange * mainHeight).toFloat()
        val lowerY = mainHeight - ((bb.lower - minPrice) / priceRange * mainHeight).toFloat()

        if (i == 0) {
            upperPath.moveTo(x, upperY)
            lowerPath.moveTo(x, lowerY)
            fillPath.moveTo(x, upperY)
        } else {
            upperPath.lineTo(x, upperY)
            lowerPath.lineTo(x, lowerY)
            fillPath.lineTo(x, upperY)
        }
    }

    // Traverse bottom backwards for closed fill
    for (i in validIndices.indices.reversed()) {
        val (idx, bb) = validIndices[i]
        val x = idx * spacing + (spacing / 2f)
        val lowerY = mainHeight - ((bb.lower - minPrice) / priceRange * mainHeight).toFloat()
        fillPath.lineTo(x, lowerY)
    }
    fillPath.close()

    drawPath(path = fillPath, color = Color(0xFF64B5F6).copy(alpha = 0.08f), style = Fill)
    drawPath(path = upperPath, color = Color(0xFF64B5F6).copy(alpha = 0.7f), style = Stroke(width = 1.2f))
    drawPath(path = lowerPath, color = Color(0xFF64B5F6).copy(alpha = 0.7f), style = Stroke(width = 1.2f))
}

private fun DrawScope.drawSupertrendOverlay(
    supertrendList: List<SupertrendPoint?>,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    mainHeight: Float
) {
    supertrendList.forEachIndexed { index, st ->
        if (st != null) {
            val x = index * spacing + (spacing / 2f)
            val y = mainHeight - ((st.value - minPrice) / priceRange * mainHeight).toFloat()
            val color = if (st.isBullish) BullishGreen else BearishRed

            // Draw small signal dot / triangle
            drawCircle(
                color = color,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawVolumeSubChart(
    candles: List<CandleStick>,
    spacing: Float,
    subTopY: Float,
    subHeight: Float
) {
    val maxVol = candles.maxOfOrNull { it.volume } ?: 1.0
    val barWidth = spacing * 0.7f

    candles.forEachIndexed { index, candle ->
        val x = index * spacing + (spacing / 2f)
        val isBullish = candle.close >= candle.open
        val color = if (isBullish) BullishGreen.copy(alpha = 0.6f) else BearishRed.copy(alpha = 0.6f)
        val volHeight = (candle.volume / maxVol * subHeight).toFloat().coerceAtLeast(2f)

        drawRect(
            color = color,
            topLeft = Offset(x - (barWidth / 2f), subTopY + subHeight - volHeight),
            size = Size(barWidth, volHeight)
        )
    }
}

private fun DrawScope.drawRsiSubChart(
    rsiList: List<Double?>,
    spacing: Float,
    subTopY: Float,
    subHeight: Float,
    width: Float
) {
    val rsi70Y = subTopY + subHeight - (70f / 100f * subHeight)
    val rsi30Y = subTopY + subHeight - (30f / 100f * subHeight)

    // Overbought / Oversold Band Fill
    drawRect(
        color = Color(0xFFE040FB).copy(alpha = 0.08f),
        topLeft = Offset(0f, rsi70Y),
        size = Size(width, rsi30Y - rsi70Y)
    )

    drawLine(color = BearishRed.copy(alpha = 0.5f), start = Offset(0f, rsi70Y), end = Offset(width, rsi70Y), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
    drawLine(color = BullishGreen.copy(alpha = 0.5f), start = Offset(0f, rsi30Y), end = Offset(width, rsi30Y), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))

    val path = Path()
    var started = false

    rsiList.forEachIndexed { index, rsi ->
        if (rsi != null) {
            val x = index * spacing + (spacing / 2f)
            val y = subTopY + subHeight - (rsi.toFloat() / 100f * subHeight)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (started) {
        drawPath(path = path, color = Color(0xFFE040FB), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawMacdSubChart(
    macdList: List<MacdPoint?>,
    spacing: Float,
    subTopY: Float,
    subHeight: Float,
    width: Float
) {
    val validMacd = macdList.filterNotNull()
    if (validMacd.isEmpty()) return

    val maxVal = max(
        validMacd.maxOfOrNull { max(abs(it.macd), max(abs(it.signal), abs(it.histogram))) } ?: 1.0,
        0.001
    )
    val zeroY = subTopY + (subHeight / 2f)

    // Draw Zero line
    drawLine(color = DarkBorder, start = Offset(0f, zeroY), end = Offset(width, zeroY), strokeWidth = 1f)

    // Draw Histogram Bars
    val barWidth = spacing * 0.6f
    macdList.forEachIndexed { index, item ->
        if (item != null) {
            val x = index * spacing + (spacing / 2f)
            val histHeight = (abs(item.histogram) / maxVal * (subHeight / 2f)).toFloat()
            val isPositive = item.histogram >= 0
            val color = if (isPositive) BullishGreen else BearishRed
            val topY = if (isPositive) zeroY - histHeight else zeroY

            drawRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(x - (barWidth / 2f), topY),
                size = Size(barWidth, max(histHeight, 1f))
            )
        }
    }

    // Draw MACD Line & Signal Line
    val macdPath = Path()
    val signalPath = Path()
    var started = false

    macdList.forEachIndexed { index, item ->
        if (item != null) {
            val x = index * spacing + (spacing / 2f)
            val mY = zeroY - (item.macd / maxVal * (subHeight / 2f)).toFloat()
            val sY = zeroY - (item.signal / maxVal * (subHeight / 2f)).toFloat()

            if (!started) {
                macdPath.moveTo(x, mY)
                signalPath.moveTo(x, sY)
                started = true
            } else {
                macdPath.lineTo(x, mY)
                signalPath.lineTo(x, sY)
            }
        }
    }

    if (started) {
        drawPath(path = macdPath, color = AccentCyan, style = Stroke(width = 1.8f))
        drawPath(path = signalPath, color = AccentOrange, style = Stroke(width = 1.8f))
    }
}

private fun DrawScope.renderChartDrawing(
    drawing: ChartDrawing,
    minPrice: Double,
    priceRange: Double,
    spacing: Float,
    mainHeight: Float,
    fullWidth: Float
) {
    val startX = drawing.startIndex * spacing + (spacing / 2f)
    val endX = drawing.endIndex * spacing + (spacing / 2f)
    val startY = mainHeight - ((drawing.startPrice - minPrice) / priceRange * mainHeight).toFloat()
    val endY = mainHeight - ((drawing.endPrice - minPrice) / priceRange * mainHeight).toFloat()
    val color = Color(drawing.colorHex)

    when (drawing.type) {
        DrawingTool.TREND_LINE -> {
            drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 2.5f)
            drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(startX, startY))
            drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(endX, endY))
        }
        DrawingTool.HORIZONTAL_RAY -> {
            drawLine(
                color = color,
                start = Offset(0f, startY),
                end = Offset(fullWidth, startY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
            )
        }
        DrawingTool.FIBONACCI -> {
            val fibLevels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0)
            val pDiff = drawing.endPrice - drawing.startPrice

            fibLevels.forEach { lvl ->
                val lvlPrice = drawing.startPrice + pDiff * lvl
                val lvlY = mainHeight - ((lvlPrice - minPrice) / priceRange * mainHeight).toFloat()
                if (lvlY in 0f..mainHeight) {
                    val lvlColor = when (lvl) {
                        0.618, 0.5 -> AccentGold
                        0.382, 0.786 -> AccentCyan
                        else -> Color.White.copy(alpha = 0.7f)
                    }
                    drawLine(
                        color = lvlColor,
                        start = Offset(startX, lvlY),
                        end = Offset(fullWidth, lvlY),
                        strokeWidth = if (lvl == 0.618 || lvl == 0.5) 2f else 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                    )
                }
            }
        }
        DrawingTool.RISK_REWARD -> {
            val targetY = min(startY, endY)
            val stopLossY = max(startY, endY)
            val entryY = (startY + endY) / 2f

            // Target Profit Area (Green)
            drawRect(
                color = BullishGreen.copy(alpha = 0.2f),
                topLeft = Offset(startX, targetY),
                size = Size(abs(endX - startX).coerceAtLeast(60f), entryY - targetY)
            )
            // Stop Loss Area (Red)
            drawRect(
                color = BearishRed.copy(alpha = 0.2f),
                topLeft = Offset(startX, entryY),
                size = Size(abs(endX - startX).coerceAtLeast(60f), stopLossY - entryY)
            )
        }
        DrawingTool.RULER -> {
            drawLine(
                color = Color(0xFFB388FF),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
            drawRect(
                color = Color(0xFFB388FF).copy(alpha = 0.15f),
                topLeft = Offset(min(startX, endX), min(startY, endY)),
                size = Size(abs(endX - startX), abs(endY - startY))
            )
        }
        DrawingTool.CURSOR -> Unit
    }
}
