package com.mahfuz.rebalancer.data.api

import com.mahfuz.rebalancer.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface BybitApiService {

    companion object {
        const val BASE_URL_MAINNET = "https://api.bybit.com"
        const val BASE_URL_TESTNET = "https://api-testnet.bybit.com"

        const val WS_URL_MAINNET_SPOT = "wss://stream.bybit.com/v5/public/spot"
        const val WS_URL_TESTNET_SPOT = "wss://stream-testnet.bybit.com/v5/public/spot"
    }

    // Market Data - Public
    @GET("/v5/market/tickers")
    suspend fun getTickers(
        @Query("category") category: String = "spot",
        @Query("symbol") symbol: String? = null
    ): Response<BybitResponse<TickerResult>>

    @GET("/v5/market/instruments-info")
    suspend fun getInstrumentsInfo(
        @Query("category") category: String = "spot",
        @Query("symbol") symbol: String? = null,
        @Query("limit") limit: Int = 1000,
        @Query("cursor") cursor: String? = null
    ): Response<BybitResponse<InstrumentsInfoResult>>

    @GET("/v5/market/time")
    suspend fun getServerTime(): Response<BybitResponse<ServerTimeResult>>

    // Account - Private (requires authentication)
    @GET("/v5/account/wallet-balance")
    suspend fun getWalletBalance(
        @Query("accountType") accountType: String = "UNIFIED",
        @Query("coin") coin: String? = null,
        @Header("X-BAPI-API-KEY") apiKey: String,
        @Header("X-BAPI-TIMESTAMP") timestamp: String,
        @Header("X-BAPI-SIGN") sign: String,
        @Header("X-BAPI-RECV-WINDOW") recvWindow: String = "5000"
    ): Response<BybitResponse<WalletBalanceResult>>

    // Order - Private (requires authentication)
    @POST("/v5/order/create")
    suspend fun createOrder(
        @Body order: OrderRequest,
        @Header("X-BAPI-API-KEY") apiKey: String,
        @Header("X-BAPI-TIMESTAMP") timestamp: String,
        @Header("X-BAPI-SIGN") sign: String,
        @Header("X-BAPI-RECV-WINDOW") recvWindow: String = "5000"
    ): Response<BybitResponse<OrderResult>>

    // Order History - Private
    @GET("/v5/order/history")
    suspend fun getOrderHistory(
        @Query("category") category: String = "spot",
        @Query("symbol") symbol: String? = null,
        @Query("orderId") orderId: String? = null,
        @Query("limit") limit: Int = 50,
        @Header("X-BAPI-API-KEY") apiKey: String,
        @Header("X-BAPI-TIMESTAMP") timestamp: String,
        @Header("X-BAPI-SIGN") sign: String,
        @Header("X-BAPI-RECV-WINDOW") recvWindow: String = "5000"
    ): Response<BybitResponse<OrderHistoryResult>>
}
