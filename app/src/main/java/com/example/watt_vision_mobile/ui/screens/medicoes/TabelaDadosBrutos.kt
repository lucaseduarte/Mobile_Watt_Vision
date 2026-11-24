package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BordaTabela = Color(0xFF1B2638)
private val TituloTabela = Color(0xFFF1F5F9)
private val CabecalhoTabela = Color(0xFF405571)
private val ValorTabela = Color(0xFFCBD5E1)
private val TextoSecundario = Color(0xFF94A3B8)
private val DivisoriaTabela = Color(0xFF111B29)
private val CabecalhoAtivo = Brush.verticalGradient(listOf(Color(0xFF172F4D), Color(0xFF101C2E)))
private val TextoCabecalhoAtivo = Color(0xFF438CF5)
private val CelulaAtiva = Color(0xFF0D1726)

private enum class ColunaBruta(val titulo: String, val largura: Dp, val metrica: ChartMetric? = null) {
    DATA("DATA", 106.dp),
    HORA("HORA", 90.dp),
    TENSAO("TENSÃO (V)", 108.dp, ChartMetric.VOLTAGE),
    CORRENTE("CORRENTE (A)", 122.dp, ChartMetric.CURRENT),
    POTENCIA("POTÊNCIA (kW)", 126.dp, ChartMetric.POWER),
    ENERGIA("ENERGIA (kWh)", 126.dp, ChartMetric.ENERGY),
    FREQUENCIA("FREQUÊNCIA (Hz)", 144.dp, ChartMetric.FREQUENCY),
    FATOR_POTENCIA("FATOR DE POTÊNCIA", 156.dp, ChartMetric.POWER_FACTOR)
}

@Composable
internal fun TabelaDadosBrutos(consulta: ConsultaMedicoes, metrica: ChartMetric) {
    val total = consulta.registros.size
    val totalPaginas = totalPaginasRegistros(total)
    var pagina by rememberSaveable { mutableIntStateOf(1) }
    val paginaVisivel = pagina.coerceIn(1, totalPaginas.coerceAtLeast(1))
    LaunchedEffect(paginaVisivel) { pagina = paginaVisivel }
    val inicio = (paginaVisivel - 1) * REGISTROS_POR_PAGINA
    val linhas = consulta.registros.subList(inicio, minOf(inicio + REGISTROS_POR_PAGINA, total))

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("dados-brutos"),
        color = DarkBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BordaTabela)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dados Brutos", Modifier.weight(1f).padding(end = 8.dp), color = TituloTabela,
                    fontSize = 19.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (total == 1) "1 registro" else "$total registros",
                    color = TextoSecundario, fontSize = 12.sp
                )
            }

            GradeDadosBrutos(linhas, metrica, consulta.zona)
            if (total == 0) {
                val mensagem = when {
                    consulta.carregandoRegistros -> "Carregando dados brutos…"
                    else -> consulta.avisoRegistros ?: "Nenhum registro encontrado."
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 64.dp), contentAlignment = Alignment.Center) {
                    Text(mensagem, color = TextoSecundario, fontSize = 13.sp)
                }
            } else {
                consulta.avisoRegistros?.let { Text(it, color = TextoSecundario, fontSize = 12.sp) }
                PaginacaoDadosBrutos(paginaVisivel, totalPaginas) { pagina = it }
            }
            consulta.tentarNovamente?.let { tentar ->
                TextButton(onClick = tentar, modifier = Modifier.align(Alignment.End)) {
                    Text("Tentar novamente")
                }
            }
        }
    }
}

@Composable
private fun GradeDadosBrutos(linhas: List<RegistroMedicao>, metrica: ChartMetric, zona: ZoneId) {
    val densidade = LocalDensity.current
    val escalaFonte = densidade.fontScale.coerceAtLeast(1f)
    val colunas = ColunaBruta.entries
    val larguraTotal = colunas.fold(0.dp) { total, coluna -> total + coluna.largura * escalaFonte }
    val ativa = colunas.single { it.metrica == metrica }
    val rolagem = rememberScrollState()
    val data = remember(zona) { DateTimeFormatter.ofPattern("dd/MM/yyyy", localeMedicoes).withZone(zona) }
    val hora = remember(zona) { DateTimeFormatter.ofPattern("HH:mm:ss", localeMedicoes).withZone(zona) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val larguraVisivel = maxWidth
        LaunchedEffect(metrica, larguraVisivel, escalaFonte) {
            val esquerda = with(densidade) {
                colunas.take(ativa.ordinal).sumOf { (it.largura * escalaFonte).roundToPx() }
            }
            val largura = with(densidade) { (ativa.largura * escalaFonte).roundToPx() }
            val visivel = with(densidade) { larguraVisivel.roundToPx() }
            val destino = when {
                esquerda < rolagem.value || largura >= visivel -> esquerda
                esquerda + largura > rolagem.value + visivel -> esquerda + largura - visivel
                else -> rolagem.value
            }
            if (destino != rolagem.value) rolagem.animateScrollTo(destino)
        }
        Column(
            Modifier.fillMaxWidth().testTag("dados-brutos-rolagem").horizontalScroll(rolagem).width(larguraTotal)
        ) {
            Row {
                colunas.forEach { coluna ->
                    val selecionada = coluna == ativa
                    CelulaBruta(
                        texto = coluna.titulo,
                        largura = coluna.largura * escalaFonte,
                        cor = if (selecionada) TextoCabecalhoAtivo else CabecalhoTabela,
                        peso = if (selecionada) FontWeight.Bold else FontWeight.Medium,
                        cabecalho = true,
                        modifier = Modifier.testTag("dados-brutos-coluna-${coluna.name}")
                            .semantics {
                                selected = selecionada
                                if (selecionada) stateDescription = "Coluna destacada"
                            }
                            .then(if (selecionada) Modifier.background(CabecalhoAtivo) else Modifier)
                    )
                }
            }
            HorizontalDivider(color = DivisoriaTabela)
            linhas.forEach { registro ->
                Row {
                    colunas.forEach { coluna ->
                        val selecionada = coluna == ativa
                        val texto = when (coluna) {
                            ColunaBruta.DATA -> data.format(registro.tempo)
                            ColunaBruta.HORA -> hora.format(registro.tempo)
                            else -> formatarValorBruto(requireNotNull(coluna.metrica), registro.dados)
                        }
                        CelulaBruta(
                            texto = texto,
                            largura = coluna.largura * escalaFonte,
                            cor = when {
                                selecionada -> TituloTabela
                                coluna.metrica == null -> TextoSecundario
                                else -> ValorTabela
                            },
                            peso = if (selecionada) FontWeight.Bold else FontWeight.Normal,
                            modifier = if (selecionada) Modifier.background(CelulaAtiva) else Modifier
                        )
                    }
                }
                HorizontalDivider(color = DivisoriaTabela)
            }
        }
    }
}

@Composable
private fun CelulaBruta(
    texto: String,
    largura: Dp,
    cor: Color,
    peso: FontWeight,
    modifier: Modifier = Modifier,
    cabecalho: Boolean = false
) {
    Box(
        modifier.width(largura).heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(texto, color = cor, fontWeight = peso, fontSize = if (cabecalho) 11.sp else 13.sp,
            maxLines = 1, softWrap = false)
    }
}

@Composable
private fun PaginacaoDadosBrutos(pagina: Int, total: Int, onPage: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Página $pagina de $total", color = TextoSecundario, fontSize = 12.sp)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tamanho = 48.dp
            val espaco = 4.dp
            val larguraDisponivel = maxWidth
            val todasCabem = total <= 5 && larguraDisponivel >= tamanho * total + espaco * (total - 1)
            val quantidade = when {
                todasCabem -> total
                larguraDisponivel >= tamanho * 5 + espaco * 4 -> 3
                else -> 1
            }
            val primeira = (pagina - quantidade / 2).coerceIn(1, total - quantidade + 1)
            Row(horizontalArrangement = Arrangement.spacedBy(espaco), verticalAlignment = Alignment.CenterVertically) {
                if (!todasCabem) {
                    IconButton(onClick = { onPage(pagina - 1) }, enabled = pagina > 1, modifier = Modifier.size(tamanho)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Página anterior")
                    }
                }
                for (numero in primeira until primeira + quantidade) {
                    val ativa = numero == pagina
                    Surface(
                        onClick = { onPage(numero) },
                        modifier = Modifier.size(tamanho).semantics { selected = ativa },
                        shape = RoundedCornerShape(8.dp),
                        color = if (ativa) Color(0xFF1D3B66) else Color(0xFF0B1422),
                        border = BorderStroke(1.dp, if (ativa) Color(0xFF3975C5) else Color(0xFF19283D))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                numero.toString(), color = if (ativa) Color.White else TextoSecundario, fontSize = 12.sp,
                                fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
                if (!todasCabem) {
                    IconButton(onClick = { onPage(pagina + 1) }, enabled = pagina < total, modifier = Modifier.size(tamanho)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Próxima página")
                    }
                }
            }
        }
    }
}
