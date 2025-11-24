package com.mahfuz.rebalancer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.mahfuz.rebalancer.R
import com.mahfuz.rebalancer.data.api.BybitWebSocket
import com.mahfuz.rebalancer.data.db.RebalanceEventEntity
import com.mahfuz.rebalancer.data.db.TradeHistoryEntity
import com.mahfuz.rebalancer.data.model.*
import com.mahfuz.rebalancer.data.repository.BybitRepository
import com.mahfuz.rebalancer.data.repository.PortfolioRepository
import com.mahfuz.rebalancer.ui.MainActivity
import com.mahfuz.rebalancer.util.AppLogger
import com.mahfuz.rebalancer.util.RebalanceCalculator
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class RebalancerService : Service() {

    @Inject lateinit var bybitRepository: BybitRepository
    @Inject lateinit var portfolioRepository: PortfolioRepository
    @Inject lateinit var secureStorage: SecureStorage
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var gson: Gson

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: BybitWebSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitoringJob: Job? = null
    private var pingJob: Job? = null

    private val rebalanceCalculator = RebalanceCalculator()
    private val priceCache = mutableMapOf<String, BigDecimal>()
    private var instrumentsCache = mapOf<String, InstrumentInfo>()
    private var lastRebalanceTime = 0L
    private val minRebalanceInterval = 60_000L // 1 minute minimum between rebalances

    private val _state = MutableStateFlow(RebalancerState())
    val state: StateFlow<RebalancerState> = _state.asStateFlow()

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "rebalancer_channel"
        const val ACTION_START = "com.mahfuz.rebalancer.START"
        const val ACTION_STOP = "com.mahfuz.rebalancer.STOP"
        const val ACTION_RESTART = "com.mahfuz.rebalancer.RESTART"

        private var instance: RebalancerService? = null
        fun getInstance(): RebalancerService? = instance

        fun start(context: Context) {
            val intent = Intent(context, RebalancerService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RebalancerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        acquireWakeLock()
        logger.info(AppLogger.TAG_SERVICE, "Rebalancer service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRebalancing()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_RESTART, null -> {
                startForeground(NOTIFICATION_ID, createNotification("Başlatılıyor..."))
                startRebalancing()
            }
        }

        // Return STICKY to ensure service restarts if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        stopRebalancing()
        releaseWakeLock()
        scope.cancel()
        logger.info(AppLogger.TAG_SERVICE, "Rebalancer service destroyed")

        // Schedule restart if auto-start is enabled
        if (secureStorage.isAutoStartEnabled()) {
            scheduleRestart()
        }

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App was swiped from recents, schedule restart
        if (secureStorage.isAutoStartEnabled()) {
            logger.info(AppLogger.TAG_SERVICE, "Task removed, scheduling restart")
            scheduleRestart()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(this, RestartReceiver::class.java).apply {
            action = "com.mahfuz.rebalancer.RESTART_SERVICE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pendingIntent
        )
    }

    private fun startRebalancing() {
        scope.launch {
            try {
                _state.value = _state.value.copy(serviceState = ServiceState.STARTING)
                logger.info(AppLogger.TAG_SERVICE, "Starting rebalancing service")

                // Load API credentials
                val settings = secureStorage.getSettings()
                if (!settings.apiKey.isNotBlank() || !settings.apiSecret.isNotBlank()) {
                    _state.value = _state.value.copy(
                        serviceState = ServiceState.ERROR,
                        errorMessage = "API bilgileri ayarlanmamış"
                    )
                    updateNotification("API bilgileri gerekli")
                    logger.error(AppLogger.TAG_SERVICE, "API credentials not configured")
                    return@launch
                }

                bybitRepository.setCredentials(settings.apiKey, settings.apiSecret)

                // Load instruments info
                loadInstruments()

                // Initialize WebSocket
                initWebSocket(settings.isTestnet)

                // Start monitoring
                startMonitoring()

                _state.value = _state.value.copy(
                    serviceState = ServiceState.RUNNING,
                    isConnected = true
                )
                updateNotification("Çalışıyor - Portföy izleniyor")
                logger.info(AppLogger.TAG_SERVICE, "Rebalancing service started successfully")

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    serviceState = ServiceState.ERROR,
                    errorMessage = e.message
                )
                updateNotification("Hata: ${e.message}")
                logger.error(AppLogger.TAG_SERVICE, "Failed to start rebalancing", e)
            }
        }
    }

    private fun stopRebalancing() {
        monitoringJob?.cancel()
        pingJob?.cancel()
        webSocket?.cleanup()
        webSocket = null
        _state.value = _state.value.copy(serviceState = ServiceState.STOPPED, isConnected = false)
        logger.info(AppLogger.TAG_SERVICE, "Rebalancing stopped")
    }

    private suspend fun loadInstruments() {
        val result = bybitRepository.getInstrumentsInfo()
        if (result.isSuccess) {
            instrumentsCache = result.getOrNull()!!.associateBy { it.baseCoin }
            logger.info(AppLogger.TAG_SERVICE, "Loaded ${instrumentsCache.size} trading pairs")
        } else {
            logger.error(AppLogger.TAG_SERVICE, "Failed to load instruments: ${result.exceptionOrNull()?.message}")
        }
    }

    private fun initWebSocket(isTestnet: Boolean) {
        webSocket = BybitWebSocket(isTestnet) { tag, message ->
            logger.debug(tag, message)
        }

        // Subscribe to price updates
        scope.launch {
            webSocket?.tickerUpdates?.collect { response ->
                response.data?.let { data ->
                    val coin = data.symbol.removeSuffix("USDT")
                    data.lastPrice.toBigDecimalOrNull()?.let { price ->
                        priceCache[coin] = price
                    }
                }
            }
        }

        // Subscribe to connection state
        scope.launch {
            webSocket?.connectionState?.collect { state ->
                _state.value = _state.value.copy(
                    isConnected = state == BybitWebSocket.ConnectionState.CONNECTED
                )
                when (state) {
                    BybitWebSocket.ConnectionState.CONNECTED -> {
                        updateNotification("Çalışıyor - Bağlandı")
                        // Subscribe to portfolio coins
                        subscribeToPortfolioCoins()
                    }
                    BybitWebSocket.ConnectionState.DISCONNECTED,
                    BybitWebSocket.ConnectionState.ERROR -> {
                        updateNotification("Bağlantı kesildi - Yeniden bağlanıyor...")
                    }
                    else -> {}
                }
            }
        }

        // Start WebSocket ping
        pingJob = scope.launch {
            while (isActive) {
                delay(20_000) // Ping every 20 seconds
                webSocket?.sendPing()
            }
        }

        webSocket?.connect()
    }

    private suspend fun subscribeToPortfolioCoins() {
        portfolioRepository.getActiveCoins().first().let { coins ->
            val symbols = coins
                .map { "${it.coin}USDT" }
                .filter { it != "USDTUSDT" }
            if (symbols.isNotEmpty()) {
                webSocket?.subscribeToTickers(symbols)
            }
        }
    }

    private fun startMonitoring() {
        monitoringJob = scope.launch {
            // Collect portfolio config changes
            portfolioRepository.getPortfolioConfig().collect { config ->
                if (config.isActive && config.coins.isNotEmpty()) {
                    // Subscribe to new coins
                    val symbols = config.coins.keys
                        .map { "${it}USDT" }
                        .filter { it != "USDTUSDT" }
                    webSocket?.subscribeToTickers(symbols)

                    // Check for rebalancing periodically
                    while (isActive && config.isActive) {
                        checkAndRebalance(config)
                        delay(5000) // Check every 5 seconds
                    }
                }
            }
        }
    }

    private suspend fun checkAndRebalance(config: PortfolioConfig) {
        try {
            // Don't rebalance too frequently
            val now = System.currentTimeMillis()
            if (now - lastRebalanceTime < minRebalanceInterval) {
                return
            }

            // Get current balances
            val balanceResult = bybitRepository.getWalletBalance()
            if (balanceResult.isFailure) {
                logger.error(AppLogger.TAG_REBALANCER, "Failed to get wallet balance: ${balanceResult.exceptionOrNull()?.message}")
                return
            }

            val balances = balanceResult.getOrNull()!!
                .associate { it.coin to (it.walletBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

            // Build price map - use WebSocket cache with REST API fallback
            val prices = priceCache.toMutableMap()

            // Check if we need to fetch prices via REST API (fallback)
            val missingPrices = config.coins.keys.filter { coin ->
                coin != "USDT" && !prices.containsKey(coin)
            }

            if (missingPrices.isNotEmpty() || prices.isEmpty()) {
                logger.info(AppLogger.TAG_REBALANCER, "Fetching prices via REST API for: ${missingPrices.joinToString()}")
                val tickersResult = bybitRepository.getTickers()
                if (tickersResult.isSuccess) {
                    tickersResult.getOrNull()!!
                        .filter { it.symbol.endsWith("USDT") }
                        .forEach { ticker ->
                            val coin = ticker.symbol.removeSuffix("USDT")
                            ticker.lastPrice.toBigDecimalOrNull()?.let { price ->
                                prices[coin] = price
                                // Also update cache for future use
                                priceCache[coin] = price
                            }
                        }
                } else {
                    logger.error(AppLogger.TAG_REBALANCER, "Failed to get tickers: ${tickersResult.exceptionOrNull()?.message}")
                    return
                }
            }

            // Calculate portfolio state
            val portfolioState = rebalanceCalculator.calculatePortfolioState(
                balances = balances,
                prices = prices,
                targetPercentages = config.coins
            )

            _state.value = _state.value.copy(
                portfolioState = portfolioState,
                lastCheck = now
            )

            // Check if rebalancing is needed
            if (rebalanceCalculator.needsRebalancing(portfolioState, config.threshold)) {
                logger.info(AppLogger.TAG_REBALANCER, "Rebalancing needed, calculating trades...")

                // Log portfolio state for debugging
                portfolioState.holdings.filter { it.targetPercentage > BigDecimal.ZERO }.forEach { holding ->
                    logger.debug(
                        AppLogger.TAG_REBALANCER,
                        "${holding.coin}: current=${holding.currentPercentage}%, target=${holding.targetPercentage}%, deviation=${holding.deviation}%"
                    )
                }

                val trades = rebalanceCalculator.calculateRebalanceTrades(
                    portfolioState = portfolioState,
                    tradingPairs = instrumentsCache
                )

                if (trades.isNotEmpty()) {
                    // Log planned trades
                    trades.forEach { trade ->
                        logger.info(
                            AppLogger.TAG_REBALANCER,
                            "Planned trade: ${trade.action} ${trade.amount} ${trade.coin} (${trade.usdtValue} USDT)"
                        )
                    }
                    executeRebalance(trades, portfolioState)
                } else {
                    logger.info(
                        AppLogger.TAG_REBALANCER,
                        "No trades generated - amounts may be below minimum order limits"
                    )
                }
            } else {
                logger.debug(AppLogger.TAG_REBALANCER, "Portfolio within threshold, no rebalancing needed")
            }

        } catch (e: Exception) {
            logger.error(AppLogger.TAG_REBALANCER, "Error during rebalance check", e)
        }
    }

    private suspend fun executeRebalance(trades: List<RebalanceAction>, stateBefore: PortfolioState) {
        val eventId = portfolioRepository.recordRebalanceEvent(
            RebalanceEventEntity(
                totalValueBefore = stateBefore.totalValueUsdt.toDouble(),
                totalValueAfter = null,
                trades = gson.toJson(trades.map { it.symbol }),
                status = "PENDING"
            )
        )

        logger.info(AppLogger.TAG_TRADE, "Starting rebalance with ${trades.size} trades")
        updateNotification("Rebalancing: ${trades.size} işlem yapılıyor...")

        val tradeIds = mutableListOf<Long>()
        var allSuccessful = true

        for (trade in trades) {
            try {
                // Record trade intention
                val tradeRecord = TradeHistoryEntity(
                    orderId = "",
                    symbol = trade.symbol,
                    side = trade.action.name,
                    quantity = trade.amount.toDouble(),
                    price = 0.0, // Will be updated after execution
                    usdtValue = trade.usdtValue.toDouble(),
                    fee = null,
                    status = "PENDING",
                    portfolioBefore = gson.toJson(stateBefore),
                    portfolioAfter = null,
                    reason = "Rebalance"
                )
                val tradeId = portfolioRepository.recordTrade(tradeRecord)
                tradeIds.add(tradeId)

                // Execute trade
                logger.debug(
                    AppLogger.TAG_TRADE,
                    "Executing order: ${trade.action} ${trade.symbol} qty=${trade.amount.toPlainString()}"
                )
                val result = bybitRepository.createOrder(
                    symbol = trade.symbol,
                    side = if (trade.action == TradeAction.BUY) "Buy" else "Sell",
                    qty = trade.amount.toPlainString()
                )

                if (result.isSuccess) {
                    val orderResult = result.getOrNull()!!
                    logger.info(
                        AppLogger.TAG_TRADE,
                        "${trade.action} ${trade.amount} ${trade.coin} - Order ID: ${orderResult.orderId}"
                    )

                    // Update trade record with order ID
                    portfolioRepository.updateTrade(
                        tradeRecord.copy(
                            id = tradeId,
                            orderId = orderResult.orderId,
                            status = "EXECUTED"
                        )
                    )
                } else {
                    allSuccessful = false
                    logger.error(
                        AppLogger.TAG_TRADE,
                        "Failed to execute ${trade.action} for ${trade.coin}: ${result.exceptionOrNull()?.message}"
                    )

                    portfolioRepository.updateTrade(
                        tradeRecord.copy(
                            id = tradeId,
                            status = "FAILED"
                        )
                    )
                }

                // Small delay between trades
                delay(500)

            } catch (e: Exception) {
                allSuccessful = false
                logger.error(AppLogger.TAG_TRADE, "Exception during trade execution", e)
            }
        }

        // Get portfolio state after trades
        delay(2000) // Wait for trades to settle
        val balanceResult = bybitRepository.getWalletBalance()
        val stateAfter = if (balanceResult.isSuccess) {
            val balances = balanceResult.getOrNull()!!
                .associate { it.coin to (it.walletBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

            val settings = portfolioRepository.getSettingsOnce()
            val coins = portfolioRepository.getActiveCoins().first()
            val targets = coins.associate { it.coin to BigDecimal(it.targetPercentage) }

            rebalanceCalculator.calculatePortfolioState(
                balances = balances,
                prices = priceCache.toMap(),
                targetPercentages = targets
            )
        } else null

        // Update trade records with after state
        if (stateAfter != null) {
            for (tradeId in tradeIds) {
                val trade = portfolioRepository.getTradeByOrderId("") // Get by ID instead
                // Update with after state
            }
        }

        // Update rebalance event
        val event = portfolioRepository.getRecentRebalanceEvents(1).first().firstOrNull()
        if (event != null) {
            portfolioRepository.updateRebalanceEvent(
                event.copy(
                    totalValueAfter = stateAfter?.totalValueUsdt?.toDouble(),
                    status = if (allSuccessful) "COMPLETED" else "PARTIAL",
                    errorMessage = if (!allSuccessful) "Some trades failed" else null
                )
            )
        }

        lastRebalanceTime = System.currentTimeMillis()
        _state.value = _state.value.copy(
            lastRebalance = lastRebalanceTime,
            portfolioState = stateAfter ?: stateBefore
        )

        val statusMsg = if (allSuccessful) "Rebalance tamamlandı" else "Rebalance kısmen başarılı"
        updateNotification(statusMsg)
        logger.info(AppLogger.TAG_REBALANCER, statusMsg)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RebalancerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bybit Rebalancer")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Durdur", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BybitRebalancer::RebalancerWakeLock"
        ).apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
