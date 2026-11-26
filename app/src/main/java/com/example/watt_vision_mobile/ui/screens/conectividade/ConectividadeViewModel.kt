package com.example.watt_vision_mobile.ui.screens.conectividade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.ChartCatalogApiService
import com.example.watt_vision_mobile.data.api.ChartPointDto
import com.example.watt_vision_mobile.data.api.DispositivoConsultaDto
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.model.ApiResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import retrofit2.Response
import com.example.watt_vision_mobile.ui.screens.operacional.TabFilterState
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal typealias DispositivoIoTDto = DispositivoConsultaDto

internal fun cadastroIoTExemplo(ultimoContato: String) = DispositivoIoTDto(
    1, "ESP32-001", "ESP32 Usinagem 01", "online", ultimoContato, "ESP32-DevKitC", 1,
    numero_serie = "WV-ESP-001", versao_firmware = "v1.3.0", tx_interval_sec = 5,
    mac_address = "24:6F:28:A1:0B:01", data_instalacao = "2026-03-11",
    ssid = "WattVision-IoT", ip = "192.168.1.42",
    broker = "broker.hivemq.com", port = 1883, topic = "wattvision/medicoes", client_id = "wattvision-esp32-"
)

internal data class DispositivoConectividade(
    val cadastro: DispositivoIoTDto,
    val local: String = "",
    val setor: String = "",
    val equipamento: String = "",
    val pontosMedicao: List<String> = emptyList(),
    val vinculoDoPonto: String? = null
) {
    val id get() = cadastro.id
    val codigo get() = cadastro.identificador
    val nome get() = cadastro.nome
    val status get() = when (cadastro.status) {
        "online" -> "Online"
        "offline" -> "Offline"
        "instavel" -> "Instável"
        "disponivel" -> "Disponível"
        else -> "Indisponível"
    }
    val tipo get() = cadastro.modelo ?: "—"
    val comunicacao get() = if (cadastro.mqtt == 1) "MQTT" else "—"
    val ultimoContato get() = cadastro.data_comunicacao ?: "—"
    val vinculo get() = vinculoDoPonto ?: if (equipamento.isBlank()) "—" else "EQUIPAMENTO · $equipamento"

    fun correspondeAoLocalESetor(filtros: TabFilterState) =
        (filtros.locationFilter == "Todos" || filtros.locationFilter == local) &&
            (filtros.sectorFilter == "Todos" || filtros.sectorFilter == setor)

    fun corresponde(filtros: TabFilterState): Boolean {
        val termo = filtros.searchQuery.trim()
        return (codigo.contains(termo, ignoreCase = true) || nome.contains(termo, ignoreCase = true)) &&
            (filtros.statusFilter == "Todos" || filtros.statusFilter == status) &&
            correspondeAoLocalESetor(filtros) &&
            (filtros.equipmentFilter == "Todos" || filtros.equipmentFilter == equipamento)
    }
}

internal data class EventoConectividade(
    val id: Long,
    val dispositivoId: Long,
    val evento: String,
    val momento: LocalDateTime
)

internal data class ConectividadeUiState(
    val dispositivos: List<DispositivoConectividade> = emptyList(),
    val eventos: List<EventoConectividade> = emptyList(),
    val carregando: Boolean = true,
    val erro: Boolean = false
)

internal fun interface FonteConectividade {
    suspend fun carregar(): ConectividadeUiState
}

internal class ConectividadeViewModel(
    private val fonte: FonteConectividade = fonteConectividade()
) : ViewModel() {
    private val _estado = MutableStateFlow(ConectividadeUiState())
    val estado = _estado.asStateFlow()
    private var carga: Job? = null

    init { atualizar() }

    fun atualizarAutomaticamente() {
        if (carga?.isActive != true) atualizar()
    }

    fun atualizar() {
        carga?.cancel()
        _estado.update { it.copy(carregando = true, erro = false) }
        carga = viewModelScope.launch {
            try {
                val dados = fonte.carregar()
                ensureActive()
                val dispositivos = dados.dispositivos.distinctBy { it.id }
                val ids = dispositivos.map { it.id }.toSet()
                _estado.value = dados.copy(
                    dispositivos = dispositivos,
                    eventos = dados.eventos.filter { it.dispositivoId in ids }.distinctBy { it.id }
                        .sortedWith(compareByDescending<EventoConectividade> { it.momento }.thenByDescending { it.id }),
                    carregando = false,
                    erro = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ensureActive()
                _estado.update { it.copy(carregando = false, erro = true) }
            }
        }
    }

    override fun onCleared() {
        carga?.cancel()
        _estado.value = ConectividadeUiState(carregando = false)
        super.onCleared()
    }
}

internal fun fonteConectividadeBackend(contexto: android.content.Context): FonteConectividade {
    val api = ApiClient.createService<WattVisionApiService>(contexto)
    val catalogo = ApiClient.createService<ChartCatalogApiService>(contexto)
    return FonteConectividade {
        coroutineScope {
            val consultaDispositivos = async { api.getDispositivos().dadosConectividade() }
            val consultaPontos = async { consultarVinculos(catalogo::pontos) }
            val consultaLocais = async { consultarVinculos(catalogo::locais) }
            val consultaSetores = async { consultarVinculos(catalogo::setores) }
            val consultaEquipamentos = async { consultarVinculos(catalogo::equipamentos) }

            val pontosPorDispositivo = consultaPontos.await().groupBy { it.id_dispositivo_iot }
            val locais = consultaLocais.await().associateBy { it.id }
            val setores = consultaSetores.await().associateBy { it.id }
            val equipamentos = consultaEquipamentos.await().associateBy { it.id }
            val dispositivos = consultaDispositivos.await().map { dto ->
                val pontos = pontosPorDispositivo[dto.id].orEmpty()
                    .sortedWith(compareByDescending<ChartPointDto> { it.status == "Ativo" }.thenBy { it.id })
                val ponto = pontos.firstOrNull()
                val equipamento = ponto?.id_equipamento?.let(equipamentos::get)
                val setor = (ponto?.id_setor ?: equipamento?.id_setor)?.let(setores::get)
                val local = (ponto?.id_local ?: setor?.id_local)?.let(locais::get)
                DispositivoConectividade(
                    cadastro = dto,
                    local = local?.nome.orEmpty(),
                    setor = setor?.nome.orEmpty(),
                    equipamento = equipamento?.nome.orEmpty(),
                    pontosMedicao = pontos.mapNotNull { it.nome }.distinct(),
                    vinculoDoPonto = when {
                        ponto?.id_equipamento != null -> equipamento?.nome?.let { "EQUIPAMENTO · $it" }
                        ponto?.id_setor != null -> setor?.nome?.let { "SETOR · $it" }
                        ponto?.id_local != null -> local?.nome?.let { "LOCAL · $it" }
                        else -> null
                    }
                )
            }
            ConectividadeUiState(dispositivos = dispositivos)
        }
    }
}

private suspend fun <T> consultarVinculos(
    consultar: suspend () -> Response<ApiResponse<List<T>>>
): List<T> = try {
    val resposta = consultar()
    resposta.body()?.takeIf { resposta.isSuccessful && it.sucesso }?.dados.orEmpty()
} catch (_: IOException) {
    emptyList()
}

private fun <T> Response<ApiResponse<T>>.dadosConectividade(): T {
    if (!isSuccessful) throw HttpException(this)
    return requireNotNull(body()?.takeIf { it.sucesso }?.dados)
}
