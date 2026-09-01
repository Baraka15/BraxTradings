package com.example.data.market

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*
import kotlin.random.Random

interface MarketDataService {
    val priceTicks: StateFlow<Map<String, PriceTick>>
    val latestTick: StateFlow<PriceTick?>
    val isStreaming: StateFlow<Boolean>
    val tickSpeedMs: StateFlow<Long>
    val tickStream: SharedFlow<PriceTick>

    fun getTickFlow(symbol: String): Flow<PriceTick>
    fun startStreaming()
    fun pauseStreaming()
    fun toggleStreaming()
    fun setTickSpeed(speedMs: Long)
    fun getHistoricalTicks(symbol: String): List<PriceTick>
}

class RealMarketDataService(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : MarketDataService {

    data class StockProfile(
        val symbol: String,
        val name: String,
        val basePrice: Double,
        val volatility: Double,
        val assetType: AssetType = AssetType.STOCK
    )

    private val defaultProfiles = listOf(
        StockProfile("NVDA", "NVIDIA Corporation", 128.45, 0.0035),
        StockProfile("TSLA", "Tesla Inc.", 218.80, 0.0050),
        StockProfile("AAPL", "Apple Inc.", 224.50, 0.0020),
        StockProfile("AMD", "Advanced Micro Devices", 154.20, 0.0038),
        StockProfile("MSFT", "Microsoft Corp.", 422.10, 0.0018),
        StockProfile("AMZN", "Amazon.com Inc.", 178.60, 0.0025),
        StockProfile("GOOGL", "Alphabet Inc.", 166.80, 0.0022),
        StockProfile("META", "Meta Platforms Inc.", 512.40, 0.0032),
        StockProfile("PLTR", "Palantir Technologies", 32.40, 0.0055),
        StockProfile("COIN", "Coinbase Global Inc.", 215.10, 0.0060),
        StockProfile("SPY", "SPDR S&P 500 ETF", 558.90, 0.0015, AssetType.INDEX),
        StockProfile("QQQ", "Invesco QQQ Trust", 482.30, 0.0022, AssetType.INDEX),
        StockProfile("BTC/USD", "Bitcoin USD", 64250.00, 0.0040, AssetType.CRYPTO),
        StockProfile("ETH/USD", "Ethereum USD", 3450.00, 0.0045, AssetType.CRYPTO)
    )

    private val profilesMap = defaultProfiles.associateBy { it.symbol }.toMutableMap()

    // Reference base / day-open prices for daily percentage calculation
    private val baselinePrices = mutableMapOf<String, Double>()
    private val dayHighs = mutableMapOf<String, Double>()
    private val dayLows = mutableMapOf<String, Double>()
    private val totalVolumes = mutableMapOf<String, Double>()
    private val tickHistories = mutableMapOf<String, MutableList<PriceTick>>()

    // StateFlow holding real-time map of latest PriceTick for each symbol
    private val _priceTicks = MutableStateFlow<Map<String, PriceTick>>(emptyMap())
    override val priceTicks: StateFlow<Map<String, PriceTick>> = _priceTicks.asStateFlow()

    // StateFlow for the most recently updated tick across all active symbols
    private val _latestTick = MutableStateFlow<PriceTick?>(null)
    override val latestTick: StateFlow<PriceTick?> = _latestTick.asStateFlow()

    // Hot SharedFlow for tick event streams (useful for trade tape, audio alerts, or execution checks)
    private val _tickStream = MutableSharedFlow<PriceTick>(extraBufferCapacity = 128)
    override val tickStream: SharedFlow<PriceTick> = _tickStream.asSharedFlow()

    private val _isStreaming = MutableStateFlow(true)
    override val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _tickSpeedMs = MutableStateFlow(500L)
    override val tickSpeedMs: StateFlow<Long> = _tickSpeedMs.asStateFlow()

    private var streamingJob: Job? = null

    init {
        initializeInitialTicks()
        startStreaming()
    }

    private fun initializeInitialTicks() {
        val initialMap = mutableMapOf<String, PriceTick>()
        val now = System.currentTimeMillis()

        for (profile in defaultProfiles) {
            val base = profile.basePrice
            val dayOpen = round2(base * (1.0 + Random.nextDouble(-0.015, 0.015)))
            baselinePrices[profile.symbol] = dayOpen
            dayHighs[profile.symbol] = max(base, dayOpen) + (base * 0.01)
            dayLows[profile.symbol] = min(base, dayOpen) - (base * 0.01)
            totalVolumes[profile.symbol] = Random.nextDouble(500_000.0, 5_000_000.0)

            val spread = max(0.01, round2(base * 0.0003))
            val bid = round2(base - (spread / 2))
            val ask = round2(base + (spread / 2))
            val change = round2(base - dayOpen)
            val changePct = round2((change / dayOpen) * 100.0)

            val initialTick = PriceTick(
                symbol = profile.symbol,
                price = base,
                change = change,
                changePercent = changePct,
                volume = totalVolumes[profile.symbol] ?: 0.0,
                bid = bid,
                ask = ask,
                bidSize = (Random.nextInt(10, 50) * 10).toDouble(),
                askSize = (Random.nextInt(10, 50) * 10).toDouble(),
                high = dayHighs[profile.symbol] ?: base,
                low = dayLows[profile.symbol] ?: base,
                isUptick = change >= 0,
                timestamp = now
            )

            initialMap[profile.symbol] = initialTick
            tickHistories[profile.symbol] = mutableListOf(initialTick)
        }

        _priceTicks.value = initialMap
        _latestTick.value = initialMap["NVDA"]
    }

    override fun startStreaming() {
        _isStreaming.value = true
        streamingJob?.cancel()
        streamingJob = coroutineScope.launch {
            while (isActive) {
                val delayMs = _tickSpeedMs.value
                delay(delayMs)
                if (_isStreaming.value) {
                    generateNextTicks()
                }
            }
        }
    }

    override fun pauseStreaming() {
        _isStreaming.value = false
        streamingJob?.cancel()
        streamingJob = null
    }

    override fun toggleStreaming() {
        if (_isStreaming.value) {
            pauseStreaming()
        } else {
            startStreaming()
        }
    }

    override fun setTickSpeed(speedMs: Long) {
        _tickSpeedMs.value = speedMs.coerceIn(50L, 5000L)
    }

    override fun getHistoricalTicks(symbol: String): List<PriceTick> {
        return tickHistories[symbol]?.toList() ?: emptyList()
    }

    override fun getTickFlow(symbol: String): Flow<PriceTick> {
        return _priceTicks.mapNotNull { it[symbol] }.distinctUntilChanged()
    }

    private suspend fun generateNextTicks() {
        val currentTicks = _priceTicks.value.toMutableMap()
        val now = System.currentTimeMillis()

        // Pick 2-5 active symbols per cycle to simulate real-market asynchronous quoting
        val symbolsToUpdate = profilesMap.values.shuffled().take(Random.nextInt(2, 6))

        for (profile in symbolsToUpdate) {
            val symbol = profile.symbol
            val lastTick = currentTicks[symbol] ?: continue
            val dayOpen = baselinePrices[symbol] ?: lastTick.price

            // Realistic geometric Brownian motion tick simulation
            val isJump = Random.nextDouble() < 0.05 // 5% chance of sudden liquidity shift or breakout
            val jumpMultiplier = if (isJump) Random.nextDouble(1.8, 3.2) else 1.0
            val deltaPct = Random.nextDouble(-profile.volatility, profile.volatility) * jumpMultiplier
            val newPrice = max(0.01, round2(lastTick.price * (1.0 + deltaPct)))

            val isUptick = newPrice >= lastTick.price
            val currentHigh = max(dayHighs[symbol] ?: newPrice, newPrice)
            val currentLow = min(dayLows[symbol] ?: newPrice, newPrice)
            dayHighs[symbol] = currentHigh
            dayLows[symbol] = currentLow

            // Volume generation
            val tickVol = if (profile.assetType == AssetType.CRYPTO) {
                round2(Random.nextDouble(0.05, 3.5))
            } else {
                (Random.nextInt(1, 25) * 10).toDouble()
            }
            val accumulatedVol = (totalVolumes[symbol] ?: 0.0) + tickVol
            totalVolumes[symbol] = accumulatedVol

            // Tight institutional bid/ask spread
            val spread = max(0.01, round2(newPrice * 0.0003))
            val bid = round2(newPrice - (spread / 2))
            val ask = round2(newPrice + (spread / 2))

            val change = round2(newPrice - dayOpen)
            val changePercent = round2((change / dayOpen) * 100.0)

            val updatedTick = PriceTick(
                symbol = symbol,
                price = newPrice,
                change = change,
                changePercent = changePercent,
                volume = accumulatedVol,
                bid = bid,
                ask = ask,
                bidSize = (Random.nextInt(1, 30) * 10).toDouble(),
                askSize = (Random.nextInt(1, 30) * 10).toDouble(),
                high = currentHigh,
                low = currentLow,
                isUptick = isUptick,
                timestamp = now
            )

            currentTicks[symbol] = updatedTick
            _latestTick.value = updatedTick
            _tickStream.emit(updatedTick)

            // Maintain rolling history
            val history = tickHistories.getOrPut(symbol) { mutableListOf() }
            history.add(updatedTick)
            if (history.size > 200) {
                history.removeAt(0)
            }
        }

        _priceTicks.value = currentTicks
    }

    fun addCustomSymbol(
        symbol: String,
        name: String,
        basePrice: Double,
        volatility: Double,
        assetType: AssetType = AssetType.STOCK
    ) {
        val profile = StockProfile(symbol, name, basePrice, volatility, assetType)
        profilesMap[symbol] = profile
        baselinePrices[symbol] = basePrice
        dayHighs[symbol] = basePrice
        dayLows[symbol] = basePrice
        totalVolumes[symbol] = 100_000.0

        val tick = PriceTick(
            symbol = symbol,
            price = basePrice,
            change = 0.0,
            changePercent = 0.0,
            volume = 100_000.0,
            bid = round2(basePrice * 0.9998),
            ask = round2(basePrice * 1.0002),
            bidSize = 100.0,
            askSize = 100.0,
            high = basePrice,
            low = basePrice,
            isUptick = true,
            timestamp = System.currentTimeMillis()
        )

        val updated = _priceTicks.value.toMutableMap()
        updated[symbol] = tick
        _priceTicks.value = updated
        tickHistories[symbol] = mutableListOf(tick)
    }

    private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0
}
