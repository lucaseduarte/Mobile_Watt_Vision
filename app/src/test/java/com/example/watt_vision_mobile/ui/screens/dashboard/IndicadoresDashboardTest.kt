package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.data.api.ChartPointDto
import com.example.watt_vision_mobile.model.ConsumoDto
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.model.TarifaDto
import org.junit.Assert.assertTrue
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class IndicadoresDashboardTest {
    @Test fun comparaHojeComMesmoHorarioDeOntem() {
        val agora = LocalDateTime.parse("2026-11-25T12:00:00")
        val periodo = requireNotNull(ChartLogic.period("Hoje", agora))
        val anterior = periodoAnterior(periodo, "Hoje", agora)
        val linhas = listOf(
            leitura("2026-11-24T10:00:00", 200.0),
            leitura("2026-11-24T16:00:00", 400.0),
            leitura("2026-11-25T11:00:00", 230.0),
            leitura("2026-11-25T08:00:00", 210.0)
        )
        val indicadores = montarIndicadores(
            linhas, periodo, anterior, ChartMetric.VOLTAGE,
            ChartCatalog(pontos = listOf(ChartPointDto(1, null, null, null, "Ponto do motor"))),
            ZoneOffset.UTC, null
        )
        assertEquals(listOf("230,00 V", "220,00 V", "230,00 V", "210,00 V", "—", "10,00%"),
            indicadores.map { it.valor })
        assertEquals("Registrada às 11:00", indicadores[0].detalhe)
        assertEquals("Ponto do motor", indicadores[2].detalhe)
    }

    @Test fun diferenciaSemConsultaDeNenhumDispositivoNoStatus() {
        val semConsulta = cardsDashboard(null, null)
        val comConsulta = cardsDashboard(null, listOf("online", "ativo", "offline", "instavel"))
        assertEquals(listOf("—", "—", "—", "—"), semConsulta.map { it.value })
        assertEquals(listOf("—", "2", "1", "—"), comConsulta.map { it.value })
    }

    @Test fun preservaCustoAbaixoDeUmCentavoEIdentificaFaltaDeComparacao() {
        val agora = LocalDateTime.parse("2026-11-26T12:00:00")
        val periodo = requireNotNull(ChartLogic.period("Hoje", agora))
        val indicadores = montarIndicadores(
            listOf(leitura("2026-11-26T11:00:00", 220.0)),
            periodo, periodoAnterior(periodo, "Hoje", agora), ChartMetric.VOLTAGE,
            ChartCatalog(), ZoneOffset.UTC, consumoDoDia()
        )
        assertTrue(indicadores[4].valor.endsWith("0,0027"))
        assertEquals("—", indicadores[5].valor)
        assertEquals("Sem leituras no período anterior", indicadores[5].detalhe)
    }

    @Test fun usaApenasOContadorDaJanelaDeSeisHoras() {
        val agora = LocalDateTime.parse("2026-11-26T12:00:00")
        val periodo = requireNotNull(ChartLogic.period("6 horas", agora))
        val linhas = listOf(
            leitura("2026-11-26T01:00:00", 220.0).copy(energiaAcumuladaRaw = 7.0),
            leitura("2026-11-26T07:00:00", 220.0).copy(energiaAcumuladaRaw = 10.000),
            leitura("2026-11-26T11:00:00", 220.0).copy(energiaAcumuladaRaw = 10.006)
        )
        val indicadores = montarIndicadores(
            linhas, periodo, periodoAnterior(periodo, "6 horas", agora), ChartMetric.VOLTAGE,
            ChartCatalog(), ZoneOffset.UTC, consumoDoDia(), calcularConsumoNaJanela = true
        )
        assertTrue(indicadores[4].valor.endsWith("0,0054"))
        assertEquals("0,00%", indicadores[5].valor)
    }

    private fun consumoDoDia() =
        ConsumoDto(1, null, null, 2, null, null, 0.003, TarifaDto(1, null, 0.895), 0.0, "BRL")

    private fun leitura(hora: String, tensao: Double) =
        MedicaoDto(id_pzem = 1, id_ponto_medicao = 1, data_hora = hora, tensaoRaw = tensao)
}
