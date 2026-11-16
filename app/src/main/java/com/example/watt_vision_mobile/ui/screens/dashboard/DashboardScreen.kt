package com.example.watt_vision_mobile.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.watt_vision_mobile.ui.screens.dashboard.components.*
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.setVisible(true)
            if (event == Lifecycle.Event.ON_STOP) viewModel.setVisible(false)
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.setVisible(true)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.setVisible(false) }
    }
    Column(modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DashboardHeaderAndFilters(state, viewModel::select, viewModel::loadDashboardData,
            { onNavigate("Alarmes") }, { onNavigate("Configurações") })
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (state.stale) "Atualizando · exibindo dados da consulta anterior." else "Consultando dados…", color = TextMuted)
            }
            if (state.stale && !state.isLoading) Text("Dados desatualizados da última consulta. Tente atualizar.", color = ToneYellowText)
            state.errorMessage?.let {
                Text(it, color = ToneRedText)
                OutlinedButton(onClick = viewModel::loadDashboardData, enabled = !state.isLoading) { Text("Tentar novamente") }
            }
            state.messages.forEach { Text(it, color = ToneYellowText, style = MaterialTheme.typography.bodySmall) }
            Text(state.scopeText, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            StatCardsSection(state.summaryCards)
            Text("Amostra: ${state.measurementTime}\nHorários preservados na origem; fuso do servidor não confirmado. Atualização da consulta não significa nova medição.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            if (state.indicators.isNotEmpty()) IndicadoresEnergeticosSection(state.indicators, periodText = "amostra retornada")
            SystemPerformanceChart(state.chartPoints, state.selectedMetric, state.selection.period,
                viewModel::onMetricSelected, { viewModel.select(state.selection.copy(period = it)) })
            ConsumptionDates(state.selection) { start, end -> viewModel.select(state.selection.copy(start = start, end = end)) }
            StatCardsSection(state.consumptionCards)
            Text("Composição por locais e indicadores consolidados indisponíveis: não há agregação confirmada para o sistema completo.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            LocalEnergySimulation()
            RecentAlarmsSection(alarms = state.alarms, unavailable = state.alarmsUnavailable, total = state.alarmCount,
                onSeeAllClick = { onNavigate("Alarmes") })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConsumptionDates(selection: DashboardSelection, onQuery: (String, String) -> Unit) {
    var start by remember(selection.start) { mutableStateOf(DashboardValues.displayDate(selection.start)) }
    var end by remember(selection.end) { mutableStateOf(DashboardValues.displayDate(selection.end)) }
    val parsedStart = DashboardValues.date(start)
    val parsedEnd = DashboardValues.date(end)
    val valid = parsedStart != null && parsedEnd != null && DashboardValues.validDates(parsedStart, parsedEnd)
    Surface(color = SurfaceDark, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Consumo e custo por datas", style = MaterialTheme.typography.titleMedium)
            Text("Datas de calendário da API, independentes da janela móvel do gráfico. Não inclui consumo anterior à primeira leitura retornada.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            OutlinedTextField(value = start, onValueChange = { start = it.take(10) }, label = { Text("Início · DD/MM/AAAA") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = end, onValueChange = { end = it.take(10) }, label = { Text("Fim · DD/MM/AAAA (inclusive)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (!valid) Text("Informe datas válidas, com fim igual ou posterior ao início.", color = ToneYellowText)
            OutlinedButton(onClick = { onQuery(parsedStart!!, parsedEnd!!) }, enabled = valid && selection.sensor != null) { Text("Consultar período") }
        }
    }
}

@Composable
private fun LocalEnergySimulation() {
    var energy by remember { mutableStateOf("") }
    var tariff by remember { mutableStateOf("") }
    val result = remember(energy, tariff) { DashboardValues.simulation(energy, tariff) }
    Surface(color = SurfaceDark, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Simulação local de custo", style = MaterialTheme.typography.titleMedium)
            Text("Valores informados por você, sem salvar ou alterar tarifas. Energia × preço por kWh; não representa a fatura completa.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            OutlinedTextField(value = energy, onValueChange = { energy = it.take(20) }, label = { Text("Energia simulada · kWh") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tariff, onValueChange = { tariff = it.take(20) }, label = { Text("Preço simulado · R$/kWh") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(result?.let { "Simulação: ${java.text.NumberFormat.getCurrencyInstance(DashboardValues.locale).format(it)}" }
                ?: "Informe valores não negativos para simular.", color = ToneCyanText)
        }
    }
}
