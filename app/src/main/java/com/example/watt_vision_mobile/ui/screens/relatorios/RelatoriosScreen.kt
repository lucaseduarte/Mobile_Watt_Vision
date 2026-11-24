package com.example.watt_vision_mobile.ui.screens.relatorios

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.theme.CardBorder
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import com.example.watt_vision_mobile.ui.theme.TextSubtle
import com.example.watt_vision_mobile.ui.theme.ToneBlueBg
import com.example.watt_vision_mobile.ui.theme.ToneBlueText
import com.example.watt_vision_mobile.ui.theme.ToneGreenText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

internal val CardRelatorioCor = Color(0xFF111827)
private val horaRelatorio = DateTimeFormatter.ofPattern("HH:mm", ptRelatorio)
internal val dataRelatorio = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", ptRelatorio)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RelatoriosScreen(viewModel: RelatoriosViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var filtrosAbertos by rememberSaveable { mutableStateOf(false) }
    var mensagem by rememberSaveable { mutableStateOf<String?>(null) }
    var pdfPendente by remember { mutableStateOf<RelatorioDocumento?>(null) }
    var gerandoPdf by remember { mutableStateOf(false) }
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val documento = pdfPendente
        pdfPendente = null
        if (uri != null && documento != null) {
            escopo.launch {
                gerandoPdf = true
                try {
                    val pdf = gerarRelatorioPdf(contexto, documento)
                    withContext(Dispatchers.IO) {
                        requireNotNull(contexto.contentResolver.openOutputStream(uri)).use { it.write(pdf.bytes) }
                    }
                    mensagem = "PDF salvo."
                } catch (cancelada: kotlinx.coroutines.CancellationException) {
                    if (cancelada !is kotlinx.coroutines.TimeoutCancellationException) throw cancelada
                    mensagem = "A geração do PDF excedeu o tempo limite."
                } catch (falha: Exception) {
                    Log.w("RelatoriosPdf", "Falha ao exportar relatório", falha)
                    mensagem = "Não foi possível salvar o PDF."
                } finally {
                    gerandoPdf = false
                }
            }
        }
    }
    val relatorio = estado.relatorio
    val documento = relatorio?.takeIf { it.podeExportar }?.let { RelatorioDocumento(it, estado) }
    val nomePdf = "relatorio-${estado.filtros.periodo.titulo.lowercase(ptRelatorio).replace(' ', '-')}.pdf"
    val posicao = rememberLazyListState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WattVisionScreenHeader(
                title = "Relatórios",
                subtitle = "Visão geral do sistema",
                actions = {
                    IconButton(onClick = viewModel::atualizar, enabled = !estado.carregando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Atualizar dados")
                    }
                    IconButton(enabled = documento != null && !gerandoPdf, onClick = {
                        pdfPendente = documento
                        exportar.launch(nomePdf)
                    }) { Icon(Icons.Filled.PictureAsPdf, contentDescription = "Exportar PDF") }
                    IconButton(enabled = documento != null && !gerandoPdf, onClick = {
                        if (documento != null) {
                            escopo.launch {
                                gerandoPdf = true
                                try {
                                    val pdf = gerarRelatorioPdf(contexto, documento)
                                    if (!imprimirRelatorio(contexto, pdf, nomePdf)) mensagem = "Impressão indisponível."
                                } catch (cancelada: kotlinx.coroutines.CancellationException) {
                                    if (cancelada !is kotlinx.coroutines.TimeoutCancellationException) throw cancelada
                                    mensagem = "A geração do PDF excedeu o tempo limite."
                                } catch (falha: Exception) {
                                    Log.w("RelatoriosPdf", "Falha ao preparar impressão", falha)
                                    mensagem = "Não foi possível preparar a impressão."
                                } finally {
                                    gerandoPdf = false
                                }
                            }
                        }
                    }) { Icon(Icons.Filled.Print, contentDescription = "Imprimir relatório") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = posicao, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (gerandoPdf) {
                item("gerando-pdf") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            item("status") {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    val hora = estado.atualizadoEm?.let { instante -> relatorio?.zona?.let { horaRelatorio.format(instante.atZone(it)) } }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (hora != null) {
                                Box(Modifier.size(6.dp).background(ToneGreenText, CircleShape))
                            }
                            Text(
                                hora?.let { "Dados atualizados às $it" } ?: "Sem atualização de dados",
                                color = TextSubtle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = ToneBlueBg) {
                            Text(
                                "Tarifa indisponível para o relatório",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = ToneBlueText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    BotaoFiltrosRelatorio(estado.filtros.ativos) { filtrosAbertos = true }
                }
            }
            if (relatorio != null) item("cobertura") {
                CardRelatorio {
                    Text(relatorio.cobertura.titulo, fontWeight = FontWeight.Bold)
                    if (relatorio.limiteAtingido) Text("Limite de 10.000 registros atingido. Possível truncamento.", color = TextSubtle)
                    if (relatorio.leituras > 0) {
                        Text("Consumo e custo consideram apenas os intervalos válidos observados.", color = TextSubtle)
                        if (!relatorio.podeExportar) {
                            Text("PDF e impressão indisponíveis para leituras inconsistentes ou sem base de consumo.", color = TextSubtle)
                        }
                    }
                }
            }
            item("resumo") { ResumoRelatorio(estado) }
            item("abas") {
                SecondaryTabRow(
                    selectedTabIndex = aba,
                    containerColor = DarkBackground,
                    contentColor = ToneBlueText,
                    divider = { HorizontalDivider(color = CardBorder, thickness = 1.dp) }
                ) {
                    listOf("Gráficos", "Medições por dia").forEachIndexed { indice, titulo ->
                        Tab(selected = aba == indice, onClick = { aba = indice }, text = { Text(titulo) })
                    }
                }
            }
            when {
                estado.carregando -> item("carregando") {
                    CardRelatorio {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Montando relatório…")
                    }
                }
                estado.erro != null -> item("erro") {
                    CardRelatorio {
                        Text(estado.erro.orEmpty())
                        TextButton(onClick = viewModel::atualizar) { Text("Tentar novamente") }
                    }
                }
                relatorio == null || relatorio.leituras == 0 -> item("vazio") {
                    CardRelatorio {
                        Text("Nenhuma leitura para os filtros selecionados.")
                    }
                }
                aba == 0 -> {
                    val grandezas = estado.filtros.grandeza?.let(::listOf) ?: GrandezaRelatorio.entries
                    item("titulo-graficos") {
                        Column {
                            Text("Grandezas ao longo do tempo", style = MaterialTheme.typography.titleMedium)
                            val pontos = estado.filtros.grandeza?.let { relatorio.series.getValue(it).pontos.size }
                                ?: relatorio.pontosExibidos
                            Text("$pontos pontos exibidos", color = TextSubtle)
                            if (GrandezaRelatorio.ENERGIA in grandezas) {
                                Text("A curva de energia mostra o contador acumulado em kWh.", color = TextSubtle)
                            }
                        }
                    }
                    items(grandezas, key = { it.name }) { grandeza ->
                        GraficoRelatorio(grandeza, relatorio.series.getValue(grandeza), relatorio.zona)
                    }
                }
                else -> {
                    item("titulo-diario") {
                        Text("${relatorio.leituras} leituras encontradas para o período selecionado", color = TextSubtle)
                    }
                    items(relatorio.dias, key = { it.dia.toEpochDay() }) { dia -> CardDiaRelatorio(dia) }
                }
            }
        }
    }
    if (filtrosAbertos) {
        FiltrosRelatorioSheet(estado, { filtrosAbertos = false }) { filtros ->
            viewModel.aplicar(filtros)
            filtrosAbertos = false
        }
    }
    mensagem?.let { aviso ->
        AlertDialog(
            onDismissRequest = { mensagem = null },
            text = { Text(aviso) },
            confirmButton = { TextButton(onClick = { mensagem = null }) { Text("OK") } }
        )
    }
}

@Composable
internal fun CardRelatorio(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(), color = CardRelatorioCor,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ResumoRelatorio(estado: RelatoriosUiState) {
    val r = estado.relatorio
    fun energia(valor: Double?) = valor?.let { "${numeroRelatorio(it, 3)} kWh" } ?: "—"
    val indicadores = listOf(
        Triple("Consumo observado", energia(r?.consumo), Icons.Outlined.Bolt),
        Triple("Média diária observada", energia(r?.media), Icons.AutoMirrored.Outlined.TrendingUp),
        Triple("Maior consumo diário observado", energia(r?.pico), Icons.Outlined.BarChart),
        Triple("Custo estimado", r?.custo?.let(::moedaRelatorio) ?: "—", Icons.Outlined.AttachMoney)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        indicadores.chunked(2).forEach { linha ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { (titulo, valor, icone) ->
                    CardRelatorio(Modifier.weight(1f).fillMaxHeight()) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = ToneBlueBg.copy(alpha = 0.65f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icone, contentDescription = null, tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(titulo, style = MaterialTheme.typography.labelMedium, color = TextSubtle)
                        Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(estado.filtros.periodo.titulo, style = MaterialTheme.typography.bodySmall, color = TextSubtle)
                        if (titulo == "Custo estimado") Text("Estimativa", color = ToneBlueText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

    }
}

@Composable
private fun CardDiaRelatorio(dia: DiaRelatorio) {
    CardRelatorio {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(dataRelatorio.format(dia.dia), fontWeight = FontWeight.Bold)
                Text(if (dia.leituras == 1) "1 leitura" else "${dia.leituras} leituras", color = TextSubtle, fontSize = 12.sp)
            }
            Text(dia.custo?.let(::moedaRelatorio) ?: "—", fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = CardBorder)
        GrandezaRelatorio.entries.chunked(3).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                linha.forEach { grandeza ->
                    Column(Modifier.weight(1f)) {
                        Text(grandeza.curto, fontSize = 11.sp, color = TextSubtle)
                        val valor = dia.valores[grandeza]?.let {
                            "${numeroRelatorio(it, grandeza.casas)} ${grandeza.unidade}".trim()
                        }
                        Text(valor ?: "—", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = grandeza.cor)
                    }
                }
            }
        }
    }
}
