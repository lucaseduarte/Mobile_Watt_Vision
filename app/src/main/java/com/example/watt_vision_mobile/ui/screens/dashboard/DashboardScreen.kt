package com.example.watt_vision_mobile.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.watt_vision_mobile.data.api.integracaoLocal
import kotlinx.coroutines.delay
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import com.example.watt_vision_mobile.ui.screens.dashboard.components.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    if (integracaoLocal && !viewModel.demonstracaoDisponivel) {
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle, viewModel) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    viewModel.atualizarAutomaticamente()
                    delay(5_000)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Cabeçalho com barra de filtros compacta e ModalBottomSheet
            DashboardHeaderAndFilters(
                filters = uiState.filters,
                catalog = uiState.chartCatalog,
                atualizadoEm = uiState.atualizadoEm,
                carregando = uiState.isLoading,
                onFilterChanged = { newFilters -> viewModel.applyFilters(newFilters) },
                onRefreshClick = { viewModel.loadDashboardData() },
                onRemoveFilter = { filterKey -> viewModel.removeSpecificFilter(filterKey) },
                onAlarmesClick = { onNavigate("Alarmes") },
                onConfiguracoesClick = { onNavigate("Configurações") }
            )

            // Conteúdo restante com espaçamento lateral de 16.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 3. Cards de Resumo (StatCards em grid 2 colunas para mobile)
                StatCardsSection(cards = uiState.summaryCards)

                // 4. Gráfico "Desempenho do Sistema"
                SystemPerformanceChart(
                    uiState.chartPoints, uiState.filters.metric.key, uiState.selectedChartPeriod,
                    viewModel::onMetricSelected, viewModel::onChartPeriodSelected, uiState.chart,
                    viewModel::retryChart
                )

                RecentAlarmsSection(
                    alarms = uiState.alarms,
                    onSeeAllClick = { onNavigate("Alarmes") }
                )

                if (integracaoLocal && !viewModel.demonstracaoDisponivel) {
                    IndicadoresDashboardSection(
                        uiState.filters.metric.label, uiState.filters.period,
                        uiState.indicadores.ifEmpty { indicadoresDashboardVazios }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
