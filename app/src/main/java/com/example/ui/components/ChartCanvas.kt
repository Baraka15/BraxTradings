package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.CandleStick
import com.example.ui.theme.*

@Composable
fun RealtimeCandleChart(
    candles: List<CandleStick>,
    alertPriceTargets: List<Double> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading Real-Time Chart...", color = TextSecondary)
        }
        return
    }

    val candleLows = candles.map { it.low } + alertPriceTargets
    val candleHighs = candles.map { it.high } + alertPriceTargets
    val minPrice = candleLows.minOrNull() ?: 0.0
    val maxPrice = candleHighs.maxOrNull() ?: 1.0
    val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0

    Box(
        modifier = modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 20.dp.toPx()
            val candleCount = candles.size
            val candleWidth = (width / candleCount) * 0.7f
            val spacing = width / candleCount

            // Draw grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = (height / gridLines) * i
                drawLine(
                    color = DarkBorder,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw candles
            candles.forEachIndexed { index, candle ->
                val centerX = index * spacing + (spacing / 2f)
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) BullishGreen else BearishRed

                val openY = height - ((candle.open - minPrice) / priceRange * height).toFloat()
                val closeY = height - ((candle.close - minPrice) / priceRange * height).toFloat()
                val highY = height - ((candle.high - minPrice) / priceRange * height).toFloat()
                val lowY = height - ((candle.low - minPrice) / priceRange * height).toFloat()

                // High-Low Wick
                drawLine(
                    color = color,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 2f
                )

                // Body
                val topY = minOf(openY, closeY)
                val bodyHeight = maxOf(Math.abs(closeY - openY), 2f)
                drawRect(
                    color = color,
                    topLeft = Offset(centerX - (candleWidth / 2f), topY),
                    size = Size(candleWidth, bodyHeight)
                )
            }

            // Draw active alert target price dashed lines
            alertPriceTargets.forEach { alertTarget ->
                val alertY = height - ((alertTarget - minPrice) / priceRange * height).toFloat()
                if (alertY in 0f..height) {
                    drawLine(
                        color = AccentGold,
                        start = Offset(0f, alertY),
                        end = Offset(width, alertY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }
            }
        }

        // Overlay with High/Low and Alert Target note
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(DarkBackground.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("High: \$${String.format("%.2f", maxPrice)}", color = BullishGreen, fontSize = 10.sp)
            Text("Low: \$${String.format("%.2f", minPrice)}", color = BearishRed, fontSize = 10.sp)
            if (alertPriceTargets.isNotEmpty()) {
                Text("🎯 Alert: \$${String.format("%.2f", alertPriceTargets.first())}", color = AccentGold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SparklineView(
    data: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    val min = data.minOrNull() ?: 0.0
    val max = data.maxOrNull() ?: 1.0
    val range = if (max - min > 0) max - min else 1.0
    val color = if (isPositive) BullishGreen else BearishRed

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        val path = Path()

        data.forEachIndexed { i, price ->
            val x = i * stepX
            val y = height - ((price - min) / range * height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )
    }
}
