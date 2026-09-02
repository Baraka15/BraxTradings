package com.example.data.market

import com.example.domain.trading.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import kotlin.random.Random

class MarketDataEngine {

    val availableSymbols = listOf(
        TickerQuote("BTC/USD", "Bitcoin", 67420.50, 1420.50, 2.15, 68900.0, 65800.0, 32450000000.0, 1320000000000.0),
        TickerQuote("ETH/USD", "Ethereum", 3540.80, -45.20, -1.26, 3620.0, 3480.0, 16800000000.0, 425000000000.0),
        TickerQuote("SOL/USD", "Solana", 178.45, 8.75, 5.16, 182.0, 168.5, 4200000000.0, 82000000000.0),
        TickerQuote("NVDA", "NVIDIA Corp", 128.60, 4.30, 3.46, 130.2, 124.8, 48500000000.0, 3160000000000.0, "STOCKS"),
        TickerQuote("TSLA", "Tesla Inc", 248.90, -3.10, -1.23, 254.0, 244.5, 18900000000.0, 792000000000.0, "STOCKS"),
        TickerQuote("AAPL", "Apple Inc", 228.30, 1.10, 0.48, 230.0, 226.5, 12400000000.0, 3490000000000.0, "STOCKS")
    )

    fun generateHistoricalCandles(symbol: String, timeframe: Timeframe, count: Int = 100): List<Candle> {
        val quote = availableSymbols.find { it.symbol == symbol } ?: availableSymbols.first()
        val basePrice = quote.currentPrice
        val list = mutableListOf<Candle>()
        val intervalSec = timeframe.seconds
        val now = System.currentTimeMillis() / 1000

        var currentClose = basePrice * 0.92
        val volatility = basePrice * 0.005

        for (i in count downTo 1) {
            val ts = now - (i * intervalSec)
            val open = currentClose
            val trend = (Random.nextDouble() - 0.48) * volatility
            val close = (open + trend).coerceAtLeast(1.0)
            val high = maxOf(open, close) + Random.nextDouble() * volatility * 0.8
            val low = minOf(open, close) - Random.nextDouble() * volatility * 0.8
            val volume = Random.nextDouble(100.0, 1500.0) * (basePrice / 100.0)

            list.add(Candle(ts * 1000, open, high, low, close, volume))
            currentClose = close
        }
        return list
    }

    fun generateOrderBook(currentPrice: Double): Pair<List<OrderBookLevel>, List<OrderBookLevel>> {
        val bids = mutableListOf<OrderBookLevel>()
        val asks = mutableListOf<OrderBookLevel>()
        val step = currentPrice * 0.0005

        var runningBidTotal = 0.0
        for (i in 1..10) {
            val price = currentPrice - (i * step)
            val qty = Random.nextDouble(0.5, 5.0) * (50000.0 / currentPrice)
            runningBidTotal += qty
            bids.add(OrderBookLevel(price, qty, runningBidTotal, (i * 10f).coerceAtMost(100f)))
        }

        var runningAskTotal = 0.0
        for (i in 1..10) {
            val price = currentPrice + (i * step)
            val qty = Random.nextDouble(0.5, 5.0) * (50000.0 / currentPrice)
            runningAskTotal += qty
            asks.add(OrderBookLevel(price, qty, runningAskTotal, (i * 10f).coerceAtMost(100f)))
        }

        return Pair(bids, asks)
    }

    fun generateRecentTrades(currentPrice: Double, count: Int = 15): List<MarketTrade> {
        val now = System.currentTimeMillis()
        return (1..count).map { i ->
            val isBuyer = Random.nextBoolean()
            val delta = (Random.nextDouble() - 0.5) * (currentPrice * 0.001)
            MarketTrade(
                id = UUID.randomUUID().toString().take(8),
                price = currentPrice + delta,
                quantity = Random.nextDouble(0.05, 3.5),
                isBuyerMaker = isBuyer,
                timestamp = now - (i * 800)
            )
        }
    }

    fun getTechnicalConfluence(symbol: String, currentPrice: Double): TechnicalSignalSummary {
        val hash = (symbol.hashCode() + currentPrice.toInt()).let { if (it < 0) -it else it }
        val buys = (hash % 12) + 6
        val sells = ((hash / 2) % 6) + 1
        val neutrals = 26 - buys - sells
        val rating = when {
            buys >= 14 -> "Strong Buy"
            buys >= 10 -> "Buy"
            sells >= 12 -> "Strong Sell"
            sells >= 8 -> "Sell"
            else -> "Neutral"
        }
        val score = (buys - sells) / 26f
        return TechnicalSignalSummary(symbol, rating, buys, neutrals, sells, score)
    }

    fun streamLiveTicks(baseQuote: TickerQuote): Flow<TickerQuote> = flow {
        var current = baseQuote
        while (true) {
            delay(1500)
            val pctDelta = (Random.nextDouble() - 0.495) * 0.004
            val newPrice = (current.currentPrice * (1 + pctDelta)).coerceAtLeast(0.01)
            val diff = newPrice - (current.currentPrice - current.change)
            val newChangePct = (diff / (current.currentPrice - current.change)) * 100

            current = current.copy(
                currentPrice = newPrice,
                change = diff,
                changePercent = newChangePct,
                high24h = maxOf(current.high24h, newPrice),
                low24h = minOf(current.low24h, newPrice)
            )
            emit(current)
        }
    }
}
