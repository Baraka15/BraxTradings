package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    @Query("SELECT * FROM positions WHERE shares > 0 ORDER BY symbol ASC")
    fun getAllPositions(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions WHERE symbol = :symbol LIMIT 1")
    suspend fun getPositionBySymbol(symbol: String): PositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(position: PositionEntity)

    @Query("DELETE FROM positions WHERE symbol = :symbol")
    suspend fun deletePosition(symbol: String)

    @Query("DELETE FROM positions")
    suspend fun clearAll()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE symbol = :symbol ORDER BY createdAt DESC")
    fun getOrdersForSymbol(symbol: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = 'CANCELLED' WHERE orderId = :orderId AND status = 'PENDING'")
    suspend fun cancelOrder(orderId: String): Int

    @Query("DELETE FROM orders")
    suspend fun clearAll()
}

@Dao
interface TradeLedgerDao {
    @Query("SELECT * FROM trade_ledger ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeLedgerEntity>>

    @Query("SELECT * FROM trade_ledger WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getTradesForSymbol(symbol: String): Flow<List<TradeLedgerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeLedgerEntity)

    @Query("SELECT SUM(realizedPnL) FROM trade_ledger")
    fun getTotalRealizedPnL(): Flow<Double?>

    @Query("DELETE FROM trade_ledger")
    suspend fun clearAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isTriggered = 1, triggeredAt = :triggeredAt, triggerPrice = :triggerPrice, isActive = 0 WHERE alertId = :alertId")
    suspend fun markAlertTriggered(alertId: String, triggeredAt: Long, triggerPrice: Double)

    @Query("UPDATE alerts SET isActive = :isActive WHERE alertId = :alertId")
    suspend fun toggleAlertActive(alertId: String, isActive: Boolean)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE alertId = :alertId")
    suspend fun deleteAlertById(alertId: String)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY displayOrder ASC, symbol ASC")
    fun getAllWatchlistItems(): Flow<List<WatchlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WatchlistItemEntity>)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE symbol = :symbol")
    suspend fun containsSymbol(symbol: String): Int
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM account_state WHERE id = 1 LIMIT 1")
    fun getAccountFlow(): Flow<AccountEntity?>

    @Query("SELECT * FROM account_state WHERE id = 1 LIMIT 1")
    suspend fun getAccount(): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: AccountEntity)

    @Query("UPDATE account_state SET cashBalance = :newCash, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateCashBalance(newCash: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE account_state SET leverageMultiplier = :leverage, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLeverage(leverage: Double, updatedAt: Long = System.currentTimeMillis())
}
