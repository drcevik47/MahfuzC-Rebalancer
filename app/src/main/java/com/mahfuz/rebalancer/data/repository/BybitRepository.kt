package com.mahfuz.rebalancer.data.repository

import com.google.gson.Gson
import com.mahfuz.rebalancer.data.api.BybitApiService
import com.mahfuz.rebalancer.data.api.BybitSignatureHelper
import com.mahfuz.rebalancer.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BybitRepository @Inject constructor(
    private val apiService: BybitApiService,
    private val gson: Gson
) {
    private var apiKey: String = ""
    private var apiSecret: String = ""

    fun setCredentials(apiKey: String, apiSecret: String) {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
    }

    fun hasCredentials(): Boolean = apiKey.isNotBlank() && apiSecret.isNotBlank()

    // Public endpoints - no authentication needed
    suspend fun getServerTime(): Result<ServerTimeResult> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getServerTime()
            if (response.isSuccessful && response.body()?.retCode == 0) {
                Result.success(response.body()!!.result!!)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTickers(symbol: String? = null): Result<List<TickerInfo>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTickers(symbol = symbol)
            if (response.isSuccessful && response.body()?.retCode == 0) {
                Result.success(response.body()!!.result!!.list)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInstrumentsInfo(): Result<List<InstrumentInfo>> = withContext(Dispatchers.IO) {
        try {
            val allInstruments = mutableListOf<InstrumentInfo>()
            var cursor: String? = null

            do {
                val response = apiService.getInstrumentsInfo(cursor = cursor)
                if (response.isSuccessful && response.body()?.retCode == 0) {
                    val result = response.body()!!.result!!
                    allInstruments.addAll(result.list)
                    cursor = result.nextPageCursor?.takeIf { it.isNotBlank() }
                } else {
                    return@withContext Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
                }
            } while (cursor != null)

            // Filter for USDT trading pairs that are active
            val usdtPairs = allInstruments.filter {
                it.quoteCoin == "USDT" && it.status == "Trading"
            }

            Result.success(usdtPairs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Private endpoints - require authentication
    suspend fun getWalletBalance(coin: String? = null): Result<List<CoinBalance>> = withContext(Dispatchers.IO) {
        if (!hasCredentials()) {
            return@withContext Result.failure(Exception("API credentials not set"))
        }

        try {
            val queryParams = mutableMapOf<String, String?>("accountType" to "UNIFIED")
            if (coin != null) {
                queryParams["coin"] = coin
            }

            val signed = BybitSignatureHelper.signGetRequest(
                apiKey = apiKey,
                apiSecret = apiSecret,
                queryParams = queryParams
            )

            val response = apiService.getWalletBalance(
                coin = coin,
                apiKey = apiKey,
                timestamp = signed.timestamp,
                sign = signed.signature,
                recvWindow = signed.recvWindow
            )

            if (response.isSuccessful && response.body()?.retCode == 0) {
                val coins = response.body()!!.result!!.list
                    .flatMap { it.coin }
                    .filter { it.walletBalance.toDoubleOrNull() ?: 0.0 > 0 }
                Result.success(coins)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        symbol: String,
        side: String,
        qty: String,
        marketUnit: String? = null
    ): Result<OrderResult> = withContext(Dispatchers.IO) {
        if (!hasCredentials()) {
            return@withContext Result.failure(Exception("API credentials not set"))
        }

        try {
            val orderRequest = OrderRequest(
                category = "spot",
                symbol = symbol,
                side = side,
                orderType = "Market",
                qty = qty,
                marketUnit = marketUnit
            )

            val jsonBody = gson.toJson(orderRequest)
            val signed = BybitSignatureHelper.signPostRequest(
                apiKey = apiKey,
                apiSecret = apiSecret,
                jsonBody = jsonBody
            )

            val response = apiService.createOrder(
                order = orderRequest,
                apiKey = apiKey,
                timestamp = signed.timestamp,
                sign = signed.signature,
                recvWindow = signed.recvWindow
            )

            if (response.isSuccessful && response.body()?.retCode == 0) {
                Result.success(response.body()!!.result!!)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderHistory(
        symbol: String? = null,
        orderId: String? = null
    ): Result<List<OrderHistoryItem>> = withContext(Dispatchers.IO) {
        if (!hasCredentials()) {
            return@withContext Result.failure(Exception("API credentials not set"))
        }

        try {
            val queryParams = mutableMapOf<String, String?>("category" to "spot")
            symbol?.let { queryParams["symbol"] = it }
            orderId?.let { queryParams["orderId"] = it }

            val signed = BybitSignatureHelper.signGetRequest(
                apiKey = apiKey,
                apiSecret = apiSecret,
                queryParams = queryParams
            )

            val response = apiService.getOrderHistory(
                symbol = symbol,
                orderId = orderId,
                apiKey = apiKey,
                timestamp = signed.timestamp,
                sign = signed.signature,
                recvWindow = signed.recvWindow
            )

            if (response.isSuccessful && response.body()?.retCode == 0) {
                Result.success(response.body()!!.result!!.list)
            } else {
                Result.failure(Exception(response.body()?.retMsg ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get available coins for trading
    suspend fun getAvailableCoins(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val instrumentsResult = getInstrumentsInfo()
            if (instrumentsResult.isFailure) {
                return@withContext Result.failure(instrumentsResult.exceptionOrNull()!!)
            }

            val coins = instrumentsResult.getOrNull()!!
                .map { it.baseCoin }
                .distinct()
                .sorted()

            Result.success(coins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
