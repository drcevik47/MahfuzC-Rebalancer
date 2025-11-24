package com.mahfuz.rebalancer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfuz.rebalancer.data.model.AppSettings
import com.mahfuz.rebalancer.data.repository.BybitRepository
import com.mahfuz.rebalancer.util.AppLogger
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isTestnet: Boolean = false,
    val autoStartOnBoot: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val logRetentionDays: Int = 30,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val bybitRepository: BybitRepository,
    private val logger: AppLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val settings = secureStorage.getSettings()
        _uiState.update {
            it.copy(
                apiKey = settings.apiKey,
                apiSecret = settings.apiSecret,
                isTestnet = settings.isTestnet,
                autoStartOnBoot = settings.autoStartOnBoot,
                notificationsEnabled = settings.notificationsEnabled,
                logRetentionDays = settings.logRetentionDays
            )
        }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value, testResult = null, saveSuccess = false) }
    }

    fun updateApiSecret(value: String) {
        _uiState.update { it.copy(apiSecret = value, testResult = null, saveSuccess = false) }
    }

    fun updateTestnet(value: Boolean) {
        _uiState.update { it.copy(isTestnet = value, testResult = null) }
    }

    fun updateAutoStart(value: Boolean) {
        _uiState.update { it.copy(autoStartOnBoot = value) }
        secureStorage.setAutoStart(value)
    }

    fun updateNotifications(value: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = value) }
    }

    fun updateLogRetention(days: Int) {
        _uiState.update { it.copy(logRetentionDays = days) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }

            try {
                // Set credentials temporarily for testing
                bybitRepository.setCredentials(
                    _uiState.value.apiKey,
                    _uiState.value.apiSecret
                )

                // Try to get wallet balance
                val result = bybitRepository.getWalletBalance()

                if (result.isSuccess) {
                    val coins = result.getOrNull()!!
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testResult = "Bağlantı başarılı! ${coins.size} coin bulundu."
                        )
                    }
                    logger.info(AppLogger.TAG_API, "API connection test successful")
                } else {
                    _uiState.update {
                        it.copy(
                            isTesting = false,
                            testResult = "Hata: ${result.exceptionOrNull()?.message}"
                        )
                    }
                    logger.error(AppLogger.TAG_API, "API connection test failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTesting = false,
                        testResult = "Hata: ${e.message}"
                    )
                }
                logger.error(AppLogger.TAG_API, "API connection test exception", e)
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val settings = AppSettings(
                    apiKey = _uiState.value.apiKey,
                    apiSecret = _uiState.value.apiSecret,
                    isTestnet = _uiState.value.isTestnet,
                    autoStartOnBoot = _uiState.value.autoStartOnBoot,
                    notificationsEnabled = _uiState.value.notificationsEnabled,
                    logRetentionDays = _uiState.value.logRetentionDays
                )

                secureStorage.saveSettings(settings)
                bybitRepository.setCredentials(settings.apiKey, settings.apiSecret)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true
                    )
                }
                logger.info(AppLogger.TAG_SYSTEM, "Settings saved successfully")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        testResult = "Kaydetme hatası: ${e.message}"
                    )
                }
                logger.error(AppLogger.TAG_SYSTEM, "Failed to save settings", e)
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logger.clearAllLogs()
            logger.info(AppLogger.TAG_SYSTEM, "All logs cleared")
        }
    }
}
