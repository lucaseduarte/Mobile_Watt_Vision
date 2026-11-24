package com.example.watt_vision_mobile.ui.screens.alarmes.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.alarmes.AlarmesCounts
import com.example.watt_vision_mobile.ui.screens.dashboard.components.CardComSombreamentoDashboard
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun AlarmesIndicators(counts: AlarmesCounts?) {
    BoxWithConstraints {
        val medir = rememberTextMeasurer()
        val larguraTexto = (maxWidth - 10.dp) / 2 - 28.dp
        val empilhar = with(LocalDensity.current) {
            medir.measure("acompanhamento", TextStyle(fontSize = 10.sp), softWrap = false)
                .size.width > larguraTexto.toPx()
        }
        val ativos: @Composable (Modifier) -> Unit = {
            AlarmesIndicator("Alarmes Ativos", counts?.ativos, "Ativos e reconhecidos", Icons.Default.Notifications, ToneBlueText, it)
        }
        val criticos: @Composable (Modifier) -> Unit = {
            AlarmesIndicator("Críticos", counts?.criticos, "Requerem ação imediata", Icons.Default.Error, ToneRedText, it)
        }
        val atencao: @Composable (Modifier) -> Unit = {
            AlarmesIndicator("Atenção", counts?.atencao, "Alarmes em acompanhamento", Icons.Default.Warning, ToneYellowText, it)
        }
        val resolvidos: @Composable (Modifier) -> Unit = {
            AlarmesIndicator("Resolvidos", counts?.resolvidos, "Total recebido da API", Icons.Default.CheckCircle, ToneGreenText, it)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (empilhar) {
                listOf(ativos, criticos, atencao, resolvidos).forEach { it(Modifier.fillMaxWidth()) }
            } else {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ativos(Modifier.weight(1f).fillMaxHeight())
                    criticos(Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    atencao(Modifier.weight(1f).fillMaxHeight())
                    resolvidos(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun AlarmesIndicator(label: String, value: Int?, detail: String, icon: ImageVector, tone: Color, modifier: Modifier) {
    CardComSombreamentoDashboard(modifier = modifier, fundo = Color(0xFF111827), borda = Color(0xFF1E293B)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AlarmeIcon(icon, tone)
            Text(value?.toString() ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Text(detail, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
internal fun AlarmeIcon(icon: ImageVector, tone: Color) {
    Surface(
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = tone.copy(alpha = .10f),
        border = BorderStroke(1.dp, tone.copy(alpha = .35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(20.dp))
        }
    }
}
