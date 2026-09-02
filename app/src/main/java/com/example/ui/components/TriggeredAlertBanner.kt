package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.PriceAlert
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TriggeredAlertBanner(
    alert: PriceAlert,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GoldWarning.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Triggered Alert",
                tint = GoldWarning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "TRIGGERED: ${alert.symbol}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = GoldWarning
                )
                Text(
                    text = "Hit target of " + String.format(Locale.US, "$%.2f", alert.targetPrice) + if (alert.note.isNotEmpty()) " (${alert.note})" else "",
                    fontSize = 11.sp,
                    color = TextPrimary
                )
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
