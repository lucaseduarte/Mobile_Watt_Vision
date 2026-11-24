package com.example.watt_vision_mobile.data.api

import android.content.Context
import com.example.watt_vision_mobile.BuildConfig
import com.example.watt_vision_mobile.data.repository.AuthRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    // Retrofit exige uma URL mesmo quando o Debug está sem servidor configurado.
    private const val DISABLED_BASE_URL = "https://watt-vision.invalid/"

    private var retrofitInstance: Retrofit? = null

    @Synchronized
    fun getRetrofit(context: Context): Retrofit {
        if (retrofitInstance == null) {
            val app = context.applicationContext
            val authInterceptor = AuthInterceptor(
                currentCredential = { AuthRepository.getInstance(app).currentCredential() },
                rejectIfCurrent = { AuthRepository.getInstance(app).rejectIfCurrent(it) },
                localSeriesEnabled = integracaoLocal,
                localSeriesPort = portaConsultaLocal
            )

            val baseUrl = BuildConfig.API_BASE_URL
            val client = OkHttpClient.Builder()
                .apply {
                    if (baseUrl.isEmpty()) {
                        addInterceptor { throw IOException("API Debug desativada. Configure wattVisionDebugApiUrl.") }
                    }
                }
                .addInterceptor(authInterceptor)
                .followRedirects(false)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()

            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl.ifEmpty { DISABLED_BASE_URL })
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    inline fun <reified T> createService(context: Context): T {
        return getRetrofit(context).create(T::class.java)
    }
}
