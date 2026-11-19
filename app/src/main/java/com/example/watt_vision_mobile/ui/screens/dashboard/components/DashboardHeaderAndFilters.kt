package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.components.filtros.*
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardFilterState
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartLogic
import com.example.watt_vision_mobile.ui.theme.*

// ==========================================
// NOTA TÉCNICA DE PAGINAÇÃO (Item 5):
// Se a lista de equipamentos ou setores crescer para centenas/milhares de registros,
// a busca e paginação (Paging 3 ou LazyPagingItems) deve ser conectada aqui através
// de um Flow do ViewModel consumindo uma API paginada no DashboardRepository:
// ex: GET /api/equipamentos?search=query&page=page&limit=20
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeaderAndFilters(
    filters: DashboardFilterState,
    lastUpdatedText: String,
    onFilterChanged: (DashboardFilterState) -> Unit,
    onRefreshClick: () -> Unit,
    onRemoveFilter: (String) -> Unit,
    onAlarmesClick: () -> Unit,
    onConfiguracoesClick: () -> Unit,
    modifier: Modifier = Modifier,
    catalog: ChartCatalog? = null
) {
    var isFilterSheetOpen by rememberSaveable { mutableStateOf(false) }

    // Estado temporário (draft) para manipulação no BottomSheet sem afetar o aplicado instantaneamente
    var draftFilters by remember { mutableStateOf(filters) }

    // Estados para controlar qual seletor de busca de 2º nível está aberto
    var activeSelector by remember { mutableStateOf<SelectorType?>(null) }

    // Sincroniza o draft sempre que os filtros aplicados mudarem externamente
    LaunchedEffect(filters) {
        draftFilters = filters
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Cabeçalho superior padrão limpo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Dashboard",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Monitoramento Energético em Tempo Real",
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onAlarmesClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alarmes recentes",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onConfiguracoesClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurações do sistema",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Barra compacta de status e botão "Filtros" com Badge (Sticky ao rolar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InputBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRefreshClick() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar dados",
                        tint = LinkBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastUpdatedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BotaoFiltros(filters.activeCount) {
                draftFilters = filters
                isFilterSheetOpen = true
            }
        }

        // LazyRow de chips ativos com ícone ✕ para remoção individual
        if (!filters.isDefault) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (filters.period != "Hoje") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onRemoveFilter("period") },
                            label = { Text("Período: ${filters.period}", fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover filtro Período ${filters.period}",
                                    modifier = Modifier.size(16.dp)
                                )
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
                if (filters.location != "Todos") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onRemoveFilter("location") },
                            label = { Text("Local: ${filters.location}", fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover filtro Local ${filters.location}",
                                    modifier = Modifier.size(16.dp)
                                )
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
                if (filters.sector != "Todos") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onRemoveFilter("sector") },
                            label = { Text("Setor: ${filters.sector}", fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover filtro Setor ${filters.sector}",
                                    modifier = Modifier.size(16.dp)
                                )
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
                if (filters.equipment != "Todos") {
                    item {
                        InputChip(
                            selected = true,
                            onClick = { onRemoveFilter("equipment") },
                            label = { Text("Equip.: ${filters.equipment}", fontSize = 11.sp) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remover filtro Equipamento ${filters.equipment}",
                                    modifier = Modifier.size(16.dp)
                                )
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
        }
    }

    // Modal Bottom Sheet Principal
    if (isFilterSheetOpen) {
        PainelFiltros(
            onDismiss = { isFilterSheetOpen = false },
            onClear = { draftFilters = DashboardFilterState() },
            onApply = {
                onFilterChanged(draftFilters)
                isFilterSheetOpen = false
            }
        ) {
            // 1. Período: FilterChips em fluxo
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Período", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartLogic.periods.forEach { periodOpt ->
                        val isSelected = draftFilters.period == periodOpt
                        FilterChip(
                            selected = isSelected,
                            onClick = { draftFilters = draftFilters.copy(period = periodOpt) },
                            label = { Text(periodOpt, fontSize = 11.sp) },
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
            }

            // 2. Seletor de Local (FiltroLinhaSeletor)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Local", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
                FiltroLinhaSeletor(
                    label = "Local selecionado",
                    valorAtual = draftFilters.location,
                    onClick = { activeSelector = SelectorType.LOCAL }
                )
            }

            // 3. Seletor de Setor (FiltroLinhaSeletor com dependência em cascata do Local)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Setor", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
                FiltroLinhaSeletor(
                    label = "Setor selecionado",
                    valorAtual = draftFilters.sector,
                    onClick = { activeSelector = SelectorType.SETOR }
                )
            }

            // 4. Seletor de Equipamento (FiltroLinhaSeletor com dependência em cascata do Setor)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Equipamento", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle)
                FiltroLinhaSeletor(
                    label = "Equipamento selecionado",
                    valorAtual = draftFilters.equipment,
                    onClick = { activeSelector = SelectorType.EQUIPAMENTO }
                )
            }
        }
    }

    // Seletor de Segundo Nível (FiltroSeletorBusca)
    activeSelector?.let { selector ->
        val all = FiltroItemModel("Todos", "Todos")
        val (titulo, items) = when (selector) {
            SelectorType.LOCAL -> "Local" to (listOf(all) + catalog?.locais.orEmpty().mapNotNull { local ->
                val id = local.id?.takeIf { it > 0 } ?: return@mapNotNull null
                val name = local.nome?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                FiltroItemModel(id.toString(), name)
            })
            SelectorType.SETOR -> "Setor" to (listOf(all) + catalog?.sectors(draftFilters.locationId).orEmpty().mapNotNull { sector ->
                val id = sector.id?.takeIf { it > 0 } ?: return@mapNotNull null
                val name = sector.nome?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                FiltroItemModel(id.toString(), name, catalog?.locais?.firstOrNull { it.id == sector.id_local }?.nome)
            })
            SelectorType.EQUIPAMENTO -> "Equipamento" to (listOf(all) + catalog?.equipment(draftFilters.locationId, draftFilters.sectorId).orEmpty().mapNotNull { equipment ->
                val id = equipment.id?.takeIf { it > 0 } ?: return@mapNotNull null
                val name = equipment.nome?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                FiltroItemModel(id.toString(), name, catalog?.setores?.firstOrNull { it.id == equipment.id_setor }?.nome)
            })
        }

        val selectedValue = when (selector) {
            SelectorType.LOCAL -> draftFilters.locationId?.toString() ?: "Todos"
            SelectorType.SETOR -> draftFilters.sectorId?.toString() ?: "Todos"
            SelectorType.EQUIPAMENTO -> draftFilters.equipmentId?.toString() ?: "Todos"
        }

        FiltroSeletorBusca(
            titulo = titulo,
            items = items,
            selectedId = selectedValue,
            onItemSelected = {},
            onItemIdSelected = { selectedId ->
                val selected = items.singleOrNull { it.id == selectedId } ?: return@FiltroSeletorBusca
                val selectedTitle = selected.title
                val id = selectedId.toIntOrNull()
                draftFilters = when (selector) {
                    SelectorType.LOCAL -> {
                        // Mudar Local reseta Setor e Equipamento para "Todos"
                        draftFilters.copy(
                            location = selectedTitle,
                            locationId = id,
                            sector = "Todos",
                            sectorId = null,
                            equipment = "Todos",
                            equipmentId = null
                        )
                    }
                    SelectorType.SETOR -> {
                        // Mudar Setor reseta Equipamento para "Todos"
                        draftFilters.copy(
                            sector = selectedTitle,
                            sectorId = id,
                            equipment = "Todos",
                            equipmentId = null
                        )
                    }
                    SelectorType.EQUIPAMENTO -> {
                        draftFilters.copy(equipment = selectedTitle, equipmentId = id)
                    }
                }
                activeSelector = null
            },
            onDismiss = { activeSelector = null }
        )
    }
}

enum class SelectorType {
    LOCAL, SETOR, EQUIPAMENTO
}

@Composable
fun FiltroLinhaSeletor(
    label: String,
    valorAtual: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InputBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp) // Requisito Mínimo 48dp
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .semantics {
                contentDescription = "$label, selecionado: $valorAtual. Toque para alterar"
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valorAtual,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS (Conforme solicitado)
// ==========================================

@Preview(name = "Sem filtros", showBackground = true, backgroundColor = 0xFF070C15)
@Composable
fun PreviewDashboardHeaderWithoutFilters() {
    WattVisionTheme {
        DashboardHeaderAndFilters(
            filters = DashboardFilterState(),
            lastUpdatedText = "Atualizado às 10:28",
            onFilterChanged = {},
            onRefreshClick = {},
            onRemoveFilter = {},
            onAlarmesClick = {},
            onConfiguracoesClick = {}
        )
    }
}

@Preview(name = "Com filtros ativos", showBackground = true, backgroundColor = 0xFF070C15)
@Composable
fun PreviewDashboardHeaderWithActiveFilters() {
    WattVisionTheme {
        DashboardHeaderAndFilters(
            filters = DashboardFilterState(period = "7 dias", location = "Planta Industrial Jandira", sector = "Produção"),
            lastUpdatedText = "Atualizado às 10:28",
            onFilterChanged = {},
            onRefreshClick = {},
            onRemoveFilter = {},
            onAlarmesClick = {},
            onConfiguracoesClick = {}
        )
    }
}
