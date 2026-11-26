package com.example.watt_vision_mobile.ui.screens.dashboard

enum class StatTone {
    BLUE, GREEN, RED, YELLOW
}

enum class AlarmType {
    CRITICAL, WARNING, INFO, SUCCESS
}

data class StatCardData(
    val label: String,
    val value: String,
    val detail: String,
    val change: String? = null,
    val tone: StatTone = StatTone.BLUE,
    val iconName: String = "cpu"
)

data class AlarmData(
    val title: String,
    val equipment: String,
    val time: String,
    val type: AlarmType
)

data class ComposicaoItem(
    val id: String,
    val entidade: String,
    val valor: Double,
    val origem: String
)

data class DashboardFilterState(
    val period: String = "Hoje",
    val location: String = "Todos",
    val sector: String = "Todos",
    val equipment: String = "Todos",
    val locationId: Int? = null,
    val sectorId: Int? = null,
    val equipmentId: Int? = null,
    val metric: ChartMetric = ChartMetric.VOLTAGE
) : java.io.Serializable {
    val isDefault: Boolean
        get() = activeCount == 0

    val activeCount: Int
        get() {
            var count = 0
            if (location != "Todos") count++
            if (sector != "Todos") count++
            if (equipment != "Todos") count++
            return count
        }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val simulated: Boolean = false,
    val summaryCards: List<StatCardData> = emptyList(),
    val indicadores: List<IndicadorDashboard> = emptyList(),
    val selectedChartPeriod: String = "Hoje",
    val chartPoints: List<MedicaoPoint> = emptyList(),
    val chart: ChartState = ChartState(),
    val chartCatalog: ChartCatalog? = null,
    val alarms: List<AlarmData>? = null,
    val composicaoItems: List<ComposicaoItem> = emptyList(),
    val isComposicaoModalOpen: Boolean = false,
    val atualizadoEm: java.time.LocalDateTime? = null,
    val filters: DashboardFilterState = DashboardFilterState(),
    val errorMessage: String? = null
)
