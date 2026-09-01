package com.example.domain.trading

enum class OrderSide(val label: String) {
    BUY("Buy / Long"),
    SELL("Sell / Short")
}

enum class OrderType(val label: String) {
    MARKET("Market"),
    LIMIT("Limit"),
    STOP_LOSS("Stop Loss"),
    TAKE_PROFIT("Take Profit")
}

enum class OrderStatus(val label: String) {
    PENDING("Working / Open"),
    FILLED("Filled / Settled"),
    CANCELLED("Cancelled"),
    REJECTED("Rejected")
}

data class OrderRequest(
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val quantity: Double,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null
)

sealed class RiskValidationResult {
    data object Approved : RiskValidationResult()
    data class Rejected(val reason: String) : RiskValidationResult()
}

data class PortfolioSummary(
    val netLiquidity: Double,
    val cashBalance: Double,
    val investedValue: Double,
    val buyingPower: Double,
    val unrealizedPnL: Double,
    val unrealizedPnLPercent: Double,
    val totalRealizedPnL: Double,
    val todayPnL: Double,
    val todayPnLPercent: Double,
    val openPositionsCount: Int,
    val winRatePercent: Double,
    val totalTradesCount: Int
)
