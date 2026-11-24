package com.example.watt_vision_mobile.ui.screens.conectividade

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.components.WattVisionHeaderShortcut
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.screens.dashboard.components.CardComSombreamentoDashboard
import com.example.watt_vision_mobile.ui.screens.operacional.TabFilterState
import com.example.watt_vision_mobile.ui.screens.operacional.components.OperacionalFilterBar
import com.example.watt_vision_mobile.ui.theme.*
import java.time.LocalDate

private enum class Aba(val titulo: String) {
    STATUS("Status Atual"),
    HISTORICO("Histórico de Conectividade"),
    CONFIGURACAO("Configuração")
}

private enum class Periodo(val titulo: String, val dias: Long) {
    HOJE("Hoje", 1),
    SEMANA("7 dias", 7),
    MES("30 dias", 30)
}

private data class Indicador(
    val valor: Int,
    val titulo: String,
    val descricao: String,
    val icone: ImageVector,
    val inicio: Color,
    val fim: Color
)

private val modelosIndicadores = listOf(
    Indicador(7, "Total de Dispositivos", "dispositivos cadastrados", Icons.Outlined.Dns,
        Color(0xFF315FAE), Color(0xFF17315F)),
    Indicador(4, "Online", "comunicando normalmente", Icons.Outlined.CheckCircle,
        Color(0xFF267447), Color(0xFF12372D)),
    Indicador(1, "Instáveis", "com falhas intermitentes", Icons.Outlined.WarningAmber,
        Color(0xFFA99A76), Color(0xFF51462F)),
    Indicador(2, "Offline", "sem comunicação", Icons.Outlined.WifiOff,
        Color(0xFF632632), Color(0xFF2A1822))
)

private data class Evento(
    val dispositivo: String,
    val evento: String,
    val data: LocalDate,
    val horario: String,
    val duracao: String
)

private val filtrosConectividadeSaver = listSaver<TabFilterState, String>(
    save = { listOf(it.searchQuery, it.statusFilter, it.locationFilter, it.sectorFilter, it.equipmentFilter) },
    restore = { TabFilterState(it[0], it[1], it[2], it[3], it.getOrElse(4) { "Todos" }) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConectividadeScreen(
    viewModel: ConectividadeViewModel,
    onNavigate: (String) -> Unit,
    onVerDetalhes: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var filtros by rememberSaveable(stateSaver = filtrosConectividadeSaver) { mutableStateOf(TabFilterState()) }
    var aba by rememberSaveable { mutableStateOf(Aba.STATUS) }
    var periodo by rememberSaveable { mutableStateOf(Periodo.HOJE) }
    val hoje = LocalDate.now()
    val estado by viewModel.estado.collectAsState()
    val dispositivos = estado.dispositivos
    val porId = remember(dispositivos) { dispositivos.associateBy { it.id } }
    val eventos = remember(estado.eventos, porId) {
        estado.eventos.mapNotNull { evento ->
            porId[evento.dispositivoId]?.let { dispositivo ->
                Evento(dispositivo.codigo, evento.evento, evento.momento.toLocalDate(), evento.momento.toLocalTime().toString(), "—")
            }
        }
    }
    val valores = listOf(dispositivos.size, dispositivos.count { it.status == "Online" },
        dispositivos.count { it.status == "Instável" }, dispositivos.count { it.status == "Offline" })
    val indicadores = modelosIndicadores.mapIndexed { index, indicador -> indicador.copy(valor = valores[index]) }
    val posicao = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(DarkBackground),
        state = posicao,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("cabecalho") {
            Column {
                WattVisionScreenHeader(
                    title = "Conectividade",
                    subtitle = "Gerencie os dispositivos responsáveis pela coleta e transmissão das medições do Watt Vision",
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    WattVisionHeaderShortcut(Icons.Filled.Notifications, "Alarmes recentes") { onNavigate("Alarmes") }
                    Spacer(Modifier.width(8.dp))
                    WattVisionHeaderShortcut(Icons.Filled.Settings, "Configurações do sistema") { onNavigate("Configurações") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status registrado no Backend", style = MaterialTheme.typography.labelMedium, color = ToneBlueText)
                    IconButton(onClick = viewModel::atualizar, enabled = !estado.carregando) {
                        Icon(Icons.Default.Refresh, "Atualizar conectividade", tint = ToneBlueText)
                    }
                }
                if (estado.carregando) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (estado.erro) {
                    Text("Falha ao consultar a conectividade.", color = TextSubtle)
                    TextButton(onClick = viewModel::atualizar) { Text("Tentar novamente") }
                }
            }
        }
        item("indicadores") {
            BoxWithConstraints {
                val typography = MaterialTheme.typography
                val medir = rememberTextMeasurer()
                val larguraTexto = (maxWidth - 12.dp) / 2 - 28.dp
                val empilhar = with(LocalDensity.current) {
                    indicadores.any { indicador ->
                        indicador.titulo.split(' ').any {
                            medir.measure(it, typography.titleSmall, softWrap = false).size.width > larguraTexto.toPx()
                        } || indicador.descricao.split(' ').any {
                            medir.measure(it, typography.bodySmall, softWrap = false).size.width > larguraTexto.toPx()
                        }
                    }
                }
                Column(
                    modifier = if (empilhar) Modifier else Modifier.height(IntrinsicSize.Min),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (empilhar) {
                        indicadores.forEach { IndicadorCard(it) }
                    } else {
                        indicadores.chunked(2).forEach { linha ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                linha.forEach { indicador ->
                                    IndicadorCard(indicador, Modifier.weight(1f).fillMaxHeight())
                                }
                            }
                        }
                    }
                }
            }
        }
        item("abas") {
            AbasConectividade(aba) { aba = it }
        }
        when (aba) {
            Aba.STATUS -> {
                item("busca-status") {
                    OperacionalFilterBar(
                        tabIndex = 0,
                        filters = filtros,
                        onQueryChanged = { filtros = filtros.copy(searchQuery = it) },
                        onStatusChanged = { filtros = filtros.copy(statusFilter = it) },
                        onApplyFilters = { filtros = it },
                        onRemoveFilter = { filtro ->
                            filtros = when (filtro) {
                                "location" -> filtros.copy(
                                    locationFilter = "Todos", sectorFilter = "Todos", equipmentFilter = "Todos"
                                )
                                "sector" -> filtros.copy(sectorFilter = "Todos", equipmentFilter = "Todos")
                                "equipment" -> filtros.copy(equipmentFilter = "Todos")
                                else -> filtros
                            }
                        },
                        locaisList = dispositivos.map { it.local }.filter { it.isNotBlank() }.distinct(),
                        setoresList = dispositivos.filter { it.setor.isNotBlank() }.map { it.setor to it.local }.distinct(),
                        searchPlaceholder = "Pesquisar dispositivo ou código...",
                        contentPadding = PaddingValues(vertical = 8.dp),
                        statusOptions = listOf("Todos", "Online", "Offline"),
                        equipamentosPorFiltro = { rascunho ->
                            dispositivos.filter { it.correspondeAoLocalESetor(rascunho) }
                                .map { it.equipamento }.filter { it.isNotBlank() }.distinct()
                        }
                    )
                }
                val visiveis = dispositivos.filter { it.corresponde(filtros) }
                items(visiveis, key = { it.id }) { dispositivo ->
                    DispositivoCard(dispositivo) { onVerDetalhes(dispositivo.id) }
                }
                if (visiveis.isEmpty() && !estado.carregando && !estado.erro) item("dispositivo-vazio") {
                    Painel {
                        Text("Nenhum dispositivo encontrado.", color = TextSubtle)
                        if (dispositivos.isNotEmpty()) TextButton(onClick = { filtros = TabFilterState() }) {
                            Text("Limpar busca e filtros", color = ToneBlueText)
                        }
                    }
                }
            }
            Aba.HISTORICO -> {
                item("periodo") {
                    Column {
                        Text("Período", color = TextWhite, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Periodo.entries.forEach { opcao ->
                                val selecionado = periodo == opcao
                                FilterChip(
                                    selected = selecionado,
                                    onClick = { periodo = opcao },
                                    label = { Text(opcao.titulo) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        labelColor = TextSubtle,
                                        selectedContainerColor = Color(0xFF2C57A8),
                                        selectedLabelColor = TextWhite
                                    )
                                )
                            }
                        }
                    }
                }
                item("historico") {
                    val inicio = hoje.minusDays(periodo.dias - 1)
                    HistoricoTabela(eventos.filter { it.data in inicio..hoje }, hoje)
                }
            }
            Aba.CONFIGURACAO -> {
                item("deteccao") { DeteccaoCard() }
                item("alarmes") { AlarmesCard() }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbasConectividade(aba: Aba, selecionar: (Aba) -> Unit) {
    BoxWithConstraints {
        val estilo = MaterialTheme.typography.labelMedium
        val medir = rememberTextMeasurer()
        val larguraTexto = maxWidth / Aba.entries.size - 8.dp
        val precisaRolar = with(LocalDensity.current) {
            Aba.entries.any { opcao ->
                opcao.titulo.split(' ').any {
                    medir.measure(it, estilo, softWrap = false).size.width > larguraTexto.toPx()
                }
            }
        }
        val abas: @Composable () -> Unit = {
            Aba.entries.forEach { opcao ->
                Tab(selected = aba == opcao, onClick = { selecionar(opcao) }) {
                    Text(
                        opcao.titulo,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                        style = estilo,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        val divisor: @Composable () -> Unit = { HorizontalDivider(color = CardBorder) }
        if (precisaRolar) {
            PrimaryScrollableTabRow(
                selectedTabIndex = aba.ordinal,
                containerColor = DarkBackground,
                contentColor = ToneBlueText,
                edgePadding = 0.dp,
                divider = divisor,
                tabs = abas
            )
        } else {
            PrimaryTabRow(
                selectedTabIndex = aba.ordinal,
                containerColor = DarkBackground,
                contentColor = ToneBlueText,
                divider = divisor,
                tabs = abas
            )
        }
    }
}

@Composable
private fun IndicadorCard(indicador: Indicador, modifier: Modifier = Modifier) {
    CardComSombreamentoDashboard(modifier = modifier.fillMaxWidth(), fundo = CardBackground, borda = CardBorder) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconeComFundo(indicador.icone, indicador.inicio, indicador.fim, Color(0xFFD0D8E2))
            Text(indicador.valor.toString(), style = MaterialTheme.typography.headlineSmall,
                color = TextWhite, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(indicador.titulo, style = MaterialTheme.typography.titleSmall, color = TextWhite)
                Text(indicador.descricao, style = MaterialTheme.typography.bodySmall, color = TextSubtle)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DispositivoCard(dispositivo: DispositivoConectividade, onDetalhes: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DispositivoCabecalho(dispositivo)
            HorizontalDivider(color = CardBorder)
            AtributoDispositivo("Local e setor", listOf(dispositivo.local, dispositivo.setor).filter { it.isNotBlank() }.joinToString(" › ").ifEmpty { "—" })
            BoxWithConstraints {
                val medir = rememberTextMeasurer()
                val larguraCampo = (maxWidth - 12.dp) / 2
                val empilhar = with(LocalDensity.current) {
                    medir.measure("Comunicação", MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, letterSpacing = 0.2.sp),
                        softWrap = false).size.width > larguraCampo.toPx()
                }
                if (empilhar) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AtributoDispositivo("Tipo", dispositivo.tipo)
                        AtributoDispositivo("Comunicação", dispositivo.comunicacao)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AtributoDispositivo("Tipo", dispositivo.tipo, Modifier.weight(1f))
                        AtributoDispositivo("Comunicação", dispositivo.comunicacao, Modifier.weight(1f))
                    }
                }
            }
            AtributoDispositivo("Relacionado a", dispositivo.vinculo)
            HorizontalDivider(color = CardBorder)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                AtributoDispositivo(
                    "Último contato", dispositivo.ultimoContato, Modifier.padding(end = 12.dp)
                )
                TextButton(
                    onClick = onDetalhes,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Ver detalhes", color = ToneBlueText)
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                        tint = ToneBlueText, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DispositivoCabecalho(dispositivo: DispositivoConectividade) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Text(
                dispositivo.codigo,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            val (cor, fundo) = coresConexao(dispositivo.status)
            Selo(dispositivo.status, cor, fundo)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(dispositivo.nome, fontSize = 14.sp, color = TextSubtle)
        }
    }
}

@Composable
private fun AtributoDispositivo(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(rotulo, fontSize = 12.sp, letterSpacing = 0.2.sp, color = Color(0xFF8E96A4))
        Text(valor, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextWhite)
    }
}

private val colunasHistorico = listOf(
    "Dispositivo" to 104.dp,
    "Evento" to 128.dp,
    "Data / Hora" to 104.dp,
    "Duração da Indisponibilidade" to 176.dp
)

@Composable
private fun HistoricoTabela(eventos: List<Evento>, hoje: LocalDate) {
    Painel {
        if (eventos.isEmpty()) {
            Text("O histórico de eventos não está disponível no Backend.", color = TextSubtle)
        } else {
            val larguras = largurasHistorico(eventos, hoje)
            val largura = larguras.fold(0.dp) { total, coluna -> total + coluna }
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(largura)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        colunasHistorico.forEachIndexed { index, (rotulo, _) ->
                            Text(
                                text = rotulo,
                                modifier = celulaHistorico(larguras[index]).padding(bottom = 5.dp),
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4A525B)
                            )
                        }
                    }
                    eventos.forEach { evento ->
                        HorizontalDivider(color = CardBorder)
                        LinhaHistorico(evento, hoje, larguras)
                    }
                }
            }
        }
    }
}

@Composable
private fun largurasHistorico(eventos: List<Evento>, hoje: LocalDate): List<Dp> {
    val medir = rememberTextMeasurer()
    val densidade = LocalDensity.current
    val texto = LocalTextStyle.current
    val cabecalho = texto.copy(fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    val valor = texto.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val horario = texto.copy(fontSize = 12.sp)
    val selo = MaterialTheme.typography.labelMedium

    return remember(eventos, hoje, medir, densidade, cabecalho, valor, horario, selo) {
        with(densidade) {
            fun larguraDoTexto(conteudo: String, estilo: TextStyle): Int =
                conteudo.splitToSequence(' ', '\n', '\t')
                    .filter { it.isNotEmpty() }
                    .maxOfOrNull { medir.measure(it, estilo, softWrap = false).size.width } ?: 0

            // Keep the original widths as minimums and leave natural breaks between words.
            val paddingCelula = 12.dp.roundToPx()
            val paddingSelo = 2 * 10.dp.roundToPx()
            val larguras = colunasHistorico.map { (rotulo, minima) ->
                maxOf(minima.roundToPx(), larguraDoTexto(rotulo, cabecalho) + paddingCelula)
            }.toMutableList()

            fun acomodar(coluna: Int, conteudo: String, estilo: TextStyle, padding: Int = 0) {
                larguras[coluna] = maxOf(larguras[coluna], larguraDoTexto(conteudo, estilo) + paddingCelula + padding)
            }

            eventos.forEach { evento ->
                acomodar(0, evento.dispositivo, valor)
                acomodar(1, "•\u00A0${evento.evento}", selo, padding = paddingSelo)
                acomodar(2, if (evento.data == hoje) "Hoje" else evento.data.toString(), valor)
                acomodar(2, evento.horario, horario)
                acomodar(3, evento.duracao, valor)
            }
            larguras.map { it.toDp() }
        }
    }
}

@Composable
private fun LinhaHistorico(evento: Evento, hoje: LocalDate, larguras: List<Dp>) {
    val reconectado = evento.evento.equals("Reconectado", ignoreCase = true) ||
        evento.evento.equals("Conectado", ignoreCase = true)
    Row(
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextoHistorico(evento.dispositivo, celulaHistorico(larguras[0]))
        Box(celulaHistorico(larguras[1])) {
            Selo(
                texto = "• ${evento.evento}",
                cor = if (reconectado) ToneGreenText else ToneYellowText,
                fundo = if (reconectado) ToneGreenBg else ToneYellowBg
            )
        }
        Column(celulaHistorico(larguras[2]), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            TextoHistorico(if (evento.data == hoje) "Hoje" else evento.data.toString())
            Text(evento.horario, fontSize = 12.sp, color = Color(0xFF8E96A4))
        }
        TextoHistorico(evento.duracao, celulaHistorico(larguras[3]))
    }
}

private fun celulaHistorico(largura: Dp) = Modifier.width(largura).padding(end = 12.dp)

@Composable
private fun TextoHistorico(texto: String, modifier: Modifier = Modifier) {
    Text(texto, modifier, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
}

@Composable
private fun DeteccaoCard() {
    Painel {
        CabecalhoConfiguracao(Icons.Outlined.Schedule, "Detecção de Status",
            "Tempos globais usados para classificar a comunicação dos dispositivos")
        ConfiguracaoLinha("Tempo para considerar offline",
            "Após este tempo sem contato, o dispositivo é marcado como offline.", "5 min")
        ConfiguracaoLinha("Tempo para considerar instável",
            "Após este tempo sem comunicação estável.", "2 min")
        ConfiguracaoLinha("Tempo para reconexão estável",
            "Dispositivo precisa ficar online por este tempo para fechar o alarme de comunicação.", "3 min")
    }
}

@Composable
private fun AlarmesCard() {
    Painel {
        CabecalhoConfiguracao(Icons.Outlined.Wifi, "Comportamento dos Alarmes",
            "Defina quais eventos de conectividade devem gerar notificações")
        ConfiguracaoLinha("Gerar alarme quando dispositivo ficar offline",
            "Cria um alerta de comunicação após o tempo de tolerância.", "Ativado", ToneGreenText)
        ConfiguracaoLinha("Gerar alarme quando dispositivo reconectar",
            "Notifica quando a comunicação estável for restabelecida.", "Desativado", TextSubtle)
        ConfiguracaoLinha("Registrar eventos no histórico de conectividade",
            "Mantém quedas e reconexões disponíveis para consulta.", "Ativado", ToneGreenText)
    }
}

@Composable
private fun CabecalhoConfiguracao(icone: ImageVector, titulo: String, descricao: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        IconeComFundo(icone, ToneBlueBg, ToneBlueBg, ToneBlueText)
        CampoConfiguracao(titulo, descricao)
    }
}

@Composable
private fun ConfiguracaoLinha(titulo: String, descricao: String, valor: String, cor: Color = ToneBlueText) {
    HorizontalDivider(color = CardBorder)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CampoConfiguracao(titulo, descricao)
        Selo(valor, cor, InputBackground)
    }
}

@Composable
private fun CampoConfiguracao(titulo: String, descricao: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = TextWhite)
        Text(descricao, style = MaterialTheme.typography.bodySmall, color = TextSubtle)
    }
}

@Composable
private fun Painel(modifier: Modifier = Modifier, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), color = CardBackground,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = conteudo)
    }
}

@Composable
private fun IconeComFundo(icone: ImageVector, inicio: Color, fim: Color, tint: Color) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(listOf(inicio, fim))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icone, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Selo(texto: String, cor: Color, fundo: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = fundo) {
        Text(texto, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = cor)
    }
}

internal fun coresConexao(status: String): Pair<Color, Color> = when (status) {
    "Online" -> ToneGreenText to ToneGreenBg
    "Offline" -> ToneRedText to ToneRedBg
    "Instável" -> ToneYellowText to ToneYellowBg
    else -> TextSubtle to InputBackground
}
