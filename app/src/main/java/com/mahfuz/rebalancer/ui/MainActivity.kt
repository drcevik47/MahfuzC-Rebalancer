package com.mahfuz.rebalancer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mahfuz.rebalancer.service.RebalancerService
import com.mahfuz.rebalancer.ui.screens.home.HomeScreen
import com.mahfuz.rebalancer.ui.screens.logs.LogsScreen
import com.mahfuz.rebalancer.ui.screens.portfolio.PortfolioScreen
import com.mahfuz.rebalancer.ui.screens.settings.SettingsScreen
import com.mahfuz.rebalancer.ui.theme.BybitRebalancerTheme
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var secureStorage: SecureStorage

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndStartService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions
        requestPermissions()

        // Request battery optimization exemption
        requestBatteryOptimizationExemption()

        setContent {
            BybitRebalancerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RebalancerApp()
                }
            }
        }

        // Start service if credentials exist
        checkAndStartService()
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = packageName

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Some devices don't support this
            }
        }
    }

    private fun checkAndStartService() {
        if (secureStorage.hasApiCredentials() && secureStorage.isAutoStartEnabled()) {
            RebalancerService.start(this)
        }
    }
}

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val PORTFOLIO = "portfolio"
    const val LOGS = "logs"
}

@Composable
fun RebalancerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToPortfolio = { navController.navigate(Routes.PORTFOLIO) },
                onNavigateToLogs = { navController.navigate(Routes.LOGS) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PORTFOLIO) {
            PortfolioScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LOGS) {
            LogsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
