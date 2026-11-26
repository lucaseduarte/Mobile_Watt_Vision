package com.example.watt_vision_mobile.ui.screens.relatorios

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

class RelatoriosCalculosTest {
    private val agora = ZonedDateTime.parse("2026-11-20T18:00:00-03:00[America/Sao_Paulo]")
    private val energia = GrandezaRelatorio.ENERGIA
    private val potencia = GrandezaRelatorio.POTENCIA

    private fun leitura(horas: Long, valor: Double, medidor: String = "pzem-a", local: String = "a") =
        LeituraRelatorio(medidor, local, "setor-$local", agora.minusHours(horas).toInstant(),
            mapOf(energia to valor, potencia to 2.0))

    private fun relatorio(vararg registros: LeituraRelatorio, filtros: FiltrosRelatorio = FiltrosRelatorio()) =
        montarRelatorio(FonteRelatorio(emptyList(), emptyList(), registros.toList()), filtros, BigDecimal("0.80"), agora)

    @Test
    fun somaDeltasPorMedidorSemCobrarReinicio() {
        val r = relatorio(
            leitura(5, 100.0), leitura(4, 110.0), leitura(3, 4.0), leitura(2, 5.0),
            leitura(5, 200.0, "pzem-b"), leitura(2, 203.0, "pzem-b")
        )

        assertEquals(14.0, r.consumo!!, 0.0)
        assertEquals(0, BigDecimal("11.20").compareTo(r.custo!!))
        assertEquals(6, r.dias.single().leituras)
    }

    @Test
    fun grandezaNaoMudaResumoEPeriodoExcluiMeiaNoiteDeHojeEmOntem() {
        val r = relatorio(
            leitura(24, 10.0), leitura(19, 13.0), leitura(18, 15.0), leitura(2, 18.0),
            filtros = FiltrosRelatorio(periodo = PeriodoRelatorio.ONTEM, grandeza = potencia)
        )

        assertEquals(2, r.leituras)
        assertEquals(3.0, r.consumo!!, 0.0)
        assertEquals(2.0, r.dias.single().valores.getValue(potencia), 0.0)
    }

    @Test
    fun amostragemNaoReduzContagemAgregada() {
        val registros = (0..130).map { i ->
            leitura(0, i.toDouble()).copy(instante = agora.minusMinutes(130L - i).toInstant())
        }
        val r = relatorio(*registros.toTypedArray())

        assertEquals(120, r.pontosExibidos)
        assertEquals(131, r.leituras)
        assertEquals(registros, r.registros)
        assertEquals(131, r.series.getValue(energia).quantidade)
        assertEquals(130.0, r.consumo!!, 0.0)
    }

    @Test
    fun escalaRecenteDestacaVariacaoAposLacunaSemAlterarHistorico() {
        val r = relatorio(
            leitura(4, 10.0).copy(valores = mapOf(potencia to 10.0)),
            leitura(0, 0.0).copy(instante = agora.minusSeconds(20).toInstant(), valores = mapOf(potencia to 2.089)),
            leitura(0, 0.0).copy(instante = agora.minusSeconds(10).toInstant(), valores = mapOf(potencia to 2.090)),
            leitura(0, 0.0).copy(valores = mapOf(potencia to 2.091))
        )
        val serie = r.series.getValue(potencia)
        assertEquals(4, serie.pontos.size)
        assertEquals(listOf(2.089, 2.090, 2.091), serie.pontosRecentes.map { it.valor })
        val escala = escalaAutomatica(serie.pontosRecentes, potencia)
        assertEquals(2.0887, escala.minimo, 0.000001)
        assertEquals(2.0913, escala.maximo, 0.000001)
    }

    @Test
    fun equipamentoRestringeLeiturasConsumoEGraficos() {
        val r = relatorio(
            leitura(4, 10.0).copy(equipamentoId = "motor"),
            leitura(2, 13.0).copy(equipamentoId = "motor"),
            leitura(1, 200.0, "pzem-b").copy(equipamentoId = "compressor"),
            filtros = FiltrosRelatorio(equipamentoId = "motor")
        )

        assertEquals(2, r.leituras)
        assertEquals(3.0, r.consumo!!, 0.0)
        assertEquals(2, r.series.getValue(energia).quantidade)
    }

    @Test
    fun distingueGradeCompletaIncompletaIndeterminadaESemDados() {
        val periodo = FiltrosRelatorio(periodo = PeriodoRelatorio.SEIS_HORAS)
        val equipamento = EquipamentoRelatorio("motor", "Motor", "a", "setor-a")
        val linhas = (6L downTo 0L).map { leitura(it, 106.0 - it).copy(equipamentoId = "motor") }
        val fonte = FonteRelatorio(emptyList(), emptyList(), linhas, listOf(equipamento),
            medidores = mapOf("pzem-a" to equipamento), cadenciaSegundos = 3_600)
        fun montar(f: FonteRelatorio) = montarRelatorio(f, periodo, BigDecimal("0.80"), agora)

        assertEquals(CoberturaRelatorio.COMPROVADA, montar(fonte).cobertura)
        val comLacuna = montar(fonte.copy(leituras = linhas.filterIndexed { i, _ -> i != 3 }))
        assertEquals(CoberturaRelatorio.PARCIAL, comLacuna.cobertura)
        assertEquals(4.0, comLacuna.consumo!!, 0.0)
        assertFalse(comLacuna.podeExportar)
        assertEquals(CoberturaRelatorio.INDETERMINADA, montar(fonte.copy(cadenciaSegundos = null)).cobertura)
        assertEquals(CoberturaRelatorio.SEM_DADOS, montar(fonte.copy(leituras = emptyList())).cobertura)
        assertFalse(montar(fonte.copy(quantidadeRecebida = 9_999)).limiteAtingido)
        val noLimite = montar(fonte.copy(quantidadeRecebida = 10_000))
        assertTrue(noLimite.limiteAtingido)
        assertEquals(CoberturaRelatorio.PARCIAL, noLimite.cobertura)
    }

    @Test
    fun desempataMesmoInstantePorIdEAmostraInvalidaInterrompeDeltas() {
        val r = relatorio(
            leitura(1, 110.0),
            leitura(3, 5.0).copy(id = 5_000_000_000L),
            leitura(3, 102.0).copy(id = 5_000_000_001L),
            leitura(2, Double.NaN),
            leitura(4, 100.0)
        )
        assertEquals(4, r.leituras)
        assertEquals(102.0, r.registros[1].valores.getValue(energia), 0.0)
        assertFalse(energia in r.registros[2].valores)
        assertEquals(2.0, r.registros[2].valores.getValue(potencia), 0.0)
        assertEquals(2.0, r.consumo!!, 0.0)
        assertEquals(CoberturaRelatorio.PARCIAL, r.cobertura)
        assertFalse(r.podeExportar)
    }

    @Test
    fun recorteDeSeisHorasCruzaMeiaNoiteComLimitesInclusivos() {
        val meiaNoite = agora.toLocalDate().atStartOfDay(agora.zone)
        val referencia = meiaNoite.plusHours(2)
        val base = leitura(0, 99.0)
        val fonte = FonteRelatorio(emptyList(), emptyList(), listOf(
            base.copy(instante = referencia.minusHours(6).minusSeconds(1).toInstant()),
            base.copy(instante = referencia.minusHours(6).toInstant()),
            base.copy(instante = referencia.toInstant()),
            base.copy(instante = referencia.plusSeconds(1).toInstant())
        ))
        val r = montarRelatorio(fonte, FiltrosRelatorio(periodo = PeriodoRelatorio.SEIS_HORAS),
            BigDecimal("0.80"), referencia)
        assertEquals(2, r.leituras)
        assertEquals(listOf(meiaNoite.toLocalDate(), meiaNoite.toLocalDate().minusDays(1)), r.dias.map { it.dia })
        assertEquals(0.0, r.consumo!!, 0.0)
    }

    @Test
    fun primeiraLeituraNaoViraConsumoZero() {
        val r = relatorio(leitura(1, 35.0), leitura(7, 15.0),
            filtros = FiltrosRelatorio(periodo = PeriodoRelatorio.SEIS_HORAS))

        assertEquals(1, r.leituras)
        assertNull(r.consumo)
        assertNull(r.dias.single().custo)
    }
}
