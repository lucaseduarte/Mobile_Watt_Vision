package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release não contém demonstração, seletor de origem nem registros fictícios de Alarmes. */
@Composable
fun AlarmesSourceHost(realState: AlarmesUiState, onFiltersChanged: (AlarmesFilters) -> Unit, onRefresh: () -> Unit, modifier: Modifier) {
    AlarmesContent(realState, onFiltersChanged, onRefresh, modifier)
}
