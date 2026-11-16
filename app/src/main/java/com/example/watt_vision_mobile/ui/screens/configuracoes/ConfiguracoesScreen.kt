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
                            text = "Consultas e preferências",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Informações de consulta e preferências locais",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                    1 -> AbaTarifas(uiState = uiState)
                    2 -> AbaUsuarios(uiState = uiState)
                    else -> AbaPontosMedicao(uiState = uiState)
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
            InfoGrupoCard(titulo = "Informações da empresa · somente consulta") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Nome da empresa", fontSize = 12.sp, color = TextMuted)
                    Text(text = uiState.geral.nomeEmpresa, color = TextWhite, fontSize = 14.sp)

                    Text(text = "Fuso corporativo · somente consulta", fontSize = 12.sp, color = TextMuted)
                    Text(text = uiState.geral.fusoHorario, color = TextWhite, fontSize = 14.sp)

                    Text(text = "Formato de data", fontSize = 12.sp, color = TextMuted)
                    Text(text = "DD/MM/AAAA (fixo)", color = TextWhite, fontSize = 14.sp)

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
                    Text(text = "Seleção local de tema · demonstração", fontSize = 12.sp, color = TextMuted)
                    Text(text = "O tema global ainda não está integrado. A interface permanece escura.", fontSize = 12.sp, color = TextMuted)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val temas = listOf(
                            Triple("Claro", Icons.Default.LightMode, "Modo Claro"),
                            Triple("Escuro", Icons.Default.DarkMode, "Modo Escuro (Padrão)")
                        )
                        temas.forEach { (temaKey, icon, label) ->
                            val isSelected = uiState.preferencias.tema == temaKey
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
    uiState: ConfiguracoesUiState
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
                                    text = "Ativa",
                                    fontSize = 10.sp,
                                    color = ToneGreenText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (vigente != null) {
                        val inicioFmt = DateFormatter.formatIsoDate(vigente.inicio)
                        val fimFmt = if (vigente.fim.isNotBlank()) DateFormatter.formatIsoDate(vigente.fim) else "Sem data final"

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

            }
        }

        // Consulta do histórico

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
                val inicioFmt = DateFormatter.formatIsoDate(tarifa.inicio)
                val fimFmt = if (tarifa.fim.isNotBlank()) DateFormatter.formatIsoDate(tarifa.fim) else "atual"

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

                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ABA USUÁRIOS — SOMENTE CONSULTA
// ==========================================
@Composable
fun AbaUsuarios(
    uiState: ConfiguracoesUiState
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

                    }
                }
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

                    }
                }
            }
        }
    }
}

// ==========================================
// 4. ABA PONTOS DE MEDIÇÃO — SOMENTE CONSULTA
// ==========================================
@Composable
fun AbaPontosMedicao(
    uiState: ConfiguracoesUiState
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

                }
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

                    }
                }
            }
        }
    }
}
