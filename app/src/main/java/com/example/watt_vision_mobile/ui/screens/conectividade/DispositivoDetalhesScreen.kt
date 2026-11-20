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

private val rotuloTecnico = Color(0xFF8E96A4)
private val valorTecnico = Color(0xFFF1F5F9)
private const val SensorPzem = "PZEM-004T"

private val telemetria = listOf(
    "Tensão" to "220,4 V",
    "Corrente" to "8,7 A",
    "Potência Ativa" to "1.820 W",
    "Energia Acumulada" to "84,2 kWh",
    "Frequência" to "59,9 Hz",
    "Fator de Potência" to "0,94 FP"
)

private data class EventoConexao(val conectado: Boolean, val horario: String, val duracao: String)

private val eventosConexao = listOf(
    EventoConexao(true, "Hoje, 14:32:08", "Disponível"),
    EventoConexao(false, "Hoje, 14:23:51", "2min 14s"),
    EventoConexao(true, "Hoje, 10:05:22", "4h 16min"),
    EventoConexao(false, "Ontem, 18:42:10", "38s")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivoDetalhesScreen(onVoltar: () -> Unit, modifier: Modifier = Modifier) {
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
                ResumoDispositivo()
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
        estadoAbas.SaveableStateProvider(aba.name) {
            when (aba) {
                AbaDetalhes.TELEMETRIA -> TelemetriaDetalhes(conteudo)
                AbaDetalhes.HARDWARE -> HardwareDetalhes(conteudo)
                AbaDetalhes.HISTORICO -> HistoricoDetalhes(conteudo)
            }
        }
    }
}

@Composable
private fun ResumoDispositivo() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(DispositivoExemplo.codigo, Modifier.weight(1f), fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = valorTecnico)
            SeloDetalhes("● ${DispositivoExemplo.status}", ToneGreenBg, ToneGreenText)
        }
        Text(DispositivoExemplo.nome, fontSize = 14.sp, color = TextSubtle)
        Text("${DispositivoExemplo.setor} / ${DispositivoExemplo.local}", fontSize = 12.sp, color = TextSubtle)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = rotuloTecnico)
            Text("Último contato: há 3s", fontSize = 12.sp, color = rotuloTecnico)
        }
        Text("Dados de exemplo", style = MaterialTheme.typography.labelMedium, color = ToneBlueText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TelemetriaDetalhes(modifier: Modifier) {
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
                    TituloSecao("Sensor $SensorPzem")
                    SeloDetalhes("● Ao Vivo", ToneGreenBg, ToneGreenText)
                }
                telemetria.chunked(2).forEach { linha ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        linha.forEach { (rotulo, valor) ->
                            Surface(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = InputBackground,
                                border = BorderStroke(1.dp, CardBorder)
                            ) {
                                CampoTecnico(rotulo, valor, Modifier.padding(10.dp), destaque = true)
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
                    "${DispositivoExemplo.local} › ${DispositivoExemplo.setor} › ${DispositivoExemplo.equipamento}"
                )
            }
        }
    }
}

@Composable
private fun HardwareDetalhes(modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardDetalhes {
                TituloSecao("Especificações do Dispositivo")
                LinhaTecnica("Modelo" to DispositivoExemplo.tipo, "Nº Série" to "WV-ESP-001")
                LinhaTecnica("Firmware" to "v1.3.0", "Intervalo" to "5 s")
                CampoTecnico("Ponto de Medição", "$SensorPzem · PZEM-A1B2C3")
                CampoCopiavel("MAC Address", "24:6F:28:A1:0B:01")
                CampoTecnico("Data de Instalação", "11/03/2026")
            }
        }
        item {
            CardDetalhes {
                TituloSecao("Conexão Wi-Fi")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoTecnico("Rede (SSID)", "WattVision-IoT", Modifier.weight(1f))
                    CampoCopiavel("Endereço IP", "192.168.1.42", Modifier.weight(1.35f))
                }
            }
        }
        item {
            CardDetalhes {
                TituloSecao("Configurações MQTT")
                SeloDetalhes("Configuração atual de teste", ToneBlueBg, ToneBlueText)
                CampoCopiavel("Broker", "broker.hivemq.com")
                CampoTecnico("Porta", "1883")
                CampoCopiavel("Tópico", "wattvision/medicoes")
                CampoTecnico("Client ID", "wattvision-esp32-")
            }
        }
    }
}

@Composable
private fun HistoricoDetalhes(modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { TituloSecao("Histórico de Conexão e Eventos") }
        items(eventosConexao, key = { it.horario }) { evento ->
            CardDetalhes {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cor = if (evento.conectado) ToneGreenText else ToneYellowText
                    Box(Modifier.size(8.dp).background(cor, CircleShape))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (evento.conectado) "Conectado" else "Queda de Conexão",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = valorTecnico)
                        Text(evento.horario, fontSize = 12.sp, color = rotuloTecnico)
                    }
                    Text(evento.duracao, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cor)
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
        IconButton(onClick = {
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
