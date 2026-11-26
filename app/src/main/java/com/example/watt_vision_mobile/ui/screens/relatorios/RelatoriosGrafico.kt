package com.example.watt_vision_mobile.ui.screens.relatorios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.theme.CardBorder
import com.example.watt_vision_mobile.ui.theme.InputBackground
import com.example.watt_vision_mobile.ui.theme.PrimaryBlue
import com.example.watt_vision_mobile.ui.theme.TextMuted
import com.example.watt_vision_mobile.ui.theme.TextSubtle
import com.example.watt_vision_mobile.ui.theme.TextWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10

internal data class EscalaRelatorio(val minimo: Double, val maximo: Double) {
    val casas: Int get() = ceil(-log10((maximo - minimo) / 4)).toInt().coerceIn(1, 8)
    fun fracao(valor: Double): Float = ((valor - minimo) / (maximo - minimo)).toFloat()
}

internal fun escalaAutomatica(pontos: List<PontoRelatorio>, grandeza: GrandezaRelatorio): EscalaRelatorio {
    val minimo = pontos.minOf { it.valor }
    val maximo = pontos.maxOf { it.valor }
    val amplitude = maximo - minimo
    val margem = if (amplitude > 0) amplitude * 0.15 else (abs(minimo) * 0.02).coerceAtLeast(0.001)
    val inferior = minimo - margem
    val superior = maximo + margem
    return if (grandeza == GrandezaRelatorio.FATOR && minimo >= 0 && maximo <= 1) {
        EscalaRelatorio(inferior.coerceAtLeast(0.0), superior.coerceAtMost(1.0))
    } else EscalaRelatorio(inferior, superior)
}

private val eixoMinutos = DateTimeFormatter.ofPattern("dd/MM HH:mm", ptRelatorio)
private val eixoSegundos = DateTimeFormatter.ofPattern("HH:mm:ss", ptRelatorio)
private val horarioLeitura = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm:ss", ptRelatorio)

@Composable
internal fun GraficoRelatorio(
    grandeza: GrandezaRelatorio, serie: SerieRelatorio, zona: ZoneId, acompanhar: Boolean
) {
    var verPeriodoCompleto by rememberSaveable(acompanhar) { mutableStateOf(!acompanhar) }
    val periodoCompleto = serie.pontos.ifEmpty { serie.pontosRecentes }
    val podeAlternar = serie.pontosRecentes != periodoCompleto
    val pontos = if (verPeriodoCompleto || !podeAlternar) periodoCompleto else serie.pontosRecentes
    var ajustar by rememberSaveable { mutableStateOf(false) }
    var minimoTexto by rememberSaveable { mutableStateOf("") }
    var maximoTexto by rememberSaveable { mutableStateOf("") }
    CardRelatorio {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(grandeza.titulo, style = MaterialTheme.typography.titleSmall)
                Text(if (serie.quantidade == 1) "1 leitura" else "${serie.quantidade} leituras", color = TextSubtle, fontSize = 12.sp)
            }
            if (pontos.isNotEmpty()) {
                IconButton(onClick = { ajustar = !ajustar }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Escala Y de ${grandeza.titulo}")
                }
            }
        }
        if (pontos.isEmpty()) {
            Text("Sem pontos disponíveis para esta grandeza.", color = TextSubtle)
        } else {
            if (podeAlternar) {
                TextButton(onClick = { verPeriodoCompleto = !verPeriodoCompleto }) {
                    Text(if (verPeriodoCompleto) "Voltar às últimas leituras" else "Ver período completo")
                }
            }
            val auto = remember(pontos, grandeza) { escalaAutomatica(pontos, grandeza) }
            val minimo = if (minimoTexto.isBlank()) auto.minimo else decimalRelatorio(minimoTexto)?.toDouble()
            val maximo = if (maximoTexto.isBlank()) auto.maximo else decimalRelatorio(maximoTexto)?.toDouble()
            val escalaManual = if (minimo != null && maximo != null && minimo.isFinite() && maximo.isFinite() &&
                minimo < maximo && (grandeza != GrandezaRelatorio.FATOR || (minimo >= 0 && maximo <= 1))
            ) EscalaRelatorio(minimo, maximo) else null
            AnimatedVisibility(visible = ajustar) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Escala do eixo Y", style = MaterialTheme.typography.titleSmall)
                    Text("Automática ou personalizada", color = TextSubtle, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampoEscala("Mínima", minimoTexto, numeroRelatorio(auto.minimo, auto.casas), escalaManual == null, Modifier.weight(1f)) { minimoTexto = it }
                        CampoEscala("Máxima", maximoTexto, numeroRelatorio(auto.maximo, auto.casas), escalaManual == null, Modifier.weight(1f)) { maximoTexto = it }
                    }
                    if (escalaManual == null) {
                        Text("Informe limites válidos com mínima menor que máxima.", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { minimoTexto = ""; maximoTexto = "" }) { Text("Automático") }
                }
            }
            GraficoLinha(grandeza, pontos, escalaManual ?: auto, zona)
        }
    }
}

@Composable
private fun CampoEscala(
    titulo: String, texto: String, automatico: String, erro: Boolean,
    modifier: Modifier, onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = texto, onValueChange = { onChange(it.take(16)) }, modifier = modifier,
        label = { Text(titulo) },
        placeholder = { Text(automatico) },
        isError = erro, singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InputBackground,
            unfocusedContainerColor = InputBackground,
            errorContainerColor = InputBackground,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = CardBorder,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedLabelColor = TextSubtle,
            unfocusedLabelColor = TextMuted,
            focusedPlaceholderColor = TextSubtle,
            unfocusedPlaceholderColor = TextMuted
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun GraficoLinha(
    grandeza: GrandezaRelatorio, pontos: List<PontoRelatorio>, escala: EscalaRelatorio, zona: ZoneId
) {
    var selecionado by remember { mutableStateOf<Pair<Instant, String>?>(null) }
    val indiceSelecionado = pontos.indexOfFirst { it.instante == selecionado?.first && it.medidor == selecionado?.second }
    fun selecionarIndice(indice: Int) {
        val ponto = pontos[indice]
        selecionado = ponto.instante to ponto.medidor
    }
    val inicio = pontos.first().instante.toEpochMilli()
    val intervalo = (pontos.last().instante.toEpochMilli() - inicio).coerceAtLeast(1)
    fun fracao(instante: Instant): Float = if (pontos.first().instante == pontos.last().instante) 0.5f else
        ((instante.toEpochMilli() - inicio).toDouble() / intervalo).toFloat()
    fun selecionar(x: Float, largura: Int) {
        selecionarIndice(pontos.indices.minBy { abs(fracao(pontos[it].instante) - x / largura.coerceAtLeast(1)) })
    }
    val corGrade = CardBorder
    val corSelecao = corTema(0xFFFFFFFF, 0xFF64748B)
    val corGrandeza = when (grandeza) {
        GrandezaRelatorio.ENERGIA -> corTema(0xFF3B82F6, 0xFF1D4ED8)
        GrandezaRelatorio.POTENCIA -> corTema(0xFF10B981, 0xFF166534)
        GrandezaRelatorio.TENSAO -> corTema(0xFF6366F1, 0xFF4338CA)
        GrandezaRelatorio.CORRENTE -> corTema(0xFFF59E0B, 0xFF854D0E)
        GrandezaRelatorio.FREQUENCIA -> corTema(0xFFA855F7, 0xFF7E22CE)
        GrandezaRelatorio.FATOR -> corTema(0xFFF97316, 0xFF9A3412)
    }
    val grupos = remember(pontos) { pontos.groupBy { it.medidor }.values }
    val atual = pontos.getOrNull(indiceSelecionado)
    val horaLeitura = atual?.let { horarioLeitura.format(it.instante.atZone(zona)) }
    val eixoX = if (intervalo < 3_600_000) eixoSegundos else eixoMinutos
    val valorLeitura = atual?.let {
        val casas = maxOf(grandeza.casas, escala.casas)
        "${grandeza.titulo}: ${numeroRelatorio(it.valor, casas)} ${grandeza.unidade}".trimEnd()
    }
    Text("Toque ou arraste para consultar uma leitura", fontSize = 12.sp, color = TextSubtle)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.height(170.dp), verticalArrangement = Arrangement.SpaceBetween) {
            for (indice in 4 downTo 0) {
                val valor = escala.minimo + (escala.maximo - escala.minimo) * indice / 4
                Text(numeroRelatorio(valor, escala.casas), fontSize = 10.sp, color = TextSubtle)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BoxWithConstraints {
                Canvas(
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                        .semantics {
                            contentDescription = if (horaLeitura != null && valorLeitura != null) {
                                "$horaLeitura, $valorLeitura"
                            } else {
                                "Gráfico de ${grandeza.titulo}"
                            }
                            customActions = listOf(
                                CustomAccessibilityAction("Leitura anterior") {
                                    selecionarIndice((indiceSelecionado - 1).coerceAtLeast(0))
                                    true
                                },
                                CustomAccessibilityAction("Próxima leitura") {
                                    selecionarIndice((indiceSelecionado + 1).coerceAtMost(pontos.lastIndex))
                                    true
                                }
                            )
                        }
                        .pointerInput(pontos) { detectTapGestures { selecionar(it.x, size.width) } }
                        .pointerInput(pontos) {
                            detectHorizontalDragGestures(
                                onDragStart = { selecionar(it.x, size.width) },
                                onHorizontalDrag = { change, _ -> selecionar(change.position.x, size.width) }
                            )
                        }
                ) {
                    for (indice in 0..4) {
                        val y = indice * size.height / 4f
                        drawLine(corGrade, Offset(0f, y), Offset(size.width, y))
                    }
                    fun coordenada(ponto: PontoRelatorio) =
                        Offset(fracao(ponto.instante) * size.width, (1 - escala.fracao(ponto.valor)) * size.height)
                    clipRect {
                        grupos.forEach { grupo ->
                            if (grupo.size == 1) {
                                drawCircle(corGrandeza, radius = 3.dp.toPx(), center = coordenada(grupo.first()))
                            } else {
                                val path = Path()
                                var anterior = coordenada(grupo.first())
                                path.moveTo(anterior.x, anterior.y)
                                grupo.drop(1).forEach { ponto ->
                                    val atualPonto = coordenada(ponto)
                                    val meio = (anterior.x + atualPonto.x) / 2
                                    path.cubicTo(meio, anterior.y, meio, atualPonto.y, atualPonto.x, atualPonto.y)
                                    anterior = atualPonto
                                }
                                drawPath(path, corGrandeza, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                        atual?.let {
                            val posicao = coordenada(it)
                            drawLine(corSelecao.copy(alpha = 0.65f), Offset(posicao.x, 0f), Offset(posicao.x, size.height))
                            drawCircle(corGrandeza, 4.dp.toPx(), posicao)
                        }
                    }
                }
                if (atual != null && horaLeitura != null && valorLeitura != null) {
                    val larguraTooltip = minOf(196.dp, maxWidth)
                    val esquerda = (maxWidth * fracao(atual.instante) - larguraTooltip / 2)
                        .coerceIn(0.dp, maxWidth - larguraTooltip)
                    Surface(
                        modifier = Modifier.offset(x = esquerda, y = 4.dp).width(larguraTooltip),
                        shape = RoundedCornerShape(12.dp),
                        color = corTema(0xFF101827, 0xFFFFFFFF),
                        border = BorderStroke(1.dp, corTema(0xFF263448, 0xFFCBD8E7))
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(horaLeitura, fontSize = 11.sp, color = corTema(0xFFE2E8F0, 0xFF111827))
                            Text(valorLeitura, fontSize = 12.sp, color = corGrandeza)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(eixoX.format(pontos.first().instante.atZone(zona)), fontSize = 10.sp, color = TextSubtle)
                if (pontos.last().instante != pontos.first().instante) {
                    Text(eixoX.format(pontos.last().instante.atZone(zona)), fontSize = 10.sp, color = TextSubtle)
                }
            }
        }
    }
}
