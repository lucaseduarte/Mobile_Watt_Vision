package com.example.watt_vision_mobile.ui.screens.conectividade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.ui.screens.operacional.TabFilterState
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DispositivoIoTDto(
    val id: Long,
    val identificador: String,
    val nome: String,
    val status: String?,
    val data_comunicacao: String?,
    val modelo: String?,
    val mqtt: Int?,
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
    val client_id: String? = null
)

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
    val equipamento: String = ""
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
    val vinculo get() = if (equipamento.isBlank()) "—" else "EQUIPAMENTO · $equipamento"

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
    val api = com.example.watt_vision_mobile.data.api.ApiClient
        .createService<com.example.watt_vision_mobile.data.api.WattVisionApiService>(contexto)
    return FonteConectividade {
        val resposta = api.getDispositivos()
        if (!resposta.isSuccessful) throw retrofit2.HttpException(resposta)
        val dispositivos = requireNotNull(resposta.body()?.takeIf { it.sucesso }?.dados)
        ConectividadeUiState(dispositivos = dispositivos.map { dto ->
            DispositivoConectividade(
                DispositivoIoTDto(dto.id, dto.identificador, dto.nome, dto.status,
                    dto.data_comunicacao, dto.modelo, mqtt = null)
            )
        })
    }
}
