package com.example.watt_vision_mobile.ui.screens.configuracoes

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.watt_vision_mobile.model.UsuarioDto
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeuPerfilTest {
    private val instrumentacao = InstrumentationRegistry.getInstrumentation()

    @Test
    fun somenteContaAtualAbrePerfilMesmoComIdIgualAoDeOutroUsuario() {
        val conta = UsuarioDto(1, 1, "Conta de teste", "conta@example.invalid", "Gestor", "Ativo")
        abrir(conta).use {
            clicar("Usuários")
            val outroUsuario = encontrar("Gabriel Santos")
            assertFalse(gerarAncestrais(outroUsuario).any { no -> no.isClickable })

            clicar("Minha conta")
            encontrar("CONTA ATUAL")
            encontrar("conta@example.invalid")
            clicar("Alterar senha")
            encontrar("Senha atual")
            clicar("Cancelar alteração de senha")
            encontrar("Alterar senha")
            assertFalse(procurar("Senha atual") != null)
        }
    }

    @Test
    fun semSessaoNaoDisponibilizaMeuPerfil() {
        abrir(null).use {
            clicar("Usuários")
            encontrar("Meu perfil requer uma sessão autenticada.")
            assertFalse(procurar("Minha conta") != null)
        }
    }

    @Test
    fun demonstracaoAbrePerfilFicticioSomenteDepoisDeAtivarETocarNoCard() {
        abrir(null).use {
            clicar("Usuários")
            assertFalse(procurar("Minha conta") != null)
            clicar("Ver demonstração de Meu perfil")
            assertFalse(procurar("CONTA ATUAL") != null)
            clicar("Minha conta")
            encontrar("CONTA ATUAL")
            encontrar("DEBUG · DADOS SIMULADOS")
            encontrar("perfil@example.invalid")
        }
    }

    private fun abrir(conta: UsuarioDto?): ActivityScenario<ComponentActivity> =
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            val viewModel = ConfiguracoesViewModel()
            activity.setContent {
                WattVisionTheme {
                    ConfiguracoesScreen(viewModel = viewModel, carregarConta = { conta }, onBackClick = {})
                }
            }
        }

    private fun gerarAncestrais(no: AccessibilityNodeInfo) = generateSequence(no) { it.parent }

    private fun clicar(texto: String) {
        val no = gerarAncestrais(encontrar(texto)).firstOrNull { it.isClickable }
            ?: error("Elemento sem ação: $texto")
        assertTrue(no.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentacao.waitForIdleSync()
    }

    private fun encontrar(texto: String): AccessibilityNodeInfo {
        val prazo = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < prazo) {
            procurar(texto)?.let { return it }
            SystemClock.sleep(40)
        }
        error("Elemento ausente: $texto")
    }

    private fun procurar(texto: String): AccessibilityNodeInfo? {
        fun busca(no: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (no == null) return null
            if (no.text?.toString() == texto) return no
            for (indice in 0 until no.childCount) {
                busca(no.getChild(indice))?.let { return it }
            }
            return null
        }
        return busca(instrumentacao.uiAutomation.rootInActiveWindow)
    }
}
