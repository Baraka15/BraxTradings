package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.OrderBookDepth
import com.example.domain.trading.TapeTrade
import com.example.ui.theme.*

@Composable
fun OrderBookAndTapeView(
    orderBook: OrderBookDepth,
    recentTrades: List<TapeTrade>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Order Book (Left side)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Order Book (L2)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Asks (Sells)
            orderBook.asks.take(4).reversed().forEach { ask ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("\$${String.format("%.2f", ask.price)}", color = BearishRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format("%.1f", ask.size), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Spread Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(DarkSurface, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Spread \$${String.format("%.2f", orderBook.spread)}",
                    color = AccentCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bids (Buys)
            orderBook.bids.take(4).forEach { bid ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("\$${String.format("%.2f", bid.price)}", color = BullishGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(String.format("%.1f", bid.size), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Live Trades Tape (Right side)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Live Tape",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            recentTrades.take(7).forEach { trade ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(trade.time, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "\$${String.format("%.2f", trade.price)}",
                        color = if (trade.side == com.example.domain.trading.OrderSide.BUY) BullishGreen else BearishRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(String.format("%.1f", trade.size), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
