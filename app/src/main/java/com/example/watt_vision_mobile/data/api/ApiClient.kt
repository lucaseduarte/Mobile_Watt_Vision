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

    private val clientes = mutableMapOf<String, Retrofit>()

    fun getRetrofit(context: Context): Retrofit = getRetrofit(context, ConfiguracaoApi.url)

    @Synchronized
    internal fun getRetrofit(context: Context, baseUrl: String): Retrofit =
        clientes.getOrPut(baseUrl) {
            val app = context.applicationContext
            val origem = ConfiguracaoApi.origemLocal(baseUrl)
            val authInterceptor = AuthInterceptor(
                currentCredential = { AuthRepository.getInstance(app, baseUrl).currentCredential() },
                rejectIfCurrent = { AuthRepository.getInstance(app, baseUrl).rejectIfCurrent(it) },
                localSeriesEnabled = BuildConfig.LOCAL_SERIES_ENABLED && origem != null,
                localSeriesPort = origem?.port,
                localSeriesHost = origem?.host.orEmpty()
            )
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

            Retrofit.Builder()
                .baseUrl(baseUrl.ifEmpty { DISABLED_BASE_URL })
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

    inline fun <reified T> createService(context: Context): T {
        return getRetrofit(context).create(T::class.java)
    }
}
