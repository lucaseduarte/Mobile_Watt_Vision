package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.ChartPointDto
import com.example.watt_vision_mobile.data.api.DispositivoConsultaDto
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.data.api.integracaoLocal
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.theme.*
import kotlinx.coroutines.CancellationException

@Composable
internal fun BotaoPontosMedicao(modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, corTema(0xFF3768C5, 0xFF8CAED6)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = corTema(0xFF101E36, 0xFFE5EEFF),
            contentColor = corTema(0xFF8EB5FF, 0xFF1E40AF)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pontos de Medição", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PontosMedicaoSheet(
    catalogo: ChartCatalog?,
    falhou: Boolean,
    atualizar: () -> Unit,
    onFechar: () -> Unit
) {
    val contexto = LocalContext.current.applicationContext
    val dispositivos by produceState<Map<Long, DispositivoConsultaDto>>(emptyMap(), contexto, catalogo) {
        if (!integracaoLocal || catalogo?.pontos.isNullOrEmpty()) return@produceState
        value = try {
            val resposta = ApiClient.createService<WattVisionApiService>(contexto).getDispositivos()
            resposta.body()?.takeIf { resposta.isSuccessful && it.sucesso }?.dados.orEmpty().associateBy { it.id }
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (_: Exception) {
            emptyMap()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        contentColor = TextWhite
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pontos de Medição", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onFechar) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar pontos de medição", tint = TextMuted)
                }
            }
            HorizontalDivider(color = CardBorder)
            when {
                !integracaoLocal -> Text("Consulta de pontos indisponível.", Modifier.padding(16.dp), color = TextMuted)
                falhou -> Column(Modifier.padding(16.dp)) {
                    Text("Não foi possível consultar os pontos de medição.", color = TextMuted)
                    TextButton(onClick = atualizar) { Text("Tentar novamente") }
                }
                catalogo == null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                catalogo.pontos.isEmpty() -> Text("Nenhum ponto de medição cadastrado.", Modifier.padding(16.dp), color = TextMuted)
                else -> ListaPontosMedicao(catalogo, dispositivos)
            }
        }
    }
}

@Composable
private fun ListaPontosMedicao(catalogo: ChartCatalog, dispositivos: Map<Long, DispositivoConsultaDto>) {
    val sensores = remember(catalogo) { catalogo.sensores.groupBy { it.id_ponto_medicao } }
    val ativos = catalogo.pontos.count { it.status.equals("Ativo", ignoreCase = true) }
    val inativos = catalogo.pontos.count { it.status.equals("Inativo", ignoreCase = true) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("${catalogo.pontos.size} pontos cadastrados · $ativos ativos · $inativos inativos",
                color = TextMuted, fontSize = 12.sp)
        }
        items(catalogo.pontos) { ponto ->
            val dispositivo = ponto.id_dispositivo_iot?.let { id ->
                dispositivos[id]?.let { "${it.nome} · ${it.identificador}" } ?: "Dispositivo #$id"
            }
            val modelos = sensores[ponto.id].orEmpty().mapNotNull { it.modelo?.takeIf(String::isNotBlank) }.distinct()
            CartaoPonto(
                ponto,
                vinculoPonto(catalogo, ponto),
                dispositivo ?: "Dispositivo não informado",
                modelos.takeIf { it.isNotEmpty() }?.joinToString() ?: "Modelo não informado"
            )
        }
    }
}

private fun vinculoPonto(catalogo: ChartCatalog, ponto: ChartPointDto): String = when {
    ponto.id_equipamento != null -> "Equipamento · " +
        (catalogo.equipamentos.firstOrNull { it.id == ponto.id_equipamento }?.nome ?: "#${ponto.id_equipamento}")
    ponto.id_setor != null -> "Setor · " +
        (catalogo.setores.firstOrNull { it.id == ponto.id_setor }?.nome ?: "#${ponto.id_setor}")
    ponto.id_local != null -> "Local · " +
        (catalogo.locais.firstOrNull { it.id == ponto.id_local }?.nome ?: "#${ponto.id_local}")
    else -> "Sem vínculo cadastrado"
}

@Composable
private fun CartaoPonto(ponto: ChartPointDto, vinculo: String, dispositivo: String, modelo: String) {
    val status = ponto.status?.takeIf(String::isNotBlank) ?: "Status não informado"
    val (fundo, texto) = when {
        status.equals("Ativo", ignoreCase = true) -> ToneGreenBg to ToneGreenText
        status.equals("Inativo", ignoreCase = true) -> ToneRedBg to ToneRedText
        else -> InputBackground to TextMuted
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ponto.nome?.takeIf(String::isNotBlank) ?: "Ponto de medição", fontSize = 14.sp,
                fontWeight = FontWeight.Bold, color = TextWhite)
            Text(vinculo, fontSize = 12.sp, color = TextSecondary)
            Text(dispositivo, fontSize = 12.sp, color = TextMuted)
            Text("PZEM: $modelo", fontSize = 12.sp, color = TextMuted)
            Surface(shape = RoundedCornerShape(12.dp), color = fundo) {
                Text(status, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = texto, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
