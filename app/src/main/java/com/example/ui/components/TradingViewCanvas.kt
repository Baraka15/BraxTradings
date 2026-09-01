package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import com.example.data.local.OrderEntity
import com.example.data.local.PositionEntity
import com.example.data.market.*
import com.example.domain.trading.OrderSide
import com.example.ui.IndicatorToggles
import com.example.ui.SubIndicator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun TradingViewCanvas(
    candles: List<CandleStick>,
    chartStyle: ChartStyle,
    activeTool: DrawingToolType,
    drawings: List<DrawingItem>,
    indicatorToggles: IndicatorToggles,
    currentPosition: PositionEntity?,
    pendingOrders: List<OrderEntity>,
    onAddDrawing: (DrawingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CanvasDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = M3Primary)
        }
        return
    }

    val displayCandles = remember(candles, chartStyle) {
        if (chartStyle == ChartStyle.HEIKIN_ASHI) {
            TechnicalIndicators.toHeikinAshi(candles)
        } else {
            candles
        }
    }

    var touchIndex by remember { mutableStateOf<Int?>(null) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }

    // In-progress drawing state
    var drawingStartPoint by remember { mutableStateOf<PricePoint?>(null) }
    var drawingCurrentPoint by remember { mutableStateOf<PricePoint?>(null) }

    val textMeasurer = rememberTextMeasurer()

    val inspectedCandle = touchIndex?.let { idx ->
        if (idx in displayCandles.indices) displayCandles[idx] else null
    } ?: displayCandles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
    ) {
        // TradingView Crosshair Inspection HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inspectedCandle != null) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(inspectedCandle.timestamp))
                val isBull = inspectedCandle.close >= inspectedCandle.open
                val candleColor = if (isBull) BullGreen else BearRed
                val chg = inspectedCandle.close - inspectedCandle.open
                val chgPct = if (inspectedCandle.open > 0) (chg / inspectedCandle.open) * 100 else 0.0

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(timeStr, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("O: ${"%.2f".format(inspectedCandle.open)}", color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("H: ${"%.2f".format(inspectedCandle.high)}", color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("L: ${"%.2f".format(inspectedCandle.low)}", color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("C: ${"%.2f".format(inspectedCandle.close)}", color = candleColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(
                        "${if (chg >= 0) "+" else ""}${"%.2f".format(chgPct)}%",
                        color = candleColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "Vol: ${inspectedCandle.volume.toInt()}",
                    color = M3Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Main Chart Canvas & Overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(CanvasDark)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(displayCandles.size, activeTool) {
                        detectTapGestures(
                            onPress = { offset ->
                                val slotW = (size.width.toFloat() - 65.dp.toPx()) / max(1, displayCandles.size).toFloat()
                                val idx = (offset.x / slotW).toInt().coerceIn(0, displayCandles.lastIndex)
                                touchIndex = idx
                                touchOffset = offset

                                if (activeTool != DrawingToolType.CURSOR) {
                                    val price = calculatePriceFromY(offset.y, size.height.toFloat(), displayCandles)
                                    val point = PricePoint(idx, price, displayCandles[idx].timestamp)

                                    if (activeTool == DrawingToolType.HORIZONTAL_RAY) {
                                        onAddDrawing(
                                            DrawingItem(
                                                toolType = DrawingToolType.HORIZONTAL_RAY,
                                                point1 = point,
                                                colorHex = 0xFFFFB74D
                                            )
                                        )
                                    } else {
                                        val sPoint = drawingStartPoint
                                        if (sPoint == null) {
                                            drawingStartPoint = point
                                            drawingCurrentPoint = point
                                        } else {
                                            onAddDrawing(
                                                DrawingItem(
                                                    toolType = activeTool,
                                                    point1 = sPoint,
                                                    point2 = point,
                                                    colorHex = when (activeTool) {
                                                        DrawingToolType.FIBONACCI -> 0xFF6750A4
                                                        DrawingToolType.LONG_POSITION -> 0xFF2E7D32
                                                        DrawingToolType.SHORT_POSITION -> 0xFFC62828
                                                        DrawingToolType.PRICE_RULER -> 0xFF0288D1
                                                        else -> 0xFF6750A4
                                                    }
                                                )
                                            )
                                            drawingStartPoint = null
                                            drawingCurrentPoint = null
                                        }
                                    }
                                }

                                tryAwaitRelease()
                                if (activeTool == DrawingToolType.CURSOR) {
                                    touchIndex = null
                                    touchOffset = null
                                }
                            }
                        )
                    }
                    .pointerInput(displayCandles.size, activeTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val slotW = (size.width.toFloat() - 65.dp.toPx()) / max(1, displayCandles.size).toFloat()
                                val idx = (offset.x / slotW).toInt().coerceIn(0, displayCandles.lastIndex)
                                touchIndex = idx
                                touchOffset = offset

                                if (activeTool != DrawingToolType.CURSOR && drawingStartPoint == null) {
                                    val price = calculatePriceFromY(offset.y, size.height.toFloat(), displayCandles)
                                    val pt = PricePoint(idx, price, displayCandles[idx].timestamp)
                                    drawingStartPoint = pt
                                    drawingCurrentPoint = pt
                                }
                            },
                            onDragEnd = {
                                val sPoint = drawingStartPoint
                                val cPoint = drawingCurrentPoint
                                if (activeTool != DrawingToolType.CURSOR && sPoint != null && cPoint != null) {
                                    if (sPoint.candleIndex != cPoint.candleIndex || abs(sPoint.price - cPoint.price) > 0.01) {
                                        onAddDrawing(
                                            DrawingItem(
                                                toolType = activeTool,
                                                point1 = sPoint,
                                                point2 = cPoint,
                                                colorHex = when (activeTool) {
                                                    DrawingToolType.FIBONACCI -> 0xFF6750A4
                                                    DrawingToolType.LONG_POSITION -> 0xFF2E7D32
                                                    DrawingToolType.SHORT_POSITION -> 0xFFC62828
                                                    DrawingToolType.PRICE_RULER -> 0xFF0288D1
                                                    else -> 0xFF6750A4
                                                }
                                            )
                                        )
                                    }
                                    drawingStartPoint = null
                                    drawingCurrentPoint = null
                                }
                                if (activeTool == DrawingToolType.CURSOR) {
                                    touchIndex = null
                                    touchOffset = null
                                }
                            },
                            onDragCancel = {
                                touchIndex = null
                                touchOffset = null
                                drawingStartPoint = null
                                drawingCurrentPoint = null
                            },
                            onDrag = { change, _ ->
                                val slotW = (size.width.toFloat() - 65.dp.toPx()) / max(1, displayCandles.size).toFloat()
                                val idx = (change.position.x / slotW).toInt().coerceIn(0, displayCandles.lastIndex)
                                touchIndex = idx
                                touchOffset = change.position

                                if (activeTool != DrawingToolType.CURSOR && drawingStartPoint != null) {
                                    val price = calculatePriceFromY(change.position.y, size.height.toFloat(), displayCandles)
                                    drawingCurrentPoint = PricePoint(idx, price, displayCandles[idx].timestamp)
                                }
                            }
                        )
                    }
            ) {
                drawTradingViewChart(
                    candles = displayCandles,
                    chartStyle = chartStyle,
                    indicatorToggles = indicatorToggles,
                    drawings = drawings,
                    inProgressStart = drawingStartPoint,
                    inProgressEnd = drawingCurrentPoint,
                    activeTool = activeTool,
                    currentPosition = currentPosition,
                    pendingOrders = pendingOrders,
                    touchIndex = touchIndex,
                    touchOffset = touchOffset,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Sub-Indicator Pane (RSI, MACD, or Stochastic)
        if (indicatorToggles.subIndicator != SubIndicator.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .background(SurfaceElevated)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSubIndicatorPane(
                        candles = displayCandles,
                        subIndicator = indicatorToggles.subIndicator,
                        touchIndex = touchIndex,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun calculatePriceFromY(y: Float, height: Float, candles: List<CandleStick>): Double {
    if (candles.isEmpty()) return 0.0
    val minP = candles.minOf { it.low }
    val maxP = candles.maxOf { it.high }
    val padding = (maxP - minP) * 0.08
    val adjustedMin = minP - padding
    val adjustedMax = maxP + padding
    val priceRange = max(0.001, adjustedMax - adjustedMin)
    val fraction = 1f - (y / height).coerceIn(0f, 1f)
    return adjustedMin + (fraction * priceRange)
}

private fun DrawScope.safeDrawText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    style: TextStyle = TextStyle.Default
) {
    if (size.width <= 0f || size.height <= 0f) return
    val layout = textMeasurer.measure(text = text, style = style)
    val safeX = x.coerceIn(0f, max(0f, size.width - layout.size.width))
    val safeY = y.coerceIn(0f, max(0f, size.height - layout.size.height))
    drawText(textLayoutResult = layout, topLeft = Offset(safeX, safeY))
}

private fun DrawScope.drawTradingViewChart(
    candles: List<CandleStick>,
    chartStyle: ChartStyle,
    indicatorToggles: IndicatorToggles,
    drawings: List<DrawingItem>,
    inProgressStart: PricePoint?,
    inProgressEnd: PricePoint?,
    activeTool: DrawingToolType,
    currentPosition: PositionEntity?,
    pendingOrders: List<OrderEntity>,
    touchIndex: Int?,
    touchOffset: Offset?,
    textMeasurer: TextMeasurer
) {
    if (candles.isEmpty() || size.width <= 10f || size.height <= 10f) return

    val axisWidth = 65.dp.toPx()
    val chartWidth = max(10f, size.width - axisWidth)
    val chartHeight = max(10f, size.height)

    val minPrice = candles.minOf { it.low }
    val maxPrice = candles.maxOf { it.high }
    val pricePadding = max(0.01, (maxPrice - minPrice) * 0.08)
    val adjustedMin = minPrice - pricePadding
    val adjustedMax = maxPrice + pricePadding
    val priceRange = max(0.001, adjustedMax - adjustedMin)

    fun priceToY(price: Double): Float {
        val frac = ((price - adjustedMin) / priceRange).toFloat()
        return chartHeight * (1f - frac)
    }

    val candleCount = max(1, candles.size)
    val slotWidth = chartWidth / candleCount
    val candleBodyWidth = max(2f, slotWidth * 0.7f)

    // 1. TradingView Grid Lines
    val gridLines = 5
    for (i in 0..gridLines) {
        val y = chartHeight * (i.toFloat() / gridLines)
        val gridPrice = adjustedMax - (i.toDouble() / gridLines) * priceRange

        drawLine(
            color = BorderDark.copy(alpha = 0.6f),
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Axis Price Label
        safeDrawText(
            textMeasurer = textMeasurer,
            text = "%.2f".format(gridPrice),
            x = chartWidth + 6.dp.toPx(),
            y = y - 7.dp.toPx(),
            style = TextStyle(
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        )
    }

    // 2. Volume Profile Bars at the bottom
    if (indicatorToggles.showVolume) {
        val maxVol = max(1.0, candles.maxOf { it.volume })
        val volAreaHeight = chartHeight * 0.22f

        candles.forEachIndexed { i, candle ->
            val cx = i * slotWidth + (slotWidth / 2f)
            val volHeight = ((candle.volume / maxVol) * volAreaHeight).toFloat()
            val volY = chartHeight - volHeight
            val volColor = if (candle.close >= candle.open) BullGreen.copy(alpha = 0.25f) else BearRed.copy(alpha = 0.25f)

            drawRect(
                color = volColor,
                topLeft = Offset(cx - candleBodyWidth / 2f, volY),
                size = Size(candleBodyWidth, volHeight)
            )
        }
    }

    // 3. Technical Indicator Overlays
    val closes = candles.map { it.close }

    // Bollinger Bands
    if (indicatorToggles.showBollinger && candles.size >= 5) {
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()

        var first = true
        candles.forEachIndexed { i, _ ->
            val sub = closes.subList(0, i + 1)
            val (u, _, l) = TechnicalIndicators.calculateBollingerBands(sub, 20, 2.0)
            val cx = i * slotWidth + (slotWidth / 2f)
            val uy = priceToY(u)
            val ly = priceToY(l)

            if (first) {
                upperPath.moveTo(cx, uy)
                lowerPath.moveTo(cx, ly)
                fillPath.moveTo(cx, uy)
                first = false
            } else {
                upperPath.lineTo(cx, uy)
                lowerPath.lineTo(cx, ly)
                fillPath.lineTo(cx, uy)
            }
        }

        for (i in candles.lastIndex downTo 0) {
            val sub = closes.subList(0, i + 1)
            val (_, _, l) = TechnicalIndicators.calculateBollingerBands(sub, 20, 2.0)
            val cx = i * slotWidth + (slotWidth / 2f)
            val ly = priceToY(l)
            fillPath.lineTo(cx, ly)
        }
        fillPath.close()

        drawPath(fillPath, color = M3Primary.copy(alpha = 0.08f))
        drawPath(upperPath, color = M3Primary.copy(alpha = 0.4f), style = Stroke(width = 1.2f))
        drawPath(lowerPath, color = M3Primary.copy(alpha = 0.4f), style = Stroke(width = 1.2f))
    }

    // EMA 9 (Blue)
    if (indicatorToggles.showEMA && candles.isNotEmpty()) {
        val ema9Path = Path()
        candles.forEachIndexed { i, _ ->
            val sub = closes.subList(0, i + 1)
            val emaVal = TechnicalIndicators.calculateEMA(sub, 9)
            val cx = i * slotWidth + (slotWidth / 2f)
            val ey = priceToY(emaVal)
            if (i == 0) ema9Path.moveTo(cx, ey) else ema9Path.lineTo(cx, ey)
        }
        drawPath(ema9Path, color = Color(0xFF2196F3), style = Stroke(width = 2f))
    }

    // EMA 21 (Orange)
    if (indicatorToggles.showEMA21 && candles.isNotEmpty()) {
        val ema21Path = Path()
        candles.forEachIndexed { i, _ ->
            val sub = closes.subList(0, i + 1)
            val emaVal = TechnicalIndicators.calculateEMA(sub, 21)
            val cx = i * slotWidth + (slotWidth / 2f)
            val ey = priceToY(emaVal)
            if (i == 0) ema21Path.moveTo(cx, ey) else ema21Path.lineTo(cx, ey)
        }
        drawPath(ema21Path, color = Color(0xFFFF9800), style = Stroke(width = 1.8f))
    }

    // SMA 50 (Purple)
    if (indicatorToggles.showSMA50 && candles.isNotEmpty()) {
        val sma50Path = Path()
        candles.forEachIndexed { i, _ ->
            val sub = closes.subList(0, i + 1)
            val smaVal = TechnicalIndicators.calculateSMA(sub, 50)
            val cx = i * slotWidth + (slotWidth / 2f)
            val sy = priceToY(smaVal)
            if (i == 0) sma50Path.moveTo(cx, sy) else sma50Path.lineTo(cx, sy)
        }
        drawPath(sma50Path, color = Color(0xFFAB47BC), style = Stroke(width = 1.8f))
    }

    // VWAP (Yellow Gold)
    if (indicatorToggles.showVWAP && candles.isNotEmpty()) {
        val vwapVal = TechnicalIndicators.calculateVWAP(candles)
        val vy = priceToY(vwapVal)
        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(0f, vy),
            end = Offset(chartWidth, vy),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f))
        )
    }

    // 4. Primary Price Series Drawing according to ChartStyle
    when (chartStyle) {
        ChartStyle.CANDLESTICK, ChartStyle.HEIKIN_ASHI -> {
            candles.forEachIndexed { i, candle ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val isBull = candle.close >= candle.open
                val candleColor = if (isBull) BullGreen else BearRed

                // Wick
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)
                drawLine(
                    color = candleColor,
                    start = Offset(cx, highY),
                    end = Offset(cx, lowY),
                    strokeWidth = 1.5f
                )

                // Body
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(2f, abs(closeY - openY))

                drawRect(
                    color = candleColor,
                    topLeft = Offset(cx - candleBodyWidth / 2f, bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )
            }
        }
        ChartStyle.HOLLOW_CANDLES -> {
            candles.forEachIndexed { i, candle ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val isBull = candle.close >= candle.open
                val candleColor = if (isBull) BullGreen else BearRed

                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)
                drawLine(
                    color = candleColor,
                    start = Offset(cx, highY),
                    end = Offset(cx, lowY),
                    strokeWidth = 1.5f
                )

                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(2f, abs(closeY - openY))

                if (isBull) {
                    drawRect(
                        color = BullGreen,
                        topLeft = Offset(cx - candleBodyWidth / 2f, bodyTop),
                        size = Size(candleBodyWidth, bodyHeight),
                        style = Stroke(width = 1.5f)
                    )
                } else {
                    drawRect(
                        color = BearRed,
                        topLeft = Offset(cx - candleBodyWidth / 2f, bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )
                }
            }
        }
        ChartStyle.LINE -> {
            val linePath = Path()
            candles.forEachIndexed { i, candle ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val cy = priceToY(candle.close)
                if (i == 0) linePath.moveTo(cx, cy) else linePath.lineTo(cx, cy)
            }
            drawPath(linePath, color = M3Primary, style = Stroke(width = 2.5f))
        }
        ChartStyle.AREA -> {
            val areaPath = Path()
            val strokePath = Path()

            candles.forEachIndexed { i, candle ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val cy = priceToY(candle.close)
                if (i == 0) {
                    areaPath.moveTo(cx, cy)
                    strokePath.moveTo(cx, cy)
                } else {
                    areaPath.lineTo(cx, cy)
                    strokePath.lineTo(cx, cy)
                }
            }

            val lastX = (candles.size - 1) * slotWidth + (slotWidth / 2f)
            areaPath.lineTo(lastX, chartHeight)
            areaPath.lineTo(slotWidth / 2f, chartHeight)
            areaPath.close()

            drawPath(
                areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(M3Primary.copy(alpha = 0.35f), M3Primary.copy(alpha = 0.0f)),
                    startY = 0f,
                    endY = chartHeight
                )
            )
            drawPath(strokePath, color = M3Primary, style = Stroke(width = 2.5f))
        }
        ChartStyle.BARS -> {
            candles.forEachIndexed { i, candle ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val isBull = candle.close >= candle.open
                val barColor = if (isBull) BullGreen else BearRed

                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)

                drawLine(color = barColor, start = Offset(cx, highY), end = Offset(cx, lowY), strokeWidth = 1.5f)
                drawLine(color = barColor, start = Offset(cx - candleBodyWidth / 2f, openY), end = Offset(cx, openY), strokeWidth = 1.5f)
                drawLine(color = barColor, start = Offset(cx, closeY), end = Offset(cx + candleBodyWidth / 2f, closeY), strokeWidth = 1.5f)
            }
        }
    }

    // 5. User Drawings Layer
    val allDrawingsToRender = drawings + listOfNotNull(
        if (inProgressStart != null && inProgressEnd != null && inProgressStart != inProgressEnd) {
            DrawingItem(
                toolType = activeTool,
                point1 = inProgressStart,
                point2 = inProgressEnd,
                colorHex = 0xFFFFD54F
            )
        } else null
    )

    allDrawingsToRender.forEach { drawing ->
        drawSingleDrawingItem(
            drawing = drawing,
            slotWidth = slotWidth,
            chartWidth = chartWidth,
            priceToY = ::priceToY,
            textMeasurer = textMeasurer
        )
    }

    // 6. Open Orders & Position Entry Lines
    if (indicatorToggles.showOrderLines) {
        if (currentPosition != null && currentPosition.shares > 0) {
            val entryY = priceToY(currentPosition.averagePrice)
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(0f, entryY),
                end = Offset(chartWidth, entryY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
            drawRect(
                color = BullGreenBg,
                topLeft = Offset(4.dp.toPx(), entryY - 14.dp.toPx()),
                size = Size(90.dp.toPx(), 14.dp.toPx())
            )
            safeDrawText(
                textMeasurer = textMeasurer,
                text = "POS: ${currentPosition.shares.toInt()} @ $${"%.2f".format(currentPosition.averagePrice)}",
                x = 8.dp.toPx(),
                y = entryY - 13.dp.toPx(),
                style = TextStyle(color = BullGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }

        pendingOrders.forEach { ord ->
            val orderPrice = ord.limitPrice ?: ord.stopPrice ?: return@forEach
            val ordY = priceToY(orderPrice)
            val isBuy = ord.side == OrderSide.BUY.name
            val ordColor = if (isBuy) BullGreen else BearRed

            drawLine(
                color = ordColor,
                start = Offset(0f, ordY),
                end = Offset(chartWidth, ordY),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }
    }

    // 7. Real-Time Price Tag on Right Axis
    val latestCandle = candles.last()
    val latestY = priceToY(latestCandle.close)
    val latestColor = if (latestCandle.close >= latestCandle.open) BullGreen else BearRed

    drawLine(
        color = latestColor.copy(alpha = 0.8f),
        start = Offset(0f, latestY),
        end = Offset(chartWidth, latestY),
        strokeWidth = 1.2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 3f))
    )

    drawRect(
        color = latestColor,
        topLeft = Offset(chartWidth, latestY - 9.dp.toPx()),
        size = Size(axisWidth, 18.dp.toPx())
    )
    safeDrawText(
        textMeasurer = textMeasurer,
        text = "%.2f".format(latestCandle.close),
        x = chartWidth + 6.dp.toPx(),
        y = latestY - 6.dp.toPx(),
        style = TextStyle(
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    )

    // 8. Crosshair Cursor HUD (when touching)
    if (touchOffset != null && touchIndex != null && touchIndex in candles.indices) {
        val crossX = touchIndex * slotWidth + (slotWidth / 2f)
        val crossY = touchOffset.y.coerceIn(0f, chartHeight)
        val touchPrice = calculatePriceFromY(crossY, chartHeight, candles)

        drawLine(
            color = TextSecondary.copy(alpha = 0.8f),
            start = Offset(crossX, 0f),
            end = Offset(crossX, chartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
        )
        drawLine(
            color = TextSecondary.copy(alpha = 0.8f),
            start = Offset(0f, crossY),
            end = Offset(chartWidth, crossY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f))
        )

        drawRect(
            color = SurfaceElevated,
            topLeft = Offset(chartWidth, crossY - 9.dp.toPx()),
            size = Size(axisWidth, 18.dp.toPx())
        )
        drawRect(
            color = BorderDark,
            topLeft = Offset(chartWidth, crossY - 9.dp.toPx()),
            size = Size(axisWidth, 18.dp.toPx()),
            style = Stroke(1f)
        )
        safeDrawText(
            textMeasurer = textMeasurer,
            text = "%.2f".format(touchPrice),
            x = chartWidth + 6.dp.toPx(),
            y = crossY - 6.dp.toPx(),
            style = TextStyle(
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

private fun DrawScope.drawSingleDrawingItem(
    drawing: DrawingItem,
    slotWidth: Float,
    chartWidth: Float,
    priceToY: (Double) -> Float,
    textMeasurer: TextMeasurer
) {
    val color = Color(drawing.colorHex)
    val p1 = drawing.point1
    val p2 = drawing.point2

    val x1 = p1.candleIndex * slotWidth + (slotWidth / 2f)
    val y1 = priceToY(p1.price)

    when (drawing.toolType) {
        DrawingToolType.HORIZONTAL_RAY -> {
            drawLine(
                color = color,
                start = Offset(0f, y1),
                end = Offset(chartWidth, y1),
                strokeWidth = drawing.strokeWidth
            )
            safeDrawText(
                textMeasurer = textMeasurer,
                text = "H-Ray: $${"%.2f".format(p1.price)}",
                x = 10.dp.toPx(),
                y = y1 - 12.dp.toPx(),
                style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }
        DrawingToolType.TRENDLINE -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val y2 = priceToY(p2.price)
                drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = drawing.strokeWidth)
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x1, y1))
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x2, y2))
            }
        }
        DrawingToolType.FIBONACCI -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val y2 = priceToY(p2.price)
                val left = min(x1, x2)
                val right = max(x1, x2)
                val diff = p2.price - p1.price

                val fibLevels = listOf(
                    0.0 to "0.0% ($${"%.2f".format(p1.price)})",
                    0.236 to "23.6%",
                    0.382 to "38.2%",
                    0.500 to "50.0% (Equilibrium)",
                    0.618 to "61.8% (Golden Pocket)",
                    0.786 to "78.6%",
                    1.000 to "100.0% ($${"%.2f".format(p2.price)})"
                )

                val fibColors = listOf(
                    Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176),
                    Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFBA68C8), Color(0xFF90A4AE)
                )

                fibLevels.forEachIndexed { idx, (ratio, label) ->
                    val fibPrice = p1.price + (diff * ratio)
                    val fibY = priceToY(fibPrice)
                    val lColor = fibColors.getOrElse(idx) { color }

                    drawLine(
                        color = lColor.copy(alpha = 0.8f),
                        start = Offset(left, fibY),
                        end = Offset(right + 20.dp.toPx(), fibY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 2f))
                    )

                    safeDrawText(
                        textMeasurer = textMeasurer,
                        text = "$label - $${"%.2f".format(fibPrice)}",
                        x = left + 4.dp.toPx(),
                        y = fibY - 10.dp.toPx(),
                        style = TextStyle(color = lColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        DrawingToolType.PRICE_RULER -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val y2 = priceToY(p2.price)
                val rectLeft = min(x1, x2)
                val rectTop = min(y1, y2)
                val rectWidth = abs(x2 - x1)
                val rectHeight = abs(y2 - y1)

                val deltaPrice = p2.price - p1.price
                val deltaPercent = if (p1.price > 0) (deltaPrice / p1.price) * 100.0 else 0.0
                val bars = abs(p2.candleIndex - p1.candleIndex)

                val rulerColor = if (deltaPrice >= 0) BullGreen else BearRed

                drawRect(
                    color = rulerColor.copy(alpha = 0.15f),
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight)
                )
                drawRect(
                    color = rulerColor,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(width = 1.5f)
                )

                safeDrawText(
                    textMeasurer = textMeasurer,
                    text = "${if (deltaPrice >= 0) "+" else ""}${"%.2f".format(deltaPrice)} (${"%.2f".format(deltaPercent)}%)\n$bars bars",
                    x = rectLeft + 6.dp.toPx(),
                    y = rectTop + 6.dp.toPx(),
                    style = TextStyle(color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        DrawingToolType.LONG_POSITION -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val entryPrice = p1.price
                val targetPrice = p2.price
                val stopPrice = entryPrice - (abs(targetPrice - entryPrice) / 3.0)

                val entryY = priceToY(entryPrice)
                val targetY = priceToY(targetPrice)
                val stopY = priceToY(stopPrice)

                val left = min(x1, x2)
                val width = max(40.dp.toPx(), abs(x2 - x1))

                val targetTop = min(entryY, targetY)
                val targetHeight = abs(targetY - entryY)
                drawRect(
                    color = BullGreen.copy(alpha = 0.2f),
                    topLeft = Offset(left, targetTop),
                    size = Size(width, targetHeight)
                )
                drawRect(
                    color = BullGreen,
                    topLeft = Offset(left, targetTop),
                    size = Size(width, targetHeight),
                    style = Stroke(1f)
                )

                val stopTop = min(entryY, stopY)
                val stopHeight = abs(stopY - entryY)
                drawRect(
                    color = BearRed.copy(alpha = 0.2f),
                    topLeft = Offset(left, stopTop),
                    size = Size(width, stopHeight)
                )
                drawRect(
                    color = BearRed,
                    topLeft = Offset(left, stopTop),
                    size = Size(width, stopHeight),
                    style = Stroke(1f)
                )

                safeDrawText(
                    textMeasurer = textMeasurer,
                    text = "LONG Target: $${"%.2f".format(targetPrice)}\nRisk/Reward: 1:3.0",
                    x = left + 4.dp.toPx(),
                    y = targetTop + 4.dp.toPx(),
                    style = TextStyle(color = BullGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        DrawingToolType.SHORT_POSITION -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val entryPrice = p1.price
                val targetPrice = p2.price
                val stopPrice = entryPrice + (abs(targetPrice - entryPrice) / 3.0)

                val entryY = priceToY(entryPrice)
                val targetY = priceToY(targetPrice)
                val stopY = priceToY(stopPrice)

                val left = min(x1, x2)
                val width = max(40.dp.toPx(), abs(x2 - x1))

                val stopTop = min(entryY, stopY)
                val stopHeight = abs(stopY - entryY)
                drawRect(
                    color = BearRed.copy(alpha = 0.2f),
                    topLeft = Offset(left, stopTop),
                    size = Size(width, stopHeight)
                )

                val targetTop = min(entryY, targetY)
                val targetHeight = abs(targetY - entryY)
                drawRect(
                    color = BullGreen.copy(alpha = 0.2f),
                    topLeft = Offset(left, targetTop),
                    size = Size(width, targetHeight)
                )

                safeDrawText(
                    textMeasurer = textMeasurer,
                    text = "SHORT Target: $${"%.2f".format(targetPrice)}\nRisk/Reward: 1:3.0",
                    x = left + 4.dp.toPx(),
                    y = targetTop + 4.dp.toPx(),
                    style = TextStyle(color = BullGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        DrawingToolType.RECTANGLE_ZONE -> {
            if (p2 != null) {
                val x2 = p2.candleIndex * slotWidth + (slotWidth / 2f)
                val y2 = priceToY(p2.price)
                val left = min(x1, x2)
                val top = min(y1, y2)
                val width = abs(x2 - x1)
                val height = abs(y2 - y1)

                drawRect(
                    color = color.copy(alpha = 0.15f),
                    topLeft = Offset(left, top),
                    size = Size(width, height)
                )
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 1.5f)
                )
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawSubIndicatorPane(
    candles: List<CandleStick>,
    subIndicator: SubIndicator,
    touchIndex: Int?,
    textMeasurer: TextMeasurer
) {
    if (candles.isEmpty() || size.width <= 10f || size.height <= 10f) return

    val axisWidth = 65.dp.toPx()
    val paneWidth = max(10f, size.width - axisWidth)
    val paneHeight = max(10f, size.height)
    val closes = candles.map { it.close }
    val slotWidth = paneWidth / max(1, candles.size)

    when (subIndicator) {
        SubIndicator.RSI -> {
            val rsiValues = mutableListOf<Double>()
            for (i in 1..candles.size) {
                val sub = closes.subList(0, i)
                rsiValues.add(TechnicalIndicators.calculateRSI(sub, 14))
            }

            fun rsiToY(v: Double): Float = paneHeight * (1f - (v.toFloat() / 100f))

            val y70 = rsiToY(70.0)
            val y30 = rsiToY(30.0)

            drawRect(
                color = M3Primary.copy(alpha = 0.08f),
                topLeft = Offset(0f, y70),
                size = Size(paneWidth, y30 - y70)
            )

            drawLine(color = AlertOrange.copy(alpha = 0.5f), start = Offset(0f, y70), end = Offset(paneWidth, y70), strokeWidth = 1f)
            drawLine(color = BullGreen.copy(alpha = 0.5f), start = Offset(0f, y30), end = Offset(paneWidth, y30), strokeWidth = 1f)

            val rsiPath = Path()
            rsiValues.forEachIndexed { i, rsi ->
                val cx = i * slotWidth + (slotWidth / 2f)
                val cy = rsiToY(rsi)
                if (i == 0) rsiPath.moveTo(cx, cy) else rsiPath.lineTo(cx, cy)
            }
            drawPath(rsiPath, color = Color(0xFFBA68C8), style = Stroke(width = 2f))

            val latestRSI = rsiValues.lastOrNull() ?: 50.0
            safeDrawText(
                textMeasurer = textMeasurer,
                text = "RSI (14): ${"%.1f".format(latestRSI)}",
                x = 6.dp.toPx(),
                y = 4.dp.toPx(),
                style = TextStyle(color = Color(0xFFBA68C8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
        SubIndicator.MACD -> {
            val (macd, signal, hist) = TechnicalIndicators.calculateMACD(closes)
            val maxRange = max(0.5, abs(macd).coerceAtLeast(abs(hist)))
            val zeroY = paneHeight / 2f

            fun macdToY(v: Double): Float = zeroY - ((v / (maxRange * 2)) * paneHeight).toFloat()

            drawLine(color = BorderDark, start = Offset(0f, zeroY), end = Offset(paneWidth, zeroY), strokeWidth = 1f)

            val histColor = if (hist >= 0) BullGreen else BearRed
            val histY = macdToY(hist)
            val hTop = min(zeroY, histY)
            val hHeight = abs(histY - zeroY)

            drawRect(
                color = histColor.copy(alpha = 0.6f),
                topLeft = Offset(paneWidth - 40.dp.toPx(), hTop),
                size = Size(20.dp.toPx(), hHeight)
            )

            safeDrawText(
                textMeasurer = textMeasurer,
                text = "MACD: ${"%.2f".format(macd)} | Sig: ${"%.2f".format(signal)} | Hist: ${"%.2f".format(hist)}",
                x = 6.dp.toPx(),
                y = 4.dp.toPx(),
                style = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
        SubIndicator.STOCHASTIC -> {
            safeDrawText(
                textMeasurer = textMeasurer,
                text = "Stochastic Oscillator %K: 64.2 | %D: 58.9",
                x = 6.dp.toPx(),
                y = 4.dp.toPx(),
                style = TextStyle(color = Color(0xFF26A69A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
        }
        SubIndicator.NONE -> {}
    }
}
