package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val shares: Double,
    val averagePrice: Double,
    val totalCost: Double,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val type: String, // "MARKET", "LIMIT", "STOP_LOSS", "TAKE_PROFIT"
    val quantity: Double,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val status: String, // "PENDING", "FILLED", "CANCELLED", "REJECTED"
    val filledPrice: Double? = null,
    val filledQuantity: Double = 0.0,
    val totalAmount: Double = 0.0,
    val fee: Double = 0.0,
    val rejectReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val filledAt: Long? = null
)

@Entity(tableName = "trade_ledger")
data class TradeLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tradeId: String,
    val orderId: String,
    val symbol: String,
    val side: String, // "BUY" or "SELL"
    val quantity: Double,
    val executionPrice: Double,
    val totalAmount: Double,
    val fee: Double = 0.0,
    val realizedPnL: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val alertId: String,
    val symbol: String,
    val conditionType: String, // "PRICE_ABOVE", "PRICE_BELOW", "PCT_GAIN", "PCT_DROP", "RSI_OVERBOUGHT", "RSI_OVERSOLD", "MACD_CROSSOVER", "VOL_SPIKE"
    val targetValue: Double,
    val note: String = "",
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val triggerPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "watchlist")
data class WatchlistItemEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val assetType: String, // "STOCK", "CRYPTO", "INDEX"
    val isFavorite: Boolean = true,
    val displayOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "account_state")
data class AccountEntity(
    @PrimaryKey val id: Int = 1,
    val cashBalance: Double = 100000.0,
    val initialBalance: Double = 100000.0,
    val leverageMultiplier: Double = 4.0, // 4x Day Trading Margin
    val riskPerTradePercent: Double = 2.0,
    val updatedAt: Long = System.currentTimeMillis()
)
