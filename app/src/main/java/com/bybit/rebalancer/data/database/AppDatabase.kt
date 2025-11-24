package com.bybit.rebalancer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bybit.rebalancer.data.model.AppLog
import com.bybit.rebalancer.data.model.PortfolioCoin
import com.bybit.rebalancer.data.model.TradeLog

@Database(
    entities = [
        PortfolioCoin::class,
        TradeLog::class,
        AppLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portfolioCoinDao(): PortfolioCoinDao
    abstract fun tradeLogDao(): TradeLogDao
    abstract fun appLogDao(): AppLogDao
}
