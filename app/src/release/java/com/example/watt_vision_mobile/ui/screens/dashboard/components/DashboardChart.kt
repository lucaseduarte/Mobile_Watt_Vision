package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.runtime.Composable
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartState
import com.example.watt_vision_mobile.ui.screens.dashboard.MedicaoPoint

/** Release oferece exclusivamente o gráfico real, sem controles ou dados de demonstração. */
@Composable
fun DashboardChart(
    points: List<MedicaoPoint>, selectedMetric: String, selectedPeriod: String,
    onMetricSelected: (String) -> Unit, onPeriodSelected: (String) -> Unit,
    state: ChartState, onRetry: () -> Unit, demonstracao: Boolean, onToggleDemo: () -> Unit
) {
    SystemPerformanceChart(points, selectedMetric, selectedPeriod, onMetricSelected, onPeriodSelected, state, onRetry)
}
