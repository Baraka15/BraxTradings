package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.market.BinanceWebSocketClient
import com.example.data.market.MarketDataEngine
import com.example.data.notification.PriceAlertNotificationManager
import com.example.domain.trading.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val marketEngine = MarketDataEngine()
    private val notificationManager = PriceAlertNotificationManager(application)
    private val binanceClient = BinanceWebSocketClient()
    private val prefs: SharedPreferences = application.getSharedPreferences("brax_trading_workspace", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _quotes = MutableStateFlow<List<TickerQuote>>(marketEngine.availableSymbols)
    val quotes: StateFlow<List<TickerQuote>> = _quotes.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("BTC/USD")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.M15)
    val timeframe: StateFlow<Timeframe> = _timeframe.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLES)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

    private val _activeIndicators = MutableStateFlow<Set<TechnicalIndicator>>(
        setOf(TechnicalIndicator.EMA_21, TechnicalIndicator.VOLUME)
    )
    val activeIndicators: StateFlow<Set<TechnicalIndicator>> = _activeIndicators.asStateFlow()

    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.NONE)
    val activeDrawingTool: StateFlow<DrawingToolType> = _activeDrawingTool.asStateFlow()

    private val _drawings = MutableStateFlow<List<DrawingShape>>(emptyList())
    val drawings: StateFlow<List<DrawingShape>> = _drawings.asStateFlow()

    private val _candles = MutableStateFlow<List<Candle>>(emptyList())
    val candles: StateFlow<List<Candle>> = _candles.asStateFlow()

    // Workspace & Multi-Pane Layout System
    private val savedLayoutId = prefs.getInt("pref_layout_mode", WorkspaceLayoutMode.SINGLE.id)
    private val initialLayout = WorkspaceLayoutMode.entries.find { it.id == savedLayoutId } ?: WorkspaceLayoutMode.SINGLE
    private val _layoutMode = MutableStateFlow(initialLayout)
    val layoutMode: StateFlow<WorkspaceLayoutMode> = _layoutMode.asStateFlow()

    private val _focusedPaneId = MutableStateFlow(0)
    val focusedPaneId: StateFlow<Int> = _focusedPaneId.asStateFlow()

    private val _maximizedPaneId = MutableStateFlow<Int?>(null)
    val maximizedPaneId: StateFlow<Int?> = _maximizedPaneId.asStateFlow()

    // 4 Distinct Configurable Chart Panes
    private val _paneConfigs = MutableStateFlow(
        listOf(
            ChartPaneConfig(
                id = 0,
                symbol = prefs.getString("pane_0_sym", "BTC/USD") ?: "BTC/USD",
                timeframe = Timeframe.entries.find { it.label == prefs.getString("pane_0_tf", "15m") } ?: Timeframe.M15,
                indicators = setOf(TechnicalIndicator.EMA_21, TechnicalIndicator.VOLUME)
            ),
            ChartPaneConfig(
                id = 1,
                symbol = prefs.getString("pane_1_sym", "ETH/USD") ?: "ETH/USD",
                timeframe = Timeframe.entries.find { it.label == prefs.getString("pane_1_tf", "1h") } ?: Timeframe.H1,
                indicators = setOf(TechnicalIndicator.EMA_50, TechnicalIndicator.VOLUME)
            ),
            ChartPaneConfig(
                id = 2,
                symbol = prefs.getString("pane_2_sym", "SOL/USD") ?: "SOL/USD",
                timeframe = Timeframe.entries.find { it.label == prefs.getString("pane_2_tf", "5m") } ?: Timeframe.M5,
                indicators = setOf(TechnicalIndicator.EMA_9, TechnicalIndicator.RSI)
            ),
            ChartPaneConfig(
                id = 3,
                symbol = prefs.getString("pane_3_sym", "NVDA") ?: "NVDA",
                timeframe = Timeframe.entries.find { it.label == prefs.getString("pane_3_tf", "1D") } ?: Timeframe.D1,
                indicators = setOf(TechnicalIndicator.EMA_200, TechnicalIndicator.VOLUME)
            )
        )
    )
    val paneConfigs: StateFlow<List<ChartPaneConfig>> = _paneConfigs.asStateFlow()

    // Candlestick data for each pane independently
    private val _paneCandles = MutableStateFlow<Map<Int, List<Candle>>>(emptyMap())
    val paneCandles: StateFlow<Map<Int, List<Candle>>> = _paneCandles.asStateFlow()

    // Synchronized Crosshair
    private val savedSyncCrosshair = prefs.getBoolean("pref_sync_crosshair", true)
    private val _syncCrosshair = MutableStateFlow(savedSyncCrosshair)
    val syncCrosshair: StateFlow<Boolean> = _syncCrosshair.asStateFlow()

    private val _crosshairSyncState = MutableStateFlow(CrosshairSyncState(isEnabled = savedSyncCrosshair))
    val crosshairSyncState: StateFlow<CrosshairSyncState> = _crosshairSyncState.asStateFlow()

    // Resizable & Collapsible Side Panels
    private val savedSidebarWidth = prefs.getFloat("pref_sidebar_width", 340f)
    private val _sidebarWidth = MutableStateFlow(savedSidebarWidth)
    val sidebarWidth: StateFlow<Float> = _sidebarWidth.asStateFlow()

    private val savedSidebarCollapsed = prefs.getBoolean("pref_sidebar_collapsed", false)
    private val _isSidebarCollapsed = MutableStateFlow(savedSidebarCollapsed)
    val isSidebarCollapsed: StateFlow<Boolean> = _isSidebarCollapsed.asStateFlow()

    private val savedSidebarTab = prefs.getInt("pref_sidebar_tab", 0)
    private val _activeSidebarTab = MutableStateFlow(savedSidebarTab)
    val activeSidebarTab: StateFlow<Int> = _activeSidebarTab.asStateFlow()

    // Real-Time Price Alerts
    private val _alerts = MutableStateFlow<List<PriceAlert>>(
        listOf(
            PriceAlert("a1", "BTC/USD", 70000.0, isAbove = true, note = "Breakout resistance"),
            PriceAlert("a2", "ETH/USD", 3400.0, isAbove = false, note = "Key demand support"),
            PriceAlert("a3", "SOL/USD", 195.0, isAbove = true, note = "Momentum retest")
        )
    )
    val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

    // Explore / Community Mocks
    private val _exploreNews = MutableStateFlow<List<ExploreNews>>(
        listOf(
            ExploreNews("1", "TradingView", "8:30 AM", "XAU/USD: Gold Slides Below 200-Day Average as Yields Defeat Safe-Haven Demand", "GOLD"),
            ExploreNews("2", "Reuters", "7:45 AM", "S&P 500 futures dip as investors await jobs report data", "S&P 500"),
            ExploreNews("3", "CoinDesk", "6:10 AM", "Bitcoin dominance hits 3-year high as altcoins struggle to maintain momentum", "BTCUSD")
        )
    )
    val exploreNews: StateFlow<List<ExploreNews>> = _exploreNews.asStateFlow()

    private val _communityPosts = MutableStateFlow<List<CommunityPost>>(
        listOf(
            CommunityPost("1", "FIONA_MARKET_EXPERT", "fiona_expert", "16h ago", "Double Bottom Reversal After Liquidity Sweep | Bullish Market Setup", "Here is a detailed breakdown of the double bottom setup.", "XAUUSD", null, 24, 5),
            CommunityPost("2", "IGT_Traders", "igt_traders", "Sep 1", "BTC forming massive bull flag on the weekly time frame, expect break out soon", "Targeting 85,000 for the next leg up if 77,000 holds.", "BTCUSD", null, 156, 32)
        )
    )
    val communityPosts: StateFlow<List<CommunityPost>> = _communityPosts.asStateFlow()

    init {
        // Load initial data for all 4 panes
        _paneConfigs.value.forEach { config ->
            loadPaneCandles(config.id, config.symbol, config.timeframe)
        }
        val mainConfig = _paneConfigs.value.first()
        _selectedSymbol.value = mainConfig.symbol
        _timeframe.value = mainConfig.timeframe
        startLiveDataLoop()
    }

    // Workspace & Layout Control
    fun setLayoutMode(mode: WorkspaceLayoutMode) {
        _layoutMode.value = mode
        _maximizedPaneId.value = null
        prefs.edit().putInt("pref_layout_mode", mode.id).apply()
    }

    fun setFocusedPane(id: Int) {
        _focusedPaneId.value = id
        val config = _paneConfigs.value.find { it.id == id } ?: return
        _selectedSymbol.value = config.symbol
        _timeframe.value = config.timeframe
        _activeIndicators.value = config.indicators
        _candles.value = _paneCandles.value[id] ?: emptyList()
    }

    fun toggleMaximizePane(id: Int) {
        if (_maximizedPaneId.value == id) {
            _maximizedPaneId.value = null
        } else {
            _maximizedPaneId.value = id
            setFocusedPane(id)
        }
    }

    fun setPaneSymbol(paneId: Int, symbol: String) {
        val updated = _paneConfigs.value.map {
            if (it.id == paneId) it.copy(symbol = symbol) else it
        }
        _paneConfigs.value = updated
        prefs.edit().putString("pane_${paneId}_sym", symbol).apply()
        val config = updated.find { it.id == paneId } ?: return
        loadPaneCandles(paneId, symbol, config.timeframe)
        if (_focusedPaneId.value == paneId) {
            _selectedSymbol.value = symbol
        }
    }

    fun setPaneTimeframe(paneId: Int, tf: Timeframe) {
        val updated = _paneConfigs.value.map {
            if (it.id == paneId) it.copy(timeframe = tf) else it
        }
        _paneConfigs.value = updated
        prefs.edit().putString("pane_${paneId}_tf", tf.label).apply()
        val config = updated.find { it.id == paneId } ?: return
        loadPaneCandles(paneId, config.symbol, tf)
        if (_focusedPaneId.value == paneId) {
            _timeframe.value = tf
        }
    }

    fun togglePaneIndicator(paneId: Int, indicator: TechnicalIndicator) {
        val updated = _paneConfigs.value.map { config ->
            if (config.id == paneId) {
                val current = config.indicators.toMutableSet()
                if (current.contains(indicator)) current.remove(indicator) else current.add(indicator)
                config.copy(indicators = current)
            } else config
        }
        _paneConfigs.value = updated
        if (_focusedPaneId.value == paneId) {
            val conf = updated.find { it.id == paneId }
            if (conf != null) _activeIndicators.value = conf.indicators
        }
    }

    // Crosshair Sync
    fun toggleCrosshairSync() {
        val newState = !_syncCrosshair.value
        _syncCrosshair.value = newState
        _crosshairSyncState.value = _crosshairSyncState.value.copy(isEnabled = newState, active = false)
        prefs.edit().putBoolean("pref_sync_crosshair", newState).apply()
    }

    fun broadcastCrosshair(sourcePaneId: Int, timestamp: Long, normalizedX: Float, price: Double) {
        if (!_syncCrosshair.value) return
        _crosshairSyncState.value = CrosshairSyncState(
            isEnabled = true,
            active = true,
            sourcePaneId = sourcePaneId,
            timestamp = timestamp,
            normalizedX = normalizedX,
            price = price
        )
    }

    fun clearCrosshair() {
        if (_crosshairSyncState.value.active) {
            _crosshairSyncState.value = _crosshairSyncState.value.copy(active = false)
        }
    }

    // Panel Resizing & Collapsing
    fun setSidebarWidth(width: Float) {
        val clamped = width.coerceIn(220f, 520f)
        _sidebarWidth.value = clamped
        prefs.edit().putFloat("pref_sidebar_width", clamped).apply()
    }

    fun toggleSidebarCollapsed() {
        val collapsed = !_isSidebarCollapsed.value
        _isSidebarCollapsed.value = collapsed
        prefs.edit().putBoolean("pref_sidebar_collapsed", collapsed).apply()
    }

    fun setActiveSidebarTab(tab: Int) {
        _activeSidebarTab.value = tab
        prefs.edit().putInt("pref_sidebar_tab", tab).apply()
    }

    // Price Alert Management
    fun addAlert(symbol: String, targetPrice: Double, isAbove: Boolean, note: String = "") {
        val newAlert = PriceAlert(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            targetPrice = targetPrice,
            isAbove = isAbove,
            note = note
        )
        _alerts.value = listOf(newAlert) + _alerts.value
    }

    fun removeAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }
    }

    fun selectSymbol(symbol: String) {
        setPaneSymbol(_focusedPaneId.value, symbol)
    }

    fun setTimeframe(tf: Timeframe) {
        setPaneTimeframe(_focusedPaneId.value, tf)
    }

    fun setChartStyle(style: ChartStyle) {
        _chartStyle.value = style
    }

    fun toggleIndicator(indicator: TechnicalIndicator) {
        togglePaneIndicator(_focusedPaneId.value, indicator)
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

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private fun loadPaneCandles(paneId: Int, symbol: String, tf: Timeframe) {
        viewModelScope.launch(Dispatchers.Default) {
            val c = marketEngine.generateHistoricalCandles(symbol, tf, count = 300)
            val map = _paneCandles.value.toMutableMap()
            map[paneId] = c
            _paneCandles.value = map
            if (paneId == _focusedPaneId.value) {
                _candles.value = c
            }

            if (symbol == "BTC/USD" || symbol == "BTCUSD") {
                binanceClient.connect("btcusdt")
            }
        }
    }

    private fun startLiveDataLoop() {
        // Collect Binance real-time data for BTC
        viewModelScope.launch(Dispatchers.Default) {
            binanceClient.liveCandle.collect { liveCandle ->
                if (liveCandle != null) {
                    val map = _paneCandles.value.toMutableMap()
                    var changed = false
                    _paneConfigs.value.forEach { config ->
                        if (config.symbol == "BTC/USD" || config.symbol == "BTCUSD") {
                            val list = map[config.id]?.toMutableList()
                            if (!list.isNullOrEmpty()) {
                                list[list.lastIndex] = liveCandle
                                map[config.id] = list
                                changed = true
                            }
                        }
                    }
                    if (changed) {
                        _paneCandles.value = map
                        if (_selectedSymbol.value == "BTC/USD" || _selectedSymbol.value == "BTCUSD") {
                            _candles.value = map[_focusedPaneId.value] ?: emptyList()
                        }
                    }

                    // Update quotes & evaluate price alerts
                    _quotes.value = _quotes.value.map { q ->
                        if (q.symbol == "BTC/USD" || q.symbol == "BTCUSD") {
                            val diff = liveCandle.close - (q.currentPrice - q.change)
                            val pct = if (q.currentPrice != q.change) (diff / (q.currentPrice - q.change)) * 100 else 0.0
                            val updated = q.copy(currentPrice = liveCandle.close, change = diff, changePercent = pct)
                            checkAlerts(updated)
                            updated
                        } else q
                    }
                }
            }
        }

        // Live tick simulation for other symbols
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _quotes.value = _quotes.value.map { q ->
                    if (q.symbol != "BTC/USD" && q.symbol != "BTCUSD") {
                        val pctDelta = (kotlin.random.Random.nextDouble() - 0.495) * 0.004
                        val newPrice = (q.currentPrice * (1 + pctDelta)).coerceAtLeast(0.01)
                        val diff = newPrice - (q.currentPrice - q.change)
                        val pct = if (q.currentPrice != q.change) (diff / (q.currentPrice - q.change)) * 100 else 0.0
                        val updated = q.copy(currentPrice = newPrice, change = diff, changePercent = pct)
                        checkAlerts(updated)

                        // Update pane candles for this symbol
                        val map = _paneCandles.value.toMutableMap()
                        var mapChanged = false
                        _paneConfigs.value.forEach { config ->
                            if (config.symbol == q.symbol) {
                                val cList = map[config.id]?.toMutableList()
                                if (!cList.isNullOrEmpty()) {
                                    val last = cList.last()
                                    val updatedLast = last.copy(
                                        high = maxOf(last.high, newPrice),
                                        low = minOf(last.low, newPrice),
                                        close = newPrice,
                                        volume = last.volume + kotlin.random.Random.nextDouble(2.0, 30.0)
                                    )
                                    cList[cList.lastIndex] = updatedLast
                                    map[config.id] = cList
                                    mapChanged = true
                                }
                            }
                        }
                        if (mapChanged) {
                            _paneCandles.value = map
                            if (_selectedSymbol.value == q.symbol) {
                                _candles.value = map[_focusedPaneId.value] ?: emptyList()
                            }
                        }
                        updated
                    } else q
                }
            }
        }
    }

    private fun checkAlerts(quote: TickerQuote) {
        _alerts.value.forEach { alert ->
            if (!alert.triggered && alert.symbol == quote.symbol) {
                val hit = if (alert.isAbove) quote.currentPrice >= alert.targetPrice else quote.currentPrice <= alert.targetPrice
                if (hit) {
                    notificationManager.dispatchAlertNotification(alert, quote.currentPrice)
                    _alerts.value = _alerts.value.map {
                        if (it.id == alert.id) it.copy(triggered = true, triggeredAt = System.currentTimeMillis()) else it
                    }
                }
            }
        }
    }
}


