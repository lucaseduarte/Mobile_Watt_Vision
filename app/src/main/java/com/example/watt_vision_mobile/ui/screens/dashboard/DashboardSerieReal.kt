package com.example.watt_vision_mobile.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.screens.dashboard.components.StatCardsSection
import com.example.watt_vision_mobile.ui.screens.medicoes.EstadoSerieReal
import com.example.watt_vision_mobile.ui.screens.medicoes.GraficoMedicoes
import com.example.watt_vision_mobile.ui.screens.medicoes.JANELAS_SERIE_REAL
import com.example.watt_vision_mobile.ui.screens.medicoes.OpcoesMedicao
import com.example.watt_vision_mobile.ui.screens.medicoes.ResumoConsultaReal
import com.example.watt_vision_mobile.ui.screens.medicoes.ResultadoSerieReal
import com.example.watt_vision_mobile.ui.screens.medicoes.SerieRealViewModel
import com.example.watt_vision_mobile.ui.screens.medicoes.consultaMedicoesReal
import com.example.watt_vision_mobile.ui.screens.medicoes.numeroMedicao
import com.example.watt_vision_mobile.ui.screens.medicoes.valorExibidoMedicao
import com.example.watt_vision_mobile.ui.theme.DarkBackground

@Composable
internal fun DashboardComSerieReal(
    viewModel: DashboardViewModel,
    serieReal: SerieRealViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!serieReal.habilitada) {
        DashboardScreen(viewModel, onNavigate, modifier)
        return
    }

    val fonte = serieReal.fonte
    val estado by fonte.estado.collectAsState()
    if (estado.modoReal) {
        DashboardReal(estado, fonte::selecionarJanela, fonte::atualizar, {
            fonte.selecionarModoReal(false)
        }, modifier)
    } else {
        LaunchedEffect(viewModel) {
            if (!viewModel.uiState.value.simulated) viewModel.alternarDemonstracao()
        }
        Column(modifier) {
            OutlinedButton(
                onClick = { fonte.selecionarModoReal(true) },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) { Text("Dados reais do Backend") }
            DashboardScreen(viewModel, onNavigate, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardReal(
    estado: EstadoSerieReal,
    selecionarJanela: (Int) -> Unit,
    atualizar: () -> Unit,
    demonstracao: () -> Unit,
    modifier: Modifier
) {
    var grandeza by rememberSaveable { mutableStateOf(ChartMetric.VOLTAGE) }
    val consulta = remember(estado, grandeza, atualizar) {
        consultaMedicoesReal(estado, grandeza, atualizar)
    }
    val cartoes = remember(estado.resultado) { cartoesDashboardReal(estado.resultado) }

    Column(modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState())) {
        WattVisionScreenHeader(title = "Dashboard", subtitle = "Medições do sensor de referência") {}
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = demonstracao) { Text("Dados simulados") }
            ResumoConsultaReal(estado)
            OpcoesMedicao("Janela solicitada", "${estado.horas} h", JANELAS_SERIE_REAL.map { "$it h" }, true) {
                selecionarJanela(it.substringBefore(' ').toInt())
            }
            OutlinedButton(onClick = atualizar, enabled = !consulta.carregando) { Text("Atualizar") }
            StatCardsSection(cards = cartoes)
            OpcoesMedicao("Grandeza", grandeza.label, ChartMetric.entries.map { it.label }) { escolha ->
                grandeza = ChartMetric.entries.first { it.label == escolha }
            }
            GraficoMedicoes(
                grandeza, if (grandeza == ChartMetric.POWER) "kW" else grandeza.unit,
                "Últimas ${estado.horas} horas", consulta
            )
        }
    }
}

internal fun cartoesDashboardReal(resultado: ResultadoSerieReal): List<StatCardData> {
    val ultima = (resultado as? ResultadoSerieReal.ComDados)?.serie?.registros?.lastOrNull()?.dados

    fun cartao(metrica: ChartMetric, casas: Int, icone: String): StatCardData {
        val unidade = if (metrica == ChartMetric.POWER) "kW" else metrica.unit
        val valor = ultima?.let { valorExibidoMedicao(metrica, it) }?.let {
            listOf(numeroMedicao(it, casas), unidade).filter(String::isNotEmpty).joinToString(" ")
        }
        return StatCardData(
            label = metrica.label,
            value = valor ?: "—",
            detail = if (valor == null) "Dado indisponível" else "Última leitura",
            iconName = icone
        )
    }

    return listOf(
        cartao(ChartMetric.VOLTAGE, 2, "flash"),
        cartao(ChartMetric.POWER, 2, "zap"),
        cartao(ChartMetric.ENERGY, 3, "cpu"),
        cartao(ChartMetric.FREQUENCY, 2, "activity"),
        cartao(ChartMetric.POWER_FACTOR, 3, "activity")
    )
}
