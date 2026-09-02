package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.components.PositionRiskCard
import com.example.ui.theme.*

@Composable
fun RiskDashboardScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolio.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Risk Engine Status", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Max Drawdown Limit: 5.0%", fontSize = 12.sp, color = TextSecondary)
                    Text("Portfolio VaR (95%): $1,420.00", fontSize = 12.sp, color = TextSecondary)
                    Text("Automated Stop-Loss Guard: ACTIVE", fontSize = 12.sp, color = GreenBullish, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("Risk Guarded Positions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        }

        items(portfolio.size) { idx ->
            PositionRiskCard(position = portfolio[idx])
        }
    }
}
