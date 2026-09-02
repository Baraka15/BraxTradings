package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.PortfolioPosition
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun PortfolioAllocationDonutChart(
    positions: List<PortfolioPosition>,
    modifier: Modifier = Modifier
) {
    val totalValue = positions.sumOf { it.currentValue }.coerceAtLeast(1.0)
    val colors = listOf(BlueAccent, PurpleAccent, CyanAccent, GoldWarning, GreenBullish)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Asset Allocation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        var startAngle = -90f
                        positions.forEachIndexed { i, p ->
                            val sweep = (p.currentValue / totalValue * 360f).toFloat()
                            val sliceColor = colors[i % colors.size]
                            drawArc(
                                color = sliceColor,
                                startAngle = startAngle,
                                sweepAngle = sweep - 2f,
                                useCenter = false,
                                style = Stroke(width = 16f)
                            )
                            startAngle += sweep
                        }
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.0fk", totalValue / 1000)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    positions.forEachIndexed { i, p ->
                        val pct = (p.currentValue / totalValue) * 100
                        val c = colors[i % colors.size]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(c, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(p.symbol, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                String.format(Locale.US, "%.1f%%", pct),
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
