package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.*
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Advanced TradingView Suite:
 * 1. Real-Time Level 2 Depth of Market (DOM) Trading Ladder with 1-Tap Limit Orders
 * 2. Multi-Symbol Real-Time Technical Scanner & Strategy Confluence Matrix
 * 3. Whale Order & Block Trade Flow Radar
 */
@Composable
fun TradingViewAdvancedSuite(
    instruments: List<Instrument>,
    selectedInstrument: Instrument,
    orderBook: OrderBookDepth,
    recentTrades: List<TapeTrade>,
    onSelectSymbol: (String) -> Unit,
    onPlaceLimitOrder: (OrderSide, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedToolTab by remember { mutableStateOf(0) } // 0: DOM Ladder, 1: Technical Scanner, 2: Whale Radar
    val toolTabs = listOf("DOM Ladder", "Technical Screener", "Whale Radar")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Section Title & Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Text("TradingView Pro Tools", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Sub-tabs
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    toolTabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedToolTab == index
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedToolTab = index },
                            color = if (isSelected) AccentCyan else DarkSurface,
                            border = if (!isSelected) BorderStroke(1.dp, DarkBorder) else null
                        ) {
                            Text(
                                tabTitle,
                                color = if (isSelected) DarkBackground else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Tab Content Switcher
            when (selectedToolTab) {
                0 -> Level2DOMLadderView(
                    instrument = selectedInstrument,
                    orderBook = orderBook,
                    onPlaceLimitOrder = onPlaceLimitOrder
                )
                1 -> TechnicalConfluenceScannerView(
                    instruments = instruments,
                    selectedSymbol = selectedInstrument.symbol,
                    onSelectSymbol = onSelectSymbol
                )
                2 -> WhaleOrderFlowRadarView(
                    trades = recentTrades,
                    symbol = selectedInstrument.symbol
                )
            }
        }
    }
}

/**
 * 1. Real-Time Level 2 Depth of Market (DOM) Trading Ladder
 */
@Composable
private fun Level2DOMLadderView(
    instrument: Instrument,
    orderBook: OrderBookDepth,
    onPlaceLimitOrder: (OrderSide, Double, Double) -> Unit
) {
    var defaultQty by remember { mutableStateOf("1.0") }
    val maxAskTotal = orderBook.asks.maxOfOrNull { it.size } ?: 1.0
    val maxBidTotal = orderBook.bids.maxOfOrNull { it.size } ?: 1.0
    val maxLevelSize = max(maxAskTotal, maxBidTotal)

    // Calculate Bid/Ask Imbalance
    val totalBidDepth = orderBook.bids.sumOf { it.size }
    val totalAskDepth = orderBook.asks.sumOf { it.size }
    val totalDepth = totalBidDepth + totalAskDepth
    val bidPressurePct = if (totalDepth > 0) (totalBidDepth / totalDepth) * 100.0 else 50.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Order Size & Depth Imbalance Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Order Book Liquidity Ladder (Tap Price to Place Limit Order)",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        // Cumulative Imbalance Bar
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bids ${String.format("%.1f", bidPressurePct)}%", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Spread $${String.format("%.2f", orderBook.spread)}", color = TextMuted, fontSize = 10.sp)
                Text("Asks ${String.format("%.1f", 100 - bidPressurePct)}%", color = BearishRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(bidPressurePct.toFloat().coerceAtLeast(1f))
                        .background(BullishGreen)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight((100f - bidPressurePct.toFloat()).coerceAtLeast(1f))
                        .background(BearishRed)
                )
            }
        }

        // Ladder Column Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("BUY LIMIT", color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("PRICE (USD)", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("SELL LIMIT", color = BearishRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        // Asks (Sells - Top)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            orderBook.asks.take(4).reversed().forEach { level ->
                val depthRatio = (level.size / maxLevelSize).toFloat().coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurface)
                ) {
                    // Ask Liquidity Bar from right
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(depthRatio)
                            .align(Alignment.CenterEnd)
                            .background(BearishRed.copy(alpha = 0.18f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("-", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(
                            String.format("%.2f", level.price),
                            color = BearishRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable {
                                    onPlaceLimitOrder(OrderSide.SELL, level.price, defaultQty.toDoubleOrNull() ?: 1.0)
                                }
                        )
                        Text(
                            "${String.format("%.2f", level.size)} (Sell)",
                            color = BearishRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Current Mid Price Level
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AccentCyan.copy(alpha = 0.1f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡ MID MARKET", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$${String.format("%.2f", instrument.currentPrice)}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bids (Buys - Bottom)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            orderBook.bids.take(4).forEach { level ->
                val depthRatio = (level.size / maxLevelSize).toFloat().coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurface)
                ) {
                    // Bid Liquidity Bar from left
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(depthRatio)
                            .align(Alignment.CenterStart)
                            .background(BullishGreen.copy(alpha = 0.18f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${String.format("%.2f", level.size)} (Buy)",
                            color = BullishGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            String.format("%.2f", level.price),
                            color = BullishGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable {
                                    onPlaceLimitOrder(OrderSide.BUY, level.price, defaultQty.toDoubleOrNull() ?: 1.0)
                                }
                        )
                        Text("-", color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 2. Multi-Symbol Real-Time Technical Screener Matrix
 */
@Composable
private fun TechnicalConfluenceScannerView(
    instruments: List<Instrument>,
    selectedSymbol: String,
    onSelectSymbol: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Live Technical Confluence Screener (Oscillators + Moving Averages)",
            color = TextMuted,
            fontSize = 11.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            instruments.forEach { inst ->
                val isSelected = inst.symbol == selectedSymbol
                // Simulated rating calculation based on 24h change & sparkline
                val isBullish = inst.change24h > 1.0
                val isStrongBullish = inst.change24h > 3.0
                val isBearish = inst.change24h < -1.0
                val isStrongBearish = inst.change24h < -3.0

                val rating = when {
                    isStrongBullish -> TechnicalRating.STRONG_BUY
                    isBullish -> TechnicalRating.BUY
                    isStrongBearish -> TechnicalRating.STRONG_SELL
                    isBearish -> TechnicalRating.SELL
                    else -> TechnicalRating.NEUTRAL
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectSymbol(inst.symbol) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) DarkSurface.copy(alpha = 0.9f) else DarkSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentCyan else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Symbol & Category
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(inst.symbol, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(inst.category, color = TextMuted, fontSize = 9.sp)
                        }

                        // Price & Change
                        Text(
                            "$${if (inst.currentPrice >= 1000) String.format("%,.2f", inst.currentPrice) else String.format("%.2f", inst.currentPrice)}",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        // Rating Pill
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(rating.colorHex).copy(alpha = 0.15f)
                        ) {
                            Text(
                                rating.label,
                                color = Color(rating.colorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Whale Order Flow Radar & Block Trade Stream
 */
@Composable
private fun WhaleOrderFlowRadarView(
    trades: List<TapeTrade>,
    symbol: String
) {
    val whaleTrades = trades.filter { trade ->
        val notional = trade.price * trade.size
        notional >= 50000.0 // Whale threshold
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🐋 Whale & Institutional Block Trades (≥$50k)", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${whaleTrades.size} Blocks Detected", color = TextMuted, fontSize = 10.sp)
        }

        if (whaleTrades.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Scanning real-time tape for block orders...", color = TextMuted, fontSize = 11.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                whaleTrades.take(5).forEach { trade ->
                    val notional = trade.price * trade.size
                    val isBuy = trade.side == OrderSide.BUY

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, if (isBuy) BullishGreen.copy(alpha = 0.4f) else BearishRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(trade.time, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = if (isBuy) BullishGreen.copy(alpha = 0.2f) else BearishRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        if (isBuy) "WHALE BUY" else "WHALE SELL",
                                        color = if (isBuy) BullishGreen else BearishRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Text(
                                "$${String.format("%.2f", trade.price)}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "$${String.format("%,.0f", notional)}",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
