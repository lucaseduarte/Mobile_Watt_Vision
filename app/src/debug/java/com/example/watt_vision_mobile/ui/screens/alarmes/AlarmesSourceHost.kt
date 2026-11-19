package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.theme.ToneYellowText
import java.time.LocalDateTime

@Composable
fun AlarmesSourceHost(
    realState: AlarmesUiState,
    onFiltersChanged: (AlarmesFilters) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
    carregarEntidade: suspend (Alarme) -> AlarmeEntidade?,
    onEntidadeClick: (AlarmeEntidade) -> Unit
) {
    var demonstration by rememberSaveable { mutableStateOf(false) }
    var demoState by remember { mutableStateOf<AlarmesUiState?>(null) }
    fun refreshDemo() {
        val now = LocalDateTime.now()
        demoState = (demoState ?: AlarmesUiState()).loaded(AlarmesDemoData.create(now), now)
    }
    LaunchedEffect(demonstration) { if (demonstration && demoState == null) refreshDemo() }
    AlarmesContent(
        state = if (demonstration) demoState ?: AlarmesUiState() else realState,
        onFiltersChanged = { filters ->
            if (demonstration) demoState = (demoState ?: AlarmesUiState()).copy(filters = filters)
            else onFiltersChanged(filters)
        },
        onRefresh = { if (demonstration) refreshDemo() else onRefresh() },
        modifier = modifier,
        carregarEntidade = if (demonstration) AlarmesDemoData::entidade else carregarEntidade,
        onEntidadeClick = onEntidadeClick,
        modeControl = {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    if (!demonstration && demoState == null) refreshDemo()
                    demonstration = !demonstration
                }) { Text(if (demonstration) "Voltar aos dados da API" else "Ver demonstração de Alarmes", fontSize = 12.sp) }
                if (demonstration) Text("DEBUG · DADOS SIMULADOS\nExemplos locais, sem alteração no sistema.", color = ToneYellowText, fontSize = 12.sp)
            }
        }
    )
}
