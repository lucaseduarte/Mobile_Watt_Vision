package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.components.WattVisionHeaderShortcut
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.components.filtros.BotaoFiltros
import com.example.watt_vision_mobile.ui.components.filtros.PainelFiltros
import com.example.watt_vision_mobile.ui.screens.dashboard.components.CardComSombreamentoDashboard
import com.example.watt_vision_mobile.ui.screens.dashboard.components.FiltroItemModel
import com.example.watt_vision_mobile.ui.screens.dashboard.components.FiltroLinhaSeletor
import com.example.watt_vision_mobile.ui.screens.dashboard.components.FiltroSeletorBusca
import com.example.watt_vision_mobile.ui.screens.dashboard.components.SelectorType
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardFilterState
import com.example.watt_vision_mobile.ui.theme.CardBackground
import com.example.watt_vision_mobile.ui.theme.InputBackground
import com.example.watt_vision_mobile.ui.theme.PrimaryBlue
import com.example.watt_vision_mobile.ui.theme.SurfaceDark
import com.example.watt_vision_mobile.ui.theme.TextMuted
import com.example.watt_vision_mobile.ui.theme.TextSubtle
import com.example.watt_vision_mobile.ui.theme.TextWhite

private const val TODOS_LOCAIS = "Todos os locais"
private const val TODOS_SETORES = "Todos os setores"
private const val TODOS_EQUIPAMENTOS = "Todos os equipamentos"
private val periodos = listOf("Hoje", "Ontem", "Últimos 7 dias", "Últimos 30 dias")

private enum class Grandeza(
    val rotulo: String,
    val metrica: ChartMetric,
    val descricao: String,
    val maior: String
) {
    TENSAO("Tensão", ChartMetric.VOLTAGE, "tensão", "Maior tensão registrada"),
    CORRENTE("Corrente", ChartMetric.CURRENT, "corrente", "Maior corrente registrada"),
    POTENCIA("Potência", ChartMetric.POWER, "potência ativa", "Maior potência ativa registrada"),
    ENERGIA("Energia", ChartMetric.ENERGY, "energia acumulada", "Maior energia acumulada registrada"),
    FREQUENCIA("Frequência", ChartMetric.FREQUENCY, "frequência", "Maior frequência registrada"),
    FATOR_POTENCIA("Fator de potência", ChartMetric.POWER_FACTOR, "fator de potência", "Maior fator de potência registrado");

    val unidade get() = if (metrica == ChartMetric.POWER) "kW" else metrica.unit
}

private data class FiltrosMedicao(
    val local: String = TODOS_LOCAIS,
    val setor: String = TODOS_SETORES,
    val equipamento: String = TODOS_EQUIPAMENTOS,
    val periodo: String = periodos.first(),
    val grandeza: Grandeza = Grandeza.TENSAO,
    val localId: Int? = null,
    val setorId: Int? = null,
    val equipamentoId: Int? = null
) {
    fun comLocal(nome: String, id: Int? = null) = copy(
        local = nome, localId = id, setor = TODOS_SETORES, setorId = null,
        equipamento = TODOS_EQUIPAMENTOS, equipamentoId = null
    )

    fun comSetor(nome: String, id: Int? = null) = copy(
        setor = nome, setorId = id, equipamento = TODOS_EQUIPAMENTOS, equipamentoId = null
    )

    fun filtroConsulta() = DashboardFilterState(
        period = periodo,
        location = if (local == TODOS_LOCAIS) "Todos" else local,
        sector = if (setor == TODOS_SETORES) "Todos" else setor,
        equipment = if (equipamento == TODOS_EQUIPAMENTOS) "Todos" else equipamento,
        locationId = localId,
        sectorId = setorId,
        equipmentId = equipamentoId
    )

    fun chipsAtivos(): List<Pair<String, FiltrosMedicao>> = buildList {
        if (periodo != periodos.first()) add("Período: $periodo" to copy(periodo = periodos.first()))
        if (local != TODOS_LOCAIS) add("Local: $local" to comLocal(TODOS_LOCAIS))
        if (setor != TODOS_SETORES) add("Setor: $setor" to comSetor(TODOS_SETORES))
        if (equipamento != TODOS_EQUIPAMENTOS) add("Equip.: $equipamento" to copy(equipamento = TODOS_EQUIPAMENTOS, equipamentoId = null))
        if (grandeza != Grandeza.TENSAO) add("Grandeza: ${grandeza.rotulo}" to copy(grandeza = Grandeza.TENSAO))
    }

    companion object {
        val Saver = listSaver(
            save = { filtros: FiltrosMedicao ->
                listOf(
                    filtros.local, filtros.setor, filtros.equipamento, filtros.periodo, filtros.grandeza.name,
                    filtros.localId?.toString().orEmpty(), filtros.setorId?.toString().orEmpty(),
                    filtros.equipamentoId?.toString().orEmpty()
                )
            },
            restore = {
                FiltrosMedicao(
                    it[0], it[1], it[2], it[3], Grandeza.valueOf(it[4]),
                    it.getOrNull(5)?.toIntOrNull(), it.getOrNull(6)?.toIntOrNull(), it.getOrNull(7)?.toIntOrNull()
                )
            }
        )
    }
}

@Composable
internal fun MedicoesScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    serieReal: SerieRealViewModel? = null
) {
    var filtrosReais by rememberSaveable(stateSaver = FiltrosMedicao.Saver) { mutableStateOf(FiltrosMedicao()) }
    var filtrosDemo by rememberSaveable(stateSaver = FiltrosMedicao.Saver) { mutableStateOf(FiltrosMedicao()) }
    var filtrosAbertos by rememberSaveable { mutableStateOf(false) }
    var pontosAbertos by rememberSaveable { mutableStateOf(false) }

    val fonte: FonteSerieSimulada? = remember { fonteSerieSimulada() }
    val estadoReal = serieReal?.estadoDaFonte()
    val real = estadoReal?.modoReal == true
    var demonstracaoAtiva by rememberSaveable { mutableStateOf(serieReal != null && fonte != null) }
    var revisao by remember { mutableIntStateOf(0) }
    val simulada = !real && fonte != null && (serieReal?.habilitada == true || demonstracaoAtiva)
    val filtros = if (simulada) filtrosDemo else filtrosReais
    val grandeza = filtros.grandeza
    val aplicarFiltros: (FiltrosMedicao) -> Unit = {
        if (simulada) filtrosDemo = it else filtrosReais = it
    }
    val atualizar: () -> Unit = {
        if (real) serieReal.fonte.atualizar() else revisao++
    }
    val consulta = when {
        real -> remember(estadoReal, grandeza, serieReal) {
            consultaMedicoesReal(estadoReal, grandeza.metrica, atualizar)
        }
        simulada ->
            consultarMedicoes(fonte, filtros.filtroConsulta().copy(metric = grandeza.metrica), revisao, atualizar)
        else -> ConsultaMedicoes(aviso = "Consultas de negócio suspensas.")
    }
    val periodoVisivel = if (real) "Últimas ${estadoReal.horas} horas" else filtros.periodo
    val ultima = if (real) consulta.registros.firstOrNull()?.let {
        valorExibidoMedicao(grandeza.metrica, it.dados)
    } else consulta.serie.amostras.lastOrNull()?.valor
    val valores = remember(consulta.serie) { consulta.serie.amostras.map { it.valor } }
    val casas = when (grandeza) {
        Grandeza.POTENCIA, Grandeza.ENERGIA, Grandeza.FATOR_POTENCIA -> 3
        else -> 1
    }
    fun formatarResumo(valor: Double?) = valor?.let { numeroMedicao(it, casas, minimo = 1) } ?: "—"

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        WattVisionScreenHeader(
            title = "Medições",
            subtitle = "Visão geral do sistema"
        ) {
            WattVisionHeaderShortcut(Icons.Filled.Notifications, "Alarmes recentes") { onNavigate("Alarmes") }
            Spacer(Modifier.width(8.dp))
            WattVisionHeaderShortcut(Icons.Filled.Settings, "Configurações do sistema") { onNavigate("Configurações") }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chips = filtros.chipsAtivos()
            if (serieReal?.habilitada == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        filtrosAbertos = false
                        pontosAbertos = false
                        serieReal.fonte.selecionarModoReal(!real)
                    }) { Text(if (real) "Dados simulados" else "Dados reais do Backend") }
                    if (real || simulada) {
                        IconButton(
                            onClick = atualizar,
                            enabled = !real || estadoReal?.resultado != ResultadoSerieReal.Carregando
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription =
                                if (real) "Atualizar medições reais" else "Atualizar medições simuladas")
                        }
                    }
                }
            } else if (fonte != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { demonstracaoAtiva = !demonstracaoAtiva },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (simulada) "Sair da demonstração" else "Demonstração")
                    }
                    if (simulada) {
                        IconButton(onClick = atualizar) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Atualizar medições simuladas")
                        }
                    }
                }
            }
            if (real) {
                ResumoConsultaReal(estadoReal)
                OpcoesMedicao("Janela solicitada", "${estadoReal.horas} h", JANELAS_SERIE_REAL.map { "$it h" }, true) {
                    serieReal.fonte.selecionarJanela(it.substringBefore(' ').toInt())
                }
                OpcoesMedicao("Grandeza", grandeza.rotulo, Grandeza.entries.map { it.rotulo }) { escolha ->
                    aplicarFiltros(filtros.copy(grandeza = Grandeza.entries.first { it.rotulo == escolha }))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BotaoPontosMedicao(Modifier.weight(1f)) { pontosAbertos = true }
                    BotaoFiltros(chips.size) { filtrosAbertos = true }
                }
            }
            if (simulada) {
                Text("Demonstração · Dados simulados", color = TextSubtle, fontSize = 12.sp)
            }
            if (!real && chips.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chips, key = { it.first }) { (rotulo, filtrosAtualizados) ->
                        InputChip(
                            selected = true,
                            onClick = { aplicarFiltros(filtrosAtualizados) },
                            label = { Text(rotulo, fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, "Remover filtro $rotulo", Modifier.size(16.dp))
                            },
                            colors = InputChipDefaults.inputChipColors(
                                containerColor = SurfaceDark,
                                selectedContainerColor = SurfaceDark,
                                labelColor = TextWhite,
                                selectedLabelColor = TextWhite,
                                trailingIconColor = TextMuted
                            )
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CardMedicao(
                    "Média do Período", formatarResumo(valores.takeIf { it.isNotEmpty() }?.average()), grandeza.unidade,
                    "Média de ${grandeza.descricao}", IconeMedicao.MEDIA, Modifier.weight(1f).fillMaxHeight()
                )
                CardMedicao(
                    "Valor Máximo", formatarResumo(valores.maxOrNull()), grandeza.unidade,
                    grandeza.maior, IconeMedicao.MAXIMO, Modifier.weight(1f).fillMaxHeight()
                )
            }
            CardMedicao(
                if (real) "Última leitura" else "Valor Atual", formatarResumo(ultima), grandeza.unidade,
                "Última leitura de ${grandeza.descricao}", IconeMedicao.ATUAL, Modifier.fillMaxWidth()
            )
            key(filtros, simulada) {
                GraficoMedicoes(grandeza.metrica, grandeza.unidade, periodoVisivel, consulta)
            }
            key(filtros.filtroConsulta(), simulada) {
                TabelaDadosBrutos(consulta, grandeza.metrica)
            }
        }
    }

    if (!real && pontosAbertos) {
        PontosMedicaoSheet(onFechar = { pontosAbertos = false })
    }

    if (!real && filtrosAbertos) {
        FiltrosMedicaoSheet(
            filtros = filtros,
            catalogo = consulta.catalogo,
            onApply = {
                aplicarFiltros(it)
                filtrosAbertos = false
            },
            onDismiss = { filtrosAbertos = false }
        )
    }
}

@Composable
private fun SerieRealViewModel.estadoDaFonte(): EstadoSerieReal = fonte.estado.collectAsState().value

@Composable
internal fun ResumoConsultaReal(estado: EstadoSerieReal) {
    Text("Dados reais do Backend · PZEM 1 · Motor Elétrico WEG 01", color = TextSubtle, fontSize = 12.sp)
    val atualizadaEm = when (val resultado = estado.resultado) {
        is ResultadoSerieReal.ComDados -> {
            val serie = resultado.serie
            Text("${serie.quantidade} medições · Intervalo observado", color = TextSubtle, fontSize = 12.sp)
            Text("${serie.primeiroTimestamp} a ${serie.ultimoTimestamp}", color = TextSubtle, fontSize = 12.sp)
            Text("Última medição: ${serie.ultimoTimestamp}", color = TextSubtle, fontSize = 12.sp)
            serie.atualizadaEm
        }
        is ResultadoSerieReal.Vazio -> {
            Text("0 medições", color = TextSubtle, fontSize = 12.sp)
            resultado.atualizadaEm
        }
        else -> null
    }
    if (atualizadaEm != null) {
        val formato = remember {
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", localeMedicoes)
                .withZone(ZONA_SERIE_REAL)
        }
        Text("Consulta atualizada em ${formato.format(atualizadaEm)}", color = TextSubtle, fontSize = 12.sp)
    }
    Text("Horários da série interpretados em America/Sao_Paulo.", color = TextSubtle, fontSize = 12.sp)
}

@Composable
private fun FiltrosMedicaoSheet(
    filtros: FiltrosMedicao,
    catalogo: ChartCatalog,
    onApply: (FiltrosMedicao) -> Unit,
    onDismiss: () -> Unit
) {
    var rascunho by rememberSaveable(filtros, stateSaver = FiltrosMedicao.Saver) { mutableStateOf(filtros) }
    var seletor by rememberSaveable { mutableStateOf<SelectorType?>(null) }

    PainelFiltros(
        onDismiss = onDismiss,
        onClear = { rascunho = FiltrosMedicao() },
        onApply = { onApply(rascunho) }
    ) {
        OpcoesMedicao("Período", rascunho.periodo, periodos, linhaUnica = true) {
            rascunho = rascunho.copy(periodo = it)
        }
        CampoMedicao("Local", "Local selecionado", rascunho.local) { seletor = SelectorType.LOCAL }
        CampoMedicao("Setor", "Setor selecionado", rascunho.setor) { seletor = SelectorType.SETOR }
        CampoMedicao("Equipamento", "Equipamento selecionado", rascunho.equipamento) {
            seletor = SelectorType.EQUIPAMENTO
        }
        OpcoesMedicao("Grandeza", rascunho.grandeza.rotulo, Grandeza.entries.map { it.rotulo }) { rotulo ->
            rascunho = rascunho.copy(grandeza = Grandeza.entries.first { it.rotulo == rotulo })
        }
    }

    seletor?.let { tipo ->
        val (titulo, selecionado, itens) = when (tipo) {
            SelectorType.LOCAL -> Triple("Local", rascunho.localId, catalogo.locais.mapNotNull {
                itemMedicao(it.id, it.nome)
            })
            SelectorType.SETOR -> Triple("Setor", rascunho.setorId, catalogo.sectors(rascunho.localId).mapNotNull { setor ->
                itemMedicao(setor.id, setor.nome, catalogo.locais.firstOrNull { it.id == setor.id_local }?.nome)
            })
            SelectorType.EQUIPAMENTO -> Triple(
                "Equipamento", rascunho.equipamentoId,
                catalogo.equipment(rascunho.localId, rascunho.setorId).mapNotNull { equipamento ->
                    itemMedicao(equipamento.id, equipamento.nome,
                        catalogo.setores.firstOrNull { it.id == equipamento.id_setor }?.nome)
                }
            )
        }
        val todos = when (tipo) {
            SelectorType.LOCAL -> TODOS_LOCAIS
            SelectorType.SETOR -> TODOS_SETORES
            SelectorType.EQUIPAMENTO -> TODOS_EQUIPAMENTOS
        }
        val opcoes = listOf(FiltroItemModel("Todos", todos)) + itens.distinctBy { it.id }
        FiltroSeletorBusca(
            titulo = titulo,
            items = opcoes,
            selectedId = selecionado?.toString() ?: "Todos",
            onItemSelected = {},
            onItemIdSelected = { chave ->
                val opcao = opcoes.firstOrNull { it.id == chave } ?: return@FiltroSeletorBusca
                val id = opcao.id.toIntOrNull()
                rascunho = when (tipo) {
                    SelectorType.LOCAL -> rascunho.comLocal(opcao.title, id)
                    SelectorType.SETOR -> rascunho.comSetor(opcao.title, id)
                    SelectorType.EQUIPAMENTO -> rascunho.copy(equipamento = opcao.title, equipamentoId = id)
                }
                seletor = null
            },
            onDismiss = { seletor = null }
        )
    }
}

private fun itemMedicao(id: Int?, nome: String?, contexto: String? = null): FiltroItemModel? {
    if (id == null || id <= 0 || nome.isNullOrBlank()) return null
    return FiltroItemModel(id.toString(), nome, contexto)
}

@Composable
private fun CampoMedicao(titulo: String, label: String, valor: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
        FiltroLinhaSeletor(label, valor, onClick)
    }
}

@Composable
internal fun OpcoesMedicao(
    titulo: String,
    selecionado: String,
    opcoes: List<String>,
    linhaUnica: Boolean = false,
    onSelect: (String) -> Unit
) {
    val chips: @Composable (List<String>) -> Unit = { opcoesDaLinha ->
        opcoesDaLinha.forEach { opcao ->
            FilterChip(
                selected = opcao == selecionado,
                onClick = { onSelect(opcao) },
                label = { Text(opcao.removePrefix("Últimos "), fontSize = 11.sp, maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = InputBackground,
                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                    labelColor = TextMuted,
                    selectedLabelColor = PrimaryBlue
                )
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
        val linhas = if (linhaUnica) listOf(opcoes) else opcoes.chunked(3)
        Column(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            linhas.forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips(linha) }
            }
        }
    }
}

// Símbolos Lucide e gradientes da página de Medições do Front_WV.
private enum class IconeMedicao(inicio: Long, fim: Long, svgPath: String) {
    MEDIA(0xFF2C57A8, 0xFF17315F, "M13 2 3 14h9l-1 8 10-12h-9l1-8z"),
    MAXIMO(0xFF2A8246, 0xFF102520, "M22 12h-4l-3 9L9 3l-3 9H2"),
    ATUAL(0xFF285C68, 0xFF132A38, "m22 7-8.5 8.5-5-5L2 17 M16 7h6v6");

    val gradiente = Brush.linearGradient(listOf(Color(inicio), Color(fim)))
    val vetor = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(svgPath).toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }.build()
}

@Composable
private fun CardMedicao(
    titulo: String,
    valor: String,
    unidade: String,
    descricao: String,
    icone: IconeMedicao,
    modifier: Modifier = Modifier
) {
    CardComSombreamentoDashboard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        fundo = CardBackground,
        borda = Color(0xFF263448)
    ) {
        Column(Modifier.padding(16.dp)) {
            val formatoIcone = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier.size(36.dp)
                    .background(icone.gradiente, formatoIcone)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), formatoIcone),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone.vetor, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color(0xFF8DA7C7))
            }
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(valor, Modifier.alignByBaseline(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC0C5C8))
                if (valor != "—" && unidade.isNotEmpty()) {
                    Text(unidade, Modifier.alignByBaseline(), fontSize = 11.sp, color = Color(0xFFAFB5BA))
                }
            }
            Text(titulo, fontSize = 12.sp, color = Color(0xFF9CAFC4))
            Text(descricao, Modifier.padding(top = 4.dp), fontSize = 11.sp, color = Color(0xFF8899AE))
        }
    }
}
