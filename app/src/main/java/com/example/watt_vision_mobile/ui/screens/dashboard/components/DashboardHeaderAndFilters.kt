package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun DashboardHeaderAndFilters(
    state: DashboardUiState,
    onFilterChanged: (DashboardSelection) -> Unit,
    onRefreshClick: () -> Unit,
    onAlarmesClick: () -> Unit,
    onConfiguracoesClick: () -> Unit
) {
    var filtersOpen by remember { mutableStateOf(false) }
    val s = state.selection
    val c = state.catalog
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall, color = TextWhite)
        Text(state.lastUpdatedText, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filtersOpen = !filtersOpen }) { Text("Filtros") }
            OutlinedButton(onClick = onRefreshClick, enabled = !state.isLoading) { Text("Atualizar") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAlarmesClick) { Text("Alarmes") }
            TextButton(onClick = onConfiguracoesClick) { Text("Configurações") }
        }
        Text("Dados: API · sem demonstração neste Dashboard", style = MaterialTheme.typography.bodySmall, color = ToneCyanText)
        Text("Local: ${c.locais.firstOrNull { it.id == s.location }?.nome ?: if (s.location == null) "Todos autorizados" else "Indisponível"} · Setor: ${s.sector ?: "Todos"} · Equipamento: ${s.equipment ?: "Todos"}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text("Ponto: ${s.point ?: "Todos"} · Medidor: ${s.sensor?.let { "PZEM #$it" } ?: "Selecione nos filtros"}", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
        if (filtersOpen) {
            Surface(color = SurfaceDark, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, CardBorder)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filtros por vínculo cadastral. Grandezas e consumo exigem um único medidor.", style = MaterialTheme.typography.bodySmall)
                    CatalogSelector("Local", s.location, c.locais.mapNotNull { it.id?.let { id -> id to (it.nome ?: "Local #$id") } }) {
                        onFilterChanged(s.copy(location = it, sector = null, equipment = null, point = null, sensor = null))
                    }
                    val sectors = c.setores.filter { s.location == null || it.id_local == s.location }
                    CatalogSelector("Setor", s.sector, sectors.mapNotNull { it.id?.let { id -> id to (it.nome ?: "Setor #$id") } }) {
                        onFilterChanged(s.copy(sector = it, equipment = null, point = null, sensor = null))
                    }
                    val sectorIds = sectors.filter { s.sector == null || it.id == s.sector }.mapNotNull { it.id }.toSet()
                    CatalogSelector("Equipamento", s.equipment, c.equipamentos.filter { it.id_setor in sectorIds }.mapNotNull { it.id?.let { id -> id to (it.nome ?: "Equipamento #$id") } }) {
                        onFilterChanged(s.copy(equipment = it, point = null, sensor = null))
                    }
                    CatalogSelector("Ponto de medição", s.point, c.pointsFor(s.copy(point = null)).mapNotNull { it.id?.let { id -> id to (it.nome ?: "Ponto #$id") } }) {
                        onFilterChanged(s.copy(point = it, sensor = null))
                    }
                    CatalogSelector("Medidor PZEM", s.sensor, c.sensorsFor(s).mapNotNull { it.id?.let { id -> id to "${it.modelo ?: "PZEM"} · ${it.nome_ponto_medicao ?: "Ponto #${it.id_ponto_medicao}"}" } }, "Nenhum selecionado") {
                        onFilterChanged(s.copy(sensor = it))
                    }
                    TextButton(onClick = { onFilterChanged(DashboardSelection(start = s.start, end = s.end, period = s.period)) }) { Text("Limpar seleção de entidades") }
                }
            }
        }
    }
}

@Composable
private fun CatalogSelector(label: String, selected: Int?, options: List<Pair<Int, String>>, emptyLabel: String = "Todos", onSelect: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("$label: ${options.firstOrNull { it.first == selected }?.let { "${it.second} (#${it.first})" } ?: if (selected == null) emptyLabel else "Indisponível #$selected"}")
    }
    if (open) FiltroSeletorBusca(
        titulo = label,
        items = listOf(FiltroItemModel("Todos", emptyLabel)) + options.map { FiltroItemModel(it.first.toString(), "${it.second} (#${it.first})") },
        selectedId = selected?.toString() ?: "Todos",
        onItemSelected = { id -> onSelect(id.toIntOrNull()); open = false },
        onDismiss = { open = false }
    )
}
