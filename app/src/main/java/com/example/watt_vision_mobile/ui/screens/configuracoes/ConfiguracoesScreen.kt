package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoGrupoCard
import com.example.watt_vision_mobile.ui.theme.*
import com.example.watt_vision_mobile.util.DateFormatter
import java.util.Locale

@Composable
fun ConfiguracoesScreen(
    viewModel: ConfiguracoesViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar configurações",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Configurações do Sistema",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Informações gerais, tarifas, usuários e pontos de medição",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Barra Inferior de Ações (Cancelar e Salvar Alterações)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = InputBackground,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelarAlteracoes(); onBackClick() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Cancelar", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.salvarAlteracoes(); onBackClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Alterações", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tabs = listOf(
                Triple(0, "Geral", Icons.Default.Settings),
                Triple(1, "Tarifas", Icons.Default.Bolt),
                Triple(2, "Usuários", Icons.Default.People),
                Triple(3, "Pontos de Medição", Icons.Default.Place)
            )

            PrimaryScrollableTabRow(
                selectedTabIndex = uiState.activeTab,
                containerColor = DarkBackground,
                contentColor = PrimaryBlue,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = CardBorder, thickness = 1.dp) }
            ) {
                tabs.forEach { (index, title, icon) ->
                    val isSelected = uiState.activeTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setActiveTab(index) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryBlue else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PrimaryBlue else TextMuted
                                )
                            }
                        },
                        modifier = Modifier.heightIn(min = 44.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.activeTab) {
                    0 -> AbaGeral(uiState = uiState, viewModel = viewModel)
                    1 -> AbaTarifas(uiState = uiState, viewModel = viewModel)
                    2 -> AbaUsuarios(uiState = uiState, viewModel = viewModel)
                    else -> AbaPontosMedicao(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 1. ABA GERAL (Com Informações do Sistema + Aparência)
// ==========================================
@Composable
fun AbaGeral(
    uiState: ConfiguracoesUiState,
    viewModel: ConfiguracoesViewModel
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Card Informações do Sistema
        item {
            InfoGrupoCard(titulo = "Informações do Sistema") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Nome da empresa", fontSize = 12.sp, color = TextMuted)
                    OutlinedTextField(
                        value = uiState.geral.nomeEmpresa,
                        onValueChange = { viewModel.updateNomeEmpresa(it) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Fuso horário", fontSize = 12.sp, color = TextMuted)
                    OutlinedTextField(
                        value = uiState.geral.fusoHorario,
                        onValueChange = { viewModel.updateFusoHorario(it) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputBackground,
                            unfocusedContainerColor = InputBackground,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Formato de data exibido", fontSize = 12.sp, color = TextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY/MM/DD").forEach { formato ->
                            val isSelected = uiState.geral.formatoData == formato
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else InputBackground,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateFormatoData(formato) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = formato,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextWhite else TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Unidade de energia", fontSize = 12.sp, color = TextMuted)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = InputBackground,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "kWh (FIXO)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ToneCyanText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Card Aparência (Preferências visuais Claro / Escuro)
        item {
            InfoGrupoCard(titulo = "Aparência (Preferências visuais)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Tema da Interface", fontSize = 12.sp, color = TextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val temas = listOf(
                            Triple("Claro", Icons.Default.LightMode, "Modo Claro"),
                            Triple("Escuro", Icons.Default.DarkMode, "Modo Escuro (Padrão)")
                        )
                        temas.forEach { (temaKey, icon, label) ->
                            val isSelected = uiState.geral.tema == temaKey
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else InputBackground,
                                border = BorderStroke(1.5.dp, if (isSelected) PrimaryBlue else CardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateTema(temaKey) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) PrimaryBlue else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) TextWhite else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ABA TARIFAS (Com Exibição de Data Final)
// ==========================================
@Composable
fun AbaTarifas(
    uiState: ConfiguracoesUiState,
    viewModel: ConfiguracoesViewModel
) {
    val vigente = remember(uiState.tarifas) {
        uiState.tarifas.firstOrNull { it.ativa }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Card Tarifa Ativa
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Tarifa Ativa", fontSize = 12.sp, color = TextMuted)
                        if (vigente != null) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ToneGreenText.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, ToneGreenText)
                            ) {
                                Text(
                                    text = "Vigente",
                                    fontSize = 10.sp,
                                    color = ToneGreenText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (vigente != null) {
                        val inicioFmt = DateFormatter.formatIsoDate(vigente.inicio, uiState.geral.formatoData)
                        val fimFmt = if (vigente.fim.isNotBlank()) DateFormatter.formatIsoDate(vigente.fim, uiState.geral.formatoData) else "Atual (Sem data final)"

                        Text(
                            text = String.format(Locale.getDefault(), "R$ %.2f / kWh", vigente.valor),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ToneCyanText
                        )
                        Text(
                            text = vigente.descricao,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vigência: $inicioFmt  ➔  $fimFmt",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        Text(
                            text = "Nenhuma tarifa ativa cadastrada.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Histórico de Tarifas
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Histórico de Tarifas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "Valores cadastrados por período de vigência",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = { viewModel.abrirFormTarifa() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Nova Tarifa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Formulário de Tarifa
        if (uiState.mostrarFormTarifa) {
            item {
                FormTarifaCard(viewModel = viewModel, uiState = uiState)
            }
        }

        if (uiState.tarifas.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Nenhuma tarifa cadastrada.", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            items(uiState.tarifas, key = { it.id }) { tarifa ->
                val inicioFmt = DateFormatter.formatIsoDate(tarifa.inicio, uiState.geral.formatoData)
                val fimFmt = if (tarifa.fim.isNotBlank()) DateFormatter.formatIsoDate(tarifa.fim, uiState.geral.formatoData) else "atual"

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tarifa.descricao, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "R$ %.2f / kWh", tarifa.valor),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ToneCyanText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vigência: $inicioFmt a $fimFmt",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { viewModel.abrirFormTarifa(tarifa) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.excluirTarifa(tarifa.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = ToneRedText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormTarifaCard(viewModel: ConfiguracoesViewModel, uiState: ConfiguracoesUiState) {
    val editing = uiState.tarifas.firstOrNull { it.id == uiState.tarifaEditandoId }
    var desc by remember(uiState.tarifaEditandoId) { mutableStateOf(editing?.descricao ?: "") }
    var valorStr by remember(uiState.tarifaEditandoId) { mutableStateOf(editing?.valor?.toString() ?: "0.80") }
    var inicio by remember(uiState.tarifaEditandoId) { mutableStateOf(editing?.inicio ?: "") }
    var fim by remember(uiState.tarifaEditandoId) { mutableStateOf(editing?.fim ?: "") }
    var ativa by remember(uiState.tarifaEditandoId) { mutableStateOf(editing?.ativa ?: false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = if (editing != null) "Editar Tarifa" else "Cadastrar Tarifa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Descrição da Tarifa") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = valorStr,
                onValueChange = { valorStr = it },
                label = { Text("Valor (R$/kWh)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            // Datas divididas em 2 colunas para melhor usabilidade no celular
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inicio,
                    onValueChange = { inicio = it },
                    label = { Text("Data Inicial") },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fim,
                    onValueChange = { fim = it },
                    label = { Text("Data Final (opcional)") },
                    placeholder = { Text("YYYY-MM-DD", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Definir como Tarifa Ativa:", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Medium)
                Switch(
                    checked = ativa,
                    onCheckedChange = { ativa = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextWhite, checkedTrackColor = PrimaryBlue)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.fecharFormTarifa() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextMuted)
                }

                Button(
                    onClick = {
                        val v = valorStr.toDoubleOrNull() ?: 0.80
                        viewModel.salvarTarifa(desc.ifBlank { "Nova Tarifa" }, v, inicio, fim, ativa)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar Tarifa", color = TextWhite)
                }
            }
        }
    }
}

// ==========================================
// 3. ABA USUÁRIOS (Visual do Header, Badges Admin/Gestor e Controles de Seleção)
// ==========================================
@Composable
fun AbaUsuarios(
    uiState: ConfiguracoesUiState,
    viewModel: ConfiguracoesViewModel
) {
    val admins = remember(uiState.usuarios) { uiState.usuarios.count { it.perfil == "Administrador" } }
    val gestores = remember(uiState.usuarios) { uiState.usuarios.count { it.perfil == "Gestor" } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Div/Header de Resumo de Usuários corrigida (sem truncar ou quebrar no mobile)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Usuários do Sistema",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "${uiState.usuarios.size} cadastrados ( $admins Admin · $gestores Gestores )",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Button(
                            onClick = { viewModel.abrirFormUsuario() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Novo Usuário", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.mostrarFormUsuario) {
            item {
                FormUsuarioCard(viewModel = viewModel, uiState = uiState)
            }
        }

        items(uiState.usuarios, key = { it.id }) { user ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = user.nome, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            // Badges com Cores Distintas conforme Frontend (Administrador = Roxo/Indigo, Gestor = Ciano)
                            val isAdmin = user.perfil == "Administrador"
                            val perfilBg = if (isAdmin) Color(0xFF1E1B4B) else ToneCyanBg
                            val perfilText = if (isAdmin) Color(0xFF818CF8) else ToneCyanText
                            val perfilBorder = if (isAdmin) Color(0xFF6366F1) else ToneCyanText
                            val perfilIcon = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = perfilBg,
                                border = BorderStroke(1.dp, perfilBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(imageVector = perfilIcon, contentDescription = null, tint = perfilText, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(user.perfil, color = perfilText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text(text = user.email, fontSize = 11.sp, color = TextMuted)
                        Text(text = "Local: ${user.local}", fontSize = 11.sp, color = TextSubtle)

                        // Destaque para o Último Acesso com Ícone de Relógio
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Último acesso: ${user.ultimoAcesso}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge Status
                        val isAtivo = user.status == "Ativo"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAtivo) ToneGreenBg else ToneRedBg,
                            border = BorderStroke(1.dp, if (isAtivo) ToneGreenText else ToneRedText)
                        ) {
                            Text(
                                text = user.status,
                                fontSize = 10.sp,
                                color = if (isAtivo) ToneGreenText else ToneRedText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.abrirFormUsuario(user) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.excluirUsuario(user.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = ToneRedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormUsuarioCard(viewModel: ConfiguracoesViewModel, uiState: ConfiguracoesUiState) {
    val editing = uiState.usuarios.firstOrNull { it.id == uiState.usuarioEditandoId }
    var nome by remember(uiState.usuarioEditandoId) { mutableStateOf(editing?.nome ?: "") }
    var email by remember(uiState.usuarioEditandoId) { mutableStateOf(editing?.email ?: "") }
    var senha by remember(uiState.usuarioEditandoId) { mutableStateOf("") }
    var perfil by remember(uiState.usuarioEditandoId) { mutableStateOf(editing?.perfil ?: "Administrador") }
    var status by remember(uiState.usuarioEditandoId) { mutableStateOf(editing?.status ?: "Ativo") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = if (editing != null) "Editar Usuário" else "Cadastrar Usuário", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome Completo") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha (mínimo de 6 caracteres)") },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            // SELETOR DE PERFIL INTUITIVO (Segmented Control alinhado com o frontend)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Perfil de Acesso", fontSize = 12.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Botão Administrador
                    val isAdmin = perfil == "Administrador"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAdmin) Color(0xFF1E1B4B) else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (isAdmin) Color(0xFF6366F1) else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { perfil = "Administrador" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, tint = if (isAdmin) Color(0xFF818CF8) else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Administrador", fontSize = 11.sp, fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Medium, color = if (isAdmin) Color(0xFF818CF8) else TextMuted)
                        }
                    }

                    // Botão Gestor
                    val isGestor = perfil == "Gestor"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGestor) ToneCyanBg else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (isGestor) ToneCyanText else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { perfil = "Gestor" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = if (isGestor) ToneCyanText else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gestor", fontSize = 11.sp, fontWeight = if (isGestor) FontWeight.Bold else FontWeight.Medium, color = if (isGestor) ToneCyanText else TextMuted)
                        }
                    }
                }
            }

            // SELETOR DE STATUS INTUITIVO (Segmented Control)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Status da Conta", fontSize = 12.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Botão Ativo
                    val isAtivo = status == "Ativo"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAtivo) ToneGreenBg else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (isAtivo) ToneGreenText else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = "Ativo" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = if (isAtivo) ToneGreenText else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ativo", fontSize = 11.sp, fontWeight = if (isAtivo) FontWeight.Bold else FontWeight.Medium, color = if (isAtivo) ToneGreenText else TextMuted)
                        }
                    }

                    // Botão Inativo
                    val isInativo = status == "Inativo"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isInativo) ToneRedBg else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (isInativo) ToneRedText else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = "Inativo" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = if (isInativo) ToneRedText else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inativo", fontSize = 11.sp, fontWeight = if (isInativo) FontWeight.Bold else FontWeight.Medium, color = if (isInativo) ToneRedText else TextMuted)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.fecharFormUsuario() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextMuted)
                }

                Button(
                    onClick = {
                        viewModel.salvarUsuario(nome.ifBlank { "Novo Usuário" }, email.ifBlank { "user@wattvision.com" }, senha, perfil, status)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar Usuário", color = TextWhite)
                }
            }
        }
    }
}

// ==========================================
// 4. ABA PONTOS DE MEDIÇÃO (Com Seletor de Escopo, Modelo e Status Melhorado)
// ==========================================
@Composable
fun AbaPontosMedicao(
    uiState: ConfiguracoesUiState,
    viewModel: ConfiguracoesViewModel
) {
    val totalAtivos = remember(uiState.pontosMedicao) { uiState.pontosMedicao.count { it.ativo } }
    val totalInativos = remember(uiState.pontosMedicao) { uiState.pontosMedicao.size - totalAtivos }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Pontos de Medição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${uiState.pontosMedicao.size} pontos ($totalAtivos ativos · $totalInativos inativos)", fontSize = 11.sp, color = TextMuted)
                    }

                    Button(
                        onClick = { viewModel.abrirFormPonto() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Novo Ponto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.mostrarFormPonto) {
            item {
                FormPontoCard(viewModel = viewModel, uiState = uiState)
            }
        }

        items(uiState.pontosMedicao, key = { it.id }) { ponto ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ponto.nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${ponto.dispositivo}  ·  Modelo: ${ponto.modelo}", fontSize = 11.sp, color = TextMuted)
                        Text(text = "Escopo: ${ponto.escopo} (${ponto.entidade})", fontSize = 10.sp, color = TextSubtle)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ponto.ativo) ToneGreenBg else ToneRedBg,
                            border = BorderStroke(1.dp, if (ponto.ativo) ToneGreenText else ToneRedText)
                        ) {
                            Text(
                                text = if (ponto.ativo) "Ativo" else "Inativo",
                                fontSize = 10.sp,
                                color = if (ponto.ativo) ToneGreenText else ToneRedText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.abrirFormPonto(ponto) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { viewModel.excluirPonto(ponto.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = ToneRedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormPontoCard(viewModel: ConfiguracoesViewModel, uiState: ConfiguracoesUiState) {
    val editing = uiState.pontosMedicao.firstOrNull { it.id == uiState.pontoEditandoId }
    var nome by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.nome ?: "") }
    var escopo by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.escopo ?: "Equipamento") }
    var entidade by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.entidade ?: "") }
    var dispositivo by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.dispositivo ?: "ESP32-01") }
    var modelo by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.modelo ?: "PZEM-004T V3") }
    var ativo by remember(uiState.pontoEditandoId) { mutableStateOf(editing?.ativo ?: true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = if (editing != null) "Editar Ponto de Medição" else "Cadastrar Ponto de Medição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do Ponto") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dispositivo,
                    onValueChange = { dispositivo = it },
                    label = { Text("Dispositivo") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = entidade,
                    onValueChange = { entidade = it },
                    label = { Text("Entidade Monitorada") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
            }

            // SELETOR DE ESCOPO INTUITIVO (Equipamento / Setor / Local)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Escopo de Medição", fontSize = 12.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Equipamento", "Setor", "Local").forEach { item ->
                        val isSel = escopo == item
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) PrimaryBlue.copy(alpha = 0.25f) else SurfaceDark,
                            border = BorderStroke(1.5.dp, if (isSel) PrimaryBlue else CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { escopo = item }
                        ) {
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) TextWhite else TextMuted,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // SELETOR DE MODELO INTUITIVO
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Modelo do Medidor", fontSize = 12.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("PZEM-004T V3", "PZEM-016", "Outro").forEach { mod ->
                        val isSel = modelo == mod
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) ToneCyanBg else SurfaceDark,
                            border = BorderStroke(1.5.dp, if (isSel) ToneCyanText else CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { modelo = mod }
                        ) {
                            Text(
                                text = mod,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) ToneCyanText else TextMuted,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // SELETOR DE STATUS DO PONTO (Ativo / Inativo)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Status do Ponto de Medição", fontSize = 12.sp, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (ativo) ToneGreenBg else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (ativo) ToneGreenText else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { ativo = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = if (ativo) ToneGreenText else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ativo", fontSize = 11.sp, fontWeight = if (ativo) FontWeight.Bold else FontWeight.Medium, color = if (ativo) ToneGreenText else TextMuted)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!ativo) ToneRedBg else SurfaceDark,
                        border = BorderStroke(1.5.dp, if (!ativo) ToneRedText else CardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { ativo = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = if (!ativo) ToneRedText else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inativo", fontSize = 11.sp, fontWeight = if (!ativo) FontWeight.Bold else FontWeight.Medium, color = if (!ativo) ToneRedText else TextMuted)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { viewModel.fecharFormPonto() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextMuted)
                }

                Button(
                    onClick = {
                        viewModel.salvarPonto(
                            nome.ifBlank { "Novo Ponto" },
                            escopo,
                            entidade.ifBlank { "PE-12" },
                            dispositivo,
                            modelo,
                            ativo = ativo
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar Ponto", color = TextWhite)
                }
            }
        }
    }
}
