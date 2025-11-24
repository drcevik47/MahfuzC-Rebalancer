package com.mahfuz.rebalancer.data.api

import com.google.gson.Gson
import com.mahfuz.rebalancer.data.model.WebSocketMessage
import com.mahfuz.rebalancer.data.model.WebSocketTickerResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class BybitWebSocket(
    private val isTestnet: Boolean = false,
    private val onLog: (String, String) -> Unit = { _, _ -> }
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tickerUpdates = MutableSharedFlow<WebSocketTickerResponse>(replay = 1)
    val tickerUpdates: SharedFlow<WebSocketTickerResponse> = _tickerUpdates.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val subscribedSymbols = mutableSetOf<String>()
    private val pendingSubscriptions = Channel<List<String>>(Channel.BUFFERED)

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        ERROR
    }

    private val wsUrl: String
        get() = if (isTestnet) {
            BybitApiService.WS_URL_TESTNET_SPOT
        } else {
            BybitApiService.WS_URL_MAINNET_SPOT
        }

    fun connect() {
        if (isConnected) return

        _connectionState.value = ConnectionState.CONNECTING
        onLog("WEBSOCKET", "Connecting to $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
        _connectionState.value = ConnectionState.DISCONNECTED
        subscribedSymbols.clear()
        onLog("WEBSOCKET", "Disconnected")
    }

    fun subscribeToTickers(symbols: List<String>) {
        val newSymbols = symbols.filter { it !in subscribedSymbols }
        if (newSymbols.isEmpty()) return

        if (isConnected) {
            sendSubscription(newSymbols)
        } else {
            scope.launch {
                pendingSubscriptions.send(newSymbols)
            }
        }
    }

    fun unsubscribeFromTickers(symbols: List<String>) {
        if (!isConnected) return

        val args = symbols.map { "tickers.$it" }
        val message = WebSocketMessage(
            op = "unsubscribe",
            args = args
        )

        webSocket?.send(gson.toJson(message))
        subscribedSymbols.removeAll(symbols.toSet())
        onLog("WEBSOCKET", "Unsubscribed from: ${symbols.joinToString()}")
    }

    private fun sendSubscription(symbols: List<String>) {
        val args = symbols.map { "tickers.$it" }
        val message = WebSocketMessage(
            op = "subscribe",
            args = args
        )

        val json = gson.toJson(message)
        webSocket?.send(json)
        subscribedSymbols.addAll(symbols)
        onLog("WEBSOCKET", "Subscribed to: ${symbols.joinToString()}")
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            isConnected = true
            _connectionState.value = ConnectionState.CONNECTED
            onLog("WEBSOCKET", "Connected successfully")

            // Process pending subscriptions
            scope.launch {
                while (true) {
                    val symbols = pendingSubscriptions.tryReceive().getOrNull() ?: break
                    sendSubscription(symbols)
                }
            }

            // Resubscribe to previously subscribed symbols
            if (subscribedSymbols.isNotEmpty()) {
                val symbols = subscribedSymbols.toList()
                subscribedSymbols.clear()
                sendSubscription(symbols)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                // Check if it's a pong response or subscription confirmation
                if (text.contains("\"op\"")) {
                    onLog("WEBSOCKET", "Control message: ${text.take(100)}")
                    return
                }

                // Parse ticker update
                val response = gson.fromJson(text, WebSocketTickerResponse::class.java)
                if (response.topic?.startsWith("tickers.") == true && response.data != null) {
                    scope.launch {
                        _tickerUpdates.emit(response)
                    }
                }
            } catch (e: Exception) {
                onLog("WEBSOCKET_ERROR", "Failed to parse message: ${e.message}")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            isConnected = false
            _connectionState.value = ConnectionState.ERROR
            onLog("WEBSOCKET_ERROR", "Connection failed: ${t.message}")

            scheduleReconnect()
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            onLog("WEBSOCKET", "Closing: $code - $reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            isConnected = false
            _connectionState.value = ConnectionState.DISCONNECTED
            onLog("WEBSOCKET", "Closed: $code - $reason")

            if (code != 1000) {
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            _connectionState.value = ConnectionState.RECONNECTING
            delay(5000) // Wait 5 seconds before reconnecting
            onLog("WEBSOCKET", "Attempting to reconnect...")
            connect()
        }
    }

    fun sendPing() {
        if (isConnected) {
            val pingMessage = """{"op":"ping"}"""
            webSocket?.send(pingMessage)
        }
    }

    fun cleanup() {
        scope.cancel()
        disconnect()
        client.dispatcher.executorService.shutdown()
    }
}
