package com.mahfuz.rebalancer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("BootReceiver", "Device boot completed, checking auto-start setting")

            // Check if auto-start is enabled and API credentials are set
            if (secureStorage.isAutoStartEnabled() && secureStorage.hasApiCredentials()) {
                Log.i("BootReceiver", "Auto-start enabled, starting RebalancerService")
                RebalancerService.start(context)
            } else {
                Log.i("BootReceiver", "Auto-start disabled or no API credentials")
            }
        }
    }
}
