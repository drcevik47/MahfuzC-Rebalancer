package com.bybit.rebalancer.api

import com.bybit.rebalancer.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Bybit API v5 Interface
 * Dokümantasyon: https://bybit-exchange.github.io/docs/v5/intro
 */
interface BybitApiService {

    /**
     * Cüzdan bakiyelerini al
     * GET /v5/account/wallet-balance
     */
    @GET("/v5/account/wallet-balance")
    suspend fun getWalletBalance(
        @Query("accountType") accountType: String = "UNIFIED"
    ): Response<BybitResponse<WalletBalanceResult>>

    /**
     * Tüm ticker bilgilerini al
     * GET /v5/market/tickers
     */
    @GET("/v5/market/tickers")
    suspend fun getTickers(
        @Query("category") category: String = "spot"
    ): Response<BybitResponse<TickersResult>>

    /**
     * Belirli bir sembol için ticker bilgisi al
     * GET /v5/market/tickers
     */
    @GET("/v5/market/tickers")
    suspend fun getTickerBySymbol(
        @Query("category") category: String = "spot",
        @Query("symbol") symbol: String
    ): Response<BybitResponse<TickersResult>>

    /**
     * Instruments (işlem çiftleri) bilgisini al
     * GET /v5/market/instruments-info
     */
    @GET("/v5/market/instruments-info")
    suspend fun getInstrumentsInfo(
        @Query("category") category: String = "spot",
        @Query("limit") limit: Int = 1000
    ): Response<BybitResponse<InstrumentsResult>>

    /**
     * Spot market order oluştur
     * POST /v5/order/create
     */
    @POST("/v5/order/create")
    suspend fun createOrder(
        @Body orderRequest: OrderRequest
    ): Response<BybitResponse<OrderResult>>

    /**
     * Order durumunu kontrol et
     * GET /v5/order/realtime
     */
    @GET("/v5/order/realtime")
    suspend fun getOrderStatus(
        @Query("category") category: String = "spot",
        @Query("orderId") orderId: String
    ): Response<BybitResponse<OrderStatusResult>>
}

/**
 * Order Request Body
 */
data class OrderRequest(
    val category: String = "spot",
    val symbol: String,
    val side: String,           // Buy, Sell
    val orderType: String,      // Market, Limit
    val qty: String,
    val marketUnit: String? = null,  // baseCoin veya quoteCoin (market order için)
    val price: String? = null,  // Limit order için
    val timeInForce: String? = null,
    val orderLinkId: String? = null
)

/**
 * Order Status Result
 */
data class OrderStatusResult(
    val list: List<OrderInfo>
)

data class OrderInfo(
    val orderId: String,
    val orderLinkId: String,
    val symbol: String,
    val side: String,
    val orderType: String,
    val price: String,
    val qty: String,
    val cumExecQty: String,     // Gerçekleşen miktar
    val cumExecValue: String,   // Gerçekleşen değer
    val avgPrice: String,       // Ortalama fiyat
    val orderStatus: String,    // New, PartiallyFilled, Filled, Cancelled, Rejected
    val createdTime: String,
    val updatedTime: String
)
