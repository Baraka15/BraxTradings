package com.example.domain.trading

import com.example.data.local.OrderEntity
import com.example.data.market.Quote
import java.util.UUID

data class OrderFill(
    val orderId: String,
    val symbol: String,
    val side: OrderSide,
    val quantity: Double,
    val fillPrice: Double,
    val totalAmount: Double,
    val fee: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

class MatchingEngine(
    private val onOrderFilled: suspend (OrderFill, OrderEntity) -> Unit
) {
    // In-memory working limit / stop orders
    private val workingOrders = mutableListOf<OrderEntity>()

    @Synchronized
    fun registerOrder(order: OrderEntity) {
        if (order.status == OrderStatus.PENDING.name) {
            workingOrders.removeAll { it.orderId == order.orderId }
            workingOrders.add(order)
        }
    }

    @Synchronized
    fun cancelOrder(orderId: String): Boolean {
        return workingOrders.removeAll { it.orderId == orderId }
    }

    @Synchronized
    fun syncPendingOrders(orders: List<OrderEntity>) {
        workingOrders.clear()
        workingOrders.addAll(orders.filter { it.status == OrderStatus.PENDING.name })
    }

    /**
     * Executes immediate market order fill against current quote
     */
    suspend fun executeMarketOrder(order: OrderEntity, currentQuote: Quote): OrderFill {
        val fillPrice = if (order.side == OrderSide.BUY.name) currentQuote.ask else currentQuote.bid
        val total = fillPrice * order.quantity
        val fill = OrderFill(
            orderId = order.orderId,
            symbol = order.symbol,
            side = OrderSide.valueOf(order.side),
            quantity = order.quantity,
            fillPrice = fillPrice,
            totalAmount = total,
            fee = 0.0
        )
        onOrderFilled(fill, order)
        return fill
    }

    /**
     * Evaluates all working limit and stop orders against incoming quote tick
     */
    suspend fun processQuoteTick(quote: Quote) {
        val fillsToProcess = mutableListOf<Pair<OrderFill, OrderEntity>>()

        synchronized(this) {
            val iterator = workingOrders.iterator()
            while (iterator.hasNext()) {
                val order = iterator.next()
                if (order.symbol != quote.symbol) continue

                var shouldFill = false
                var executedPrice = quote.price

                when (order.type) {
                    OrderType.LIMIT.name -> {
                        val limitPrice = order.limitPrice ?: continue
                        if (order.side == OrderSide.BUY.name && quote.ask <= limitPrice) {
                            shouldFill = true
                            executedPrice = quote.ask
                        } else if (order.side == OrderSide.SELL.name && quote.bid >= limitPrice) {
                            shouldFill = true
                            executedPrice = quote.bid
                        }
                    }
                    OrderType.STOP_LOSS.name -> {
                        val stopPrice = order.stopPrice ?: continue
                        if (order.side == OrderSide.SELL.name && quote.bid <= stopPrice) {
                            shouldFill = true
                            executedPrice = quote.bid
                        }
                    }
                    OrderType.TAKE_PROFIT.name -> {
                        val stopPrice = order.stopPrice ?: continue
                        if (order.side == OrderSide.SELL.name && quote.bid >= stopPrice) {
                            shouldFill = true
                            executedPrice = quote.bid
                        }
                    }
                }

                if (shouldFill) {
                    iterator.remove()
                    val total = executedPrice * order.quantity
                    val fill = OrderFill(
                        orderId = order.orderId,
                        symbol = order.symbol,
                        side = OrderSide.valueOf(order.side),
                        quantity = order.quantity,
                        fillPrice = executedPrice,
                        totalAmount = total,
                        fee = 0.0
                    )
                    fillsToProcess.add(Pair(fill, order))
                }
            }
        }

        for ((fill, order) in fillsToProcess) {
            onOrderFilled(fill, order)
        }
    }
}
