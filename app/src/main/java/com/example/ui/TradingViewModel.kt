package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.market.MarketDataEngine
import com.example.data.notification.PriceAlertNotificationManager
import com.example.domain.trading.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.data.market.BinanceWebSocketClient

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val marketEngine = MarketDataEngine()
    private val notificationManager = PriceAlertNotificationManager(application)
    private val binanceClient = BinanceWebSocketClient()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _quotes = MutableStateFlow<List<TickerQuote>>(marketEngine.availableSymbols)
    val quotes: StateFlow<List<TickerQuote>> = _quotes.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("BTCUSD")
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

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private fun loadSymbolData(symbol: String, tf: Timeframe) {
        viewModelScope.launch(Dispatchers.Default) {
            val c = marketEngine.generateHistoricalCandles(symbol, tf, count = 300) // Much more history for scrolling
            _candles.value = c
            
            if (symbol == "BTCUSD") {
                binanceClient.connect("btcusdt")
            } else {
                binanceClient.disconnect()
            }
        }
    }

    private fun startLiveDataLoop() {
        // Collect Binance real-time data
        viewModelScope.launch(Dispatchers.Default) {
            binanceClient.liveCandle.collect { liveCandle ->
                if (liveCandle != null && _selectedSymbol.value == "BTCUSD") {
                    val cList = _candles.value.toMutableList()
                    if (cList.isNotEmpty()) {
                        cList[cList.lastIndex] = liveCandle
                        _candles.value = cList
                    }
                    
                    _quotes.value = _quotes.value.map { q ->
                        if (q.symbol == "BTCUSD") {
                            val diff = liveCandle.close - (q.currentPrice - q.change)
                            val pct = (diff / (q.currentPrice - q.change)) * 100
                            q.copy(currentPrice = liveCandle.close, change = diff, changePercent = pct)
                        } else q
                    }
                }
            }
        }

        // Random mock loop for other symbols
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(1000) // 1s tick
                val currentSymbol = _selectedSymbol.value
                if (currentSymbol != "BTCUSD") {
                    val currentQuote = _quotes.value.find { it.symbol == currentSymbol } ?: continue
                    val pctDelta = (kotlin.random.Random.nextDouble() - 0.495) * 0.005
                    val newPrice = (currentQuote.currentPrice * (1 + pctDelta)).coerceAtLeast(0.01)

                    _quotes.value = _quotes.value.map { q ->
                        if (q.symbol == currentSymbol || kotlin.random.Random.nextFloat() > 0.7f) {
                            val symPrice = if (q.symbol == currentSymbol) newPrice else q.currentPrice * (1 + (kotlin.random.Random.nextDouble() - 0.5) * 0.002)
                            val diff = symPrice - (q.currentPrice - q.change)
                            val pct = (diff / (q.currentPrice - q.change)) * 100
                            q.copy(
                                currentPrice = symPrice,
                                change = diff,
                                changePercent = pct
                            )
                        } else q
                    }

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
                }
            }
        }
    }
}

