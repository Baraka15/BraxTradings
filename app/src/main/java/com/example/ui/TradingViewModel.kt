package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.market.MarketDataEngine
import com.example.data.notification.PriceAlertNotificationManager
import com.example.domain.trading.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val marketEngine = MarketDataEngine()
    private val notificationManager = PriceAlertNotificationManager(application)

    private val _quotes = MutableStateFlow<List<TickerQuote>>(marketEngine.availableSymbols)
    val quotes: StateFlow<List<TickerQuote>> = _quotes.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("BTC/USD")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.M15)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLES)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

    private val _activeIndicators = MutableStateFlow<Set<TechnicalIndicator>>(
        setOf(TechnicalIndicator.EMA_21, TechnicalIndicator.VOLUME, TechnicalIndicator.RSI)
    )
    val activeIndicators: StateFlow<Set<TechnicalIndicator>> = _activeIndicators.asStateFlow()

    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.NONE)
    val activeDrawingTool: StateFlow<DrawingToolType> = _activeDrawingTool.asStateFlow()

    private val _drawings = MutableStateFlow<List<DrawingShape>>(emptyList())
    val drawings: StateFlow<List<DrawingShape>> = _drawings.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    private val _orderBookBids = MutableStateFlow<List<OrderBookLevel>>(emptyList())
    val orderBookBids: StateFlow<List<OrderBookLevel>> = _orderBookBids.asStateFlow()

    private val _orderBookAsks = MutableStateFlow<List<OrderBookLevel>>(emptyList())
    val orderBookAsks: StateFlow<List<OrderBookLevel>> = _orderBookAsks.asStateFlow()

    private val _recentTrades = MutableStateFlow<List<MarketTrade>>(emptyList())
    val recentTrades: StateFlow<List<MarketTrade>> = _recentTrades.asStateFlow()

    private val _alerts = MutableStateFlow<List<PriceAlert>>(
        listOf(
            PriceAlert(UUID.randomUUID().toString(), "BTC/USD", 70000.0, true, note = "Breakout resistance"),
            PriceAlert(UUID.randomUUID().toString(), "BTC/USD", 65000.0, false, note = "Support dip buy"),
            PriceAlert(UUID.randomUUID().toString(), "ETH/USD", 3800.0, true, note = "Major high retest")
        )
    )
    val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

    private val _recentTriggeredAlert = MutableStateFlow<PriceAlert?>(null)
    val recentTriggeredAlert: StateFlow<PriceAlert?> = _recentTriggeredAlert.asStateFlow()

    private val _portfolio = MutableStateFlow<List<PortfolioPosition>>(
        listOf(
            PortfolioPosition("BTC/USD", "Bitcoin", 0.45, 62100.0, 67420.50, stopLoss = 64000.0, takeProfit = 72000.0),
            PortfolioPosition("ETH/USD", "Ethereum", 4.20, 3120.0, 3540.80, stopLoss = 3300.0, takeProfit = 3900.0),
            PortfolioPosition("SOL/USD", "Solana", 35.0, 142.0, 178.45, stopLoss = 160.0, takeProfit = 210.0),
            PortfolioPosition("NVDA", "NVIDIA Corp", 50.0, 115.0, 128.60, stopLoss = 120.0, takeProfit = 145.0)
        )
    )
    val portfolio: StateFlow<List<PortfolioPosition>> = _portfolio.asStateFlow()

    init {
        loadSymbolData(_selectedSymbol.value, _timeframe.value)
        startLiveDataLoop()
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        loadSymbolData(symbol, _timeframe.value)
    }

    fun setTimeframe(tf: Timeframe) {
        _timeframe.value = tf
        loadSymbolData(_selectedSymbol.value, tf)
    }

    fun setChartStyle(style: ChartStyle) {
        _chartStyle.value = style
    }

    fun toggleIndicator(indicator: TechnicalIndicator) {
        val current = _activeIndicators.value.toMutableSet()
        if (current.contains(indicator)) {
            current.remove(indicator)
        } else {
            current.add(indicator)
        }
        _activeIndicators.value = current
    }

    fun setDrawingTool(tool: DrawingToolType) {
        _activeDrawingTool.value = tool
    }

    fun addDrawing(shape: DrawingShape) {
        _drawings.value = _drawings.value + shape
    }

    fun clearDrawings() {
        _drawings.value = emptyList()
    }

    fun createAlert(symbol: String, targetPrice: Double, isAbove: Boolean, note: String) {
        val alert = PriceAlert(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            targetPrice = targetPrice,
            isAbove = isAbove,
            note = note
        )
        _alerts.value = listOf(alert) + _alerts.value
    }

    fun deleteAlert(id: String) {
        _alerts.value = _alerts.value.filterNot { it.id == id }
    }

    fun dismissBanner() {
        _recentTriggeredAlert.value = null
    }

    private fun loadSymbolData(symbol: String, tf: Timeframe) {
        val c = marketEngine.generateHistoricalCandles(symbol, tf)
        _candles.value = c
        val currentPrice = c.lastOrNull()?.close ?: 100.0
        val (bids, asks) = marketEngine.generateOrderBook(currentPrice)
        _orderBookBids.value = bids
        _orderBookAsks.value = asks
        _recentTrades.value = marketEngine.generateRecentTrades(currentPrice)
    }

    private fun startLiveDataLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000)
                val currentSymbol = _selectedSymbol.value
                val currentQuote = _quotes.value.find { it.symbol == currentSymbol } ?: continue
                val pctDelta = (kotlin.random.Random.nextDouble() - 0.495) * 0.003
                val newPrice = (currentQuote.currentPrice * (1 + pctDelta)).coerceAtLeast(0.01)

                // update quotes
                _quotes.value = _quotes.value.map { q ->
                    if (q.symbol == currentSymbol) {
                        val diff = newPrice - (q.currentPrice - q.change)
                        val pct = (diff / (q.currentPrice - q.change)) * 100
                        q.copy(
                            currentPrice = newPrice,
                            change = diff,
                            changePercent = pct,
                            high24h = maxOf(q.high24h, newPrice),
                            low24h = minOf(q.low24h, newPrice)
                        )
                    } else q
                }

                // update portfolio current prices
                _portfolio.value = _portfolio.value.map { pos ->
                    if (pos.symbol == currentSymbol) pos.copy(currentPrice = newPrice) else pos
                }

                // update active candle
                val cList = _candles.value.toMutableList()
                if (cList.isNotEmpty()) {
                    val last = cList.last()
                    val updatedLast = last.copy(
                        high = maxOf(last.high, newPrice),
                        low = minOf(last.low, newPrice),
                        close = newPrice,
                        volume = last.volume + kotlin.random.Random.nextDouble(5.0, 50.0)
                    )
                    cList[cList.lastIndex] = updatedLast
                    _candles.value = cList
                }

                // orderbook & trades
                val (bids, asks) = marketEngine.generateOrderBook(newPrice)
                _orderBookBids.value = bids
                _orderBookAsks.value = asks
                val newTrade = MarketTrade(
                    id = UUID.randomUUID().toString().take(8),
                    price = newPrice,
                    quantity = kotlin.random.Random.nextDouble(0.05, 1.8),
                    isBuyerMaker = kotlin.random.Random.nextBoolean(),
                    timestamp = System.currentTimeMillis()
                )
                _recentTrades.value = (listOf(newTrade) + _recentTrades.value).take(20)

                // check alerts
                checkAlertTriggers(currentSymbol, newPrice)
            }
        }
    }

    private fun checkAlertTriggers(symbol: String, price: Double) {
        val updatedAlerts = _alerts.value.map { alert ->
            if (!alert.triggered && alert.symbol == symbol) {
                val conditionMet = if (alert.isAbove) price >= alert.targetPrice else price <= alert.targetPrice
                if (conditionMet) {
                    val triggeredAlert = alert.copy(triggered = true, triggeredAt = System.currentTimeMillis())
                    notificationManager.dispatchAlertNotification(triggeredAlert, price)
                    _recentTriggeredAlert.value = triggeredAlert
                    triggeredAlert
                } else alert
            } else alert
        }
        _alerts.value = updatedAlerts
    }
}
