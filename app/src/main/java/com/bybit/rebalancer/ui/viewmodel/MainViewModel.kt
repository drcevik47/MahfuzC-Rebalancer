package com.bybit.rebalancer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bybit.rebalancer.api.BybitWebSocket
import com.bybit.rebalancer.data.model.*
import com.bybit.rebalancer.data.repository.LogRepository
import com.bybit.rebalancer.data.repository.PortfolioRepository
import com.bybit.rebalancer.data.repository.SettingsRepository
import com.bybit.rebalancer.data.repository.TradeRepository
import com.bybit.rebalancer.service.RebalancerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = false,
    val hasApiCredentials: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val portfolioCoins: List<PortfolioCoin> = emptyList(),
    val walletBalances: List<CoinBalance> = emptyList(),
    val portfolioSnapshot: PortfolioSnapshot? = null,
    val availableCoins: List<String> = emptyList(),
    val serviceState: ServiceState = ServiceState(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val portfolioRepository: PortfolioRepository,
    private val tradeRepository: TradeRepository,
    private val logRepository: LogRepository,
    private val webSocket: BybitWebSocket
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeServiceState()
        observePortfolioCoins()
        observeSettings()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val hasCredentials = settingsRepository.hasApiCredentials()
                _uiState.update { it.copy(hasApiCredentials = hasCredentials) }

                if (hasCredentials) {
                    loadWalletBalances()
                    loadAvailableCoins()
                }
            } catch (e: Exception) {
                logRepository.logError("MainViewModel", "Initial load error: ${e.message}")
                _uiState.update { it.copy(errorMessage = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            RebalancerService.serviceState.collect { state ->
                _uiState.update { it.copy(serviceState = state) }
            }
        }
    }

    private fun observePortfolioCoins() {
        viewModelScope.launch {
            portfolioRepository.getAllCoins().collect { coins ->
                _uiState.update { it.copy(portfolioCoins = coins) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun saveApiCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                settingsRepository.saveApiCredentials(apiKey, apiSecret)
                _uiState.update {
                    it.copy(
                        hasApiCredentials = true,
                        successMessage = "API bilgileri kaydedildi"
                    )
                }

                // API bilgileri kaydedildikten sonra verileri yükle
                loadWalletBalances()
                loadAvailableCoins()

                logRepository.logInfo("Settings", "API credentials saved")
            } catch (e: Exception) {
                logRepository.logError("Settings", "API credentials save error: ${e.message}")
                _uiState.update { it.copy(errorMessage = "API bilgileri kaydedilemedi: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = portfolioRepository.getWalletBalances()
                if (result.isSuccess) {
                    _uiState.update { it.copy(successMessage = "Bağlantı başarılı!") }
                    logRepository.logInfo("API", "Connection test successful")
                } else {
                    _uiState.update { it.copy(errorMessage = "Bağlantı hatası: ${result.exceptionOrNull()?.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Bağlantı hatası: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadWalletBalances() {
        viewModelScope.launch {
            try {
                val result = portfolioRepository.getWalletBalances()
                if (result.isSuccess) {
                    _uiState.update { it.copy(walletBalances = result.getOrNull() ?: emptyList()) }
                }
            } catch (e: Exception) {
                logRepository.logError("MainViewModel", "Load balances error: ${e.message}")
            }
        }
    }

    fun loadAvailableCoins() {
        viewModelScope.launch {
            try {
                val result = portfolioRepository.getAvailableInstruments()
                if (result.isSuccess) {
                    val coins = result.getOrNull()?.map { it.baseCoin }?.distinct()?.sorted() ?: emptyList()
                    _uiState.update { it.copy(availableCoins = coins) }
                }
            } catch (e: Exception) {
                logRepository.logError("MainViewModel", "Load coins error: ${e.message}")
            }
        }
    }

    fun addCoinToPortfolio(coin: String, targetPercentage: Double) {
        viewModelScope.launch {
            try {
                val portfolioCoin = PortfolioCoin(
                    coin = coin,
                    targetPercentage = targetPercentage
                )
                portfolioRepository.addCoin(portfolioCoin)
                _uiState.update { it.copy(successMessage = "$coin portföye eklendi") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Coin eklenemedi: ${e.message}") }
            }
        }
    }

    fun removeCoinFromPortfolio(coin: String) {
        viewModelScope.launch {
            try {
                portfolioRepository.removeCoin(coin)
                _uiState.update { it.copy(successMessage = "$coin portföyden kaldırıldı") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Coin kaldırılamadı: ${e.message}") }
            }
        }
    }

    fun updateCoinPercentage(coin: String, percentage: Double) {
        viewModelScope.launch {
            try {
                portfolioRepository.updateTargetPercentage(coin, percentage)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Yüzde güncellenemedi: ${e.message}") }
            }
        }
    }

    fun updateRebalanceThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.setRebalanceThreshold(threshold)
        }
    }

    fun updateMinTradeUsdt(amount: Double) {
        viewModelScope.launch {
            settingsRepository.setMinTradeUsdt(amount)
        }
    }

    fun updateCheckInterval(seconds: Int) {
        viewModelScope.launch {
            settingsRepository.setCheckInterval(seconds)
        }
    }

    fun setTestnet(isTestnet: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTestnet(isTestnet)
        }
    }

    fun refreshPortfolioSnapshot() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val result = portfolioRepository.calculatePortfolioSnapshot()
                if (result.isSuccess) {
                    _uiState.update { it.copy(portfolioSnapshot = result.getOrNull()) }
                }
            } catch (e: Exception) {
                logRepository.logError("MainViewModel", "Portfolio snapshot error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
