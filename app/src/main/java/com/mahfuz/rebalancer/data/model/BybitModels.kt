package com.mahfuz.rebalancer.data.model

import com.google.gson.annotations.SerializedName

// Base API Response
data class BybitResponse<T>(
    @SerializedName("retCode") val retCode: Int,
    @SerializedName("retMsg") val retMsg: String,
    @SerializedName("result") val result: T?,
    @SerializedName("time") val time: Long
)

// Wallet Balance Response
data class WalletBalanceResult(
    @SerializedName("list") val list: List<AccountInfo>
)

data class AccountInfo(
    @SerializedName("accountType") val accountType: String,
    @SerializedName("accountIMRate") val accountIMRate: String?,
    @SerializedName("accountMMRate") val accountMMRate: String?,
    @SerializedName("totalEquity") val totalEquity: String,
    @SerializedName("totalWalletBalance") val totalWalletBalance: String,
    @SerializedName("totalAvailableBalance") val totalAvailableBalance: String,
    @SerializedName("coin") val coin: List<CoinBalance>
)

data class CoinBalance(
    @SerializedName("coin") val coin: String,
    @SerializedName("equity") val equity: String,
    @SerializedName("usdValue") val usdValue: String,
    @SerializedName("walletBalance") val walletBalance: String,
    @SerializedName("availableToWithdraw") val availableToWithdraw: String,
    @SerializedName("availableToBorrow") val availableToBorrow: String?,
    @SerializedName("borrowAmount") val borrowAmount: String?,
    @SerializedName("locked") val locked: String?,
    @SerializedName("spotHedgingQty") val spotHedgingQty: String?,
    @SerializedName("free") val free: String?
)

// Instruments Info Response (for getting tradeable pairs)
data class InstrumentsInfoResult(
    @SerializedName("category") val category: String,
    @SerializedName("list") val list: List<InstrumentInfo>,
    @SerializedName("nextPageCursor") val nextPageCursor: String?
)

data class InstrumentInfo(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("baseCoin") val baseCoin: String,
    @SerializedName("quoteCoin") val quoteCoin: String,
    @SerializedName("status") val status: String,
    @SerializedName("lotSizeFilter") val lotSizeFilter: LotSizeFilter?,
    @SerializedName("priceFilter") val priceFilter: PriceFilter?
)

data class LotSizeFilter(
    @SerializedName("basePrecision") val basePrecision: String?,
    @SerializedName("quotePrecision") val quotePrecision: String?,
    @SerializedName("minOrderQty") val minOrderQty: String?,
    @SerializedName("maxOrderQty") val maxOrderQty: String?,
    @SerializedName("minOrderAmt") val minOrderAmt: String?,
    @SerializedName("maxOrderAmt") val maxOrderAmt: String?
)

data class PriceFilter(
    @SerializedName("tickSize") val tickSize: String?
)

// Ticker Response
data class TickerResult(
    @SerializedName("category") val category: String,
    @SerializedName("list") val list: List<TickerInfo>
)

data class TickerInfo(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("lastPrice") val lastPrice: String,
    @SerializedName("bid1Price") val bid1Price: String?,
    @SerializedName("ask1Price") val ask1Price: String?,
    @SerializedName("bid1Size") val bid1Size: String?,
    @SerializedName("ask1Size") val ask1Size: String?,
    @SerializedName("volume24h") val volume24h: String?,
    @SerializedName("turnover24h") val turnover24h: String?,
    @SerializedName("highPrice24h") val highPrice24h: String?,
    @SerializedName("lowPrice24h") val lowPrice24h: String?,
    @SerializedName("prevPrice24h") val prevPrice24h: String?,
    @SerializedName("price24hPcnt") val price24hPcnt: String?
)

// Order Request/Response
data class OrderRequest(
    @SerializedName("category") val category: String = "spot",
    @SerializedName("symbol") val symbol: String,
    @SerializedName("side") val side: String, // "Buy" or "Sell"
    @SerializedName("orderType") val orderType: String = "Market",
    @SerializedName("qty") val qty: String,
    @SerializedName("marketUnit") val marketUnit: String? = null, // "baseCoin" or "quoteCoin"
    @SerializedName("timeInForce") val timeInForce: String? = null,
    @SerializedName("orderLinkId") val orderLinkId: String? = null
)

data class OrderResult(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderLinkId") val orderLinkId: String?
)

// Order History
data class OrderHistoryResult(
    @SerializedName("category") val category: String,
    @SerializedName("list") val list: List<OrderHistoryItem>,
    @SerializedName("nextPageCursor") val nextPageCursor: String?
)

data class OrderHistoryItem(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("orderLinkId") val orderLinkId: String?,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("side") val side: String,
    @SerializedName("orderType") val orderType: String,
    @SerializedName("price") val price: String,
    @SerializedName("qty") val qty: String,
    @SerializedName("cumExecQty") val cumExecQty: String,
    @SerializedName("cumExecValue") val cumExecValue: String,
    @SerializedName("cumExecFee") val cumExecFee: String,
    @SerializedName("avgPrice") val avgPrice: String,
    @SerializedName("orderStatus") val orderStatus: String,
    @SerializedName("createdTime") val createdTime: String,
    @SerializedName("updatedTime") val updatedTime: String
)

// Server Time
data class ServerTimeResult(
    @SerializedName("timeSecond") val timeSecond: String,
    @SerializedName("timeNano") val timeNano: String
)

// WebSocket Models
data class WebSocketMessage(
    @SerializedName("op") val op: String? = null,
    @SerializedName("args") val args: List<String>? = null,
    @SerializedName("req_id") val reqId: String? = null
)

data class WebSocketTickerResponse(
    @SerializedName("topic") val topic: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("ts") val ts: Long?,
    @SerializedName("cs") val cs: Long?,
    @SerializedName("data") val data: WebSocketTickerData?
)

data class WebSocketTickerData(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("lastPrice") val lastPrice: String,
    @SerializedName("bid1Price") val bid1Price: String?,
    @SerializedName("ask1Price") val ask1Price: String?,
    @SerializedName("highPrice24h") val highPrice24h: String?,
    @SerializedName("lowPrice24h") val lowPrice24h: String?,
    @SerializedName("volume24h") val volume24h: String?,
    @SerializedName("turnover24h") val turnover24h: String?
)
