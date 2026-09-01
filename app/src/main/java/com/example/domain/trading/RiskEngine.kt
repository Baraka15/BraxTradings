package com.example.domain.trading

import com.example.data.local.AccountEntity
import com.example.data.local.PositionEntity
import com.example.data.market.Quote

class RiskEngine {

    /**
     * Synchronous pre-trade validation.
     * Validates purchasing power, quantity bounds, position coverage for sell orders, and price sanity.
     */
    fun validateOrder(
        request: OrderRequest,
        account: AccountEntity,
        currentQuote: Quote?,
        currentPosition: PositionEntity?
    ): RiskValidationResult {
        if (request.quantity <= 0.0) {
            return RiskValidationResult.Rejected("Order quantity must be greater than zero.")
        }

        if (currentQuote == null) {
            return RiskValidationResult.Rejected("No live market data available for ${request.symbol}.")
        }

        val estimatedPrice = when (request.type) {
            OrderType.MARKET -> currentQuote.ask
            OrderType.LIMIT -> request.limitPrice ?: currentQuote.ask
            OrderType.STOP_LOSS, OrderType.TAKE_PROFIT -> request.stopPrice ?: currentQuote.bid
        }

        if (estimatedPrice <= 0.0) {
            return RiskValidationResult.Rejected("Invalid order execution price.")
        }

        val totalOrderValue = estimatedPrice * request.quantity
        val maxBuyingPower = account.cashBalance * account.leverageMultiplier

        if (request.side == OrderSide.BUY) {
            // Cash & Buying Power check
            if (totalOrderValue > maxBuyingPower) {
                val formattedNeeded = "$%,.2f".format(totalOrderValue)
                val formattedPower = "$%,.2f".format(maxBuyingPower)
                return RiskValidationResult.Rejected(
                    "Insufficient buying power ($formattedPower). Order requires $formattedNeeded."
                )
            }

            // Single position concentration limit check (max 60% of total buying power per trade)
            val maxSingleTrade = maxBuyingPower * 0.75
            if (totalOrderValue > maxSingleTrade) {
                return RiskValidationResult.Rejected(
                    "Order exceeds max risk allocation limit (75% of max portfolio power)."
                )
            }
        } else {
            // SELL Order position check
            val existingShares = currentPosition?.shares ?: 0.0
            if (request.quantity > existingShares) {
                return RiskValidationResult.Rejected(
                    "Cannot sell ${request.quantity} shares. You currently hold ${"%.2f".format(existingShares)} shares."
                )
            }
        }

        // Limit price validation sanity
        if (request.type == OrderType.LIMIT) {
            val limit = request.limitPrice ?: 0.0
            if (limit <= 0.0) {
                return RiskValidationResult.Rejected("Limit price must be greater than $0.00.")
            }
            val priceDeviationPct = kotlin.math.abs(limit - currentQuote.price) / currentQuote.price
            if (priceDeviationPct > 0.40) {
                return RiskValidationResult.Rejected("Limit price deviates more than 40% from market price.")
            }
        }

        return RiskValidationResult.Approved
    }
}
