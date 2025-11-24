package com.bybit.rebalancer.api

import android.util.Log
import com.bybit.rebalancer.data.model.ConnectionStatus
import com.bybit.rebalancer.data.model.TickerData
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bybit WebSocket Client
 * Spot market için anlık fiyat takibi yapar
 * Public WebSocket: wss://stream.bybit.com/v5/public/spot
 */
@Singleton
class BybitWebSocket @Inject constructor() {

    companion object {
        private const val TAG = "BybitWebSocket"
        private const val MAINNET_WS_URL = "wss://stream.bybit.com/v5/public/spot"
        private const val TESTNET_WS_URL = "wss://stream-testnet.bybit.com/v5/public/spot"
        private const val PING_INTERVAL = 20000L // 20 saniye
        private const val RECONNECT_DELAY = 5000L // 5 saniye
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    private var webSocket: WebSocket? = null
    private var isTestnet = false
    private var reconnectAttempts = 0
    private var shouldReconnect = true

    // Abone olunan semboller
    private val subscribedSymbols = ConcurrentHashMap<String, Boolean>()

    // Son fiyatlar cache
    private val priceCache = ConcurrentHashMap<String, TickerData>()

    // State flows
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickerUpdates = MutableSharedFlow<TickerData>(extraBufferCapacity = 100)
    val tickerUpdates: SharedFlow<TickerData> = _tickerUpdates.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .pingInterval(PING_INTERVAL, TimeUnit.MILLISECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * WebSocket bağlantısını başlat
     */
    fun connect(testnet: Boolean = false) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED ||
            _connectionStatus.value == ConnectionStatus.CONNECTING) {
            Log.d(TAG, "Already connected or connecting")
            return
        }

        isTestnet = testnet
        shouldReconnect = true
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val url = if (testnet) TESTNET_WS_URL else MAINNET_WS_URL
        val request = Request.Builder()
            .url(url)
            .build()

        Log.d(TAG, "Connecting to: $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionStatus.value = ConnectionStatus.CONNECTED
                reconnectAttempts = 0

                // Daha önce abone olunan sembollere tekrar abone ol
                resubscribeAll()

                // Ping loop başlat
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                attemptReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                _connectionStatus.value = ConnectionStatus.ERROR
                scope.launch {
                    _errors.emit("WebSocket error: ${t.message}")
                }
                attemptReconnect()
            }
        })
    }

    /**
     * Bağlantıyı kapat
     */
    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        subscribedSymbols.clear()
        priceCache.clear()
    }

    /**
     * Ticker stream'e abone ol
     */
    fun subscribeTicker(symbol: String) {
        val topic = "tickers.$symbol"
        if (subscribedSymbols.containsKey(topic)) {
            Log.d(TAG, "Already subscribed to $topic")
            return
        }

        subscribedSymbols[topic] = true

        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            sendSubscribe(listOf(topic))
        }
    }

    /**
     * Birden fazla sembole abone ol
     */
    fun subscribeMultipleTickers(symbols: List<String>) {
        val topics = symbols.map { "tickers.$it" }
            .filter { !subscribedSymbols.containsKey(it) }

        topics.forEach { subscribedSymbols[it] = true }

        if (_connectionStatus.value == ConnectionStatus.CONNECTED && topics.isNotEmpty()) {
            sendSubscribe(topics)
        }
    }

    /**
     * Ticker aboneliğini iptal et
     */
    fun unsubscribeTicker(symbol: String) {
        val topic = "tickers.$symbol"
        subscribedSymbols.remove(topic)
        priceCache.remove(symbol)

        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            sendUnsubscribe(listOf(topic))
        }
    }

    /**
     * Cached fiyatı al
     */
    fun getCachedPrice(symbol: String): TickerData? {
        return priceCache[symbol]
    }

    /**
     * Tüm cached fiyatları al
     */
    fun getAllCachedPrices(): Map<String, TickerData> {
        return priceCache.toMap()
    }

    private fun sendSubscribe(topics: List<String>) {
        val message = mapOf(
            "op" to "subscribe",
            "args" to topics
        )
        val json = gson.toJson(message)
        Log.d(TAG, "Subscribing: $json")
        webSocket?.send(json)
    }

    private fun sendUnsubscribe(topics: List<String>) {
        val message = mapOf(
            "op" to "unsubscribe",
            "args" to topics
        )
        val json = gson.toJson(message)
        webSocket?.send(json)
    }

    private fun resubscribeAll() {
        val topics = subscribedSymbols.keys.toList()
        if (topics.isNotEmpty()) {
            Log.d(TAG, "Resubscribing to ${topics.size} topics")
            sendSubscribe(topics)
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)

            // Ping/Pong response
            if (json.has("op") && json.get("op").asString == "pong") {
                return
            }

            // Subscription response
            if (json.has("success")) {
                val success = json.get("success").asBoolean
                Log.d(TAG, "Subscription response: success=$success")
                return
            }

            // Ticker data
            if (json.has("topic") && json.has("data")) {
                val topic = json.get("topic").asString
                if (topic.startsWith("tickers.")) {
                    val dataObj = json.getAsJsonObject("data")
                    val tickerData = TickerData(
                        symbol = dataObj.get("symbol").asString,
                        lastPrice = dataObj.get("lastPrice").asString,
                        price24hPcnt = dataObj.get("price24hPcnt")?.asString ?: "0",
                        highPrice24h = dataObj.get("highPrice24h")?.asString ?: "0",
                        lowPrice24h = dataObj.get("lowPrice24h")?.asString ?: "0",
                        volume24h = dataObj.get("volume24h")?.asString ?: "0",
                        turnover24h = dataObj.get("turnover24h")?.asString ?: "0"
                    )

                    priceCache[tickerData.symbol] = tickerData

                    scope.launch {
                        _tickerUpdates.emit(tickerData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
        }
    }

    private fun startPingLoop() {
        scope.launch {
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(PING_INTERVAL)
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    val pingMessage = """{"op":"ping"}"""
                    webSocket?.send(pingMessage)
                }
            }
        }
    }

    private fun attemptReconnect() {
        if (!shouldReconnect) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached")
            scope.launch {
                _errors.emit("Bağlantı kurulamadı. Maksimum deneme sayısına ulaşıldı.")
            }
            return
        }

        reconnectAttempts++
        Log.d(TAG, "Attempting reconnect ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

        scope.launch {
            delay(RECONNECT_DELAY * reconnectAttempts)
            if (shouldReconnect && _connectionStatus.value != ConnectionStatus.CONNECTED) {
                connect(isTestnet)
            }
        }
    }
}
