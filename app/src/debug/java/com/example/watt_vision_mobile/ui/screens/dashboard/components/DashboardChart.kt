package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import com.example.watt_vision_mobile.ui.theme.TextMuted
import com.example.watt_vision_mobile.ui.theme.ToneYellowText
import java.time.LocalDateTime
import java.time.ZoneId

/** Ativação explícita, restrita a Debug. A origem real nunca alimenta a demonstração. */
@Composable
fun DashboardChart(
    points: List<MedicaoPoint>, selectedMetric: String, selectedPeriod: String,
    onMetricSelected: (String) -> Unit, onPeriodSelected: (String) -> Unit,
    state: ChartState, onRetry: () -> Unit
) {
    var demonstration by rememberSaveable { mutableStateOf(false) }
    var metric by rememberSaveable { mutableStateOf("tensao") }
    var period by rememberSaveable { mutableStateOf("Hoje") }
    val now = remember(demonstration) { LocalDateTime.now() }
    val demoPoints = remember(demonstration, metric, period, now) {
        if (demonstration) ChartDemoData.points(metric, period, now, ZoneId.systemDefault()) else emptyList()
    }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { demonstration = !demonstration }) {
                Text(if (demonstration) "Voltar aos dados da API" else "Ver demonstração do gráfico", fontSize = 11.sp)
            }
        }
        if (demonstration) {
            Text("DEBUG · DADOS SIMULADOS — somente este gráfico", fontSize = 11.sp, color = ToneYellowText)
            Text("Amostras locais; os filtros de entidades não se aplicam à demonstração.", fontSize = 10.sp, color = TextMuted)
            SystemPerformanceChart(
                demoPoints, metric, period, { metric = it }, { period = it },
                ChartState(status = ChartStatus.READY), onRetry = {}
            )
        } else {
            SystemPerformanceChart(points, selectedMetric, selectedPeriod, onMetricSelected, onPeriodSelected, state, onRetry)
        }
    }
}
