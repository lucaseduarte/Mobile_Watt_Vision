package com.example.watt_vision_mobile.ui.screens.dashboard.components

import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/** Amostras artificiais para exercitar desenho, lacunas, unidades, toque e rolagem. */
object ChartDemoData {
    fun points(metric: String, label: String, now: LocalDateTime, zone: ZoneId): List<MedicaoPoint> {
        val period = ChartLogic.period(label, now) ?: return emptyList()
        val seconds = Duration.between(period.start, period.end).seconds.coerceAtLeast(0)
        val voltage = listOf(220.4, 219.7, 221.0, 218.9, 220.8, 221.6, 219.5, 220.2)
        val current = listOf(8.2, 8.5, 9.1, 10.4, 9.8, 8.7, 7.9, 8.3)
        val power = listOf(1720.0, 1810.0, 1920.0, 2180.0, 2050.0, 1840.0, 1650.0, 1740.0)
        val rows = List(120) { index ->
            val sample = index % voltage.size
            MedicaoDto(
                id_medicao = index, id_pzem = -1, id_ponto_medicao = -1,
                data_hora = period.start.plusSeconds(seconds * index / 120).toString(),
                tensaoRaw = if (index in 48..49) null else voltage[sample],
                correnteRaw = if (index in 48..49) null else current[sample],
                potenciaAtivaRaw = if (index in 48..49) null else power[sample],
                energiaAcumuladaRaw = if (index in 48..49) null else 120.0 + index * 0.05,
                frequenciaRaw = if (index in 48..49) null else listOf(59.98, 60.01, 60.0, 59.99)[index % 4],
                fatorPotenciaRaw = if (index in 48..49) null else listOf(0.94, 0.96, 0.95, 0.97)[index % 4]
            )
        }
        return ChartLogic.map(rows, ChartMetric.from(metric), period, zone)
    }
}
