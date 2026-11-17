package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.model.alarmes.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class AlarmesRulesTest {
    private val now = LocalDateTime.parse("2026-11-17T15:00:00")
    private fun alarm(id: Long, status: String? = "ativo", severity: String? = "critico", category: String? = "energetico", message: String = "Tensão acima do limite") =
        AlarmeMapper.map(AlarmeDto(id = id, status = status, severidade = severity, categoria_alerta = category,
            mensagem = message, tipo_alerta = "Evento de tensão", id_dispositivo_iot = 901, data_hora = "2026-11-17 14:30:00"))

    @Test fun countersIncludeSeverityOnlyForActiveRecords() {
        val records = listOf(alarm(1), alarm(2, severity = "atencao"), alarm(3, severity = "informativo"),
            alarm(4, "reconhecido"), alarm(5, "reconhecido", "atencao"), alarm(6, "inesperado"))
        assertEquals(AlarmesCounts(3, 1, 1, 2), AlarmesRules.counts(records))
        assertEquals(AlarmesCounts(0, 0, 0, 0), AlarmesRules.counts(emptyList()))
    }

    @Test fun unknownAndMissingValuesNeverBecomeValidClassifications() {
        val unknown = alarm(1, "resolvido", "aviso", "outra")
        assertEquals(AlarmeStatus.DESCONHECIDO, unknown.status)
        assertEquals(AlarmeSeveridade.DESCONHECIDA, unknown.severidade)
        assertEquals(AlarmeCategoria.DESCONHECIDA, unknown.categoria)
        assertTrue(unknown.statusLabel.contains("resolvido"))
        assertEquals(AlarmesCounts(0, 0, 0, 0), AlarmesRules.counts(listOf(unknown)))
        val missing = AlarmeMapper.map(AlarmeDto())
        assertEquals("Status não informado", missing.statusLabel)
        assertNull(missing.valorDetectado)
        assertNull(missing.dispositivoIotId)
    }

    @Test fun combinedFiltersAndAccentInsensitiveSearchPreserveOrder() {
        val first = alarm(1, category = "comunicacao", message = "Comunicação interrompida")
        val second = alarm(2, "reconhecido", category = "comunicacao", message = "Comunicação retomada")
        val third = alarm(3, severity = "atencao", category = "comunicacao", message = "Comunicação intermitente")
        val records = listOf(first, second, third)
        assertEquals(listOf(first, second, third), AlarmesRules.filter(records, AlarmesFilters(query = " COMUNICACAO ")))
        assertEquals(listOf(third), AlarmesRules.filter(records, AlarmesFilters(query = "comunicacao", status = AlarmeStatus.ATIVO,
            severidade = AlarmeSeveridade.ATENCAO, categoria = AlarmeCategoria.COMUNICACAO)))
        assertTrue(AlarmesRules.filter(records, AlarmesFilters(query = "inexistente")).isEmpty())
        assertEquals(listOf(first, third), AlarmesRules.filter(records, AlarmesFilters(status = AlarmeStatus.ATIVO)))
    }

    @Test fun searchesUseOnlyAvailableMessageTypeAndIdentifiers() {
        val records = listOf(alarm(8))
        for (query in listOf("901", "DISPOSITIVO IOT", "tensao", "Evento", "8")) {
            assertEquals(records, AlarmesRules.filter(records, AlarmesFilters(query = query)))
        }
        assertTrue(AlarmesRules.filter(records, AlarmesFilters(query = "nome de equipamento inventado")).isEmpty())
    }

    @Test fun mappingSupportsBackendDecimalStringsBigintAndRealFieldNames() {
        val dto = Gson().fromJson("""{
            "id": 5000000000, "id_tipo_alerta": 12, "id_medicao": 6000000000,
            "id_dispositivo_iot": 22, "tipo_alerta":"Limite excedido", "categoria_alerta":"energetico",
            "severidade":"atencao", "status":"reconhecido", "valor_detectado":"245.125", "valor_limite":"0.000",
            "data_hora":"2026-11-17 09:30:00", "reconhecido_por": 19, "data_reconhecimento":"2026-11-17 09:40:00"
        }""", AlarmeDto::class.java)
        val mapped = AlarmeMapper.map(dto)
        assertEquals(5000000000L, mapped.id)
        assertEquals(6000000000L, mapped.medicaoId)
        assertEquals("Limite excedido", mapped.titulo)
        assertEquals(245.125, mapped.valorDetectado)
        assertEquals(0.0, mapped.valorLimite)
        assertEquals(AlarmeStatus.RECONHECIDO, mapped.status)
        assertEquals(19L, mapped.reconhecidoPor)
    }

    @Test fun datesKeepCivilTimeOrExplicitOffsetWithoutInventingTimezone() {
        assertEquals("17/11/2026 09:30:00", AlarmeDate.format("2026-11-17 09:30:00"))
        assertEquals("17/11/2026 09:30:00 -03:00", AlarmeDate.format("2026-11-17T09:30:00-03:00"))
        assertEquals("17/11/2026 12:30:00 Z", AlarmeDate.format("2026-11-17T12:30:00Z"))
        assertEquals("Não informada", AlarmeDate.format(null))
        assertEquals("Data inválida", AlarmeDate.format("texto inválido"))
    }

    @Test fun unavailableMeasurementsAreNotZero() {
        val mapped = AlarmeMapper.map(AlarmeDto(valor_detectado = Double.NaN, valor_limite = Double.POSITIVE_INFINITY))
        assertNull(mapped.valorDetectado)
        assertNull(mapped.valorLimite)
        assertEquals(0.0, AlarmeMapper.map(AlarmeDto(valor_detectado = 0.0)).valorDetectado)
    }

    @Test fun emptyResponseAndNoSearchResultsAreDifferentStates() {
        val empty = AlarmesUiState().loaded(emptyList(), now)
        assertEquals(AlarmesPhase.EMPTY, empty.phase)
        assertTrue(empty.hasSnapshot)
        val searched = AlarmesUiState().loaded(listOf(alarm(1)), now).copy(filters = AlarmesFilters(query = "ausente"))
        assertEquals(AlarmesPhase.CONTENT, searched.phase)
        assertTrue(searched.records.isNotEmpty())
        assertTrue(AlarmesRules.filter(searched.records, searched.filters).isEmpty())
        assertFalse(AlarmesUiState().failed(AlarmesPhase.ERROR).hasSnapshot)
    }

    @Test fun networkFailureRetainsSnapshotAsStaleAndRetryKeepsFilters() {
        val before = AlarmesUiState(filters = AlarmesFilters(query = "tensao")).loaded(listOf(alarm(1)), now)
        val failed = before.failed(AlarmesPhase.ERROR)
        assertEquals(before.records, failed.records)
        assertTrue(failed.isStale)
        assertEquals(now, failed.updatedAt)
        val retry = failed.refreshing()
        assertTrue(retry.isRefreshing)
        assertEquals(before.filters, retry.filters)
        val success = retry.loaded(emptyList(), now.plusMinutes(1))
        assertFalse(success.isStale)
        assertEquals(AlarmesPhase.EMPTY, success.phase)
        assertTrue(success.records.isEmpty())
    }

    @Test fun sessionOrPermissionFailureClearsPreviousDataAndCounts() {
        val before = AlarmesUiState().loaded(listOf(alarm(1)), now)
        for (reason in listOf(AlarmesPhase.NO_SESSION, AlarmesPhase.FORBIDDEN)) {
            val after = before.failed(reason)
            assertTrue(after.records.isEmpty())
            assertFalse(after.hasSnapshot)
            assertFalse(after.isStale)
            assertNull(after.updatedAt)
        }
    }
}
