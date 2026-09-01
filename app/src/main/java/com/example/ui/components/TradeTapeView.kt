package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.TradeTapeItem
import com.example.ui.theme.*
import kotlinx.coroutines.flow.SharedFlow
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TradeTapeView(
    tapeStream: SharedFlow<TradeTapeItem>,
    symbolFilter: String,
    modifier: Modifier = Modifier
) {
    val tapeHistory = remember { mutableStateListOf<TradeTapeItem>() }

    LaunchedEffect(symbolFilter) {
        tapeHistory.clear()
        tapeStream.collect { item ->
            if (item.symbol == symbolFilter) {
                tapeHistory.add(0, item)
                if (tapeHistory.size > 30) {
                    tapeHistory.removeLast()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(6.dp)
            .testTag("time_and_sales_tape")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TIME", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("PRICE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("SIZE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("SIDE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
        }

        if (tapeHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Streaming live Time & Sales...", color = TextMuted, fontSize = 11.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                items(tapeHistory, key = { it.id }) { item ->
                    val color = if (item.isBuyerMaker) BullGreen else BearRed
                    val sideLabel = if (item.isBuyerMaker) "BUY" else "SELL"
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(timeStr, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text("$%.2f".format(item.price), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text(
                            if (item.size > 100) "${item.size.toInt()}" else "%.2f".format(item.size),
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            sideLabel,
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }
    }
}
