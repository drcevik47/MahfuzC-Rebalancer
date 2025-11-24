package com.bybit.rebalancer.di

import android.content.Context
import androidx.room.Room
import com.bybit.rebalancer.api.BybitApiService
import com.bybit.rebalancer.api.BybitAuthInterceptor
import com.bybit.rebalancer.api.BybitWebSocket
import com.bybit.rebalancer.data.database.AppDatabase
import com.bybit.rebalancer.data.database.AppLogDao
import com.bybit.rebalancer.data.database.PortfolioCoinDao
import com.bybit.rebalancer.data.database.TradeLogDao
import com.bybit.rebalancer.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val MAINNET_BASE_URL = "https://api.bybit.com"
    private const val TESTNET_BASE_URL = "https://api-testnet.bybit.com"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bybit_rebalancer.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePortfolioCoinDao(database: AppDatabase): PortfolioCoinDao {
        return database.portfolioCoinDao()
    }

    @Provides
    @Singleton
    fun provideTradeLogDao(database: AppDatabase): TradeLogDao {
        return database.tradeLogDao()
    }

    @Provides
    @Singleton
    fun provideAppLogDao(database: AppDatabase): AppLogDao {
        return database.appLogDao()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(settingsRepository: SettingsRepository): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = BybitAuthInterceptor(
            apiKeyProvider = {
                runBlocking { settingsRepository.getApiKey() }
            },
            apiSecretProvider = {
                runBlocking { settingsRepository.getApiSecret() }
            }
        )

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, settingsRepository: SettingsRepository): Retrofit {
        // Default olarak mainnet kullan
        val baseUrl = runBlocking {
            val settings = settingsRepository.getSettings()
            if (settings.isTestnet) TESTNET_BASE_URL else MAINNET_BASE_URL
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBybitApiService(retrofit: Retrofit): BybitApiService {
        return retrofit.create(BybitApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBybitWebSocket(): BybitWebSocket {
        return BybitWebSocket()
    }
}
