package com.example.watt_vision_mobile.ui.screens.relatorios

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.components.filtros.FiltroItemModel
import com.example.watt_vision_mobile.ui.components.filtros.FiltroLinhaSeletor
import com.example.watt_vision_mobile.ui.components.filtros.FiltroSeletorBusca
import com.example.watt_vision_mobile.ui.theme.PrimaryBlue
import com.example.watt_vision_mobile.ui.theme.SurfaceDark
import com.example.watt_vision_mobile.ui.theme.ToneBlueBg
import com.example.watt_vision_mobile.ui.theme.ToneBlueText
import com.example.watt_vision_mobile.ui.theme.TextSecondary
import com.example.watt_vision_mobile.ui.theme.TextWhite

private enum class TipoFiltro(val titulo: String) {
    PERIODO("Período"), LOCAL("Local"), SETOR("Setor"), EQUIPAMENTO("Equipamento"), GRANDEZA("Grandeza")
}

private data class Opcao(val id: String?, val titulo: String)

private fun opcoes(tipo: TipoFiltro, estado: RelatoriosUiState, filtros: FiltrosRelatorio): List<Opcao> = when (tipo) {
    TipoFiltro.PERIODO -> PeriodoRelatorio.entries.map { Opcao(it.name, it.titulo) }
    TipoFiltro.LOCAL -> listOf(Opcao(null, "Todos os locais")) + estado.locais.map { Opcao(it.id, it.nome) }
    TipoFiltro.SETOR -> listOf(Opcao(null, "Todos os setores")) + estado.setores
        .filter { filtros.localId == null || filtros.localId == it.localId }.map { Opcao(it.id, it.nome) }
    TipoFiltro.EQUIPAMENTO -> listOf(Opcao(null, "Todos os equipamentos")) + estado.equipamentos
        .filter {
            (filtros.localId == null || filtros.localId == it.localId) &&
                (filtros.setorId == null || filtros.setorId == it.setorId)
        }.map { Opcao(it.id, it.nome) }
    TipoFiltro.GRANDEZA -> listOf(Opcao(null, "Todas as grandezas")) + GrandezaRelatorio.entries.map { Opcao(it.name, it.titulo) }
}

private fun idSelecionado(tipo: TipoFiltro, filtro: FiltrosRelatorio): String? = when (tipo) {
    TipoFiltro.PERIODO -> filtro.periodo.name
    TipoFiltro.LOCAL -> filtro.localId
    TipoFiltro.SETOR -> filtro.setorId
    TipoFiltro.EQUIPAMENTO -> filtro.equipamentoId
    TipoFiltro.GRANDEZA -> filtro.grandeza?.name
}

private fun alterar(filtros: FiltrosRelatorio, tipo: TipoFiltro, id: String?): FiltrosRelatorio = when (tipo) {
    TipoFiltro.PERIODO -> filtros.copy(periodo = PeriodoRelatorio.valueOf(requireNotNull(id)))
    TipoFiltro.LOCAL -> if (id == filtros.localId) filtros else filtros.copy(localId = id, setorId = null, equipamentoId = null)
    TipoFiltro.SETOR -> if (id == filtros.setorId) filtros else filtros.copy(setorId = id, equipamentoId = null)
    TipoFiltro.EQUIPAMENTO -> filtros.copy(equipamentoId = id)
    TipoFiltro.GRANDEZA -> filtros.copy(grandeza = id?.let(GrandezaRelatorio::valueOf))
}

@Composable
internal fun BotaoFiltrosRelatorio(ativos: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(36.dp).semantics { stateDescription = "$ativos filtros ativos" }
    ) {
        Icon(Icons.Filled.FilterList, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Filtros", fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.Bold)
        if (ativos > 0) {
            Spacer(Modifier.width(6.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = PrimaryBlue, modifier = Modifier.size(18.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(ativos.toString(), fontSize = 10.sp, color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FiltrosRelatorioSheet(
    estado: RelatoriosUiState,
    onFechar: () -> Unit,
    onAplicar: (FiltrosRelatorio) -> Unit
) {
    var filtros by remember { mutableStateOf(estado.filtros) }
    var seletor by remember { mutableStateOf<TipoFiltro?>(null) }
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        contentColor = TextWhite
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Filtros do relatório", style = MaterialTheme.typography.titleLarge)
            TipoFiltro.entries.forEach { tipo ->
                val id = idSelecionado(tipo, filtros)
                FiltroLinhaSeletor(
                    label = tipo.titulo,
                    valorAtual = opcoes(tipo, estado, filtros).firstOrNull { it.id == id }?.titulo.orEmpty(),
                    onClick = { seletor = tipo }
                )
            }
            TarifaFiltro(estado, filtros.periodo)
            Button(
                onClick = { onAplicar(filtros) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = TextWhite),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Aplicar Filtros") }
        }
    }
    seletor?.let { tipo ->
        val itens = opcoes(tipo, estado, filtros)
        FiltroSeletorBusca(
            titulo = tipo.titulo,
            items = itens.map { FiltroItemModel(it.id ?: "Todos", it.titulo) },
            selectedId = idSelecionado(tipo, filtros) ?: "Todos",
            onItemSelected = { titulo ->
                itens.firstOrNull { it.titulo == titulo }?.let { filtros = alterar(filtros, tipo, it.id) }
                seletor = null
            },
            onDismiss = { seletor = null },
            mostrarBusca = tipo != TipoFiltro.PERIODO && tipo != TipoFiltro.GRANDEZA
        )
    }
}

@Composable
private fun TarifaFiltro(estado: RelatoriosUiState, periodo: PeriodoRelatorio) {
    var expandida by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(20.dp), color = ToneBlueBg) {
            Text(
                if (estado.demonstracao) "Tarifa simulada: ${moedaRelatorio(estado.tarifa)}/kWh" else "Tarifa indisponível",
                Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = ToneBlueText, style = MaterialTheme.typography.labelMedium
            )
        }
        IconButton(onClick = { expandida = !expandida }) {
            Icon(
                if (expandida) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = if (expandida) "Ocultar explicação da tarifa" else "Mostrar explicação da tarifa",
                tint = ToneBlueText
            )
        }
    }
    if (expandida) {
        Text(
            buildAnnotatedString {
                append("O custo da demonstração usa os deltas válidos do contador e uma tarifa de exemplo. O recorte considera ")
                withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.SemiBold)) {
                    append(periodo.titulo.lowercase(ptRelatorio))
                }
                append(" e os filtros de local, setor e equipamento.")
            },
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
