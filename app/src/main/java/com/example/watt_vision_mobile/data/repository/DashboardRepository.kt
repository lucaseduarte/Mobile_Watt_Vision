package com.example.watt_vision_mobile.data.repository

import android.content.Context
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.model.AlertaDto
import com.example.watt_vision_mobile.model.ConsumoDto
import com.example.watt_vision_mobile.model.EquipamentoDto
import com.example.watt_vision_mobile.model.MedicaoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardRepository(context: Context) {
    private val chartSessionApi = ApiClient.createService<com.example.watt_vision_mobile.data.api.AuthApiService>(context)
    private val chartCatalogApi = ApiClient.createService<com.example.watt_vision_mobile.data.api.ChartCatalogApiService>(context)
    private val apiService = ApiClient.createService<WattVisionApiService>(context)

    suspend fun getResumoMedicoes(): Result<MedicaoDto?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getResumoMedicoes()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados)
                } else {
                    Result.failure(Exception("Erro ao buscar resumo de medições"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getUltimaMedicao(): Result<MedicaoDto?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUltimaMedicao()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados)
                } else {
                    Result.failure(Exception("Erro ao buscar última medição"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getSerieTemporal(idPzem: Int, horas: Int = 24): Result<List<MedicaoDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSerieTemporal(idPzem, horas)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados ?: emptyList())
                } else {
                    Result.failure(Exception("Erro ao buscar série temporal"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Fluxo exclusivo do gráfico: conserva HTTP e cancelamento, sem alterar outros cards.
    private suspend fun requireChartSession() {
        val response = chartSessionApi.getMe()
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        check(response.body()?.sucesso == true) { "Não foi possível validar a sessão do gráfico" }
    }

    suspend fun getChartCatalog(): Result<com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog> =
        withContext(Dispatchers.IO) {
            try {
                requireChartSession()
                fun <T> read(response: retrofit2.Response<com.example.watt_vision_mobile.model.ApiResponse<List<T>>>): List<T> {
                    if (!response.isSuccessful) throw retrofit2.HttpException(response)
                    val body = requireNotNull(response.body()) { "Catálogo ausente" }
                    check(body.sucesso) { "Catálogo indisponível" }
                    return requireNotNull(body.dados) { "Lista ausente" }
                }
                Result.success(com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog(
                    locais = read(chartCatalogApi.locais()), setores = read(chartCatalogApi.setores()),
                    equipamentos = read(chartCatalogApi.equipamentos()), pontos = read(chartCatalogApi.pontos()),
                    sensores = read(chartCatalogApi.sensores())
                ))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun getChartSerieTemporal(idPzem: Int, horas: Int): Result<List<MedicaoDto>> =
        withContext(Dispatchers.IO) {
            try {
                requireChartSession()
                val response = apiService.getSerieTemporal(idPzem, horas)
                if (!response.isSuccessful) throw retrofit2.HttpException(response)
                val body = response.body() ?: error("Resposta vazia")
                if (!body.sucesso) error("Consulta não concluída")
                Result.success(requireNotNull(body.dados) { "Série ausente na resposta" })
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

    suspend fun getConsumo(idPzem: Int, inicio: String, fim: String): Result<ConsumoDto?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getConsumo(idPzem, inicio, fim)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados)
                } else {
                    Result.failure(Exception("Erro ao buscar consumo"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getAlertasAtivos(): Result<List<AlertaDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAlertasAtivos()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados ?: emptyList())
                } else {
                    Result.failure(Exception("Erro ao buscar alertas ativos"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getEquipamentos(): Result<List<EquipamentoDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEquipamentos()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()?.dados ?: emptyList())
                } else {
                    Result.failure(Exception("Erro ao buscar equipamentos"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
