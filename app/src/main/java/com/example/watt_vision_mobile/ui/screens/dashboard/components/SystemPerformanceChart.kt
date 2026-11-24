package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import com.example.watt_vision_mobile.ui.theme.*
import java.text.NumberFormat
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun SystemPerformanceChart(
    points: List<MedicaoPoint>,
    selectedMetric: String,
    selectedPeriod: String,
    onMetricSelected: (String) -> Unit,
    onPeriodSelected: (String) -> Unit,
    state: ChartState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metric = ChartMetric.from(selectedMetric)
    val metricColor = when (metric) {
        ChartMetric.ENERGY -> LinkBlue
        ChartMetric.POWER -> ToneGreenText
        ChartMetric.CURRENT -> ToneYellowText
        ChartMetric.FREQUENCY -> PrimaryBlue
        ChartMetric.POWER_FACTOR -> ToneYellowText
        else -> Color(0xFF818CF8) // Cor da tensão preservada do protótipo.
    }
    val number = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply { maximumFractionDigits = 4 } }
    var selectedKey by remember(selectedMetric, state.pzemId, selectedPeriod) { mutableStateOf<String?>(null) }
    val selectedPoint = remember(selectedKey, points) { points.firstOrNull { it.key == selectedKey } }
    LaunchedEffect(points) { if (selectedPoint == null) selectedKey = null }

    // Estrutura externa idêntica à referência a30af6e.
    Box(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark).border(1.dp, CardBorder, RoundedCornerShape(20.dp)).padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Desempenho do Sistema", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("${metric.label}${if (metric.unit.isBlank()) "" else " (${metric.unit})"} ao longo do período", fontSize = 11.sp, color = TextMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(InputBackground, RoundedCornerShape(10.dp)).padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChartLogic.periods.forEach { period ->
                    val active = period == selectedPeriod
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (active) PrimaryBlue else Color.Transparent)
                            .clickable { onPeriodSelected(period) }.heightIn(min = 44.dp).padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(period, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) TextWhite else TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartMetric.entries.forEach { option ->
                    val active = metric == option
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) PrimaryBlue.copy(alpha = 0.25f) else InputBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) PrimaryBlue else Color.Transparent),
                        modifier = Modifier.clickable { onMetricSelected(option.key) }
                    ) {
                        Box(Modifier.heightIn(min = 44.dp).padding(vertical = 6.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
                            Text(option.label, fontSize = 11.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) PrimaryBlue else TextMuted, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Calendário do aparelho (${ZoneId.systemDefault().id}). A API não informa o fuso dos horários sem offset.",
                fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))

            val message = when (state.status) {
                ChartStatus.LOADING -> if (points.isEmpty()) "Carregando medições…" else "Atualizando medições…"
                ChartStatus.EMPTY -> "Nenhum dado desta grandeza no período."
                ChartStatus.ERROR -> "Falha na consulta. Tente novamente."
                ChartStatus.NO_SESSION -> "Entre na API para consultar medições reais."
                ChartStatus.FORBIDDEN -> "Acesso negado aos dados necessários para o gráfico."
                ChartStatus.SELECT_ENTITY -> "Selecione uma entidade no filtro superior para consultar medições."
                ChartStatus.NO_SENSOR -> "Nenhum medidor vinculado à entidade selecionada."
                ChartStatus.AMBIGUOUS -> "Há vários medidores neste filtro. Selecione uma entidade com um único medidor no filtro superior."
                ChartStatus.FILTER_UNSUPPORTED -> "Não foi possível determinar o medidor desta entidade. Atualize os filtros e selecione novamente."
                ChartStatus.UNSUPPORTED_PERIOD -> "Período indisponível. Selecione Hoje, 6 horas ou Ontem."
                ChartStatus.READY -> null
            }
            if (message != null) Text(message, color = TextMuted, fontSize = 11.sp)
            if (state.status == ChartStatus.ERROR || state.status == ChartStatus.FORBIDDEN) {
                TextButton(onClick = onRetry) { Text("Tentar novamente", fontSize = 11.sp) }
            }
            if (state.stale) Text("Dados desatualizados — última consulta: ${state.updatedAt?.format(ChartLogic.dateTimeFormat).orEmpty()}", color = ToneYellowText, fontSize = 11.sp)
            if (state.truncated) Text(com.example.watt_vision_mobile.model.AVISO_LIMITE_SERIE_TEMPORAL, color = ToneYellowText, fontSize = 11.sp)
            if (state.outOfScope > 0) Text("${state.outOfScope} leitura(s) de outros pontos não são exibidas neste filtro.", color = TextMuted, fontSize = 11.sp)
            if (state.invalidTimestamps > 0) Text("${state.invalidTimestamps} registro(s) sem data válida não podem ser posicionados.", color = ToneYellowText, fontSize = 11.sp)
            selectedPoint?.let { point ->
                Column(Modifier.fillMaxWidth().testTag("chart-selected-sample").background(InputBackground, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(point.timestamp.format(ChartLogic.dateTimeFormat), color = TextMuted, fontSize = 11.sp, modifier = Modifier.testTag("chart-selected-time"))
                    Text("${point.valor?.let(number::format) ?: "Indisponível"} ${metric.unit}", color = metricColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            ChartPlot(points, metric, metricColor, selectedKey, state.pzemId, selectedPeriod) { selectedKey = it }
        }
    }
}

@Composable
private fun ChartPlot(
    points: List<MedicaoPoint>, metric: ChartMetric, color: Color, selectedKey: String?, pzem: Int?, period: String,
    onSelect: (String?) -> Unit
) {
    val number = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply { maximumFractionDigits = 4 } }
    val bounds = remember(points) {
        val values = points.mapNotNull { it.valor }
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val margin = if (max == min) maxOf(abs(min) * 0.1, 1.0) else (max - min) * 0.1
        min - margin to max + margin
    }
    var first by remember(pzem, period) { mutableIntStateOf(0) }
    var anchor by remember(pzem, period) { mutableStateOf<String?>(null) }
    var remainder by remember(pzem, period) { mutableFloatStateOf(0f) }
    LaunchedEffect(points) {
        val anchored = points.indexOfFirst { it.key == anchor }
        if (anchored >= 0) first = anchored
        else first = first.coerceIn(0, (points.size - 1).coerceAtLeast(0))
    }
    Row(Modifier.fillMaxWidth().height(180.dp)) {
        Box(Modifier.width(64.dp).fillMaxHeight()) {
            if (points.any { it.valor != null }) {
                Column(Modifier.fillMaxWidth().height(154.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    (4 downTo 0).forEach { tick ->
                        Text(number.format(bounds.first + (bounds.second - bounds.first) * tick / 4), fontSize = 9.sp, color = TextMuted, maxLines = 1)
                    }
                }
            }
            Text(metric.unit.ifBlank { "FP" }, fontSize = 9.sp, color = TextMuted, modifier = Modifier.align(Alignment.BottomStart))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val visibleCount = (maxWidth.value / 32).toInt().coerceAtLeast(2)
            val start = first.coerceIn(0, (points.size - visibleCount).coerceAtLeast(0))
            val window = points.subList(start, (start + visibleCount).coerceAtMost(points.size))
            val span = if (window.size > 1) Duration.between(window.first().timestamp, window.last().timestamp).toMillis().coerceAtLeast(1) else 1L
            val drag = rememberDraggableState { delta ->
                remainder -= delta / (widthPx / (visibleCount - 1)).coerceAtLeast(1f)
                val steps = remainder.toInt()
                if (steps != 0) {
                    first = ChartLogic.windowStart(first, steps.toFloat(), points.size, visibleCount)
                    anchor = points.getOrNull(first)?.key
                    remainder -= steps
                }
            }
            Canvas(
                Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(2.dp))
                    .testTag("chart-plot")
                    .semantics {
                        contentDescription = "Gráfico de ${metric.label}. Arraste horizontalmente para navegar; toque para consultar uma amostra."
                        stateDescription = "Amostras ${if (window.isEmpty()) 0 else start + 1} a ${start + window.size} de ${points.size}"
                    }
                    .draggable(drag, Orientation.Horizontal)
                    .pointerInput(window, selectedKey) {
                        detectTapGestures { tap ->
                            onSelect(ChartLogic.nearest(points, start, visibleCount, tap.x / size.width.coerceAtLeast(1))?.key)
                        }
                    }
            ) {
                val height = size.height - 14.dp.toPx()
                val range = bounds.second - bounds.first
                fun position(point: MedicaoPoint, index: Int): Offset {
                    val x = if (window.size == 1) size.width / 2 else if (window.first().timestamp == window.last().timestamp) {
                        size.width * index / (window.size - 1)
                    } else size.width * (Duration.between(window.first().timestamp, point.timestamp).toMillis().toDouble() / span).toFloat()
                    val y = 7.dp.toPx() + height * (1 - ((point.valor!! - bounds.first) / range).toFloat())
                    return Offset(x, y)
                }
                for (i in 0..4) {
                    val y = 7.dp.toPx() + height * i / 4
                    drawLine(CardBorder.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                window.forEachIndexed { index, point ->
                    if (point.valor == null) return@forEachIndexed
                    val at = position(point, index)
                    val previous = window.getOrNull(index - 1)
                    if (!point.breakBefore && previous?.valor != null) {
                        val before = position(previous, index - 1)
                        val fill = Path().apply {
                            moveTo(before.x, size.height); lineTo(before.x, before.y)
                            lineTo(at.x, at.y); lineTo(at.x, size.height); close()
                        }
                        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
                        drawLine(color, before, at, 3.dp.toPx())
                    }
                    val selected = point.key == selectedKey
                    drawCircle(if (selected) Color.White else color, if (selected) 6.dp.toPx() else 3.5.dp.toPx(), at)
                    if (selected) drawCircle(color, 8.dp.toPx(), at, style = Stroke(2.dp.toPx()))
                }
            }
            Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                listOfNotNull(window.firstOrNull(), window.lastOrNull()?.takeIf { window.size > 1 }).forEach { point ->
                    Text(point.timestamp.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), fontSize = 9.sp, color = TextMuted)
                }
            }
        }
    }
    if (points.isNotEmpty()) {
        Text(if (points.size > 2) "← Arraste no gráfico para explorar as amostras →" else "Toque para consultar a amostra",
            fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}
