package com.example.domain.trading

enum class Timeframe(val label: String, val seconds: Long) {
    M1("1m", 60),
    M5("5m", 300),
    M15("15m", 900),
    H1("1h", 3600),
    D1("1D", 86400)
}

enum class ChartStyle(val label: String) {
    CANDLES("Candles"),
    HEIKIN_ASHI("Heikin-Ashi"),
    MOUNTAIN("Mountain"),
    HOLLOW_BARS("Hollow")
}

enum class TechnicalIndicator(val label: String, val category: String) {
    EMA_9("EMA 9", "Overlays"),
    EMA_21("EMA 21", "Overlays"),
    EMA_50("EMA 50", "Overlays"),
    EMA_200("EMA 200", "Overlays"),
    BOLLINGER_BANDS("Bollinger Bands (20,2)", "Overlays"),
    VWAP("VWAP", "Overlays"),
    SUPERTREND("Supertrend (10,3)", "Overlays"),
    VOLUME("Volume", "Oscillators"),
    RSI("RSI (14)", "Oscillators"),
    MACD("MACD (12,26,9)", "Oscillators")
}

enum class DrawingToolType(val label: String) {
    NONE("Select / Pan"),
    CROSSHAIR("Crosshair"),
    TRENDLINE("Trendline"),
    HORIZONTAL_RAY("Horiz Ray"),
    FIBONACCI("Fib Retracement"),
    RISK_REWARD("Long/Short R:R"),
    RULER("Measurement Ruler")
}

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class TickerQuote(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val marketCap: Double,
    val category: String = "CRYPTO"
)

data class OrderBookLevel(
    val price: Double,
    val quantity: Double,
    val total: Double,
    val depthPercentage: Float
)

data class MarketTrade(
    val id: String,
    val price: Double,
    val quantity: Double,
    val isBuyerMaker: Boolean,
    val timestamp: Long
)

data class PriceAlert(
    val id: String,
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val triggered: Boolean = false,
    val triggeredAt: Long? = null,
    val note: String = ""
)

data class PortfolioPosition(
    val symbol: String,
    val name: String,
    val shares: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null
) {
    val totalInvested: Double get() = shares * entryPrice
    val currentValue: Double get() = shares * currentPrice
    val pnl: Double get() = currentValue - totalInvested
    val pnlPercent: Double get() = if (totalInvested > 0) (pnl / totalInvested) * 100 else 0.0
}

data class TechnicalSignalSummary(
    val symbol: String,
    val rating: String,
    val buyCount: Int,
    val neutralCount: Int,
    val sellCount: Int,
    val score: Float
)

data class CommunityPost(
    val id: String,
    val authorName: String,
    val authorUsername: String,
    val timeAgo: String,
    val title: String,
    val description: String,
    val symbol: String,
    val chartImageRes: Int? = null,
    val likes: Int,
    val comments: Int
)

data class ExploreNews(
    val id: String,
    val publisher: String,
    val timeAgo: String,
    val title: String,
    val symbol: String? = null
)

data class DrawingShape(
    val id: String,
    val type: DrawingToolType,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val startPrice: Double = 0.0,
    val endPrice: Double = 0.0
)

enum class WorkspaceLayoutMode(val id: Int, val label: String, val panesCount: Int, val description: String) {
    SINGLE(1, "1x1", 1, "Single Chart"),
    SPLIT_HORIZONTAL(2, "1x2", 2, "Split Horizontal"),
    SPLIT_VERTICAL(3, "2x1", 2, "Split Vertical"),
    GRID_4(4, "2x2", 4, "Grid 4-Pane")
}

data class ChartPaneConfig(
    val id: Int,
    val symbol: String,
    val timeframe: Timeframe,
    val indicators: Set<TechnicalIndicator> = setOf(TechnicalIndicator.EMA_21, TechnicalIndicator.VOLUME),
    val chartStyle: ChartStyle = ChartStyle.CANDLES
)

data class CrosshairSyncState(
    val isEnabled: Boolean = false,
    val active: Boolean = false,
    val sourcePaneId: Int = -1,
    val timestamp: Long = 0L,
    val normalizedX: Float = -1f,
    val price: Double = 0.0
)

