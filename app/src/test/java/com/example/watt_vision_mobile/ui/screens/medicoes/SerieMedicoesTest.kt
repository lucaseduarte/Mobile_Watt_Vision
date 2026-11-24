package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class SerieMedicoesTest {
    private val zona = ZoneId.of("America/Sao_Paulo")
    private val agora = ZonedDateTime.parse("2026-11-19T12:00-03:00[America/Sao_Paulo]")
    private val inicio = Instant.parse("2026-11-19T11:00:00Z")

    private fun leitura(minuto: Long, valor: Any?, id: Int = minuto.toInt(), ponto: Int = 1) = MedicaoDto(
        id_medicao = id.toLong(),
        id_ponto_medicao = ponto,
        data_hora = inicio.plusSeconds(minuto * 60).toString(),
        tensaoRaw = valor,
        potenciaAtivaRaw = valor,
        energiaAcumuladaRaw = valor
    )

    private fun serie(linhas: List<MedicaoDto>, metrica: ChartMetric = ChartMetric.VOLTAGE, pontos: Set<Int>? = null) =
        montarSerie(linhas, metrica, janelaMedicoes("Hoje", agora), zona, pontos)

    @Test fun ordenaPorInstanteEDeduplicaPeloMaiorIdValido() {
        val primeira = leitura(0, "219.5").copy(data_hora = "2026-11-19 08:00:00")
        val linhas = listOf(leitura(1, 222), leitura(0, null, 12), leitura(0, 220, 11), primeira)
        val amostras = serie(linhas).amostras
        assertEquals(listOf(220.0, 222.0), amostras.map { it.valor })
        assertEquals(inicio, amostras.first().tempo)
    }

    @Test fun interrompeAposNulosMudancasDePontoELacunas() {
        val linhas = listOf(leitura(0, 220), leitura(1, 221), leitura(2, Double.NaN),
            leitura(3, 223), leitura(4, 224, ponto = 2), leitura(50, 225, ponto = 2))
        assertEquals(listOf(2, 1, 1, 1), serie(linhas).trechos.map { it.size })
        assertEquals(listOf(220.0, 221.0, 223.0), serie(linhas, pontos = setOf(1)).amostras.map { it.valor })
        assertTrue(serie(listOf(leitura(0, "inválido"), leitura(1, 1).copy(data_hora = "inválida"))).amostras.isEmpty())
    }

    @Test fun potenciaEhConvertidaParaKwEEnergiaContinuaAcumulada() {
        assertEquals(1.25, serie(listOf(leitura(0, 1250)), ChartMetric.POWER).amostras.single().valor, 0.0)
        val energia = serie(listOf(leitura(0, 12.5), leitura(1, 13), leitura(2, 2)), ChartMetric.ENERGY)
        assertEquals(listOf(12.5, 13.0, 2.0), energia.amostras.map { it.valor })
        assertEquals(listOf(2, 1), energia.trechos.map { it.size })
    }

    @Test fun trataJanelasComCalendarioELimiteDoContrato() {
        val ontem = janelaMedicoes("Ontem", agora)
        assertTrue(ontem.contem(Instant.parse("2026-11-18T03:00:00Z")))
        assertFalse(ontem.contem(Instant.parse("2026-11-19T03:00:00Z")))
        assertEquals(720, janelaMedicoes("Últimos 30 dias", agora).horas)
    }

    @Test fun escalaConstantePossuiMargemEFormatacaoBrasileira() {
        val escala = escalaMedicoes(listOf(AmostraMedicao(inicio, .997)))
        assertTrue(escala.min < .997 && escala.max > .997)
        assertEquals(5, escala.marcas.map { numeroMedicao(it, escala.casas) }.distinct().size)
        assertEquals("0,997", numeroMedicao(.997, 3))
        assertEquals("19/11, 08:00", formatoEixoMedicoes.withZone(zona).format(inicio))
    }

    @Test fun curvaSegueTempoIrregularSemCriarExtremos() {
        val valores = listOf(2.0, 3.0, 17.0, 17.0, 1.0, 2.0)
        val controles = controlesMonotonicos(listOf(0.0, 1.0, 5.0, 9.0, 10.0, 30.0), valores)
        controles.forEachIndexed { i, (c1, c2) ->
            val a = valores[i]
            val b = valores[i + 1]
            var anterior = a
            for (passo in 0..100) {
                val t = passo / 100.0
                val r = 1 - t
                val y = r * r * r * a + 3 * r * r * t * c1 + 3 * r * t * t * c2 + t * t * t * b
                assertTrue(y in (minOf(a, b) - 1e-10)..(maxOf(a, b) + 1e-10))
                assertTrue((b - a) * (y - anterior) >= -1e-10)
                anterior = y
            }
        }
    }
}
