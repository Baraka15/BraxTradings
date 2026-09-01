package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.market.*
import com.example.domain.trading.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class IndicatorToggles(
    val showEMA: Boolean = true,
    val showEMA21: Boolean = true,
    val showSMA50: Boolean = false,
    val showBollinger: Boolean = true,
    val showVWAP: Boolean = true,
    val showVolume: Boolean = true,
    val showSupertrend: Boolean = false,
    val showPivotPoints: Boolean = false,
    val showOrderLines: Boolean = true,
    val subIndicator: SubIndicator = SubIndicator.RSI
)

enum class SubIndicator(val label: String) {
    RSI("RSI (14)"),
    MACD("MACD (12,26,9)"),
    STOCHASTIC("Stoch (14,3,3)"),
    NONE("Hide Sub")
}

sealed class UiMessage {
    data class Success(val message: String) : UiMessage()
    data class Error(val message: String) : UiMessage()
    data class TradeFilled(val fill: OrderFill) : UiMessage()
}

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val positionDao = database.positionDao()
    private val orderDao = database.orderDao()
    private val tradeLedgerDao = database.tradeLedgerDao()
    private val alertDao = database.alertDao()
    private val watchlistDao = database.watchlistDao()
    private val accountDao = database.accountDao()

    val marketEngine = MarketDataEngine(viewModelScope)
    val realMarketDataService = RealMarketDataService(viewModelScope)
    val priceTicks: StateFlow<Map<String, PriceTick>> = realMarketDataService.priceTicks
    val latestTick: StateFlow<PriceTick?> = realMarketDataService.latestTick
    private val riskEngine = RiskEngine()
    val ledgerService = LedgerService(database)
    val alertEngine = MarketAlertEngine(alertDao)

    private val matchingEngine = MatchingEngine { fill, order ->
        val trade = ledgerService.settleOrderFill(fill, order)
        _uiMessages.emit(UiMessage.TradeFilled(fill))
    }

    // Active State
    private val _selectedSymbol = MutableStateFlow("NVDA")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.M1)
    val selectedTimeFrame: StateFlow<TimeFrame> = _selectedTimeFrame.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLESTICK)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.CURSOR)
    val activeDrawingTool: StateFlow<DrawingToolType> = _activeDrawingTool.asStateFlow()

    private val _drawings = MutableStateFlow<Map<String, List<DrawingItem>>>(emptyMap())
    val drawings: StateFlow<Map<String, List<DrawingItem>>> = _drawings.asStateFlow()

    private val _indicatorToggles = MutableStateFlow(IndicatorToggles())
    val indicatorToggles: StateFlow<IndicatorToggles> = _indicatorToggles.asStateFlow()

    private val _uiMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
    val uiMessages: SharedFlow<UiMessage> = _uiMessages.asSharedFlow()

    // Database reactive streams
    val positions: StateFlow<List<PositionEntity>> = positionDao.getAllPositions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<OrderEntity>> = orderDao.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingOrders: StateFlow<List<OrderEntity>> = orderDao.getPendingOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tradeHistory: StateFlow<List<TradeLedgerEntity>> = tradeLedgerDao.getAllTrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<AlertEntity>> = alertDao.getAllAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<WatchlistItemEntity>> = watchlistDao.getAllWatchlistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountState: StateFlow<AccountEntity?> = accountDao.getAccountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Derived Portfolio Summary
    val portfolioSummary: StateFlow<PortfolioSummary> = combine(
        accountState,
        positions,
        tradeHistory,
        marketEngine.quotes
    ) { account, posList, trades, quotes ->
        ledgerService.computePortfolioSummary(account, posList, trades, quotes)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PortfolioSummary(100000.0, 100000.0, 0.0, 400000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0)
    )

    // Current Quote for selected symbol
    val currentQuote: StateFlow<Quote?> = combine(
        _selectedSymbol,
        marketEngine.quotes
    ) { sym, quotesMap ->
        quotesMap[sym]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Price Tick for selected symbol
    val currentTick: StateFlow<PriceTick?> = combine(
        _selectedSymbol,
        realMarketDataService.priceTicks
    ) { sym, ticksMap ->
        ticksMap[sym]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Candlesticks for selected symbol and timeframe
    val currentCandles: StateFlow<List<CandleStick>> = combine(
        _selectedSymbol,
        _selectedTimeFrame,
        marketEngine.candles
    ) { sym, tf, candlesMap ->
        candlesMap[sym]?.get(tf) ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current OrderBook for selected symbol
    val currentOrderBook: StateFlow<OrderBook?> = combine(
        _selectedSymbol,
        marketEngine.orderBooks
    ) { sym, books ->
        books[sym]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current position for selected symbol
    val currentPosition: StateFlow<PositionEntity?> = combine(
        _selectedSymbol,
        positions
    ) { sym, posList ->
        posList.firstOrNull { it.symbol == sym }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Drawings for selected symbol
    val currentDrawings: StateFlow<List<DrawingItem>> = combine(
        _selectedSymbol,
        _drawings
    ) { sym, map ->
        map[sym] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Synchronize pending orders into matching engine
        viewModelScope.launch {
            pendingOrders.collect { orders ->
                matchingEngine.syncPendingOrders(orders)
            }
        }

        // Evaluate live ticks against matching engine and alert engine
        viewModelScope.launch {
            marketEngine.quotes.collect { quotesMap ->
                val activeAlerts = alerts.value.filter { it.isActive && !it.isTriggered }
                if (activeAlerts.isNotEmpty()) {
                    alertEngine.evaluateAlerts(activeAlerts, quotesMap)
                }
                quotesMap.values.forEach { quote ->
                    matchingEngine.processQuoteTick(quote)
                }
            }
        }

        // Real-time alert notifications stream to UI toast
        viewModelScope.launch {
            alertEngine.alertEvents.collect { triggered ->
                _uiMessages.emit(
                    UiMessage.Success("🚨 ALERT TRIGGERED: ${triggered.symbol} - ${triggered.conditionText}")
                )
            }
        }
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
    }

    fun selectTimeFrame(tf: TimeFrame) {
        _selectedTimeFrame.value = tf
    }

    fun setChartStyle(style: ChartStyle) {
        _chartStyle.value = style
    }

    fun setActiveDrawingTool(tool: DrawingToolType) {
        _activeDrawingTool.value = tool
    }

    fun addDrawing(drawing: DrawingItem) {
        val sym = _selectedSymbol.value
        val list = _drawings.value[sym] ?: emptyList()
        _drawings.value = _drawings.value + (sym to (list + drawing))
    }

    fun deleteDrawing(id: String) {
        val sym = _selectedSymbol.value
        val list = _drawings.value[sym] ?: return
        _drawings.value = _drawings.value + (sym to list.filter { it.id != id })
    }

    fun clearDrawingsForCurrentSymbol() {
        val sym = _selectedSymbol.value
        _drawings.value = _drawings.value + (sym to emptyList())
        _uiMessages.tryEmit(UiMessage.Success("Cleared drawings on $sym"))
    }

    fun toggleEMA() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showEMA = !_indicatorToggles.value.showEMA)
    }

    fun toggleEMA21() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showEMA21 = !_indicatorToggles.value.showEMA21)
    }

    fun toggleSMA50() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showSMA50 = !_indicatorToggles.value.showSMA50)
    }

    fun toggleBollinger() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showBollinger = !_indicatorToggles.value.showBollinger)
    }

    fun toggleVWAP() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showVWAP = !_indicatorToggles.value.showVWAP)
    }

    fun toggleSupertrend() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showSupertrend = !_indicatorToggles.value.showSupertrend)
    }

    fun togglePivotPoints() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showPivotPoints = !_indicatorToggles.value.showPivotPoints)
    }

    fun toggleOrderLines() {
        _indicatorToggles.value = _indicatorToggles.value.copy(showOrderLines = !_indicatorToggles.value.showOrderLines)
    }

    fun setSubIndicator(sub: SubIndicator) {
        _indicatorToggles.value = _indicatorToggles.value.copy(subIndicator = sub)
    }

    fun updateIndicatorToggles(toggles: IndicatorToggles) {
        _indicatorToggles.value = toggles
    }

    fun cycleSubIndicator() {
        val current = _indicatorToggles.value.subIndicator
        val next = when (current) {
            SubIndicator.RSI -> SubIndicator.MACD
            SubIndicator.MACD -> SubIndicator.STOCHASTIC
            SubIndicator.STOCHASTIC -> SubIndicator.NONE
            SubIndicator.NONE -> SubIndicator.RSI
        }
        _indicatorToggles.value = _indicatorToggles.value.copy(subIndicator = next)
    }

    /**
     * Places an order after pre-trade risk engine validation
     */
    fun placeOrder(request: OrderRequest) {
        viewModelScope.launch {
            val account = accountState.value ?: AccountEntity()
            val quote = marketEngine.quotes.value[request.symbol]
            val pos = currentPosition.value

            // 1. Synchronous Pre-Trade Risk Engine Check
            when (val riskResult = riskEngine.validateOrder(request, account, quote, pos)) {
                is RiskValidationResult.Rejected -> {
                    _uiMessages.emit(UiMessage.Error("Risk Engine Rejected: ${riskResult.reason}"))
                    return@launch
                }
                is RiskValidationResult.Approved -> {
                    // Risk checks passed
                }
            }

            if (quote == null) return@launch

            val orderId = "ORD_${UUID.randomUUID().toString().take(8).uppercase()}"
            val initialStatus = if (request.type == OrderType.MARKET) OrderStatus.FILLED.name else OrderStatus.PENDING.name

            val orderEntity = OrderEntity(
                orderId = orderId,
                symbol = request.symbol,
                side = request.side.name,
                type = request.type.name,
                quantity = request.quantity,
                limitPrice = request.limitPrice,
                stopPrice = request.stopPrice,
                status = initialStatus,
                createdAt = System.currentTimeMillis()
            )

            // Save order to database
            orderDao.insertOrder(orderEntity)

            if (request.type == OrderType.MARKET) {
                // Immediate fill via matching engine
                matchingEngine.executeMarketOrder(orderEntity, quote)
            } else {
                // Register in-memory working order for price matching
                matchingEngine.registerOrder(orderEntity)
                _uiMessages.emit(UiMessage.Success("Limit order placed: ${request.side.label} ${request.quantity} ${request.symbol} @ $${request.limitPrice ?: request.stopPrice}"))
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            matchingEngine.cancelOrder(orderId)
            val updated = orderDao.cancelOrder(orderId)
            if (updated > 0) {
                _uiMessages.emit(UiMessage.Success("Order #$orderId cancelled."))
            }
        }
    }

    fun marketClosePosition(symbol: String) {
        viewModelScope.launch {
            val pos = positionDao.getPositionBySymbol(symbol) ?: return@launch
            if (pos.shares <= 0) return@launch

            val request = OrderRequest(
                symbol = symbol,
                side = OrderSide.SELL,
                type = OrderType.MARKET,
                quantity = pos.shares
            )
            placeOrder(request)
        }
    }

    fun createAlert(symbol: String, conditionType: String, targetValue: Double, note: String) {
        viewModelScope.launch {
            val alert = AlertEntity(
                alertId = "ALT_${UUID.randomUUID().toString().take(8).uppercase()}",
                symbol = symbol,
                conditionType = conditionType,
                targetValue = targetValue,
                note = note,
                isActive = true,
                isTriggered = false
            )
            alertDao.insertAlert(alert)
            _uiMessages.emit(UiMessage.Success("Alert created for $symbol!"))
        }
    }

    fun toggleAlert(alertId: String, isActive: Boolean) {
        viewModelScope.launch {
            alertDao.toggleAlertActive(alertId, isActive)
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            alertDao.deleteAlertById(alertId)
            _uiMessages.emit(UiMessage.Success("Alert deleted."))
        }
    }

    fun toggleWatchlist(symbol: String, name: String, assetType: AssetType) {
        viewModelScope.launch {
            val exists = watchlistDao.containsSymbol(symbol) > 0
            if (exists) {
                watchlistDao.removeFromWatchlist(symbol)
                _uiMessages.emit(UiMessage.Success("Removed $symbol from watchlist."))
            } else {
                watchlistDao.insertWatchlistItem(
                    WatchlistItemEntity(
                        symbol = symbol,
                        name = name,
                        assetType = assetType.name,
                        isFavorite = true
                    )
                )
                _uiMessages.emit(UiMessage.Success("Added $symbol to watchlist."))
            }
        }
    }

    fun setSimulationSpeed(speedMs: Long) {
        marketEngine.setTickSpeed(speedMs)
        realMarketDataService.setTickSpeed(speedMs)
    }

    fun toggleStreaming() {
        marketEngine.toggleStreaming()
        realMarketDataService.toggleStreaming()
    }

    fun updateLeverage(leverage: Double) {
        viewModelScope.launch {
            accountDao.updateLeverage(leverage)
            _uiMessages.emit(UiMessage.Success("Leverage updated to ${leverage.toInt()}x."))
        }
    }

    fun resetPaperTradingBalance(amount: Double) {
        viewModelScope.launch {
            ledgerService.resetAccountBalance(amount)
            _uiMessages.emit(UiMessage.Success("Account reset with $%,.2f paper trading capital.".format(amount)))
        }
    }
}
