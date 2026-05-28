package com.example.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- CoinCap Retrofit Service for Crypto Prices ---
interface CoinCapService {
    @GET("v2/assets")
    suspend fun getAssets(
        @Query("limit") limit: Int
    ): CoinCapResponse
}

data class CoinCapResponse(
    val data: List<CoinData>
)

data class CoinData(
    val id: String,
    val symbol: String,
    val name: String,
    val priceUsd: String,
    val changePercent24Hr: String?
)

// --- Exchange Rate API Service for Forex Rates ---
interface ExchangeRateService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateResponse
}

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)

// --- Core API Client ---
object RealDataClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val coinCapService: CoinCapService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.coincap.io/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CoinCapService::class.java)
    }

    val exchangeRateService: ExchangeRateService by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateService::class.java)
    }
}
