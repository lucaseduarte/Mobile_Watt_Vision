package com.example.watt_vision_mobile.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.medicoes.FonteSerieSimulada
import com.example.watt_vision_mobile.ui.screens.medicoes.fonteSerieSimulada
import java.io.IOException
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

class DashboardAtualizacaoTest {
    private val store = ViewModelStore()
    private val agora = ZonedDateTime.parse("2026-11-23T12:00:00Z")
    private val pedidos = Channel<CompletableDeferred<List<MedicaoDto>>>(Channel.UNLIMITED)
    private val pendentes = mutableListOf<CompletableDeferred<List<MedicaoDto>>>()
    private val fonte = object : FonteSerieSimulada {
        override val catalogo = requireNotNull(fonteSerieSimulada()).catalogo
        override suspend fun consultar(pzem: Int, horas: Int): List<MedicaoDto> {
            assertEquals(1, pzem)
            val resposta = CompletableDeferred<List<MedicaoDto>>()
            pendentes += resposta
            pedidos.trySend(resposta).getOrThrow()
            // Simula um transporte que termina mesmo após o cancelamento do consumidor.
            return withContext(NonCancellable) { resposta.await() }
        }
    }

    @After fun limpar() = main {
        store.clear()
        pendentes.forEach { it.complete(emptyList()) }
    }

    @Test fun manualRefreshRetriesAndShowsEmptyOrConsistentSnapshot() = runBlocking {
        val vm = criar()
        assertTrue(pedidos.tryReceive().isFailure)
        main { vm.alternarDemonstracao() }
        val inicial = pedido()
        assertTrue(vm.uiState.value.isLoading)
        inicial.completeExceptionally(IOException("503 simulado"))
        vm.aguardar(ChartStatus.ERROR)

        main { vm.retryChart() }
        pedido().complete(emptyList())
        vm.aguardar(ChartStatus.EMPTY)

        main { vm.loadDashboardData() }
        pedido().complete(listOf(leitura(220.0)))
        val estado = vm.aguardar(ChartStatus.READY)
        assertEquals(220.0, estado.chartPoints.single().valor)
        assertTrue(estado.summaryCards.single { it.label == "Tensão Atual" }.value.startsWith("220"))
        assertEquals("—", estado.summaryCards.single { it.label == "Dispositivos Ativos" }.value)
        assertTrue(pedidos.tryReceive().isFailure)
    }

    @Test fun onlyFlagsPossibleTruncationAtServerCapAndReplacesPreviousBatch() = runBlocking {
        val vm = criar()
        fun dados(total: Int) = List(total) { indice ->
            leitura(220.0).copy(
                id_medicao = 5_000_000_000L + indice,
                data_hora = agora.minusSeconds((total - indice).toLong()).toString()
            )
        }

        main { vm.alternarDemonstracao() }
        pedido().complete(dados(9_999))
        val abaixoDoLimite = vm.aguardar(ChartStatus.READY)
        assertFalse(abaixoDoLimite.chart.truncated)
        assertEquals(9_999, abaixoDoLimite.chartPoints.size)

        main { vm.loadDashboardData() }
        pedido().complete(dados(10_000))
        val noLimite = vm.aguardar(ChartStatus.READY)
        assertTrue(noLimite.chart.truncated)
        assertEquals(10_000, noLimite.chartPoints.size)
    }

    @Test fun delayedResponseCannotReplaceNewFilterOrClosedSession() = runBlocking {
        val vm = criar()
        main { vm.alternarDemonstracao() }
        val antiga = pedido()
        main { vm.onChartPeriodSelected("6 horas") }
        val atual = pedido()
        atual.complete(listOf(leitura(225.0)))
        vm.aguardar(ChartStatus.READY)
        antiga.complete(listOf(leitura(210.0)))
        main { assertEquals(225.0, vm.uiState.value.chartPoints.single().valor) }

        main { vm.loadDashboardData() }
        val atrasada = pedido()
        main { store.clear() }
        atrasada.complete(listOf(leitura(215.0)))
        main { assertEquals(DashboardUiState(), vm.uiState.value) }
    }

    private fun leitura(tensao: Double) = MedicaoDto(
        id_medicao = 1, id_pzem = 1, id_ponto_medicao = 1,
        data_hora = agora.minusMinutes(10).toString(), tensaoRaw = tensao
    )

    private suspend fun pedido() = withTimeout(5_000) { pedidos.receive() }

    private suspend fun DashboardViewModel.aguardar(status: ChartStatus) =
        withTimeout(5_000) { uiState.first { it.chart.status == status } }

    private fun criar(): DashboardViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        lateinit var model: DashboardViewModel
        main {
            val factory = object : ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    requireNotNull(modelClass.cast(DashboardViewModel(app, SavedStateHandle(), fonte) { agora }))
            }
            model = ViewModelProvider(store, factory)[DashboardViewModel::class.java]
        }
        return model
    }

    private fun main(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
}
