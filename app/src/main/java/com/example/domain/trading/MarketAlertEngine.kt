package com.example.domain.trading

import com.example.data.local.AlertDao
import com.example.data.local.AlertEntity
import com.example.data.market.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

data class TriggeredAlertNotification(
    val alertId: String,
    val symbol: String,
    val conditionText: String,
    val triggerPrice: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MarketAlertEngine(
    private val alertDao: AlertDao
) {
    private val _alertEvents = MutableSharedFlow<TriggeredAlertNotification>(extraBufferCapacity = 32)
    val alertEvents: SharedFlow<TriggeredAlertNotification> = _alertEvents.asSharedFlow()

    suspend fun evaluateAlerts(activeAlerts: List<AlertEntity>, quotes: Map<String, Quote>) = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()

        for (alert in activeAlerts) {
            if (!alert.isActive || alert.isTriggered) continue
            val quote = quotes[alert.symbol] ?: continue

            var isTriggered = false
            var conditionDesc = ""

            when (alert.conditionType) {
                "PRICE_ABOVE" -> {
                    if (quote.price >= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "Price broke ABOVE $${"%.2f".format(alert.targetValue)} (Current: $${"%.2f".format(quote.price)})"
                    }
                }
                "PRICE_BELOW" -> {
                    if (quote.price <= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "Price dropped BELOW $${"%.2f".format(alert.targetValue)} (Current: $${"%.2f".format(quote.price)})"
                    }
                }
                "PCT_GAIN" -> {
                    if (quote.changePercent >= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "Day Gain surged +${"%.2f".format(quote.changePercent)}% (Target: +${"%.2f".format(alert.targetValue)}%)"
                    }
                }
                "PCT_DROP" -> {
                    if (quote.changePercent <= -alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "Day Loss exceeded -${"%.2f".format(alert.targetValue)}% (Current: ${"%.2f".format(quote.changePercent)}%)"
                    }
                }
                "RSI_OVERBOUGHT" -> {
                    if (quote.rsi14 >= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "RSI reached ${"%.1f".format(quote.rsi14)} (Overbought threshold ${alert.targetValue.toInt()})"
                    }
                }
                "RSI_OVERSOLD" -> {
                    if (quote.rsi14 <= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "RSI dipped to ${"%.1f".format(quote.rsi14)} (Oversold threshold ${alert.targetValue.toInt()})"
                    }
                }
                "MACD_CROSSOVER" -> {
                    if (quote.macdHistogram > 0.05 && quote.macdLine > quote.macdSignal) {
                        isTriggered = true
                        conditionDesc = "Bullish MACD Golden Cross confirmed on ${quote.symbol}"
                    }
                }
                "VOL_SPIKE" -> {
                    if (quote.volume >= alert.targetValue) {
                        isTriggered = true
                        conditionDesc = "Volume spiked to ${quote.volume} shares"
                    }
                }
            }

            if (isTriggered) {
                withContext(Dispatchers.IO) {
                    alertDao.markAlertTriggered(alert.alertId, now, quote.price)
                }
                _alertEvents.emit(
                    TriggeredAlertNotification(
                        alertId = alert.alertId,
                        symbol = alert.symbol,
                        conditionText = conditionDesc,
                        triggerPrice = quote.price,
                        note = alert.note,
                        timestamp = now
                    )
                )
            }
        }
    }
}
