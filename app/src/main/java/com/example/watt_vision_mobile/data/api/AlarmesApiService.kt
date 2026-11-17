package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.alarmes.AlarmeDto
import retrofit2.Response
import retrofit2.http.GET

/** Serviço exclusivo consultivo. O GET geral já contém os dados necessários ao modal. */
interface AlarmesApiService {
    @GET("api/alertas")
    suspend fun listar(): Response<ApiResponse<List<AlarmeDto>>>
}
