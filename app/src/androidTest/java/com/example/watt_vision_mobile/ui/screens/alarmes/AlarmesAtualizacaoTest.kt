package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test

class AlarmesAtualizacaoTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualRetryDisposesPendingSnapshotWhenSessionEnds() {
        val sessao = mutableStateOf(true)
        val pedidos = Channel<CompletableDeferred<List<Alarme>>>(Channel.UNLIMITED)
        val pendentes = mutableListOf<CompletableDeferred<List<Alarme>>>()
        val carregar: suspend () -> List<Alarme> = {
            val resposta = CompletableDeferred<List<Alarme>>()
            pendentes += resposta
            pedidos.trySend(resposta).getOrThrow()
            withContext(NonCancellable) { resposta.await() }
        }
        fun pedido(): CompletableDeferred<List<Alarme>> {
            var resposta: CompletableDeferred<List<Alarme>>? = null
            compose.waitUntil(5_000) {
                resposta = pedidos.tryReceive().getOrNull()
                resposta != null
            }
            return requireNotNull(resposta)
        }

        compose.setContent {
            WattVisionTheme {
                if (sessao.value) {
                    AlarmesSourceHost(
                        AlarmesUiState().failed(AlarmesPhase.NO_SESSION), {}, {}, Modifier,
                        { null }, {}, carregarDemo = carregar
                    )
                }
            }
        }
        try {
            compose.onNodeWithText("Ver demonstração de Alarmes").performClick()
            val inicial = pedido()
            compose.onNodeWithContentDescription("Atualizar alarmes").assertIsNotEnabled()
            inicial.completeExceptionally(IOException("Falha simulada"))
            val lista = compose.onNode(hasScrollToIndexAction())
            lista.performScrollToNode(hasText("Tentar novamente"))
            compose.onNodeWithText("Tentar novamente").performClick()
            pedido().complete(emptyList())
            lista.performScrollToNode(hasText("Nenhum alarme disponível."))
            compose.onNodeWithText("Nenhum alarme disponível.").assertIsDisplayed()

            lista.performScrollToNode(hasContentDescription("Atualizar alarmes"))
            compose.onNodeWithContentDescription("Atualizar alarmes").performClick()
            val atrasada = pedido()
            compose.runOnIdle { sessao.value = false }
            compose.waitForIdle()
            atrasada.complete(AlarmesDemoData.create(LocalDateTime.of(2026, 11, 23, 12, 0)))

            compose.runOnIdle { sessao.value = true }
            compose.onNodeWithText("Ver demonstração de Alarmes").assertIsDisplayed()
            compose.onNodeWithText("Equipamento de exemplo 1", substring = true).assertDoesNotExist()
        } finally {
            compose.runOnIdle { pendentes.forEach { it.complete(emptyList()) } }
        }
    }
}
