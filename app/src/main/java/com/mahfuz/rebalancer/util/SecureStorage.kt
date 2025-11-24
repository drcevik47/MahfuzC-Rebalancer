package com.mahfuz.rebalancer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mahfuz.rebalancer.data.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val regularPrefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_API_SECRET = "api_secret"
        private const val KEY_IS_TESTNET = "is_testnet"
        private const val KEY_AUTO_START = "auto_start_on_boot"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_LOG_RETENTION = "log_retention_days"
        private const val KEY_FIRST_RUN = "first_run"
    }

    // API Credentials (encrypted)
    fun saveApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(): String = securePrefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiSecret(apiSecret: String) {
        securePrefs.edit().putString(KEY_API_SECRET, apiSecret).apply()
    }

    fun getApiSecret(): String = securePrefs.getString(KEY_API_SECRET, "") ?: ""

    fun hasApiCredentials(): Boolean = getApiKey().isNotBlank() && getApiSecret().isNotBlank()

    fun clearApiCredentials() {
        securePrefs.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_API_SECRET)
            .apply()
    }

    // App Settings
    fun saveSettings(settings: AppSettings) {
        // API credentials are stored encrypted
        if (settings.apiKey.isNotBlank()) saveApiKey(settings.apiKey)
        if (settings.apiSecret.isNotBlank()) saveApiSecret(settings.apiSecret)

        // Other settings stored in regular prefs
        regularPrefs.edit()
            .putBoolean(KEY_IS_TESTNET, settings.isTestnet)
            .putBoolean(KEY_AUTO_START, settings.autoStartOnBoot)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putInt(KEY_LOG_RETENTION, settings.logRetentionDays)
            .apply()
    }

    fun getSettings(): AppSettings {
        return AppSettings(
            apiKey = getApiKey(),
            apiSecret = getApiSecret(),
            isTestnet = regularPrefs.getBoolean(KEY_IS_TESTNET, false),
            autoStartOnBoot = regularPrefs.getBoolean(KEY_AUTO_START, true),
            notificationsEnabled = regularPrefs.getBoolean(KEY_NOTIFICATIONS, true),
            logRetentionDays = regularPrefs.getInt(KEY_LOG_RETENTION, 30)
        )
    }

    fun isTestnet(): Boolean = regularPrefs.getBoolean(KEY_IS_TESTNET, false)

    fun setTestnet(isTestnet: Boolean) {
        regularPrefs.edit().putBoolean(KEY_IS_TESTNET, isTestnet).apply()
    }

    fun isAutoStartEnabled(): Boolean = regularPrefs.getBoolean(KEY_AUTO_START, true)

    fun setAutoStart(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun isFirstRun(): Boolean = regularPrefs.getBoolean(KEY_FIRST_RUN, true)

    fun setFirstRunComplete() {
        regularPrefs.edit().putBoolean(KEY_FIRST_RUN, false).apply()
    }
}
