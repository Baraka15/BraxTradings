package com.example.domain.trading

data class Instrument(
    val symbol: String,
    val name: String,
    val category: String, // STOCKS, CRYPTO, FOREX, COMMODITIES
    val currentPrice: Double,
    val change24h: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val sparkline: List<Double> = emptyList()
)

data class CandleStick(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class TimeFrame(val label: String, val seconds: Long) {
    M1("1m", 60),
    M5("5m", 300),
    M15("15m", 900),
    H1("1h", 3600),
    D1("1D", 86400)
}

enum class OrderSide { BUY, SELL }
enum class OrderType { MARKET, LIMIT }

data class Order(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val quantity: Double,
    val price: Double,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Position(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Double,
    val entryPrice: Double,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercentage: Double,
    val leverage: Double = 1.0
)

data class OrderBookLevel(
    val price: Double,
    val size: Double,
    val total: Double
)

data class OrderBookDepth(
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val spread: Double
)

data class TapeTrade(
    val id: String,
    val time: String,
    val price: Double,
    val size: Double,
    val side: OrderSide
)

enum class AlertCondition(val title: String, val description: String) {
    CROSSES_ABOVE("Rises Above (≥)", "Trigger when price rises to or crosses above target"),
    CROSSES_BELOW("Falls Below (≤)", "Trigger when price drops to or crosses below target")
}

data class StockPriceAlert(
    val id: String,
    val symbol: String,
    val instrumentName: String,
    val targetPrice: Double,
    val initialPriceAtCreation: Double,
    val condition: AlertCondition,
    val note: String = "",
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val triggerPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RiskAlertLevel(val label: String, val description: String) {
    SAFE("Optimal / Safe", "Margin utilization is well within conservative day-trading limits"),
    ELEVATED("Elevated Exposure", "Exposure is moderate; monitor stop-losses and market volatility"),
    HIGH_RISK("High Risk Alert", "High leverage or margin usage; approaching maximum safe threshold"),
    CRITICAL_MARGIN_CALL("Margin Call Danger", "Immediate risk of liquidation; reduce position sizes immediately")
}

data class ExposureSlice(
    val id: String,
    val label: String,
    val subLabel: String,
    val notionalValue: Double,
    val percentage: Double,
    val colorHex: Long
)

data class PositionRiskAssessment(
    val position: Position,
    val instrument: Instrument?,
    val notionalExposure: Double,
    val marginAllocated: Double,
    val exposureSharePct: Double,
    val liquidationPrice: Double,
    val distToLiquidationPct: Double,
    val var95Risk: Double,
    val riskScore: Int
)

data class AccountRiskMetrics(
    val totalEquity: Double,
    val usedMargin: Double,
    val freeMargin: Double,
    val marginUtilizationPct: Double,
    val grossNotionalExposure: Double,
    val netExposure: Double,
    val longExposure: Double,
    val shortExposure: Double,
    val effectiveLeverage: Double,
    val valueAtRisk95: Double,
    val dailyLossLimit: Double,
    val currentDailyPnL: Double,
    val dailyLossUtilizationPct: Double,
    val riskLevel: RiskAlertLevel,
    val closestLiquidationDistancePct: Double
)

enum class PriceDirection {
    UP, DOWN, NEUTRAL
}

enum class StreamTransportMode(val label: String, val badge: String) {
    WEBSOCKET("WebSocket (Push)", "WS LIVE"),
    POLLING("HTTP Polling (Pull)", "POLL LIVE")
}

data class TickerStreamInfo(
    val isStreaming: Boolean,
    val transportMode: StreamTransportMode,
    val intervalMs: Long,
    val totalTicksReceived: Long,
    val latencyMs: Long,
    val isConnected: Boolean,
    val lastTickTimestamp: Long
)

