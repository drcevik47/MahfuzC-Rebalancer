package com.mahfuz.rebalancer.data.repository

import com.mahfuz.rebalancer.data.db.*
import com.mahfuz.rebalancer.data.model.PortfolioConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepository @Inject constructor(
    private val portfolioCoinDao: PortfolioCoinDao,
    private val portfolioSettingsDao: PortfolioSettingsDao,
    private val tradeHistoryDao: TradeHistoryDao,
    private val rebalanceEventDao: RebalanceEventDao
) {
    // Portfolio Coins
    fun getActiveCoins(): Flow<List<PortfolioCoinEntity>> = portfolioCoinDao.getActiveCoins()

    fun getAllCoins(): Flow<List<PortfolioCoinEntity>> = portfolioCoinDao.getAllCoins()

    suspend fun addCoin(coin: String, targetPercentage: Double) {
        portfolioCoinDao.insert(
            PortfolioCoinEntity(
                coin = coin,
                targetPercentage = targetPercentage,
                isActive = true
            )
        )
    }

    suspend fun updateCoinPercentage(coin: String, targetPercentage: Double) {
        val existing = portfolioCoinDao.getCoin(coin)
        if (existing != null) {
            portfolioCoinDao.update(existing.copy(targetPercentage = targetPercentage))
        }
    }

    suspend fun removeCoin(coin: String) {
        portfolioCoinDao.deleteByCoin(coin)
    }

    suspend fun setCoinActive(coin: String, active: Boolean) {
        val existing = portfolioCoinDao.getCoin(coin)
        if (existing != null) {
            portfolioCoinDao.update(existing.copy(isActive = active))
        }
    }

    suspend fun getTotalPercentage(): Double = portfolioCoinDao.getTotalPercentage() ?: 0.0

    suspend fun clearAllCoins() = portfolioCoinDao.deleteAll()

    // Portfolio Settings
    fun getSettings(): Flow<PortfolioSettingsEntity?> = portfolioSettingsDao.getSettings()

    suspend fun getSettingsOnce(): PortfolioSettingsEntity? = portfolioSettingsDao.getSettingsOnce()

    suspend fun initializeSettings() {
        if (portfolioSettingsDao.getSettingsOnce() == null) {
            portfolioSettingsDao.insert(PortfolioSettingsEntity())
        }
    }

    suspend fun setThreshold(threshold: Double) {
        initializeSettings()
        portfolioSettingsDao.setThreshold(threshold)
    }

    suspend fun setRebalancingActive(active: Boolean) {
        initializeSettings()
        portfolioSettingsDao.setRebalancingActive(active)
    }

    // Combined Portfolio Config
    fun getPortfolioConfig(): Flow<PortfolioConfig> {
        return portfolioCoinDao.getActiveCoins().map { coins ->
            val settings = portfolioSettingsDao.getSettingsOnce()
            PortfolioConfig(
                coins = coins.associate { it.coin to BigDecimal(it.targetPercentage) },
                threshold = BigDecimal(settings?.threshold ?: 1.0),
                isActive = settings?.isRebalancingActive ?: false
            )
        }
    }

    // Trade History
    suspend fun recordTrade(trade: TradeHistoryEntity): Long = tradeHistoryDao.insert(trade)

    suspend fun updateTrade(trade: TradeHistoryEntity) = tradeHistoryDao.update(trade)

    fun getRecentTrades(limit: Int = 100): Flow<List<TradeHistoryEntity>> =
        tradeHistoryDao.getRecentTrades(limit)

    suspend fun getTradeByOrderId(orderId: String): TradeHistoryEntity? =
        tradeHistoryDao.getTradeByOrderId(orderId)

    suspend fun clearAllTrades() = tradeHistoryDao.deleteAllTrades()

    // Rebalance Events
    suspend fun recordRebalanceEvent(event: RebalanceEventEntity): Long =
        rebalanceEventDao.insert(event)

    suspend fun updateRebalanceEvent(event: RebalanceEventEntity) =
        rebalanceEventDao.update(event)

    fun getRecentRebalanceEvents(limit: Int = 50): Flow<List<RebalanceEventEntity>> =
        rebalanceEventDao.getRecentEvents(limit)

    // Cleanup
    suspend fun cleanupOldData(retentionDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        tradeHistoryDao.deleteOldTrades(cutoffTime)
        rebalanceEventDao.deleteOldEvents(cutoffTime)
    }
}
