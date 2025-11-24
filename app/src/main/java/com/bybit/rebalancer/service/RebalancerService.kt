package com.bybit.rebalancer.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.bybit.rebalancer.BybitRebalancerApp
import com.bybit.rebalancer.R
import com.bybit.rebalancer.api.BybitWebSocket
import com.bybit.rebalancer.data.model.ConnectionStatus
import com.bybit.rebalancer.data.model.ServiceState
import com.bybit.rebalancer.data.repository.LogRepository
import com.bybit.rebalancer.data.repository.PortfolioRepository
import com.bybit.rebalancer.data.repository.SettingsRepository
import com.bybit.rebalancer.data.repository.TradeRepository
import com.bybit.rebalancer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class RebalancerService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var portfolioRepository: PortfolioRepository

    @Inject
    lateinit var tradeRepository: TradeRepository

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var webSocket: BybitWebSocket

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitorJob: Job? = null
    private var isRunning = false

    companion object {
        private const val TAG = "RebalancerService"
        const val ACTION_START = "com.bybit.rebalancer.START_SERVICE"
        const val ACTION_STOP = "com.bybit.rebalancer.STOP_SERVICE"

        private val _serviceState = MutableStateFlow(ServiceState())
        val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

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

        fun isRunning(): Boolean = _serviceState.value.isRunning
    }

    override fun onCreate() {
        super.onCreate()
        logRepository.logInfo(TAG, "Service oluşturuldu")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRebalancer()
            ACTION_STOP -> stopRebalancer()
            else -> {
                // Service restart edildiğinde
                if (!isRunning) {
                    serviceScope.launch {
                        if (settingsRepository.getSettings().isServiceEnabled) {
                            startRebalancer()
                        }
                    }
                }
            }
        }
        return START_STICKY // Service kill edilirse yeniden başlat
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        logRepository.logInfo(TAG, "Service destroy edildi")
        cleanup()

        // Servis yeniden başlatılmalı mı kontrol et
        serviceScope.launch {
            if (settingsRepository.getSettings().isServiceEnabled) {
                logRepository.logWarning(TAG, "Service beklenmedik şekilde kapandı, yeniden başlatılıyor...")
                scheduleServiceRestart()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        logRepository.logInfo(TAG, "Task removed - uygulama kapatıldı")

        // Uygulama kapatıldığında servisi yeniden başlat
        serviceScope.launch {
            if (settingsRepository.getSettings().isServiceEnabled) {
                scheduleServiceRestart()
            }
        }
    }

    private fun startRebalancer() {
        if (isRunning) {
            logRepository.logInfo(TAG, "Service zaten çalışıyor")
            return
        }

        logRepository.logInfo(TAG, "Rebalancer başlatılıyor...")
        isRunning = true

        // Foreground notification başlat
        startForeground(BybitRebalancerApp.NOTIFICATION_ID, createNotification())

        // Wake lock al
        acquireWakeLock()

        // State güncelle
        _serviceState.value = ServiceState(
            isRunning = true,
            connectionStatus = ConnectionStatus.CONNECTING
        )

        // Ana izleme döngüsünü başlat
        startMonitoringLoop()
    }

    private fun stopRebalancer() {
        logRepository.logInfo(TAG, "Rebalancer durduruluyor...")
        isRunning = false
        cleanup()

        serviceScope.launch {
            settingsRepository.setServiceEnabled(false)
        }

        _serviceState.value = ServiceState(
            isRunning = false,
            connectionStatus = ConnectionStatus.DISCONNECTED
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMonitoringLoop() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            try {
                // Ayarları al
                val settings = settingsRepository.getSettings()

                // WebSocket bağlantısını başlat
                webSocket.connect(settings.isTestnet)

                // Bağlantı durumunu dinle
                launch {
                    webSocket.connectionStatus.collect { status ->
                        _serviceState.value = _serviceState.value.copy(connectionStatus = status)
                        updateNotification()
                    }
                }

                // Instrument bilgilerini yükle
                tradeRepository.loadInstruments()

                // Portföy coinlerini WebSocket'e abone et
                val coins = portfolioRepository.getEnabledCoinsOnce()
                val symbols = coins.filter { it.coin != "USDT" }.map { "${it.coin}USDT" }
                webSocket.subscribeMultipleTickers(symbols)

                logRepository.logInfo(TAG, "Monitoring başladı - ${symbols.size} sembol izleniyor")

                // Ana döngü
                while (isActive && isRunning) {
                    try {
                        val currentSettings = settingsRepository.getSettings()

                        // Rebalance gerekli mi kontrol et
                        val needsResult = tradeRepository.needsRebalancing(currentSettings.rebalanceThreshold)

                        if (needsResult.isSuccess && needsResult.getOrNull() == true) {
                            logRepository.logInfo(TAG, "Rebalance gerekli, işlemler hesaplanıyor...")

                            _serviceState.value = _serviceState.value.copy(
                                lastCheckTime = System.currentTimeMillis()
                            )

                            // Trade'leri hesapla
                            val tradesResult = tradeRepository.calculateRebalanceTrades(
                                threshold = currentSettings.rebalanceThreshold,
                                minTradeUsdt = currentSettings.minTradeUsdt
                            )

                            if (tradesResult.isSuccess) {
                                val trades = tradesResult.getOrNull()!!
                                if (trades.isNotEmpty()) {
                                    logRepository.logInfo(TAG, "${trades.size} işlem yapılacak")

                                    // Trade'leri gerçekleştir
                                    val executeResult = tradeRepository.executeAllTrades(trades)

                                    _serviceState.value = _serviceState.value.copy(
                                        lastRebalanceTime = System.currentTimeMillis()
                                    )

                                    updateNotification()
                                }
                            }
                        }

                        _serviceState.value = _serviceState.value.copy(
                            lastCheckTime = System.currentTimeMillis(),
                            errorMessage = null
                        )

                    } catch (e: Exception) {
                        logRepository.logError(TAG, "Monitoring döngüsü hatası: ${e.message}", e.stackTraceToString())
                        _serviceState.value = _serviceState.value.copy(
                            errorMessage = e.message
                        )
                    }

                    // Belirlenen aralıkla kontrol et
                    val interval = settingsRepository.getSettings().checkIntervalSeconds
                    delay(interval * 1000L)
                }

            } catch (e: CancellationException) {
                logRepository.logInfo(TAG, "Monitoring döngüsü iptal edildi")
            } catch (e: Exception) {
                logRepository.logError(TAG, "Monitoring başlatma hatası: ${e.message}", e.stackTraceToString())
                _serviceState.value = _serviceState.value.copy(
                    errorMessage = e.message,
                    connectionStatus = ConnectionStatus.ERROR
                )
            }
        }
    }

    private fun cleanup() {
        monitorJob?.cancel()
        webSocket.disconnect()
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        wakeLock?.let { return }
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BybitRebalancer::WakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 saat
        }
        logRepository.logDebug(TAG, "WakeLock alındı")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                logRepository.logDebug(TAG, "WakeLock bırakıldı")
            }
        }
        wakeLock = null
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RebalancerService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val state = _serviceState.value
        val statusText = when (state.connectionStatus) {
            ConnectionStatus.CONNECTED -> "Bağlı - Portföy izleniyor"
            ConnectionStatus.CONNECTING -> "Bağlanıyor..."
            ConnectionStatus.DISCONNECTED -> "Bağlantı kesildi"
            ConnectionStatus.ERROR -> "Hata: ${state.errorMessage ?: "Bilinmeyen hata"}"
        }

        return NotificationCompat.Builder(this, BybitRebalancerApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.stop_service),
                stopIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(BybitRebalancerApp.NOTIFICATION_ID, createNotification())
    }

    private fun scheduleServiceRestart() {
        val restartIntent = Intent(this, ServiceRestartReceiver::class.java).apply {
            action = "com.bybit.rebalancer.RESTART_SERVICE"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5000, // 5 saniye sonra
            pendingIntent
        )

        logRepository.logInfo(TAG, "Service restart zamanlandı")
    }
}
