package com.example.watt_vision_mobile

import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesViewModel
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST

class ConsultativeScopeTest {
    @Test fun localPreferencesNeverChangeBusinessRecords() {
        val vm = ConfiguracoesViewModel()
        val original = vm.uiState.value
        vm.updateTema("Claro")
        vm.salvarPreferenciasLocais()
        for (tab in 0..1) vm.setActiveTab(tab)
        val current = vm.uiState.value
        assertEquals(original.geral.nomeEmpresa, current.geral.nomeEmpresa)
        assertEquals(original.geral.fusoHorario, current.geral.fusoHorario)
        assertEquals(original.usuarios, current.usuarios)
        assertEquals("DD/MM/AAAA", current.geral.formatoData)
        assertEquals("Claro", current.geral.tema)
    }
    @Test fun cancelRestoresOnlySavedPreferencesAndKeepsConsultationTab() {
        val vm = ConfiguracoesViewModel()
        vm.salvarPreferenciasLocais()
        val saved = vm.uiState.value
        vm.updateTema("Claro")
        vm.setActiveTab(1)
        vm.cancelarPreferenciasLocais()
        assertEquals(saved.copy(activeTab = 1), vm.uiState.value)
    }
    @Test fun monitoringInterfaceExposesOnlyReviewedReadOperations() {
        val expected = setOf("api/medicoes/resumo", "api/medicoes/ultima", "api/medicoes/serie-temporal",
            "api/medicoes/consumo", "api/equipamentos", "api/equipamentos/{id}", "api/alertas", "api/alertas/ativos")
        val routes = WattVisionApiService::class.java.declaredMethods.filterNot { it.isSynthetic }.map {
            requireNotNull(it.getAnnotation(GET::class.java)).value
        }
        assertEquals(expected, routes.toSet())
        assertEquals(expected.size, routes.size)
    }
    @Test fun sessionKeepsLoginAsOnlyPost() {
        val methods = AuthApiService::class.java.declaredMethods.filterNot { it.isSynthetic }
        assertEquals(2, methods.size)
        assertEquals("api/auth/login", methods.single { it.getAnnotation(POST::class.java) != null }.getAnnotation(POST::class.java)!!.value)
        assertEquals("api/auth/me", methods.single { it.getAnnotation(GET::class.java) != null }.getAnnotation(GET::class.java)!!.value)
    }
}
