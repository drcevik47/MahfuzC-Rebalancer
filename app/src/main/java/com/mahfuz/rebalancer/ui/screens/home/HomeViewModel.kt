package com.mahfuz.rebalancer.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfuz.rebalancer.data.model.*
import com.mahfuz.rebalancer.data.repository.BybitRepository
import com.mahfuz.rebalancer.data.repository.PortfolioRepository
import com.mahfuz.rebalancer.service.RebalancerService
import com.mahfuz.rebalancer.util.AppLogger
import com.mahfuz.rebalancer.util.RebalanceCalculator
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class HomeUiState(
    val serviceState: ServiceState = ServiceState.STOPPED,
    val isConnected: Boolean = false,
    val portfolioState: PortfolioState? = null,
    val portfolioConfig: PortfolioConfig = PortfolioConfig.EMPTY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasApiCredentials: Boolean = false,
    val lastUpdate: Long = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bybitRepository: BybitRepository,
    private val portfolioRepository: PortfolioRepository,
    private val secureStorage: SecureStorage,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val rebalanceCalculator = RebalanceCalculator()

    init {
        checkApiCredentials()
        observePortfolioConfig()
        observeServiceState()
    }

    private fun checkApiCredentials() {
        val hasCredentials = secureStorage.hasApiCredentials()
        _uiState.update { it.copy(hasApiCredentials = hasCredentials) }

        if (hasCredentials) {
            val settings = secureStorage.getSettings()
            bybitRepository.setCredentials(settings.apiKey, settings.apiSecret)
        }
    }

    private fun observePortfolioConfig() {
        viewModelScope.launch {
            portfolioRepository.getPortfolioConfig().collect { config ->
                _uiState.update { it.copy(portfolioConfig = config) }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            // Poll service state
            while (true) {
                val service = RebalancerService.getInstance()
                if (service != null) {
                    service.state.collect { state ->
                        _uiState.update {
                            it.copy(
                                serviceState = state.serviceState,
                                isConnected = state.isConnected,
                                portfolioState = state.portfolioState ?: it.portfolioState,
                                lastUpdate = state.lastCheck
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            serviceState = ServiceState.STOPPED,
                            isConnected = false
                        )
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun refreshPortfolio() {
        // Re-check API credentials first
        checkApiCredentials()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val balanceResult = bybitRepository.getWalletBalance()
                if (balanceResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = balanceResult.exceptionOrNull()?.message
                        )
                    }
                    return@launch
                }

                val balances = balanceResult.getOrNull()!!
                    .associate { it.coin to (it.walletBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

                // Get prices
                val tickersResult = bybitRepository.getTickers()
                if (tickersResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = tickersResult.exceptionOrNull()?.message
                        )
                    }
                    return@launch
                }

                val prices = tickersResult.getOrNull()!!
                    .filter { it.symbol.endsWith("USDT") }
                    .associate {
                        it.symbol.removeSuffix("USDT") to (it.lastPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                    }

                // Get target percentages
                val config = _uiState.value.portfolioConfig

                val portfolioState = rebalanceCalculator.calculatePortfolioState(
                    balances = balances,
                    prices = prices,
                    targetPercentages = config.coins
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        portfolioState = portfolioState,
                        lastUpdate = System.currentTimeMillis()
                    )
                }

            } catch (e: Exception) {
                logger.error(AppLogger.TAG_PORTFOLIO, "Failed to refresh portfolio", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun startService(context: android.content.Context) {
        RebalancerService.start(context)
        logger.info(AppLogger.TAG_SERVICE, "Service start requested from UI")
    }

    fun stopService(context: android.content.Context) {
        RebalancerService.stop(context)
        logger.info(AppLogger.TAG_SERVICE, "Service stop requested from UI")
    }

    fun toggleRebalancing(active: Boolean) {
        viewModelScope.launch {
            portfolioRepository.setRebalancingActive(active)
            logger.info(AppLogger.TAG_REBALANCER, "Rebalancing ${if (active) "enabled" else "disabled"}")
        }
    }
}
