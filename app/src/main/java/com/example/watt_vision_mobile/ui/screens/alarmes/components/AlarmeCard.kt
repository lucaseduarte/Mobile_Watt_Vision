package com.example.watt_vision_mobile.ui.screens.alarmes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.alarmes.*
import com.example.watt_vision_mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Locale

private fun severityColor(severity: AlarmeSeveridade) = when (severity) {
    AlarmeSeveridade.CRITICO -> ToneRedText
    AlarmeSeveridade.ATENCAO -> ToneYellowText
    AlarmeSeveridade.INFORMATIVO -> ToneCyanText
    AlarmeSeveridade.DESCONHECIDA -> TextSubtle
}

@Composable
fun AlarmeCard(alarme: Alarme, onClick: () -> Unit) {
    val resumo = resumoAlarme(alarme)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val (icon, tone) = when (alarme.severidade) {
                    AlarmeSeveridade.CRITICO -> Icons.Default.Error to ToneRedText
                    AlarmeSeveridade.ATENCAO -> Icons.Default.Warning to ToneYellowText
                    else -> Icons.Default.Info to ToneBlueText
                }
                AlarmeIcon(icon, tone)
                Text(alarme.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ChevronRight, "Ver detalhes do alarme", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
            resumo?.let { Text(it, color = TextSubtle, fontSize = 12.sp) }
            AlarmesBadges(alarme)
            Text(
                alarme.nomeEquipamento ?: alarme.dispositivoIotId?.let { "Dispositivo IoT #$it" }
                    ?: "Equipamento não informado",
                color = TextSubtle, fontSize = 12.sp
            )
            Text(AlarmeDate.format(alarme.dataHora), color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun resumoAlarme(alarme: Alarme): String? {
    val mensagem = alarme.mensagem?.takeIf { it.isNotBlank() }
    if (alarme.titulo.trim().equals("Dispositivo offline", ignoreCase = true) &&
        (alarme.status == AlarmeStatus.ATIVO || alarme.status == AlarmeStatus.RECONHECIDO)) {
        val inicio = alarme.dataHora?.trim()?.replace(' ', 'T')
        val elapsed by produceState(tempoOffline(inicio), inicio) {
            value = tempoOffline(inicio)
            while (value != null) {
                delay(30_000)
                value = tempoOffline(inicio)
            }
        }
        return elapsed ?: mensagem
    }
    if (alarme.valorDetectado != null || alarme.valorLimite != null ||
        alarme.titulo.trim().equals("Limite excedido", ignoreCase = true)) {
        val decimal = remember {
            NumberFormat.getNumberInstance(Locale.ROOT).apply {
                isGroupingUsed = false
                minimumFractionDigits = 3
                maximumFractionDigits = 3
            }
        }
        val detectado = alarme.valorDetectado?.let(decimal::format) ?: "—"
        val limite = alarme.valorLimite?.let(decimal::format) ?: "—"
        return "Valor detectado: $detectado (Limite: $limite)"
    }
    return mensagem
}

private fun tempoOffline(inicio: String?): String? {
    if (inicio.isNullOrBlank()) return null
    val duracao = runCatching { Duration.between(OffsetDateTime.parse(inicio), OffsetDateTime.now()) }
        .recoverCatching { Duration.between(LocalDateTime.parse(inicio), LocalDateTime.now()) }
        .getOrNull() ?: return null
    val minutos = duracao.toMinutes().coerceAtLeast(0)
    return when {
        minutos < 1 -> "Dispositivo offline há menos de 1 min"
        minutos < 60 -> "Dispositivo offline há ${minutos} min"
        minutos < 1440 -> "Dispositivo offline há ${minutos / 60} h ${minutos % 60} min"
        else -> "Dispositivo offline há ${minutos / 1440} d ${minutos / 60 % 24} h"
    }
}

private fun statusColor(status: AlarmeStatus) = when (status) {
    AlarmeStatus.ATIVO -> ToneBlueText
    AlarmeStatus.RECONHECIDO, AlarmeStatus.RESOLVIDO -> ToneGreenText
    AlarmeStatus.DESCONHECIDO -> TextSubtle
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmesBadges(alarme: Alarme) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AlarmeBadge(alarme.severidadeLabel, severityColor(alarme.severidade))
        AlarmeBadge(alarme.statusLabel, statusColor(alarme.status))
    }
}

@Composable
private fun AlarmeBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = .10f), border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
        Text(label, fontSize = 10.sp, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
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

    val condicao = resumoAlarme(alarme)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var abrindoEntidade by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceDark) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Detalhes do alarme", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fechar detalhes", tint = TextMuted) }
            }
            AlarmesBadges(alarme)
            Text(alarme.titulo, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

            val alvo = entidade
            when {
                carregando -> Text("Carregando vínculo operacional…", color = TextMuted, fontSize = 12.sp)
                alvo != null -> {
                    AlarmeField(alvo.tipo.label, alvo.nome)
                    if (alvo.tipo == AlarmeTipoEntidade.EQUIPAMENTO) AlarmeField("Setor", alvo.setor)
                    if (alvo.tipo != AlarmeTipoEntidade.LOCAL) AlarmeField("Local", alvo.local)
                }
                else -> AlarmeField("Vínculo operacional", null)
            }

            AlarmeField("Condição", condicao)
            if (alvo != null) {
                Button(
                    onClick = {
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
                    enabled = !abrindoEntidade,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = TextWhite),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Ver ${alvo.tipo.label.lowercase(Locale.ROOT)}")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlarmeField(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = TextMuted)
        Text(value?.takeIf { it.isNotBlank() } ?: "Não informado", color = TextSubtle, fontSize = 13.sp)
    }
}
