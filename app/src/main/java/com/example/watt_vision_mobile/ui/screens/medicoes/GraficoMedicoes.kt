package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.theme.LinkBlue
import com.example.watt_vision_mobile.ui.theme.Slate900
import com.example.watt_vision_mobile.ui.theme.SurfaceDark
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val FundoGrafico
    @Composable get() = corTema(0xFF0F1627, 0xFFFFFFFF)
private val BordaGrafico
    @Composable get() = corTema(0xFF253246, 0xFFCBD8E7)
private val CorTitulo
    @Composable get() = corTema(0xFFF1F5F9, 0xFF111827)
private val CorEixos
    @Composable get() = corTema(0xFF94A3B8, 0xFF475569)
private val CorGrade
    @Composable get() = corTema(0xFF334155, 0xFFCBD5E1)

private val passosTempo = listOf(1L, 2L, 5L, 10L, 15L, 30L) +
    listOf(1, 2, 5, 10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 4320, 10080).map { it * 60L }

@Composable
internal fun GraficoMedicoes(
    grandeza: ChartMetric,
    unidade: String,
    periodo: String,
    consulta: ConsultaMedicoes
) {
    val corCurva = when (grandeza) {
        ChartMetric.VOLTAGE -> LinkBlue
        ChartMetric.CURRENT -> corTema(0xFF22C55E, 0xFF166534)
        ChartMetric.POWER -> corTema(0xFF14B8A6, 0xFF0F766E)
        ChartMetric.ENERGY -> corTema(0xFFEAB308, 0xFF854D0E)
        ChartMetric.FREQUENCY -> corTema(0xFFA855F7, 0xFF7E22CE)
        ChartMetric.POWER_FACTOR -> corTema(0xFFF97316, 0xFF9A3412)
    }
    val total = consulta.serie.amostras.size
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 296.dp),
        color = FundoGrafico,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BordaGrafico)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cabecalho: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${grandeza.label} ao Longo do Tempo", color = CorTitulo, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Histórico de medições · $periodo", color = CorEixos, fontSize = 12.sp)
                    }
                }
                val contagem = if (total == 1) "1 registro" else "$total registros"
                if (maxWidth < 290.dp) {
                    Column(Modifier.fillMaxWidth()) {
                        cabecalho()
                        Text(contagem, Modifier.align(Alignment.End), color = CorEixos, fontSize = 12.sp)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { cabecalho() }
                        Text(contagem, color = CorEixos, fontSize = 12.sp)
                    }
                }
            }
            if (total > 0) {
                key(grandeza, periodo, consulta.registros.firstOrNull()?.dados?.id_pzem) {
                    JanelaGraficoMedicoes(consulta.serie, grandeza.label, unidade, corCurva, consulta.zona, periodo != "Ontem")
                }
            } else {
                Box(Modifier.fillMaxWidth().height(186.dp), contentAlignment = Alignment.Center) {
                    if (consulta.carregando) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = LinkBlue, strokeWidth = 2.dp)
                            Text("Carregando medições…", color = CorEixos, fontSize = 13.sp)
                        }
                    } else {
                        Text(
                            consulta.aviso ?: "Nenhuma medição válida no período.",
                            color = CorEixos, fontSize = 13.sp
                        )
                    }
                }
            }
            if (total > 0 && consulta.aviso != null) {
                Text(consulta.aviso, color = CorEixos, fontSize = 12.sp)
            }
            consulta.tentarNovamente?.let { tentar ->
                TextButton(onClick = tentar, modifier = Modifier.align(Alignment.End)) {
                    Text("Tentar novamente")
                }
            }
        }
    }
}

private data class TrechoPlot(val inicio: AmostraMedicao, val seguintes: List<ParcelaPlot>)
private data class ParcelaPlot(val fim: AmostraMedicao, val curva: ControlesMonotonicos)

@Composable
private fun JanelaGraficoMedicoes(
    serie: SerieMedicoes,
    grandeza: String,
    unidade: String,
    corCurva: Color,
    zona: ZoneId,
    acompanhar: Boolean
) {
    val amostras = serie.amostras
    var ancora by remember { mutableStateOf<Instant?>(null) }
    var deslocamento by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val quantidade = ((maxWidth.value - 64) / 12).toInt().coerceIn(2, 24)
        val ultimoInicio = (amostras.size - quantidade).coerceAtLeast(0)
        val inicioRecente = maxOf(ultimoInicio, amostras.size - serie.trechos.last().size)
        val ancorado = ancora?.let { tempo -> amostras.binarySearchBy(tempo) { it.tempo } } ?: -1
        val seguindo = acompanhar && ancorado < 0
        val inicio = when {
            seguindo -> inicioRecente
            ancorado >= 0 -> ancorado.coerceAtMost(ultimoInicio)
            else -> 0
        }
        val janela = amostras.subList(inicio, (inicio + quantidade).coerceAtMost(amostras.size))
        val trechos = remember(serie, janela) {
            val periodo = janela.first().tempo..janela.last().tempo
            serie.trechos.mapNotNull { trecho ->
                trecho.filter { it.tempo in periodo }.takeIf { it.isNotEmpty() }
            }
        }
        val larguraPx = with(LocalDensity.current) { maxWidth.toPx() }
        val drag = rememberDraggableState { delta ->
            deslocamento -= delta * quantidade / larguraPx.coerceAtLeast(1f)
            val passos = deslocamento.toInt()
            if (passos != 0) {
                val destino = (inicio + passos).coerceAtLeast(0)
                val proximo = destino.coerceAtMost(ultimoInicio)
                if (acompanhar && destino >= ultimoInicio && passos > 0) {
                    ancora = null
                } else if (proximo != inicio) {
                    ancora = amostras[proximo].tempo
                }
                deslocamento -= passos
            }
        }
        Column {
            PlotMedicoes(trechos, janela, grandeza, unidade, corCurva, zona, drag)
            if (acompanhar && !seguindo) {
                TextButton(onClick = { ancora = null }) { Text("Voltar às últimas leituras") }
            }
        }
    }
}

@Composable
private fun PlotMedicoes(
    trechos: List<List<AmostraMedicao>>,
    amostras: List<AmostraMedicao>,
    grandeza: String,
    unidade: String,
    corCurva: Color,
    zona: ZoneId,
    drag: DraggableState
) {
    val inicio = amostras.first().tempo
    val fim = amostras.last().tempo
    val escala = remember(amostras) { escalaMedicoes(amostras) }
    val segundos = Duration.between(inicio, fim).let { it.seconds + it.nano / 1e9 }
    val formatador = remember(zona, segundos < 3600) {
        val formato = if (segundos < 3600) DateTimeFormatter.ofPattern("HH:mm:ss") else formatoEixoMedicoes
        formato.withZone(zona)
    }
    val medir = rememberTextMeasurer()
    val corEixos = CorEixos
    val corGrade = CorGrade
    val corDestaque = SurfaceDark
    val estiloEixos = TextStyle(color = corEixos, fontSize = 10.sp)
    val density = LocalDensity.current
    val rotulosY = remember(escala, unidade, medir, estiloEixos) {
        escala.marcas.map { valor ->
            medir.measure(numeroMedicao(valor, escala.casas) + if (unidade.isBlank()) "" else " $unidade", estiloEixos)
        }
    }
    val larguraY = with(density) { rotulosY.maxOf { it.size.width }.toDp() } + 10.dp
    val curvas = remember(trechos) {
        trechos.map { trecho ->
            val origem = trecho.first().tempo
            val tempos = trecho.map { Duration.between(origem, it.tempo).let { d -> d.seconds + d.nano / 1e9 } }
            val controles = controlesMonotonicos(tempos, trecho.map { it.valor })
            TrechoPlot(trecho.first(), trecho.drop(1).zip(controles) { amostra, controle -> ParcelaPlot(amostra, controle) })
        }
    }
    val altura = 208.dp
    var selecionada by remember { mutableStateOf<Instant?>(null) }
    val destaque = remember(amostras, selecionada) { amostras.firstOrNull { it.tempo == selecionada } }

    Row(Modifier.fillMaxWidth()) {
        Canvas(Modifier.width(larguraY).height(altura).testTag("medicoes-eixo-y").semantics {
            contentDescription = "Eixo vertical de valores"
        }) {
            val topo = 10.dp.toPx()
            val base = size.height - 26.dp.toPx()
            val eixo = size.width - 0.5.dp.toPx()
            drawLine(corEixos, Offset(eixo, topo), Offset(eixo, base + 0.5.dp.toPx()), 1.dp.toPx())
            rotulosY.forEachIndexed { i, rotulo ->
                val y = base - (base - topo) * i / rotulosY.lastIndex
                drawLine(corEixos, Offset(eixo - 4.dp.toPx(), y), Offset(eixo, y), 1.dp.toPx())
                drawText(rotulo, topLeft = Offset(eixo - 8.dp.toPx() - rotulo.size.width, y - rotulo.size.height / 2f))
            }
        }

        BoxWithConstraints(Modifier.weight(1f).height(altura)) {
            val larguraVisivel = maxWidth
            val larguraPx = with(density) { larguraVisivel.toPx() }
            val recuo = with(density) { 5.dp.toPx() }
            fun x(tempo: Instant): Float {
                if (segundos == 0.0) return larguraPx / 2
                val delta = Duration.between(inicio, tempo).let { it.seconds + it.nano / 1e9 }
                return recuo + (delta / segundos).toFloat() * (larguraPx - 2 * recuo).coerceAtLeast(1f)
            }
            val marcasX = remember(inicio, fim, larguraVisivel, medir, formatador, estiloEixos) {
                val exemplo = medir.measure(formatador.format(inicio), estiloEixos).size.width
                val espacamento = exemplo + with(density) { 14.dp.toPx() }
                val quantidade = (larguraPx / espacamento).toInt().coerceAtLeast(1)
                val alvo = segundos / quantidade
                val passo = passosTempo.firstOrNull { it >= alvo } ?: passosTempo.last()
                val primeira = inicio.epochSecond + ((passo - Math.floorMod(inicio.epochSecond, passo)) % passo)
                val marcas = if (segundos == 0.0) listOf(inicio) else
                    generateSequence(Instant.ofEpochSecond(primeira).let { if (it < inicio) it.plusSeconds(passo) else it }) {
                        it.plusSeconds(passo)
                    }.takeWhile { it <= fim }.toList().ifEmpty { listOf(inicio) }
                var bordaAnterior = -espacamento
                marcas.mapNotNull { tempo ->
                    val texto = medir.measure(formatador.format(tempo), estiloEixos)
                    val pos = (x(tempo) - texto.size.width / 2f).coerceIn(0f, (larguraPx - texto.size.width).coerceAtLeast(0f))
                    if (pos < bordaAnterior + with(density) { 8.dp.toPx() }) null
                    else {
                        bordaAnterior = pos + texto.size.width
                        Triple(tempo, pos, texto)
                    }
                }
            }

            Box(Modifier.fillMaxSize().draggable(drag, Orientation.Horizontal).testTag("medicoes-rolagem")) {
                Canvas(
                    Modifier.fillMaxWidth().height(altura).testTag("medicoes-grafico")
                        .semantics {
                            contentDescription = "Série temporal de medições. Toque no gráfico para consultar a leitura mais próxima."
                        }
                        .pointerInput(amostras, larguraVisivel) {
                            detectTapGestures { toque ->
                                val fracao = ((toque.x - recuo) / (larguraPx - 2 * recuo).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                val alvo = inicio.plusNanos((fracao * segundos * 1e9).toLong())
                                val indice = amostras.binarySearchBy(alvo) { it.tempo }.let { if (it < 0) -it - 1 else it }
                                val proxima = listOfNotNull(amostras.getOrNull(indice - 1), amostras.getOrNull(indice))
                                    .minByOrNull { abs(x(it.tempo) - toque.x) }
                                    ?.takeIf { toque.y <= size.height - 26.dp.toPx() }
                                selecionada = proxima?.tempo?.takeUnless { it == selecionada }
                            }
                        }
                ) {
                    val topo = 10.dp.toPx()
                    val base = size.height - 26.dp.toPx()
                    fun y(valor: Double): Float =
                        base - (base - topo) * ((valor - escala.min) / (escala.max - escala.min)).toFloat()
                    val tracejado = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                    escala.marcas.forEach { valor ->
                        val linha = y(valor)
                        drawLine(corGrade, Offset(0f, linha), Offset(size.width, linha), 1.dp.toPx(), pathEffect = tracejado)
                    }
                    drawLine(corEixos, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
                    marcasX.forEach { (tempo, pos, texto) ->
                        val marca = x(tempo)
                        drawLine(corEixos, Offset(marca, base), Offset(marca, base + 4.dp.toPx()), 1.dp.toPx())
                        drawText(texto, topLeft = Offset(pos, base + 7.dp.toPx()))
                    }
                    curvas.forEach { trecho ->
                        val primeiroX = x(trecho.inicio.tempo)
                        val primeiroY = y(trecho.inicio.valor)
                        if (trecho.seguintes.isEmpty()) {
                            drawLine(corCurva, Offset(primeiroX - 1.dp.toPx(), primeiroY),
                                Offset(primeiroX + 1.dp.toPx(), primeiroY), 2.dp.toPx(), StrokeCap.Round)
                        } else {
                            val path = Path().apply {
                                moveTo(primeiroX, primeiroY)
                                var anteriorX = primeiroX
                                trecho.seguintes.forEach { parcela ->
                                    val fimX = x(parcela.fim.tempo)
                                    val terco = (fimX - anteriorX) / 3
                                    cubicTo(anteriorX + terco, y(parcela.curva.primeiro),
                                        fimX - terco, y(parcela.curva.segundo), fimX, y(parcela.fim.valor))
                                    anteriorX = fimX
                                }
                            }
                            drawPath(path, corCurva, style = Stroke(2.25.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    destaque?.let {
                        val pos = x(it.tempo)
                        drawLine(corEixos, Offset(pos, topo), Offset(pos, base), 1.dp.toPx())
                        val centro = Offset(pos, y(it.valor))
                        drawCircle(corDestaque, 5.dp.toPx(), centro)
                        drawCircle(corCurva, 4.dp.toPx(), centro, style = Stroke(2.dp.toPx()))
                    }
                }
            }

            destaque?.let { amostra ->
                val tooltipWidth = minOf(156.dp, larguraVisivel)
                val pos = with(density) { x(amostra.tempo).toDp() }
                val esquerda = (pos - tooltipWidth / 2).coerceIn(0.dp, larguraVisivel - tooltipWidth)
                TooltipMedicao(amostra, grandeza, unidade, zona, Modifier.offset(x = esquerda).width(tooltipWidth))
            }
        }
    }
}

@Composable
private fun TooltipMedicao(amostra: AmostraMedicao, grandeza: String, unidade: String, zona: ZoneId, modifier: Modifier) {
    val dataHora = remember(zona) {
        DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm:ss", localeMedicoes).withZone(zona)
    }
    val forma = RoundedCornerShape(8.dp)
    Column(
        modifier.testTag("medicoes-tooltip")
            .background(Slate900, forma)
            .border(1.dp, CorGrade, forma)
            .padding(8.dp)
    ) {
        Text(dataHora.format(amostra.tempo), color = CorEixos, fontSize = 11.sp)
        val valor = numeroMedicao(amostra.valor, casas = 12, minimo = 0)
        val valorComUnidade = if (unidade.isBlank()) valor else "$valor $unidade"
        Text("$grandeza: $valorComUnidade", color = CorTitulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
