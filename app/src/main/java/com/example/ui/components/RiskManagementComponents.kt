package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.PortfolioPosition
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun PositionRiskCard(
    position: PortfolioPosition,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(position.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("${position.shares} shares @ $${String.format(Locale.US, "%.2f", position.entryPrice)}", color = TextSecondary, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val isProfit = position.pnl >= 0
                    Text(
                        text = String.format(Locale.US, "%+$%,.2f", position.pnl),
                        color = if (isProfit) GreenBullish else RedBearish,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.US, "%+,.2f%%", position.pnlPercent),
                        color = if (isProfit) GreenBullish else RedBearish,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stop loss and take profit chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = RedBearish.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Stop Loss", color = RedBearish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = position.stopLoss?.let { String.format(Locale.US, "$%.2f", it) } ?: "None",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Surface(
                    color = GreenBullish.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Take Profit", color = GreenBullish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = position.takeProfit?.let { String.format(Locale.US, "$%.2f", it) } ?: "None",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
