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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.alarmes.AlarmesCounts
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun AlarmesIndicators(counts: AlarmesCounts?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AlarmesIndicator("Alarmes Ativos", counts?.ativos, "Ativos e reconhecidos", Icons.Default.Notifications, ToneBlueText, Modifier.weight(1f).fillMaxHeight())
            AlarmesIndicator("Críticos", counts?.criticos, "Requerem ação imediata", Icons.Default.Error, ToneRedText, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AlarmesIndicator("Atenção", counts?.atencao, "Alarmes em acompanhamento", Icons.Default.Warning, ToneYellowText, Modifier.weight(1f).fillMaxHeight())
            AlarmesIndicator("Resolvidos", counts?.resolvidos, "Total recebido da API", Icons.Default.CheckCircle, ToneGreenText, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun AlarmesIndicator(label: String, value: Int?, detail: String, icon: ImageVector, tone: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
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
