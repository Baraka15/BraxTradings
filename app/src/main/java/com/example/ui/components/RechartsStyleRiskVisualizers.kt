package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.AccountRiskMetrics
import com.example.domain.trading.ExposureSlice
import com.example.domain.trading.RiskAlertLevel
import com.example.ui.theme.*

/**
 * Recharts-inspired Interactive Donut / Radial Chart visualizing
 * Asset Exposure and Portfolio Distribution.
 */
@Composable
fun RechartsDonutExposureChart(
    slices: List<ExposureSlice>,
    totalExposure: Double,
    modifier: Modifier = Modifier
) {
    var selectedSliceId by remember { mutableStateOf<String?>(null) }
    val activeSlice = slices.find { it.id == selectedSliceId } ?: slices.firstOrNull()

    if (slices.isEmpty() || totalExposure <= 0.0) {
        Box(
            modifier = modifier
                .background(DarkSurface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                Text("No Open Day Trading Exposure", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Open a position from Markets to visualize live portfolio risk.", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Exposure Allocation Breakdown", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Recharts-style interactive radial exposure", color = TextSecondary, fontSize = 11.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${slices.size} Active Positions",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Donut Chart Graphic with Centered Callout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(190.dp)
                ) {
                    val strokeWidth = 26.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    var currentStartAngle = -90f
                    val totalDegrees = 360f

                    slices.forEach { slice ->
                        val sweep = (slice.percentage / 100.0 * totalDegrees).toFloat()
                        val isSelected = slice.id == activeSlice?.id
                        val sliceColor = Color(slice.colorHex)

                        // Draw main slice arc
                        drawArc(
                            color = sliceColor,
                            startAngle = currentStartAngle + 1.5f,
                            sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isSelected) strokeWidth + 6f else strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )

                        currentStartAngle += sweep
                    }
                }

                // Centered Recharts-style Dynamic Value Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    if (activeSlice != null) {
                        Text(
                            activeSlice.label,
                            color = Color(activeSlice.colorHex),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "\$${String.format("%,.0f", activeSlice.notionalValue)}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${String.format("%.1f", activeSlice.percentage)}% of Gross",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            "Total Gross",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            "\$${String.format("%,.0f", totalExposure)}",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Interactive Slice Legend Cards (Tap to Focus Slice)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.forEach { slice ->
                    val isSelected = slice.id == activeSlice?.id
                    val sliceColor = Color(slice.colorHex)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedSliceId = slice.id },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) DarkSurface else Color.Transparent,
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(sliceColor, sliceColor.copy(alpha = 0.3f)))) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(sliceColor, CircleShape)
                                )
                                Column {
                                    Text(slice.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(slice.subLabel, color = TextMuted, fontSize = 10.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "\$${String.format("%,.2f", slice.notionalValue)}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${String.format("%.1f", slice.percentage)}%",
                                    color = sliceColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-inspired Margin Utilization Gauge Meter with Safety Zones
 * (Safe -> Caution -> Danger -> Liquidation Call)
 */
@Composable
fun MarginUtilizationGaugeMeter(
    metrics: AccountRiskMetrics,
    modifier: Modifier = Modifier
) {
    val utilPct = metrics.marginUtilizationPct.coerceIn(0.0, 100.0)
    val animatedProgress by animateFloatAsState(
        targetValue = (utilPct / 100.0).toFloat(),
        animationSpec = tween(durationMillis = 600),
        label = "marginUtilProgress"
    )

    val (zoneColor, statusText) = when {
        utilPct < 50.0 -> BullishGreen to "SAFE (<50%)"
        utilPct < 75.0 -> AccentCyan to "MODERATE (50-75%)"
        utilPct < 90.0 -> AccentOrange to "HIGH RISK (75-90%)"
        else -> BearishRed to "MARGIN CALL (≥90%)"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Margin Utilization & Health", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Used vs Available Day Trading Margin", color = TextSecondary, fontSize = 11.sp)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = zoneColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusText,
                        color = zoneColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Big Number Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${String.format("%.1f", utilPct)}%",
                        color = zoneColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "utilized",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Free Margin", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "\$${String.format("%,.2f", metrics.freeMargin)}",
                        color = BullishGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Multi-segment Gauge Bar (0-50 Safe, 50-75 Elevated, 75-90 High Risk, 90-100 Danger)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(DarkSurface, RoundedCornerShape(7.dp))
                ) {
                    // Zone backgrounds
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(0.50f).fillMaxHeight().background(BullishGreen.copy(alpha = 0.2f), RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)))
                        Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(AccentCyan.copy(alpha = 0.2f)))
                        Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(AccentOrange.copy(alpha = 0.2f)))
                        Box(modifier = Modifier.weight(0.10f).fillMaxHeight().background(BearishRed.copy(alpha = 0.25f), RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)))
                    }

                    // Active Filled Progress Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BullishGreen, AccentCyan, zoneColor)
                                ),
                                RoundedCornerShape(7.dp)
                            )
                    )
                }

                // Ticks & Threshold Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", color = TextMuted, fontSize = 9.sp)
                    Text("50% (Comfort)", color = BullishGreen, fontSize = 9.sp)
                    Text("75% (Caution)", color = AccentCyan, fontSize = 9.sp)
                    Text("90% (Margin Call)", color = BearishRed, fontSize = 9.sp)
                }
            }

            // Key Margin Figures in 3 Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Used Margin", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "\$${String.format("%,.2f", metrics.usedMargin)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Equity", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "\$${String.format("%,.2f", metrics.totalEquity)}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Effective Leverage", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "${String.format("%.2f", metrics.effectiveLeverage)}x",
                        color = if (metrics.effectiveLeverage > 5.0) AccentOrange else AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Recharts-inspired Long vs Short Exposure Bar visualizer
 */
@Composable
fun RechartsExposureBarComparison(
    longExposure: Double,
    shortExposure: Double,
    grossExposure: Double,
    modifier: Modifier = Modifier
) {
    val longPct = if (grossExposure > 0) (longExposure / grossExposure).toFloat() else 0.5f
    val shortPct = if (grossExposure > 0) (shortExposure / grossExposure).toFloat() else 0.5f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Long vs Short Exposure Ratio", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Gross: \$${String.format("%,.0f", grossExposure)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Stacked Recharts Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(DarkSurface, RoundedCornerShape(6.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (longPct > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(longPct.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(BullishGreen, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (longPct > 0.15f) {
                                Text("${(longPct * 100).toInt()}%", color = DarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (shortPct > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(shortPct.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(BearishRed, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shortPct > 0.15f) {
                                Text("${(shortPct * 100).toInt()}%", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(BullishGreen, CircleShape))
                    Text("Long: \$${String.format("%,.2f", longExposure)}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(BearishRed, CircleShape))
                    Text("Short: \$${String.format("%,.2f", shortExposure)}", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Day Trader Circuit Breaker & Daily Max Drawdown Tracker
 */
@Composable
fun DayTraderCircuitBreakerProgress(
    dailyLossLimit: Double,
    currentDailyPnL: Double,
    lossUtilizationPct: Double,
    onAdjustLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoss = currentDailyPnL < 0
    val progress = (lossUtilizationPct / 100.0).toFloat().coerceIn(0f, 1f)
    val isBreakerTripped = lossUtilizationPct >= 100.0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBreakerTripped) BearishRed.copy(alpha = 0.15f) else DarkSurfaceElevated
        ),
        border = if (isBreakerTripped) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(BearishRed, BearishRed))) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (isBreakerTripped) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isBreakerTripped) BearishRed else AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Day Trader Daily Loss Circuit Breaker", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                TextButton(
                    onClick = onAdjustLimit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Edit Limit", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Today's Net Realized PnL", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "${if (currentDailyPnL >= 0) "+" else ""}\$${String.format("%.2f", currentDailyPnL)}",
                        color = if (currentDailyPnL >= 0) BullishGreen else BearishRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Max Daily Loss Cap", color = TextMuted, fontSize = 10.sp)
                    Text(
                        "-\$${String.format("%.2f", dailyLossLimit)}",
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Progress Bar of Drawdown vs Cap
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(DarkSurface, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                if (isBreakerTripped) BearishRed else if (progress > 0.7f) AccentOrange else AccentGold,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isLoss) "${String.format("%.1f", lossUtilizationPct)}% of daily loss buffer consumed" else "Within safe daily profit buffer",
                        color = if (isBreakerTripped) BearishRed else TextSecondary,
                        fontSize = 10.sp
                    )
                    Text("Cap: \$${String.format("%.0f", dailyLossLimit)}", color = TextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}
