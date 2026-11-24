package com.example.watt_vision_mobile.ui.screens.relatorios

import com.example.watt_vision_mobile.ui.screens.medicoes.JanelaMedicoes
import com.example.watt_vision_mobile.ui.screens.medicoes.janelaMedicoes
import java.time.Duration
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt

internal val ptRelatorio: Locale = Locale.forLanguageTag("pt-BR")

internal fun numeroRelatorio(valor: Double, casas: Int = 1): String =
    NumberFormat.getNumberInstance(ptRelatorio).apply {
        minimumFractionDigits = casas
        maximumFractionDigits = casas
    }.format(valor)

internal fun moedaRelatorio(valor: BigDecimal): String =
    NumberFormat.getCurrencyInstance(ptRelatorio).format(valor.setScale(2, RoundingMode.HALF_UP))

internal fun decimalRelatorio(texto: String): BigDecimal? =
    texto.trim().takeIf { it.matches(Regex("[+-]?[0-9]+(?:[,.][0-9]+)?")) }
        ?.replace(',', '.')?.toBigDecimalOrNull()

private fun custoRelatorio(energia: Double, tarifa: BigDecimal) = BigDecimal.valueOf(energia).multiply(tarifa)

internal fun montarRelatorio(
    fonte: FonteRelatorio,
    filtros: FiltrosRelatorio,
    tarifa: BigDecimal,
    agora: ZonedDateTime
): Relatorio {
    val janelaBase = janelaMedicoes(filtros.periodo.titulo, agora)
    val janela = if (filtros.periodo == PeriodoRelatorio.SEIS_HORAS)
        janelaBase.copy(inicio = agora.minusHours(6).toInstant()) else janelaBase
    val energia = GrandezaRelatorio.ENERGIA
    fun noEscopo(local: String, setor: String, equipamento: String?) =
        (filtros.localId == null || filtros.localId == local) &&
            (filtros.setorId == null || filtros.setorId == setor) &&
            (filtros.equipamentoId == null || filtros.equipamentoId == equipamento)

    val cadencia = fonte.cadenciaSegundos?.takeIf { it > 0 }
    var inconsistencias = fonte.registrosInvalidos
    val recebidas = fonte.leituras.asSequence()
        .filter { janela.contem(it.instante) && noEscopo(it.localId, it.setorId, it.equipamentoId) }
        .groupBy { it.medidor to it.instante }
        .values.map { grupo ->
            val escolhida = grupo.maxBy { it.id ?: Long.MIN_VALUE }
            if (grupo.any { it.id == escolhida.id && it.valores != escolhida.valores }) inconsistencias = true
            val valores = escolhida.valores.filter { (grandeza, valor) ->
                valor.isFinite() && (grandeza != energia || valor >= 0)
            }
            if (valores.size < escolhida.valores.size || energia !in valores) inconsistencias = true
            escolhida.copy(valores = valores)
        }
        .sortedWith(compareBy({ it.instante }, { it.medidor }, { it.id }))
    val leituras = recebidas.filter { it.valores.isNotEmpty() }
    val porMedidor = recebidas.groupBy { it.medidor }
    val consumoPorDia = porMedidor.values.flatMap { registros ->
        registros.zipWithNext { anterior, atual ->
            val antes = anterior.valores[energia]
            val depois = atual.valores[energia]
            val lacuna = cadencia != null &&
                Duration.between(anterior.instante, atual.instante) > Duration.ofSeconds(cadencia)
            if (antes == null || depois == null || depois < antes || lacuna) {
                inconsistencias = true
                null
            } else {
                atual.instante.atZone(agora.zone).toLocalDate() to (depois - antes)
            }
        }.filterNotNull()
    }.groupingBy { it.first }.fold(0.0) { total, (_, delta) -> total + delta }
    val esperados = fonte.medidores.filterValues { noEscopo(it.localId, it.setorId, it.id) }.keys
    val chavesConhecidas = cadencia != null && esperados.isNotEmpty()
    val gradeCompleta = chavesConhecidas && porMedidor.keys == esperados &&
        porMedidor.values.all { coincideComGrade(it, janela, requireNotNull(cadencia)) }
    val cobertura = when {
        leituras.isEmpty() && !inconsistencias && !fonte.limiteAtingido -> CoberturaRelatorio.SEM_DADOS
        inconsistencias || fonte.limiteAtingido || (chavesConhecidas && !gradeCompleta) -> CoberturaRelatorio.PARCIAL
        gradeCompleta -> CoberturaRelatorio.COMPROVADA
        else -> CoberturaRelatorio.INDETERMINADA
    }
    val dias = leituras.groupBy { it.instante.atZone(agora.zone).toLocalDate() }.map { (dia, registros) ->
        val valores = buildMap {
            consumoPorDia[dia]?.let { put(energia, it) }
            GrandezaRelatorio.entries.filter { it != energia }.forEach { grandeza ->
                val amostras = registros.mapNotNull { it.valores[grandeza]?.takeIf(Double::isFinite) }
                if (amostras.isNotEmpty()) put(grandeza, amostras.average())
            }
        }
        DiaRelatorio(dia, registros.size, valores, valores[energia]?.let { custoRelatorio(it, tarifa) })
    }.asReversed()
    val amostra = if (leituras.size <= 120) leituras else List(120) { indice ->
        leituras[(indice * (leituras.lastIndex / 119.0)).roundToInt()]
    }
    val series = GrandezaRelatorio.entries.associateWith { grandeza ->
        val pontos = amostra.mapNotNull { leitura ->
            leitura.valores[grandeza]?.takeIf { it.isFinite() && (grandeza != energia || it >= 0) }
                ?.let { PontoRelatorio(leitura.instante, it, leitura.medidor) }
        }
        SerieRelatorio(pontos, leituras.count { it.valores[grandeza]?.let { v -> v.isFinite() && (grandeza != energia || v >= 0) } == true })
    }
    val consumo = consumoPorDia.values.takeIf { it.isNotEmpty() }?.sum()
    return Relatorio(
        leituras.size, amostra.size, series, dias, consumo,
        consumo?.div(consumoPorDia.size), consumoPorDia.values.maxOrNull(),
        consumo?.let { custoRelatorio(it, tarifa) }, agora.zone, leituras,
        cobertura, fonte.limiteAtingido, inconsistencias
    )
}

private fun coincideComGrade(registros: List<LeituraRelatorio>, janela: JanelaMedicoes, cadencia: Long): Boolean {
    val inicio = janela.inicio.epochSecond + if (janela.inicio.nano > 0) 1 else 0
    val fim = janela.fim.epochSecond - if (janela.exclusiva && janela.fim.nano == 0) 1 else 0
    val primeiro = inicio + Math.floorMod(-inicio, cadencia)
    val total = if (primeiro > fim) 0 else (fim - primeiro) / cadencia + 1
    return registros.size.toLong() == total && registros.withIndex().all { (indice, leitura) ->
        leitura.instante.nano == 0 && leitura.instante.epochSecond == primeiro + indice * cadencia
    }
}
