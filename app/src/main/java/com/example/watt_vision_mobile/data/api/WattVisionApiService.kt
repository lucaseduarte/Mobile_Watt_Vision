package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.model.AlertaDto
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.ConsumoDto
import com.example.watt_vision_mobile.model.EquipamentoDto
import com.example.watt_vision_mobile.model.MedicaoDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

data class DispositivoConsultaDto(
    val id: Long,
    val identificador: String,
    val nome: String,
    val status: String?,
    val data_comunicacao: String?,
    val modelo: String?,
    val mqtt: Int? = null,
    val numero_serie: String? = null,
    val versao_firmware: String? = null,
    val tx_interval_sec: Int? = null,
    val mac_address: String? = null,
    val data_instalacao: String? = null,
    val ssid: String? = null,
    val ip: String? = null,
    val broker: String? = null,
    val port: Int? = null,
    val topic: String? = null,
    val client_id: String? = null,
    val wifi: Int? = null
)

interface WattVisionApiService {
    @GET("api/medicoes/resumo")
    suspend fun getResumoMedicoes(): Response<ApiResponse<MedicaoDto>>

    @GET("api/medicoes/ultima")
    suspend fun getUltimaMedicao(): Response<ApiResponse<MedicaoDto>>

    @GET("api/medicoes/serie-temporal")
    suspend fun getSerieTemporal(
        @Query("id_pzem") idPzem: Int,
        @Query("horas") horas: Int? = 24
    ): Response<ApiResponse<List<MedicaoDto>>>

    @GET("api/medicoes/consumo")
    suspend fun getConsumo(
        @Query("id_pzem") idPzem: Int,
        @Query("inicio") inicio: String,
        @Query("fim") fim: String
    ): Response<ApiResponse<ConsumoDto>>

    @GET("api/equipamentos")
    suspend fun getEquipamentos(): Response<ApiResponse<List<EquipamentoDto>>>

    @GET("api/equipamentos/{id}")
    suspend fun getEquipamento(@Path("id") id: Int): Response<ApiResponse<EquipamentoDto>>

    @GET("api/dispositivos-iot")
    suspend fun getDispositivos(): Response<ApiResponse<List<DispositivoConsultaDto>>>

    @GET("api/alertas")
    suspend fun getAlertas(): Response<ApiResponse<List<AlertaDto>>>

    @GET("api/alertas/ativos")
    suspend fun getAlertasAtivos(): Response<ApiResponse<List<AlertaDto>>>
}
