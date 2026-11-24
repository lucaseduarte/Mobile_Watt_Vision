package com.example.watt_vision_mobile.ui.screens.relatorios

import android.content.Context
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.medicoes.ZONA_SERIE_REAL
import com.example.watt_vision_mobile.ui.screens.medicoes.carregarFonteBackend
import com.example.watt_vision_mobile.ui.screens.medicoes.instanteMedicao
import com.example.watt_vision_mobile.ui.screens.medicoes.valorExibidoMedicao

internal suspend fun carregarRelatorioBackend(contexto: Context): FonteRelatorio {
    val fonte = carregarFonteBackend(contexto)
    val catalogo = fonte.catalogo
    val locais = catalogo.locais.mapNotNull { local ->
        LocalRelatorio(local.id?.toString() ?: return@mapNotNull null, local.nome.orEmpty())
    }
    val setores = catalogo.setores.mapNotNull { setor ->
        SetorRelatorio(setor.id?.toString() ?: return@mapNotNull null, setor.nome.orEmpty(),
            setor.id_local?.toString() ?: return@mapNotNull null)
    }
    val setoresPorId = setores.associateBy { it.id }
    val equipamentos = catalogo.equipamentos.mapNotNull { equipamento ->
        val setor = setoresPorId[equipamento.id_setor?.toString()] ?: return@mapNotNull null
        EquipamentoRelatorio(equipamento.id?.toString() ?: return@mapNotNull null,
            equipamento.nome.orEmpty(), setor.localId, setor.id)
    }
    val equipamentosPorId = equipamentos.associateBy { it.id }
    val pontosPorId = catalogo.pontos.associateBy { it.id }
    val grandezas = mapOf(
        GrandezaRelatorio.ENERGIA to ChartMetric.ENERGY,
        GrandezaRelatorio.POTENCIA to ChartMetric.POWER,
        GrandezaRelatorio.TENSAO to ChartMetric.VOLTAGE,
        GrandezaRelatorio.CORRENTE to ChartMetric.CURRENT,
        GrandezaRelatorio.FREQUENCIA to ChartMetric.FREQUENCY,
        GrandezaRelatorio.FATOR to ChartMetric.POWER_FACTOR
    )
    val leituras = mutableListOf<LeituraRelatorio>()
    var invalidos = false
    var maiorSerie = 0
    for (sensor in catalogo.sensores) {
        val id = requireNotNull(sensor.id)
        val linhas = fonte.consultar(id, 720)
        maiorSerie = maxOf(maiorSerie, linhas.size)
        for (linha in linhas) {
            val instante = instanteMedicao(linha.data_hora, ZONA_SERIE_REAL)
            val ponto = pontosPorId[linha.id_ponto_medicao]
            if (instante == null || ponto == null || linha.id_pzem != id || linha.id_medicao == null) {
                invalidos = true
                continue
            }
            val equipamento = equipamentosPorId[ponto.id_equipamento?.toString()]
            val setor = setoresPorId[ponto.id_setor?.toString() ?: equipamento?.setorId]
            val local = ponto.id_local?.toString() ?: setor?.localId.orEmpty()
            val valores = grandezas.mapNotNull { (grandeza, metrica) ->
                valorExibidoMedicao(metrica, linha)?.let { grandeza to it }
            }.toMap()
            // Cada vínculo tem seu próprio contador, inclusive após remanejamento do sensor.
            leituras += LeituraRelatorio("$id/${ponto.id}", local, setor?.id.orEmpty(), instante,
                valores, equipamento?.id, linha.id_medicao)
        }
    }
    return FonteRelatorio(locais, setores, leituras, equipamentos,
        quantidadeRecebida = maiorSerie, registrosInvalidos = invalidos)
}
