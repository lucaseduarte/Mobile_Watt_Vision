package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.model.DashboardCatalog
import com.example.watt_vision_mobile.model.MedicaoDto
import java.time.LocalDate

enum class StatTone { BLUE, GREEN, RED, YELLOW }
enum class AlarmType { CRITICAL, WARNING, INFO, SUCCESS, UNKNOWN }
data class StatCardData(val label: String, val value: String, val detail: String, val change: String? = null,
    val tone: StatTone = StatTone.BLUE, val iconName: String = "cpu")
data class AlarmData(val title: String, val equipment: String, val time: String, val type: AlarmType,
    val status: String = "Não informado", val severity: String = "Não informada")
data class EnergeticMetricData(val title: String, val value: String, val detail: String, val iconName: String)
data class MedicaoPoint(val time: String, val valor: Float, val position: Long = 0)
data class ComposicaoItem(val id: String, val entidade: String, val valor: Double, val origem: String)

data class DashboardSelection(
    val location: Int? = null, val sector: Int? = null, val equipment: Int? = null,
    val point: Int? = null, val sensor: Int? = null, val period: String = "24h",
    val start: String = LocalDate.now().toString(), val end: String = LocalDate.now().toString()
)

data class DashboardUiState(
    val selection: DashboardSelection = DashboardSelection(),
    val catalog: DashboardCatalog = DashboardCatalog(),
    val isLoading: Boolean = false,
    val stale: Boolean = false,
    val selectedMetric: String = "tensao",
    val summaryCards: List<StatCardData> = emptyList(),
    val indicators: List<EnergeticMetricData> = emptyList(),
    val chartPoints: List<MedicaoPoint> = emptyList(),
    val measurements: List<MedicaoDto> = emptyList(),
    val alarms: List<AlarmData> = emptyList(),
    val alarmsUnavailable: Boolean = true,
    val alarmCount: Int? = null,
    val consumptionCards: List<StatCardData> = emptyList(),
    val messages: List<String> = emptyList(),
    val errorMessage: String? = null,
    val scopeText: String = "Selecione um medidor após consultar a API.",
    val lastUpdatedText: String = "Aguardando consulta",
    val measurementTime: String = "Sem amostra disponível"
)
