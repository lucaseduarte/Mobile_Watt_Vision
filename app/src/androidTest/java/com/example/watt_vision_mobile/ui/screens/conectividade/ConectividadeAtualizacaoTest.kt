package com.example.watt_vision_mobile.ui.screens.conectividade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.time.LocalDateTime
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

class ConectividadeAtualizacaoTest {
    private val store = ViewModelStore()
    private val pedidos = Channel<CompletableDeferred<ConectividadeUiState>>(Channel.UNLIMITED)
    private val pendentes = mutableListOf<CompletableDeferred<ConectividadeUiState>>()
    private val fonte = FonteConectividade {
        val resposta = CompletableDeferred<ConectividadeUiState>()
        pendentes += resposta
        pedidos.trySend(resposta).getOrThrow()
        withContext(NonCancellable) { resposta.await() }
    }

    @After fun limpar() = main {
        store.clear()
        pendentes.forEach { it.complete(ConectividadeUiState()) }
    }

    @Test fun refreshHandlesErrorEmptyAndOrdersValidEvents() = runBlocking {
        val vm = criar()
        val original = pedido()
        assertTrue(vm.estado.value.carregando)
        original.completeExceptionally(IOException("Falha simulada"))
        aguardar(vm)
        assertTrue(vm.estado.value.erro)

        main { vm.atualizar() }
        pedido().complete(ConectividadeUiState())
        aguardar(vm)
        assertTrue(vm.estado.value.dispositivos.isEmpty())
        assertFalse(vm.estado.value.erro)

        main { vm.atualizar() }
        pedido().complete(snapshot())
        aguardar(vm)
        assertEquals(listOf(1L), vm.estado.value.dispositivos.map { it.id })
        assertEquals(listOf(2L, 1L), vm.estado.value.eventos.map { it.id })
        assertTrue(pedidos.tryReceive().isFailure)
    }

    @Test fun refreshAndSessionDisposalDiscardLateRecords() = runBlocking {
        val vm = criar()
        val antiga = pedido()
        main { vm.atualizar() }
        pedido().complete(ConectividadeUiState())
        aguardar(vm)
        antiga.complete(snapshot())
        main { assertTrue(vm.estado.value.dispositivos.isEmpty()) }

        main { vm.atualizar() }
        val atrasada = pedido()
        main { store.clear() }
        atrasada.complete(snapshot())
        main { assertEquals(ConectividadeUiState(carregando = false), vm.estado.value) }
    }

    private fun snapshot(): ConectividadeUiState {
        val momento = LocalDateTime.parse("2026-11-23T12:00:00")
        val dispositivo = DispositivoConectividade(DispositivoIoTDto(1, "ESP-1", "Exemplo", "online", null, null, null))
        return ConectividadeUiState(
            dispositivos = listOf(dispositivo, dispositivo),
            eventos = listOf(
                EventoConectividade(1, 1, "Desconectado", momento.minusMinutes(3)),
                EventoConectividade(2, 1, "Reconectado", momento.minusMinutes(1)),
                EventoConectividade(3, 9, "Desconectado", momento)
            )
        )
    }

    private suspend fun pedido() = withTimeout(5_000) { pedidos.receive() }
    private suspend fun aguardar(vm: ConectividadeViewModel) = withTimeout(5_000) { vm.estado.first { !it.carregando } }
    private fun main(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)

    private fun criar(): ConectividadeViewModel {
        lateinit var vm: ConectividadeViewModel
        main {
            val factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    requireNotNull(modelClass.cast(ConectividadeViewModel(fonte)))
            }
            vm = ViewModelProvider(store, factory)[ConectividadeViewModel::class.java]
        }
        return vm
    }
}
