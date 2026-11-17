package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.ui.screens.dashboard.components.ChartDemoData
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ChartDemoDataTest {
    @Test fun demonstrationCoversAllMetricsAndPeriodsWithoutLosingGaps() {
        val now = LocalDateTime.parse("2026-11-17T12:00:00")
        val zone = ZoneId.of("America/Sao_Paulo")
        for (label in ChartLogic.periods) for (metric in ChartMetric.entries) {
            val points = ChartDemoData.points(metric.key, label, now, zone)
            val range = ChartLogic.period(label, now)!!
            assertTrue(points.isNotEmpty())
            assertTrue(points.all { range.contains(it.timestamp) })
            assertTrue(points.any { it.valor == null })
            assertTrue(points.mapNotNull { it.valor }.all { it.isFinite() })
            assertTrue(points.zipWithNext().all { (a, b) -> !b.timestamp.isBefore(a.timestamp) })
        }
    }

    @Test fun unsupportedPeriodNeverFallsBackToOtherDemonstrationData() {
        assertTrue(ChartDemoData.points("tensao", "30 dias", LocalDateTime.now(), ZoneId.systemDefault()).isEmpty())
    }
}
