package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.alarmes.AlarmeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

data class AlarmePontoDto(
    val id_dispositivo_iot: Long?,
    val nome_equipamento: String?,
    val id_local: Int? = null,
    val id_setor: Int? = null,
    val id_equipamento: Int? = null
)

data class AlarmeLimiteDto(val id_local: Int?, val id_setor: Int?, val id_equipamento: Int?)

/** Consultas usadas na lista e na identificação da entidade vinculada. */
interface AlarmesApiService {
    @GET("api/alertas")
    suspend fun listar(): Response<ApiResponse<List<AlarmeDto>>>

    @GET("api/limites/{id}")
    suspend fun limite(@Path("id") id: Long): Response<ApiResponse<AlarmeLimiteDto>>

    @GET("api/pontos-medicao")
    suspend fun pontos(): Response<ApiResponse<List<AlarmePontoDto>>>
}
