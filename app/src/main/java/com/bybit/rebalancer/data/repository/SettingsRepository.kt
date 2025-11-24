package com.bybit.rebalancer.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bybit.rebalancer.data.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferenceKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val API_SECRET = stringPreferencesKey("api_secret")
        val IS_TESTNET = booleanPreferencesKey("is_testnet")
        val REBALANCE_THRESHOLD = doublePreferencesKey("rebalance_threshold")
        val MIN_TRADE_USDT = doublePreferencesKey("min_trade_usdt")
        val IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        val CHECK_INTERVAL_SECONDS = intPreferencesKey("check_interval_seconds")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                apiKey = preferences[PreferenceKeys.API_KEY] ?: "",
                apiSecret = preferences[PreferenceKeys.API_SECRET] ?: "",
                isTestnet = preferences[PreferenceKeys.IS_TESTNET] ?: false,
                rebalanceThreshold = preferences[PreferenceKeys.REBALANCE_THRESHOLD] ?: 1.0,
                minTradeUsdt = preferences[PreferenceKeys.MIN_TRADE_USDT] ?: 10.0,
                isServiceEnabled = preferences[PreferenceKeys.IS_SERVICE_ENABLED] ?: false,
                checkIntervalSeconds = preferences[PreferenceKeys.CHECK_INTERVAL_SECONDS] ?: 30
            )
        }

    suspend fun getSettings(): AppSettings {
        return settingsFlow.first()
    }

    suspend fun saveApiCredentials(apiKey: String, apiSecret: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
            preferences[PreferenceKeys.API_SECRET] = apiSecret
        }
    }

    suspend fun getApiKey(): String {
        return context.dataStore.data.first()[PreferenceKeys.API_KEY] ?: ""
    }

    suspend fun getApiSecret(): String {
        return context.dataStore.data.first()[PreferenceKeys.API_SECRET] ?: ""
    }

    suspend fun setTestnet(isTestnet: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_TESTNET] = isTestnet
        }
    }

    suspend fun setRebalanceThreshold(threshold: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.REBALANCE_THRESHOLD] = threshold
        }
    }

    suspend fun setMinTradeUsdt(amount: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MIN_TRADE_USDT] = amount
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setCheckInterval(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CHECK_INTERVAL_SECONDS] = seconds
        }
    }

    suspend fun hasApiCredentials(): Boolean {
        val settings = getSettings()
        return settings.apiKey.isNotBlank() && settings.apiSecret.isNotBlank()
    }

    suspend fun clearApiCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.API_KEY)
            preferences.remove(PreferenceKeys.API_SECRET)
        }
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
