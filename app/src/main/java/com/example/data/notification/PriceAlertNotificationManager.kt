package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.domain.trading.PriceAlert
import java.util.Locale

class PriceAlertNotificationManager(private val context: Context) {

    private val channelId = "price_alerts_channel"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Price Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live triggers for target stock & crypto prices"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun dispatchAlertNotification(alert: PriceAlert, currentPrice: Double) {
        val direction = if (alert.isAbove) "surpassed" else "dropped below"
        val formattedTarget = String.format(Locale.US, "$%.2f", alert.targetPrice)
        val formattedCurrent = String.format(Locale.US, "$%.2f", currentPrice)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle("🚨 Price Alert: ${alert.symbol}")
            .setContentText("${alert.symbol} has $direction $formattedTarget! (Now: $formattedCurrent)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alert.id.hashCode(), notification)
    }
}
