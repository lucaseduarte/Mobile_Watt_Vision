package com.example.watt_vision_mobile

import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesViewModel
import com.example.watt_vision_mobile.util.DateFormatter
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ConsultativeScopeTest {
    @Test
    fun themeSelectionAndNavigationPreserveBusinessRecords() {
        val viewModel = ConfiguracoesViewModel()
        val initial = viewModel.uiState.value
        viewModel.updateTema("Claro")
        for (tab in 0..3) viewModel.setActiveTab(tab)
        val current = viewModel.uiState.value
        assertEquals(initial.geral, current.geral)
        assertEquals(initial.tarifas, current.tarifas)
        assertEquals(initial.usuarios, current.usuarios)
        assertEquals(initial.pontosMedicao, current.pontosMedicao)
        assertEquals("Claro", current.preferencias.tema)
    }

    @Test
    fun tariffDatesUseFixedBrazilianFormat() {
        assertEquals("31/01/2026", DateFormatter.formatIsoDate("2026-01-31"))
    }

    @Test
    fun measurementApiExposesOnlyReviewedReadEndpoints() {
        val expected = setOf(
            "api/medicoes/resumo", "api/medicoes/ultima", "api/medicoes/serie-temporal",
            "api/medicoes/consumo", "api/equipamentos", "api/alertas", "api/alertas/ativos"
        )
        val routes = WattVisionApiService::class.java.declaredMethods.filterNot { it.isSynthetic }.map { method ->
            val get = method.getAnnotation(GET::class.java)
            assertNotNull("Operação fora do catálogo consultivo: ${method.name}", get)
            requireNotNull(get).value
        }
        assertEquals(expected, routes.toSet())
        assertEquals(expected.size, routes.size)
    }

    @Test
    fun authenticationIsTheOnlyPostAndKeepsSessionConsultation() {
        val methods = AuthApiService::class.java.declaredMethods
        val login = methods.single { it.getAnnotation(POST::class.java) != null }
        assertEquals("api/auth/login", requireNotNull(login.getAnnotation(POST::class.java)).value)
        val me = methods.single { it.getAnnotation(GET::class.java) != null }
        assertEquals("api/auth/me", requireNotNull(me.getAnnotation(GET::class.java)).value)
        assertEquals(2, methods.size)
    }
}
