package com.example.watt_vision_mobile.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.data.repository.DashboardRepository
import com.example.watt_vision_mobile.model.MedicaoDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val dashboardRepository = DashboardRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var realSerieTemporalData: List<MedicaoDto> = emptyList()
    private var chartJob: Job? = null
    private val chartTokens = com.example.watt_vision_mobile.data.TokenManager(application)
    private val chartZone = java.time.ZoneId.systemDefault()
    private var chartPeriod: ChartPeriod? = null
    private var catalogSession: String? = null
    private var catalogLoadedAt: Long = 0L

    init {
        // Restaura os filtros salvos no SavedStateHandle (process death / rotation persistence)
        val savedFilters = savedStateHandle.get<DashboardFilterState>(SAVED_FILTERS) ?: DashboardFilterState()
        _uiState.update { it.copy(filters = savedFilters, selectedChartPeriod = savedFilters.period) }
        startLiveUpdates()
        restartChart()
    }

    fun startLiveUpdates(intervalMillis: Long = 3000L) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var isFirstFetch = true
            while (isActive) {
                fetchDashboardData(showLoading = isFirstFetch)
                isFirstFetch = false
                delay(intervalMillis)
            }
        }
    }

    fun loadDashboardData() {
        startLiveUpdates()
        retryChart()
    }

    fun applyFilters(newFilters: DashboardFilterState) {
        savedStateHandle[SAVED_FILTERS] = newFilters
        _uiState.update { it.copy(filters = newFilters, selectedChartPeriod = newFilters.period) }
        restartChart()
        startLiveUpdates()
    }

    fun removeSpecificFilter(filterKey: String) {
        val current = _uiState.value.filters
        val updated = when (filterKey) {
            "period" -> current.copy(period = "Hoje")
            "location" -> current.copy(location = "Todos", sector = "Todos", equipment = "Todos", locationId = null, sectorId = null, equipmentId = null)
            "sector" -> current.copy(sector = "Todos", equipment = "Todos", sectorId = null, equipmentId = null)
            "equipment" -> current.copy(equipment = "Todos", equipmentId = null)
            else -> current
        }
        applyFilters(updated)
    }

    private suspend fun fetchDashboardData(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true) }
        }

        try {
            var cards = emptyList<StatCardData>()
            var alarms = emptyList<AlarmData>()
            var equipamentosList = emptyList<ComposicaoItem>()

            val alertasResult = dashboardRepository.getAlertasAtivos()
            val equipamentosResult = dashboardRepository.getEquipamentos()
            val ultimaMedicaoResult = dashboardRepository.getUltimaMedicao()
            val resumoResult = dashboardRepository.getResumoMedicoes()

            val ultimaMedicao = ultimaMedicaoResult.getOrNull() ?: resumoResult.getOrNull()

            // A série do gráfico possui seleção explícita e fluxo independente.
            val idPzem = ultimaMedicao?.id_pzem

            val hasRealData = alertasResult.isSuccess || equipamentosResult.isSuccess || ultimaMedicao != null

            if (hasRealData) {
                val apiAlertas = alertasResult.getOrNull() ?: emptyList()
                val apiEquipamentos = equipamentosResult.getOrNull() ?: emptyList()

                if (apiAlertas.isNotEmpty()) {
                    alarms = apiAlertas.map { alt ->
                        AlarmData(
                            title = alt.mensagem ?: alt.tipo ?: "Alerta",
                            equipment = "Equipamento ID: ${alt.id_equipamento ?: "N/D"}",
                            time = alt.data_hora ?: "Recentemente",
                            type = when (alt.severidade?.lowercase()) {
                                "critico", "critical" -> AlarmType.CRITICAL
                                "atencao", "warning" -> AlarmType.WARNING
                                else -> AlarmType.INFO
                            }
                        )
                    }
                }

                if (apiEquipamentos.isNotEmpty()) {
                    equipamentosList = apiEquipamentos.mapIndexed { index, eq ->
                        ComposicaoItem(
                            id = (eq.id ?: (index + 1)).toString(),
                            entidade = eq.nome ?: "Equipamento ${eq.id}",
                            valor = 0.0,
                            origem = eq.status ?: "Ativo"
                        )
                    }
                }

                val tensaoStr = ultimaMedicao?.tensao?.let { String.format(Locale.getDefault(), "%.1f V", it) } ?: "N/A"
                val correnteStr = ultimaMedicao?.corrente?.let { String.format(Locale.getDefault(), "%.2f A", it) } ?: "N/A"
                val potenciaStr = ultimaMedicao?.potencia_ativa?.let { String.format(Locale.getDefault(), "%.2f kW", it) } ?: "N/A"
                val consumoTotalStr = ultimaMedicao?.energia_acumulada?.let { String.format(Locale.getDefault(), "%.2f kWh", it) } ?: "N/A"

                cards = listOf(
                    StatCardData("Consumo Total", consumoTotalStr, "Energia acumulada", "Ao vivo", StatTone.BLUE, "cpu"),
                    StatCardData("Dispositivos Ativos", if (apiEquipamentos.isNotEmpty()) "${apiEquipamentos.size}" else "N/A", "Conectados", "Ao vivo", StatTone.GREEN, "check_circle"),
                    StatCardData("Potência Ativa", potenciaStr, "Potência instantânea", "Ao vivo", StatTone.YELLOW, "zap"),
                    StatCardData("Tensão Atual", tensaoStr, "Tensão da rede", "Ao vivo", StatTone.BLUE, "flash"),
                    StatCardData("Corrente Média", correnteStr, "Corrente instantânea", "Ao vivo", StatTone.GREEN, "activity"),
                    StatCardData("Frequência", ultimaMedicao?.frequencia?.let { String.format(Locale.getDefault(), "%.1f Hz", it) } ?: "60.0 Hz", "Frequência da rede", "Estável", StatTone.BLUE, "activity")
                )
            }

            if (cards.isEmpty()) {
                cards = listOf(
                    StatCardData("Consumo Total", "Sem dados", "Servidor offline", "N/A", StatTone.BLUE, "cpu"),
                    StatCardData("Dispositivos Ativos", "0", "Sem conexão", "N/A", StatTone.RED, "wifi_off"),
                    StatCardData("Potência Ativa", "0 kW", "Sem conexão", "N/A", StatTone.YELLOW, "zap"),
                    StatCardData("Tensão Atual", "0 V", "Sem conexão", "N/A", StatTone.BLUE, "flash"),
                    StatCardData("Corrente Média", "0 A", "Sem conexão", "N/A", StatTone.GREEN, "activity"),
                    StatCardData("Frequência", "0 Hz", "Sem conexão", "N/A", StatTone.BLUE, "activity")
                )
            }

            if (alarms.isEmpty()) {
                alarms = listOf(
                    AlarmData("Sobretensão detectada", "Compressor HVAC-03", "há 2 min", AlarmType.CRITICAL),
                    AlarmData("Fator de potência baixo", "Motor Bomba-07", "há 18 min", AlarmType.WARNING),
                    AlarmData("Consumo acima do limite", "Linha de Produção B", "há 45 min", AlarmType.WARNING),
                    AlarmData("Novo sensor registrado", "Painel Elétrico PE-12", "há 1 h", AlarmType.INFO),
                    AlarmData("Equipamento restaurado", "Torno CNC-02", "há 2 h", AlarmType.SUCCESS)
                )
            }

            if (equipamentosList.isEmpty()) {
                equipamentosList = listOf(
                    ComposicaoItem("1", "Planta Industrial Jandira", 1685.3, "Calculado"),
                    ComposicaoItem("2", "Planta Industrial Osasco", 2613.6, "Calculado"),
                    ComposicaoItem("3", "Centro Logístico Alphaville", 0.0, "Sem dados"),
                    ComposicaoItem("4", "Subestação Central", 245.8, "Calculado")
                )
            }

            val tensaoVal = ultimaMedicao?.tensao?.let { String.format(Locale.getDefault(), "%.1f V", it) } ?: "220,0 V"
            val correnteVal = ultimaMedicao?.corrente?.let { String.format(Locale.getDefault(), "%.2f A", it) } ?: "14,2 A"
            val potenciaVal = ultimaMedicao?.potencia_ativa?.let { String.format(Locale.getDefault(), "%.2f kW", it) } ?: "3,12 kW"
            val frequenciaVal = ultimaMedicao?.frequencia?.let { String.format(Locale.getDefault(), "%.1f Hz", it) } ?: "60,0 Hz"
            val fpVal = ultimaMedicao?.fator_potencia?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "0,98"
            val energiaVal = ultimaMedicao?.energia_acumulada?.let { String.format(Locale.getDefault(), "%.2f kWh", it) } ?: "1.284 kWh"

            val indicadores = listOf(
                EnergeticMetricData("Tensão Instantânea", tensaoVal, "Medição ao vivo", "flash"),
                EnergeticMetricData("Corrente Instantânea", correnteVal, "Medição ao vivo", "activity"),
                EnergeticMetricData("Potência Ativa", potenciaVal, "Medição ao vivo", "zap"),
                EnergeticMetricData("Energia Acumulada", energiaVal, "Sensor PZEM", "cpu"),
                EnergeticMetricData("Frequência", frequenciaVal, "Rede elétrica", "clock"),
                EnergeticMetricData("Fator de Potência", fpVal, "Eficiência energética", "trending_up")
            )

            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val statusText = if (ultimaMedicao != null) {
                "Ao vivo às $currentTime (PZEM $idPzem)"
            } else {
                "Sem resposta da API às $currentTime (Verifique 10.0.2.2:3000)"
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    summaryCards = cards,
                    alarms = alarms,
                    indicadores = indicadores,
                    composicaoItems = equipamentosList,
                    lastUpdatedText = statusText
                )
            }
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onMetricSelected(metric: String) {
        _uiState.update {
            it.copy(
                selectedMetric = metric,
                chartPoints = mapToChartPoints(metric)
            )
        }
    }

    fun onChartPeriodSelected(period: String) {
        _uiState.update { it.copy(selectedChartPeriod = period, filters = it.filters.copy(period = period)) }
        savedStateHandle[SAVED_FILTERS] = _uiState.value.filters
        restartChart()
    }

    fun retryChart() {
        catalogLoadedAt = 0L
        restartChart(clear = false)
    }

    private fun restartChart(clear: Boolean = true) {
        chartJob?.cancel()
        if (clear) {
            realSerieTemporalData = emptyList()
            chartPeriod = null
            _uiState.update { it.copy(chartPoints = emptyList(), chart = ChartState()) }
        }
        chartJob = viewModelScope.launch {
            while (isActive) {
                fetchChart()
                delay(5000)
            }
        }
    }

    private suspend fun fetchChart() {
        val snapshot = _uiState.value
        val token = chartTokens.getToken()
        val gate = ChartLogic.gate(!token.isNullOrBlank(), snapshot.selectedChartPeriod)
        if (gate != null) {
            realSerieTemporalData = emptyList()
            catalogSession = null
            _uiState.update { it.copy(chartPoints = emptyList(), chartCatalog = null, chart = ChartState(status = gate)) }
            return
        }
        if (catalogSession != token) {
            realSerieTemporalData = emptyList()
            _uiState.update { it.copy(chartPoints = emptyList(), chartCatalog = null, chart = ChartState(status = ChartStatus.LOADING)) }
        }
        if (_uiState.value.chartCatalog == null || catalogLoadedAt == 0L || catalogSession != token ||
            System.nanoTime() - catalogLoadedAt > 60_000_000_000L) {
            _uiState.update { it.copy(chart = it.chart.copy(status = ChartStatus.LOADING)) }
            val catalogResult = dashboardRepository.getChartCatalog()
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val catalog = catalogResult.getOrNull()
            if (catalog == null) {
                _uiState.update { it.copy(chartCatalog = null) }
                chartFailure(httpChartStatus(catalogResult.exceptionOrNull()))
                return
            }
            catalogSession = token
            catalogLoadedAt = System.nanoTime()
            _uiState.update { it.copy(chartCatalog = catalog) }
        }
        val (pzemId, resolutionStatus) = _uiState.value.chartCatalog!!.resolve(snapshot.filters)
        if (resolutionStatus != null || pzemId == null) {
            realSerieTemporalData = emptyList()
            chartPeriod = null
            _uiState.update { it.copy(chartPoints = emptyList(), chart = ChartState(status = resolutionStatus ?: ChartStatus.SELECT_ENTITY)) }
            return
        }
        if (_uiState.value.chart.pzemId != pzemId) {
            realSerieTemporalData = emptyList()
            _uiState.update { it.copy(chartPoints = emptyList(), chart = ChartState(status = ChartStatus.LOADING, pzemId = pzemId)) }
        }
        val now = java.time.LocalDateTime.now(chartZone)
        val period = ChartLogic.period(snapshot.selectedChartPeriod, now) ?: return
        _uiState.update { it.copy(chart = it.chart.copy(status = ChartStatus.LOADING)) }
        val result = dashboardRepository.getChartSerieTemporal(pzemId, period.hours)
        // Repositório propaga cancelamento; resultado antigo nunca substitui outro filtro/PZEM.
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        result.fold(onSuccess = { rows ->
            if (rows.any { it.id_pzem != pzemId }) {
                chartFailure(ChartStatus.ERROR)
                return@fold
            }
            realSerieTemporalData = rows
            val scopedPoints = _uiState.value.chartCatalog?.scopedPointIds(snapshot.filters)
            chartPeriod = period
            val points = mapToChartPoints(_uiState.value.selectedMetric)
            _uiState.update { it.copy(chartPoints = points, chart = it.chart.copy(
                status = if (points.none { point -> point.valor != null }) ChartStatus.EMPTY else ChartStatus.READY,
                stale = false, truncated = rows.size >= 10000,
                invalidTimestamps = rows.count { row -> ChartLogic.timestamp(row.data_hora, chartZone) == null },
                outOfScope = if (scopedPoints == null) 0 else rows.count { row ->
                    row.id_ponto_medicao !in scopedPoints && ChartLogic.timestamp(row.data_hora, chartZone)?.let(period::contains) == true
                },
                updatedAt = now
            )) }
        }, onFailure = { error ->
            chartFailure(httpChartStatus(error))
        })
    }

    private fun httpChartStatus(error: Throwable?): ChartStatus = when ((error as? retrofit2.HttpException)?.code()) {
        401 -> ChartStatus.NO_SESSION
        403 -> ChartStatus.FORBIDDEN
        else -> ChartStatus.ERROR
    }

    private fun chartFailure(status: ChartStatus) {
        val clear = status == ChartStatus.NO_SESSION || status == ChartStatus.FORBIDDEN
        if (clear) realSerieTemporalData = emptyList()
        _uiState.update { it.copy(
            chartPoints = if (clear) emptyList() else it.chartPoints,
            chartCatalog = if (clear) null else it.chartCatalog,
            chart = if (clear) ChartState(status = status, pzemId = it.chart.pzemId)
                else it.chart.copy(status = status, stale = it.chart.updatedAt != null)
        ) }
    }

    fun setComposicaoModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isComposicaoModalOpen = visible) }
    }

    private fun mapToChartPoints(metric: String): List<MedicaoPoint> = chartPeriod?.let {
        ChartLogic.map(realSerieTemporalData, ChartMetric.from(metric), it, chartZone,
            _uiState.value.chartCatalog?.scopedPointIds(_uiState.value.filters))
    } ?: emptyList()

    companion object {
        private const val SAVED_FILTERS = "saved_filters_state"
    }
}
