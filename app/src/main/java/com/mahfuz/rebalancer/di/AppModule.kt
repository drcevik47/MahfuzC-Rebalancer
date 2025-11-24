package com.mahfuz.rebalancer.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mahfuz.rebalancer.data.api.BybitApiService
import com.mahfuz.rebalancer.data.db.*
import com.mahfuz.rebalancer.util.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
        secureStorage: SecureStorage
    ): Retrofit {
        val baseUrl = if (secureStorage.isTestnet()) {
            BybitApiService.BASE_URL_TESTNET
        } else {
            BybitApiService.BASE_URL_MAINNET
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideBybitApiService(retrofit: Retrofit): BybitApiService =
        retrofit.create(BybitApiService::class.java)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    @Singleton
    fun provideLogDao(database: AppDatabase): LogDao = database.logDao()

    @Provides
    @Singleton
    fun providePortfolioCoinDao(database: AppDatabase): PortfolioCoinDao =
        database.portfolioCoinDao()

    @Provides
    @Singleton
    fun providePortfolioSettingsDao(database: AppDatabase): PortfolioSettingsDao =
        database.portfolioSettingsDao()

    @Provides
    @Singleton
    fun provideTradeHistoryDao(database: AppDatabase): TradeHistoryDao =
        database.tradeHistoryDao()

    @Provides
    @Singleton
    fun providePriceHistoryDao(database: AppDatabase): PriceHistoryDao =
        database.priceHistoryDao()

    @Provides
    @Singleton
    fun provideRebalanceEventDao(database: AppDatabase): RebalanceEventDao =
        database.rebalanceEventDao()

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage =
        SecureStorage(context)
}
