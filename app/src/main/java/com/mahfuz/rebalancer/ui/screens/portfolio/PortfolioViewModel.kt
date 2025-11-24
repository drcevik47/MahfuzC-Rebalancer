package com.mahfuz.rebalancer.ui.screens.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfuz.rebalancer.data.db.PortfolioCoinEntity
import com.mahfuz.rebalancer.data.model.CoinBalance
import com.mahfuz.rebalancer.data.repository.BybitRepository
import com.mahfuz.rebalancer.data.repository.PortfolioRepository
import com.mahfuz.rebalancer.util.AppLogger
import com.mahfuz.rebalancer.util.RebalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class PortfolioUiState(
    val availableCoins: List<String> = emptyList(),
    val walletCoins: List<CoinBalance> = emptyList(),
    val portfolioCoins: List<PortfolioCoinEntity> = emptyList(),
    val threshold: Double = 1.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalPercentage: Double = 0.0,
    val isValid: Boolean = false
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val bybitRepository: BybitRepository,
    private val portfolioRepository: PortfolioRepository,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val rebalanceCalculator = RebalanceCalculator()

    init {
        loadData()
        observePortfolio()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get available coins from exchange
                val coinsResult = bybitRepository.getAvailableCoins()
                if (coinsResult.isSuccess) {
                    _uiState.update {
                        it.copy(availableCoins = listOf("USDT") + coinsResult.getOrNull()!!)
                    }
                }

                // Get wallet balances
                val balanceResult = bybitRepository.getWalletBalance()
                if (balanceResult.isSuccess) {
                    _uiState.update {
                        it.copy(walletCoins = balanceResult.getOrNull()!!)
                    }
                }

                // Initialize portfolio settings
                portfolioRepository.initializeSettings()

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                logger.error(AppLogger.TAG_PORTFOLIO, "Failed to load portfolio data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun observePortfolio() {
        viewModelScope.launch {
            portfolioRepository.getAllCoins().collect { coins ->
                val total = coins.filter { it.isActive }.sumOf { it.targetPercentage }
                val validation = rebalanceCalculator.validateTargetPercentages(
                    coins.filter { it.isActive }.associate { it.coin to BigDecimal(it.targetPercentage) }
                )

                _uiState.update {
                    it.copy(
                        portfolioCoins = coins,
                        totalPercentage = total,
                        isValid = validation.isValid
                    )
                }
            }
        }

        viewModelScope.launch {
            portfolioRepository.getSettings().collect { settings ->
                _uiState.update {
                    it.copy(threshold = settings?.threshold ?: 1.0)
                }
            }
        }
    }

    fun addCoin(coin: String, percentage: Double) {
        viewModelScope.launch {
            portfolioRepository.addCoin(coin, percentage)
            logger.info(AppLogger.TAG_PORTFOLIO, "Added $coin with ${percentage}% target")
        }
    }

    fun updateCoinPercentage(coin: String, percentage: Double) {
        viewModelScope.launch {
            portfolioRepository.updateCoinPercentage(coin, percentage)
        }
    }

    fun removeCoin(coin: String) {
        viewModelScope.launch {
            portfolioRepository.removeCoin(coin)
            logger.info(AppLogger.TAG_PORTFOLIO, "Removed $coin from portfolio")
        }
    }

    fun setCoinActive(coin: String, active: Boolean) {
        viewModelScope.launch {
            portfolioRepository.setCoinActive(coin, active)
        }
    }

    fun setThreshold(threshold: Double) {
        viewModelScope.launch {
            portfolioRepository.setThreshold(threshold)
            logger.info(AppLogger.TAG_PORTFOLIO, "Threshold set to ${threshold}%")
        }
    }

    fun distributeEvenly() {
        viewModelScope.launch {
            val activeCoins = _uiState.value.portfolioCoins.filter { it.isActive }
            if (activeCoins.isEmpty()) return@launch

            val evenPercentage = 100.0 / activeCoins.size
            activeCoins.forEach { coin ->
                portfolioRepository.updateCoinPercentage(coin.coin, evenPercentage)
            }
            logger.info(AppLogger.TAG_PORTFOLIO, "Distributed percentages evenly: ${evenPercentage}% each")
        }
    }

    fun addFromWallet() {
        viewModelScope.launch {
            val walletCoins = _uiState.value.walletCoins
            val existingCoins = _uiState.value.portfolioCoins.map { it.coin }

            walletCoins.forEach { balance ->
                if (balance.coin !in existingCoins) {
                    val usdValue = balance.usdValue.toDoubleOrNull() ?: 0.0
                    if (usdValue > 1.0) { // Only add coins worth more than $1
                        portfolioRepository.addCoin(balance.coin, 0.0)
                    }
                }
            }
            logger.info(AppLogger.TAG_PORTFOLIO, "Added coins from wallet")
        }
    }

    fun clearPortfolio() {
        viewModelScope.launch {
            portfolioRepository.clearAllCoins()
            logger.info(AppLogger.TAG_PORTFOLIO, "Cleared all portfolio coins")
        }
    }
}
