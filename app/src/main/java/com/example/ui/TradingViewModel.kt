package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.market.MarketDataEngine
import com.example.data.notification.PriceAlertNotificationManager
import com.example.domain.trading.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TradingViewModel(application: Application) : AndroidViewModel(application) {
    private val marketEngine = MarketDataEngine()
    val notificationManager = PriceAlertNotificationManager(application)

    val instruments = marketEngine.instruments
    val selectedSymbol = marketEngine.selectedSymbol
    val candleHistory = marketEngine.candleHistory
    val orderBook = marketEngine.orderBook
    val recentTrades = marketEngine.recentTrades
    val isStreaming = marketEngine.isStreaming

    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.M5)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

    private val _balance = MutableStateFlow(50000.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Price Alerts list
    private val _alerts = MutableStateFlow<List<StockPriceAlert>>(
        listOf(
            StockPriceAlert(
                id = "alert_nvda_high",
                symbol = "NVDA",
                instrumentName = "NVIDIA Corporation",
                targetPrice = 126.00,
                initialPriceAtCreation = 124.80,
                condition = AlertCondition.CROSSES_ABOVE,
                note = "Breakout above resistance level",
                isEnabled = true
            ),
            StockPriceAlert(
                id = "alert_aapl_low",
                symbol = "AAPL",
                instrumentName = "Apple Inc.",
                targetPrice = 227.00,
                initialPriceAtCreation = 228.40,
                condition = AlertCondition.CROSSES_BELOW,
                note = "Dip buy trigger zone",
                isEnabled = true
            )
        )
    )
    val alerts: StateFlow<List<StockPriceAlert>> = _alerts.asStateFlow()

    // Triggered alerts history / banner queue
    private val _triggeredAlertBanner = MutableStateFlow<StockPriceAlert?>(null)
    val triggeredAlertBanner: StateFlow<StockPriceAlert?> = _triggeredAlertBanner.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Continuous price monitor loop for alerts & positions
        viewModelScope.launch {
            instruments.collect { instList ->
                evaluatePriceAlerts(instList)
                updatePositionsPnL(instList)
            }
        }
    }

    /**
     * Evaluates all enabled price alerts against the latest market prices.
     * Fires a high-priority system notification and in-app banner upon target hit.
     */
    private fun evaluatePriceAlerts(instList: List<Instrument>) {
        val priceMap = instList.associate { it.symbol to it.currentPrice }
        val currentAlerts = _alerts.value

        var hasChanges = false
        val updatedAlerts = currentAlerts.map { alert ->
            if (alert.isEnabled && !alert.isTriggered) {
                val livePrice = priceMap[alert.symbol]
                if (livePrice != null) {
                    val isHit = when (alert.condition) {
                        AlertCondition.CROSSES_ABOVE -> livePrice >= alert.targetPrice
                        AlertCondition.CROSSES_BELOW -> livePrice <= alert.targetPrice
                    }

                    if (isHit) {
                        hasChanges = true
                        // Trigger local system notification
                        notificationManager.triggerPriceAlertNotification(alert, livePrice)

                        // Trigger in-app alert banner
                        val triggeredAlert = alert.copy(
                            isTriggered = true,
                            triggeredAt = System.currentTimeMillis(),
                            triggerPrice = livePrice
                        )
                        _triggeredAlertBanner.value = triggeredAlert
                        return@map triggeredAlert
                    }
                }
            }
            alert
        }

        if (hasChanges) {
            _alerts.value = updatedAlerts
        }
    }

    private fun updatePositionsPnL(instList: List<Instrument>) {
        val instMap = instList.associateBy { it.symbol }
        val updatedPositions = _positions.value.map { pos ->
            val curPrice = instMap[pos.symbol]?.currentPrice ?: pos.currentPrice
            val rawDiff = if (pos.side == OrderSide.BUY) curPrice - pos.entryPrice else pos.entryPrice - curPrice
            val pnl = rawDiff * pos.quantity * pos.leverage
            val pnlPct = (rawDiff / pos.entryPrice) * 100.0 * pos.leverage
            pos.copy(
                currentPrice = curPrice,
                pnl = pnl,
                pnlPercentage = pnlPct
            )
        }
        _positions.value = updatedPositions
    }

    fun dismissBanner() {
        _triggeredAlertBanner.value = null
    }

    fun createPriceAlert(
        symbol: String,
        targetPrice: Double,
        condition: AlertCondition,
        note: String
    ) {
        val inst = instruments.value.find { it.symbol == symbol }
        val curPrice = inst?.currentPrice ?: targetPrice

        val newAlert = StockPriceAlert(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            instrumentName = inst?.name ?: symbol,
            targetPrice = targetPrice,
            initialPriceAtCreation = curPrice,
            condition = condition,
            note = note.trim(),
            isEnabled = true,
            isTriggered = false
        )

        _alerts.value = listOf(newAlert) + _alerts.value

        viewModelScope.launch {
            val condText = if (condition == AlertCondition.CROSSES_ABOVE) "≥" else "≤"
            _toastMessage.emit("🔔 Alert created: $symbol $condText \$${String.format("%.2f", targetPrice)}")
        }
    }

    fun toggleAlertEnabled(id: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    fun resetAlert(id: String) {
        val alert = _alerts.value.find { it.id == id } ?: return
        val currentPrice = instruments.value.find { it.symbol == alert.symbol }?.currentPrice ?: alert.targetPrice

        _alerts.value = _alerts.value.map {
            if (it.id == id) {
                it.copy(
                    isTriggered = false,
                    triggeredAt = null,
                    triggerPrice = null,
                    isEnabled = true,
                    initialPriceAtCreation = currentPrice
                )
            } else it
        }

        viewModelScope.launch {
            _toastMessage.emit("🔄 Alert reactivated for ${alert.symbol}")
        }
    }

    fun deleteAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }
    }

    fun sendTestNotification(symbol: String, targetPrice: Double) {
        val cur = instruments.value.find { it.symbol == symbol }?.currentPrice ?: targetPrice
        notificationManager.sendTestNotification(symbol, targetPrice, cur)
        viewModelScope.launch {
            _toastMessage.emit("🚀 Test notification dispatched to system tray!")
        }
    }

    fun forceTriggerPrice(symbol: String, simulatedPrice: Double) {
        marketEngine.setPriceForTesting(symbol, simulatedPrice)
    }

    fun selectSymbol(symbol: String) {
        marketEngine.selectSymbol(symbol)
    }

    fun selectTimeFrame(tf: TimeFrame) {
        _selectedTimeFrame.value = tf
    }

    fun toggleStreaming() {
        marketEngine.toggleStreaming()
    }

    fun placeOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        leverage: Double = 1.0
    ) {
        val currentPrice = instruments.value.find { it.symbol == symbol }?.currentPrice ?: return
        val requiredMargin = (currentPrice * quantity) / leverage

        if (_balance.value < requiredMargin) {
            viewModelScope.launch {
                _toastMessage.emit("❌ Insufficient margin balance!")
            }
            return
        }

        val order = Order(
            id = UUID.randomUUID().toString().take(8),
            symbol = symbol,
            side = side,
            type = type,
            quantity = quantity,
            price = currentPrice,
            status = "FILLED"
        )
        _orders.value = listOf(order) + _orders.value
        _balance.value -= requiredMargin

        val newPosition = Position(
            id = UUID.randomUUID().toString().take(8),
            symbol = symbol,
            side = side,
            quantity = quantity,
            entryPrice = currentPrice,
            currentPrice = currentPrice,
            pnl = 0.0,
            pnlPercentage = 0.0,
            leverage = leverage
        )
        _positions.value = listOf(newPosition) + _positions.value

        viewModelScope.launch {
            _toastMessage.emit("✅ Order filled: ${side.name} $quantity $symbol @ \$${String.format("%.2f", currentPrice)}")
        }
    }

    fun closePosition(positionId: String) {
        val pos = _positions.value.find { it.id == positionId } ?: return
        val margin = (pos.entryPrice * pos.quantity) / pos.leverage
        val returnAmount = margin + pos.pnl
        _balance.value += returnAmount
        _positions.value = _positions.value.filter { it.id != positionId }

        viewModelScope.launch {
            val pnlFormatted = String.format("%.2f", pos.pnl)
            _toastMessage.emit("Closed position on ${pos.symbol}. Net PnL: \$$pnlFormatted")
        }
    }
}
