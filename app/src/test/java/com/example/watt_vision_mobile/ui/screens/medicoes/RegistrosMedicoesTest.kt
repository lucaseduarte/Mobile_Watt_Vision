package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrosMedicoesTest {
    @Test fun filtraEscopoEOrdenaSemDescartarLeiturasDoMesmoSegundo() {
        val zona = ZoneId.of("America/Sao_Paulo")
        val janela = janelaMedicoes("Hoje", ZonedDateTime.parse("2026-11-19T12:00:00-03:00"))
        fun leitura(id: Int, data: String, ponto: Int = 1) =
            MedicaoDto(id_medicao = id.toLong(), data_hora = data, id_ponto_medicao = ponto)

        val primeira = leitura(1, "2026-11-19 08:59:59")
        val segunda = leitura(2, "2026-11-19T12:00:00Z")
        val terceira = leitura(3, "2026-11-19T09:00:00-03:00")
        val linhas = listOf(
            segunda, leitura(4, "inválida"), leitura(5, "2026-11-18 23:59:59"),
            leitura(6, "2026-11-19 09:00:01", ponto = 2), primeira, terceira
        )

        val registros = montarRegistros(linhas, janela, zona, setOf(1))
        assertEquals(listOf(3L, 2L, 1L), registros.map { it.dados.id_medicao })
        assertEquals(registros[0].tempo, registros[1].tempo)
    }

    @Test fun formataTodasAsGrandezasSemTrocarAusenciasPorZero() {
        val dados = MedicaoDto(
            tensaoRaw = 221.1, correnteRaw = "9.8", potenciaAtivaRaw = 2100.01,
            energiaAcumuladaRaw = 15.4, frequenciaRaw = 59.991, fatorPotenciaRaw = .98
        )
        assertEquals(
            listOf("221,10", "9,800", "2,10001", "15,400", "59,99", "0,980"),
            ChartMetric.entries.map { formatarValorBruto(it, dados) }
        )
        assertEquals("—", formatarValorBruto(ChartMetric.CURRENT, dados.copy(correnteRaw = Double.NaN)))
        assertEquals("—", formatarValorBruto(ChartMetric.ENERGY, MedicaoDto()))
    }

    @Test fun paginaSemLimitarOConjuntoCarregado() {
        assertEquals(listOf(0, 1, 1, 2, 4, 1000),
            listOf(0, 1, 10, 11, 34, 10_000).map(::totalPaginasRegistros))
    }
}
