package com.example.watt_vision_mobile.ui.screens.alarmes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.model.alarmes.*
import com.example.watt_vision_mobile.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private fun severityColor(severity: AlarmeSeveridade) = when (severity) {
    AlarmeSeveridade.CRITICO -> ToneRedText
    AlarmeSeveridade.ATENCAO -> ToneYellowText
    AlarmeSeveridade.INFORMATIVO -> ToneCyanText
    AlarmeSeveridade.DESCONHECIDA -> TextSubtle
}

@Composable
fun AlarmeCard(alarme: Alarme, onClick: () -> Unit) {
    val color = severityColor(alarme.severidade)
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(when (alarme.severidade) {
                    AlarmeSeveridade.CRITICO -> Icons.Default.Error
                    AlarmeSeveridade.ATENCAO -> Icons.Default.Warning
                    else -> Icons.Default.Info
                }, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(alarme.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ChevronRight, "Ver detalhes do alarme", tint = TextMuted, modifier = Modifier.size(20.dp))
            }
            Text(alarme.mensagem?.takeIf { it.isNotBlank() } ?: "Mensagem não informada", color = TextSubtle, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            AlarmesBadges(alarme)
            alarme.dispositivoIotId?.let { Text("Dispositivo IoT #$it", color = TextMuted, fontSize = 11.sp) }
            Text(AlarmeDate.format(alarme.dataHora), color = TextMuted, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmesBadges(alarme: Alarme) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AlarmeBadge(alarme.severidadeLabel, severityColor(alarme.severidade))
        AlarmeBadge(alarme.statusLabel, when (alarme.status) {
            AlarmeStatus.ATIVO -> ToneBlueText
            AlarmeStatus.RECONHECIDO -> ToneGreenText
            AlarmeStatus.DESCONHECIDO -> TextSubtle
        })
        AlarmeBadge(alarme.categoriaLabel, when (alarme.categoria) {
            AlarmeCategoria.ENERGETICO -> ToneBlueText
            AlarmeCategoria.COMUNICACAO -> ToneCyanText
            AlarmeCategoria.DESCONHECIDA -> TextSubtle
        })
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
fun AlarmeDetailsSheet(alarme: Alarme, onDismiss: () -> Unit) {
    val number = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply { maximumFractionDigits = 3 } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceDark) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Detalhes do alarme", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Fechar detalhes", tint = TextMuted) }
            }
            Text(alarme.titulo, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            AlarmesBadges(alarme)
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AlarmeField("Mensagem", alarme.mensagem)
                    AlarmeField("Data e horário", AlarmeDate.format(alarme.dataHora))
                    AlarmeField("Valor detectado", alarme.valorDetectado?.let(number::format))
                    AlarmeField("Valor limite", alarme.valorLimite?.let(number::format))
                    Text("A unidade dos valores não é informada no contrato do alerta.", fontSize = 11.sp, color = TextMuted)
                    HorizontalDivider(color = CardBorder)
                    AlarmeField("Identificador do alarme", alarme.id?.toString())
                    AlarmeField("Tipo de alerta — ID", alarme.tipoId?.toString())
                    AlarmeField("Dispositivo IoT — ID", alarme.dispositivoIotId?.toString())
                    AlarmeField("Medição — ID", alarme.medicaoId?.toString())
                    AlarmeField("Limite — ID", alarme.limiteId?.toString())
                    AlarmeField("Reconhecido por — ID do usuário", alarme.reconhecidoPor?.toString())
                    AlarmeField("Data de reconhecimento", AlarmeDate.format(alarme.dataReconhecimento))
                    Text("Horários sem fuso são preservados conforme recebidos.", fontSize = 11.sp, color = TextMuted)
                }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Fechar") }
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
