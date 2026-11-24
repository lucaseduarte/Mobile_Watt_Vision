package com.example.watt_vision_mobile.ui.screens.medicoes

import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10

internal val localeMedicoes = Locale.forLanguageTag("pt-BR")
internal val formatoEixoMedicoes = DateTimeFormatter.ofPattern("dd/MM, HH:mm", localeMedicoes)

internal fun numeroMedicao(valor: Double, casas: Int = 1, minimo: Int = casas): String =
    NumberFormat.getNumberInstance(localeMedicoes).apply {
        minimumFractionDigits = minimo
        maximumFractionDigits = casas
        isGroupingUsed = false
    }.format(valor)

internal data class JanelaMedicoes(val inicio: Instant, val fim: Instant, val exclusiva: Boolean, val horas: Int) {
    fun contem(tempo: Instant) = tempo >= inicio && (if (exclusiva) tempo < fim else tempo <= fim)
}

internal fun janelaMedicoes(periodo: String, agora: ZonedDateTime): JanelaMedicoes {
    val meiaNoite = agora.toLocalDate().atStartOfDay(agora.zone)
    val inicio = when (periodo) {
        "Ontem" -> meiaNoite.minusDays(1)
        "Últimos 7 dias" -> agora.minusHours(168)
        "Últimos 30 dias" -> agora.minusHours(720)
        else -> meiaNoite
    }.toInstant()
    val fim = if (periodo == "Ontem") meiaNoite.toInstant() else agora.toInstant()
    val horas = (ceil(Duration.between(inicio, agora.toInstant()).toMillis() / 3_600_000.0).toInt() + 1)
        .coerceIn(1, 720)
    return JanelaMedicoes(inicio, fim, periodo == "Ontem", horas)
}

internal data class AmostraMedicao(val tempo: Instant, val valor: Double)

internal data class SerieMedicoes(val trechos: List<List<AmostraMedicao>> = emptyList()) {
    val amostras = trechos.flatten()
}

internal fun instanteMedicao(raw: String?, zona: ZoneId): Instant? {
    val texto = raw?.trim()?.replace(' ', 'T') ?: return null
    return runCatching { OffsetDateTime.parse(texto).toInstant() }.getOrElse {
        runCatching { LocalDateTime.parse(texto).atZone(zona).toInstant() }.getOrNull()
    }
}

// O Desktop e o Dashboard identificam potencia_ativa como W.
internal fun valorExibidoMedicao(grandeza: ChartMetric, linha: MedicaoDto): Double? =
    grandeza.value(linha)?.let { if (grandeza == ChartMetric.POWER) it / 1_000 else it }

internal fun montarSerie(
    linhas: List<MedicaoDto>,
    grandeza: ChartMetric,
    janela: JanelaMedicoes,
    zona: ZoneId,
    pontos: Set<Int>?
): SerieMedicoes {
    data class Leitura(val tempo: Instant, val ponto: Int?, val id: Long?, val valor: Double?)
    val ordenadas = linhas.mapNotNull { linha ->
        val tempo = instanteMedicao(linha.data_hora, zona)?.takeIf(janela::contem) ?: return@mapNotNull null
        val valor = if (pontos == null || linha.id_ponto_medicao in pontos) valorExibidoMedicao(grandeza, linha) else null
        Leitura(tempo, linha.id_ponto_medicao, linha.id_medicao, valor)
    }.sortedWith(compareBy({ it.tempo }, { it.id }))

    // Uma coordenada por instante. Prefere o maior ID entre as leituras válidas daquele instante.
    val unicas = ordenadas.groupBy { it.tempo }.values.map { grupo -> grupo.lastOrNull { it.valor != null } ?: grupo.last() }
    val intervalos = unicas.zipWithNext { a, b -> Duration.between(a.tempo, b.tempo).toMillis() }.sorted()
    val limiteMs = if (intervalos.size < 2) 15 * 60_000L else
        (3 * intervalos[(intervalos.size - 1) / 2]).coerceAtLeast(60_000L)
    val trechos = mutableListOf<MutableList<AmostraMedicao>>()
    var anterior: Leitura? = null
    for (atual in unicas) {
        val previa = anterior
        anterior = atual
        val valor = atual.valor ?: continue
        if (previa?.valor == null || atual.ponto != previa.ponto ||
            Duration.between(previa.tempo, atual.tempo).toMillis() > limiteMs ||
            (grandeza == ChartMetric.ENERGY && valor < previa.valor)
        ) {
            trechos.add(mutableListOf())
        }
        trechos.last().add(AmostraMedicao(atual.tempo, valor))
    }
    return SerieMedicoes(trechos)
}

internal data class EscalaMedicoes(val min: Double, val max: Double) {
    val marcas = List(5) { min + (max - min) * it / 4 }
    val casas: Int = ceil(-log10((max - min) / 4)).toInt().coerceIn(1, 10)
}

internal fun escalaMedicoes(amostras: List<AmostraMedicao>): EscalaMedicoes {
    val min = amostras.minOf { it.valor }
    val max = amostras.maxOf { it.valor }
    val margem = if (max > min) (max - min) * .15 else (abs(min) * .02).coerceAtLeast(.001)
    return EscalaMedicoes(min - margem, max + margem)
}

internal data class ControlesMonotonicos(val primeiro: Double, val segundo: Double)

// Derivadas PCHIP com média harmônica ponderada pelo espaçamento temporal.
internal fun controlesMonotonicos(x: List<Double>, y: List<Double>): List<ControlesMonotonicos> {
    val h = x.zipWithNext { a, b -> b - a }
    val secantes = h.indices.map { i -> (y[i + 1] - y[i]) / h[i] }
    if (secantes.isEmpty()) return emptyList()
    val tangentes = y.indices.map { i ->
        when (i) {
            0 -> secantes.first()
            y.lastIndex -> secantes.last()
            else -> {
                val a = secantes[i - 1]
                val b = secantes[i]
                if (a * b <= 0) 0.0 else {
                    val w1 = 2 * h[i] + h[i - 1]
                    val w2 = h[i] + 2 * h[i - 1]
                    (w1 + w2) / (w1 / a + w2 / b)
                }
            }
        }
    }
    return secantes.indices.map { i ->
        val a = y[i]
        val b = y[i + 1]
        val limites = minOf(a, b)..maxOf(a, b)
        val c1 = (a + tangentes[i] * h[i] / 3).coerceIn(limites)
        val c2 = (b - tangentes[i + 1] * h[i] / 3).coerceIn(limites)
        if ((b - a) * (c2 - c1) < 0) {
            val meio = (c1 + c2) / 2
            ControlesMonotonicos(meio, meio)
        } else ControlesMonotonicos(c1, c2)
    }
}
