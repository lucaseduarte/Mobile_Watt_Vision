package com.example.watt_vision_mobile.data.repository

import android.content.Context
import com.example.watt_vision_mobile.data.TokenManager
import com.example.watt_vision_mobile.data.api.*
import com.example.watt_vision_mobile.model.*
import kotlinx.coroutines.*
import retrofit2.Response

class DashboardApiException(val code: Int) : Exception("Consulta indisponível ($code)")

interface DashboardSource {
    suspend fun catalog(): DashboardCatalog
    suspend fun series(id: Int, hours: Int): List<MedicaoDto>
    suspend fun consumption(id: Int, start: String, end: String): ConsumoDto
    suspend fun alarms(): List<AlertaDto>
}

class DashboardRepository(context: Context) : DashboardSource {
    private val tokenManager = TokenManager(context)
    private val auth = ApiClient.createService<AuthApiService>(context)
    private val catalogApi = ApiClient.createService<DashboardApiService>(context)
    private val measurements = ApiClient.createService<WattVisionApiService>(context)

    private fun <T> read(response: Response<ApiResponse<T>>): T {
        if (!response.isSuccessful) throw DashboardApiException(response.code())
        val body = response.body()
        if (body?.sucesso != true || body.dados == null) throw DashboardApiException(502)
        return body.dados
    }

    override suspend fun catalog(): DashboardCatalog = withContext(Dispatchers.IO) {
        // Também no Debug: não usar rotas abertas como alternativa à autenticação.
        if (tokenManager.getToken().isNullOrBlank()) throw DashboardApiException(401)
        val user = read(auth.getMe())
        val company = user.id_empresa ?: throw DashboardApiException(403)
        val locais = read(catalogApi.locais()).filter { it.id != null && it.id_empresa == company }
        if (locais.isEmpty()) return@withContext DashboardCatalog()
        coroutineScope {
            val setoresJob = async { read(catalogApi.setores()) }
            val equipamentosJob = async { read(catalogApi.equipamentos()) }
            val pontosJob = async { read(catalogApi.pontos()) }
            val sensoresJob = async { read(catalogApi.sensores()) }
            val dispositivosJob = async { read(catalogApi.dispositivos()) }
            val raw = DashboardCatalog(locais, setoresJob.await(), equipamentosJob.await(),
                pontosJob.await(), sensoresJob.await(), dispositivosJob.await())
            authorizedCatalog(raw, company)
        }
    }

    override suspend fun series(id: Int, hours: Int) = withContext(Dispatchers.IO) {
        read(measurements.getSerieTemporal(id, hours))
    }
    override suspend fun consumption(id: Int, start: String, end: String) = withContext(Dispatchers.IO) {
        read(measurements.getConsumo(id, start, end))
    }
    override suspend fun alarms() = withContext(Dispatchers.IO) { read(measurements.getAlertasAtivos()) }
}

/** Redução defensiva por IDs; não substitui autorização e isolamento no servidor. */
fun authorizedCatalog(raw: DashboardCatalog, company: Int): DashboardCatalog {
    val locais = raw.locais.filter { it.id != null && it.id_empresa == company }
    val localIds = locais.mapNotNull { it.id }.toSet()
    val setores = raw.setores.filter { it.id != null && it.id_local in localIds }
    val setorIds = setores.mapNotNull { it.id }.toSet()
    val equipamentos = raw.equipamentos.filter { it.id != null && it.id_setor in setorIds }
    val equipamentoIds = equipamentos.mapNotNull { it.id }.toSet()
    val dispositivos = raw.dispositivos.filter { it.id != null && it.id_empresa == company }
    val deviceIds = dispositivos.mapNotNull { it.id }.toSet()
    val pontos = raw.pontos.filter { point ->
        point.id != null && point.id_dispositivo_iot in deviceIds &&
            listOf(point.id_local, point.id_setor, point.id_equipamento).count { it != null } == 1 &&
            (point.id_local in localIds || point.id_setor in setorIds || point.id_equipamento in equipamentoIds)
    }
    val pointIds = pontos.mapNotNull { it.id }.toSet()
    return DashboardCatalog(locais, setores, equipamentos, pontos,
        raw.sensores.filter { it.id != null && it.id_ponto_medicao in pointIds }, dispositivos)
}
