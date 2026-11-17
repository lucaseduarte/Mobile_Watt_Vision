package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.model.alarmes.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class AlarmesDemoDataTest {
    @Test fun demonstrationHasKnownCountsAndUnknownDataDoesNotPolluteThem() {
        val records = AlarmesDemoData.create(LocalDateTime.parse("2026-11-17T15:00:00"))
        assertEquals(10, records.size)
        assertEquals(AlarmesCounts(6, 2, 2, 3), AlarmesRules.counts(records))
        assertEquals(1, records.count { it.status == AlarmeStatus.DESCONHECIDO })
        assertEquals(2, AlarmesRules.filter(records, AlarmesFilters(status = AlarmeStatus.ATIVO,
            severidade = AlarmeSeveridade.ATENCAO, categoria = AlarmeCategoria.ENERGETICO)).size)
        assertEquals(0, records.count { it.statusOriginal == "resolvido" })
    }
}
