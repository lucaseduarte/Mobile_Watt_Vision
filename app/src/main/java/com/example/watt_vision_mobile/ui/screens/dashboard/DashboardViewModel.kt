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
import com.example.watt_vision_mobile.data.api.integracaoLocal
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

    init { if (fonte == null && integracaoLocal) loadDashboardData() }

    fun alternarDemonstracao() {
        val fonte = fonte ?: return
        carga?.cancel()
        val ativa = !_uiState.value.simulated
        _uiState.update {
            it.copy(
                simulated = ativa,
                isLoading = false,
                lastUpdatedText = if (ativa) "Dados simulados" else "Consultas de negócio suspensas",
                chart = ChartState(ChartStatus.NO_SESSION),
                chartCatalog = if (ativa) fonte.catalogo else null,
                chartPoints = emptyList(),
                summaryCards = emptyList()
            )
        }
        if (ativa) loadDashboardData()
    }

    fun loadDashboardData() {
        val estado = _uiState.value
        if (fonte == null && !integracaoLocal || fonte != null && !estado.simulated) return
        carga?.cancel()
        val filtros = estado.filters
        _uiState.update { it.copy(
            isLoading = true,
            lastUpdatedText = "Carregando medições",
            chart = ChartState(ChartStatus.LOADING),
            chartPoints = emptyList(),
            summaryCards = emptyList()
        ) }
        carga = viewModelScope.launch {
            val momento = agora()
            val periodo = ChartLogic.period(filtros.period, momento.toLocalDateTime())
            try {
                val fonte = fonte ?: carregarFonteBackend(getApplication())
                _uiState.update { it.copy(chartCatalog = fonte.catalogo) }
                val (pzem, status) = fonte.catalogo.resolve(filtros)
                if (periodo == null || pzem == null) {
                    concluir(ChartState(if (periodo == null) ChartStatus.UNSUPPORTED_PERIOD else status ?: ChartStatus.EMPTY))
                    return@launch
                }
                val linhas = fonte.consultar(pzem, periodo.hours)
                ensureActive()
                check(linhas.all { it.id_pzem == pzem })
                val escopo = fonte.catalogo.scopedPointIds(filtros)
                val pontos = ChartLogic.map(linhas, filtros.metric, periodo, momento.zone, escopo)
                val ultima = linhas.mapNotNull { linha ->
                    if (escopo != null && linha.id_ponto_medicao !in escopo) return@mapNotNull null
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
                    cards(ultima)
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (erro: Exception) {
                ensureActive()
                concluir(ChartState(if (erro is HttpException && erro.code() == 403) ChartStatus.FORBIDDEN else ChartStatus.ERROR))
            }
        }
    }

    private fun concluir(chart: ChartState, pontos: List<MedicaoPoint> = emptyList(), cards: List<StatCardData> = emptyList()) {
        _uiState.update { it.copy(
            isLoading = false,
            lastUpdatedText = when (chart.status) {
                ChartStatus.ERROR -> "Falha na consulta"
                ChartStatus.EMPTY -> "Sem leituras no período"
                ChartStatus.READY -> if (it.simulated) "Dados simulados" else "Dados do Backend"
                else -> "Medições"
            },
            chart = chart,
            chartPoints = pontos,
            summaryCards = cards
        ) }
    }

    private fun cards(ultima: MedicaoDto?): List<StatCardData> {
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
