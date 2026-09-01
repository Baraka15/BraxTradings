package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.OrderSide
import com.example.ui.TradingViewModel
import com.example.ui.theme.*

@Composable
fun PortfolioScreen(viewModel: TradingViewModel) {
    val balance by viewModel.balance.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val orders by viewModel.orders.collectAsState()

    val totalUnrealizedPnL = positions.sumOf { it.pnl }
    val totalEquity = balance + positions.sumOf { (it.entryPrice * it.quantity) / it.leverage } + totalUnrealizedPnL

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 84.dp, top = 8.dp)
    ) {
        // Total Account Equity Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Account Equity", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        "\$${String.format("%,.2f", totalEquity)}",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available Margin", color = TextMuted, fontSize = 11.sp)
                            Text("\$${String.format("%,.2f", balance)}", color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Unrealized PnL", color = TextMuted, fontSize = 11.sp)
                            val isPos = totalUnrealizedPnL >= 0
                            Text(
                                "${if (isPos) "+" else ""}\$${String.format("%.2f", totalUnrealizedPnL)}",
                                color = if (isPos) BullishGreen else BearishRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Open Positions
        item {
            Text("Open Positions (${positions.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (positions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(DarkSurface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active positions. Execute orders from Dashboard.", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(positions) { pos ->
                val isLong = pos.side == OrderSide.BUY
                val isPnlPos = pos.pnl >= 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(pos.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isLong) BullishGreen.copy(alpha = 0.15f) else BearishRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${if (isLong) "LONG" else "SHORT"} ${pos.leverage.toInt()}x",
                                        color = if (isLong) BullishGreen else BearishRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.closePosition(pos.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = BearishRed.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Close", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Size: ${pos.quantity}", color = TextSecondary, fontSize = 11.sp)
                                Text("Entry: \$${String.format("%.2f", pos.entryPrice)}", color = TextMuted, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (isPnlPos) "+" else ""}\$${String.format("%.2f", pos.pnl)}",
                                    color = if (isPnlPos) BullishGreen else BearishRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${if (isPnlPos) "+" else ""}${String.format("%.2f", pos.pnlPercentage)}%",
                                    color = if (isPnlPos) BullishGreen else BearishRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Order History
        item {
            Text("Order Log (${orders.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(orders) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(order.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                order.side.name,
                                color = if (order.side == OrderSide.BUY) BullishGreen else BearishRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("${order.type.name} • ${order.quantity} shares", color = TextSecondary, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("\$${String.format("%.2f", order.price)}", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text(order.status, color = BullishGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
