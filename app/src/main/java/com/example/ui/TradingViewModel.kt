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
    val priceDirections = marketEngine.priceDirections
    val tickerStreamInfo = marketEngine.tickerStreamInfo
    val streamIntervalMs = marketEngine.streamIntervalMs
    val transportMode = marketEngine.transportMode

    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.M5)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

    private val _balance = MutableStateFlow(28500.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _dailyLossLimit = MutableStateFlow(2500.0)
    val dailyLossLimit: StateFlow<Double> = _dailyLossLimit.asStateFlow()

    private val _positions = MutableStateFlow<List<Position>>(
        listOf(
            Position(
                id = "pos_nvda_long",
                symbol = "NVDA",
                side = OrderSide.BUY,
                quantity = 120.0,
                entryPrice = 122.50,
                currentPrice = 124.80,
                pnl = 276.0,
                pnlPercentage = 3.75,
                leverage = 2.0
            ),
            Position(
                id = "pos_btc_long",
                symbol = "BTC/USDT",
                side = OrderSide.BUY,
                quantity = 0.40,
                entryPrice = 63800.00,
                currentPrice = 64450.00,
                pnl = 260.0,
                pnlPercentage = 5.09,
                leverage = 5.0
            ),
            Position(
                id = "pos_tsla_short",
                symbol = "TSLA",
                side = OrderSide.SELL,
                quantity = 50.0,
                entryPrice = 222.00,
                currentPrice = 218.60,
                pnl = 170.0,
                pnlPercentage = 3.06,
                leverage = 2.0
            ),
            Position(
                id = "pos_aapl_long",
                symbol = "AAPL",
                side = OrderSide.BUY,
                quantity = 45.0,
                entryPrice = 227.10,
                currentPrice = 228.40,
                pnl = 58.5,
                pnlPercentage = 0.57,
                leverage = 1.0
            )
        )
    )
    val positions: StateFlow<List<Position>> = _positions.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Color palette for Recharts exposure slices
    private val sliceColors = listOf(
        0xFF00E5FF, // Accent Cyan
        0xFF00E676, // Bright Green
        0xFFFFAB00, // Gold / Amber
        0xFFFF5252, // Coral Red
        0xFF7C4DFF, // Deep Purple
        0xFF40C4FF, // Light Blue
        0xFFFF6E40  // Deep Orange
    )

    // Derived StateFlows for Day Trader Risk & Exposure
    val positionRiskAssessments: StateFlow<List<PositionRiskAssessment>> = combine(
        _positions,
        instruments
    ) { posList, instList ->
        val instMap = instList.associateBy { it.symbol }
        val totalNotionalAll = posList.sumOf { it.currentPrice * it.quantity }

        posList.map { pos ->
            val inst = instMap[pos.symbol]
            val notional = pos.currentPrice * pos.quantity
            val marginAllocated = notional / pos.leverage
            val sharePct = if (totalNotionalAll > 0) (notional / totalNotionalAll) * 100.0 else 0.0

            // Liquidation estimation formula based on leverage & side
            // Long liq ~ entryPrice * (1 - (1/leverage) * 0.90)
            // Short liq ~ entryPrice * (1 + (1/leverage) * 0.90)
            val liqPrice = if (pos.side == OrderSide.BUY) {
                pos.entryPrice * (1.0 - (1.0 / pos.leverage) * 0.90).coerceAtLeast(0.01)
            } else {
                pos.entryPrice * (1.0 + (1.0 / pos.leverage) * 0.90)
            }

            val distToLiqPct = if (pos.side == OrderSide.BUY) {
                ((pos.currentPrice - liqPrice) / pos.currentPrice * 100.0).coerceAtLeast(0.0)
            } else {
                ((liqPrice - pos.currentPrice) / pos.currentPrice * 100.0).coerceAtLeast(0.0)
            }

            // 1-Day 95% Parametric VaR estimation (~ 1.65 * 2.5% daily vol * notional * sqrt(leverage))
            val var95 = notional * 0.038 * kotlin.math.sqrt(pos.leverage)
            val riskScore = ((100.0 - distToLiqPct * 2.5) + (pos.leverage * 8.0)).toInt().coerceIn(5, 99)

            PositionRiskAssessment(
                position = pos,
                instrument = inst,
                notionalExposure = notional,
                marginAllocated = marginAllocated,
                exposureSharePct = sharePct,
                liquidationPrice = liqPrice,
                distToLiquidationPct = distToLiqPct,
                var95Risk = var95,
                riskScore = riskScore
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val exposureSlices: StateFlow<List<ExposureSlice>> = positionRiskAssessments
        .map { assessments ->
            val totalNotional = assessments.sumOf { it.notionalExposure }
            assessments.mapIndexed { idx, item ->
                val color = sliceColors[idx % sliceColors.size]
                ExposureSlice(
                    id = item.position.id,
                    label = item.position.symbol,
                    subLabel = if (item.position.side == OrderSide.BUY) "Long ${item.position.leverage.toInt()}x" else "Short ${item.position.leverage.toInt()}x",
                    notionalValue = item.notionalExposure,
                    percentage = if (totalNotional > 0) (item.notionalExposure / totalNotional) * 100.0 else 0.0,
                    colorHex = color
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accountRiskMetrics: StateFlow<AccountRiskMetrics> = combine(
        _balance,
        _positions,
        _dailyLossLimit,
        positionRiskAssessments
    ) { balance, positions, lossLimit, assessments ->
        val totalUnrealizedPnL = positions.sumOf { it.pnl }
        val usedMargin = positions.sumOf { (it.entryPrice * it.quantity) / it.leverage }
        val totalEquity = balance + usedMargin + totalUnrealizedPnL
        val freeMargin = (totalEquity - usedMargin).coerceAtLeast(0.0)

        val marginUtilPct = if (totalEquity > 0) (usedMargin / totalEquity * 100.0).coerceIn(0.0, 150.0) else 100.0
        val grossExposure = assessments.sumOf { it.notionalExposure }

        val longExposure = assessments.filter { it.position.side == OrderSide.BUY }.sumOf { it.notionalExposure }
        val shortExposure = assessments.filter { it.position.side == OrderSide.SELL }.sumOf { it.notionalExposure }
        val netExposure = longExposure - shortExposure

        val effectiveLeverage = if (totalEquity > 0) grossExposure / totalEquity else 1.0
        val totalVaR95 = assessments.sumOf { it.var95Risk }

        val lossUtilPct = if (totalUnrealizedPnL < 0) {
            (kotlin.math.abs(totalUnrealizedPnL) / lossLimit * 100.0).coerceIn(0.0, 150.0)
        } else {
            0.0
        }

        val minLiqDist = assessments.minOfOrNull { it.distToLiquidationPct } ?: 100.0

        val level = when {
            marginUtilPct >= 90.0 || minLiqDist < 5.0 -> RiskAlertLevel.CRITICAL_MARGIN_CALL
            marginUtilPct >= 75.0 || minLiqDist < 12.0 -> RiskAlertLevel.HIGH_RISK
            marginUtilPct >= 50.0 -> RiskAlertLevel.ELEVATED
            else -> RiskAlertLevel.SAFE
        }

        AccountRiskMetrics(
            totalEquity = totalEquity,
            usedMargin = usedMargin,
            freeMargin = freeMargin,
            marginUtilizationPct = marginUtilPct,
            grossNotionalExposure = grossExposure,
            netExposure = netExposure,
            longExposure = longExposure,
            shortExposure = shortExposure,
            effectiveLeverage = effectiveLeverage,
            valueAtRisk95 = totalVaR95,
            dailyLossLimit = lossLimit,
            currentDailyPnL = totalUnrealizedPnL,
            dailyLossUtilizationPct = lossUtilPct,
            riskLevel = level,
            closestLiquidationDistancePct = minLiqDist
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountRiskMetrics(
            totalEquity = 50000.0,
            usedMargin = 0.0,
            freeMargin = 50000.0,
            marginUtilizationPct = 0.0,
            grossNotionalExposure = 0.0,
            netExposure = 0.0,
            longExposure = 0.0,
            shortExposure = 0.0,
            effectiveLeverage = 1.0,
            valueAtRisk95 = 0.0,
            dailyLossLimit = 2500.0,
            currentDailyPnL = 0.0,
            dailyLossUtilizationPct = 0.0,
            riskLevel = RiskAlertLevel.SAFE,
            closestLiquidationDistancePct = 100.0
        )
    )

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
        price: Double? = null,
        leverage: Double = 1.0
    ) {
        val currentPrice = instruments.value.find { it.symbol == symbol }?.currentPrice ?: return
        val executionPrice = if (type == OrderType.LIMIT && price != null) price else currentPrice
        val requiredMargin = (executionPrice * quantity) / leverage

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
            price = executionPrice,
            status = "FILLED"
        )
        _orders.value = listOf(order) + _orders.value
        _balance.value -= requiredMargin

        val newPosition = Position(
            id = UUID.randomUUID().toString().take(8),
            symbol = symbol,
            side = side,
            quantity = quantity,
            entryPrice = executionPrice,
            currentPrice = currentPrice,
            pnl = 0.0,
            pnlPercentage = 0.0,
            leverage = leverage
        )
        _positions.value = listOf(newPosition) + _positions.value

        viewModelScope.launch {
            _toastMessage.emit("✅ ${type.name} Order filled: ${side.name} $quantity $symbol @ \$${String.format("%.2f", executionPrice)}")
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

    fun reducePositionExposure(positionId: String, reductionPct: Double = 0.5) {
        val pos = _positions.value.find { it.id == positionId } ?: return
        val reduceQty = pos.quantity * reductionPct
        val remainingQty = pos.quantity - reduceQty

        val closedMargin = (pos.entryPrice * reduceQty) / pos.leverage
        val realizedPnL = pos.pnl * reductionPct
        _balance.value += (closedMargin + realizedPnL)

        if (remainingQty <= 0.001) {
            _positions.value = _positions.value.filter { it.id != positionId }
        } else {
            _positions.value = _positions.value.map {
                if (it.id == positionId) {
                    it.copy(
                        quantity = remainingQty,
                        pnl = pos.pnl * (1.0 - reductionPct)
                    )
                } else it
            }
        }

        viewModelScope.launch {
            _toastMessage.emit("🛡️ De-risked ${pos.symbol} by ${(reductionPct * 100).toInt()}% (Freed \$${String.format("%.2f", closedMargin)})")
        }
    }

    fun emergencyDeRiskAll() {
        val currentPositions = _positions.value
        if (currentPositions.isEmpty()) return

        var freedMarginTotal = 0.0
        val updated = currentPositions.map { pos ->
            val cutQty = pos.quantity * 0.5
            val marginFreed = (pos.entryPrice * cutQty) / pos.leverage
            val realizedPnL = pos.pnl * 0.5
            freedMarginTotal += (marginFreed + realizedPnL)

            pos.copy(
                quantity = pos.quantity * 0.5,
                pnl = pos.pnl * 0.5
            )
        }

        _balance.value += freedMarginTotal
        _positions.value = updated

        viewModelScope.launch {
            _toastMessage.emit("🚨 Emergency De-Risk: 50% Exposure Cut Across All Assets! Freed \$${String.format("%,.2f", freedMarginTotal)}")
        }
    }

    fun updateDailyLossLimit(newLimit: Double) {
        _dailyLossLimit.value = newLimit.coerceAtLeast(100.0)
        viewModelScope.launch {
            _toastMessage.emit("🛡️ Daily Loss Circuit Breaker updated to \$${String.format("%,.0f", newLimit)}")
        }
    }

    fun setStreamInterval(intervalMs: Long) {
        marketEngine.setStreamInterval(intervalMs)
    }

    fun setTransportMode(mode: StreamTransportMode) {
        marketEngine.setTransportMode(mode)
        viewModelScope.launch {
            _toastMessage.emit("Switched feed to ${mode.label}")
        }
    }

    fun refreshTickNow() {
        marketEngine.refreshTickNow()
    }
}

