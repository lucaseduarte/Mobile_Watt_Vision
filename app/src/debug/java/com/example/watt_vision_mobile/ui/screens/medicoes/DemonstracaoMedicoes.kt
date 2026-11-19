package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.watt_vision_mobile.data.api.ChartEquipmentDto
import com.example.watt_vision_mobile.data.api.ChartLocalDto
import com.example.watt_vision_mobile.data.api.ChartSectorDto
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

private val catalogoDemo = ChartCatalog(
    locais = listOf(ChartLocalDto(1, "Local de demonstração")),
    setores = listOf(ChartSectorDto(1, "Setor de demonstração", id_local = 1)),
    equipamentos = listOf(ChartEquipmentDto(1, "Equipamento de demonstração", id_setor = 1))
)

@Composable
internal fun demonstracaoMedicoes(grandeza: ChartMetric, periodo: String): DemonstracaoMedicoes {
    var ativa by rememberSaveable { mutableStateOf(false) }
    val registros = remember(ativa, periodo) {
        if (ativa) gerarRegistrosDemonstracao(janelaMedicoes(periodo, ZonedDateTime.now())) else emptyList()
    }
    val serie = remember(registros, grandeza) {
        if (!ativa) null else {
            val amostras = registros.asReversed().map {
                AmostraMedicao(it.tempo, requireNotNull(valorExibidoMedicao(grandeza, it.dados)))
            }
            SerieMedicoes(listOf(amostras.take(18), amostras.drop(18)))
        }
    }
    return DemonstracaoMedicoes(serie = serie, catalogo = catalogoDemo, registros = registros) { ativa = !ativa }
}

private fun gerarRegistrosDemonstracao(janela: JanelaMedicoes): List<RegistroMedicao> {
    val intervalo = Duration.between(janela.inicio, janela.fim).toNanos()
    return List(34) { indice ->
        val onda = sin(indice * .55) + cos(indice * 1.2) * .35
        val tempo = janela.inicio.plusNanos(intervalo * (indice + 1) / 35)
        RegistroMedicao(
            tempo,
            MedicaoDto(
                id_medicao = indice + 1,
                data_hora = tempo.toString(),
                tensaoRaw = 220.0 + onda * 3.4,
                correnteRaw = 6.2 + onda * 1.1,
                potenciaAtivaRaw = 1230.0 + onda * 240.0,
                energiaAcumuladaRaw = 84.0 + indice * .16,
                frequenciaRaw = 60.0 + onda * .09,
                fatorPotenciaRaw = .94 + onda * .025
            )
        )
    }.asReversed()
}
