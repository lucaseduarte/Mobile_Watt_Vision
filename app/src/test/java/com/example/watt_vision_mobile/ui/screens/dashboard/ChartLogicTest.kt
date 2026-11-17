package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.model.MedicaoDto
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ChartLogicTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = LocalDateTime.parse("2026-11-17T12:00:00")
    private val today = ChartLogic.period("Hoje", now)!!
    private fun row(id: Int, date: String = "2026-11-17 10:00:00", value: Any? = 220.0, point: Int = 4) =
        MedicaoDto(id_medicao = id, id_pzem = 9, id_ponto_medicao = point, data_hora = date,
            tensaoRaw = value, correnteRaw = 1.2, potenciaAtivaRaw = 1500, energiaAcumuladaRaw = 12.5,
            frequenciaRaw = 60.0, fatorPotenciaRaw = 0.95)

    @Test fun todayStartsAtMidnightAndYesterdayIsAnExclusiveCalendarDay() {
        assertEquals(now.toLocalDate().atStartOfDay(), today.start)
        assertTrue(today.contains(now))
        assertFalse(today.contains(now.minusDays(1)))
        val yesterday = ChartLogic.period("Ontem", now)!!
        assertTrue(yesterday.contains(LocalDateTime.parse("2026-11-16T00:00:00")))
        assertTrue(yesterday.contains(LocalDateTime.parse("2026-11-16T23:59:59")))
        assertFalse(yesterday.contains(LocalDateTime.parse("2026-11-17T00:00:00")))
        assertEquals(ChartLogic.periods, listOf("Hoje", "6 horas", "Ontem"))
        assertNull(ChartLogic.period("30d", now))
    }

    @Test fun requestCoversCalendarBoundaryButNeverExceedsApiLimit() {
        for (hour in 0..23) for (period in ChartLogic.periods) {
            val at = now.withHour(hour).withMinute(59)
            val range = ChartLogic.period(period, at)!!
            assertTrue(range.hours in 1..168)
            assertFalse(at.minusHours(range.hours.toLong()).isAfter(range.start))
        }
        val six = ChartLogic.period("6 horas", now)!!
        assertEquals(now.minusHours(6), six.start)
    }

    @Test fun yesterdayDoesNotIncludeTodayEvenWhenApiReturnsBoth() {
        val yesterday = ChartLogic.period("Ontem", now)!!
        val rows = listOf(row(1, "2026-11-16 15:00:00"), row(2))
        val mapped = ChartLogic.map(rows, ChartMetric.VOLTAGE, yesterday, zone)
        assertEquals(1, mapped.size)
        assertEquals("9:1", mapped.single().key)
    }

    @Test fun sixMetricsPreserveOriginalValuesAndUnits() {
        val values = ChartMetric.entries.map { it.value(row(1)) }
        assertEquals(listOf(220.0, 1.2, 1500.0, 12.5, 60.0, 0.95), values)
        assertEquals(listOf("V", "A", "W", "kWh", "Hz", ""), ChartMetric.entries.map { it.unit })
        assertEquals("Energia acumulada", ChartMetric.ENERGY.label)
    }

    @Test fun missingInvalidAndNonFiniteReadingsAreNotZero() {
        val rows = listOf(row(1, value = null), row(2, value = "inválido"), row(3, value = Double.NaN),
            row(4, value = Double.POSITIVE_INFINITY), row(5, value = 0))
        val mapped = ChartLogic.map(rows, ChartMetric.VOLTAGE, today, zone)
        assertEquals(5, mapped.size)
        assertTrue(mapped.take(4).all { it.valor == null })
        assertEquals(0.0, mapped.last().valor)
    }

    @Test fun invalidTimestampAndMeasurementPointChangeBreakTheLine() {
        val rows = listOf(row(1), row(2, "inválido"), row(3, "2026-11-17 10:02:00"),
            row(4, "2026-11-17 10:03:00", point = 8))
        val mapped = ChartLogic.map(rows, ChartMetric.VOLTAGE, today, zone)
        assertEquals(3, mapped.size)
        assertTrue(mapped[1].breakBefore)
        assertTrue(mapped[2].breakBefore)
    }

    @Test fun accumulatedEnergyResetIsNotAContinuousSegment() {
        val rows = listOf(row(1), row(2, "2026-11-17 10:01:00").copy(energiaAcumuladaRaw = 1))
        assertTrue(ChartLogic.map(rows, ChartMetric.ENERGY, today, zone).last().breakBefore)
    }

    @Test fun timestampsKeepCivilTimesAndRespectExplicitOffsets() {
        assertEquals(LocalDateTime.parse("2026-11-17T10:00:00"), ChartLogic.timestamp("2026-11-17 10:00:00", zone))
        assertEquals(LocalDateTime.parse("2026-11-17T10:00:00"), ChartLogic.timestamp("2026-11-17T13:00:00Z", zone))
        assertNull(ChartLogic.timestamp("", zone))
    }

    @Test fun unsortedDataIsOrderedWithoutInventingContinuousSegments() {
        val mapped = ChartLogic.map(listOf(row(1, "2026-11-17 11:00:00"), row(2)), ChartMetric.VOLTAGE, today, zone)
        assertEquals("9:2", mapped.first().key)
        assertTrue(mapped.all { it.breakBefore })
    }

    @Test fun requestsRequireSessionAndRejectUnsupportedPeriod() {
        assertEquals(ChartStatus.NO_SESSION, ChartLogic.gate(false, "Hoje"))
        assertEquals(ChartStatus.UNSUPPORTED_PERIOD, ChartLogic.gate(true, "30d"))
        assertNull(ChartLogic.gate(true, "Hoje"))
    }

    @Test fun extensiveSeriesIsNotReducedAndSelectionKeySurvivesNewReadings() {
        val rows = (0 until 10000).map { row(it, now.toLocalDate().atStartOfDay().plusSeconds(it.toLong()).toString()) }
        val original = ChartLogic.map(rows, ChartMetric.VOLTAGE, today, zone)
        assertEquals(10000, original.size)
        val key = original[500].key
        val updated = ChartLogic.map(rows + row(10000), ChartMetric.VOLTAGE, today, zone)
        assertEquals(original[500], updated.single { it.key == key })
        assertEquals(9992, ChartLogic.windowStart(9990, 100f, original.size, 8))
        assertEquals(0, ChartLogic.windowStart(3, -100f, original.size, 8))
        assertEquals(original[507], ChartLogic.nearest(original, 500, 8, 1f))
        assertEquals(original[500], ChartLogic.nearest(original, 500, 8, 0f))
    }

    @Test fun emptyAndSinglePointSeriesSupportTouchAndNavigation() {
        assertNull(ChartLogic.nearest(emptyList(), 0, 8, .5f))
        val one = ChartLogic.map(listOf(row(1)), ChartMetric.VOLTAGE, today, zone)
        assertEquals(one.single(), ChartLogic.nearest(one, 0, 8, 1f))
        assertEquals(0, ChartLogic.windowStart(0, 10f, 1, 8))
    }
}
