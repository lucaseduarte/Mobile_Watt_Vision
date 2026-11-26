package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.UsuarioDto
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoGrupoCard
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun ConfiguracoesScreen(
    viewModel: ConfiguracoesViewModel,
    carregarConta: suspend () -> UsuarioDto?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLogout: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WattVisionScreenHeader(
                title = "Configurações do Sistema",
                subtitle = "Informações gerais e usuários",
                leadingAction = {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar configurações",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val tabs = listOf(
                Triple(0, "Geral", Icons.Default.Settings),
                Triple(1, "Usuários", Icons.Default.People)
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
                                    color = if (isSelected) ToneBlueText else TextSecondary
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
                    0 -> AbaGeral(uiState = uiState)
                    1 -> AbaUsuarios(carregarConta = carregarConta, onLogout = onLogout)
                }
            }
        }
    }
}

// ==========================================
// 1. ABA GERAL (Com Informações do Sistema + Aparência)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbaGeral(uiState: ConfiguracoesUiState) {
    val temaClaro = LocalTemaClaro.current
    val alterarTema = LocalAlterarTema.current
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
                        onValueChange = {},
                        readOnly = true,
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

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unidade de energia",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(8.dp),
                            color = InputBackground,
                            border = BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = "kWh (FIXO)",
                                softWrap = false,
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
                            Triple(true, Icons.Default.LightMode, "Modo Claro"),
                            Triple(false, Icons.Default.DarkMode, "Modo Escuro")
                        )
                        temas.forEach { (temaKey, icon, label) ->
                            val isSelected = temaClaro == temaKey
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else InputBackground,
                                border = BorderStroke(1.5.dp, if (isSelected) PrimaryBlue else CardBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(selected = isSelected, role = Role.RadioButton) { alterarTema(temaKey) }
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
// 2. ABA USUÁRIOS (Visual do Header, Badges Admin/Gestor e Controles de Seleção)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbaUsuarios(
    carregarConta: suspend () -> UsuarioDto?,
    onLogout: (() -> Unit)?
) {
    var meuUsuario by remember { mutableStateOf<UsuarioItem?>(null) }
    var contaConsultada by remember { mutableStateOf(false) }
    var mostrarPerfil by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        meuUsuario = carregarConta()?.let { conta ->
            UsuarioItem(
                id = requireNotNull(conta.id).toLong(),
                nome = conta.nome?.takeIf { it.isNotBlank() } ?: "Nome não informado",
                email = requireNotNull(conta.email),
                perfil = conta.perfil?.takeIf { it.isNotBlank() } ?: "Não informado",
                local = "Não informado",
                status = conta.status?.takeIf { it.isNotBlank() } ?: "Não informado"
            )
        }
        contaConsultada = true
    }
    val conta = meuUsuario
    val usuarios = listOfNotNull(conta)

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
                                text = "Conta atual",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "Dados da sessão autenticada",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                    }
                }
            }
        }

        if (contaConsultada && meuUsuario == null) {
            item("conta-indisponivel") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Meu perfil requer uma sessão autenticada.", fontSize = 12.sp, color = TextMuted)
                }
            }
        }

        items(usuarios, key = { if (it === conta) "conta-atual" else "usuario-${it.id}" }) { user ->
            val contaAtual = user === conta
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
                    .then(
                        if (contaAtual) Modifier.clickable(onClickLabel = "Abrir Meu perfil") { mostrarPerfil = true }
                        else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.nome,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(text = user.email, fontSize = 11.sp, color = TextMuted)

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAdmin = user.perfil == "Administrador"
                        val perfilBg = if (isAdmin) corTema(0xFF1E1B4B, 0xFFE0E7FF) else ToneCyanBg
                        val perfilText = if (isAdmin) corTema(0xFF818CF8, 0xFF3730A3) else ToneCyanText
                        val perfilIcon = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = perfilBg,
                            border = BorderStroke(1.dp, perfilText.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(perfilIcon, contentDescription = null, tint = perfilText, modifier = Modifier.size(12.dp))
                                Text(user.perfil, color = perfilText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val isAtivo = user.status == "Ativo"
                        val statusText = if (isAtivo) ToneGreenText else ToneRedText
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAtivo) ToneGreenBg else ToneRedBg,
                            border = BorderStroke(1.dp, statusText.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = user.status,
                                fontSize = 10.sp,
                                color = statusText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (contaAtual) Text("Minha conta", fontSize = 11.sp, color = ToneBlueText)
                    Text(text = "Local: ${user.local}", fontSize = 11.sp, color = TextSubtle)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Último acesso: ${user.ultimoAcesso}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }

    if (mostrarPerfil && conta != null) {
        MeuPerfilSheet(
            conta,
            demonstracao = false,
            onLogout = onLogout,
            onFechar = { mostrarPerfil = false }
        )
    }
}
