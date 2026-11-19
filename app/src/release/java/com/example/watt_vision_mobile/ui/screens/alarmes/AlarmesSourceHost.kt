package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade

/** Release não contém demonstração, seletor de origem nem registros fictícios de Alarmes. */
@Composable
fun AlarmesSourceHost(
    realState: AlarmesUiState,
    onFiltersChanged: (AlarmesFilters) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
    carregarEntidade: suspend (Alarme) -> AlarmeEntidade?,
    onEntidadeClick: (AlarmeEntidade) -> Unit
) {
    AlarmesContent(realState, onFiltersChanged, onRefresh, modifier, carregarEntidade, onEntidadeClick)
}
