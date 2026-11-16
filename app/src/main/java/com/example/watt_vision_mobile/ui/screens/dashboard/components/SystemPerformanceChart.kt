package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import com.example.watt_vision_mobile.ui.theme.*
import kotlin.math.abs

@Composable
fun SystemPerformanceChart(
    points: List<MedicaoPoint>, selectedMetric: String, selectedPeriod: String,
    onMetricSelected: (String) -> Unit, onPeriodSelected: (String) -> Unit, modifier: Modifier = Modifier
) {
    val unit = DashboardValues.unit(selectedMetric)
    var selected by remember(points, selectedMetric) { mutableStateOf<Int?>(null) }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = SurfaceDark) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Desempenho do Sistema", style = MaterialTheme.typography.titleMedium, color = TextWhite)
            Text("${DashboardValues.metrics.firstOrNull { it.first == selectedMetric }?.second} · $unit", color = ToneCyanText)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (DashboardValues.periods.keys + "30d").forEach { period ->
                    FilterChip(selected = selectedPeriod == period, onClick = { onPeriodSelected(period) },
                        label = { Text(if (period == "30d") "30d · indisponível" else period) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            Text("Janela móvel: $selectedPeriod · não equivale a dia civil. Máximo de 10.000 amostras; não há paginação.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardValues.metrics.forEach { (id, label) ->
                    FilterChip(selected = selectedMetric == id, onClick = { onMetricSelected(id) }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            Text("Pontos observados · sem interpolação. Energia acumulada é o contador do sensor, não consumo do período.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            if (points.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("Sem amostras válidas para esta grandeza/período.", color = TextMuted)
                }
            } else {
                val min = points.minOf { it.valor }.toDouble()
                val max = points.maxOf { it.valor }.toDouble()
                val padding = if (min == max) maxOf(abs(min) * 0.05, 0.1) else (max - min) * 0.05
                val low = maxOf(0.0, min - padding)
                val high = max + padding
                val first = points.first().position
                val span = (points.last().position - first).coerceAtLeast(1).toDouble()
                Row(Modifier.fillMaxWidth().height(190.dp)) {
                    Column(Modifier.width(65.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(high, (low + high) / 2, low).forEach { Text(DashboardValues.number(it, digits = 2), style = MaterialTheme.typography.labelSmall, color = TextMuted) }
                    }
                    Canvas(Modifier.weight(1f).fillMaxHeight().pointerInput(points) {
                        detectTapGestures { tap ->
                            val target = first + (tap.x / size.width).coerceIn(0f, 1f) * span
                            selected = points.indices.minByOrNull { abs(points[it].position - target) }
                        }
                    }) {
                        for (fraction in listOf(0f, 0.5f, 1f)) drawLine(CardBorder, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction))
                        points.forEachIndexed { index, p ->
                            val x = if (points.size == 1) size.width / 2 else ((p.position - first) / span).toFloat() * size.width
                            val y = size.height * (1 - (p.valor - low) / (high - low)).toFloat()
                            drawCircle(if (index == selected) TextWhite else ToneCyanText,
                                radius = if (index == selected) 6.dp.toPx() else 2.5.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(points.first().time, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    if (points.size > 1) Text(points.last().time, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                Text("Eixo horizontal: tempo da medição conforme origem. Toque para consultar uma amostra.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                selected?.let { points.getOrNull(it) }?.let {
                    Text("${it.time}\n${DashboardValues.number(it.valor.toDouble(), if (selectedMetric == "fp") "" else unit, 3)}", color = ToneCyanText)
                }
                Text("${points.size} amostras válidas desenhadas; campos nulos/ inválidos não viram zero.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
