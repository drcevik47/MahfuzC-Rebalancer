package com.mahfuz.rebalancer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LogEntry::class,
        PortfolioCoinEntity::class,
        PortfolioSettingsEntity::class,
        TradeHistoryEntity::class,
        PriceHistoryEntity::class,
        RebalanceEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao
    abstract fun portfolioCoinDao(): PortfolioCoinDao
    abstract fun portfolioSettingsDao(): PortfolioSettingsDao
    abstract fun tradeHistoryDao(): TradeHistoryDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun rebalanceEventDao(): RebalanceEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rebalancer_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
