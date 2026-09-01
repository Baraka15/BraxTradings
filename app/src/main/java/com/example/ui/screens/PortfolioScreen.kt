package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.local.PositionEntity
import com.example.data.local.TradeLedgerEntity
import com.example.domain.trading.OrderSide
import com.example.domain.trading.OrderStatus
import com.example.ui.TradingViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PortfolioScreen(
    viewModel: TradingViewModel,
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.portfolioSummary.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val openOrders by viewModel.pendingOrders.collectAsState()
    val tradeHistory by viewModel.tradeHistory.collectAsState()
    val quotesMap by viewModel.marketEngine.quotes.collectAsState()

    var selectedSection by remember { mutableStateOf(0) } // 0 = Positions, 1 = Open Orders, 2 = Trade Ledger

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 12.dp)
            .testTag("portfolio_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Account Equity Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("TOTAL NET LIQUIDITY", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$%,.2f".format(summary.netLiquidity),
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val isTodayPos = summary.todayPnL >= 0
                    val todayColor = if (isTodayPos) BullGreen else BearRed

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTodayPos) BullGreenBg else BearRedBg
                        ) {
                            Text(
                                text = "${if (isTodayPos) "+" else ""}$${"%,.2f".format(summary.todayPnL)} (${if (isTodayPos) "+" else ""}${"%.2f".format(summary.todayPnLPercent)}%)",
                                color = todayColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Today", color = TextSecondary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderDark, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid of 4 sub-metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem("Cash Balance", "$%,.2f".format(summary.cashBalance))
                        MetricItem("Buying Power", "$%,.2f".format(summary.buyingPower))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val isRealizedPos = summary.totalRealizedPnL >= 0
                        MetricItem(
                            "Realized P&L",
                            "${if (isRealizedPos) "+" else ""}$${"%,.2f".format(summary.totalRealizedPnL)}",
                            if (isRealizedPos) BullGreen else BearRed
                        )
                        MetricItem(
                            "Win Rate",
                            "${"%.1f".format(summary.winRatePercent)}% (${summary.totalTradesCount} trades)",
                            M3Primary
                        )
                    }
                }
            }
        }

        // 2. Portfolio Section Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PortfolioSectionTab(
                    title = "Positions (${positions.size})",
                    isSelected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                PortfolioSectionTab(
                    title = "Open Orders (${openOrders.size})",
                    isSelected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    modifier = Modifier.weight(1.1f)
                )
                PortfolioSectionTab(
                    title = "Trade Ledger (${tradeHistory.size})",
                    isSelected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    modifier = Modifier.weight(1.2f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Section Content
        when (selectedSection) {
            0 -> {
                // Active Positions List
                if (positions.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = Icons.Default.ShowChart,
                            title = "No Open Positions",
                            subtitle = "Place a Buy order on any stock in the Terminal to start building positions."
                        )
                    }
                } else {
                    items(positions, key = { it.symbol }) { pos ->
                        val currentPrice = quotesMap[pos.symbol]?.price ?: pos.averagePrice
                        val marketValue = pos.shares * currentPrice
                        val uPnL = marketValue - pos.totalCost
                        val uPnLPct = if (pos.totalCost > 0) (uPnL / pos.totalCost) * 100.0 else 0.0
                        val isGain = uPnL >= 0

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectSymbol(pos.symbol)
                                    onNavigateToTerminal()
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(pos.symbol, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${"%.2f".format(pos.shares)} Shares",
                                            color = M3Primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(M3SecondaryContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isGain) "+" else ""}$${"%,.2f".format(uPnL)}",
                                            color = if (isGain) BullGreen else BearRed,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "(${if (isGain) "+" else ""}${"%.2f".format(uPnLPct)}%)",
                                            color = if (isGain) BullGreen else BearRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Avg Price", color = TextMuted, fontSize = 10.sp)
                                            Text("$${"%.2f".format(pos.averagePrice)}", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Column {
                                            Text("Current Price", color = TextMuted, fontSize = 10.sp)
                                            Text("$${"%.2f".format(currentPrice)}", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Column {
                                            Text("Market Value", color = TextMuted, fontSize = 10.sp)
                                            Text("$${"%,.2f".format(marketValue)}", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.selectSymbol(pos.symbol)
                                            onNavigateToTerminal()
                                        },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Text("Chart / Trade", fontSize = 11.sp, color = M3Primary)
                                    }

                                    Button(
                                        onClick = { viewModel.marketClosePosition(pos.symbol) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BearRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Text("Market Close", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Open Pending Orders List
                if (openOrders.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = Icons.Default.FormatListBulleted,
                            title = "No Open Orders",
                            subtitle = "Pending Limit and Stop orders will appear here until triggered."
                        )
                    }
                } else {
                    items(openOrders, key = { it.orderId }) { order ->
                        OpenOrderCard(
                            order = order,
                            onCancel = { viewModel.cancelOrder(order.orderId) }
                        )
                    }
                }
            }

            2 -> {
                // Completed Trades Ledger
                if (tradeHistory.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = Icons.Default.ReceiptLong,
                            title = "No Trade History Yet",
                            subtitle = "Filled executions and closed round-trips will be permanently recorded here."
                        )
                    }
                } else {
                    items(tradeHistory, key = { it.tradeId }) { trade ->
                        TradeLedgerCard(trade = trade)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, valueColor: Color = TextPrimary) {
    Column {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PortfolioSectionTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) M3PrimaryContainer else Color.Transparent,
        modifier = modifier
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) M3OnPrimaryContainer else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier
                .padding(vertical = 7.dp)
                .wrapContentWidth(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun OpenOrderCard(
    order: OrderEntity,
    onCancel: () -> Unit
) {
    val isBuy = order.side == OrderSide.BUY.name
    val sideColor = if (isBuy) BullGreen else BearRed

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(order.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBuy) BullGreenBg else BearRedBg
                    ) {
                        Text(
                            order.side,
                            color = sideColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    Text(order.type, color = TextSecondary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Qty: ${"%.2f".format(order.quantity)} | Price: $${"%.2f".format(order.limitPrice ?: order.stopPrice ?: 0.0)}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BearRedBg,
                    contentColor = BearRed
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TradeLedgerCard(
    trade: TradeLedgerEntity
) {
    val isGain = trade.realizedPnL >= 0
    val pnlColor = if (isGain) BullGreen else BearRed
    val timeStr = SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()).format(Date(trade.timestamp))
    val isBuy = trade.side == OrderSide.BUY.name
    val sideColor = if (isBuy) BullGreen else BearRed

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(trade.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isBuy) BullGreenBg else BearRedBg
                    ) {
                        Text(
                            trade.side,
                            color = sideColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        "${"%.2f".format(trade.quantity)} shares",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    "Exec @ $${"%.2f".format(trade.executionPrice)} ($${"%,.2f".format(trade.totalAmount)})",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(timeStr, color = TextMuted, fontSize = 10.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                if (trade.realizedPnL != 0.0) {
                    Text(
                        text = "${if (isGain) "+" else ""}$${"%,.2f".format(trade.realizedPnL)}",
                        color = pnlColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Fee: $${"%.2f".format(trade.fee)}",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
