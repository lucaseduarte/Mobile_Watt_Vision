package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.data.repository.SessionCredential
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.dashboard.cartoesDashboardReal
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.net.ConnectException
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SerieRealTest {
    private val atualizadaEm = Instant.parse("2026-11-23T23:26:36Z")

    private class Sessao {
        var credencial: SessionCredential? = SessionCredential("fixture")
        val mudancas = MutableStateFlow(0)
        fun trocar(nova: SessionCredential?) {
            credencial = nova
            mudancas.value++
        }
    }

    private fun testar(
        habilitada: Boolean = true,
        consultar: suspend (Int, Int) -> Result<List<MedicaoDto>>,
        bloco: suspend CoroutineScope.(FonteSerieReal, Sessao) -> Unit
    ) = runBlocking {
        withTimeout(5_000) {
            val escopo = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[kotlinx.coroutines.Job]))
            val sessao = Sessao()
            val fonte = FonteSerieReal(escopo, habilitada, { sessao.credencial }, sessao.mudancas, consultar) {
                atualizadaEm
            }
            try {
                bloco(fonte, sessao)
            } finally {
                escopo.cancel()
            }
        }
    }

    private fun leitura(id: Long, horario: String = "2026-11-19 18:20:46") =
        MedicaoDto(id, 1, 1, horario, potenciaAtivaRaw = "2100.00", energiaAcumuladaRaw = "15.400")

    private suspend fun FonteSerieReal.aguardarResultado() = estado.first {
        it.resultado != ResultadoSerieReal.Carregando && it.resultado != ResultadoSerieReal.NaoIniciado
    }.resultado

    @Test fun ordenaDecimalENuloSemPerderIdLongECompartilhaResultado() {
        val dto = Gson().fromJson(
            """{"id":9007199254740993,"id_pzem":1,"id_ponto_medicao":1,
            "data_hora":"2026-11-19 18:20:46","tensao":null,"corrente":"0.000",
            "potencia_ativa":"2100.00","energia_acumulada":"15.400"}""",
            MedicaoDto::class.java
        )
        var chamadas = 0
        testar(consultar = { id, horas ->
            chamadas++
            assertEquals(1, id)
            assertEquals(24, horas)
            Result.success(listOf(dto, leitura(529), leitura(528, "2026-11-19 18:00:00")))
        }) { fonte, _ ->
            assertFalse(fonte.estado.value.modoReal)
            fonte.selecionarModoReal(true)
            assertEquals(0, chamadas)
            fonte.atualizar()
            fonte.atualizar()
            val resultado = fonte.aguardarResultado() as ResultadoSerieReal.ComDados
            val serie = resultado.serie
            assertEquals(listOf(528L, 529L, 9007199254740993L), serie.registros.map { it.dados.id_medicao })
            assertEquals("2026-11-19 18:00:00", serie.primeiroTimestamp)
            assertEquals("2026-11-19 18:20:46", serie.ultimoTimestamp)
            assertEquals(Instant.parse("2026-11-19T21:20:46Z"), serie.registros.last().tempo)
            assertEquals(atualizadaEm, serie.atualizadaEm)
            assertEquals(3, serie.quantidade)
            assertNull(dto.tensao)
            assertEquals(0.0, requireNotNull(dto.corrente), 0.0)
            assertEquals("2100.00", dto.potenciaAtivaRaw)
            assertEquals(2.10, requireNotNull(valorExibidoMedicao(ChartMetric.POWER, dto)), 0.0)
            // Um segundo consumidor da sessão observa o mesmo resultado, sem uma segunda consulta.
            assertSame(resultado, fonte.estado.first().resultado)
            assertEquals(1, chamadas)
        }
    }

    @Test fun vazioEValidoECarregaHorarioDaConsulta() =
        testar(consultar = { _, _ -> Result.success(emptyList()) }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            assertEquals(ResultadoSerieReal.Vazio(atualizadaEm), fonte.aguardarResultado())
        }

    @Test fun distingueHttpConexaoEEnvelopeInvalido() {
        val casos = listOf(
            ConnectException() to ResultadoSerieReal.ErroConexao,
            HttpException(Response.error<Any>(401, "".toResponseBody())) to ResultadoSerieReal.ErroHttp(401),
            HttpException(Response.error<Any>(403, "".toResponseBody())) to ResultadoSerieReal.AcessoNegado,
            JsonSyntaxException("fixture") to ResultadoSerieReal.RespostaInvalida,
            IllegalStateException("Série ausente na resposta") to ResultadoSerieReal.RespostaInvalida
        )
        for ((erro, esperado) in casos) {
            testar(consultar = { _, _ -> Result.failure(erro) }) { fonte, _ ->
                fonte.selecionarModoReal(true)
                fonte.atualizar()
                assertEquals(esperado, fonte.aguardarResultado())
            }
        }
    }

    @Test fun janelaSubstituidaNaoPublicaRespostaAtrasada() {
        val iniciada = CompletableDeferred<Unit>()
        val liberar = CompletableDeferred<Unit>()
        val encerrada = CompletableDeferred<Unit>()
        testar(consultar = { _, horas ->
            if (horas == 24) {
                try {
                    withContext(NonCancellable) {
                        iniciada.complete(Unit)
                        liberar.await()
                    }
                } finally {
                    encerrada.complete(Unit)
                }
            }
            Result.success(listOf(leitura(horas.toLong())))
        }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            iniciada.await()
            fonte.selecionarJanela(100)
            assertEquals(24, fonte.estado.value.horas)
            fonte.selecionarJanela(168)
            fonte.atualizar()
            val resultado = fonte.aguardarResultado() as ResultadoSerieReal.ComDados
            assertEquals(168L, resultado.serie.registros.single().dados.id_medicao)
            liberar.complete(Unit)
            encerrada.await()
            yield()
            assertSame(resultado, fonte.estado.value.resultado)
        }
    }

    @Test fun logoutETrocaDescartam200DaSessaoAntiga() {
        val iniciada = CompletableDeferred<Unit>()
        val liberar = CompletableDeferred<Unit>()
        val encerrada = CompletableDeferred<Unit>()
        var chamadas = 0
        testar(consultar = { _, _ ->
            chamadas++
            try {
                withContext(NonCancellable) {
                    iniciada.complete(Unit)
                    liberar.await()
                }
            } finally {
                encerrada.complete(Unit)
            }
            Result.success(listOf(leitura(3175)))
        }) { fonte, sessao ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            iniciada.await()
            sessao.trocar(null)
            fonte.estado.first { !it.modoReal }
            sessao.trocar(SessionCredential("fixture"))
            liberar.complete(Unit)
            encerrada.await()
            yield()
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            assertEquals(EstadoSerieReal(), fonte.estado.value)
            assertEquals(1, chamadas)
        }
    }

    @Test fun alternarModosLimpaDadosEExigeAtualizacaoManual() {
        var chamadas = 0
        testar(consultar = { _, _ ->
            chamadas++
            Result.success(listOf(leitura(3175)))
        }) { fonte, _ ->
            fonte.atualizar()
            assertEquals(0, chamadas)
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            assertTrue(fonte.aguardarResultado() is ResultadoSerieReal.ComDados)
            fonte.selecionarModoReal(false)
            fonte.atualizar()
            assertEquals(EstadoSerieReal(), fonte.estado.value)
            fonte.selecionarModoReal(true)
            assertEquals(ResultadoSerieReal.NaoIniciado, fonte.estado.value.resultado)
            assertEquals(1, chamadas)
        }
    }

    @Test fun alternarParaSimuladoCancelaConsultaEmAndamento() {
        val iniciou = CompletableDeferred<Unit>()
        val cancelou = CompletableDeferred<Unit>()
        testar(consultar = { _, _ ->
            try {
                iniciou.complete(Unit)
                CompletableDeferred<Unit>().await()
                error("A consulta deveria ter sido cancelada")
            } finally {
                cancelou.complete(Unit)
            }
        }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            iniciou.await()
            fonte.selecionarModoReal(false)
            cancelou.await()
            fonte.selecionarModoReal(true)
            assertEquals(ResultadoSerieReal.NaoIniciado, fonte.estado.value.resultado)
        }
    }

    @Test fun marcaQuedaDoContadorSemAlterarValoresBrutos() {
        val anterior = leitura(529, "2026-11-19 18:20:29").copy(energiaAcumuladaRaw = "2386.951")
        val atual = leitura(3175)
        testar(consultar = { _, _ -> Result.success(listOf(atual, anterior)) }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            val serie = (fonte.aguardarResultado() as ResultadoSerieReal.ComDados).serie
            assertTrue(serie.houveQueda)
            assertEquals("15.400", serie.registros.last().dados.energiaAcumuladaRaw)
        }
    }

    @Test fun dashboardEMedicoesLeemMesmaSerieSemReconsultarAoTrocarConsumidor() {
        var chamadas = 0
        val anterior = leitura(529, "2026-11-19 18:20:29").copy(energiaAcumuladaRaw = "2386.951")
        val ultima = leitura(3175).copy(
            tensaoRaw = "221.10", frequenciaRaw = "60.00", fatorPotenciaRaw = "0.970"
        )
        testar(consultar = { pzem, horas ->
            assertEquals(1, pzem)
            assertEquals(168, horas)
            when (++chamadas) {
                1 -> Result.success(listOf(ultima, anterior))
                2 -> Result.success(emptyList())
                else -> Result.failure(ConnectException())
            }
        }) { fonte, sessao ->
            fonte.selecionarModoReal(true)
            fonte.selecionarJanela(168)
            assertEquals(0, chamadas)
            fonte.atualizar()
            fonte.aguardarResultado()

            val estadoMedicoes = fonte.estado.first()
            val tabela = consultaMedicoesReal(estadoMedicoes, ChartMetric.POWER, fonte::atualizar)
            val estadoDashboard = fonte.estado.first()
            assertSame(estadoMedicoes, estadoDashboard)
            assertEquals(1, chamadas)
            assertEquals(3175L, tabela.registros.first().dados.id_medicao)
            assertEquals(listOf(2.1, 2.1), tabela.serie.amostras.map { it.valor })
            assertTrue(tabela.aviso.orEmpty().contains("Queda no contador"))
            val cartoes = cartoesDashboardReal(estadoDashboard.resultado)
            assertEquals(listOf("221,10 V", "2,10 kW", "15,400 kWh", "60,00 Hz", "0,970"), cartoes.map { it.value })
            assertEquals("Energia acumulada", cartoes[2].label)

            fonte.atualizar()
            assertTrue(fonte.aguardarResultado() is ResultadoSerieReal.Vazio)
            assertTrue(cartoesDashboardReal(fonte.estado.value.resultado).all { it.value == "—" })
            fonte.atualizar()
            assertEquals(ResultadoSerieReal.ErroConexao, fonte.aguardarResultado())
            assertTrue(cartoesDashboardReal(fonte.estado.value.resultado).all { it.value == "—" })

            sessao.trocar(null)
            fonte.estado.first { !it.modoReal }
            assertEquals(ResultadoSerieReal.NaoIniciado, fonte.estado.value.resultado)
            assertTrue(cartoesDashboardReal(fonte.estado.value.resultado).all { it.value == "—" })
            assertEquals(3, chamadas)
        }
    }

    @Test fun cartoesUsamSomenteUltimaLinhaEPreservamZeroENulo() {
        val anterior = leitura(529, "2026-11-19 18:20:29").copy(tensaoRaw = "220.00")
        val ultima = leitura(3175).copy(tensaoRaw = null, potenciaAtivaRaw = "0.00")
        testar(consultar = { _, _ -> Result.success(listOf(ultima, anterior)) }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            val cartoes = cartoesDashboardReal(fonte.aguardarResultado())
            assertEquals("—", cartoes[0].value)
            assertEquals("0,00 kW", cartoes[1].value)
            fonte.selecionarModoReal(false)
            assertTrue(cartoesDashboardReal(fonte.estado.value.resultado).all { it.value == "—" })
        }
    }

    @Test fun optInDesligadoNuncaInvocaRepositorio() =
        testar(habilitada = false, consultar = { _, _ -> error("Consulta inesperada") }) { fonte, _ ->
            fonte.selecionarModoReal(true)
            fonte.atualizar()
            assertEquals(EstadoSerieReal(), fonte.estado.value)
        }
}
