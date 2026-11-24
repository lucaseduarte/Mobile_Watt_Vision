package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.model.alarmes.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class AlarmesDemoDataTest {
    @Test fun readsOrderedStableIdsAndEmptySnapshotsThroughRetrofit() = kotlinx.coroutines.runBlocking {
        val dados = AlarmesDemoData.responses(LocalDateTime.parse("2026-11-23T12:00:00"))
        var resposta = dados.asReversed() + dados.first()
        val api = com.example.watt_vision_mobile.data.api.transporteDemonstracao { path ->
            assertEquals("/api/alertas", path)
            com.example.watt_vision_mobile.model.ApiResponse(true, null, resposta)
        }.create(com.example.watt_vision_mobile.data.api.AlarmesApiService::class.java)
        val fonte = AlarmesDemonstracao(api)
        val lista = fonte.listar()
        assertEquals(dados.map { it.id }, lista.map { it.id })
        assertEquals(lista, fonte.listar())
        assertEquals(1, AlarmesDemoData.entidade(lista.first())?.id)
        assertNull(AlarmesDemoData.entidade(lista.last()))

        resposta = emptyList()
        assertTrue(fonte.listar().isEmpty())
    }

    @Test fun demonstrationHasKnownCountsAndUnknownDataDoesNotPolluteThem() {
        val records = AlarmesDemoData.create(LocalDateTime.parse("2026-11-17T15:00:00"))
        assertEquals(10, records.size)
        assertEquals(AlarmesCounts(9, 3, 3, 0), AlarmesRules.counts(records))
        assertEquals(1, records.count { it.status == AlarmeStatus.DESCONHECIDO })
        assertEquals(2, AlarmesRules.filter(records, AlarmesFilters(status = AlarmeStatus.ATIVO,
            severidade = AlarmeSeveridade.ATENCAO, categoria = AlarmeCategoria.ENERGETICO)).size)
        assertEquals(0, records.count { it.statusOriginal == "resolvido" })
    }
}
