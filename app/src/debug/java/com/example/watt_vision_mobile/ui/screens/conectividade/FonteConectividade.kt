package com.example.watt_vision_mobile.ui.screens.conectividade

import com.example.watt_vision_mobile.data.api.lerDemonstracao
import com.example.watt_vision_mobile.data.api.transporteDemonstracao
import com.example.watt_vision_mobile.model.ApiResponse
import java.time.LocalDateTime
import retrofit2.Response
import retrofit2.http.GET

private interface DispositivosDemoApi {
    @GET("api/dispositivos-iot")
    suspend fun listar(): Response<ApiResponse<List<DispositivoIoTDto>>>
}

internal fun fonteConectividade(agora: LocalDateTime = LocalDateTime.now()): FonteConectividade {
    val referencia = agora.withSecond(0).withNano(0)
    val cadastros = listOf(
        cadastroIoTExemplo(referencia.minusMinutes(1).toString()),
        DispositivoIoTDto(6, "ESP32-006", "ESP32 Usinagem 06", "offline", referencia.minusMinutes(4).toString(), "ESP32-DevKitC", 1)
    )
    val eventos = listOf(
        EventoConectividade(1, 1, "Desconectado", referencia.minusDays(1)),
        EventoConectividade(2, 1, "Reconectado", referencia.minusMinutes(8)),
        EventoConectividade(3, 6, "Desconectado", referencia.minusMinutes(3))
    )
    val api = transporteDemonstracao { path ->
        if (path == "/api/dispositivos-iot") ApiResponse(true, null, cadastros) else null
    }.create(DispositivosDemoApi::class.java)
    return FonteConectividade {
        val dispositivos = lerDemonstracao(api.listar()).map { cadastro ->
            if (cadastro.id == 1L) {
                DispositivoConectividade(cadastro, "Planta Industrial A", "Usinagem e Motores", "Motor Elétrico WEG 01")
            } else {
                DispositivoConectividade(cadastro)
            }
        }
        ConectividadeUiState(dispositivos, eventos)
    }
}
