package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.TechnicalSignalSummary
import com.example.ui.theme.*

@Composable
fun TechnicalConfluenceCard(
    summary: TechnicalSignalSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Technical Consensus (${summary.symbol})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Surface(
                    color = when (summary.rating) {
                        "Strong Buy", "Buy" -> GreenBullish.copy(alpha = 0.2f)
                        "Strong Sell", "Sell" -> RedBearish.copy(alpha = 0.2f)
                        else -> GoldWarning.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = summary.rating.uppercase(),
                        color = when (summary.rating) {
                            "Strong Buy", "Buy" -> GreenBullish
                            "Strong Sell", "Sell" -> RedBearish
                            else -> GoldWarning
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BUY", color = GreenBullish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${summary.buyCount}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEUTRAL", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${summary.neutralCount}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SELL", color = RedBearish, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${summary.sellCount}", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Gauge Bar
            LinearProgressIndicator(
                progress = { (summary.score + 1f) / 2f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (summary.score >= 0) GreenBullish else RedBearish,
                trackColor = SurfaceDark,
            )
        }
    }
}
