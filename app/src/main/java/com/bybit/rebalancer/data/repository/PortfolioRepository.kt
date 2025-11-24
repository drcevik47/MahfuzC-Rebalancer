package com.bybit.rebalancer.data.repository

import com.bybit.rebalancer.api.BybitApiService
import com.bybit.rebalancer.api.BybitWebSocket
import com.bybit.rebalancer.data.database.PortfolioCoinDao
import com.bybit.rebalancer.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepository @Inject constructor(
    private val portfolioCoinDao: PortfolioCoinDao,
    private val apiService: BybitApiService,
    private val webSocket: BybitWebSocket,
    private val logRepository: LogRepository
) {
    private val gson = Gson()

    // Portföy coinleri
    fun getAllCoins(): Flow<List<PortfolioCoin>> = portfolioCoinDao.getAllCoins()
    fun getEnabledCoins(): Flow<List<PortfolioCoin>> = portfolioCoinDao.getEnabledCoins()
    suspend fun getEnabledCoinsOnce(): List<PortfolioCoin> = portfolioCoinDao.getEnabledCoinsOnce()

    suspend fun addCoin(coin: PortfolioCoin) {
        portfolioCoinDao.insertCoin(coin)
        logRepository.logInfo("Portfolio", "Coin eklendi: ${coin.coin} - Hedef: %${coin.targetPercentage}")
    }

    suspend fun updateCoin(coin: PortfolioCoin) {
        portfolioCoinDao.updateCoin(coin)
    }

    suspend fun removeCoin(coin: String) {
        portfolioCoinDao.deleteCoinBySymbol(coin)
        logRepository.logInfo("Portfolio", "Coin kaldırıldı: $coin")
    }

    suspend fun updateTargetPercentage(coin: String, percentage: Double) {
        portfolioCoinDao.updateTargetPercentage(coin, percentage)
        logRepository.logDebug("Portfolio", "$coin hedef yüzdesi güncellendi: %$percentage")
    }

    suspend fun getTotalTargetPercentage(): Double {
        return portfolioCoinDao.getTotalTargetPercentage() ?: 0.0
    }

    /**
     * Cüzdan bakiyelerini API'den al
     */
    suspend fun getWalletBalances(): Result<List<CoinBalance>> {
        return try {
            val response = apiService.getWalletBalance()
            if (response.isSuccessful && response.body()?.retCode == 0) {
                val result = response.body()?.result
                val balances = result?.list?.firstOrNull()?.coin?.map { coinInfo ->
                    CoinBalance(
                        coin = coinInfo.coin,
                        walletBalance = coinInfo.walletBalance,
                        availableToWithdraw = coinInfo.availableToWithdraw,
                        usdValue = coinInfo.usdValue
                    )
                } ?: emptyList()

                logRepository.logDebug("API", "Cüzdan bakiyeleri alındı: ${balances.size} coin")
                Result.success(balances)
            } else {
                val errorMsg = response.body()?.retMsg ?: response.message()
                logRepository.logError("API", "Cüzdan bakiyeleri alınamadı: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logRepository.logError("API", "Cüzdan bakiyeleri hatası: ${e.message}", e.stackTraceToString())
            Result.failure(e)
        }
    }

    /**
     * Tüm trading çiftlerini al
     */
    suspend fun getAvailableInstruments(): Result<List<InstrumentInfo>> {
        return try {
            val response = apiService.getInstrumentsInfo()
            if (response.isSuccessful && response.body()?.retCode == 0) {
                val instruments = response.body()?.result?.list ?: emptyList()
                // Sadece USDT çiftlerini filtrele ve aktif olanları al
                val usdtPairs = instruments.filter {
                    it.quoteCoin == "USDT" && it.status == "Trading"
                }
                logRepository.logDebug("API", "İşlem çiftleri alındı: ${usdtPairs.size} USDT çifti")
                Result.success(usdtPairs)
            } else {
                val errorMsg = response.body()?.retMsg ?: response.message()
                logRepository.logError("API", "İşlem çiftleri alınamadı: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logRepository.logError("API", "İşlem çiftleri hatası: ${e.message}", e.stackTraceToString())
            Result.failure(e)
        }
    }

    /**
     * Anlık fiyatları al (REST API)
     */
    suspend fun getTickers(): Result<Map<String, TickerInfo>> {
        return try {
            val response = apiService.getTickers()
            if (response.isSuccessful && response.body()?.retCode == 0) {
                val tickers = response.body()?.result?.list?.associateBy { it.symbol } ?: emptyMap()
                Result.success(tickers)
            } else {
                val errorMsg = response.body()?.retMsg ?: response.message()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Belirli bir sembol için fiyat al
     */
    suspend fun getTickerPrice(symbol: String): Result<String> {
        // Önce WebSocket cache'e bak
        val cached = webSocket.getCachedPrice(symbol)
        if (cached != null) {
            return Result.success(cached.lastPrice)
        }

        // Cache'de yoksa REST API'den al
        return try {
            val response = apiService.getTickerBySymbol(symbol = symbol)
            if (response.isSuccessful && response.body()?.retCode == 0) {
                val price = response.body()?.result?.list?.firstOrNull()?.lastPrice ?: "0"
                Result.success(price)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Portföy anlık durumunu hesapla
     */
    suspend fun calculatePortfolioSnapshot(): Result<PortfolioSnapshot> {
        return try {
            val balancesResult = getWalletBalances()
            if (balancesResult.isFailure) {
                return Result.failure(balancesResult.exceptionOrNull()!!)
            }

            val balances = balancesResult.getOrNull()!!
            val portfolioCoins = getEnabledCoinsOnce()

            // Toplam USDT değerini hesapla
            var totalValueUsdt = 0.0
            val coinSnapshots = mutableListOf<CoinSnapshot>()

            for (portfolioCoin in portfolioCoins) {
                val balance = balances.find { it.coin == portfolioCoin.coin }
                if (balance != null) {
                    val usdValue = balance.usdValue.toDoubleOrNull() ?: 0.0
                    totalValueUsdt += usdValue

                    coinSnapshots.add(
                        CoinSnapshot(
                            coin = portfolioCoin.coin,
                            balance = balance.walletBalance.toDoubleOrNull() ?: 0.0,
                            usdtValue = usdValue,
                            currentPercentage = 0.0, // Sonra hesaplanacak
                            targetPercentage = portfolioCoin.targetPercentage,
                            deviation = 0.0
                        )
                    )
                }
            }

            // Yüzdeleri hesapla
            val finalSnapshots = coinSnapshots.map { snapshot ->
                val currentPercentage = if (totalValueUsdt > 0) {
                    (snapshot.usdtValue / totalValueUsdt) * 100
                } else 0.0

                snapshot.copy(
                    currentPercentage = currentPercentage,
                    deviation = currentPercentage - snapshot.targetPercentage
                )
            }

            Result.success(
                PortfolioSnapshot(
                    totalValueUsdt = totalValueUsdt,
                    coins = finalSnapshots
                )
            )
        } catch (e: Exception) {
            logRepository.logError("Portfolio", "Portföy hesaplama hatası: ${e.message}", e.stackTraceToString())
            Result.failure(e)
        }
    }

    /**
     * Portföy snapshot'ını JSON'a çevir
     */
    fun snapshotToJson(snapshot: PortfolioSnapshot): String {
        return gson.toJson(snapshot)
    }
}
