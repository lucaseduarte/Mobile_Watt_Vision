package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
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
                            text = "Informações gerais, tarifas e preferências",
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
                        onClick = onBackClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("Cancelar", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onBackClick,
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
            // Abas de Configurações com Ícones (Geral, Tarifas, Usuários, Pontos de Medição)
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

                    Text(text = "Formato de data", fontSize = 12.sp, color = TextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD").forEach { formato ->
                            val isSelected = uiState.geral.formatoData == formato
                            Button(
                                onClick = { viewModel.updateFormatoData(formato) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) PrimaryBlue else InputBackground
                                ),
                                border = BorderStroke(1.dp, CardBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = formato,
                                    fontSize = 10.sp,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    maxLines = 1
                                )
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

        // Card Aparência (Faltava na versão mobile!)
        item {
            InfoGrupoCard(titulo = "Aparência (Preferências visuais)") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Tema da Interface", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(text = "Modo escuro padronizado Watt Vision", fontSize = 11.sp, color = TextMuted)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = InputBackground,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "Escuro (FIXO)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ABA TARIFAS
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
                        Text(
                            text = String.format(Locale.getDefault(), "R$ %.2f / kWh", vigente.valor),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ToneCyanText
                        )
                        Text(
                            text = "${vigente.descricao} — desde ${vigente.inicio}",
                            fontSize = 11.sp,
                            color = TextWhite
                        )
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
                FormTarifaCard(viewModel = viewModel)
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
                            Text(
                                text = String.format(Locale.getDefault(), "R$ %.2f / kWh  ·  Vigência: %s", tarifa.valor, tarifa.inicio),
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
fun FormTarifaCard(viewModel: ConfiguracoesViewModel) {
    var desc by remember { mutableStateOf("") }
    var valorStr by remember { mutableStateOf("0.80") }
    var inicio by remember { mutableStateOf("2026-01-01") }
    var fim by remember { mutableStateOf("") }
    var ativa by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Cadastrar / Editar Tarifa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Descrição da Tarifa") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = valorStr,
                    onValueChange = { valorStr = it },
                    label = { Text("Valor R$/kWh") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = inicio,
                    onValueChange = { inicio = it },
                    label = { Text("Data Inicial") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Definir como Tarifa Ativa", fontSize = 12.sp, color = TextWhite)
                Switch(checked = ativa, onCheckedChange = { ativa = it })
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
// 3. ABA USUÁRIOS
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
                    Text(
                        text = "${uiState.usuarios.size} usuários cadastrados · $admins Administradores · $gestores Gestores",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

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

        if (uiState.mostrarFormUsuario) {
            item {
                FormUsuarioCard(viewModel = viewModel)
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${user.email}  ·  ${user.perfil}", fontSize = 11.sp, color = TextMuted)
                        Text(text = "Local: ${user.local}", fontSize = 10.sp, color = TextMuted)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge Status
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (user.status == "Ativo") ToneGreenText.copy(alpha = 0.15f) else CardBorder,
                            border = BorderStroke(1.dp, if (user.status == "Ativo") ToneGreenText else CardBorder)
                        ) {
                            Text(
                                text = user.status,
                                fontSize = 10.sp,
                                color = if (user.status == "Ativo") ToneGreenText else TextMuted,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
fun FormUsuarioCard(viewModel: ConfiguracoesViewModel) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf("Administrador") }
    var status by remember { mutableStateOf("Ativo") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Cadastrar Usuário", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.fecharFormUsuario() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = TextMuted)
                }

                Button(
                    onClick = {
                        viewModel.salvarUsuario(nome.ifBlank { "Novo Usuário" }, email.ifBlank { "user@wattvision.com" }, perfil, status)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar", color = TextWhite)
                }
            }
        }
    }
}

// ==========================================
// 4. ABA PONTOS DE MEDIÇÃO
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
                    Column {
                        Text(text = "Pontos de Medição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${uiState.pontosMedicao.size} pontos configurados · $totalAtivos ativos · $totalInativos inativos", fontSize = 11.sp, color = TextMuted)
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
                FormPontoCard(viewModel = viewModel)
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
                        Text(text = "Escopo: ${ponto.escopo} (${ponto.entidade})", fontSize = 10.sp, color = TextMuted)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ponto.ativo) ToneGreenText.copy(alpha = 0.15f) else CardBorder,
                            border = BorderStroke(1.dp, if (ponto.ativo) ToneGreenText else CardBorder)
                        ) {
                            Text(
                                text = if (ponto.ativo) "Ativo" else "Inativo",
                                fontSize = 10.sp,
                                color = if (ponto.ativo) ToneGreenText else TextMuted,
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
fun FormPontoCard(viewModel: ConfiguracoesViewModel) {
    var nome by remember { mutableStateOf("") }
    var escopo by remember { mutableStateOf("Equipamento") }
    var entidade by remember { mutableStateOf("") }
    var dispositivo by remember { mutableStateOf("ESP32-01") }
    var modelo by remember { mutableStateOf("PZEM-004T V3") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = InputBackground,
        border = BorderStroke(1.dp, PrimaryBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Cadastrar Ponto de Medição", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome do Ponto") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modelo
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salvar", color = TextWhite)
                }
            }
        }
    }
}
