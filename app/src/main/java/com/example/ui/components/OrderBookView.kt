package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.OrderBook
import com.example.data.market.OrderBookLevel
import com.example.ui.theme.*

@Composable
fun OrderBookView(
    orderBook: OrderBook?,
    onPriceSelected: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (orderBook == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Text("Connecting to L2 Market Depth...", color = TextMuted, fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(6.dp)
            .testTag("order_book_ladder")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("SIZE (BIDS)", color = BullGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("PRICE (USD)", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
            Text("SIZE (ASKS)", color = BearRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        // Asks (displayed in reverse so lowest ask is closest to spread)
        val visibleAsks = orderBook.asks.take(5).reversed()
        val visibleBids = orderBook.bids.take(5)

        Column(modifier = Modifier.fillMaxWidth()) {
            visibleAsks.forEach { ask ->
                OrderBookRow(
                    level = ask,
                    isBid = false,
                    onPriceClick = { onPriceSelected(ask.price) }
                )
            }
        }

        // Spread Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SPREAD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                "$${"%.2f".format(orderBook.spread)} (${"%.2f".format(orderBook.spreadPercent)}%)",
                color = M3Primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Bids
        Column(modifier = Modifier.fillMaxWidth()) {
            visibleBids.forEach { bid ->
                OrderBookRow(
                    level = bid,
                    isBid = true,
                    onPriceClick = { onPriceSelected(bid.price) }
                )
            }
        }
    }
}

@Composable
private fun OrderBookRow(
    level: OrderBookLevel,
    isBid: Boolean,
    onPriceClick: () -> Unit
) {
    val barColor = if (isBid) BullGreen.copy(alpha = 0.18f) else BearRed.copy(alpha = 0.18f)
    val textColor = if (isBid) BullGreen else BearRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .clickable { onPriceClick() }
            .padding(vertical = 1.dp)
    ) {
        // Depth fill background
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level.depthPercentage)
                .align(if (isBid) Alignment.CenterStart else Alignment.CenterEnd)
                .background(barColor)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBid) {
                Text(
                    text = if (level.size > 100) "${level.size.toInt()}" else "%.2f".format(level.size),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$%.2f".format(level.price),
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$%.2f".format(level.price),
                    color = textColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = if (level.size > 100) "${level.size.toInt()}" else "%.2f".format(level.size),
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
