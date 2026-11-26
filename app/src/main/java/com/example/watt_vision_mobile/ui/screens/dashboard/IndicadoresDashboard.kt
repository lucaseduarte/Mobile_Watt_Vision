package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.model.ConsumoDto
import com.example.watt_vision_mobile.model.MedicaoDto
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private val ptBr = Locale.forLanguageTag("pt-BR")
private val horarioDashboard = DateTimeFormatter.ofPattern("HH:mm", ptBr)

internal fun horarioDashboard(tempo: LocalDateTime) = tempo.format(horarioDashboard)

internal fun cardsDashboard(ultima: MedicaoDto?, estados: List<String?>?): List<StatCardData> {
    val status = estados?.map { it?.trim()?.lowercase(Locale.ROOT) }
    return listOf(
        StatCardData("Tensão Atual", formatar(ultima?.let(ChartMetric.VOLTAGE::value), 2, "V"),
            "Última leitura do sensor", iconName = "lucide_activity"),
        StatCardData("Dispositivos Ativos", status?.count { it in setOf("ativo", "online") }?.toString() ?: "—",
            "Status ativo ou online", tone = StatTone.GREEN, iconName = "lucide_check"),
        StatCardData("Dispositivos Offline", status?.count { it in setOf("offline", "inativo") }?.toString() ?: "—",
            "Status offline ou inativo", tone = StatTone.RED, iconName = "lucide_wifi_off"),
        StatCardData("Fator de Potência", formatar(ultima?.let(ChartMetric.POWER_FACTOR::value), 2, ""),
            "Última leitura do sensor", tone = StatTone.YELLOW, iconName = "lucide_zap")
    )
}

data class IndicadorDashboard(
    val titulo: String,
    val valor: String,
    val detalhe: String,
    val icone: String
)

internal fun periodoAnterior(periodo: ChartPeriod, rotulo: String, agora: LocalDateTime): ChartPeriod {
    val duracao = Duration.between(periodo.start, periodo.end)
    val inicio = if (rotulo == "6 horas") periodo.start.minus(duracao) else periodo.start.minusDays(1)
    val fim = inicio.plus(duracao).coerceAtMost(periodo.start)
    val horas = (ceil(Duration.between(inicio, agora).seconds / 3600.0).toInt() + 1)
    return ChartPeriod(inicio, fim, true, horas)
}

internal fun montarIndicadores(
    linhas: List<MedicaoDto>,
    periodo: ChartPeriod?,
    anterior: ChartPeriod?,
    metrica: ChartMetric,
    catalogo: ChartCatalog,
    zona: ZoneId,
    consumo: ConsumoDto?,
    calcularConsumoNaJanela: Boolean = false
): List<IndicadorDashboard> {
    data class Amostra(val hora: LocalDateTime, val id: Long?, val ponto: Int?, val valor: Double)

    val amostras = linhas.mapNotNull { linha ->
        val hora = ChartLogic.timestamp(linha.data_hora, zona) ?: return@mapNotNull null
        val valor = metrica.value(linha) ?: return@mapNotNull null
        Amostra(hora, linha.id_medicao, linha.id_ponto_medicao, valor)
    }
    val atuais = amostras.filter { periodo?.contains(it.hora) == true }
    val ultima = atuais.maxWithOrNull(compareBy({ it.hora }, { it.id }))
    val maxima = atuais.maxByOrNull { it.valor }
    val minima = atuais.minByOrNull { it.valor }
    val media = atuais.map { it.valor }.average().takeIf(Double::isFinite)
    val anteriores = amostras.filter { anterior?.contains(it.hora) == true }
    val mediaAnterior = anteriores.map { it.valor }.average()
    val variacao = media?.takeIf { mediaAnterior > 0 && mediaAnterior.isFinite() }
        ?.let { 100 * (it - mediaAnterior) / mediaAnterior }
    val detalheVariacao = when {
        atuais.isEmpty() -> "Sem leituras no período selecionado"
        anteriores.isEmpty() -> "Sem leituras no período anterior"
        mediaAnterior <= 0 -> "A média anterior precisa ser maior que zero"
        else -> "Média em relação ao período anterior"
    }
    val tarifa = consumo?.tarifa?.valor_kwh?.takeIf { it.isFinite() && it >= 0 }
    val energia = if (calcularConsumoNaJanela && periodo != null) {
        consumoNaJanela(linhas, periodo, zona)
    } else consumo?.consumo_kwh
    val custo = if (tarifa != null && energia != null) tarifa * energia
        else consumo?.custo_estimado?.takeUnless { calcularConsumoNaJanela }
    val casasCusto = if (custo != null && custo > 0 && custo < 0.01) 4 else 2
    val casas = if (metrica == ChartMetric.VOLTAGE) 2 else 3

    fun nomePonto(ponto: Int?, padrao: String): String = ponto?.let { id ->
        catalogo.pontos.firstOrNull { it.id == id }?.nome?.takeIf(String::isNotBlank) ?: "Ponto $id"
    } ?: padrao

    return listOf(
        IndicadorDashboard("Última leitura", formatar(ultima?.valor, casas, metrica.unit),
            ultima?.let { "Registrada às ${horarioDashboard(it.hora)}" } ?: "Leitura mais recente do período", "lucide_zap"),
        IndicadorDashboard("Média", formatar(media, casas, metrica.unit),
            "Média das leituras do período", "lucide_activity"),
        IndicadorDashboard("Máximo", formatar(maxima?.valor, casas, metrica.unit),
            nomePonto(maxima?.ponto, "Maior leitura do período"), "lucide_trend"),
        IndicadorDashboard("Mínimo", formatar(minima?.valor, casas, metrica.unit),
            nomePonto(minima?.ponto, "Menor leitura do período"), "lucide_clock"),
        IndicadorDashboard("Custo Estimado", moeda(custo, casasCusto) ?: "—",
            when {
                custo == null -> "Consumo ou tarifa indisponível no período"
                else -> moeda(tarifa, maximoCasas = 6)?.let { "Tarifa $it/kWh" } ?: "Custo do período"
            }, "lucide_dollar"),
        IndicadorDashboard("Variação", formatar(variacao, 2, "%"), detalheVariacao, "lucide_percent")
    )
}

private fun consumoNaJanela(linhas: List<MedicaoDto>, periodo: ChartPeriod, zona: ZoneId): Double? {
    val ordenadas = linhas.mapNotNull { linha ->
        val hora = ChartLogic.timestamp(linha.data_hora, zona)?.takeIf(periodo::contains) ?: return@mapNotNull null
        hora to linha
    }.sortedWith(compareBy({ it.first }, { it.second.id_medicao })).map { it.second }
    if (ordenadas.size < 2) return null

    fun contador(linha: MedicaoDto) = ChartMetric.ENERGY.value(linha)?.takeIf { it >= 0 }
    return ordenadas.zipWithNext().sumOf { (anterior, atual) ->
        if (atual.id_ponto_medicao != anterior.id_ponto_medicao) return null
        val primeiro = contador(anterior) ?: return null
        val segundo = contador(atual) ?: return null
        if (segundo < primeiro) return null
        segundo - primeiro
    }
}

internal val indicadoresDashboardVazios = montarIndicadores(
    emptyList(), null, null, ChartMetric.VOLTAGE, ChartCatalog(), ZoneOffset.UTC, null
)

private fun formatar(valor: Double?, casas: Int, unidade: String): String {
    if (valor == null || !valor.isFinite()) return "—"
    val numero = NumberFormat.getNumberInstance(ptBr).apply {
        minimumFractionDigits = casas
        maximumFractionDigits = casas
    }.format(valor)
    return when (unidade) {
        "" -> numero
        "%" -> "$numero%"
        else -> "$numero $unidade"
    }
}

private fun moeda(valor: Double?, maximoCasas: Int = 2): String? =
    valor?.takeIf(Double::isFinite)?.let {
        NumberFormat.getCurrencyInstance(ptBr).apply { maximumFractionDigits = maximoCasas }.format(it)
    }
