package com.example.data.market

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class MarketDataEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    // Tick speed in milliseconds (default 500ms for active day trading feel)
    private val _tickSpeedMs = MutableStateFlow<Long>(600L)
    val tickSpeedMs: StateFlow<Long> = _tickSpeedMs.asStateFlow()

    private val _isStreaming = MutableStateFlow(true)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Real-time Quotes map (Symbol -> Quote)
    private val _quotes = MutableStateFlow<Map<String, Quote>>(emptyMap())
    val quotes: StateFlow<Map<String, Quote>> = _quotes.asStateFlow()

    // Multi-timeframe Candles (Symbol -> (TimeFrame -> List<CandleStick>))
    private val _candles = MutableStateFlow<Map<String, Map<TimeFrame, List<CandleStick>>>>(emptyMap())
    val candles: StateFlow<Map<String, Map<TimeFrame, List<CandleStick>>>> = _candles.asStateFlow()

    // Order Books (Symbol -> OrderBook)
    private val _orderBooks = MutableStateFlow<Map<String, OrderBook>>(emptyMap())
    val orderBooks: StateFlow<Map<String, OrderBook>> = _orderBooks.asStateFlow()

    // Trade Tape live stream
    private val _tradeTape = MutableSharedFlow<TradeTapeItem>(extraBufferCapacity = 64)
    val tradeTape: SharedFlow<TradeTapeItem> = _tradeTape.asSharedFlow()

    // Technical Signals Stream
    private val _signals = MutableStateFlow<List<TechnicalSignal>>(emptyList())
    val signals: StateFlow<List<TechnicalSignal>> = _signals.asStateFlow()

    private var tickJob: Job? = null

    // Base asset definitions
    data class AssetConfig(
        val symbol: String,
        val name: String,
        val basePrice: Double,
        val volatility: Double, // annual or per-tick variance
        val assetType: AssetType
    )

    private val assetConfigs = listOf(
        AssetConfig("NVDA", "NVIDIA Corporation", 128.45, 0.0035, AssetType.STOCK),
        AssetConfig("TSLA", "Tesla Inc.", 218.80, 0.0050, AssetType.STOCK),
        AssetConfig("AAPL", "Apple Inc.", 224.50, 0.0020, AssetType.STOCK),
        AssetConfig("AMD", "Advanced Micro Devices", 154.20, 0.0038, AssetType.STOCK),
        AssetConfig("BTC/USD", "Bitcoin USD", 64250.00, 0.0040, AssetType.CRYPTO),
        AssetConfig("ETH/USD", "Ethereum USD", 3450.00, 0.0045, AssetType.CRYPTO),
        AssetConfig("SPY", "SPDR S&P 500 ETF", 558.90, 0.0015, AssetType.INDEX),
        AssetConfig("QQQ", "Invesco QQQ Trust", 482.30, 0.0022, AssetType.INDEX),
        AssetConfig("PLTR", "Palantir Technologies", 32.40, 0.0055, AssetType.STOCK),
        AssetConfig("COIN", "Coinbase Global Inc.", 215.10, 0.0060, AssetType.STOCK),
        AssetConfig("MSFT", "Microsoft Corp.", 422.10, 0.0018, AssetType.STOCK),
        AssetConfig("AMZN", "Amazon.com Inc.", 178.60, 0.0025, AssetType.STOCK)
    )

    init {
        initializeHistoricalData()
        startStreaming()
    }

    private fun initializeHistoricalData() {
        val initialQuotes = mutableMapOf<String, Quote>()
        val initialCandles = mutableMapOf<String, MutableMap<TimeFrame, List<CandleStick>>>()
        val initialOrderBooks = mutableMapOf<String, OrderBook>()

        val now = System.currentTimeMillis()

        for (config in assetConfigs) {
            val symbol = config.symbol
            val base = config.basePrice

            // Generate initial candles for 1m, 5m, 15m, 1h, 1D
            val timeframeCandles = mutableMapOf<TimeFrame, List<CandleStick>>()
            for (tf in TimeFrame.values()) {
                val count = 25
                val candleList = mutableListOf<CandleStick>()
                var currentClose = base * (1.0 + (Random.nextDouble(-0.02, 0.02)))
                val periodMs = tf.seconds * 1000L

                for (i in count downTo 0) {
                    val time = now - (i * periodMs)
                    val change = currentClose * Random.nextDouble(-config.volatility * 2, config.volatility * 2)
                    val open = currentClose
                    val close = max(0.01, open + change)
                    val high = max(open, close) + abs(change * Random.nextDouble(0.2, 0.8))
                    val low = min(open, close) - abs(change * Random.nextDouble(0.2, 0.8))
                    val vol = Random.nextDouble(1000.0, 50000.0) * if (config.assetType == AssetType.CRYPTO) 0.5 else 1.0

                    candleList.add(CandleStick(time, round2(open), round2(high), round2(low), round2(close), round2(vol)))
                    currentClose = close
                }
                timeframeCandles[tf] = candleList
            }
            initialCandles[symbol] = timeframeCandles

            val m1List = timeframeCandles[TimeFrame.M1] ?: emptyList()
            val lastClose = m1List.lastOrNull()?.close ?: base
            val prevClose = m1List.firstOrNull()?.open ?: (base * 0.98)
            val closes = m1List.map { it.close }

            val rsi = TechnicalIndicators.calculateRSI(closes)
            val ema9 = TechnicalIndicators.calculateEMA(closes, 9)
            val ema21 = TechnicalIndicators.calculateEMA(closes, 21)
            val ema50 = TechnicalIndicators.calculateEMA(closes, 50)
            val (upper, mid, lower) = TechnicalIndicators.calculateBollingerBands(closes)
            val (macd, signal, hist) = TechnicalIndicators.calculateMACD(closes)
            val vwap = TechnicalIndicators.calculateVWAP(m1List)

            val spread = max(0.01, lastClose * 0.0003)
            val bid = round2(lastClose - spread / 2)
            val ask = round2(lastClose + spread / 2)

            val quote = Quote(
                symbol = symbol,
                name = config.name,
                price = lastClose,
                previousClose = round2(prevClose),
                open = m1List.firstOrNull()?.open ?: lastClose,
                high = m1List.maxOfOrNull { it.high } ?: lastClose,
                low = m1List.minOfOrNull { it.low } ?: lastClose,
                volume = (m1List.sumOf { it.volume }).toLong(),
                avgVolume = 15400000L,
                bid = bid,
                ask = ask,
                bidSize = Random.nextDouble(100.0, 2500.0),
                askSize = Random.nextDouble(100.0, 2500.0),
                vwap = round2(vwap),
                rsi14 = round2(rsi),
                ema9 = round2(ema9),
                ema21 = round2(ema21),
                ema50 = round2(ema50),
                upperBollinger = round2(upper),
                lowerBollinger = round2(lower),
                middleBollinger = round2(mid),
                macdLine = round2(macd),
                macdSignal = round2(signal),
                macdHistogram = round2(hist),
                assetType = config.assetType,
                sparkline = closes.takeLast(15)
            )
            initialQuotes[symbol] = quote
            initialOrderBooks[symbol] = generateOrderBook(symbol, quote.price, quote.assetType)
        }

        _quotes.value = initialQuotes
        _candles.value = initialCandles
        _orderBooks.value = initialOrderBooks
        generateInitialSignals(initialQuotes)
    }

    private fun generateInitialSignals(quotes: Map<String, Quote>) {
        val signalsList = mutableListOf<TechnicalSignal>()
        quotes.values.forEach { q ->
            if (q.rsi14 <= 32.0) {
                signalsList.add(
                    TechnicalSignal(
                        id = UUID.randomUUID().toString(),
                        symbol = q.symbol,
                        type = SignalType.RSI_OVERSOLD,
                        price = q.price,
                        confidence = 88,
                        description = "${q.symbol} RSI reached ${q.rsi14.toInt()} — oversold reversal zone on 1M/5M timeframe."
                    )
                )
            } else if (q.rsi14 >= 68.0) {
                signalsList.add(
                    TechnicalSignal(
                        id = UUID.randomUUID().toString(),
                        symbol = q.symbol,
                        type = SignalType.RSI_OVERBOUGHT,
                        price = q.price,
                        confidence = 82,
                        description = "${q.symbol} RSI reached ${q.rsi14.toInt()} — overbought momentum extension."
                    )
                )
            }
            if (q.macdHistogram > 0.1 && q.macdLine > q.macdSignal) {
                signalsList.add(
                    TechnicalSignal(
                        id = UUID.randomUUID().toString(),
                        symbol = q.symbol,
                        type = SignalType.MACD_BULLISH_CROSS,
                        price = q.price,
                        confidence = 91,
                        description = "Bullish MACD crossover triggered on ${q.symbol}. Buying pressure increasing."
                    )
                )
            }
        }
        _signals.value = signalsList.take(6)
    }

    fun setTickSpeed(speedMs: Long) {
        _tickSpeedMs.value = speedMs.coerceAtLeast(100L)
    }

    fun toggleStreaming() {
        if (_isStreaming.value) {
            pauseStreaming()
        } else {
            startStreaming()
        }
    }

    fun pauseStreaming() {
        _isStreaming.value = false
        tickJob?.cancel()
        tickJob = null
    }

    fun startStreaming() {
        _isStreaming.value = true
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val delayMs = _tickSpeedMs.value
                delay(delayMs)
                if (_isStreaming.value) {
                    processTick()
                }
            }
        }
    }

    private suspend fun processTick() {
        val currentQuotes = _quotes.value.toMutableMap()
        val currentCandles = _candles.value.toMutableMap()
        val currentOrderBooks = _orderBooks.value.toMutableMap()
        val now = System.currentTimeMillis()

        // Pick 2-4 active symbols each tick for hyper-realistic trading tape activity
        val symbolsToUpdate = assetConfigs.shuffled().take(Random.nextInt(2, 5))

        for (config in symbolsToUpdate) {
            val symbol = config.symbol
            val quote = currentQuotes[symbol] ?: continue

            // Brownian drift + occasional jump
            val isJump = Random.nextDouble() < 0.08
            val jumpMultiplier = if (isJump) Random.nextDouble(1.8, 3.5) else 1.0
            val deltaPct = Random.nextDouble(-config.volatility, config.volatility) * jumpMultiplier
            val newPrice = max(0.01, round2(quote.price * (1.0 + deltaPct)))

            // Update trade tape
            val isBuy = newPrice >= quote.price
            val tradeSize = if (config.assetType == AssetType.CRYPTO) {
                round2(Random.nextDouble(0.05, 4.2))
            } else {
                (Random.nextInt(1, 35) * 10).toDouble()
            }

            val tapeItem = TradeTapeItem(
                id = UUID.randomUUID().toString().take(8),
                symbol = symbol,
                price = newPrice,
                size = tradeSize,
                isBuyerMaker = isBuy,
                timestamp = now
            )
            _tradeTape.emit(tapeItem)

            // Update candle sticks
            val symbolCandles = currentCandles[symbol]?.toMutableMap() ?: mutableMapOf()
            val m1Candles = symbolCandles[TimeFrame.M1]?.toMutableList() ?: mutableListOf()

            if (m1Candles.isNotEmpty()) {
                val lastCandle = m1Candles.last()
                val candleAge = now - lastCandle.timestamp
                val periodMs = 60 * 1000L

                if (candleAge < periodMs) {
                    // Update active candle
                    val updatedCandle = lastCandle.copy(
                        high = max(lastCandle.high, newPrice),
                        low = min(lastCandle.low, newPrice),
                        close = newPrice,
                        volume = lastCandle.volume + tradeSize
                    )
                    m1Candles[m1Candles.lastIndex] = updatedCandle
                } else {
                    // Start new 1M candle
                    val newCandle = CandleStick(
                        timestamp = now,
                        open = lastCandle.close,
                        high = max(lastCandle.close, newPrice),
                        low = min(lastCandle.close, newPrice),
                        close = newPrice,
                        volume = tradeSize
                    )
                    m1Candles.add(newCandle)
                    if (m1Candles.size > 50) m1Candles.removeAt(0)
                }
                symbolCandles[TimeFrame.M1] = m1Candles
            }
            currentCandles[symbol] = symbolCandles

            // Recalculate indicators
            val closes = (symbolCandles[TimeFrame.M1] ?: emptyList()).map { it.close }
            val rsi = TechnicalIndicators.calculateRSI(closes)
            val ema9 = TechnicalIndicators.calculateEMA(closes, 9)
            val ema21 = TechnicalIndicators.calculateEMA(closes, 21)
            val ema50 = TechnicalIndicators.calculateEMA(closes, 50)
            val (upper, mid, lower) = TechnicalIndicators.calculateBollingerBands(closes)
            val (macd, sig, hist) = TechnicalIndicators.calculateMACD(closes)
            val vwap = TechnicalIndicators.calculateVWAP(symbolCandles[TimeFrame.M1] ?: emptyList())

            val spread = max(0.01, newPrice * 0.0003)
            val bid = round2(newPrice - spread / 2)
            val ask = round2(newPrice + spread / 2)

            val updatedQuote = quote.copy(
                price = newPrice,
                high = max(quote.high, newPrice),
                low = min(quote.low, newPrice),
                volume = quote.volume + tradeSize.toLong(),
                bid = bid,
                ask = ask,
                bidSize = Random.nextDouble(50.0, 3000.0),
                askSize = Random.nextDouble(50.0, 3000.0),
                vwap = round2(vwap),
                rsi14 = round2(rsi),
                ema9 = round2(ema9),
                ema21 = round2(ema21),
                ema50 = round2(ema50),
                upperBollinger = round2(upper),
                lowerBollinger = round2(lower),
                middleBollinger = round2(mid),
                macdLine = round2(macd),
                macdSignal = round2(sig),
                macdHistogram = round2(hist),
                sparkline = (quote.sparkline + newPrice).takeLast(15),
                timestamp = now
            )
            currentQuotes[symbol] = updatedQuote
            currentOrderBooks[symbol] = generateOrderBook(symbol, newPrice, config.assetType)

            // Check for new technical signals
            checkSignalTrigger(updatedQuote)
        }

        _quotes.value = currentQuotes
        _candles.value = currentCandles
        _orderBooks.value = currentOrderBooks
    }

    private fun checkSignalTrigger(quote: Quote) {
        if (quote.rsi14 <= 28.0 && Random.nextDouble() < 0.15) {
            val signal = TechnicalSignal(
                id = UUID.randomUUID().toString(),
                symbol = quote.symbol,
                type = SignalType.RSI_OVERSOLD,
                price = quote.price,
                confidence = 89,
                description = "${quote.symbol} extreme oversold RSI ${quote.rsi14.toInt()} — Strong day scalp bounce potential."
            )
            addNewSignal(signal)
        } else if (quote.rsi14 >= 72.0 && Random.nextDouble() < 0.15) {
            val signal = TechnicalSignal(
                id = UUID.randomUUID().toString(),
                symbol = quote.symbol,
                type = SignalType.RSI_OVERBOUGHT,
                price = quote.price,
                confidence = 84,
                description = "${quote.symbol} overbought RSI ${quote.rsi14.toInt()} near resistance."
            )
            addNewSignal(signal)
        }
    }

    private fun addNewSignal(signal: TechnicalSignal) {
        val existing = _signals.value.filter { it.symbol != signal.symbol || (System.currentTimeMillis() - it.timestamp) > 30000 }
        _signals.value = (listOf(signal) + existing).take(8)
    }

    private fun generateOrderBook(symbol: String, price: Double, assetType: AssetType): OrderBook {
        val levels = 8
        val step = if (price > 1000) 1.5 else if (price > 100) 0.05 else 0.01

        val bids = mutableListOf<OrderBookLevel>()
        var bidTotal = 0.0
        for (i in 1..levels) {
            val lvlPrice = round2(price - (i * step))
            val lvlSize = if (assetType == AssetType.CRYPTO) {
                round2(Random.nextDouble(0.1, 5.0) * (levels - i + 1))
            } else {
                (Random.nextInt(5, 50) * 10).toDouble()
            }
            bidTotal += lvlSize
            bids.add(OrderBookLevel(lvlPrice, lvlSize, bidTotal, 0f))
        }

        val asks = mutableListOf<OrderBookLevel>()
        var askTotal = 0.0
        for (i in 1..levels) {
            val lvlPrice = round2(price + (i * step))
            val lvlSize = if (assetType == AssetType.CRYPTO) {
                round2(Random.nextDouble(0.1, 5.0) * (levels - i + 1))
            } else {
                (Random.nextInt(5, 50) * 10).toDouble()
            }
            askTotal += lvlSize
            asks.add(OrderBookLevel(lvlPrice, lvlSize, askTotal, 0f))
        }

        val maxBidTotal = bids.lastOrNull()?.total ?: 1.0
        val maxAskTotal = asks.lastOrNull()?.total ?: 1.0
        val maxTotal = max(maxBidTotal, maxAskTotal)

        val normalizedBids = bids.map { it.copy(depthPercentage = (it.total / maxTotal).toFloat().coerceIn(0.05f, 1f)) }
        val normalizedAsks = asks.map { it.copy(depthPercentage = (it.total / maxTotal).toFloat().coerceIn(0.05f, 1f)) }

        val bestBid = bids.firstOrNull()?.price ?: price
        val bestAsk = asks.firstOrNull()?.price ?: price
        val spread = round2(bestAsk - bestBid)
        val spreadPct = if (price > 0) round2((spread / price) * 100.0) else 0.0

        return OrderBook(symbol, normalizedBids, normalizedAsks, spread, spreadPct)
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
