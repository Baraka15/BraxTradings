package com.example.data.market

enum class AssetType(val label: String) {
    STOCK("Stock"),
    CRYPTO("Crypto"),
    INDEX("Index Futures")
}

enum class TimeFrame(val label: String, val seconds: Long) {
    M1("1m", 60),
    M5("5m", 300),
    M15("15m", 900),
    H1("1h", 3600),
    D1("1D", 86400)
}

enum class ChartStyle(val label: String) {
    CANDLESTICK("Candles"),
    HOLLOW_CANDLES("Hollow"),
    LINE("Line"),
    AREA("Area"),
    HEIKIN_ASHI("Heikin-Ashi"),
    BARS("OHLC Bars")
}

enum class DrawingToolType(val label: String, val iconName: String) {
    CURSOR("Crosshair", "crosshair"),
    TRENDLINE("Trendline", "trending_up"),
    HORIZONTAL_RAY("Horizontal Ray", "horizontal_rule"),
    FIBONACCI("Fib Retracement", "calculate"),
    PRICE_RULER("Price Ruler", "straighten"),
    LONG_POSITION("Long Position", "arrow_upward"),
    SHORT_POSITION("Short Position", "arrow_downward"),
    RECTANGLE_ZONE("Zone / Box", "crop_square")
}

data class PricePoint(
    val candleIndex: Int,
    val price: Double,
    val timestamp: Long = 0L
)

data class DrawingItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolType: DrawingToolType,
    val point1: PricePoint,
    val point2: PricePoint? = null,
    val colorHex: Long = 0xFF6750A4, // M3 Primary
    val strokeWidth: Float = 2.5f,
    val textNote: String = "",
    val targetRatio: Double = 3.0 // For long/short risk reward
)

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
}

data class OrderBookLevel(
    val price: Double,
    val size: Double,
    val total: Double,
    val depthPercentage: Float
)

data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookLevel>, // Sorted highest price to lowest
    val asks: List<OrderBookLevel>, // Sorted lowest price to highest
    val spread: Double,
    val spreadPercent: Double
)

data class PriceTick(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Double,
    val bid: Double,
    val ask: Double,
    val bidSize: Double,
    val askSize: Double,
    val high: Double,
    val low: Double,
    val isUptick: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class TradeTapeItem(
    val id: String,
    val symbol: String,
    val price: Double,
    val size: Double,
    val isBuyerMaker: Boolean, // true = buy aggression (green), false = sell aggression (red)
    val timestamp: Long = System.currentTimeMillis()
)

data class Quote(
    val symbol: String,
    val name: String,
    val price: Double,
    val previousClose: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val avgVolume: Long,
    val bid: Double,
    val ask: Double,
    val bidSize: Double,
    val askSize: Double,
    val vwap: Double,
    val rsi14: Double,
    val ema9: Double,
    val ema21: Double,
    val ema50: Double,
    val upperBollinger: Double,
    val lowerBollinger: Double,
    val middleBollinger: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHistogram: Double,
    val assetType: AssetType,
    val sparkline: List<Double> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    val change: Double get() = price - previousClose
    val changePercent: Double get() = if (previousClose > 0) ((price - previousClose) / previousClose) * 100.0 else 0.0
    val isPositive: Boolean get() = change >= 0.0
}

enum class SignalType(val title: String, val isBullish: Boolean) {
    RSI_OVERSOLD("RSI Oversold Scalp (<30)", true),
    RSI_OVERBOUGHT("RSI Overbought Pullback (>70)", false),
    MACD_BULLISH_CROSS("MACD Golden Crossover", true),
    MACD_BEARISH_CROSS("MACD Death Crossover", false),
    BOLLINGER_LOWER_BOUNCE("Bollinger Lower Reversal", true),
    BOLLINGER_UPPER_REJECTION("Bollinger Upper Resistance", false),
    VOLUME_BREAKOUT("Unusual Volume Breakout", true),
    EMA_TREND_SURGE("EMA 9/21 Trend Surge", true)
}

data class TechnicalSignal(
    val id: String,
    val symbol: String,
    val type: SignalType,
    val price: Double,
    val confidence: Int, // e.g. 85%
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

