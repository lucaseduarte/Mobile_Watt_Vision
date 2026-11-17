package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

// Apenas campos públicos necessários para resolver entidade -> ponto -> medidor.
data class ChartLocalDto(val id: Int?, val nome: String?)
data class ChartSectorDto(val id: Int?, val nome: String?, val id_local: Int?)
data class ChartEquipmentDto(val id: Int?, val nome: String?, val id_setor: Int?)
data class ChartPointDto(val id: Int?, val id_local: Int?, val id_setor: Int?, val id_equipamento: Int?)
data class ChartSensorDto(val id: Int?, val id_ponto_medicao: Int?)

interface ChartCatalogApiService {
    @GET("api/locais") suspend fun locais(): Response<ApiResponse<List<ChartLocalDto>>>
    @GET("api/setores") suspend fun setores(): Response<ApiResponse<List<ChartSectorDto>>>
    @GET("api/equipamentos") suspend fun equipamentos(): Response<ApiResponse<List<ChartEquipmentDto>>>
    @GET("api/pontos-medicao") suspend fun pontos(): Response<ApiResponse<List<ChartPointDto>>>
    @GET("api/pzems") suspend fun sensores(): Response<ApiResponse<List<ChartSensorDto>>>
}
