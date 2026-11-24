package com.example.watt_vision_mobile.ui.screens.relatorios

import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.medicoes.MINUTOS_POR_AMOSTRA
import com.example.watt_vision_mobile.ui.screens.medicoes.fonteSerieSimulada
import com.example.watt_vision_mobile.ui.screens.medicoes.instanteMedicao
import com.example.watt_vision_mobile.ui.screens.medicoes.valorExibidoMedicao
import java.time.ZonedDateTime

internal fun fonteDemonstrativaRelatorio(): (suspend (ZonedDateTime) -> FonteRelatorio) = { agora ->
    val fonte = fonteSerieSimulada()
    val catalogo = fonte.catalogo
    val locais = catalogo.locais.map { LocalRelatorio(requireNotNull(it.id).toString(), requireNotNull(it.nome)) }
    val setores = catalogo.setores.map {
        SetorRelatorio(requireNotNull(it.id).toString(), requireNotNull(it.nome), requireNotNull(it.id_local).toString())
    }
    val equipamentos = catalogo.equipamentos.map {
        val setor = setores.single { setor -> setor.id == it.id_setor.toString() }
        EquipamentoRelatorio(requireNotNull(it.id).toString(), requireNotNull(it.nome), setor.localId, setor.id)
    }
    val sensor = catalogo.sensores.single()
    val ponto = catalogo.pontos.single { it.id == sensor.id_ponto_medicao }
    val equipamento = equipamentos.single { it.id == ponto.id_equipamento.toString() }
    val idMedidor = requireNotNull(sensor.id)
    val medidor = idMedidor.toString()
    val linhas = fonte.consultar(idMedidor, 720)
    val leituras = linhas.mapNotNull { linha ->
        if (linha.id_pzem != idMedidor || linha.id_ponto_medicao != ponto.id) return@mapNotNull null
        val instante = instanteMedicao(linha.data_hora, agora.zone) ?: return@mapNotNull null
        val valores = buildMap {
            GrandezaRelatorio.entries.zip(listOf(
                ChartMetric.ENERGY, ChartMetric.POWER, ChartMetric.VOLTAGE,
                ChartMetric.CURRENT, ChartMetric.FREQUENCY, ChartMetric.POWER_FACTOR
            )).forEach { (grandeza, metrica) ->
                valorExibidoMedicao(metrica, linha)?.let { put(grandeza, it) }
            }
        }
        LeituraRelatorio(medidor, equipamento.localId, equipamento.setorId, instante, valores, equipamento.id, linha.id_medicao)
    }
    FonteRelatorio(
        locais, setores, leituras, equipamentos,
        medidores = mapOf(medidor to equipamento),
        cadenciaSegundos = MINUTOS_POR_AMOSTRA * 60,
        quantidadeRecebida = linhas.size,
        registrosInvalidos = linhas.size != leituras.size
    )
}
