package com.mahfuz.rebalancer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RestartReceiver : BroadcastReceiver() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.mahfuz.rebalancer.RESTART_SERVICE") {
            Log.i("RestartReceiver", "Received restart broadcast")

            // Check if auto-start is enabled
            if (secureStorage.isAutoStartEnabled() && secureStorage.hasApiCredentials()) {
                Log.i("RestartReceiver", "Restarting RebalancerService")
                RebalancerService.start(context)
            }
        }
    }
}
