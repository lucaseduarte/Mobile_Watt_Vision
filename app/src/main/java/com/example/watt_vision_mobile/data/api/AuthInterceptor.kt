package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.BuildConfig
import com.example.watt_vision_mobile.data.repository.SessionCredential
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.http.GET
import retrofit2.http.POST

internal class AuthInterceptor(
    private val currentCredential: () -> SessionCredential?,
    private val rejectIfCurrent: (SessionCredential) -> Unit,
    private val localSeriesEnabled: Boolean = false
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.tag(Invocation::class.java)?.method()
        if (method?.declaringClass == AuthApiService::class.java) {
            if (method.getAnnotation(POST::class.java)?.value == "api/auth/login") {
                return chain.proceed(request.newBuilder().removeHeader("Authorization").build())
            }
            if (method.getAnnotation(GET::class.java)?.value != "api/auth/me") {
                throw IOException("Rota indisponível nesta entrega.")
            }

            // Login e restauração validam um candidato explícito antes de promovê-lo à sessão.
            if (request.header("Authorization") != null) return chain.proceed(request)
        } else if (!permiteSerieLocal(request)) {
            throw IOException("Consultas de negócio estão suspensas nesta entrega.")
        }

        val credential = currentCredential() ?: throw IOException("Sessão não validada.")
        val response = chain.proceed(request.newBuilder()
            .header("Authorization", credential.authorization())
            .build())
        if (response.code == 401) rejectIfCurrent(credential)
        return response
    }

    private fun permiteSerieLocal(request: Request): Boolean {
        if (!BuildConfig.DEBUG || !localSeriesEnabled) return false

        val method = request.tag(Invocation::class.java)?.method()
        val url = request.url
        return method?.declaringClass == WattVisionApiService::class.java &&
            method.name == "getSerieTemporal" &&
            request.method == "GET" &&
            url.scheme == "http" && url.host == "10.0.2.2" && url.port == 3000 &&
            url.username.isEmpty() && url.password.isEmpty() && url.fragment == null &&
            url.encodedPath == "/api/medicoes/serie-temporal" &&
            url.querySize == 2 && url.queryParameterNames == setOf("id_pzem", "horas") &&
            url.queryParameter("id_pzem") == "1" &&
            url.queryParameter("horas") in setOf("6", "24", "48", "168")
    }
}
