package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.math.abs

@Composable
fun AdvancedChartCanvas(
    candles: List<Candle>,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    // Gesture state
    var scaleX by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scaleX = (scaleX * zoom).coerceIn(0.2f, 10f)
                    offsetX -= pan.x
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

            // Base dimensions per candle
            val defaultCandleWidth = 24f
            val actualCandleWidth = defaultCandleWidth * scaleX
            val candleSpacing = 6f * scaleX

            // Total width of one candle + spacing
            val totalCandleWidth = actualCandleWidth + candleSpacing

            // Number of candles that can fit in the chart area
            val visibleCandlesCount = (chartWidth / totalCandleWidth).toInt() + 2

            // Calculate starting index based on offset (scroll)
            val maxOffset = (candles.size * totalCandleWidth) - chartWidth
            val boundedOffset = offsetX.coerceIn(0f, maxOffset.coerceAtLeast(0f))
            if (offsetX != boundedOffset) {
                offsetX = boundedOffset
            }

            val startIndex = (boundedOffset / totalCandleWidth).toInt().coerceIn(0, candles.size - 1)
            val endIndex = (startIndex + visibleCandlesCount).coerceAtMost(candles.size)

            val visibleCandles = candles.subList(startIndex, endIndex)
            if (visibleCandles.isEmpty()) return@Canvas

            // Find min/max price for the visible range to scale vertically
            val maxPrice = visibleCandles.maxOf { it.high }
            val minPrice = visibleCandles.minOf { it.low }
            val priceRange = maxPrice - minPrice
            // Add 10% padding top and bottom
            val padding = priceRange * 0.1
            val chartMax = maxPrice + padding
            val chartMin = minPrice - padding
            val adjustedRange = chartMax - chartMin

            // Draw Background Grid
            drawGrid(chartWidth, chartHeight, chartMin, chartMax, adjustedRange)

            // Draw Candles
            visibleCandles.forEachIndexed { index, candle ->
                val x = chartWidth - ((index * totalCandleWidth) + (boundedOffset % totalCandleWidth)) - totalCandleWidth
                
                val openY = chartHeight - ((candle.open - chartMin) / adjustedRange * chartHeight).toFloat()
                val closeY = chartHeight - ((candle.close - chartMin) / adjustedRange * chartHeight).toFloat()
                val highY = chartHeight - ((candle.high - chartMin) / adjustedRange * chartHeight).toFloat()
                val lowY = chartHeight - ((candle.low - chartMin) / adjustedRange * chartHeight).toFloat()

                val color = if (candle.close >= candle.open) TvGreen else TvRed

                // Draw Wick
                drawLine(
                    color = color,
                    start = Offset(x + actualCandleWidth / 2, highY),
                    end = Offset(x + actualCandleWidth / 2, lowY),
                    strokeWidth = 2f * scaleX.coerceAtMost(1.5f)
                )

                // Draw Body
                val bodyTop = minOf(openY, closeY)
                val bodyBottom = maxOf(openY, closeY)
                val bodyHeight = maxOf(bodyBottom - bodyTop, 1f) // Ensure at least 1px height
                drawRect(
                    color = color,
                    topLeft = Offset(x, bodyTop),
                    size = Size(actualCandleWidth, bodyHeight)
                )
            }

            // Draw Price Axis (Right)
            drawRect(
                color = TvBackground,
                topLeft = Offset(chartWidth, 0f),
                size = Size(priceAxisWidth, canvasHeight)
            )
            drawLine(
                color = TvDivider,
                start = Offset(chartWidth, 0f),
                end = Offset(chartWidth, canvasHeight),
                strokeWidth = 2f
            )

            // Current Price Tag
            val currentPrice = candles.last().close
            val currentY = chartHeight - ((currentPrice - chartMin) / adjustedRange * chartHeight).toFloat()
            val tagColor = if (currentPrice >= candles.last().open) TvGreen else TvRed
            
            if (currentY in 0f..chartHeight) {
                // Background tag
                val path = Path().apply {
                    moveTo(chartWidth, currentY)
                    lineTo(chartWidth + 15f, currentY - 15f)
                    lineTo(canvasWidth, currentY - 15f)
                    lineTo(canvasWidth, currentY + 15f)
                    lineTo(chartWidth + 15f, currentY + 15f)
                    close()
                }
                drawPath(path, tagColor)
                
                // Draw text
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

private fun DrawScope.drawGrid(chartWidth: Float, chartHeight: Float, minPrice: Double, maxPrice: Double, range: Double) {
    val gridLines = 8
    for (i in 0..gridLines) {
        val y = (chartHeight / gridLines) * i
        drawLine(
            color = TvDivider,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f
        )
    }
}
