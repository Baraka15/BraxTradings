package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.TickerQuote
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun OrderBookComponent(quote: TickerQuote?, modifier: Modifier = Modifier) {
    if (quote == null) return

    val basePrice = quote.currentPrice
    val bids = (1..5).map { i ->
        val price = basePrice - (i * 0.25)
        val size = (100..1500).random()
        Pair(price, size)
    }
    val asks = (1..5).reversed().map { i ->
        val price = basePrice + (i * 0.25)
        val size = (100..1500).random()
        Pair(price, size)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DOM / Order Book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

        // Asks (Red)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(asks) { pair ->
                val price = pair.first
                val size = pair.second
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format(Locale.US, "%.2f", price),
                        style = MaterialTheme.typography.labelMedium,
                        color = BraxRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Spread / Current Price Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                String.format(Locale.US, "Last: %.2f", basePrice),
                style = MaterialTheme.typography.labelLarge,
                color = if (quote.change >= 0) BraxGreen else BraxRed,
                fontWeight = FontWeight.Black
            )
        }

        // Bids (Green)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(bids) { pair ->
                val price = pair.first
                val size = pair.second
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format(Locale.US, "%.2f", price),
                        style = MaterialTheme.typography.labelMedium,
                        color = BraxGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
