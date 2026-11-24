package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartState
import com.example.watt_vision_mobile.ui.screens.dashboard.MedicaoPoint

@Composable
fun DashboardChart(
    points: List<MedicaoPoint>, selectedMetric: String, selectedPeriod: String,
    onMetricSelected: (String) -> Unit, onPeriodSelected: (String) -> Unit,
    state: ChartState, onRetry: () -> Unit, demonstracao: Boolean, onToggleDemo: () -> Unit
) {
    Column {
        TextButton(onClick = onToggleDemo) {
            Text(if (demonstracao) "Sair da demonstração" else "Ver demonstração do gráfico", fontSize = 11.sp)
        }
        SystemPerformanceChart(points, selectedMetric, selectedPeriod, onMetricSelected, onPeriodSelected, state, onRetry)
    }
}
