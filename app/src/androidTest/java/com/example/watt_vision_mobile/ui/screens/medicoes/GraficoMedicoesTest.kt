package com.example.watt_vision_mobile.ui.screens.medicoes

import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class GraficoMedicoesTest {
    private val instrumentacao = InstrumentationRegistry.getInstrumentation()
    private val inicio = Instant.parse("2026-11-19T03:00:00Z")

    private fun serie(quantidade: Int, passoSegundos: Long) = SerieMedicoes(
        listOf(List(quantidade) { AmostraMedicao(inicio.plusSeconds(it * passoSegundos), 220.5 + it % 4) })
    )

    @OptIn(ExperimentalComposeUiApi::class)
    private fun abrir(conteudo: @Composable () -> Unit): ActivityScenario<ComponentActivity> =
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                WattVisionTheme {
                    Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) { conteudo() }
                }
            }
        }

    private fun procurar(no: AccessibilityNodeInfo?, chave: String, id: Boolean): AccessibilityNodeInfo? {
        if (no == null) return null
        if ((if (id) no.viewIdResourceName else no.text?.toString()) == chave) return no
        for (i in 0 until no.childCount) procurar(no.getChild(i), chave, id)?.let { return it }
        return null
    }

    private fun encontrar(chave: String, id: Boolean = false): AccessibilityNodeInfo {
        val prazo = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < prazo) {
            procurar(instrumentacao.uiAutomation.rootInActiveWindow, chave, id)?.let { return it }
            SystemClock.sleep(30)
        }
        error("Elemento ausente: $chave")
    }

    private fun limites(no: AccessibilityNodeInfo) = Rect().also(no::getBoundsInScreen)
    private fun limites(chave: String) = limites(encontrar(chave, id = true))

    private fun gesto(x: Int, y: Int, fimX: Int = x, fimY: Int = y) {
        val inicioGesto = SystemClock.uptimeMillis()
        fun evento(acao: Int, px: Float, py: Float) {
            val evento = MotionEvent.obtain(inicioGesto, SystemClock.uptimeMillis(), acao, px, py, 0)
            evento.source = InputDevice.SOURCE_TOUCHSCREEN
            try {
                assertTrue(instrumentacao.uiAutomation.injectInputEvent(evento, true))
            } finally {
                evento.recycle()
            }
        }
        evento(MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat())
        if (x != fimX || y != fimY) {
            for (passo in 1..12) {
                SystemClock.sleep(18)
                evento(MotionEvent.ACTION_MOVE, x + (fimX - x) * passo / 12f, y + (fimY - y) * passo / 12f)
            }
        }
        evento(MotionEvent.ACTION_UP, fimX.toFloat(), fimY.toFloat())
        instrumentacao.waitForIdleSync()
    }

    @Test fun toqueMostraTooltipEUmNovoToqueFecha() {
        abrir {
            Box(Modifier.width(300.dp)) {
                GraficoMedicoes(ChartMetric.VOLTAGE, "V", "Hoje", ConsultaMedicoes(serie = serie(1, 60)))
            }
        }.use {
            encontrar("1 registro")
            val plot = limites("medicoes-grafico")
            gesto(plot.left + 12, plot.centerY())
            encontrar("Tensão: 220,5 V")
            val horario = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault()).format(inicio)
            encontrar(horario)
            val janela = limites("medicoes-rolagem")
            val tooltip = limites("medicoes-tooltip")
            assertTrue(tooltip.left >= janela.left && tooltip.right <= janela.right)
            gesto(plot.centerX(), plot.centerY())
            val prazo = SystemClock.uptimeMillis() + 3_000
            while (procurar(instrumentacao.uiAutomation.rootInActiveWindow, "medicoes-tooltip", true) != null &&
                SystemClock.uptimeMillis() < prazo
            ) {
                SystemClock.sleep(30)
            }
            assertNull(procurar(instrumentacao.uiAutomation.rootInActiveWindow, "medicoes-tooltip", true))
        }
    }

    @Test fun rolagemHorizontalMantemEixoYEPermiteRolarPagina() {
        var pagina: androidx.compose.foundation.ScrollState? = null
        abrir {
            val vertical = rememberScrollState()
            pagina = vertical
            Column(Modifier.fillMaxSize().verticalScroll(vertical)) {
                Box(Modifier.width(320.dp)) {
                    GraficoMedicoes(ChartMetric.VOLTAGE, "V", "Últimos 7 dias", ConsultaMedicoes(serie = serie(300, 600)))
                }
                Spacer(Modifier.height(1000.dp))
            }
        }.use {
            val janela = limites("medicoes-rolagem")
            val eixoAntes = limites("medicoes-eixo-y")
            gesto(janela.centerX(), janela.centerY())
            val antes = encontrar("medicoes-tooltip", id = true).getChild(0).text
            gesto(janela.centerX(), janela.centerY())
            gesto(janela.right - 12, janela.centerY(), janela.left + 12, janela.centerY())
            SystemClock.sleep(400)
            assertEquals(eixoAntes.left, limites("medicoes-eixo-y").left)
            gesto(janela.centerX(), janela.centerY())
            val depois = encontrar("medicoes-tooltip", id = true).getChild(0).text
            assertNotEquals(antes, depois)
            gesto(janela.centerX(), janela.bottom - 40, janela.centerX(), janela.top + 20)
            instrumentacao.runOnMainSync { assertTrue(pagina!!.value > 0) }
        }
    }

    @Test fun demonstracaoSoEntraAposToqueEPodeSerDesativada() {
        val aviso = "Demonstração · Dados simulados"
        abrir { MedicoesScreen() }.use {
            val botao = limites(encontrar("Demonstração"))
            assertNull(procurar(instrumentacao.uiAutomation.rootInActiveWindow, aviso, false))
            gesto(botao.centerX(), botao.centerY())
            encontrar(aviso)
            val sair = limites(encontrar("Sair da demonstração"))
            gesto(sair.centerX(), sair.centerY())
            encontrar("Demonstração")
            assertNull(procurar(instrumentacao.uiAutomation.rootInActiveWindow, aviso, false))
        }
    }

    @Test fun dadosBrutosSelecionaSeisGrandezasEPreservaPaginaAoTrocarGrandeza() {
        val grandeza = mutableStateOf(ChartMetric.VOLTAGE)
        val escopo = mutableStateOf(0)
        val data = inicio
        val registros = (33 downTo 0).map { i ->
            RegistroMedicao(
                data.plusSeconds(i.toLong()),
                MedicaoDto(
                    id_medicao = i, tensaoRaw = 220.5 + i, correnteRaw = 5.0,
                    potenciaAtivaRaw = 1234.5, energiaAcumuladaRaw = 15.4, frequenciaRaw = 60.0,
                    fatorPotenciaRaw = .98
                )
            )
        }
        abrir {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                androidx.compose.runtime.key(escopo.value) {
                    TabelaDadosBrutos(ConsultaMedicoes(registros = registros), grandeza.value)
                }
            }
        }.use {
            encontrar("34 registros")
            encontrar("Página 1 de 4")
            val pagina4 = limites(encontrar("4"))
            gesto(pagina4.centerX(), pagina4.centerY())
            encontrar("Página 4 de 4")
            val horario = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(java.time.ZoneId.systemDefault()).format(data)
            encontrar(horario)

            val etiquetas = listOf("TENSAO", "CORRENTE", "POTENCIA", "ENERGIA", "FREQUENCIA", "FATOR_POTENCIA")
            fun selecionadas(no: AccessibilityNodeInfo?): List<String> {
                if (no == null) return emptyList()
                val id = no.viewIdResourceName
                return if (no.stateDescription?.toString() == "Coluna destacada" && id?.startsWith("dados-brutos-coluna-") == true) listOf(id)
                else (0 until no.childCount).flatMap { selecionadas(no.getChild(it)) }
            }
            ChartMetric.entries.zip(etiquetas).forEach { (metrica, etiqueta) ->
                instrumentacao.runOnMainSync { grandeza.value = metrica }
                val id = "dados-brutos-coluna-$etiqueta"
                encontrar(id, id = true)
                val prazo = SystemClock.uptimeMillis() + 3_000
                while (selecionadas(instrumentacao.uiAutomation.rootInActiveWindow) != listOf(id) &&
                    SystemClock.uptimeMillis() < prazo
                ) {
                    SystemClock.sleep(30)
                }
                assertEquals(listOf(id), selecionadas(instrumentacao.uiAutomation.rootInActiveWindow))
                encontrar("Página 4 de 4")
            }

            // Arrastar as colunas até o início não muda a página nem a grandeza selecionada.
            val grade = limites("dados-brutos-rolagem")
            val linha = limites("dados-brutos-coluna-FATOR_POTENCIA").centerY()
            val entrada = grade.left + grade.width() / 5
            val saida = grade.right - grade.width() / 5
            repeat(8) {
                gesto(entrada, linha, saida, linha)
                SystemClock.sleep(200)
            }
            assertNotEquals("Coluna destacada", encontrar("dados-brutos-coluna-DATA", id = true).stateDescription?.toString())
            encontrar("Página 4 de 4")
            instrumentacao.runOnMainSync { escopo.value++ }
            encontrar("Página 1 de 4")
        }
    }

    @Test fun mostraCarregamentoVazioErroERecuperacao() {
        val estado = mutableStateOf(ConsultaMedicoes(carregando = true))
        var tentou = false
        abrir { GraficoMedicoes(ChartMetric.FREQUENCY, "Hz", "Ontem", estado.value) }.use {
            encontrar("Carregando medições…")
            instrumentacao.runOnMainSync { estado.value = ConsultaMedicoes() }
            encontrar("Nenhuma medição válida no período.")
            instrumentacao.runOnMainSync {
                estado.value = ConsultaMedicoes(aviso = "Falha de comunicação", tentarNovamente = { tentou = true })
            }
            encontrar("Falha de comunicação")
            val botao = limites(encontrar("Tentar novamente"))
            gesto(botao.centerX(), botao.centerY())
            instrumentacao.runOnMainSync { assertTrue(tentou) }
        }
    }
}
