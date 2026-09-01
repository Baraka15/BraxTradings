package com.example.domain.trading

import com.example.data.local.*
import com.example.data.market.Quote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class LedgerService(
    private val database: AppDatabase
) {
    private val accountDao = database.accountDao()
    private val positionDao = database.positionDao()
    private val orderDao = database.orderDao()
    private val tradeLedgerDao = database.tradeLedgerDao()

    /**
     * Atomically process trade execution fill and settle balances and positions
     */
    suspend fun settleOrderFill(fill: OrderFill, order: OrderEntity): TradeLedgerEntity = withContext(Dispatchers.IO) {
        val account = accountDao.getAccount() ?: AccountEntity()
        val currentPosition = positionDao.getPositionBySymbol(fill.symbol)
        var realizedPnL = 0.0

        if (fill.side == OrderSide.BUY) {
            // Deduct cash balance
            val newCash = account.cashBalance - fill.totalAmount
            accountDao.updateCashBalance(newCash)

            // Update or create position
            if (currentPosition == null) {
                val newPosition = PositionEntity(
                    symbol = fill.symbol,
                    name = fill.symbol,
                    shares = fill.quantity,
                    averagePrice = fill.fillPrice,
                    totalCost = fill.totalAmount,
                    updatedAt = fill.timestamp
                )
                positionDao.insertOrUpdate(newPosition)
            } else {
                val updatedShares = currentPosition.shares + fill.quantity
                val updatedCost = currentPosition.totalCost + fill.totalAmount
                val updatedAvg = if (updatedShares > 0) updatedCost / updatedShares else fill.fillPrice
                val updatedPosition = currentPosition.copy(
                    shares = updatedShares,
                    averagePrice = updatedAvg,
                    totalCost = updatedCost,
                    updatedAt = fill.timestamp
                )
                positionDao.insertOrUpdate(updatedPosition)
            }
        } else {
            // SELL - Credit cash balance & calculate realized P&L
            val entryPrice = currentPosition?.averagePrice ?: fill.fillPrice
            realizedPnL = (fill.fillPrice - entryPrice) * fill.quantity

            val newCash = account.cashBalance + fill.totalAmount
            accountDao.updateCashBalance(newCash)

            if (currentPosition != null) {
                val remainingShares = currentPosition.shares - fill.quantity
                if (remainingShares <= 0.0001) {
                    positionDao.deletePosition(fill.symbol)
                } else {
                    val remainingCost = remainingShares * currentPosition.averagePrice
                    val updatedPosition = currentPosition.copy(
                        shares = remainingShares,
                        totalCost = remainingCost,
                        updatedAt = fill.timestamp
                    )
                    positionDao.insertOrUpdate(updatedPosition)
                }
            }
        }

        // Update Order state in database
        val updatedOrder = order.copy(
            status = OrderStatus.FILLED.name,
            filledPrice = fill.fillPrice,
            filledQuantity = fill.quantity,
            totalAmount = fill.totalAmount,
            filledAt = fill.timestamp
        )
        orderDao.updateOrder(updatedOrder)

        // Write immutable trade ledger entry
        val tradeEntry = TradeLedgerEntity(
            tradeId = "TXN_${UUID.randomUUID().toString().take(8).uppercase()}",
            orderId = fill.orderId,
            symbol = fill.symbol,
            side = fill.side.name,
            quantity = fill.quantity,
            executionPrice = fill.fillPrice,
            totalAmount = fill.totalAmount,
            fee = fill.fee,
            realizedPnL = realizedPnL,
            timestamp = fill.timestamp
        )
        tradeLedgerDao.insertTrade(tradeEntry)

        tradeEntry
    }

    /**
     * Computes real-time portfolio metrics given live market quotes
     */
    fun computePortfolioSummary(
        account: AccountEntity?,
        positions: List<PositionEntity>,
        trades: List<TradeLedgerEntity>,
        quotes: Map<String, Quote>
    ): PortfolioSummary {
        val cash = account?.cashBalance ?: 100000.0
        val leverage = account?.leverageMultiplier ?: 4.0

        var totalInvested = 0.0
        var totalCurrentMarketValue = 0.0

        for (pos in positions) {
            val currentPrice = quotes[pos.symbol]?.price ?: pos.averagePrice
            totalInvested += pos.totalCost
            totalCurrentMarketValue += (pos.shares * currentPrice)
        }

        val unrealizedPnL = totalCurrentMarketValue - totalInvested
        val unrealizedPnLPercent = if (totalInvested > 0) (unrealizedPnL / totalInvested) * 100.0 else 0.0

        val totalRealized = trades.sumOf { it.realizedPnL }
        val netLiquidity = cash + totalCurrentMarketValue
        val buyingPower = cash * leverage

        val winningTrades = trades.count { it.realizedPnL > 0 }
        val winRate = if (trades.isNotEmpty()) (winningTrades.toDouble() / trades.size) * 100.0 else 0.0

        val todayPnL = unrealizedPnL + totalRealized
        val initialBal = account?.initialBalance ?: 100000.0
        val todayPnLPercent = if (initialBal > 0) ((netLiquidity - initialBal) / initialBal) * 100.0 else 0.0

        return PortfolioSummary(
            netLiquidity = netLiquidity,
            cashBalance = cash,
            investedValue = totalCurrentMarketValue,
            buyingPower = buyingPower,
            unrealizedPnL = unrealizedPnL,
            unrealizedPnLPercent = unrealizedPnLPercent,
            totalRealizedPnL = totalRealized,
            todayPnL = todayPnL,
            todayPnLPercent = todayPnLPercent,
            openPositionsCount = positions.size,
            winRatePercent = winRate,
            totalTradesCount = trades.size
        )
    }

    suspend fun resetAccountBalance(amount: Double) = withContext(Dispatchers.IO) {
        accountDao.insertOrUpdate(
            AccountEntity(
                id = 1,
                cashBalance = amount,
                initialBalance = amount,
                leverageMultiplier = 4.0
            )
        )
        positionDao.clearAll()
        orderDao.clearAll()
        tradeLedgerDao.clearAll()
    }
}
