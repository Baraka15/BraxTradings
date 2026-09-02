package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.domain.trading.*
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChartCanvas(
    candles: List<Candle>,
    chartStyle: ChartStyle,
    activeIndicators: Set<TechnicalIndicator>,
    drawingTool: DrawingToolType,
    drawings: List<DrawingShape>,
    onAddDrawing: (DrawingShape) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) return

    val displayCandles = remember(candles, chartStyle) {
        if (chartStyle == ChartStyle.HEIKIN_ASHI) TechnicalAnalysis.computeHeikinAshi(candles) else candles
    }

    var crosshairPos by remember { mutableStateOf<Offset?>(null) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }

    val ema9 = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.EMA_9)) TechnicalAnalysis.calculateEMA(candles, 9) else null
    }
    val ema21 = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.EMA_21)) TechnicalAnalysis.calculateEMA(candles, 21) else null
    }
    val ema50 = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.EMA_50)) TechnicalAnalysis.calculateEMA(candles, 50) else null
    }
    val bb = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.BOLLINGER_BANDS)) TechnicalAnalysis.calculateBollingerBands(candles) else null
    }
    val vwap = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.VWAP)) TechnicalAnalysis.calculateVWAP(candles) else null
    }
    val supertrend = remember(candles, activeIndicators) {
        if (activeIndicators.contains(TechnicalIndicator.SUPERTREND)) TechnicalAnalysis.calculateSupertrend(candles) else null
    }

    val showVolume = activeIndicators.contains(TechnicalIndicator.VOLUME)
    val showRsi = activeIndicators.contains(TechnicalIndicator.RSI)
    val rsiData = remember(candles, showRsi) {
        if (showRsi) TechnicalAnalysis.calculateRSI(candles) else null
    }

    Box(
        modifier = modifier
            .background(CardBg)
            .pointerInput(drawingTool) {
                detectTapGestures(
                    onTap = { offset ->
                        if (drawingTool == DrawingToolType.CROSSHAIR) {
                            crosshairPos = if (crosshairPos != null) null else offset
                        }
                    }
                )
            }
            .pointerInput(drawingTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (drawingTool != DrawingToolType.NONE && drawingTool != DrawingToolType.CROSSHAIR) {
                            dragStart = offset
                            dragCurrent = offset
                        } else if (drawingTool == DrawingToolType.CROSSHAIR) {
                            crosshairPos = offset
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (drawingTool != DrawingToolType.NONE && drawingTool != DrawingToolType.CROSSHAIR) {
                            dragCurrent = change.position
                        } else if (drawingTool == DrawingToolType.CROSSHAIR) {
                            crosshairPos = change.position
                        }
                    },
                    onDragEnd = {
                        if (dragStart != null && dragCurrent != null && drawingTool != DrawingToolType.NONE && drawingTool != DrawingToolType.CROSSHAIR) {
                            val start = dragStart!!
                            val end = dragCurrent!!
                            if (abs(start.x - end.x) > 10 || abs(start.y - end.y) > 10) {
                                onAddDrawing(
                                    DrawingShape(
                                        id = java.util.UUID.randomUUID().toString(),
                                        type = drawingTool,
                                        startX = start.x,
                                        startY = start.y,
                                        endX = end.x,
                                        endY = end.y
                                    )
                                )
                            }
                        }
                        dragStart = null
                        dragCurrent = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height

            val rsiHeight = if (showRsi) totalHeight * 0.22f else 0f
            val mainHeight = totalHeight - rsiHeight
            val volHeight = if (showVolume) mainHeight * 0.20f else 0f
            val candleAreaHeight = mainHeight - volHeight

            val minPrice = (displayCandles.minOfOrNull { it.low } ?: 1.0) * 0.998
            val maxPrice = (displayCandles.maxOfOrNull { it.high } ?: 1.0) * 1.002
            val priceRange = if (maxPrice - minPrice > 0) maxPrice - minPrice else 1.0

            val maxVol = displayCandles.maxOfOrNull { it.volume } ?: 1.0

            val visibleCount = displayCandles.size
            val candleStep = totalWidth / visibleCount.coerceAtLeast(1)
            val candleWidth = max(2f, candleStep * 0.7f)

            // Grid Lines
            val gridCount = 5
            for (i in 0..gridCount) {
                val y = candleAreaHeight * (i.toFloat() / gridCount)
                drawLine(
                    color = ChartGridLine,
                    start = Offset(0f, y),
                    end = Offset(totalWidth, y),
                    strokeWidth = 1f
                )
            }

            // Mountain / Area line path
            if (chartStyle == ChartStyle.MOUNTAIN) {
                val linePath = Path()
                val areaPath = Path()
                areaPath.moveTo(0f, candleAreaHeight)

                displayCandles.forEachIndexed { i, c ->
                    val x = i * candleStep + candleStep / 2f
                    val y = candleAreaHeight - ((c.close - minPrice) / priceRange * candleAreaHeight).toFloat()
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        areaPath.lineTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                        areaPath.lineTo(x, y)
                    }
                }
                val lastX = (displayCandles.size - 1) * candleStep + candleStep / 2f
                areaPath.lineTo(lastX, candleAreaHeight)
                areaPath.close()

                drawPath(areaPath, Color(0x332979FF))
                drawPath(linePath, BlueAccent, style = Stroke(width = 3f))
            } else {
                // Draw Candles
                displayCandles.forEachIndexed { index, c ->
                    val x = index * candleStep + (candleStep - candleWidth) / 2f
                    val centerX = x + candleWidth / 2f

                    val isBull = c.close >= c.open
                    val candleColor = if (isBull) GreenBullish else RedBearish

                    val highY = candleAreaHeight - ((c.high - minPrice) / priceRange * candleAreaHeight).toFloat()
                    val lowY = candleAreaHeight - ((c.low - minPrice) / priceRange * candleAreaHeight).toFloat()
                    val openY = candleAreaHeight - ((c.open - minPrice) / priceRange * candleAreaHeight).toFloat()
                    val closeY = candleAreaHeight - ((c.close - minPrice) / priceRange * candleAreaHeight).toFloat()

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(centerX, highY),
                        end = Offset(centerX, lowY),
                        strokeWidth = 1.5f
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(2f, abs(closeY - openY))

                    if (chartStyle == ChartStyle.HOLLOW_BARS && isBull) {
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x, bodyTop),
                            size = Size(candleWidth, bodyHeight),
                            style = Stroke(width = 2f)
                        )
                    } else {
                        drawRect(
                            color = candleColor,
                            topLeft = Offset(x, bodyTop),
                            size = Size(candleWidth, bodyHeight)
                        )
                    }

                    // Volume Bar
                    if (showVolume) {
                        val volH = ((c.volume / maxVol) * volHeight).toFloat()
                        val volY = mainHeight - volH
                        drawRect(
                            color = if (isBull) Color(0x5500E676) else Color(0x55FF5252),
                            topLeft = Offset(x, volY),
                            size = Size(candleWidth, volH)
                        )
                    }
                }
            }

            // EMA Overlays
            fun drawCurve(values: List<Double?>?, color: Color, strokeWidth: Float = 2f) {
                if (values == null) return
                val p = Path()
                var started = false
                for (i in values.indices) {
                    val v = values[i] ?: continue
                    val x = i * candleStep + candleStep / 2f
                    val y = candleAreaHeight - ((v - minPrice) / priceRange * candleAreaHeight).toFloat()
                    if (!started) {
                        p.moveTo(x, y)
                        started = true
                    } else {
                        p.lineTo(x, y)
                    }
                }
                if (started) {
                    drawPath(p, color, style = Stroke(width = strokeWidth))
                }
            }

            drawCurve(ema9, Color(0xFFFFD600), 2f)
            drawCurve(ema21, CyanAccent, 2.5f)
            drawCurve(ema50, PurpleAccent, 2f)
            drawCurve(vwap, Color(0xFFFF9100), 2.5f)

            // Bollinger Bands
            if (bb != null) {
                drawCurve(bb.upper, Color(0xAA90CAF9), 1.5f)
                drawCurve(bb.middle, Color(0xAA90CAF9), 1f)
                drawCurve(bb.lower, Color(0xAA90CAF9), 1.5f)
            }

            // Supertrend
            if (supertrend != null) {
                for (i in 0 until displayCandles.size - 1) {
                    val st1 = supertrend.supertrend[i] ?: continue
                    val st2 = supertrend.supertrend[i + 1] ?: continue
                    val bull = supertrend.isBullish[i] ?: true
                    val color = if (bull) GreenBullish else RedBearish

                    val x1 = i * candleStep + candleStep / 2f
                    val y1 = candleAreaHeight - ((st1 - minPrice) / priceRange * candleAreaHeight).toFloat()
                    val x2 = (i + 1) * candleStep + candleStep / 2f
                    val y2 = candleAreaHeight - ((st2 - minPrice) / priceRange * candleAreaHeight).toFloat()
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = 3f)
                }
            }

            // RSI Sub-Chart
            if (showRsi && rsiData != null) {
                val rsiTop = mainHeight
                drawLine(Color(0xFF30363D), Offset(0f, rsiTop), Offset(totalWidth, rsiTop), 2f)

                val y70 = rsiTop + rsiHeight * 0.3f
                val y30 = rsiTop + rsiHeight * 0.7f
                drawLine(Color(0x33FF5252), Offset(0f, y70), Offset(totalWidth, y70), 1f)
                drawLine(Color(0x3300E676), Offset(0f, y30), Offset(totalWidth, y30), 1f)

                val rsiPath = Path()
                var rsiStarted = false
                for (i in rsiData.indices) {
                    val r = rsiData[i] ?: continue
                    val x = i * candleStep + candleStep / 2f
                    val y = rsiTop + (1f - (r.toFloat() / 100f)) * rsiHeight
                    if (!rsiStarted) {
                        rsiPath.moveTo(x, y)
                        rsiStarted = true
                    } else {
                        rsiPath.lineTo(x, y)
                    }
                }
                if (rsiStarted) {
                    drawPath(rsiPath, PurpleAccent, style = Stroke(width = 2.5f))
                }
            }

            // Persistent Drawings
            drawings.forEach { shape ->
                drawCustomShape(shape, totalWidth)
            }

            // In-Progress Drawing
            if (dragStart != null && dragCurrent != null) {
                drawCustomShape(
                    DrawingShape(
                        id = "temp",
                        type = drawingTool,
                        startX = dragStart!!.x,
                        startY = dragStart!!.y,
                        endX = dragCurrent!!.x,
                        endY = dragCurrent!!.y
                    ),
                    totalWidth
                )
            }

            // Crosshair
            crosshairPos?.let { pos ->
                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(0f, pos.y),
                    end = Offset(totalWidth, pos.y),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(pos.x, 0f),
                    end = Offset(pos.x, totalHeight),
                    strokeWidth = 1f
                )
            }
        }
    }
}

private fun DrawScope.drawCustomShape(shape: DrawingShape, totalWidth: Float) {
    when (shape.type) {
        DrawingToolType.TRENDLINE -> {
            drawLine(
                color = BlueAccent,
                start = Offset(shape.startX, shape.startY),
                end = Offset(shape.endX, shape.endY),
                strokeWidth = 2.5f
            )
        }
        DrawingToolType.HORIZONTAL_RAY -> {
            drawLine(
                color = GoldWarning,
                start = Offset(0f, shape.startY),
                end = Offset(totalWidth, shape.startY),
                strokeWidth = 2f
            )
        }
        DrawingToolType.FIBONACCI -> {
            val levels = listOf(0.0f, 0.236f, 0.382f, 0.5f, 0.618f, 0.786f, 1.0f)
            val dy = shape.endY - shape.startY
            levels.forEach { lvl ->
                val y = shape.startY + dy * lvl
                val color = if (lvl == 0.618f || lvl == 0.5f) GoldWarning else Color(0x992979FF)
                drawLine(color, Offset(0f, y), Offset(totalWidth, y), strokeWidth = 1.5f)
            }
        }
        DrawingToolType.RISK_REWARD -> {
            val topY = min(shape.startY, shape.endY)
            val bottomY = max(shape.startY, shape.endY)
            val midY = (topY + bottomY) / 2f
            val width = abs(shape.endX - shape.startX).coerceAtLeast(60f)
            val left = min(shape.startX, shape.endX)

            drawRect(Color(0x3300E676), Offset(left, topY), Size(width, midY - topY))
            drawRect(Color(0x33FF5252), Offset(left, midY), Size(width, bottomY - midY))
            drawLine(TextPrimary, Offset(left, midY), Offset(left + width, midY), strokeWidth = 2f)
        }
        DrawingToolType.RULER -> {
            drawLine(CyanAccent, Offset(shape.startX, shape.startY), Offset(shape.endX, shape.endY), strokeWidth = 2f)
            drawCircle(CyanAccent, radius = 4f, center = Offset(shape.startX, shape.startY))
            drawCircle(CyanAccent, radius = 4f, center = Offset(shape.endX, shape.endY))
        }
        else -> Unit
    }
}
