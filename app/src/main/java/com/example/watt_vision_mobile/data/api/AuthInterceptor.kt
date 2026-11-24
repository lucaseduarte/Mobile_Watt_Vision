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
        } else if (!permiteConsultaLocal(request)) {
            throw IOException("Consultas de negócio estão suspensas nesta entrega.")
        }

        val credential = currentCredential() ?: throw IOException("Sessão não validada.")
        val response = chain.proceed(request.newBuilder()
            .header("Authorization", credential.authorization())
            .build())
        if (response.code == 401) rejectIfCurrent(credential)
        return response
    }

    private fun permiteConsultaLocal(request: Request): Boolean {
        if (!BuildConfig.DEBUG || !localSeriesEnabled || request.method != "GET") return false
        val url = request.url
        if (url.scheme != "http" || url.host != "127.0.0.1" || url.port != 13000 ||
            url.username.isNotEmpty() || url.password.isNotEmpty() || url.fragment != null) return false

        val method = request.tag(Invocation::class.java)?.method() ?: return false
        return when (method.declaringClass) {
            WattVisionApiService::class.java -> when (method.name) {
                "getSerieTemporal" -> url.encodedPath == "/api/medicoes/serie-temporal" &&
                    url.querySize == 2 && url.queryParameterNames == setOf("id_pzem", "horas") &&
                    (url.queryParameter("id_pzem")?.toIntOrNull() ?: 0) > 0 &&
                    url.queryParameter("horas")?.toIntOrNull() in 1..720
                "getEquipamento" -> url.querySize == 0 && equipamento.matches(url.encodedPath)
                "getDispositivos" -> url.querySize == 0 && url.encodedPath == "/api/dispositivos-iot"
                else -> false
            }
            ChartCatalogApiService::class.java -> url.querySize == 0 && url.encodedPath == catalogo[method.name]
            AlarmesApiService::class.java -> url.querySize == 0 &&
                when (method.name) {
                    "listar" -> url.encodedPath == "/api/alertas"
                    "pontos" -> url.encodedPath == "/api/pontos-medicao"
                    "limite" -> limite.matches(url.encodedPath)
                    else -> false
                }
            else -> false
        }
    }

    private companion object {
        val catalogo = mapOf(
            "locais" to "/api/locais",
            "setores" to "/api/setores",
            "equipamentos" to "/api/equipamentos",
            "pontos" to "/api/pontos-medicao",
            "sensores" to "/api/pzems"
        )
        val equipamento = Regex("/api/equipamentos/[1-9][0-9]*")
        val limite = Regex("/api/limites/[1-9][0-9]*")
    }
}

internal val integracaoLocal: Boolean
    get() = BuildConfig.DEBUG && BuildConfig.LOCAL_SERIES_ENABLED &&
        BuildConfig.API_BASE_URL == "http://127.0.0.1:13000/"
