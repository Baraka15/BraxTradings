package com.example.domain.trading

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object TechnicalAnalysis {

    fun calculateEMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.size < period) return List(candles.size) { null }
        val result = MutableList<Double?>(candles.size) { null }
        val multiplier = 2.0 / (period + 1)
        
        var sma = 0.0
        for (i in 0 until period) {
            sma += candles[i].close
        }
        sma /= period
        result[period - 1] = sma

        var prevEma = sma
        for (i in period until candles.size) {
            val currentEma = (candles[i].close - prevEma) * multiplier + prevEma
            result[i] = currentEma
            prevEma = currentEma
        }
        return result
    }

    data class BollingerBandsResult(
        val upper: List<Double?>,
        val middle: List<Double?>,
        val lower: List<Double?>
    )

    fun calculateBollingerBands(candles: List<Candle>, period: Int = 20, multiplier: Double = 2.0): BollingerBandsResult {
        val upper = MutableList<Double?>(candles.size) { null }
        val middle = MutableList<Double?>(candles.size) { null }
        val lower = MutableList<Double?>(candles.size) { null }

        if (candles.size < period) return BollingerBandsResult(upper, middle, lower)

        for (i in (period - 1) until candles.size) {
            val sub = candles.subList(i - period + 1, i + 1)
            val mean = sub.map { it.close }.average()
            val variance = sub.map { (it.close - mean) * (it.close - mean) }.average()
            val stdDev = sqrt(variance)

            middle[i] = mean
            upper[i] = mean + (multiplier * stdDev)
            lower[i] = mean - (multiplier * stdDev)
        }
        return BollingerBandsResult(upper, middle, lower)
    }

    fun calculateVWAP(candles: List<Candle>): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        var cumVol = 0.0
        var cumVolPrice = 0.0

        for (i in candles.indices) {
            val c = candles[i]
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            cumVolPrice += typicalPrice * c.volume
            cumVol += c.volume
            result[i] = if (cumVol > 0) cumVolPrice / cumVol else typicalPrice
        }
        return result
    }

    data class SupertrendResult(
        val supertrend: List<Double?>,
        val isBullish: List<Boolean?>
    )

    fun calculateSupertrend(candles: List<Candle>, period: Int = 10, multiplier: Double = 3.0): SupertrendResult {
        val st = MutableList<Double?>(candles.size) { null }
        val dir = MutableList<Boolean?>(candles.size) { null }
        if (candles.size < period) return SupertrendResult(st, dir)

        val trList = mutableListOf<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
            } else {
                val hl = candles[i].high - candles[i].low
                val hc = abs(candles[i].high - candles[i - 1].close)
                val lc = abs(candles[i].low - candles[i - 1].close)
                trList.add(max(hl, max(hc, lc)))
            }
        }

        var inUpTrend = true
        var prevUpper = 0.0
        var prevLower = 0.0

        for (i in (period - 1) until candles.size) {
            val atr = trList.subList(i - period + 1, i + 1).average()
            val hl2 = (candles[i].high + candles[i].low) / 2.0
            var upperBand = hl2 + (multiplier * atr)
            var lowerBand = hl2 - (multiplier * atr)

            if (i > period - 1) {
                if (lowerBand < prevLower || candles[i - 1].close < prevLower) {
                    // lowerband keeps
                } else {
                    lowerBand = max(lowerBand, prevLower)
                }
                if (upperBand > prevUpper || candles[i - 1].close > prevUpper) {
                    // upper keeps
                } else {
                    upperBand = min(upperBand, prevUpper)
                }
            }

            if (candles[i].close > prevUpper && !inUpTrend) {
                inUpTrend = true
            } else if (candles[i].close < prevLower && inUpTrend) {
                inUpTrend = false
            }

            st[i] = if (inUpTrend) lowerBand else upperBand
            dir[i] = inUpTrend
            prevUpper = upperBand
            prevLower = lowerBand
        }

        return SupertrendResult(st, dir)
    }

    fun calculateRSI(candles: List<Candle>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period) return result

        var gains = 0.0
        var losses = 0.0
        for (i in 1..period) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff >= 0) gains += diff else losses += -diff
        }

        var avgGain = gains / period
        var avgLoss = losses / period
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + (avgGain / avgLoss)))

        for (i in (period + 1) until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            val gain = if (diff >= 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
        }
        return result
    }

    data class MACDResult(
        val macd: List<Double?>,
        val signal: List<Double?>,
        val histogram: List<Double?>
    )

    fun calculateMACD(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MACDResult {
        val fastEma = calculateEMA(candles, fastPeriod)
        val slowEma = calculateEMA(candles, slowPeriod)
        val macdLine = MutableList<Double?>(candles.size) { null }

        for (i in candles.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine[i] = f - s
            }
        }

        val validMacdCandles = mutableListOf<Candle>()
        val originalIndices = mutableListOf<Int>()
        for (i in macdLine.indices) {
            val v = macdLine[i]
            if (v != null) {
                validMacdCandles.add(Candle(0, v, v, v, v, 0.0))
                originalIndices.add(i)
            }
        }

        val signalSub = calculateEMA(validMacdCandles, signalPeriod)
        val signalLine = MutableList<Double?>(candles.size) { null }
        val histogram = MutableList<Double?>(candles.size) { null }

        for (k in signalSub.indices) {
            val origIdx = originalIndices[k]
            val sig = signalSub[k]
            signalLine[origIdx] = sig
            val mac = macdLine[origIdx]
            if (mac != null && sig != null) {
                histogram[origIdx] = mac - sig
            }
        }

        return MACDResult(macdLine, signalLine, histogram)
    }

    fun computeHeikinAshi(candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()
        val haList = ArrayList<Candle>(candles.size)
        var prevOpen = candles[0].open
        var prevClose = candles[0].close

        for (c in candles) {
            val haClose = (c.open + c.high + c.low + c.close) / 4.0
            val haOpen = (prevOpen + prevClose) / 2.0
            val haHigh = max(c.high, max(haOpen, haClose))
            val haLow = min(c.low, min(haOpen, haClose))
            haList.add(Candle(c.timestamp, haOpen, haHigh, haLow, haClose, c.volume))
            prevOpen = haOpen
            prevClose = haClose
        }
        return haList
    }
}
