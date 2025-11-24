package com.bybit.rebalancer.data.database

import androidx.room.*
import com.bybit.rebalancer.data.model.PortfolioCoin
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioCoinDao {

    @Query("SELECT * FROM portfolio_coins ORDER BY addedAt ASC")
    fun getAllCoins(): Flow<List<PortfolioCoin>>

    @Query("SELECT * FROM portfolio_coins WHERE isEnabled = 1 ORDER BY addedAt ASC")
    fun getEnabledCoins(): Flow<List<PortfolioCoin>>

    @Query("SELECT * FROM portfolio_coins WHERE isEnabled = 1")
    suspend fun getEnabledCoinsOnce(): List<PortfolioCoin>

    @Query("SELECT * FROM portfolio_coins WHERE coin = :coin")
    suspend fun getCoinBySymbol(coin: String): PortfolioCoin?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoin(coin: PortfolioCoin)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<PortfolioCoin>)

    @Update
    suspend fun updateCoin(coin: PortfolioCoin)

    @Delete
    suspend fun deleteCoin(coin: PortfolioCoin)

    @Query("DELETE FROM portfolio_coins WHERE coin = :coin")
    suspend fun deleteCoinBySymbol(coin: String)

    @Query("DELETE FROM portfolio_coins")
    suspend fun deleteAllCoins()

    @Query("SELECT SUM(targetPercentage) FROM portfolio_coins WHERE isEnabled = 1")
    suspend fun getTotalTargetPercentage(): Double?

    @Query("UPDATE portfolio_coins SET targetPercentage = :percentage WHERE coin = :coin")
    suspend fun updateTargetPercentage(coin: String, percentage: Double)

    @Query("UPDATE portfolio_coins SET isEnabled = :enabled WHERE coin = :coin")
    suspend fun setEnabled(coin: String, enabled: Boolean)
}
