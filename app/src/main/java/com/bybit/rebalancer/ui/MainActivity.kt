package com.bybit.rebalancer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bybit.rebalancer.service.RebalancerService
import com.bybit.rebalancer.ui.screens.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.LogViewModel
import com.bybit.rebalancer.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Ana Sayfa", Icons.Default.Dashboard)
    object Portfolio : Screen("portfolio", "Portföy", Icons.Default.AccountBalanceWallet)
    object Settings : Screen("settings", "Ayarlar", Icons.Default.Settings)
    object ApiSettings : Screen("api_settings", "API", Icons.Default.Key)
    object Logs : Screen("logs", "Kayıtlar", Icons.Default.Description)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Portfolio,
    Screen.Settings,
    Screen.ApiSettings,
    Screen.Logs
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BybitRebalancerTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    logViewModel: LogViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val logUiState by logViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error/success messages
    LaunchedEffect(mainUiState.errorMessage, mainUiState.successMessage) {
        mainUiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            mainViewModel.clearMessages()
        }
        mainUiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            mainViewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = BybitDarkCard,
                    contentColor = TextPrimary
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = BybitDarkSurface,
                contentColor = TextPrimary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BybitYellow,
                            selectedTextColor = BybitYellow,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = BybitYellow.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        },
        containerColor = BybitDark
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                DashboardScreen(
                    uiState = mainUiState,
                    onStartService = {
                        RebalancerService.start(context)
                    },
                    onStopService = {
                        RebalancerService.stop(context)
                    },
                    onRefresh = {
                        mainViewModel.loadWalletBalances()
                        mainViewModel.refreshPortfolioSnapshot()
                    }
                )
            }

            composable(Screen.Portfolio.route) {
                PortfolioScreen(
                    uiState = mainUiState,
                    onAddCoin = { coin, percentage ->
                        mainViewModel.addCoinToPortfolio(coin, percentage)
                    },
                    onRemoveCoin = { coin ->
                        mainViewModel.removeCoinFromPortfolio(coin)
                    },
                    onUpdatePercentage = { coin, percentage ->
                        mainViewModel.updateCoinPercentage(coin, percentage)
                    },
                    onRefresh = {
                        mainViewModel.loadWalletBalances()
                        mainViewModel.refreshPortfolioSnapshot()
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = mainUiState,
                    onUpdateThreshold = { mainViewModel.updateRebalanceThreshold(it) },
                    onUpdateMinTrade = { mainViewModel.updateMinTradeUsdt(it) },
                    onUpdateCheckInterval = { mainViewModel.updateCheckInterval(it) }
                )
            }

            composable(Screen.ApiSettings.route) {
                ApiSettingsScreen(
                    uiState = mainUiState,
                    onSaveCredentials = { key, secret ->
                        mainViewModel.saveApiCredentials(key, secret)
                    },
                    onTestConnection = {
                        mainViewModel.testConnection()
                    },
                    onSetTestnet = { mainViewModel.setTestnet(it) }
                )
            }

            composable(Screen.Logs.route) {
                LogsScreen(
                    uiState = logUiState,
                    onFilterChange = { logViewModel.setFilterLevel(it) },
                    onSearch = { logViewModel.searchLogs(it) },
                    onClearLogs = { logViewModel.clearAllLogs() },
                    onClearTrades = { logViewModel.clearAllTrades() },
                    onExport = { logViewModel.exportLogs() }
                )
            }
        }
    }
}
