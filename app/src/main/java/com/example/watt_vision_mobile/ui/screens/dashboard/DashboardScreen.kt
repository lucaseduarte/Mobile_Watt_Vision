package com.example.watt_vision_mobile.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import com.example.watt_vision_mobile.ui.screens.dashboard.components.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    modoCompartilhado: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()

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
                lastUpdatedText = uiState.lastUpdatedText,
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
                // 2. Botão de Composição do Cálculo (Σ Ver Composição) e Modal
                CalculoButtonAndModal(
                    composicaoItems = uiState.composicaoItems,
                    isOpen = uiState.isComposicaoModalOpen,
                    onOpenModal = { viewModel.setComposicaoModalVisible(true) },
                    onCloseModal = { viewModel.setComposicaoModalVisible(false) }
                )

                // 3. Cards de Resumo (StatCards em grid 2 colunas para mobile)
                StatCardsSection(cards = uiState.summaryCards)

                // 4. Gráfico "Desempenho do Sistema"
                if (modoCompartilhado) {
                    SystemPerformanceChart(
                        uiState.chartPoints, uiState.filters.metric.key, uiState.selectedChartPeriod,
                        viewModel::onMetricSelected, viewModel::onChartPeriodSelected, uiState.chart,
                        viewModel::retryChart
                    )
                } else DashboardChart(
                    points = uiState.chartPoints,
                    selectedMetric = uiState.filters.metric.key,
                    selectedPeriod = uiState.selectedChartPeriod,
                    onMetricSelected = { viewModel.onMetricSelected(it) },
                    onPeriodSelected = { viewModel.onChartPeriodSelected(it) },
                    state = uiState.chart,
                    onRetry = viewModel::retryChart,
                    demonstracao = uiState.simulated,
                    onToggleDemo = viewModel::alternarDemonstracao
                )

                RecentAlarmsSection(
                    alarms = uiState.alarms,
                    onSeeAllClick = { onNavigate("Alarmes") }
                )

                IndicadoresEnergeticosSection(periodText = uiState.filters.period)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
