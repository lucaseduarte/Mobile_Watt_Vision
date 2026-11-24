package com.example.watt_vision_mobile.ui.screens.alarmes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.alarmes.*
import com.example.watt_vision_mobile.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val fundoAlarme = Color(0xFF111827)
private val bordaAlarme = Color(0xFF1E293B)
private val textoAlarme = Color(0xFFF1F5F9)
private val detalheAlarme = Color(0xFF94A3B8)
private val legendaAlarme = Color(0xFF64748B)

@Composable
fun AlarmeCard(alarme: Alarme, onClick: () -> Unit) {
    val resumo = when {
        alarme.valorDetectado != null || alarme.valorLimite != null -> resumoAlarme(alarme)
        alarme.titulo.trim().equals("Dispositivo offline", ignoreCase = true) ->
            "Sem comunicação com o dispositivo"
        else -> alarme.mensagem?.takeIf { it.isNotBlank() }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = fundoAlarme,
        border = BorderStroke(1.dp, bordaAlarme)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Ver detalhes do alarme", onClick = onClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AlarmesBadges(alarme)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(alarme.titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textoAlarme)
                resumo?.let { Text(it, fontSize = 13.sp, color = detalheAlarme) }
            }
            HorizontalDivider(color = bordaAlarme)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val origem = alarme.nomeEquipamento ?: alarme.dispositivoIotId?.let { "Dispositivo IoT #$it" }
                    ?: "Origem não informada"
                Text("$origem · Local não informado", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1))
                Text(AlarmeDate.formatarInicio(alarme.dataHora), fontSize = 12.sp, color = legendaAlarme)
            }
        }
    }
}

@Composable
private fun resumoAlarme(alarme: Alarme): String? {
    if (alarme.valorDetectado == null && alarme.valorLimite == null) return null
    val decimal = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            isGroupingUsed = false
            minimumFractionDigits = 3
            maximumFractionDigits = 3
        }
    }
    val detectado = alarme.valorDetectado?.let(decimal::format) ?: "—"
    val limite = alarme.valorLimite?.let(decimal::format) ?: "—"
    return "Valor detectado: $detectado (Limite: $limite)"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmesBadges(alarme: Alarme, detalhes: Boolean = false) {
    val (fundoSeveridade, textoSeveridade) = when (alarme.severidade) {
        AlarmeSeveridade.CRITICO -> Color(0xFF3B1219) to Color(0xFFF87171)
        AlarmeSeveridade.ATENCAO -> Color(0xFF33250B) to Color(0xFFFBBF24)
        AlarmeSeveridade.INFORMATIVO -> ToneCyanBg to ToneCyanText
        AlarmeSeveridade.DESCONHECIDA -> InputBackground to TextSubtle
    }
    val (fundoStatus, textoStatus) = when (alarme.status) {
        AlarmeStatus.ATIVO -> if (detalhes) bordaAlarme to Color(0xFFCBD5E1) else Color(0xFF1D4ED8) to Color(0xFF93C5FD)
        AlarmeStatus.RECONHECIDO, AlarmeStatus.RESOLVIDO -> Color(0xFF065F46) to Color(0xFF6EE7B7)
        AlarmeStatus.DESCONHECIDO -> InputBackground to TextSubtle
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AlarmeBadge("● ${alarme.severidadeLabel}", fundoSeveridade, textoSeveridade)
            if (!detalhes) when (alarme.categoria) {
                AlarmeCategoria.ENERGETICO -> AlarmeBadge(
                    alarme.categoriaLabel, Color(0xFF1E3A8A), Color(0xFF60A5FA), icone = Icons.Filled.Bolt
                )
                AlarmeCategoria.COMUNICACAO -> AlarmeBadge(
                    alarme.categoriaLabel, Color(0xFF064E3B), Color(0xFF34D399), icone = Icons.Filled.Wifi
                )
                AlarmeCategoria.DESCONHECIDA -> AlarmeBadge(alarme.categoriaLabel, InputBackground, TextSubtle)
            }
        }
        AlarmeBadge(alarme.statusLabel, fundoStatus, textoStatus, Modifier.widthIn(max = 116.dp))
    }
}

@Composable
private fun AlarmeBadge(
    label: String,
    fundo: Color,
    cor: Color,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = fundo) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icone != null) Icon(icone, contentDescription = null, modifier = Modifier.size(12.dp), tint = cor)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = cor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmeDetailsSheet(
    alarme: Alarme,
    carregarEntidade: suspend (Alarme) -> AlarmeEntidade?,
    onEntidadeClick: (AlarmeEntidade) -> Unit,
    onDismiss: () -> Unit
) {
    var entidade by remember(alarme.id, alarme.limiteId, alarme.dispositivoIotId) {
        mutableStateOf<AlarmeEntidade?>(null)
    }
    var carregando by remember(alarme.id, alarme.limiteId, alarme.dispositivoIotId) {
        mutableStateOf(true)
    }
    LaunchedEffect(alarme.id, alarme.limiteId, alarme.dispositivoIotId, carregarEntidade) {
        carregando = true
        entidade = carregarEntidade(alarme)
        carregando = false
    }

    val condicao = resumoAlarme(alarme) ?: "Sem comunicação com o dispositivo"
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var abrindoEntidade by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = fundoAlarme,
        contentColor = textoAlarme
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AlarmesBadges(alarme, detalhes = true)
            Text(alarme.titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textoAlarme)

            OrigemAlarme(alarme, entidade)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CONDIÇÃO IDENTIFICADA", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp, color = detalheAlarme
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF261217),
                    border = BorderStroke(1.dp, Color(0xFF5C1D24))
                ) {
                    Text(
                        condicao, Modifier.padding(14.dp), fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = Color(0xFFFCA5A5)
                    )
                }
            }

            if (alarme.reconhecidoPor != null || !alarme.dataReconhecimento.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    alarme.reconhecidoPor?.let {
                        Text("Reconhecido por: usuário #$it", fontSize = 12.sp, color = detalheAlarme)
                    }
                    if (!alarme.dataReconhecimento.isNullOrBlank()) {
                        Text(
                            "Reconhecido em: ${AlarmeDate.formatarInicio(alarme.dataReconhecimento)}",
                            fontSize = 12.sp, color = detalheAlarme
                        )
                    }
                }
            }

            HorizontalDivider(color = bordaAlarme)
            val equipamento = entidade?.takeIf { it.tipo == AlarmeTipoEntidade.EQUIPAMENTO && it.id > 0 }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val alvo = equipamento ?: return@OutlinedButton
                        abrindoEntidade = true
                        scope.launch {
                            try {
                                sheetState.hide()
                                if (!sheetState.isVisible) onEntidadeClick(alvo)
                            } finally {
                                abrindoEntidade = false
                            }
                        }
                    },
                    enabled = equipamento != null && !carregando && !abrindoEntidade,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, bordaAlarme),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Ver Equipamento")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                if (carregando || equipamento == null) {
                    Text(
                        if (carregando) "Consultando vínculo do equipamento…" else "Nenhum equipamento vinculado disponível.",
                        fontSize = 12.sp, color = detalheAlarme
                    )
                }
            }
        }
    }
}

@Composable
private fun OrigemAlarme(alarme: Alarme, entidade: AlarmeEntidade?) {
    val nome = alarme.nomeEquipamento ?: entidade?.nome
        ?: alarme.dispositivoIotId?.let { "Dispositivo IoT #$it" }
        ?: "Dispositivo não informado"
    val local = if (entidade?.tipo == AlarmeTipoEntidade.LOCAL) entidade.nome else entidade?.local
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B111E),
        border = BorderStroke(1.dp, bordaAlarme)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(nome, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textoAlarme)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null,
                    modifier = Modifier.size(16.dp), tint = Color(0xFFF87171))
                Text(local?.takeIf { it.isNotBlank() } ?: "Local não informado",
                    fontSize = 13.sp, color = detalheAlarme)
            }
        }
    }
}
