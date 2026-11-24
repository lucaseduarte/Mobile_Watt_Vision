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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal data class EscalaRelatorio(val minimo: Double, val maximo: Double) {
    fun fracao(valor: Double): Float = ((valor - minimo) / (maximo - minimo)).toFloat()
}

internal fun escalaAutomatica(pontos: List<PontoRelatorio>, grandeza: GrandezaRelatorio): EscalaRelatorio {
    val min = pontos.minOf { it.valor }
    val max = pontos.maxOf { it.valor }
    val faixa = max - min
    val margem = if (faixa > 0) faixa * 0.1 else (abs(max) * 0.05).coerceAtLeast(0.005)
    val bruto = ((faixa + 2 * margem) / 4).coerceAtLeast(0.000001)
    val base = 10.0.pow(floor(log10(bruto)))
    val passo = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * base >= bruto } * base
    val inferior = floor((min - margem) / passo) * passo
    val superior = ceil((max + margem) / passo) * passo
    if (grandeza == GrandezaRelatorio.FATOR && min >= 0 && max <= 1) {
        return EscalaRelatorio(inferior.coerceAtLeast(0.0), superior.coerceAtMost(1.0))
    }
    return EscalaRelatorio(if (min >= 0) inferior.coerceAtLeast(0.0) else inferior, superior)
}

private val eixoX = DateTimeFormatter.ofPattern("HH:mm", ptRelatorio)

@Composable
internal fun GraficoRelatorio(grandeza: GrandezaRelatorio, serie: SerieRelatorio, zona: ZoneId) {
    var ajustar by rememberSaveable { mutableStateOf(false) }
    var minimoTexto by rememberSaveable { mutableStateOf("") }
    var maximoTexto by rememberSaveable { mutableStateOf("") }
    CardRelatorio {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(grandeza.titulo, style = MaterialTheme.typography.titleSmall)
                Text(if (serie.quantidade == 1) "1 leitura" else "${serie.quantidade} leituras", color = TextSubtle, fontSize = 12.sp)
            }
            if (serie.pontos.isNotEmpty()) {
                IconButton(onClick = { ajustar = !ajustar }) {
                    Icon(Icons.Filled.Tune, contentDescription = "Escala Y de ${grandeza.titulo}")
                }
            }
        }
        if (serie.pontos.isEmpty()) {
            Text("Sem pontos disponíveis para esta grandeza.", color = TextSubtle)
        } else {
            val auto = remember(serie, grandeza) { escalaAutomatica(serie.pontos, grandeza) }
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
                        CampoEscala("Mínima", minimoTexto, auto.minimo, escalaManual == null, Modifier.weight(1f)) { minimoTexto = it }
                        CampoEscala("Máxima", maximoTexto, auto.maximo, escalaManual == null, Modifier.weight(1f)) { maximoTexto = it }
                    }
                    if (escalaManual == null) {
                        Text("Informe limites válidos com mínima menor que máxima.", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { minimoTexto = ""; maximoTexto = "" }) { Text("Automático") }
                }
            }
            GraficoLinha(grandeza, serie.pontos, escalaManual ?: auto, zona)
        }
    }
}

@Composable
private fun CampoEscala(
    titulo: String, texto: String, automatico: Double, erro: Boolean,
    modifier: Modifier, onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = texto, onValueChange = { onChange(it.take(16)) }, modifier = modifier,
        label = { Text(titulo) },
        placeholder = { Text(numeroRelatorio(automatico, 3)) },
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
    var selecionado by remember(pontos) { mutableIntStateOf(-1) }
    val inicio = pontos.first().instante.toEpochMilli()
    val intervalo = (pontos.last().instante.toEpochMilli() - inicio).coerceAtLeast(1)
    fun fracao(instante: Instant): Float = if (pontos.first().instante == pontos.last().instante) 0.5f else
        ((instante.toEpochMilli() - inicio).toDouble() / intervalo).toFloat()
    fun selecionar(x: Float, largura: Int) {
        selecionado = pontos.indices.minBy { abs(fracao(pontos[it].instante) - x / largura.coerceAtLeast(1)) }
    }
    val grupos = remember(pontos) { pontos.groupBy { it.medidor }.values }
    val atual = pontos.getOrNull(selecionado)
    val horaLeitura = atual?.let { eixoX.format(it.instante.atZone(zona)) }
    val valorLeitura = atual?.let {
        "${grandeza.titulo}: ${numeroRelatorio(it.valor, grandeza.casas)} ${grandeza.unidade}".trimEnd()
    }
    Text("Toque ou arraste para consultar uma leitura", fontSize = 12.sp, color = TextSubtle)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.height(170.dp), verticalArrangement = Arrangement.SpaceBetween) {
            for (indice in 4 downTo 0) {
                val valor = escala.minimo + (escala.maximo - escala.minimo) * indice / 4
                Text(numeroRelatorio(valor, if (escala.maximo - escala.minimo < 2) 3 else 1), fontSize = 10.sp, color = TextSubtle)
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
                                    selecionado = (selecionado - 1).coerceAtLeast(0)
                                    true
                                },
                                CustomAccessibilityAction("Próxima leitura") {
                                    selecionado = (selecionado + 1).coerceAtMost(pontos.lastIndex)
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
                        drawLine(CardBorder, Offset(0f, y), Offset(size.width, y))
                    }
                    fun coordenada(ponto: PontoRelatorio) =
                        Offset(fracao(ponto.instante) * size.width, (1 - escala.fracao(ponto.valor)) * size.height)
                    clipRect {
                        grupos.forEach { grupo ->
                            if (grupo.size == 1) {
                                drawCircle(grandeza.cor, radius = 3.dp.toPx(), center = coordenada(grupo.first()))
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
                                drawPath(path, grandeza.cor, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                        atual?.let {
                            val posicao = coordenada(it)
                            drawLine(Color.White.copy(alpha = 0.65f), Offset(posicao.x, 0f), Offset(posicao.x, size.height))
                            drawCircle(grandeza.cor, 4.dp.toPx(), posicao)
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
                        color = Color(0xFF101827),
                        border = BorderStroke(1.dp, Color(0xFF263448))
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(horaLeitura, fontSize = 11.sp, color = Color(0xFFE2E8F0))
                            Text(valorLeitura, fontSize = 12.sp, color = grandeza.cor)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(eixoX.format(pontos.first().instante.atZone(zona)), fontSize = 10.sp, color = TextSubtle)
                Text(eixoX.format(pontos.last().instante.atZone(zona)), fontSize = 10.sp, color = TextSubtle)
            }
        }
    }
}
