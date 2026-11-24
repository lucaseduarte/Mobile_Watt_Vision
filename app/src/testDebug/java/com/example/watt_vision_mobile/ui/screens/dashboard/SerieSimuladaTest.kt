package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.ui.screens.medicoes.fonteSerieSimulada
import com.example.watt_vision_mobile.ui.screens.medicoes.janelaMedicoes
import com.example.watt_vision_mobile.ui.screens.medicoes.montarRegistros
import com.example.watt_vision_mobile.ui.screens.medicoes.montarSerie
import com.example.watt_vision_mobile.model.MedicaoDto
import com.google.gson.Gson
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SerieSimuladaTest {
    @Test fun numericAndStringIdsRemainExactAndSortEqualTimestamps() {
        val gson = Gson()
        val maisAntiga = gson.fromJson(
            """{"id":9007199254740993,"data_hora":"2026-11-23T11:30:00Z","potencia_ativa":1250}""",
            MedicaoDto::class.java
        )
        val maisRecente = gson.fromJson(
            """{"id_medicao":"9007199254740994","data_hora":"2026-11-23T11:30:00Z","potencia_ativa":1375}""",
            MedicaoDto::class.java
        )
        val agora = ZonedDateTime.parse("2026-11-23T12:00:00Z")
        val janela = janelaMedicoes("Hoje", agora)
        val linhas = listOf(maisRecente, maisAntiga)
        val registros = montarRegistros(linhas, janela, agora.zone, null)
        assertEquals(listOf(9_007_199_254_740_994L, 9_007_199_254_740_993L), registros.map { it.dados.id_medicao })
        val serie = montarSerie(linhas, ChartMetric.POWER, janela, agora.zone, null)
        assertEquals(1.375, serie.amostras.single().valor, 0.0)
    }

    @Test fun transportUsesSameReadingsForDashboardChartAndMeasurementRecords() = runBlocking {
        val fonte = requireNotNull(fonteSerieSimulada())
        val filtros = DashboardFilterState(period = "Ontem")
        val (pzem, estado) = fonte.catalogo.resolve(filtros)
        assertNull(estado)
        val agora = ZonedDateTime.now()
        val periodo = requireNotNull(ChartLogic.period(filtros.period, agora.toLocalDateTime()))
        val janela = janelaMedicoes(filtros.period, agora)
        val linhas = fonte.consultar(requireNotNull(pzem), janela.horas)
        val pontos = ChartLogic.map(linhas, ChartMetric.POWER, periodo, agora.zone)
        val serie = montarSerie(linhas, ChartMetric.POWER, janela, agora.zone, null)
        val registros = montarRegistros(linhas, janela, agora.zone, null)

        assertTrue(pontos.isNotEmpty())
        assertEquals(pontos.map { requireNotNull(it.valor) / 1_000 }, serie.amostras.map { it.valor })
        assertEquals(serie.amostras.last().tempo, registros.first().tempo)
        assertEquals(pontos.last().valor, registros.first().dados.potencia_ativa)
        assertTrue(linhas.all { it.id_pzem == pzem })
    }
}
