package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.data.api.*
import com.example.watt_vision_mobile.data.repository.AlarmesRepository
import com.example.watt_vision_mobile.data.repository.AlarmesResult
import com.example.watt_vision_mobile.model.*
import com.example.watt_vision_mobile.model.alarmes.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.http.GET
import java.io.IOException

class AlarmesRepositoryTest {
    private class Alerts : AlarmesApiService {
        var calls = 0
        var error: Exception? = null
        var result: Response<ApiResponse<List<AlarmeDto>>> = Response.success(ApiResponse(true, null, emptyList()))
        var limite: AlarmeLimiteDto? = null
        override suspend fun limite(id: Long) = Response.success(ApiResponse(true, null, limite))
        var pontos = emptyList<AlarmePontoDto>()
        override suspend fun pontos() = Response.success(ApiResponse(true, null, pontos))
        override suspend fun listar(): Response<ApiResponse<List<AlarmeDto>>> {
            calls++
            error?.let { throw it }
            return result
        }
    }
    private class Auth : AuthApiService {
        var calls = 0
        var onRead: () -> Unit = {}
        var result: Response<ApiResponse<UsuarioDto>> = Response.success(ApiResponse(true, null, UsuarioDto(1, 1, null, null, "Gestor", "Ativo")))
        override suspend fun getMe(): Response<ApiResponse<UsuarioDto>> { calls++; onRead(); return result }
        override suspend fun login(request: LoginRequest): Response<LoginResponse> = throw AssertionError("Não deve fazer login automaticamente")
    }
    private fun <T> error(code: Int): Response<T> = Response.error(code, "{}".toResponseBody())

    @Test fun withoutSessionNeverCallsEvenTheOpenAlertsEndpoint() = runBlocking {
        for (token in listOf(null, "", "   ")) {
            val alerts = Alerts(); val auth = Auth()
            assertEquals(AlarmesResult.NoSession, AlarmesRepository(alerts, auth) { token }.load())
            assertEquals(0, auth.calls)
            assertEquals(0, alerts.calls)
        }
    }

    @Test fun invalidOrForbiddenSessionNeverLoadsAlerts() = runBlocking {
        for (code in listOf(401, 403)) {
            val alerts = Alerts(); val auth = Auth().apply { result = error(code) }
            val actual = AlarmesRepository(alerts, auth) { "somente-fake-de-teste" }.load()
            assertEquals(if (code == 401) AlarmesResult.NoSession else AlarmesResult.Forbidden, actual)
            assertEquals(0, alerts.calls)
        }
    }

    @Test fun changedSessionDuringValidationDoesNotExposeAnotherSnapshot() = runBlocking {
        var token = "fake-anterior"
        val auth = Auth().apply { onRead = { token = "fake-novo" } }
        val alerts = Alerts()
        assertEquals(AlarmesResult.NoSession, AlarmesRepository(alerts, auth) { token }.load())
        assertEquals(0, alerts.calls)
    }

    @Test fun validEmptyResponseIsSuccessButMissingPayloadIsUnavailable() = runBlocking {
        val alerts = Alerts(); val repo = AlarmesRepository(alerts, Auth()) { "somente-fake-de-teste" }
        assertEquals(AlarmesResult.Success(emptyList()), repo.load())
        alerts.result = Response.success(ApiResponse(true, null, null))
        assertEquals(AlarmesResult.Unavailable, repo.load())
        alerts.result = Response.success(ApiResponse(false, "Erro", emptyList()))
        assertEquals(AlarmesResult.Unavailable, repo.load())
    }

    @Test fun readResponseMapsRecordsAndPreservesHttpFailures() = runBlocking {
        val alerts = Alerts(); val repo = AlarmesRepository(alerts, Auth()) { "somente-fake-de-teste" }
        alerts.result = Response.success(ApiResponse(true, null, listOf(AlarmeDto(id = 15L, mensagem = "Exemplo"))))
        assertEquals(15L, (repo.load() as AlarmesResult.Success).records.single().id)
        for ((code, expected) in listOf(401 to AlarmesResult.NoSession, 403 to AlarmesResult.Forbidden, 500 to AlarmesResult.Unavailable)) {
            alerts.result = error(code)
            assertEquals(expected, repo.load())
        }
    }

    @Test fun connectionFailureNeverCreatesDemonstrationOrSuccessfulEmptyList() = runBlocking {
        val alerts = Alerts().apply { error = IOException("Rede de teste indisponível") }
        assertEquals(AlarmesResult.NetworkError, AlarmesRepository(alerts, Auth()) { "somente-fake-de-teste" }.load())
        alerts.error = IllegalStateException("Payload inválido")
        assertEquals(AlarmesResult.Unavailable, AlarmesRepository(alerts, Auth()) { "somente-fake-de-teste" }.load())
    }

    @Test fun cancellationPropagatesInsteadOfBecomingAnErrorOrSnapshot() {
        val alerts = Alerts().apply { error = CancellationException("Consulta cancelada") }
        assertThrows(CancellationException::class.java) { runBlocking { AlarmesRepository(alerts, Auth()) { "somente-fake-de-teste" }.load() } }
    }

    @Test fun equipmentNamesUseOnlyUnambiguousDeviceLinks() = runBlocking {
        val alerts = Alerts().apply {
            result = Response.success(ApiResponse(true, null, listOf(
                AlarmeDto(id_dispositivo_iot = 1),
                AlarmeDto(id_dispositivo_iot = 2),
                AlarmeDto(id_dispositivo_iot = 2, nome_equipamento = "Nome do alerta")
            )))
            pontos = listOf(AlarmePontoDto(1, "Motor"), AlarmePontoDto(1, "Motor"),
                AlarmePontoDto(2, "Bomba"), AlarmePontoDto(2, "Ventilador"))
        }
        val result = AlarmesRepository(alerts, Auth()) { "sessao-de-teste" }.load() as AlarmesResult.Success
        assertEquals(listOf("Motor", null, "Nome do alerta"), result.records.map { it.nomeEquipamento })
    }

    @Test fun detailUsesLimitScopeAndResolvesItsCatalogHierarchy() = runBlocking {
        val alerts = Alerts().apply {
            pontos = listOf(AlarmePontoDto(1, "Motor", id_equipamento = 3))
        }
        val repo = AlarmesRepository(alerts, Auth()) { "sessao-de-teste" }
        val alarm = AlarmeMapper.map(AlarmeDto(id_limite = 4, id_dispositivo_iot = 1))

        alerts.limite = AlarmeLimiteDto(null, null, 3)
        assertEquals(AlarmeEntidade(AlarmeTipoEntidade.EQUIPAMENTO, 3, "Motor", "Produção", "Unidade"),
            repo.entidade(alarm, Catalogo()))
        alerts.limite = AlarmeLimiteDto(null, 2, null)
        assertEquals(AlarmeEntidade(AlarmeTipoEntidade.SETOR, 2, "Produção", local = "Unidade"),
            repo.entidade(alarm, Catalogo()))
        alerts.limite = AlarmeLimiteDto(1, null, null)
        assertEquals(AlarmeEntidade(AlarmeTipoEntidade.LOCAL, 1, "Unidade"),
            repo.entidade(alarm, Catalogo()))
        alerts.limite = null
        assertNull(repo.entidade(alarm, Catalogo()))
    }

    @Test fun communicationDetailRequiresOneUnambiguousOperationalTarget() = runBlocking {
        val alerts = Alerts().apply {
            pontos = listOf(AlarmePontoDto(1, "Motor", id_equipamento = 3))
        }
        val repo = AlarmesRepository(alerts, Auth()) { "sessao-de-teste" }
        val alarm = AlarmeMapper.map(AlarmeDto(id_dispositivo_iot = 1))
        assertEquals(AlarmeEntidade(AlarmeTipoEntidade.EQUIPAMENTO, 3, "Motor", "Produção", "Unidade"),
            repo.entidade(alarm, Catalogo()))
        alerts.pontos += AlarmePontoDto(1, null, id_setor = 2)
        assertNull(repo.entidade(alarm, Catalogo()))
    }

    private class Catalogo : ChartCatalogApiService {
        override suspend fun locais() = Response.success(ApiResponse(true, null, listOf(ChartLocalDto(1, "Unidade"))))
        override suspend fun setores() = Response.success(ApiResponse(true, null, listOf(ChartSectorDto(2, "Produção", 1))))
        override suspend fun equipamentos() = Response.success(ApiResponse(true, null, listOf(ChartEquipmentDto(3, "Motor", 2))))
        override suspend fun pontos(): Response<ApiResponse<List<ChartPointDto>>> = error("Consulta inesperada")
        override suspend fun sensores(): Response<ApiResponse<List<ChartSensorDto>>> = error("Consulta inesperada")
    }

    @Test fun serviceDeclaresOnlyReadEndpoints() {
        val methods = AlarmesApiService::class.java.declaredMethods.filterNot { it.isSynthetic }
        assertEquals(setOf("api/alertas", "api/pontos-medicao", "api/limites/{id}"),
            methods.map { requireNotNull(it.getAnnotation(GET::class.java)).value }.toSet())
    }
}
