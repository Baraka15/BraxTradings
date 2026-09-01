package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.domain.trading.AlertCondition
import com.example.domain.trading.StockPriceAlert

class PriceAlertNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "stock_price_alerts_channel"
        const val CHANNEL_NAME = "Stock & Market Price Alerts"
        const val CHANNEL_DESCRIPTION = "Instant notifications when your target stock prices are hit"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun triggerPriceAlertNotification(alert: StockPriceAlert, livePrice: Double) {
        val directionEmoji = if (alert.condition == AlertCondition.CROSSES_ABOVE) "📈 🚀" else "📉 ⚠️"
        val conditionText = if (alert.condition == AlertCondition.CROSSES_ABOVE) "exceeded target of" else "dropped below target of"

        val title = "$directionEmoji ${alert.symbol} Price Alert Hit!"
        val contentText = "${alert.symbol} is now \$${String.format("%.2f", livePrice)} ($conditionText \$${String.format("%.2f", alert.targetPrice)})"
        
        val bigText = buildString {
            append("🎯 Target Price: \$${String.format("%.2f", alert.targetPrice)}\n")
            append("⚡ Current Market Price: \$${String.format("%.2f", livePrice)}\n")
            if (alert.note.isNotBlank()) {
                append("📝 Note: ${alert.note}\n")
            }
            append("⏰ Triggered just now on BraxTradings")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SYMBOL", alert.symbol)
            putExtra("EXTRA_ALERT_ID", alert.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(if (alert.condition == AlertCondition.CROSSES_ABOVE) 0xFF00E676.toInt() else 0xFFFF3B30.toInt())
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(alert.id.hashCode(), notificationBuilder.build())
        } catch (e: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not yet granted
            e.printStackTrace()
        }
    }

    fun sendTestNotification(symbol: String = "BTC/USDT", targetPrice: Double = 65000.0, currentPrice: Double = 65020.50) {
        val testAlert = StockPriceAlert(
            id = "test_${System.currentTimeMillis()}",
            symbol = symbol,
            instrumentName = "Sample Asset",
            targetPrice = targetPrice,
            initialPriceAtCreation = targetPrice - 100,
            condition = AlertCondition.CROSSES_ABOVE,
            note = "Testing Local Notification System",
            isEnabled = true
        )
        triggerPriceAlertNotification(testAlert, currentPrice)
    }
}
