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
 * Servis beklenmedik şekilde kapandığında yeniden başlatır
 */
@AndroidEntryPoint
class ServiceRestartReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "ServiceRestartReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Service restart request received")

        val pendingResult = goAsync()

        scope.launch {
            try {
                val settings = settingsRepository.getSettings()

                if (settings.isServiceEnabled && settings.apiKey.isNotBlank()) {
                    Log.i(TAG, "Restarting RebalancerService...")
                    RebalancerService.start(context)
                } else {
                    Log.i(TAG, "Service restart skipped - not enabled or no credentials")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restarting service: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
