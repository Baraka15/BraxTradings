package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.components.PortfolioAllocationDonutChart
import com.example.ui.components.PositionRiskCard
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun PortfolioScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolio.collectAsState()
    val totalBalance = portfolio.sumOf { it.currentValue }
    val totalInvested = portfolio.sumOf { it.totalInvested }
    val totalPnl = totalBalance - totalInvested
    val totalPnlPct = if (totalInvested > 0) (totalPnl / totalInvested) * 100 else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Portfolio Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Net Worth", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "$%,.2f", totalBalance),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val isProfit = totalPnl >= 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%+$%,.2f (%+,.2f%%)", totalPnl, totalPnlPct),
                            color = if (isProfit) GreenBullish else RedBearish,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(" All-time", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // Donut Chart
        item {
            PortfolioAllocationDonutChart(positions = portfolio)
        }

        // Open Positions
        item {
            Text("Open Positions (${portfolio.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        }

        items(portfolio, key = { it.symbol }) { pos ->
            PositionRiskCard(position = pos)
        }
    }
}
