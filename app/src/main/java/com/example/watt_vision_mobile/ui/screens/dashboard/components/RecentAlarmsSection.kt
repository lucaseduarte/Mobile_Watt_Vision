package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.AlarmData
import com.example.watt_vision_mobile.ui.screens.dashboard.AlarmType
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun RecentAlarmsSection(
    alarms: List<AlarmData>,
    modifier: Modifier = Modifier,
    unavailable: Boolean = false,
    total: Int? = null,
    onSeeAllClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabeçalho da seção com Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarmes Recentes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ToneYellowBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (unavailable) "Indisponível" else "${total ?: alarms.size} ativos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ToneYellowText
                    )
                }
            }

            Text("Até cinco mais recentes · dispositivos do escopo, sem filtro de período. Alarmes sem vínculo de dispositivo não entram nesta consulta.", color = TextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // Lista de alarmes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (alarms.isEmpty()) {
                    Text(
                        if (unavailable) "Não foi possível consultar os alarmes." else "Nenhum alarme ativo com vínculo aos dispositivos deste escopo.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                alarms.forEach { alarm ->
                    AlarmItemRow(alarm = alarm)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão "Ver todos"
            Button(
                onClick = onSeeAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue.copy(alpha = 0.2f),
                    contentColor = ToneBlueText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ver todos",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmItemRow(alarm: AlarmData) {
    val (icon, iconColor, iconBg) = when (alarm.type) {
        AlarmType.CRITICAL -> Triple(Icons.Default.Error, ToneRedText, ToneRedBg)
        AlarmType.WARNING -> Triple(Icons.Default.Warning, ToneYellowText, ToneYellowBg)
        AlarmType.INFO -> Triple(Icons.Default.Info, ToneCyanText, ToneCyanBg)
        AlarmType.UNKNOWN -> Triple(Icons.Default.Info, TextMuted, InputBackground)
        AlarmType.SUCCESS -> Triple(Icons.Default.CheckCircle, ToneGreenText, ToneGreenBg)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alarm.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = alarm.time,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Text(
                text = "${alarm.equipment}\nSeveridade: ${alarm.severity} · Status: ${alarm.status}",
                fontSize = 11.sp,
                color = TextSubtle,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
