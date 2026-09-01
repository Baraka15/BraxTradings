package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PositionEntity::class,
        OrderEntity::class,
        TradeLedgerEntity::class,
        AlertEntity::class,
        WatchlistItemEntity::class,
        AccountEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun orderDao(): OrderDao
    abstract fun tradeLedgerDao(): TradeLedgerDao
    abstract fun alertDao(): AlertDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "braxtradings_pro.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed initial defaults in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            database.accountDao().insertOrUpdate(
                                AccountEntity(
                                    id = 1,
                                    cashBalance = 100000.0,
                                    initialBalance = 100000.0,
                                    leverageMultiplier = 4.0,
                                    riskPerTradePercent = 2.0
                                )
                            )
                            val initialWatchlist = listOf(
                                WatchlistItemEntity("NVDA", "NVIDIA Corporation", "STOCK", true, 0),
                                WatchlistItemEntity("TSLA", "Tesla Inc.", "STOCK", true, 1),
                                WatchlistItemEntity("AAPL", "Apple Inc.", "STOCK", true, 2),
                                WatchlistItemEntity("AMD", "Advanced Micro Devices", "STOCK", true, 3),
                                WatchlistItemEntity("BTC/USD", "Bitcoin USD", "CRYPTO", true, 4),
                                WatchlistItemEntity("ETH/USD", "Ethereum USD", "CRYPTO", true, 5),
                                WatchlistItemEntity("SPY", "SPDR S&P 500 ETF", "INDEX", true, 6),
                                WatchlistItemEntity("QQQ", "Invesco QQQ Trust", "INDEX", true, 7),
                                WatchlistItemEntity("PLTR", "Palantir Technologies", "STOCK", true, 8),
                                WatchlistItemEntity("COIN", "Coinbase Global Inc.", "STOCK", true, 9)
                            )
                            database.watchlistDao().insertAll(initialWatchlist)
                            // Initial sample alerts
                            database.alertDao().insertAlert(
                                AlertEntity(
                                    alertId = "alert_init_1",
                                    symbol = "NVDA",
                                    conditionType = "RSI_OVERBOUGHT",
                                    targetValue = 70.0,
                                    note = "RSI Breakout alert for momentum day scalp"
                                )
                            )
                            database.alertDao().insertAlert(
                                AlertEntity(
                                    alertId = "alert_init_2",
                                    symbol = "TSLA",
                                    conditionType = "PCT_GAIN",
                                    targetValue = 3.5,
                                    note = "Day momentum surge target +3.5%"
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
