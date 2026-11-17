package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.data.api.*
import com.example.watt_vision_mobile.model.MedicaoDto
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.GET
import java.time.LocalDateTime
import java.time.ZoneId

class ChartCatalogTest {
    private val catalog = ChartCatalog(
        locais = listOf(ChartLocalDto(10, "Local A"), ChartLocalDto(20, "Local B")),
        setores = listOf(ChartSectorDto(100, "Produção", 10), ChartSectorDto(200, "Produção", 20)),
        equipamentos = listOf(ChartEquipmentDto(1000, "Motor", 100), ChartEquipmentDto(2000, "Motor", 200)),
        pontos = listOf(ChartPointDto(7, null, null, 1000), ChartPointDto(8, null, null, 2000)),
        sensores = listOf(ChartSensorDto(91, 7), ChartSensorDto(92, 8))
    )

    @Test fun duplicateNamesAreResolvedByIdsNotDisplayText() {
        assertEquals(91 to null, catalog.resolve(DashboardFilterState(equipment = "Motor", equipmentId = 1000)))
        assertEquals(92 to null, catalog.resolve(DashboardFilterState(equipment = "Motor", equipmentId = 2000)))
        assertEquals(null to ChartStatus.FILTER_UNSUPPORTED, catalog.resolve(DashboardFilterState(equipment = "Motor")))
    }

    @Test fun parentLocationAndSectorFindDescendantSensors() {
        assertEquals(91 to null, catalog.resolve(DashboardFilterState(location = "Local A", locationId = 10)))
        assertEquals(92 to null, catalog.resolve(DashboardFilterState(sector = "Produção", sectorId = 200)))
        assertEquals(listOf(100), catalog.sectors(10).map { it.id })
        assertEquals(listOf(1000), catalog.equipment(10, null).map { it.id })
    }

    @Test fun allIsLoadedAutomaticallyOnlyWhenThereIsOneRealSensor() {
        assertEquals(null to ChartStatus.AMBIGUOUS, catalog.resolve(DashboardFilterState()))
        assertEquals(91 to null, catalog.copy(sensores = catalog.sensores.take(1)).resolve(DashboardFilterState()))
        assertEquals(null to ChartStatus.NO_SENSOR, catalog.copy(sensores = emptyList()).resolve(DashboardFilterState()))
    }

    @Test fun twoSensorsOnSameEquipmentCannotBeCollapsedOrChosenArbitrarily() {
        val multiple = catalog.copy(sensores = listOf(ChartSensorDto(91, 7), ChartSensorDto(93, 7)))
        assertEquals(null to ChartStatus.AMBIGUOUS,
            multiple.resolve(DashboardFilterState(equipment = "Motor", equipmentId = 1000)))
    }

    @Test fun unresolvedAndContradictoryRelationshipsNeverFallbackToFirstDevice() {
        val orphan = catalog.copy(sensores = listOf(ChartSensorDto(91, 999)))
        assertEquals(null to ChartStatus.FILTER_UNSUPPORTED, orphan.resolve(DashboardFilterState()))
        assertEquals(null to ChartStatus.FILTER_UNSUPPORTED, catalog.resolve(DashboardFilterState(equipmentId = 1000)))
        assertEquals(null to ChartStatus.NO_SENSOR,
            catalog.resolve(DashboardFilterState(location = "Local A", locationId = 10, equipment = "Motor", equipmentId = 2000)))
    }

    @Test fun directlyLinkedLocalAndSectorPointsUseTheirActualIds() {
        val local = catalog.copy(pontos = listOf(ChartPointDto(7, 10, null, null)), sensores = listOf(ChartSensorDto(91, 7)))
        assertEquals(91 to null, local.resolve(DashboardFilterState(location = "Local A", locationId = 10)))
        assertEquals(null to ChartStatus.NO_SENSOR, local.resolve(DashboardFilterState(equipment = "Motor", equipmentId = 1000)))
        val sector = local.copy(pontos = listOf(ChartPointDto(7, null, 100, null)))
        assertEquals(91 to null, sector.resolve(DashboardFilterState(location = "Local A", locationId = 10)))
    }

    @Test fun historicalReadingsFromAnotherPointStayAsGapsInFilteredSeries() {
        val filter = DashboardFilterState(equipment = "Motor", equipmentId = 1000)
        val allowed = catalog.scopedPointIds(filter)
        assertEquals(setOf(7), allowed)
        val rows = listOf(8, 7).mapIndexed { index, point ->
            MedicaoDto(id_medicao = index, id_pzem = 91, id_ponto_medicao = point,
                data_hora = "2026-11-17 10:0$index:00", tensaoRaw = 220)
        }
        val mapped = ChartLogic.map(rows, ChartMetric.VOLTAGE,
            ChartLogic.period("Hoje", LocalDateTime.parse("2026-11-17T12:00:00"))!!, ZoneId.of("America/Sao_Paulo"), allowed)
        assertEquals(2, mapped.size)
        assertNull(mapped.first().valor)
        assertEquals(220.0, mapped.last().valor)
        assertNull(catalog.scopedPointIds(DashboardFilterState()))
    }

    @Test fun selectedIdsAndLabelsSurviveSavedFilterSerialization() {
        val selected = DashboardFilterState(location = "Local A", locationId = 10, equipment = "Motor", equipmentId = 1000)
        val buffer = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(buffer).use { it.writeObject(selected) }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(buffer.toByteArray())).use { it.readObject() }
        assertEquals(selected, restored)
    }

    @Test fun catalogExposesOnlyExistingReadEndpoints() {
        val methods = ChartCatalogApiService::class.java.declaredMethods.filterNot { it.isSynthetic }
        assertEquals(setOf("api/locais", "api/setores", "api/equipamentos", "api/pontos-medicao", "api/pzems"),
            methods.map { requireNotNull(it.getAnnotation(GET::class.java)).value }.toSet())
        assertEquals(5, methods.size)
    }
}
