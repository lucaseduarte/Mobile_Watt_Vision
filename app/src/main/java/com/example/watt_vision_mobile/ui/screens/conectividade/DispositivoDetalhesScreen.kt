package com.example.watt_vision_mobile.ui.screens.conectividade

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.theme.*
import kotlinx.coroutines.launch

private enum class AbaDetalhes(val titulo: String) {
    TELEMETRIA("Telemetria"),
    HARDWARE("Hardware & Rede"),
    HISTORICO("Histórico")
}

private val rotuloTecnico
    @Composable get() = corTema(0xFF8E96A4, 0xFF475569)
private val valorTecnico
    @Composable get() = corTema(0xFFF1F5F9, 0xFF111827)
private val telemetria = listOf("Tensão", "Corrente", "Potência Ativa", "Energia Acumulada", "Frequência", "Fator de Potência")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DispositivoDetalhesScreen(
    dispositivo: DispositivoConectividade?,
    eventos: List<EventoConectividade>,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var aba by rememberSaveable { mutableStateOf(AbaDetalhes.TELEMETRIA) }
    val estadoAbas = rememberSaveableStateHolder()
    BackHandler(onBack = onVoltar)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = DarkBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Detalhes do dispositivo") },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground,
                        titleContentColor = TextWhite,
                        navigationIconContentColor = TextWhite
                    )
                )
                if (dispositivo != null) ResumoDispositivo(dispositivo)
                SecondaryTabRow(
                    selectedTabIndex = aba.ordinal,
                    containerColor = DarkBackground,
                    contentColor = ToneBlueText,
                    divider = { HorizontalDivider(color = CardBorder) }
                ) {
                    AbaDetalhes.entries.forEach { opcao ->
                        Tab(
                            selected = aba == opcao,
                            onClick = { aba = opcao },
                            text = { Text(opcao.titulo) },
                            selectedContentColor = ToneBlueText,
                            unselectedContentColor = TextSubtle
                        )
                    }
                }
            }
        }
    ) { padding ->
        val conteudo = Modifier.fillMaxSize().padding(padding)
        if (dispositivo == null) {
            Box(conteudo, contentAlignment = Alignment.Center) {
                Text("Dispositivo não encontrado.", color = TextSubtle)
            }
            return@Scaffold
        }
        estadoAbas.SaveableStateProvider(aba.name) {
            when (aba) {
                AbaDetalhes.TELEMETRIA -> TelemetriaDetalhes(dispositivo, conteudo)
                AbaDetalhes.HARDWARE -> HardwareDetalhes(dispositivo, conteudo)
                AbaDetalhes.HISTORICO -> HistoricoDetalhes(eventos.filter { it.dispositivoId == dispositivo.id }, conteudo)
            }
        }
    }
}

@Composable
private fun ResumoDispositivo(dispositivo: DispositivoConectividade) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dispositivo.codigo, Modifier.weight(1f), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = valorTecnico)
            val (cor, fundo) = coresConexao(dispositivo.status)
            SeloDetalhes("● ${dispositivo.status}", fundo, cor)
        }
        Text(dispositivo.nome, fontSize = 14.sp, color = TextSubtle)
        Text("Cadastro do Backend", fontSize = 12.sp, color = ToneBlueText)
        Text(listOf(dispositivo.setor, dispositivo.local).filter { it.isNotBlank() }.joinToString(" / ").ifEmpty { "—" },
            fontSize = 12.sp, color = TextSubtle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = rotuloTecnico)
            Text("Último contato: ${dispositivo.ultimoContato}", fontSize = 12.sp, color = rotuloTecnico)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TelemetriaDetalhes(dispositivo: DispositivoConectividade, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardDetalhes {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    TituloSecao("Sensor")
                    SeloDetalhes("Telemetria indisponível", ToneBlueBg, ToneBlueText)
                }
                telemetria.chunked(2).forEach { linha ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        linha.forEach { rotulo ->
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = InputBackground,
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                CampoTecnico(rotulo, "—", Modifier.padding(10.dp), destaque = true)
                            }
                        }
                    }
                }
            }
        }
        item {
            CardDetalhes {
                CampoTecnico(
                    "Vínculo Hierárquico",
                    listOf(dispositivo.local, dispositivo.setor, dispositivo.equipamento).filter { it.isNotBlank() }.joinToString(" › ").ifEmpty { "—" }
                )
            }
        }
    }
}

@Composable
private fun HardwareDetalhes(dispositivo: DispositivoConectividade, modifier: Modifier) {
    val cadastro = dispositivo.cadastro
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardDetalhes {
                TituloSecao("Especificações do Dispositivo")
                LinhaTecnica("Modelo" to dispositivo.tipo, "Nº Série" to (cadastro.numero_serie ?: "—"))
                LinhaTecnica("Firmware" to (cadastro.versao_firmware ?: "—"), "Intervalo" to (cadastro.tx_interval_sec?.let { "$it s" } ?: "—"))
                CampoTecnico("Ponto de Medição", dispositivo.pontosMedicao.joinToString(", ").ifEmpty { "—" })
                CampoCopiavel("MAC Address", cadastro.mac_address ?: "—")
                CampoTecnico("Data de Instalação", cadastro.data_instalacao ?: "—")
            }
        }
        item {
            CardDetalhes {
                TituloSecao("Conexão Wi-Fi")
                CampoTecnico("Wi-Fi", when (cadastro.wifi) {
                    1 -> "Ativo"
                    0 -> "Inativo"
                    else -> "—"
                })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTecnico("Rede (SSID)", cadastro.ssid ?: "—", Modifier.weight(1f))
                    CampoCopiavel("Endereço IP", cadastro.ip ?: "—", Modifier.weight(1.35f))
                }
            }
        }
        item {
            CardDetalhes {
                TituloSecao("Configurações MQTT")
                SeloDetalhes(dispositivo.comunicacao, ToneBlueBg, ToneBlueText)
                CampoCopiavel("Broker", cadastro.broker ?: "—")
                CampoTecnico("Porta", cadastro.port?.toString() ?: "—")
                CampoCopiavel("Tópico", cadastro.topic ?: "—")
                CampoTecnico("Client ID", cadastro.client_id ?: "—")
            }
        }
    }
}

@Composable
private fun HistoricoDetalhes(eventos: List<EventoConectividade>, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { TituloSecao("Histórico de Conexão e Eventos") }
        if (eventos.isEmpty()) item { Text("O histórico de eventos não está disponível no Backend.", color = TextSubtle) }
        items(eventos, key = { it.id }) { evento ->
            val reconectado = evento.evento == "Reconectado"
            CardDetalhes {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cor = if (reconectado) ToneGreenText else ToneYellowText
                    Box(Modifier.size(8.dp).background(cor, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (reconectado) "Conectado" else "Queda de Conexão",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valorTecnico)
                        Text(evento.momento.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontSize = 12.sp, color = rotuloTecnico)
                    }
                    Text("—", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cor)
                }
            }
        }
    }
}

@Composable
private fun CampoCopiavel(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboard.current
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CampoTecnico(rotulo, valor, Modifier.weight(1f))
        IconButton(enabled = valor != "—", onClick = {
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(rotulo, valor)))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(contexto, "$rotulo copiado", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar $rotulo",
                modifier = Modifier.size(18.dp), tint = ToneBlueText)
        }
    }
}

@Composable
private fun LinhaTecnica(esquerda: Pair<String, String>, direita: Pair<String, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampoTecnico(esquerda.first, esquerda.second, Modifier.weight(1f))
        CampoTecnico(direita.first, direita.second, Modifier.weight(1f))
    }
}

@Composable
private fun CampoTecnico(rotulo: String, valor: String, modifier: Modifier = Modifier, destaque: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rotulo, fontSize = 11.sp, color = rotuloTecnico)
        Text(valor, fontSize = if (destaque) 20.sp else 14.sp, color = valorTecnico,
            fontWeight = if (destaque) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun TituloSecao(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleSmall, color = valorTecnico, fontWeight = FontWeight.Bold)
}

@Composable
private fun CardDetalhes(conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = conteudo)
    }
}

@Composable
private fun SeloDetalhes(texto: String, fundo: Color, textoCor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = fundo) {
        Text(texto, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = textoCor)
    }
}
