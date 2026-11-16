package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.model.*
import retrofit2.Response
import retrofit2.http.GET

/** Apenas catálogos necessários à seleção e ao resumo do Dashboard. */
interface DashboardApiService {
    @GET("api/locais") suspend fun locais(): Response<ApiResponse<List<LocalConsultaDto>>>
    @GET("api/setores") suspend fun setores(): Response<ApiResponse<List<SetorConsultaDto>>>
    @GET("api/equipamentos") suspend fun equipamentos(): Response<ApiResponse<List<EquipamentoDto>>>
    @GET("api/pontos-medicao") suspend fun pontos(): Response<ApiResponse<List<PontoConsultaDto>>>
    @GET("api/pzems") suspend fun sensores(): Response<ApiResponse<List<PzemConsultaDto>>>
    @GET("api/dispositivos-iot") suspend fun dispositivos(): Response<ApiResponse<List<DispositivoConsultaDto>>>
}
