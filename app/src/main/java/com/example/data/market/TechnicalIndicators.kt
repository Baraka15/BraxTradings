package com.example.data.market

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object TechnicalIndicators {

    /**
     * Simple Moving Average (SMA)
     */
    fun calculateSMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        val window = if (prices.size >= period) prices.takeLast(period) else prices
        return window.average()
    }

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size < period) return prices.average()

        val k = 2.0 / (period + 1.0)
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = (prices[i] * k) + (ema * (1.0 - k))
        }
        return ema
    }

    /**
     * Relative Strength Index (RSI 14)
     */
    fun calculateRSI(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0

        var totalGain = 0.0
        var totalLoss = 0.0

        // Initial average
        for (i in 1..period) {
            val diff = closes[i] - closes[i - 1]
            if (diff >= 0) totalGain += diff else totalLoss += -diff
        }

        var avgGain = totalGain / period
        var avgLoss = totalLoss / period

        // Smoothed average for subsequent periods
        for (i in (period + 1) until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(0.0, 100.0)
    }

    /**
     * Bollinger Bands (20 periods, stdDevMultiplier = 2.0)
     * Returns Triple(Upper, Middle, Lower)
     */
    fun calculateBollingerBands(
        closes: List<Double>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): Triple<Double, Double, Double> {
        if (closes.isEmpty()) return Triple(0.0, 0.0, 0.0)
        val window = if (closes.size >= period) closes.takeLast(period) else closes
        val mean = window.average()
        val variance = window.sumOf { (it - mean).pow(2) } / window.size
        val stdDev = sqrt(variance)

        val upper = mean + (stdDev * stdDevMultiplier)
        val lower = mean - (stdDev * stdDevMultiplier)
        return Triple(upper, mean, lower)
    }

    /**
     * MACD (12 fast, 26 slow, 9 signal)
     * Returns Triple(MACD Line, Signal Line, Histogram)
     */
    fun calculateMACD(
        closes: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<Double, Double, Double> {
        if (closes.size < slowPeriod) {
            return Triple(0.0, 0.0, 0.0)
        }

        val macdLineValues = mutableListOf<Double>()
        for (i in slowPeriod..closes.size) {
            val subList = closes.subList(0, i)
            val fastEMA = calculateEMA(subList, fastPeriod)
            val slowEMA = calculateEMA(subList, slowPeriod)
            macdLineValues.add(fastEMA - slowEMA)
        }

        val currentMACD = macdLineValues.lastOrNull() ?: 0.0
        val signalLine = if (macdLineValues.size >= signalPeriod) {
            calculateEMA(macdLineValues, signalPeriod)
        } else {
            macdLineValues.average()
        }
        val histogram = currentMACD - signalLine

        return Triple(currentMACD, signalLine, histogram)
    }

    /**
     * Volume Weighted Average Price (VWAP)
     */
    fun calculateVWAP(candles: List<CandleStick>): Double {
        if (candles.isEmpty()) return 0.0
        var cumulativeTypicalPriceVolume = 0.0
        var cumulativeVolume = 0.0

        for (c in candles) {
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            cumulativeTypicalPriceVolume += typicalPrice * c.volume
            cumulativeVolume += c.volume
        }

        return if (cumulativeVolume > 0) cumulativeTypicalPriceVolume / cumulativeVolume else candles.last().close
    }

    /**
     * Heikin-Ashi Candle Transform
     */
    fun toHeikinAshi(candles: List<CandleStick>): List<CandleStick> {
        if (candles.isEmpty()) return emptyList()
        val result = mutableListOf<CandleStick>()

        var prevHAOpen = (candles[0].open + candles[0].close) / 2.0
        var prevHAClose = (candles[0].open + candles[0].high + candles[0].low + candles[0].close) / 4.0

        result.add(
            CandleStick(
                timestamp = candles[0].timestamp,
                open = prevHAOpen,
                high = max(candles[0].high, max(prevHAOpen, prevHAClose)),
                low = min(candles[0].low, min(prevHAOpen, prevHAClose)),
                close = prevHAClose,
                volume = candles[0].volume
            )
        )

        for (i in 1 until candles.size) {
            val raw = candles[i]
            val haClose = (raw.open + raw.high + raw.low + raw.close) / 4.0
            val haOpen = (prevHAOpen + prevHAClose) / 2.0
            val haHigh = max(raw.high, max(haOpen, haClose))
            val haLow = min(raw.low, min(haOpen, haClose))

            result.add(
                CandleStick(
                    timestamp = raw.timestamp,
                    open = haOpen,
                    high = haHigh,
                    low = haLow,
                    close = haClose,
                    volume = raw.volume
                )
            )

            prevHAOpen = haOpen
            prevHAClose = haClose
        }

        return result
    }
}

