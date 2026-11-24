package com.example.watt_vision_mobile.data.api

import com.google.gson.Gson
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal fun transporteDemonstracao(responder: (String) -> Any?): Retrofit {
    val gson = Gson()
    return Retrofit.Builder()
        .baseUrl("https://watt-vision.invalid/")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            if (request.method != "GET" || request.url.query != null) {
                throw IOException("Consulta indisponível na demonstração.")
            }
            val resultado = responder(request.url.encodedPath)
                ?: throw IOException("Rota indisponível na demonstração.")
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Demonstração")
                .body(gson.toJson(resultado).toResponseBody())
                .build()
        }.build())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}

internal fun <T> lerDemonstracao(resposta: retrofit2.Response<com.example.watt_vision_mobile.model.ApiResponse<T>>): T =
    resposta.takeIf { it.isSuccessful }?.body()?.takeIf { it.sucesso }?.dados
        ?: throw IOException("Resposta simulada indisponível.")
