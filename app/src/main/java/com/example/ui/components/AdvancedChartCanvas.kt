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
                    typeface = android.graphics.Typeface.MONOSPACE
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
