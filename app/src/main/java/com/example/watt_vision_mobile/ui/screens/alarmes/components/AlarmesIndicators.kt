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
            AlarmesIndicator("Ativos", counts?.ativos, "Status ativo", ToneBlueText, Icons.Default.Notifications, Modifier.weight(1f).fillMaxHeight())
            AlarmesIndicator("Críticos", counts?.criticos, "Somente ativos", ToneRedText, Icons.Default.Error, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AlarmesIndicator("Atenção", counts?.atencao, "Somente ativos", ToneYellowText, Icons.Default.Warning, Modifier.weight(1f).fillMaxHeight())
            AlarmesIndicator("Reconhecidos", counts?.reconhecidos, "Status reconhecido", ToneGreenText, Icons.Default.CheckCircle, Modifier.weight(1f).fillMaxHeight())
        }
        Text("Indicadores da lista recebida, antes dos filtros de pesquisa.", fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun AlarmesIndicator(label: String, value: Int?, detail: String, tone: Color, icon: ImageVector, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = CardBackground, border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(22.dp))
            Text(value?.toString() ?: "—", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Text(detail, fontSize = 10.sp, color = TextMuted)
        }
    }
}
