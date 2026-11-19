package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import java.time.Instant
import java.time.ZoneId

internal data class RegistroMedicao(val tempo: Instant, val dados: MedicaoDto)

internal fun montarRegistros(
    linhas: List<MedicaoDto>,
    janela: JanelaMedicoes,
    zona: ZoneId,
    pontos: Set<Int>?
): List<RegistroMedicao> = linhas.mapNotNull { linha ->
    if (pontos != null && linha.id_ponto_medicao !in pontos) return@mapNotNull null
    val tempo = instanteMedicao(linha.data_hora, zona)?.takeIf(janela::contem) ?: return@mapNotNull null
    RegistroMedicao(tempo, linha)
}.sortedWith(compareByDescending<RegistroMedicao> { it.tempo }.thenByDescending { it.dados.id_medicao })

internal fun formatarValorBruto(metrica: ChartMetric, dados: MedicaoDto): String {
    val casas = when (metrica) {
        ChartMetric.VOLTAGE, ChartMetric.FREQUENCY -> 2
        ChartMetric.CURRENT, ChartMetric.ENERGY, ChartMetric.POWER_FACTOR -> 3
        // O contrato recebe W com duas casas. Cinco em kW preservam essa precisão.
        ChartMetric.POWER -> 5
    }
    return valorExibidoMedicao(metrica, dados)?.let { numeroMedicao(it, casas) } ?: "—"
}

internal const val REGISTROS_POR_PAGINA = 10

internal fun totalPaginasRegistros(total: Int): Int =
    (total + REGISTROS_POR_PAGINA - 1) / REGISTROS_POR_PAGINA
