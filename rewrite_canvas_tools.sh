#!/bin/bash

# Update Watchlist Header to "Market Watch"
sed -i 's/"Watchlist", fontSize = 24.sp/"Market Watch", fontSize = 24.sp/g' app/src/main/java/com/example/ui/screens/WatchlistScreen.kt

cat << 'INNER_EOF' > app/src/main/java/com/example/ui/components/AdvancedChartCanvas.kt
package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.domain.trading.Candle
import com.example.ui.theme.*
import java.util.Locale

data class TrendLine(
    val startTime: Long,
    val startPrice: Double,
    val endTime: Long,
    val endPrice: Double
)

@Composable
fun AdvancedChartCanvas(
    candles: List<Candle>,
    isDrawingMode: Boolean = false,
    drawnLines: List<TrendLine> = emptyList(),
    onLineDrawn: (TrendLine) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    val bgColor = MaterialTheme.colorScheme.background
    val dividerColor = MaterialTheme.colorScheme.outline
    val drawingColor = TvBlue

    var scaleX by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }

    var currentChartWidth by remember { mutableFloatStateOf(0f) }
    var currentChartHeight by remember { mutableFloatStateOf(0f) }
    var currentBoundedOffset by remember { mutableFloatStateOf(0f) }
    var currentTotalCandleWidth by remember { mutableFloatStateOf(0f) }
    var currentChartMin by remember { mutableDoubleStateOf(0.0) }
    var currentAdjustedRange by remember { mutableDoubleStateOf(0.0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(isDrawingMode, candles) {
                if (isDrawingMode) {
                    detectDragGestures(
                        onDragStart = { offset -> 
                            currentDragStart = offset
                            currentDragEnd = offset
                        },
                        onDrag = { change, _ ->
                            currentDragEnd = change.position
                        },
                        onDragEnd = {
                            val start = currentDragStart
                            val end = currentDragEnd
                            if (start != null && end != null && candles.isNotEmpty()) {
                                // Convert Screen to Data coordinates for permanent anchoring
                                val startGlobalX = currentChartWidth - start.x + currentBoundedOffset
                                val startIdx = (startGlobalX / currentTotalCandleWidth).toInt().coerceIn(0, candles.size - 1)
                                val startTime = candles[startIdx].timestamp
                                val startPrice = currentChartMin + ((currentChartHeight - start.y) / currentChartHeight) * currentAdjustedRange

                                val endGlobalX = currentChartWidth - end.x + currentBoundedOffset
                                val endIdx = (endGlobalX / currentTotalCandleWidth).toInt().coerceIn(0, candles.size - 1)
                                val endTime = candles[endIdx].timestamp
                                val endPrice = currentChartMin + ((currentChartHeight - end.y) / currentChartHeight) * currentAdjustedRange

                                onLineDrawn(TrendLine(startTime, startPrice, endTime, endPrice))
                            }
                            currentDragStart = null
                            currentDragEnd = null
                        }
                    )
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scaleX = (scaleX * zoom).coerceIn(0.2f, 10f)
                        offsetX -= pan.x
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val priceAxisWidth = 150f
            val timeAxisHeight = 80f
            val chartWidth = canvasWidth - priceAxisWidth
            val chartHeight = canvasHeight - timeAxisHeight

            val defaultCandleWidth = 24f
            val actualCandleWidth = defaultCandleWidth * scaleX
            val candleSpacing = 6f * scaleX
            val totalCandleWidth = actualCandleWidth + candleSpacing
            
            val visibleCandlesCount = (chartWidth / totalCandleWidth).toInt() + 2
            val maxOffset = (candles.size * totalCandleWidth) - chartWidth
            val boundedOffset = offsetX.coerceIn(0f, maxOffset.coerceAtLeast(0f))
            
            if (offsetX != boundedOffset) {
                offsetX = boundedOffset
            }
            
            val startIndex = (boundedOffset / totalCandleWidth).toInt().coerceIn(0, candles.size - 1)
            val endIndex = (startIndex + visibleCandlesCount).coerceAtMost(candles.size)
            val visibleCandles = candles.subList(startIndex, endIndex)
            if (visibleCandles.isEmpty()) return@Canvas

            val maxPrice = visibleCandles.maxOf { it.high }
            val minPrice = visibleCandles.minOf { it.low }
            val priceRange = maxPrice - minPrice
            val padding = priceRange * 0.1
            val chartMax = maxPrice + padding
            val chartMin = minPrice - padding
            val adjustedRange = chartMax - chartMin

            currentChartWidth = chartWidth
            currentChartHeight = chartHeight
            currentBoundedOffset = boundedOffset
            currentTotalCandleWidth = totalCandleWidth
            currentChartMin = chartMin
            currentAdjustedRange = adjustedRange

            drawGrid(chartWidth, chartHeight, dividerColor)

            visibleCandles.forEachIndexed { index, candle ->
                val x = chartWidth - ((index * totalCandleWidth) + (boundedOffset % totalCandleWidth)) - totalCandleWidth
                
                val openY = chartHeight - ((candle.open - chartMin) / adjustedRange * chartHeight).toFloat()
                val closeY = chartHeight - ((candle.close - chartMin) / adjustedRange * chartHeight).toFloat()
                val highY = chartHeight - ((candle.high - chartMin) / adjustedRange * chartHeight).toFloat()
                val lowY = chartHeight - ((candle.low - chartMin) / adjustedRange * chartHeight).toFloat()

                val color = if (candle.close >= candle.open) TvGreen else TvRed

                drawLine(
                    color = color,
                    start = Offset(x + actualCandleWidth / 2, highY),
                    end = Offset(x + actualCandleWidth / 2, lowY),
                    strokeWidth = 2f * scaleX.coerceAtMost(1.5f)
                )

                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY)
                val bodyHeight = maxOf(bodyBottom - bodyTop, 1f)
                
                drawRect(
                    color = color,
                    topLeft = Offset(x, bodyTop),
                    size = Size(actualCandleWidth, bodyHeight)
                )
            }

            // Draw Anchored TrendLines
            drawnLines.forEach { line ->
                val startIdx = candles.indexOfFirst { it.timestamp == line.startTime }.takeIf { it >= 0 }
                val endIdx = candles.indexOfFirst { it.timestamp == line.endTime }.takeIf { it >= 0 }
                
                if (startIdx != null && endIdx != null) {
                    val startGlobalX = startIdx * totalCandleWidth
                    val startScreenX = chartWidth - startGlobalX + boundedOffset - totalCandleWidth / 2
                    val startScreenY = chartHeight - ((line.startPrice - chartMin) / adjustedRange * chartHeight).toFloat()

                    val endGlobalX = endIdx * totalCandleWidth
                    val endScreenX = chartWidth - endGlobalX + boundedOffset - totalCandleWidth / 2
                    val endScreenY = chartHeight - ((line.endPrice - chartMin) / adjustedRange * chartHeight).toFloat()

                    drawLine(
                        color = drawingColor,
                        start = Offset(startScreenX, startScreenY),
                        end = Offset(endScreenX, endScreenY),
                        strokeWidth = 4f
                    )
                }
            }

            // Draw live active drag line
            val cStart = currentDragStart
            val cEnd = currentDragEnd
            if (cStart != null && cEnd != null) {
                drawLine(
                    color = drawingColor,
                    start = cStart,
                    end = cEnd,
                    strokeWidth = 4f
                )
            }

            // Draw Price Axis Background
            drawRect(
                color = bgColor,
                topLeft = Offset(chartWidth, 0f),
                size = Size(priceAxisWidth, canvasHeight)
            )
            drawLine(
                color = dividerColor,
                start = Offset(chartWidth, 0f),
                end = Offset(chartWidth, canvasHeight),
                strokeWidth = 2f
            )

            // Current Price Tag
            val currentPrice = candles.last().close
            val currentY = chartHeight - ((currentPrice - chartMin) / adjustedRange * chartHeight).toFloat()
            val tagColor = if (currentPrice >= candles.last().open) TvGreen else TvRed
            
            if (currentY in 0f..chartHeight) {
                val path = Path().apply {
                    moveTo(chartWidth, currentY)
                    lineTo(chartWidth + 15f, currentY - 15f)
                    lineTo(canvasWidth, currentY - 15f)
                    lineTo(canvasWidth, currentY + 15f)
                    lineTo(chartWidth + 15f, currentY + 15f)
                    close()
                }
                drawPath(path, tagColor)
                
                val paint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 28f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.3f", currentPrice),
                    chartWidth + 20f,
                    currentY + 10f,
                    paint
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(chartWidth: Float, chartHeight: Float, dividerColor: Color) {
    val gridLines = 8
    for (i in 0..gridLines) {
        val y = (chartHeight / gridLines) * i
        drawLine(
            color = dividerColor.copy(alpha = 0.5f),
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f
        )
    }
}
INNER_EOF

cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/ChartScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.components.AdvancedChartCanvas
import com.example.ui.components.TrendLine
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ChartScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val quotes by viewModel.quotes.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val timeframe by viewModel.timeframe.collectAsState()
    val candles by viewModel.candles.collectAsState()
    
    // Core drawing state
    var isDrawingMode by remember { mutableStateOf(false) }
    val drawnLines = remember { mutableStateListOf<TrendLine>() }

    val currentQuote = quotes.find { it.symbol == selectedSymbol }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = maxWidth > 800.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            
            // LEFT DRAWING TOOLBAR (Desktop Only - Exact TV Layout)
            if (isWide) {
                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = { isDrawingMode = false }) {
                        Icon(Icons.Default.NearMe, contentDescription = "Cursor", tint = if (!isDrawingMode) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { isDrawingMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Trend Line", tint = if (isDrawingMode) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { drawnLines.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }

            // MAIN CHART AREA
            Column(modifier = Modifier.weight(1f)) {
                // Top Toolbar (Symbol + Scrollable Timeframes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedSymbol,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1m", "5m", "15m", "1h", "4h", "D", "W").forEach { tf ->
                            val isSelected = timeframe.label == tf
                            Text(
                                text = tf,
                                color = if (isSelected) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                    
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Chart Type", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp))
                    Icon(Icons.Default.Add, contentDescription = "Compare", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Main Chart Canvas with interactive Drawing Engine
                Box(modifier = Modifier.weight(1f)) {
                    AdvancedChartCanvas(
                        candles = candles,
                        isDrawingMode = isDrawingMode,
                        drawnLines = drawnLines,
                        onLineDrawn = { drawnLines.add(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Bottom Toolbar (Mobile Only)
                if (!isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isDrawingMode = !isDrawingMode }) {
                            Icon(Icons.Default.Edit, contentDescription = "Draw", tint = if (isDrawingMode) TvBlue else MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { drawnLines.clear() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = MaterialTheme.colorScheme.onBackground)
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // RIGHT SIDEBAR (Market Watch / Tools)
            if (isWide) {
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                
                Row(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                    // Market Watch Component Content
                    Box(modifier = Modifier.weight(1f)) {
                        WatchlistScreen(viewModel = viewModel)
                    }
                    
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    
                    // Far-Right TradingView Navigation Rail
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Watchlist", tint = TvBlue, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.Newspaper, contentDescription = "News", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Hotlists", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}
INNER_EOF
