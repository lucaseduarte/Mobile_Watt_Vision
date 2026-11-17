package com.example.watt_vision_mobile.ui.screens.alarmes.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.alarmes.*
import com.example.watt_vision_mobile.ui.screens.alarmes.AlarmesFilters
import com.example.watt_vision_mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmesFiltersSheet(filters: AlarmesFilters, onApply: (AlarmesFilters) -> Unit, onDismiss: () -> Unit) {
    var draft by remember(filters) { mutableStateOf(filters) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceDark) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Filtrar alarmes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fechar filtros", tint = TextMuted) }
            }
            Text("Status", color = TextSubtle, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlarmesFilterChip("Todos", draft.status == null) { draft = draft.copy(status = null) }
                AlarmeStatus.entries.filter { it != AlarmeStatus.DESCONHECIDO }.forEach { status ->
                    AlarmesFilterChip(status.label, draft.status == status) { draft = draft.copy(status = status) }
                }
            }
            Text("Severidade", color = TextSubtle, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlarmesFilterChip("Todas", draft.severidade == null) { draft = draft.copy(severidade = null) }
                AlarmeSeveridade.entries.filter { it != AlarmeSeveridade.DESCONHECIDA }.forEach { severity ->
                    AlarmesFilterChip(severity.label, draft.severidade == severity) { draft = draft.copy(severidade = severity) }
                }
            }
            Text("Categoria", color = TextSubtle, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlarmesFilterChip("Todas", draft.categoria == null) { draft = draft.copy(categoria = null) }
                AlarmeCategoria.entries.filter { it != AlarmeCategoria.DESCONHECIDA }.forEach { category ->
                    AlarmesFilterChip(category.label, draft.categoria == category) { draft = draft.copy(categoria = category) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { draft = draft.copy(status = null, severidade = null, categoria = null) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Limpar filtros")
                }
                Button(onClick = { onApply(draft); onDismiss() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Aplicar") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlarmesFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.heightIn(min = 48.dp), colors = FilterChipDefaults.filterChipColors(
            containerColor = InputBackground, labelColor = TextSubtle,
            selectedContainerColor = PrimaryBlue.copy(alpha = .25f), selectedLabelColor = ToneBlueText))
}
