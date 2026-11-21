package com.nowstation.app.core.network

import com.nowstation.app.BuildConfig
import com.nowstation.app.ride.data.RideApi
import com.nowstation.app.settings.FeedbackApi
import com.nowstation.app.station.data.StationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** 서버가 새 칸을 더해도 앱이 깨지지 않게 모르는 칸은 무시한다. */
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** 지하에서 오래 기다리면 화면이 멈춘 듯 보여 짧게 끊고 다음 주기에 다시 시도한다. */
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun stationApi(retrofit: Retrofit): StationApi = retrofit.create(StationApi::class.java)

    @Provides
    @Singleton
    fun rideApi(retrofit: Retrofit): RideApi = retrofit.create(RideApi::class.java)

    @Provides
    @Singleton
    fun feedbackApi(retrofit: Retrofit): FeedbackApi = retrofit.create(FeedbackApi::class.java)
}
