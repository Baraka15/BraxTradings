package com.example.data.market

import com.example.domain.trading.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random

class MarketDataEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _instruments = MutableStateFlow<List<Instrument>>(emptyList())
    val instruments: StateFlow<List<Instrument>> = _instruments.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("NVDA")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _candleHistory = MutableStateFlow<Map<String, Map<TimeFrame, List<CandleStick>>>>(emptyMap())
    val candleHistory: StateFlow<Map<String, Map<TimeFrame, List<CandleStick>>>> = _candleHistory.asStateFlow()

    private val _orderBook = MutableStateFlow(OrderBookDepth(emptyList(), emptyList(), 0.0))
    val orderBook: StateFlow<OrderBookDepth> = _orderBook.asStateFlow()

    private val _recentTrades = MutableStateFlow<List<TapeTrade>>(emptyList())
    val recentTrades: StateFlow<List<TapeTrade>> = _recentTrades.asStateFlow()

    private val _isStreaming = MutableStateFlow(true)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _priceDirections = MutableStateFlow<Map<String, PriceDirection>>(emptyMap())
    val priceDirections: StateFlow<Map<String, PriceDirection>> = _priceDirections.asStateFlow()

    private val _streamIntervalMs = MutableStateFlow(800L)
    val streamIntervalMs: StateFlow<Long> = _streamIntervalMs.asStateFlow()

    private val _transportMode = MutableStateFlow(StreamTransportMode.WEBSOCKET)
    val transportMode: StateFlow<StreamTransportMode> = _transportMode.asStateFlow()

    private val _tickerStreamInfo = MutableStateFlow(
        TickerStreamInfo(
            isStreaming = true,
            transportMode = StreamTransportMode.WEBSOCKET,
            intervalMs = 800L,
            totalTicksReceived = 0L,
            latencyMs = 14L,
            isConnected = true,
            lastTickTimestamp = System.currentTimeMillis()
        )
    )
    val tickerStreamInfo: StateFlow<TickerStreamInfo> = _tickerStreamInfo.asStateFlow()

    private var streamJob: Job? = null

    init {
        initializeMarkets()
        startDataStream()
    }

    private fun initializeMarkets() {

        val initialList = listOf(
            Instrument("NVDA", "NVIDIA Corporation", "STOCKS", 124.80, 3.25, 128.5, 121.0, 4850000000.0),
            Instrument("AAPL", "Apple Inc.", "STOCKS", 228.40, 1.15, 231.2, 226.0, 3100000000.0),
            Instrument("TSLA", "Tesla Inc.", "STOCKS", 218.60, -2.10, 225.0, 214.0, 2900000000.0),
            Instrument("MSFT", "Microsoft Corp.", "STOCKS", 448.50, 0.85, 452.0, 444.0, 2200000000.0),
            Instrument("AMZN", "Amazon.com Inc.", "STOCKS", 178.20, 1.95, 181.0, 175.5, 2150000000.0),
            Instrument("BTC/USDT", "Bitcoin Spot", "CRYPTO", 64450.00, 2.80, 65800.0, 63100.0, 19800000000.0),
            Instrument("ETH/USDT", "Ethereum Spot", "CRYPTO", 3490.00, 1.90, 3560.0, 3410.0, 8900000000.0),
            Instrument("SOL/USDT", "Solana", "CRYPTO", 146.50, 6.20, 154.0, 137.5, 3400000000.0)
        ).map { item ->
            val spark = (0..15).map { i ->
                item.currentPrice * (1.0 + Math.sin(i * 0.45) * 0.018)
            }
            item.copy(sparkline = spark)
        }
        _instruments.value = initialList

        // Generate initial candles
        val historyMap = mutableMapOf<String, Map<TimeFrame, List<CandleStick>>>()
        for (inst in initialList) {
            val tfMap = mutableMapOf<TimeFrame, List<CandleStick>>()
            for (tf in TimeFrame.values()) {
                var price = inst.currentPrice * 0.97
                val list = mutableListOf<CandleStick>()
                val now = System.currentTimeMillis()
                for (i in 25 downTo 0) {
                    val time = now - (i * tf.seconds * 1000L)
                    val delta = price * Random.nextDouble(-0.012, 0.015)
                    val open = price
                    val close = price + delta
                    val high = maxOf(open, close) + price * Random.nextDouble(0.001, 0.006)
                    val low = minOf(open, close) - price * Random.nextDouble(0.001, 0.006)
                    val vol = Random.nextDouble(10.0, 500.0) * (inst.volume24h / 100000000.0)
                    list.add(CandleStick(time, open, high, low, close, vol))
                    price = close
                }
                tfMap[tf] = list
            }
            historyMap[inst.symbol] = tfMap
        }
        _candleHistory.value = historyMap
        updateOrderBookAndTrades("NVDA", 124.80)
    }

    private fun startDataStream() {
        streamJob?.cancel()
        streamJob = scope.launch {
            while (isActive) {
                if (_isStreaming.value) {
                    simulateTick()
                }
                val interval = _streamIntervalMs.value
                delay(interval)
            }
        }
    }

    fun simulateTick() {
        val currentMap = _instruments.value.associateBy { it.symbol }
        val newDirections = mutableMapOf<String, PriceDirection>()

        val updated = _instruments.value.map { inst ->
            // Random fluctuation between -0.35% and +0.38%
            val pctDelta = Random.nextDouble(-0.0035, 0.0038)
            val newPrice = (inst.currentPrice * (1.0 + pctDelta)).coerceAtLeast(0.01)
            val newHigh = maxOf(inst.high24h, newPrice)
            val newLow = minOf(inst.low24h, newPrice)
            val newSpark = inst.sparkline.drop(1) + listOf(newPrice)

            val dir = when {
                newPrice > inst.currentPrice -> PriceDirection.UP
                newPrice < inst.currentPrice -> PriceDirection.DOWN
                else -> PriceDirection.NEUTRAL
            }
            newDirections[inst.symbol] = dir

            inst.copy(
                currentPrice = newPrice,
                change24h = inst.change24h + (pctDelta * 8),
                high24h = newHigh,
                low24h = newLow,
                sparkline = newSpark
            )
        }
        _priceDirections.value = newDirections
        _instruments.value = updated

        val activeSym = _selectedSymbol.value
        val activeInst = updated.find { it.symbol == activeSym }
        if (activeInst != null) {
            updateOrderBookAndTrades(activeInst.symbol, activeInst.currentPrice)
            updateLiveCandle(activeInst.symbol, activeInst.currentPrice)
        }

        val currentInfo = _tickerStreamInfo.value
        val simulatedLatency = if (_transportMode.value == StreamTransportMode.WEBSOCKET) {
            Random.nextLong(8L, 22L)
        } else {
            Random.nextLong(35L, 75L)
        }

        _tickerStreamInfo.value = currentInfo.copy(
            isStreaming = _isStreaming.value,
            transportMode = _transportMode.value,
            intervalMs = _streamIntervalMs.value,
            totalTicksReceived = currentInfo.totalTicksReceived + 1,
            latencyMs = simulatedLatency,
            isConnected = _isStreaming.value,
            lastTickTimestamp = System.currentTimeMillis()
        )
    }

    private fun updateOrderBookAndTrades(symbol: String, price: Double) {
        val spread = price * 0.0004
        var bidAccum = 0.0
        val bids = (1..5).map { i ->
            val p = price - (spread * i)
            val size = Random.nextDouble(10.0, 200.0)
            bidAccum += size
            OrderBookLevel(p, size, bidAccum)
        }

        var askAccum = 0.0
        val asks = (1..5).map { i ->
            val p = price + (spread * i)
            val size = Random.nextDouble(10.0, 200.0)
            askAccum += size
            OrderBookLevel(p, size, askAccum)
        }
        _orderBook.value = OrderBookDepth(bids, asks, spread)

        // New trade
        val side = if (Random.nextBoolean()) OrderSide.BUY else OrderSide.SELL
        val tradePrice = if (side == OrderSide.BUY) asks.first().price else bids.first().price
        val trade = TapeTrade(
            id = System.currentTimeMillis().toString(),
            time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
            price = tradePrice,
            size = Random.nextDouble(5.0, 100.0),
            side = side
        )
        _recentTrades.value = (listOf(trade) + _recentTrades.value).take(12)
    }

    private fun updateLiveCandle(symbol: String, currentPrice: Double) {
        val history = _candleHistory.value.toMutableMap()
        val symbolCandles = history[symbol]?.toMutableMap() ?: return

        for (tf in TimeFrame.values()) {
            val list = (symbolCandles[tf] ?: emptyList()).toMutableList()
            if (list.isNotEmpty()) {
                val last = list.last()
                val updatedLast = last.copy(
                    high = maxOf(last.high, currentPrice),
                    low = minOf(last.low, currentPrice),
                    close = currentPrice,
                    volume = last.volume + Random.nextDouble(1.0, 20.0)
                )
                list[list.lastIndex] = updatedLast
                symbolCandles[tf] = list
            }
        }
        history[symbol] = symbolCandles
        _candleHistory.value = history
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        val inst = _instruments.value.find { it.symbol == symbol }
        if (inst != null) {
            updateOrderBookAndTrades(symbol, inst.currentPrice)
        }
    }

    fun setPriceForTesting(symbol: String, newPrice: Double) {
        _instruments.value = _instruments.value.map { inst ->
            if (inst.symbol == symbol) {
                inst.copy(
                    currentPrice = newPrice,
                    high24h = maxOf(inst.high24h, newPrice),
                    low24h = minOf(inst.low24h, newPrice)
                )
            } else inst
        }
    }

    fun toggleStreaming() {
        val next = !_isStreaming.value
        _isStreaming.value = next
        _tickerStreamInfo.value = _tickerStreamInfo.value.copy(
            isStreaming = next,
            isConnected = next
        )
    }

    fun setStreamInterval(intervalMs: Long) {
        _streamIntervalMs.value = intervalMs
        startDataStream()
    }

    fun setTransportMode(mode: StreamTransportMode) {
        _transportMode.value = mode
        _tickerStreamInfo.value = _tickerStreamInfo.value.copy(transportMode = mode)
    }

    fun refreshTickNow() {
        simulateTick()
    }
}
