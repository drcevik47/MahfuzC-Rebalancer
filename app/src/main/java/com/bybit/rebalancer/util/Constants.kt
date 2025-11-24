package com.bybit.rebalancer.util

object Constants {
    // API URLs
    const val MAINNET_BASE_URL = "https://api.bybit.com"
    const val TESTNET_BASE_URL = "https://api-testnet.bybit.com"
    const val MAINNET_WS_URL = "wss://stream.bybit.com/v5/public/spot"
    const val TESTNET_WS_URL = "wss://stream-testnet.bybit.com/v5/public/spot"

    // API Headers
    const val API_KEY_HEADER = "X-BAPI-API-KEY"
    const val TIMESTAMP_HEADER = "X-BAPI-TIMESTAMP"
    const val SIGN_HEADER = "X-BAPI-SIGN"
    const val RECV_WINDOW_HEADER = "X-BAPI-RECV-WINDOW"
    const val DEFAULT_RECV_WINDOW = "5000"

    // Database
    const val DATABASE_NAME = "bybit_rebalancer.db"

    // Service
    const val NOTIFICATION_CHANNEL_ID = "rebalancer_service_channel"
    const val NOTIFICATION_ID = 1001
    const val SERVICE_RESTART_DELAY = 5000L

    // WebSocket
    const val WS_PING_INTERVAL = 20000L
    const val WS_RECONNECT_DELAY = 5000L
    const val WS_MAX_RECONNECT_ATTEMPTS = 10

    // Defaults
    const val DEFAULT_REBALANCE_THRESHOLD = 1.0 // %1
    const val DEFAULT_MIN_TRADE_USDT = 10.0
    const val DEFAULT_CHECK_INTERVAL_SECONDS = 30

    // Limits
    const val MIN_THRESHOLD = 0.01
    const val MAX_THRESHOLD = 100.0
    const val MIN_TRADE_AMOUNT = 1.0
    const val MAX_PORTFOLIO_COINS = 50
}

object LogTags {
    const val API = "API"
    const val WEBSOCKET = "WebSocket"
    const val SERVICE = "Service"
    const val REBALANCE = "Rebalance"
    const val TRADE = "Trade"
    const val PORTFOLIO = "Portfolio"
    const val SETTINGS = "Settings"
    const val DATABASE = "Database"
    const val SYSTEM = "System"
}

object TradeStatus {
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
}

object OrderSide {
    const val BUY = "Buy"
    const val SELL = "Sell"
}

object OrderType {
    const val MARKET = "Market"
    const val LIMIT = "Limit"
}

object OrderStatus {
    const val NEW = "New"
    const val PARTIALLY_FILLED = "PartiallyFilled"
    const val FILLED = "Filled"
    const val CANCELLED = "Cancelled"
    const val REJECTED = "Rejected"
}
