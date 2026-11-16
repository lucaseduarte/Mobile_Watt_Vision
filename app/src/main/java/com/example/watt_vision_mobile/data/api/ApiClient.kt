package com.example.watt_vision_mobile.data.api

import android.content.Context
import com.example.watt_vision_mobile.data.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.107.144.6:3000/"

    private var retrofitInstance: Retrofit? = null

    fun getRetrofit(context: Context): Retrofit {
        if (retrofitInstance == null) {
            val tokenManager = TokenManager(context.applicationContext)
            val authInterceptor = AuthInterceptor(tokenManager)

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()

            retrofitInstance = Retrofit.Builder()
                .baseUrl(BASE_URL)
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
