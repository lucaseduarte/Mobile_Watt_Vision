package com.example.watt_vision_mobile.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.screens.dashboard.components.SystemPerformanceChart
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/** Respostas controladas, sem ViewModel, autenticação ou chamadas à API. */
class ChartInteractionTest {
    @get:Rule val compose = createComposeRule()
    private fun series(count: Int) = List(count) { index ->
        val time = LocalDateTime.of(2026, 11, 17, 8, 0).plusSeconds(index.toLong())
        MedicaoPoint("9:$index", time, time.toString(), 220.0 + index % 5)
    }

    @Test fun horizontalDragNavigatesExtensiveSeriesAndVerticalDragScrollsPage() {
        var scroll: androidx.compose.foundation.ScrollState? = null
        compose.setContent {
            MaterialTheme {
                val vertical = rememberScrollState()
                scroll = vertical
                Column(Modifier.fillMaxSize().verticalScroll(vertical)) {
                    SystemPerformanceChart(series(10000), "tensao", "Hoje", {}, {},
                        ChartState(ChartStatus.READY, pzemId = 9), {}, Modifier.width(320.dp))
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        val plot = compose.onNodeWithTag("chart-plot").performScrollTo()
        val before = plot.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        plot.performTouchInput { swipeRight() }
        compose.waitForIdle()
        val after = plot.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertNotEquals(before, after)
        val scrollBefore = scroll!!.value
        plot.performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertTrue("Gesto vertical deve continuar no contêiner da página", scroll!!.value > scrollBefore)
    }

    @Test fun touchSelectsSampleAndRefreshPreservesSelection() {
        val points = mutableStateOf(series(20))
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    SystemPerformanceChart(points.value, "tensao", "Hoje", {}, {},
                        ChartState(ChartStatus.READY, pzemId = 9), {})
                }
            }
        }
        compose.onNodeWithTag("chart-plot").performScrollTo().performTouchInput { click(center) }
        compose.onNodeWithTag("chart-selected-sample").assertExists()
        val selectedBefore = compose.onNodeWithTag("chart-selected-time").fetchSemanticsNode()
            .config[SemanticsProperties.Text].single().text
        compose.runOnIdle { points.value = series(21) }
        compose.onNodeWithText(selectedBefore).assertExists()
        val janelaAtual = compose.onNodeWithTag("chart-plot").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertTrue(janelaAtual.endsWith(" a 21 de 21"))
    }

    @Test fun narrowLayoutKeepsPeriodsMetricsAndEmptyPlotWithoutSession() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    SystemPerformanceChart(emptyList(), "tensao", "Hoje", {}, {},
                        ChartState(ChartStatus.NO_SESSION), {}, Modifier.width(320.dp))
                }
            }
        }
        listOf("Hoje", "6 horas", "Ontem", "Energia acumulada", "Frequência", "Fator de potência").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("30d").assertDoesNotExist()
        compose.onNodeWithTag("chart-plot").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Entre na API para consultar medições reais.").assertExists()
    }
}
