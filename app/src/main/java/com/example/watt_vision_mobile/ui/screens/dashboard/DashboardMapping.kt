package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.data.repository.DashboardApiException
import com.example.watt_vision_mobile.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

object DashboardValues {
    val locale: Locale = Locale.forLanguageTag("pt-BR")
    val periods = mapOf("1h" to 1, "6h" to 6, "24h" to 24, "7d" to 168)
    val metrics = listOf("tensao" to "Tensão", "corrente" to "Corrente", "potencia" to "Potência",
        "energia" to "Energia acumulada", "frequencia" to "Frequência", "fp" to "Fator de potência")
    fun unit(metric: String) = when (metric) {
        "tensao" -> "V"; "corrente" -> "A"; "potencia" -> "kW"; "energia" -> "kWh"; "frequencia" -> "Hz"; else -> "sem unidade"
    }
    fun kw(watts: Double?): Double? = watts?.takeIf { it.isFinite() && it >= 0 }?.div(1000.0)
    fun number(value: Double?, unit: String = "", digits: Int = 2): String {
        if (value == null || !value.isFinite()) return "Indisponível"
        val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = digits; minimumFractionDigits = digits }
        return "${format.format(value)}${if (unit.isBlank()) "" else " $unit"}"
    }
    fun money(value: Double?): String = value?.takeIf { it.isFinite() && it >= 0 }
        ?.let { NumberFormat.getCurrencyInstance(locale).format(it) } ?: "Indisponível"
    fun value(m: MedicaoDto, metric: String): Double? = when (metric) {
        "tensao" -> m.tensao; "corrente" -> m.corrente; "potencia" -> kw(m.potencia_ativa)
        "energia" -> m.energia_acumulada; "frequencia" -> m.frequencia
        "fp" -> m.fator_potencia?.takeIf { it in 0.0..1.0 }; else -> null
    }?.takeIf { it.isFinite() && it >= 0 }

    data class Stamp(val position: Long, val label: String, val kind: String)
    fun timestamp(raw: String?): Stamp? {
        if (raw.isNullOrBlank()) return null
        val text = raw.replace(' ', 'T')
        return try {
            val parsed = OffsetDateTime.parse(text)
            Stamp(parsed.toInstant().toEpochMilli(), parsed.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss XXX")), "offset")
        } catch (_: Exception) {
            try {
                val parsed = LocalDateTime.parse(text)
                // Eixo relativo: UTC apenas como coordenada civil; não afirma o fuso do dado.
                Stamp(parsed.toInstant(ZoneOffset.UTC).toEpochMilli(), parsed.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss")), "civil")
            } catch (_: Exception) { null }
        }
    }
    fun date(text: String): String? = try {
        LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)).toString()
    } catch (_: Exception) { null }
    fun displayDate(iso: String): String = try { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/uuuu")) } catch (_: Exception) { "Data inválida" }
    fun validDates(start: String, end: String): Boolean = try { !LocalDate.parse(start).isAfter(LocalDate.parse(end)) } catch (_: Exception) { false }
    fun simulation(energy: String, tariff: String): BigDecimal? {
        fun decimal(s: String) = s.trim().takeIf { it.matches(Regex("[0-9]+([,.][0-9]+)?")) }?.replace(',', '.')?.toBigDecimalOrNull()
        val kwh = decimal(energy) ?: return null
        val price = decimal(tariff) ?: return null
        return (kwh * price).setScale(2, RoundingMode.HALF_UP)
    }
    fun error(error: Throwable): String = when ((error as? DashboardApiException)?.code) {
        401 -> "Sessão ausente ou inválida. Use Entrar na API no Debug para consultar dados reais."
        403 -> "Acesso não autorizado a esta consulta."
        404 -> "Nenhum registro encontrado para a consulta."
        422 -> "Cálculo indisponível: verifique cobertura tarifária, leituras e possíveis resets ou mudança de ponto."
        502 -> "Resposta incompleta ou inválida da API."
        null -> "Falha de comunicação. Tente novamente."
        else -> "Consulta indisponível no servidor. Tente novamente."
    }
}

fun DashboardCatalog.pointsFor(s: DashboardSelection): List<PontoConsultaDto> = pontos.filter { p ->
    val eq = equipamentos.firstOrNull { it.id == p.id_equipamento }
    val sectorId = p.id_setor ?: eq?.id_setor
    val localId = p.id_local ?: setores.firstOrNull { it.id == sectorId }?.id_local
    (s.location == null || localId == s.location) && (s.sector == null || sectorId == s.sector) &&
        (s.equipment == null || p.id_equipamento == s.equipment) && (s.point == null || p.id == s.point)
}
fun DashboardCatalog.sensorsFor(s: DashboardSelection): List<PzemConsultaDto> {
    val ids = pointsFor(s).mapNotNull { it.id }.toSet()
    return sensores.filter { it.id_ponto_medicao in ids }
}
fun chartPoints(data: List<MedicaoDto>, metric: String): List<MedicaoPoint> {
    val stamped = data.mapNotNull { m -> DashboardValues.timestamp(m.data_hora)?.let { m to it } }
    val kind = stamped.firstOrNull()?.second?.kind
    return stamped.filter { it.second.kind == kind }.mapNotNull { (m, time) ->
        DashboardValues.value(m, metric)?.toFloat()?.takeIf { it.isFinite() }?.let { MedicaoPoint(time.label, it, time.position) }
    }.sortedBy { it.position }
}

fun alarmData(alert: AlertaDto, device: Int): AlarmData = AlarmData(
    title = alert.tipo_alerta ?: alert.tipo ?: "Alarme #${alert.id ?: "—"}",
    equipment = "${alert.mensagem ?: "Mensagem não informada"} · Dispositivo #$device" +
        (alert.id_equipamento?.let { " · Equipamento #$it" } ?: ""),
    time = DashboardValues.timestamp(alert.data_hora)?.label ?: "Data não informada",
    type = when (alert.severidade?.lowercase(Locale.ROOT)) {
        "critico" -> AlarmType.CRITICAL; "atencao" -> AlarmType.WARNING; "informativo" -> AlarmType.INFO; else -> AlarmType.UNKNOWN
    }, status = alert.status ?: "Não informado", severity = alert.severidade ?: "Não informada"
)
