package com.example.watt_vision_mobile.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.model.LIMITE_REGISTROS_SERIE_TEMPORAL
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.medicoes.FonteSerieSimulada
import com.example.watt_vision_mobile.ui.screens.medicoes.carregarFonteBackend
import com.example.watt_vision_mobile.ui.screens.medicoes.ZONA_SERIE_REAL
import com.example.watt_vision_mobile.data.api.AlarmesApiService
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.repository.AlarmesRepository
import com.example.watt_vision_mobile.data.repository.AlarmesResult
import com.example.watt_vision_mobile.data.repository.AuthRepository
import com.example.watt_vision_mobile.model.alarmes.AlarmeSeveridade
import com.example.watt_vision_mobile.model.alarmes.AlarmeStatus
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.data.api.integracaoLocal
import com.example.watt_vision_mobile.model.ApiResponse
import retrofit2.Response
import kotlinx.coroutines.async
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.Locale

class DashboardViewModel internal constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val fonte: FonteSerieSimulada?,
    private val agora: () -> ZonedDateTime
) : AndroidViewModel(application) {
    constructor(application: Application, savedStateHandle: SavedStateHandle) :
        this(application, savedStateHandle, null, { ZonedDateTime.now(ZONA_SERIE_REAL) })

    private val filtros = savedStateHandle.get<DashboardFilterState>(SAVED_FILTERS) ?: DashboardFilterState()
    private val _uiState = MutableStateFlow(DashboardUiState(
        filters = filtros,
        selectedChartPeriod = filtros.period,
        chart = ChartState(ChartStatus.NO_SESSION)
    ))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    val demonstracaoDisponivel = fonte != null

    private var carga: Job? = null
    private val api by lazy { ApiClient.createService<WattVisionApiService>(getApplication()) }
    private val alarmesRepository by lazy {
        val app = getApplication<Application>()
        val sessao = AuthRepository.getInstance(app)
        AlarmesRepository(
            ApiClient.createService<AlarmesApiService>(app),
            ApiClient.createService<AuthApiService>(app)
        ) { sessao.currentCredential()?.token }
    }

    init { if (fonte == null && integracaoLocal) loadDashboardData() }

    fun alternarDemonstracao() {
        val fonte = fonte ?: return
        carga?.cancel()
        val ativa = !_uiState.value.simulated
        _uiState.update {
            it.copy(
                simulated = ativa,
                atualizadoEm = null,
                isLoading = false,
                chart = ChartState(ChartStatus.NO_SESSION),
                chartCatalog = if (ativa) fonte.catalogo else null,
                chartPoints = emptyList(),
                summaryCards = emptyList()
            )
        }
        if (ativa) loadDashboardData()
    }

    internal suspend fun atualizarAutomaticamente() {
        if (!integracaoLocal || fonte != null) return
        val consulta = carga?.takeIf { it.isActive } ?: carregarDashboard(preservarLeituras = true)
        consulta?.join()
    }

    fun loadDashboardData() {
        carregarDashboard()
    }

    private fun carregarDashboard(preservarLeituras: Boolean = false): Job? {
        val estado = _uiState.value
        if (fonte == null && !integracaoLocal || fonte != null && !estado.simulated) return null
        carga?.cancel()
        val filtros = estado.filters
        if (!preservarLeituras) _uiState.update { it.copy(
            isLoading = true,
            chart = ChartState(ChartStatus.LOADING),
            chartPoints = emptyList(),
            summaryCards = if (fonte == null) cardsDashboard(null, null) else emptyList(),
            indicadores = emptyList()
        ) }
        carga = viewModelScope.launch {
            val momento = agora()
            val periodo = ChartLogic.period(filtros.period, momento.toLocalDateTime())
            val dispositivos = if (fonte == null) async {
                consultarOpcional { api.getDispositivos() }?.map { it.status }
            } else null
            val alarmes = if (fonte == null) async { carregarAlarmes(momento) } else null
            try {
                val fonte = fonte ?: carregarFonteBackend(getApplication())
                _uiState.update { it.copy(chartCatalog = fonte.catalogo) }
                val (pzem, status) = fonte.catalogo.resolve(filtros)
                if (periodo == null || pzem == null) {
                    concluir(
                        ChartState(
                            if (periodo == null) ChartStatus.UNSUPPORTED_PERIOD else status ?: ChartStatus.EMPTY,
                            updatedAt = momento.toLocalDateTime()
                        ),
                        cards = if (dispositivos == null) emptyList() else cardsDashboard(null, dispositivos.await()),
                        alarmes = alarmes?.await()
                    )
                    return@launch
                }
                val anterior = periodoAnterior(periodo, filtros.period, momento.toLocalDateTime())
                val janelaMovel = filtros.period == "6 horas"
                val consumo = if (dispositivos != null) async {
                    val inicio = periodo.start.toLocalDate().toString()
                    val fim = if (janelaMovel) periodo.end.toLocalDate().toString() else inicio
                    consultarOpcional { api.getConsumo(pzem, inicio, fim) }
                } else null
                val linhas = fonte.consultar(pzem, if (dispositivos == null) periodo.hours else anterior.hours)
                ensureActive()
                check(linhas.all { it.id_pzem == pzem })
                val escopo = fonte.catalogo.scopedPointIds(filtros)
                val pontos = ChartLogic.map(linhas, filtros.metric, periodo, momento.zone, escopo)
                val linhasDoEscopo = linhas.filter { escopo == null || it.id_ponto_medicao in escopo }
                val ultima = linhasDoEscopo.mapNotNull { linha ->
                    val tempo = ChartLogic.timestamp(linha.data_hora, momento.zone)
                        ?.takeIf(periodo::contains) ?: return@mapNotNull null
                    tempo to linha
                }.maxWithOrNull(compareBy({ it.first }, { it.second.id_medicao }))?.second
                concluir(
                    ChartState(
                        status = if (pontos.any { it.valor != null }) ChartStatus.READY else ChartStatus.EMPTY,
                        pzemId = pzem,
                        truncated = linhas.size >= LIMITE_REGISTROS_SERIE_TEMPORAL,
                        updatedAt = momento.toLocalDateTime()
                    ),
                    pontos,
                    if (dispositivos == null) cardsSimulados(ultima) else cardsDashboard(ultima, dispositivos.await()),
                    if (dispositivos == null) emptyList() else montarIndicadores(
                        linhasDoEscopo.takeIf { linhas.size < LIMITE_REGISTROS_SERIE_TEMPORAL }.orEmpty(),
                        periodo, anterior, filtros.metric, fonte.catalogo, momento.zone, consumo?.await(),
                        calcularConsumoNaJanela = janelaMovel
                    ),
                    alarmes?.await()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (erro: Exception) {
                ensureActive()
                val proibido = erro is HttpException && erro.code() in listOf(401, 403)
                if (preservarLeituras && !proibido && _uiState.value.chart.status == ChartStatus.READY) {
                    _uiState.update { it.copy(chart = it.chart.copy(stale = true)) }
                } else {
                    concluir(
                        ChartState(if (proibido) ChartStatus.FORBIDDEN else ChartStatus.ERROR),
                        cards = if (dispositivos == null) emptyList() else cardsDashboard(
                            null, if (proibido) null else dispositivos.await()
                        ),
                        alarmes = if (proibido) null else alarmes?.await()
                    )
                }
            }
        }
        return carga
    }

    private fun concluir(
        chart: ChartState,
        pontos: List<MedicaoPoint> = emptyList(),
        cards: List<StatCardData> = emptyList(),
        indicadores: List<IndicadorDashboard> = emptyList(),
        alarmes: List<AlarmData>? = null
    ) {
        _uiState.update { it.copy(
            isLoading = false,
            chart = chart,
            atualizadoEm = if (chart.status == ChartStatus.FORBIDDEN) null else chart.updatedAt ?: it.atualizadoEm,
            chartPoints = pontos,
            summaryCards = cards,
            indicadores = indicadores,
            alarms = alarmes
        ) }
    }

    private suspend fun carregarAlarmes(momento: ZonedDateTime): List<AlarmData>? {
        val resultado = alarmesRepository.load() as? AlarmesResult.Success ?: return null
        return resultado.records.filter { it.status == AlarmeStatus.ATIVO }
            .sortedByDescending { ChartLogic.timestamp(it.dataHora, momento.zone) }
            .map { alarme ->
                AlarmData(
                    alarme.titulo,
                    alarme.nomeEquipamento ?: alarme.categoriaLabel,
                    ChartLogic.timestamp(alarme.dataHora, momento.zone)?.let(::horarioDashboard) ?: "—",
                    when (alarme.severidade) {
                        AlarmeSeveridade.CRITICO -> AlarmType.CRITICAL
                        AlarmeSeveridade.ATENCAO -> AlarmType.WARNING
                        else -> AlarmType.INFO
                    }
                )
            }
    }

    private suspend fun <T> consultarOpcional(chamada: suspend () -> Response<ApiResponse<T>>): T? =
        try {
            chamada().takeIf { it.isSuccessful }?.body()?.takeIf { it.sucesso }?.dados
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    private fun cardsSimulados(ultima: MedicaoDto?): List<StatCardData> {
        fun formatar(valor: Double?, formato: String) =
            valor?.let { String.format(Locale.getDefault(), formato, it) } ?: "—"

        return listOf(
            StatCardData("Energia acumulada", formatar(ultima?.let(ChartMetric.ENERGY::value), "%.2f kWh"), "Contador do medidor"),
            StatCardData("Potência Ativa", formatar(ultima?.let(ChartMetric.POWER::value)?.div(1_000), "%.2f kW"),
                "Potência instantânea", tone = StatTone.YELLOW, iconName = "zap"),
            StatCardData("Tensão Atual", formatar(ultima?.let(ChartMetric.VOLTAGE::value), "%.1f V"), "Tensão da rede", iconName = "flash"),
            StatCardData("Corrente", formatar(ultima?.let(ChartMetric.CURRENT::value), "%.2f A"), "Última leitura"),
            StatCardData("Frequência", formatar(ultima?.let(ChartMetric.FREQUENCY::value), "%.1f Hz"), "Frequência da rede", iconName = "activity")
        )
    }

    fun applyFilters(newFilters: DashboardFilterState) {
        if (_uiState.value.filters == newFilters) return
        savedStateHandle[SAVED_FILTERS] = newFilters
        _uiState.update { it.copy(filters = newFilters, selectedChartPeriod = newFilters.period) }
        loadDashboardData()
    }

    fun removeSpecificFilter(filterKey: String) {
        val current = _uiState.value.filters
        applyFilters(when (filterKey) {
            "location" -> current.copy(location = "Todos", sector = "Todos", equipment = "Todos", locationId = null, sectorId = null, equipmentId = null)
            "sector" -> current.copy(sector = "Todos", equipment = "Todos", sectorId = null, equipmentId = null)
            "equipment" -> current.copy(equipment = "Todos", equipmentId = null)
            else -> current
        })
    }

    fun onMetricSelected(metric: String) {
        applyFilters(_uiState.value.filters.copy(metric = ChartMetric.from(metric)))
    }

    fun onChartPeriodSelected(period: String) {
        applyFilters(_uiState.value.filters.copy(period = period))
    }

    fun retryChart() = loadDashboardData()

    fun setComposicaoModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isComposicaoModalOpen = visible) }
    }

    override fun onCleared() {
        carga?.cancel()
        _uiState.value = DashboardUiState()
        super.onCleared()
    }

    private companion object {
        const val SAVED_FILTERS = "saved_filters_state"
    }
}
