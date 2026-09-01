package com.example.domain.trading

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class ChartStyle(val label: String) {
    CANDLESTICK("Candles"),
    HEIKIN_ASHI("Heikin-Ashi"),
    LINE_AREA("Area / Line"),
    HOLLOW_CANDLES("Hollow Bars")
}

enum class SubChartType(val label: String, val shortName: String) {
    VOLUME("Volume (20 MA)", "VOL"),
    RSI("RSI (14)", "RSI"),
    MACD("MACD (12, 26, 9)", "MACD"),
    NONE("Hide Sub-Chart", "OFF")
}

enum class DrawingTool(val label: String, val iconName: String) {
    CURSOR("Crosshair", "crosshair"),
    TREND_LINE("Trend Line", "trending_up"),
    HORIZONTAL_RAY("Support / Resistance", "horizontal_rule"),
    FIBONACCI("Fib Retracement", "stacked_line_chart"),
    RISK_REWARD("Long/Short Position (R:R)", "aspect_ratio"),
    RULER("Measurement Ruler", "straighten")
}

data class ChartDrawing(
    val id: String,
    val type: DrawingTool,
    val startIndex: Int,
    val startPrice: Double,
    val endIndex: Int,
    val endPrice: Double,
    val label: String = "",
    val colorHex: Long = 0xFF00E5FF
)

data class MacdPoint(
    val macd: Double,
    val signal: Double,
    val histogram: Double
)

data class BollingerBand(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

data class SupertrendPoint(
    val value: Double,
    val isBullish: Boolean
)

data class TechnicalGauge(
    val overallRating: TechnicalRating,
    val summaryScore: Int, // -10 to +10
    val movingAverageRating: TechnicalRating,
    val movingAverageBuyCount: Int,
    val movingAverageSellCount: Int,
    val movingAverageNeutralCount: Int,
    val oscillatorRating: TechnicalRating,
    val oscillatorBuyCount: Int,
    val oscillatorSellCount: Int,
    val oscillatorNeutralCount: Int,
    val rsiValue: Double,
    val macdSignal: String,
    val adxStrength: String
)

enum class TechnicalRating(val label: String, val colorHex: Long) {
    STRONG_BUY("STRONG BUY", 0xFF00E676),
    BUY("BUY", 0xFF00C853),
    NEUTRAL("NEUTRAL", 0xFFFFD600),
    SELL("SELL", 0xFFFF5252),
    STRONG_SELL("STRONG SELL", 0xFFFF1744)
}

data class IndicatorSettings(
    val showEMA9: Boolean = true,
    val showEMA21: Boolean = true,
    val showEMA50: Boolean = false,
    val showEMA200: Boolean = false,
    val showBollingerBands: Boolean = false,
    val showVWAP: Boolean = true,
    val showSupertrend: Boolean = false,
    val subChart: SubChartType = SubChartType.VOLUME
)

object TechnicalAnalysisEngine {

    /**
     * Compute Exponential Moving Average (EMA)
     */
    fun calculateEMA(candles: List<CandleStick>, period: Int): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size < period) return result

        val multiplier = 2.0 / (period + 1)
        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        var prevEma = sum / period
        result[period - 1] = prevEma

        for (i in period until candles.size) {
            val close = candles[i].close
            val currentEma = (close - prevEma) * multiplier + prevEma
            result[i] = currentEma
            prevEma = currentEma
        }
        return result
    }

    /**
     * Compute Simple Moving Average (SMA)
     */
    fun calculateSMA(values: List<Double>, period: Int): List<Double?> {
        val result = MutableList<Double?>(values.size) { null }
        if (values.size < period) return result

        var windowSum = 0.0
        for (i in 0 until period) {
            windowSum += values[i]
        }
        result[period - 1] = windowSum / period

        for (i in period until values.size) {
            windowSum += values[i] - values[i - period]
            result[i] = windowSum / period
        }
        return result
    }

    /**
     * Compute Bollinger Bands (20 period, 2 std dev)
     */
    fun calculateBollingerBands(candles: List<CandleStick>, period: Int = 20, multiplier: Double = 2.0): List<BollingerBand?> {
        val closes = candles.map { it.close }
        val sma = calculateSMA(closes, period)
        val result = MutableList<BollingerBand?>(candles.size) { null }

        for (i in period - 1 until candles.size) {
            val mean = sma[i] ?: continue
            var varianceSum = 0.0
            for (j in (i - period + 1)..i) {
                varianceSum += (closes[j] - mean) * (closes[j] - mean)
            }
            val stdDev = sqrt(varianceSum / period)
            result[i] = BollingerBand(
                upper = mean + (multiplier * stdDev),
                middle = mean,
                lower = mean - (multiplier * stdDev)
            )
        }
        return result
    }

    /**
     * Compute Relative Strength Index (RSI 14)
     */
    fun calculateRSI(candles: List<CandleStick>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period) return result

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change > 0) gainSum += change else lossSum += abs(change)
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period

        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = 100.0 - (100.0 / (1.0 + rs))

        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result[i] = 100.0 - (100.0 / (1.0 + rs))
        }

        return result
    }

    /**
     * Compute Moving Average Convergence Divergence (MACD 12, 26, 9)
     */
    fun calculateMACD(
        candles: List<CandleStick>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): List<MacdPoint?> {
        val fastEma = calculateEMA(candles, fastPeriod)
        val slowEma = calculateEMA(candles, slowPeriod)
        val result = MutableList<MacdPoint?>(candles.size) { null }

        val macdLine = MutableList<Double?>(candles.size) { null }
        for (i in candles.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine[i] = f - s
            }
        }

        // Calculate Signal Line (9 EMA of MACD Line)
        val validMacdIndices = macdLine.mapIndexedNotNull { idx, v -> if (v != null) idx to v else null }
        if (validMacdIndices.size >= signalPeriod) {
            val multiplier = 2.0 / (signalPeriod + 1)
            var sum = 0.0
            for (k in 0 until signalPeriod) {
                sum += validMacdIndices[k].second
            }
            var prevSignal = sum / signalPeriod
            val firstSignalIdx = validMacdIndices[signalPeriod - 1].first
            val mVal = macdLine[firstSignalIdx] ?: 0.0
            result[firstSignalIdx] = MacdPoint(mVal, prevSignal, mVal - prevSignal)

            for (k in signalPeriod until validMacdIndices.size) {
                val origIdx = validMacdIndices[k].first
                val curM = validMacdIndices[k].second
                val curSignal = (curM - prevSignal) * multiplier + prevSignal
                result[origIdx] = MacdPoint(curM, curSignal, curM - curSignal)
                prevSignal = curSignal
            }
        }

        return result
    }

    /**
     * Compute Volume Weighted Average Price (VWAP)
     */
    fun calculateVWAP(candles: List<CandleStick>): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        var cumulativeTPV = 0.0
        var cumulativeVol = 0.0

        for (i in candles.indices) {
            val c = candles[i]
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            val volume = if (c.volume > 0) c.volume else 1.0
            cumulativeTPV += typicalPrice * volume
            cumulativeVol += volume
            result[i] = cumulativeTPV / cumulativeVol
        }
        return result
    }

    /**
     * Compute Supertrend Indicator (Period 10, Multiplier 3)
     */
    fun calculateSupertrend(candles: List<CandleStick>, period: Int = 10, multiplier: Double = 3.0): List<SupertrendPoint?> {
        val result = MutableList<SupertrendPoint?>(candles.size) { null }
        if (candles.size < period) return result

        val atr = calculateATR(candles, period)
        var isBullish = true
        var prevSupertrend = candles[period - 1].close

        for (i in period until candles.size) {
            val c = candles[i]
            val curAtr = atr[i] ?: continue
            val basicUpper = (c.high + c.low) / 2.0 + multiplier * curAtr
            val basicLower = (c.high + c.low) / 2.0 - multiplier * curAtr

            if (isBullish) {
                if (c.close < prevSupertrend) {
                    isBullish = false
                    prevSupertrend = basicUpper
                } else {
                    prevSupertrend = max(prevSupertrend, basicLower)
                }
            } else {
                if (c.close > prevSupertrend) {
                    isBullish = true
                    prevSupertrend = basicLower
                } else {
                    prevSupertrend = min(prevSupertrend, basicUpper)
                }
            }

            result[i] = SupertrendPoint(prevSupertrend, isBullish)
        }
        return result
    }

    /**
     * Average True Range (ATR)
     */
    private fun calculateATR(candles: List<CandleStick>, period: Int): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period) return result

        val trList = mutableListOf<Double>()
        for (i in candles.indices) {
            if (i == 0) {
                trList.add(candles[i].high - candles[i].low)
            } else {
                val tr = max(
                    candles[i].high - candles[i].low,
                    max(
                        abs(candles[i].high - candles[i - 1].close),
                        abs(candles[i].low - candles[i - 1].close)
                    )
                )
                trList.add(tr)
            }
        }

        var atrSum = 0.0
        for (i in 0 until period) {
            atrSum += trList[i]
        }
        var prevAtr = atrSum / period
        result[period - 1] = prevAtr

        for (i in period until candles.size) {
            val curAtr = (prevAtr * (period - 1) + trList[i]) / period
            result[i] = curAtr
            prevAtr = curAtr
        }

        return result
    }

    /**
     * Convert standard candlesticks to Heikin-Ashi candlesticks
     */
    fun convertToHeikinAshi(candles: List<CandleStick>): List<CandleStick> {
        if (candles.isEmpty()) return emptyList()
        val haCandles = mutableListOf<CandleStick>()

        var prevHaOpen = candles[0].open
        var prevHaClose = candles[0].close

        for (i in candles.indices) {
            val c = candles[i]
            val haClose = (c.open + c.high + c.low + c.close) / 4.0
            val haOpen = if (i == 0) {
                (c.open + c.close) / 2.0
            } else {
                (prevHaOpen + prevHaClose) / 2.0
            }
            val haHigh = max(c.high, max(haOpen, haClose))
            val haLow = min(c.low, min(haOpen, haClose))

            haCandles.add(
                CandleStick(
                    timestamp = c.timestamp,
                    open = haOpen,
                    high = haHigh,
                    low = haLow,
                    close = haClose,
                    volume = c.volume
                )
            )

            prevHaOpen = haOpen
            prevHaClose = haClose
        }

        return haCandles
    }

    /**
     * TradingView Technical Analysis Gauge (Oscillators + Moving Averages Confluence)
     */
    fun calculateTechnicalGauge(candles: List<CandleStick>): TechnicalGauge {
        if (candles.size < 30) {
            return TechnicalGauge(
                overallRating = TechnicalRating.NEUTRAL,
                summaryScore = 0,
                movingAverageRating = TechnicalRating.NEUTRAL,
                movingAverageBuyCount = 5,
                movingAverageSellCount = 5,
                movingAverageNeutralCount = 2,
                oscillatorRating = TechnicalRating.NEUTRAL,
                oscillatorBuyCount = 3,
                oscillatorSellCount = 3,
                oscillatorNeutralCount = 2,
                rsiValue = 50.0,
                macdSignal = "Neutral",
                adxStrength = "Moderate"
            )
        }

        val lastClose = candles.last().close
        val ema9 = calculateEMA(candles, 9).lastOrNull() ?: lastClose
        val ema21 = calculateEMA(candles, 21).lastOrNull() ?: lastClose
        val ema50 = calculateEMA(candles, 50).lastOrNull() ?: lastClose
        val rsiList = calculateRSI(candles, 14)
        val rsi = rsiList.lastOrNull() ?: 50.0
        val macdList = calculateMACD(candles)
        val macd = macdList.lastOrNull()

        // Moving averages rating
        var maBuy = 0
        var maSell = 0
        var maNeutral = 0

        if (lastClose > ema9) maBuy++ else if (lastClose < ema9) maSell++ else maNeutral++
        if (lastClose > ema21) maBuy++ else if (lastClose < ema21) maSell++ else maNeutral++
        if (lastClose > ema50) maBuy++ else if (lastClose < ema50) maSell++ else maNeutral++
        if (ema9 > ema21) maBuy++ else if (ema9 < ema21) maSell++ else maNeutral++
        if (ema21 > ema50) maBuy++ else if (ema21 < ema50) maSell++ else maNeutral++

        val maRating = when {
            maBuy >= 4 -> TechnicalRating.STRONG_BUY
            maBuy > maSell -> TechnicalRating.BUY
            maSell >= 4 -> TechnicalRating.STRONG_SELL
            maSell > maBuy -> TechnicalRating.SELL
            else -> TechnicalRating.NEUTRAL
        }

        // Oscillators rating
        var oscBuy = 0
        var oscSell = 0
        var oscNeutral = 0

        // RSI evaluation
        if (rsi < 30) oscBuy += 2 // Oversold -> Bullish bounce
        else if (rsi > 70) oscSell += 2 // Overbought -> Bearish reversal
        else oscNeutral++

        // MACD evaluation
        if (macd != null) {
            if (macd.histogram > 0) oscBuy++ else if (macd.histogram < 0) oscSell++ else oscNeutral++
            if (macd.macd > macd.signal) oscBuy++ else oscSell++
        }

        val oscRating = when {
            oscBuy >= 3 -> TechnicalRating.STRONG_BUY
            oscBuy > oscSell -> TechnicalRating.BUY
            oscSell >= 3 -> TechnicalRating.STRONG_SELL
            oscSell > oscBuy -> TechnicalRating.SELL
            else -> TechnicalRating.NEUTRAL
        }

        // Combined overall rating
        val totalBuy = maBuy + oscBuy
        val totalSell = maSell + oscSell
        val summaryScore = totalBuy - totalSell

        val overallRating = when {
            summaryScore >= 4 -> TechnicalRating.STRONG_BUY
            summaryScore in 1..3 -> TechnicalRating.BUY
            summaryScore in -3..-1 -> TechnicalRating.SELL
            summaryScore <= -4 -> TechnicalRating.STRONG_SELL
            else -> TechnicalRating.NEUTRAL
        }

        return TechnicalGauge(
            overallRating = overallRating,
            summaryScore = summaryScore,
            movingAverageRating = maRating,
            movingAverageBuyCount = maBuy,
            movingAverageSellCount = maSell,
            movingAverageNeutralCount = maNeutral,
            oscillatorRating = oscRating,
            oscillatorBuyCount = oscBuy,
            oscillatorSellCount = oscSell,
            oscillatorNeutralCount = oscNeutral,
            rsiValue = rsi,
            macdSignal = if (macd != null && macd.histogram >= 0) "Bullish Cross" else "Bearish Cross",
            adxStrength = if (abs(summaryScore) >= 3) "Strong Trend" else "Consolidation"
        )
    }
}
