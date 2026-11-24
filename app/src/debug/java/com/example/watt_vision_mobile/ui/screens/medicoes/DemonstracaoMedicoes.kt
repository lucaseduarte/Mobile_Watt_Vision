package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.data.api.*
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

internal const val MINUTOS_POR_AMOSTRA = 15L

internal fun fonteSerieSimulada(): FonteSerieSimulada = DemonstracaoMedicoes

private object DemonstracaoMedicoes : FonteSerieSimulada {
    private const val SENSOR = 1
    private const val PONTO = 1
    override val catalogo = ChartCatalog(
        locais = listOf(ChartLocalDto(1, "Local de demonstração")),
        setores = listOf(ChartSectorDto(1, "Setor de demonstração", id_local = 1)),
        equipamentos = listOf(ChartEquipmentDto(1, "Equipamento de demonstração", id_setor = 1)),
        pontos = listOf(ChartPointDto(PONTO, null, null, 1)),
        sensores = listOf(ChartSensorDto(SENSOR, PONTO))
    )

    private val gson = Gson()
    private val api = Retrofit.Builder()
        .baseUrl("https://watt-vision.invalid/")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val url = request.url
            if (request.method != "GET" || url.encodedPath != "/api/medicoes/serie-temporal" ||
                url.queryParameter("id_pzem")?.toIntOrNull() != SENSOR
            ) throw IOException("Consulta fora do transporte simulado.")
            val horas = url.queryParameter("horas")?.toIntOrNull()
                ?.takeIf { it in 1..720 } ?: throw IOException("Período simulado inválido.")
            val fim = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val blocoFinal = fim.epochSecond / (MINUTOS_POR_AMOSTRA * 60)
            val quantidade = horas * 4
            val dados = List(quantidade) { i ->
                val bloco = blocoFinal - quantidade + 1 + i
                val tempo = Instant.ofEpochSecond(bloco * MINUTOS_POR_AMOSTRA * 60)
                val onda = sin(bloco * .55) + cos(bloco * 1.2) * .35
                MedicaoDto(
                    id_medicao = bloco,
                    id_pzem = SENSOR,
                    id_ponto_medicao = PONTO,
                    data_hora = tempo.toString(),
                    tensaoRaw = 220.0 + onda * 3.4,
                    correnteRaw = 6.2 + onda * 1.1,
                    potenciaAtivaRaw = 1230.0 + onda * 240.0,
                    energiaAcumuladaRaw = bloco * .16,
                    frequenciaRaw = 60.0 + onda * .09,
                    fatorPotenciaRaw = .94 + onda * .025
                )
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Demonstração")
                .body(gson.toJson(ApiResponse(true, null, dados)).toResponseBody())
                .build()
        }.build())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(WattVisionApiService::class.java)

    override suspend fun consultar(pzem: Int, horas: Int): List<MedicaoDto> {
        val resposta = api.getSerieTemporal(pzem, horas)
        if (!resposta.isSuccessful) throw HttpException(resposta)
        return resposta.body()?.takeIf { it.sucesso }?.dados
            ?: throw IOException("Resposta simulada inválida.")
    }
}
