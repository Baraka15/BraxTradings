package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Real-Time Dynamic Price Ticker Ribbon Component
 * Supports dynamic stock & crypto ticks via WebSocket push or HTTP polling intervals
 * with flash animations on tick updates.
 */
@Composable
fun RealTimePriceTickerRibbon(
    instruments: List<Instrument>,
    selectedSymbol: String,
    priceDirections: Map<String, PriceDirection>,
    streamInfo: TickerStreamInfo,
    onSelectSymbol: (String) -> Unit,
    onToggleStreaming: () -> Unit,
    onSetInterval: (Long) -> Unit,
    onSetTransportMode: (StreamTransportMode) -> Unit,
    onManualRefreshTick: () -> Unit,
    onOpenCreateAlert: (Instrument) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStreamControls by remember { mutableStateOf(false) }

    // Pulsing alpha for live feed dot
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Bar: Stream Telemetry, Transport Badge, and Control Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Feed Badge & Transport Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing Connection Indicator
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (streamInfo.isStreaming) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (streamInfo.isStreaming) BullishGreen.copy(alpha = pulseAlpha) else BearishRed,
                                        CircleShape
                                    )
                            )
                            Text(
                                if (streamInfo.isStreaming) streamInfo.transportMode.badge else "STREAM PAUSED",
                                color = if (streamInfo.isStreaming) BullishGreen else BearishRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Frequency & Latency Readout
                    Text(
                        "${streamInfo.intervalMs}ms • ${streamInfo.latencyMs}ms ping",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Buttons: Manual Tick Refresh & Quick Config
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Manual Tick Trigger Button
                    IconButton(
                        onClick = onManualRefreshTick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Force Price Tick",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Toggle Stream Play/Pause
                    IconButton(
                        onClick = onToggleStreaming,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (streamInfo.isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (streamInfo.isStreaming) "Pause Stream" else "Resume Stream",
                            tint = if (streamInfo.isStreaming) AccentOrange else BullishGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Expand Stream Engine Settings
                    IconButton(
                        onClick = { showStreamControls = !showStreamControls },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (showStreamControls) Icons.Default.ExpandLess else Icons.Default.Tune,
                            contentDescription = "Configure Ticker Stream",
                            tint = if (showStreamControls) AccentCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expandable Stream Settings & Polling / WebSocket Control Panel
            AnimatedVisibility(
                visible = showStreamControls,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(DarkSurface, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Transport Selection (WebSocket vs Polling)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Protocol Feed:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StreamTransportMode.values().forEach { mode ->
                                val isSelected = streamInfo.transportMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetTransportMode(mode) },
                                    label = { Text(mode.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = AccentCyan
                                    ),
                                    border = if (isSelected) BorderStroke(1.dp, AccentCyan) else null
                                )
                            }
                        }
                    }

                    // Stream Update Interval Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tick Cadence:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                250L to "250ms (Ultra)",
                                500L to "500ms (Fast)",
                                800L to "800ms (Norm)",
                                2000L to "2s (Eco)"
                            ).forEach { (ms, label) ->
                                val isSelected = streamInfo.intervalMs == ms
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onSetInterval(ms) },
                                    color = if (isSelected) AccentCyan else DarkSurfaceElevated,
                                    border = if (!isSelected) BorderStroke(1.dp, DarkBorder) else null
                                ) {
                                    Text(
                                        label,
                                        color = if (isSelected) DarkBackground else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Total telemetry statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Ticks: ${String.format("%,d", streamInfo.totalTicksReceived)}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Status: ${if (streamInfo.isConnected) "Push Subscribed (Active)" else "Disconnected"}",
                            color = if (streamInfo.isConnected) BullishGreen else BearishRed,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Real-Time Horizontal Price Ticker Ribbon
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                instruments.forEach { instrument ->
                    RealTimePriceTickerCard(
                        instrument = instrument,
                        isSelected = instrument.symbol == selectedSymbol,
                        priceDirection = priceDirections[instrument.symbol] ?: PriceDirection.NEUTRAL,
                        onClick = { onSelectSymbol(instrument.symbol) },
                        onOpenCreateAlert = { onOpenCreateAlert(instrument) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Dynamic Ticker Chip Card with Flash Animation on Tick
 */
@Composable
fun RealTimePriceTickerCard(
    instrument: Instrument,
    isSelected: Boolean,
    priceDirection: PriceDirection,
    onClick: () -> Unit,
    onOpenCreateAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = instrument.change24h >= 0

    // Dynamic Flash Color state on price updates
    var flashKey by remember { mutableStateOf(0) }
    LaunchedEffect(instrument.currentPrice) {
        flashKey++
    }

    val flashTargetColor = when (priceDirection) {
        PriceDirection.UP -> BullishGreen.copy(alpha = 0.35f)
        PriceDirection.DOWN -> BearishRed.copy(alpha = 0.35f)
        PriceDirection.NEUTRAL -> Color.Transparent
    }

    val flashColor by animateColorAsState(
        targetValue = if (flashKey > 0) flashTargetColor else Color.Transparent,
        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
        label = "flashColor"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) DarkSurface.copy(alpha = 0.95f) else DarkSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentCyan else DarkBorder
        )
    ) {
        Box(
            modifier = Modifier
                .background(flashColor)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Symbol and Category Badge
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            instrument.symbol,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(AccentCyan, CircleShape)
                            )
                        }
                    }
                    Text(
                        instrument.category,
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mini Sparkline Graph
                if (instrument.sparkline.isNotEmpty()) {
                    Box(modifier = Modifier.size(width = 38.dp, height = 20.dp)) {
                        SparklineView(
                            data = instrument.sparkline,
                            isPositive = isPositive,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }


                // Dynamic Live Price & Percentage Change
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Formatted Price with Directional Arrow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            when {
                                instrument.currentPrice >= 1000.0 -> String.format("%,.2f", instrument.currentPrice)
                                instrument.currentPrice >= 1.0 -> String.format("%.2f", instrument.currentPrice)
                                else -> String.format("%.4f", instrument.currentPrice)
                            },
                            color = when (priceDirection) {
                                PriceDirection.UP -> BullishGreen
                                PriceDirection.DOWN -> BearishRed
                                PriceDirection.NEUTRAL -> TextPrimary
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Icon(
                            if (isPositive) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isPositive) BullishGreen else BearishRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 24h Percentage Change
                    Text(
                        "${if (isPositive) "+" else ""}${String.format("%.2f", instrument.change24h)}%",
                        color = if (isPositive) BullishGreen else BearishRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
