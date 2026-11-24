package com.example.watt_vision_mobile.ui.screens.relatorios

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.watt_vision_mobile.ui.screens.medicoes.fonteSerieSimulada
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class RelatoriosAtualizacaoTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val store = ViewModelStore()
    private val agora = ZonedDateTime.parse("2026-11-23T12:00:00Z")
    private val pedidos = Channel<CompletableDeferred<FonteRelatorio>>(Channel.UNLIMITED)
    private val pendentes = CopyOnWriteArrayList<CompletableDeferred<FonteRelatorio>>()
    private val fonte: suspend (ZonedDateTime) -> FonteRelatorio = {
        val resposta = CompletableDeferred<FonteRelatorio>()
        pendentes += resposta
        pedidos.trySend(resposta).getOrThrow()
        withContext(NonCancellable) { resposta.await() }
    }

    @After fun limpar() = main {
        store.clear()
        pendentes.forEach { it.complete(dados(null)) }
    }

    @Test fun reportsReuseTheSameSensorIdsCountersAndPowerAsMeasurements() = runBlocking {
        val atual = ZonedDateTime.now()
        val fonte = fonteDemonstrativaRelatorio()(atual)
        val medidor = fonte.medidores.keys.single()
        val comum = fonteSerieSimulada().consultar(medidor.toInt(), 2)
        val amostra = comum.first()
        val leitura = fonte.leituras.single { it.id == amostra.id_medicao }
        assertEquals(medidor, leitura.medidor)
        assertEquals(amostra.energia_acumulada, leitura.valores[GrandezaRelatorio.ENERGIA])
        assertEquals(amostra.potencia_ativa!! / 1_000, leitura.valores.getValue(GrandezaRelatorio.POTENCIA), 0.0)
    }

    @Test fun refreshHandlesFailureRetryEmptyAndSuccessfulExport() = runBlocking {
        val vm = criar()
        assertTrue(pedidos.tryReceive().isFailure)
        main { vm.demonstracao(true) }
        val primeira = pedido()
        assertTrue(vm.uiState.value.carregando)
        primeira.completeExceptionally(IOException("Falha simulada"))
        assertNotNull(aguardar(vm).erro)

        main { vm.atualizar() }
        pedido().complete(dados(null))
        assertEquals(CoberturaRelatorio.SEM_DADOS, aguardar(vm).relatorio?.cobertura)

        main { vm.atualizar() }
        pedido().complete(dados(15.0))
        val estado = aguardar(vm)
        val relatorio = requireNotNull(estado.relatorio)
        assertEquals(5.0, requireNotNull(relatorio.consumo), 0.0)
        assertTrue(pedidos.tryReceive().isFailure)
        val pdf = gerarRelatorioPdf(app, RelatorioDocumento(relatorio, estado))
        assertTrue(pdf.paginas >= 1)
        assertEquals("%PDF", String(pdf.bytes.copyOfRange(0, 4), Charsets.US_ASCII))
    }

    @Test fun pendingResultsCannotReplaceNewFilterOrClearedSession() = runBlocking {
        val vm = criar()
        main { vm.demonstracao(true) }
        val primeira = pedido()
        main { vm.aplicar(FiltrosRelatorio(periodo = PeriodoRelatorio.ONTEM)) }
        pedido().complete(dados(15.0))
        val estado = aguardar(vm)
        assertEquals(PeriodoRelatorio.ONTEM, estado.filtros.periodo)
        assertEquals(0, estado.relatorio?.leituras)
        primeira.complete(dados(22.0))
        main { assertEquals(estado, vm.uiState.value) }

        main { vm.atualizar() }
        val atrasada = pedido()
        main { store.clear() }
        atrasada.complete(dados(20.0))
        main { assertEquals(RelatoriosUiState(), vm.uiState.value) }
    }

    private fun dados(valorFinal: Double?): FonteRelatorio {
        val leituras = if (valorFinal == null) emptyList() else listOf(10.0, valorFinal).mapIndexed { i, kwh ->
            LeituraRelatorio("1", "1", "1", agora.minusMinutes(30L - i * 15L).toInstant(),
                mapOf(GrandezaRelatorio.ENERGIA to kwh, GrandezaRelatorio.POTENCIA to 1.25), "1")
        }
        return FonteRelatorio(emptyList(), emptyList(), leituras)
    }

    private suspend fun pedido() = withTimeout(5_000) { pedidos.receive() }
    private suspend fun aguardar(vm: RelatoriosViewModel) = withTimeout(5_000) { vm.uiState.first { !it.carregando } }
    private fun main(acao: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(acao)

    private fun criar(): RelatoriosViewModel {
        lateinit var vm: RelatoriosViewModel
        main {
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    requireNotNull(modelClass.cast(RelatoriosViewModel(app, fonte) { agora }))
            }
            vm = ViewModelProvider(store, factory)[RelatoriosViewModel::class.java]
        }
        return vm
    }
}
