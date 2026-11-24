package com.example.watt_vision_mobile.ui.screens.operacional

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.components.WattVisionHeaderShortcut
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.screens.operacional.components.*
import com.example.watt_vision_mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OperacionalScreen(
    viewModel: OperacionalViewModel,
    onEquipamentoClick: (String) -> Unit = {},
    onSetorClick: (SetorItem) -> Unit = {},
    onLocalClick: (LocalItem) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage = uiState.errorMessage
    val pagerState = rememberPagerState(initialPage = uiState.activeTab) { 3 }
    val coroutineScope = rememberCoroutineScope()

    // Sincroniza a aba ativa do ViewModel com o Pager
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setActiveTab(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WattVisionScreenHeader(
                title = "Operacional",
                subtitle = "Cadastro do Backend"
            ) {
                WattVisionHeaderShortcut(Icons.Filled.Notifications, "Alarmes recentes") { onNavigate("Alarmes") }
                Spacer(Modifier.width(8.dp))
                WattVisionHeaderShortcut(Icons.Filled.Settings, "Configurações do sistema") { onNavigate("Configurações") }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BoxWithConstraints {
                val tabs = listOf("Equipamentos", "Setores", "Locais")
                val medir = rememberTextMeasurer()
                val estilo = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val precisaRolar = with(LocalDensity.current) {
                    val larguraTexto = (maxWidth / tabs.size - 32.dp).toPx()
                    tabs.any { medir.measure(it, estilo, softWrap = false).size.width > larguraTexto }
                }
                val conteudo: @Composable () -> Unit = {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) ToneBlueText else TextSecondary
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .semantics { contentDescription = "Aba $title, selecionada: $isSelected" }
                        )
                    }
                }
                val divisor: @Composable () -> Unit = { HorizontalDivider(color = CardBorder) }
                if (precisaRolar) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = DarkBackground,
                        contentColor = PrimaryBlue,
                        edgePadding = 0.dp,
                        divider = divisor,
                        tabs = conteudo
                    )
                } else {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = DarkBackground,
                        contentColor = PrimaryBlue,
                        divider = divisor,
                        tabs = conteudo
                    )
                }
            }

            // Pager das Abas
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val locaisList = remember(uiState.locais) { uiState.locais.map { it.nome } }
                val setoresList = remember(uiState.setores) { uiState.setores.map { it.nome to it.localNome } }

                when (page) {
                    0 -> {
                        // ABA 0: EQUIPAMENTOS
                        val filteredEqs = viewModel.getFilteredEquipamentos()
                        val filters = uiState.equipamentosFilters

                        Column(modifier = Modifier.fillMaxSize()) {
                            OperacionalFilterBar(
                                tabIndex = 0,
                                filters = filters,
                                onQueryChanged = { viewModel.updateSearchQuery(0, it) },
                                onStatusChanged = { viewModel.updateStatusFilter(0, it) },
                                onApplyFilters = { viewModel.applyTabFilters(0, it) },
                                onRemoveFilter = { viewModel.removeSpecificFilter(0, it) },
                                locaisList = locaisList,
                                setoresList = setoresList
                            )

                            if (uiState.isLoading) {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(5) { OperacionalSkeletonCard() }
                                }
                            } else if (errorMessage != null) {
                                OperacionalErroState(errorMessage, viewModel::loadData)
                            } else if (uiState.equipamentos.isEmpty()) {
                                ListaVaziaState(
                                    mensagem = "Nenhum registro cadastrado. Os cadastros são feitos pela versão web."
                                )
                            } else if (filteredEqs.isEmpty()) {
                                BuscaSemResultadoState(
                                    onLimparFiltros = { viewModel.clearTabFilters(0) }
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredEqs, key = { it.id }) { eq ->
                                        EquipamentoCard(
                                            item = eq,
                                            onClick = { onEquipamentoClick(eq.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // ABA 1: SETORES
                        val filteredSts = viewModel.getFilteredSetores()
                        val filters = uiState.setoresFilters

                        Column(modifier = Modifier.fillMaxSize()) {
                            OperacionalFilterBar(
                                tabIndex = 1,
                                filters = filters,
                                onQueryChanged = { viewModel.updateSearchQuery(1, it) },
                                onStatusChanged = { viewModel.updateStatusFilter(1, it) },
                                onApplyFilters = { viewModel.applyTabFilters(1, it) },
                                onRemoveFilter = { viewModel.removeSpecificFilter(1, it) },
                                locaisList = locaisList,
                                setoresList = setoresList
                            )

                            if (uiState.isLoading) {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(5) { OperacionalSkeletonCard() }
                                }
                            } else if (errorMessage != null) {
                                OperacionalErroState(errorMessage, viewModel::loadData)
                            } else if (uiState.setores.isEmpty()) {
                                ListaVaziaState(
                                    mensagem = "Nenhum registro cadastrado. Os cadastros são feitos pela versão web."
                                )
                            } else if (filteredSts.isEmpty()) {
                                BuscaSemResultadoState(
                                    onLimparFiltros = { viewModel.clearTabFilters(1) }
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredSts, key = { it.id }) { st ->
                                        SetorCard(
                                            item = st,
                                            onClick = { onSetorClick(st) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // ABA 2: LOCAIS
                        val filteredLcs = viewModel.getFilteredLocais()
                        val filters = uiState.locaisFilters

                        Column(modifier = Modifier.fillMaxSize()) {
                            OperacionalFilterBar(
                                tabIndex = 2,
                                filters = filters,
                                onQueryChanged = { viewModel.updateSearchQuery(2, it) },
                                onStatusChanged = { viewModel.updateStatusFilter(2, it) },
                                onApplyFilters = { viewModel.applyTabFilters(2, it) },
                                onRemoveFilter = { viewModel.removeSpecificFilter(2, it) },
                                locaisList = locaisList,
                                setoresList = setoresList
                            )

                            if (uiState.isLoading) {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(5) { OperacionalSkeletonCard() }
                                }
                            } else if (errorMessage != null) {
                                OperacionalErroState(errorMessage, viewModel::loadData)
                            } else if (uiState.locais.isEmpty()) {
                                ListaVaziaState(
                                    mensagem = "Nenhum registro cadastrado. Os cadastros são feitos pela versão web."
                                )
                            } else if (filteredLcs.isEmpty()) {
                                BuscaSemResultadoState(
                                    onLimparFiltros = { viewModel.clearTabFilters(2) }
                                )
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredLcs, key = { it.id }) { lc ->
                                        LocalCard(
                                            item = lc,
                                            onClick = { onLocalClick(lc) }
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
}

@Composable
internal fun <T : Any> OperacionalDetalhe(
    state: OperacionalUiState,
    item: T?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
    content: @Composable (T) -> Unit
) {
    if (!state.isLoading && state.errorMessage == null && item != null) {
        content(item)
        return
    }

    BackHandler(onBack = onBack)
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onBack) { Text("Voltar") }
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.padding(24.dp))
            state.errorMessage != null -> OperacionalErroState(state.errorMessage, onRetry)
            else -> ListaVaziaState("Cadastro não encontrado.")
        }
    }
}

@Composable
private fun OperacionalErroState(mensagem: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(mensagem, fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}

// Estado Vazio (Sem registros cadastrados no sistema)
@Composable
fun ListaVaziaState(mensagem: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = mensagem,
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Estado de Busca sem Resultados
@Composable
fun BuscaSemResultadoState(onLimparFiltros: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nenhum resultado encontrado",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Button(
                onClick = onLimparFiltros,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = "Limpar filtros", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Lista Vazia de Registros", showBackground = true, backgroundColor = 0xFF070C15)
@Composable
fun PreviewListaVaziaState() {
    WattVisionTheme {
        ListaVaziaState(mensagem = "Nenhum registro cadastrado. Os cadastros são feitos pela versão web.")
    }
}

@Preview(name = "Busca sem Resultados", showBackground = true, backgroundColor = 0xFF070C15)
@Composable
fun PreviewBuscaSemResultadoState() {
    WattVisionTheme {
        BuscaSemResultadoState(onLimparFiltros = {})
    }
}
