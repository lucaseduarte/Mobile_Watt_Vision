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

data class EnergeticMetricData(
    val title: String,
    val value: String,
    val detail: String,
    val iconName: String
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
    val equipmentId: Int? = null
) : java.io.Serializable {
    val isDefault: Boolean
        get() = period == "Hoje" && location == "Todos" && sector == "Todos" && equipment == "Todos"

    val activeCount: Int
        get() {
            var count = 0
            if (period != "Hoje") count++
            if (location != "Todos") count++
            if (sector != "Todos") count++
            if (equipment != "Todos") count++
            return count
        }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summaryCards: List<StatCardData> = emptyList(),
    val selectedMetric: String = "tensao", // tensao, consumo, potencia, corrente
    val selectedChartPeriod: String = "Hoje",
    val chartPoints: List<MedicaoPoint> = emptyList(),
    val chart: ChartState = ChartState(),
    val chartCatalog: ChartCatalog? = null,
    val alarms: List<AlarmData> = emptyList(),
    val indicadores: List<EnergeticMetricData> = emptyList(),
    val composicaoItems: List<ComposicaoItem> = emptyList(),
    val isComposicaoModalOpen: Boolean = false,
    val lastUpdatedText: String = "Atualizado às 10:28",
    val filters: DashboardFilterState = DashboardFilterState(),
    val errorMessage: String? = null
)
