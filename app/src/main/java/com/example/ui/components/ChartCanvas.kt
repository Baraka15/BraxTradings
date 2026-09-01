package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.CandleStick
import com.example.data.market.TechnicalIndicators
import com.example.ui.IndicatorToggles
import com.example.ui.SubIndicator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveCandlestickChart(
    candles: List<CandleStick>,
    indicatorToggles: IndicatorToggles,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Text("Connecting to live tick feed...", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    var touchIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val selectedCandle = touchIndex?.let { idx ->
        if (idx in candles.indices) candles[idx] else null
    } ?: candles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
    ) {
        // Inspection HUD bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCandle != null) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(selectedCandle.timestamp))
                val isBull = selectedCandle.close >= selectedCandle.open
                val candleColor = if (isBull) BullGreen else BearRed

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(timeStr, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("O: ${"%.2f".format(selectedCandle.open)}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("H: ${"%.2f".format(selectedCandle.high)}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("L: ${"%.2f".format(selectedCandle.low)}", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("C: ${"%.2f".format(selectedCandle.close)}", color = candleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text("Vol: ${selectedCandle.volume.toInt()}", color = M3Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        // Main Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (indicatorToggles.subIndicator == SubIndicator.NONE) 220.dp else 170.dp)
                .background(SurfaceCard)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles.size) {
                        detectTapGestures(
                            onPress = { offset ->
                                val slotWidth = size.width / max(1, candles.size)
                                val idx = (offset.x / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                                touchIndex = idx
                                tryAwaitRelease()
                                touchIndex = null
                            }
                        )
                    }
                    .pointerInput(candles.size) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val slotWidth = size.width / max(1, candles.size)
                                val idx = (offset.x / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                                touchIndex = idx
                            },
                            onDragEnd = { touchIndex = null },
                            onDragCancel = { touchIndex = null },
                            onDrag = { change, _ ->
                                val slotWidth = size.width / max(1, candles.size)
                                val idx = (change.position.x / slotWidth).toInt().coerceIn(0, candles.lastIndex)
                                touchIndex = idx
                            }
                        )
                    }
            ) {
                drawMainChart(
                    candles = candles,
                    indicatorToggles = indicatorToggles,
                    touchIndex = touchIndex,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Sub Indicator Pane (RSI or MACD)
        if (indicatorToggles.subIndicator != SubIndicator.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(SurfaceElevated)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSubIndicator(
                        candles = candles,
                        subIndicator = indicatorToggles.subIndicator,
                        touchIndex = touchIndex,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawMainChart(
    candles: List<CandleStick>,
    indicatorToggles: IndicatorToggles,
    touchIndex: Int?,
    textMeasurer: TextMeasurer
) {
    if (candles.isEmpty()) return

    val candleCount = candles.size
    val minPrice = candles.minOf { it.low } * 0.9995
    val maxPrice = candles.maxOf { it.high } * 1.0005
    val priceRange = max(0.01, maxPrice - minPrice)

    val maxVolume = candles.maxOf { it.volume }.coerceAtLeast(1.0)

    val chartWidth = size.width - 55.dp.toPx() // Reserve right margin for price scale
    val chartHeight = size.height
    val volumeHeight = chartHeight * 0.22f

    val slotWidth = chartWidth / candleCount
    val candleBodyWidth = max(2f, slotWidth * 0.65f)

    fun priceToY(price: Double): Float {
        val normalized = ((maxPrice - price) / priceRange).toFloat()
        return normalized * (chartHeight - 20.dp.toPx()) + 10.dp.toPx()
    }

    // Helper for safe text drawing without triggering negative constraint crashes
    fun safeDraw(text: String, x: Float, y: Float, style: TextStyle) {
        if (size.width <= 0f || size.height <= 0f) return
        val layout = textMeasurer.measure(text = text, style = style)
        val safeX = x.coerceIn(0f, max(0f, size.width - layout.size.width))
        val safeY = y.coerceIn(0f, max(0f, size.height - layout.size.height))
        drawText(textLayoutResult = layout, topLeft = Offset(safeX, safeY))
    }

    // 1. Draw horizontal price grid lines and labels
    val gridCount = 4
    for (i in 0..gridCount) {
        val y = (chartHeight / gridCount) * i
        val priceAtY = maxPrice - (priceRange * (y / chartHeight))

        drawLine(
            color = ChartGridLine,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        safeDraw(
            text = "$%.2f".format(priceAtY),
            x = chartWidth + 4.dp.toPx(),
            y = y - 7.dp.toPx(),
            style = TextStyle(color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
    }

    // 2. Draw Volume Bars at bottom
    for (i in candles.indices) {
        val c = candles[i]
        val x = (i * slotWidth) + (slotWidth / 2f)
        val volBarHeight = (c.volume / maxVolume).toFloat() * volumeHeight
        val volTop = chartHeight - volBarHeight
        val volColor = if (c.isBullish) BullGreen.copy(alpha = 0.35f) else BearRed.copy(alpha = 0.35f)

        drawRect(
            color = volColor,
            topLeft = Offset(x - (candleBodyWidth / 2f), volTop),
            size = Size(candleBodyWidth, volBarHeight)
        )
    }

    // 3. Draw Bollinger Bands Overlay if enabled
    if (indicatorToggles.showBollinger) {
        val closes = candles.map { it.close }
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()

        for (i in candles.indices) {
            val subCloses = closes.subList(0, i + 1)
            val (upper, _, lower) = TechnicalIndicators.calculateBollingerBands(subCloses)
            val x = (i * slotWidth) + (slotWidth / 2f)
            val upperY = priceToY(upper)
            val lowerY = priceToY(lower)

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

        for (i in candles.indices.reversed()) {
            val subCloses = closes.subList(0, i + 1)
            val (_, _, lower) = TechnicalIndicators.calculateBollingerBands(subCloses)
            val x = (i * slotWidth) + (slotWidth / 2f)
            val lowerY = priceToY(lower)
            fillPath.lineTo(x, lowerY)
        }
        fillPath.close()

        drawPath(fillPath, color = BollingerFill)
        drawPath(upperPath, color = M3Primary.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
        drawPath(lowerPath, color = M3Primary.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
    }

    // 4. Draw EMA 9 & EMA 21 Overlays if enabled
    if (indicatorToggles.showEMA) {
        val closes = candles.map { it.close }
        val ema9Path = Path()
        val ema21Path = Path()

        for (i in candles.indices) {
            val subCloses = closes.subList(0, i + 1)
            val ema9 = TechnicalIndicators.calculateEMA(subCloses, 9)
            val ema21 = TechnicalIndicators.calculateEMA(subCloses, 21)
            val x = (i * slotWidth) + (slotWidth / 2f)

            if (i == 0) {
                ema9Path.moveTo(x, priceToY(ema9))
                ema21Path.moveTo(x, priceToY(ema21))
            } else {
                ema9Path.lineTo(x, priceToY(ema9))
                ema21Path.lineTo(x, priceToY(ema21))
            }
        }

        drawPath(ema9Path, color = CryptoGold, style = Stroke(width = 1.5f))
        drawPath(ema21Path, color = NeutralPurple, style = Stroke(width = 1.5f))
    }

    // 5. Draw VWAP line if enabled
    if (indicatorToggles.showVWAP) {
        val vwapPath = Path()
        for (i in candles.indices) {
            val subCandles = candles.subList(0, i + 1)
            val vwap = TechnicalIndicators.calculateVWAP(subCandles)
            val x = (i * slotWidth) + (slotWidth / 2f)
            val y = priceToY(vwap)

            if (i == 0) vwapPath.moveTo(x, y) else vwapPath.lineTo(x, y)
        }
        drawPath(vwapPath, color = M3Primary, style = Stroke(width = 1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))))
    }

    // 6. Draw Candlesticks (Wicks + Bodies)
    for (i in candles.indices) {
        val c = candles[i]
        val x = (i * slotWidth) + (slotWidth / 2f)
        val candleColor = if (c.isBullish) BullGreen else BearRed

        val highY = priceToY(c.high)
        val lowY = priceToY(c.low)
        val openY = priceToY(c.open)
        val closeY = priceToY(c.close)

        // Wick
        drawLine(
            color = candleColor,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.5f
        )

        // Body
        val bodyTop = min(openY, closeY)
        val bodyHeight = max(2f, kotlin.math.abs(openY - closeY))

        drawRect(
            color = candleColor,
            topLeft = Offset(x - (candleBodyWidth / 2f), bodyTop),
            size = Size(candleBodyWidth, bodyHeight)
        )
    }

    // 7. Touch Crosshair
    if (touchIndex != null && touchIndex in candles.indices) {
        val c = candles[touchIndex]
        val x = (touchIndex * slotWidth) + (slotWidth / 2f)
        val y = priceToY(c.close)

        drawLine(
            color = CrosshairColor,
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        drawLine(
            color = CrosshairColor,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        drawRect(
            color = M3PrimaryContainer,
            topLeft = Offset(chartWidth + 2.dp.toPx(), y - 9.dp.toPx()),
            size = Size(50.dp.toPx(), 18.dp.toPx())
        )
        safeDraw(
            text = "$%.2f".format(c.close),
            x = chartWidth + 4.dp.toPx(),
            y = y - 7.dp.toPx(),
            style = TextStyle(color = M3OnPrimaryContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
    }
}

private fun DrawScope.drawSubIndicator(
    candles: List<CandleStick>,
    subIndicator: SubIndicator,
    touchIndex: Int?,
    textMeasurer: TextMeasurer
) {
    if (candles.isEmpty() || size.width <= 10f || size.height <= 10f) return

    val candleCount = candles.size
    val chartWidth = max(10f, size.width - 55.dp.toPx())
    val chartHeight = max(10f, size.height)
    val slotWidth = chartWidth / max(1, candleCount)
    val closes = candles.map { it.close }

    fun safeDraw(text: String, x: Float, y: Float, style: TextStyle) {
        if (size.width <= 0f || size.height <= 0f) return
        val layout = textMeasurer.measure(text = text, style = style)
        val safeX = x.coerceIn(0f, max(0f, size.width - layout.size.width))
        val safeY = y.coerceIn(0f, max(0f, size.height - layout.size.height))
        drawText(textLayoutResult = layout, topLeft = Offset(safeX, safeY))
    }

    if (subIndicator == SubIndicator.RSI) {
        val y70 = chartHeight * 0.3f
        val y30 = chartHeight * 0.7f

        drawLine(
            color = BearRed.copy(alpha = 0.4f),
            start = Offset(0f, y70),
            end = Offset(chartWidth, y70),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        safeDraw(
            text = "70 OB",
            x = chartWidth + 4.dp.toPx(),
            y = y70 - 6.dp.toPx(),
            style = TextStyle(color = BearRed, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        )

        drawLine(
            color = BullGreen.copy(alpha = 0.4f),
            start = Offset(0f, y30),
            end = Offset(chartWidth, y30),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )
        safeDraw(
            text = "30 OS",
            x = chartWidth + 4.dp.toPx(),
            y = y30 - 6.dp.toPx(),
            style = TextStyle(color = BullGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        )

        val rsiPath = Path()
        for (i in candles.indices) {
            val sub = closes.subList(0, i + 1)
            val rsi = TechnicalIndicators.calculateRSI(sub)
            val x = (i * slotWidth) + (slotWidth / 2f)
            val y = ((100.0 - rsi) / 100.0).toFloat() * (chartHeight - 10.dp.toPx()) + 5.dp.toPx()

            if (i == 0) rsiPath.moveTo(x, y) else rsiPath.lineTo(x, y)
        }
        drawPath(rsiPath, color = M3Primary, style = Stroke(width = 1.8f))

        val currentRsi = TechnicalIndicators.calculateRSI(closes)
        safeDraw(
            text = "RSI (14): ${"%.1f".format(currentRsi)}",
            x = 8.dp.toPx(),
            y = 4.dp.toPx(),
            style = TextStyle(color = M3Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
    } else if (subIndicator == SubIndicator.MACD) {
        val (currentMacd, currentSig, currentHist) = TechnicalIndicators.calculateMACD(closes)
        val macdPath = Path()
        val signalPath = Path()

        val midY = chartHeight / 2f
        drawLine(
            color = ChartGridLine,
            start = Offset(0f, midY),
            end = Offset(chartWidth, midY),
            strokeWidth = 1f
        )

        for (i in candles.indices) {
            val sub = closes.subList(0, i + 1)
            val (m, s, h) = TechnicalIndicators.calculateMACD(sub)
            val x = (i * slotWidth) + (slotWidth / 2f)

            val mY = midY - (m * 40f).toFloat().coerceIn(-midY + 5f, midY - 5f)
            val sY = midY - (s * 40f).toFloat().coerceIn(-midY + 5f, midY - 5f)

            if (i == 0) {
                macdPath.moveTo(x, mY)
                signalPath.moveTo(x, sY)
            } else {
                macdPath.lineTo(x, mY)
                signalPath.lineTo(x, sY)
            }

            // Hist bar
            val histHeight = (h * 40f).toFloat().coerceIn(-midY + 5f, midY - 5f)
            val histColor = if (h >= 0) BullGreen.copy(alpha = 0.5f) else BearRed.copy(alpha = 0.5f)
            drawRect(
                color = histColor,
                topLeft = Offset(x - (slotWidth * 0.25f), if (h >= 0) midY - histHeight else midY),
                size = Size(slotWidth * 0.5f, kotlin.math.abs(histHeight))
            )
        }

        drawPath(macdPath, color = M3Primary, style = Stroke(width = 1.5f))
        drawPath(signalPath, color = CryptoGold, style = Stroke(width = 1.5f))

        safeDraw(
            text = "MACD: ${"%.2f".format(currentMacd)} | Sig: ${"%.2f".format(currentSig)}",
            x = 8.dp.toPx(),
            y = 4.dp.toPx(),
            style = TextStyle(color = M3Primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
    }
}
