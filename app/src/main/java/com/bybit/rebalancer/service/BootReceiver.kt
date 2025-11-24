package com.bybit.rebalancer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bybit.rebalancer.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Telefon açıldığında Rebalancer servisini başlatır
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Log.i(TAG, "Boot completed event received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON") {

            // Pending result ile asenkron işlem
            val pendingResult = goAsync()

            scope.launch {
                try {
                    // Ayarları kontrol et
                    val settings = settingsRepository.getSettings()

                    if (settings.isServiceEnabled && settings.apiKey.isNotBlank()) {
                        Log.i(TAG, "Service was enabled, starting RebalancerService...")
                        RebalancerService.start(context)
                    } else {
                        Log.i(TAG, "Service not enabled or API credentials missing")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking settings: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
