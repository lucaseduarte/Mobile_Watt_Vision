package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.model.MedicaoDto
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/** Valores originais da API: nenhuma integração, média ou conversão de unidade. */
enum class ChartMetric(val key: String, val label: String, val unit: String) {
    VOLTAGE("tensao", "Tensão", "V"), CURRENT("corrente", "Corrente", "A"),
    POWER("potencia", "Potência ativa", "W"), ENERGY("energia", "Energia acumulada", "kWh"),
    FREQUENCY("frequencia", "Frequência", "Hz"), POWER_FACTOR("fator_potencia", "Fator de potência", "");

    fun value(m: MedicaoDto): Double? = when (this) {
        VOLTAGE -> m.tensao
        CURRENT -> m.corrente
        POWER -> m.potencia_ativa
        ENERGY -> m.energia_acumulada
        FREQUENCY -> m.frequencia
        POWER_FACTOR -> m.fator_potencia
    }?.takeIf { it.isFinite() }

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key } ?: VOLTAGE
    }
}

enum class ChartStatus { LOADING, READY, EMPTY, ERROR, NO_SESSION, FORBIDDEN, SELECT_ENTITY, FILTER_UNSUPPORTED, UNSUPPORTED_PERIOD, NO_SENSOR, AMBIGUOUS }

data class ChartState(
    val status: ChartStatus = ChartStatus.SELECT_ENTITY,
    val pzemId: Int? = null,
    val stale: Boolean = false,
    val truncated: Boolean = false,
    val invalidTimestamps: Int = 0,
    val outOfScope: Int = 0,
    val updatedAt: LocalDateTime? = null
)

data class MedicaoPoint(
    val key: String,
    val timestamp: LocalDateTime,
    val rawTimestamp: String,
    val valor: Double?,
    val breakBefore: Boolean = false
)

data class ChartPeriod(val start: LocalDateTime, val end: LocalDateTime, val endExclusive: Boolean, val hours: Int) {
    fun contains(time: LocalDateTime) = !time.isBefore(start) &&
        (if (endExclusive) time.isBefore(end) else !time.isAfter(end))
}

object ChartLogic {
    val periods = listOf("Hoje", "6 horas", "Ontem")
    val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR"))

    // Calendário explicitamente identificado na interface. Timestamps sem offset são
    // preservados como horário civil; o contrato não informa o fuso do servidor.
    fun period(label: String, now: LocalDateTime): ChartPeriod? {
        val midnight = now.toLocalDate().atStartOfDay()
        val start = when (label) {
            "Hoje" -> midnight
            "6 horas" -> now.minusHours(6)
            "Ontem" -> midnight.minusDays(1)
            else -> return null
        }
        val end = if (label == "Ontem") midnight else now
        // Uma hora de margem evita perder a primeira amostra na fronteira da consulta.
        val hours = (ceil(Duration.between(start, now).seconds / 3600.0).toInt() + 1).coerceAtLeast(1)
        return ChartPeriod(start, end, label == "Ontem", hours)
    }

    fun timestamp(raw: String?, zone: ZoneId): LocalDateTime? {
        val normalized = raw?.trim()?.replace(' ', 'T') ?: return null
        return runCatching { OffsetDateTime.parse(normalized).atZoneSameInstant(zone).toLocalDateTime() }
            .getOrElse { runCatching { LocalDateTime.parse(normalized) }.getOrNull() }
    }

    fun gate(session: Boolean, period: String): ChartStatus? = when {
        !session -> ChartStatus.NO_SESSION
        period !in periods -> ChartStatus.UNSUPPORTED_PERIOD
        else -> null
    }

    fun map(rows: List<MedicaoDto>, metric: ChartMetric, period: ChartPeriod, zone: ZoneId, allowedPointIds: Set<Int>? = null): List<MedicaoPoint> {
        var interrupted = false
        var previousPoint: Int? = null
        var previousValue: Double? = null
        val mapped = rows.mapIndexedNotNull { index, row ->
            val time = timestamp(row.data_hora, zone)
            if (time == null) {
                interrupted = true
                return@mapIndexedNotNull null
            }
            val pointChanged = index > 0 && previousPoint != row.id_ponto_medicao
            previousPoint = row.id_ponto_medicao
            val value = if (allowedPointIds == null || row.id_ponto_medicao in allowedPointIds) metric.value(row) else null
            val reset = metric == ChartMetric.ENERGY && value != null && previousValue?.let { value < it } == true
            previousValue = value
            val sample = MedicaoPoint(
                key = "${row.id_pzem}:${row.id_medicao ?: row.data_hora}",
                timestamp = time,
                rawTimestamp = row.data_hora.orEmpty(),
                valor = value,
                breakBefore = interrupted || pointChanged || reset
            )
            interrupted = false
            sample
        }
        // Ordem inconsistente impede comprovar adjacência após reordenação: não ligar segmentos.
        val invalidOrder = mapped.zipWithNext().any { (a, b) -> b.timestamp.isBefore(a.timestamp) }
        return mapped.sortedBy { it.timestamp }.filter { period.contains(it.timestamp) }
            .map { if (invalidOrder) it.copy(breakBefore = true) else it }
    }

    fun windowStart(start: Int, delta: Float, count: Int, visible: Int): Int =
        (start + delta.toInt()).coerceIn(0, (count - visible).coerceAtLeast(0))

    fun nearest(points: List<MedicaoPoint>, first: Int, visible: Int, fraction: Float): MedicaoPoint? {
        val startIndex = first.coerceIn(0, points.size)
        val window = points.subList(startIndex, (startIndex + visible).coerceAtMost(points.size))
        if (window.isEmpty()) return null
        val start = window.first().timestamp
        val span = Duration.between(start, window.last().timestamp).toMillis().coerceAtLeast(1)
        val target = span * fraction.coerceIn(0f, 1f)
        return window.filter { it.valor != null }.minByOrNull { kotlin.math.abs(Duration.between(start, it.timestamp).toMillis() - target) }
    }
}
