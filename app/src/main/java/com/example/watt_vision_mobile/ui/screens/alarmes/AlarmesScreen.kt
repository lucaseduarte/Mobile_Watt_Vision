package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.alarmes.*
import com.example.watt_vision_mobile.data.api.integracaoLocal
import com.example.watt_vision_mobile.ui.components.AtualizacaoPeriodica
import com.example.watt_vision_mobile.ui.components.ControleAtualizacao
import com.example.watt_vision_mobile.ui.components.WattVisionScreenHeader
import com.example.watt_vision_mobile.ui.screens.alarmes.components.*
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun AlarmesScreen(viewModel: AlarmesViewModel, onEntidadeClick: (AlarmeEntidade) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    AtualizacaoPeriodica(integracaoLocal, viewModel::refreshAutomatically)
    AlarmesContent(state, viewModel::updateFilters, viewModel::refresh, modifier, viewModel::entidade, onEntidadeClick)
}

@Composable
fun AlarmesContent(
    state: AlarmesUiState,
    onFiltersChanged: (AlarmesFilters) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    carregarEntidade: suspend (Alarme) -> AlarmeEntidade?,
    onEntidadeClick: (AlarmeEntidade) -> Unit,
    modeControl: (@Composable () -> Unit)? = null
) {
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Alarme?>(null) }
    val visible = remember(state.records, state.filters) { AlarmesRules.filter(state.records, state.filters) }
    val counts = remember(state.records, state.hasSnapshot) { if (state.hasSnapshot) AlarmesRules.counts(state.records) else null }
    // Detalhes acompanham o snapshot atual; nunca manter registro antigo sem indicação.
    LaunchedEffect(state.records) {
        selected = selected?.let { old -> state.records.firstOrNull { it.id != null && it.id == old.id } }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(DarkBackground).imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WattVisionScreenHeader(
                title = "Alarmes",
                subtitle = "Consulta e acompanhamento",
                contentPadding = PaddingValues(top = 12.dp)
            )
            ControleAtualizacao(
                state.updatedAt?.toLocalTime(), onRefresh,
                habilitado = !state.isRefreshing, descricao = "Atualizar alarmes"
            )
        }
        if (modeControl != null) item { modeControl() }
        item { AlarmesIndicators(counts) }
        item {
            OutlinedTextField(value = state.filters.query, onValueChange = { onFiltersChanged(state.filters.copy(query = it)) },
                placeholder = { Text("Buscar alarme ou equipamento…", color = TextSecondary, fontSize = 13.sp) }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                trailingIcon = { if (state.filters.query.isNotEmpty()) IconButton(onClick = { onFiltersChanged(state.filters.copy(query = "")) }) {
                    Icon(Icons.Default.Close, "Limpar pesquisa", tint = TextMuted)
                } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryBlue, unfocusedBorderColor = CardBorder,
                    focusedContainerColor = InputBackground, unfocusedContainerColor = InputBackground))
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (state.hasSnapshot) "${visible.size} de ${state.records.size} alarmes" else "Lista de alarmes", fontSize = 12.sp, color = TextSubtle)
                Button(
                    onClick = { filtersOpen = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.filters.activeCount > 0) "Filtros (${state.filters.activeCount})" else "Filtros", fontSize = 12.sp)
                }
            }
            if (state.filters.activeCount > 0) Text(listOfNotNull(state.filters.status?.label, state.filters.severidade?.label,
                state.filters.categoria?.label).joinToString(" · "), fontSize = 11.sp, color = ToneBlueText)
        }
        if (state.isRefreshing) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue, trackColor = CardBorder) }
        val problem = when (state.phase) {
            AlarmesPhase.NO_SESSION -> "Entre na sua conta para consultar os alarmes reais."
            AlarmesPhase.FORBIDDEN -> "Acesso negado. Seu usuário não pode consultar estes dados."
            AlarmesPhase.ERROR -> "Não foi possível conectar. Verifique a conexão e tente novamente."
            AlarmesPhase.UNAVAILABLE -> "Consulta indisponível. Não foi possível obter uma resposta válida."
            else -> null
        }
        if (problem != null) item {
            AlarmesNotice(problem, if (state.phase == AlarmesPhase.NO_SESSION) null else onRefresh)
        }
        if (state.hasSnapshot && visible.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AlarmesNotice(if (state.records.isEmpty()) "Nenhum alarme disponível." else "Nenhum resultado para a pesquisa e os filtros selecionados.")
                if (state.records.isNotEmpty()) TextButton(onClick = { onFiltersChanged(AlarmesFilters()) }) { Text("Limpar pesquisa e filtros") }
            }
        }
        itemsIndexed(visible, key = { index, record -> "${record.id}:$index" }) { _, record ->
            AlarmeCard(record) { selected = record }
        }
    }
    if (filtersOpen) AlarmesFiltersSheet(state.filters, onFiltersChanged) { filtersOpen = false }
    selected?.let { record ->
        AlarmeDetailsSheet(
            alarme = record,
            carregarEntidade = carregarEntidade,
            onEntidadeClick = { entidade ->
                selected = null
                onEntidadeClick(entidade)
            },
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun AlarmesNotice(message: String, onRetry: (() -> Unit)? = null) {
    Surface(color = SurfaceDark, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, fontSize = 13.sp, color = TextSubtle)
            if (onRetry != null) TextButton(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
