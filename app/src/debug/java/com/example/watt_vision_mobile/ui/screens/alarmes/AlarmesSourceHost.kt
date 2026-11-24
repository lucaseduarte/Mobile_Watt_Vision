package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.data.api.AlarmesApiService
import com.example.watt_vision_mobile.data.api.lerDemonstracao
import com.example.watt_vision_mobile.data.api.transporteDemonstracao
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeMapper
import com.example.watt_vision_mobile.ui.theme.ToneYellowText
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

internal class AlarmesDemonstracao(private val api: AlarmesApiService) {
    suspend fun listar(): List<Alarme> = lerDemonstracao(api.listar())
        .map(AlarmeMapper::map)
        .distinctBy { it.id }
        .sortedWith(compareByDescending<Alarme> { it.dataHora }.thenByDescending { it.id })
}

@Composable
internal fun AlarmesSourceHost(
    realState: AlarmesUiState,
    onFiltersChanged: (AlarmesFilters) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier,
    carregarEntidade: suspend (Alarme) -> AlarmeEntidade?,
    onEntidadeClick: (AlarmeEntidade) -> Unit,
    carregarDemo: (suspend () -> List<Alarme>)? = null
) {
    var demonstration by rememberSaveable { mutableStateOf(false) }
    var demoState by remember { mutableStateOf(AlarmesUiState()) }
    var revisao by remember { mutableIntStateOf(0) }
    val demo = remember {
        val dados = AlarmesDemoData.responses(LocalDateTime.of(2026, 11, 23, 12, 0))
        val api = transporteDemonstracao { path ->
            if (path == "/api/alertas") ApiResponse(true, null, dados) else null
        }.create(AlarmesApiService::class.java)
        AlarmesDemonstracao(api)
    }

    LaunchedEffect(demonstration, revisao) {
        if (!demonstration) return@LaunchedEffect
        demoState = demoState.refreshing()
        try {
            val registros = carregarDemo?.invoke() ?: demo.listar()
            ensureActive()
            demoState = demoState.loaded(registros, LocalDateTime.now())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ensureActive()
            demoState = demoState.failed(AlarmesPhase.ERROR)
        }
    }

    key(demonstration) {
        AlarmesContent(
            state = if (demonstration) demoState else realState,
            onFiltersChanged = { filters ->
                if (demonstration) demoState = demoState.copy(filters = filters)
                else onFiltersChanged(filters)
            },
            onRefresh = { if (demonstration) revisao++ else onRefresh() },
            modifier = modifier,
            carregarEntidade = if (demonstration) AlarmesDemoData::entidade else carregarEntidade,
            onEntidadeClick = onEntidadeClick,
            modeControl = {
                Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { demonstration = !demonstration }) {
                        Text(if (demonstration) "Voltar aos dados da API" else "Ver demonstração de Alarmes", fontSize = 12.sp)
                    }
                    if (demonstration) Text("DEBUG · DADOS SIMULADOS\nExemplos locais, sem alteração no sistema.", color = ToneYellowText, fontSize = 12.sp)
                }
            }
        )
    }
}
