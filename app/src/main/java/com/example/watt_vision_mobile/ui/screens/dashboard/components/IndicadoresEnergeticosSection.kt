package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val fundo = Color(0xFF0B111E)
private val borda = Color(0xFF1E293B)
private val texto = Color(0xFFF1F5F9)
private val rotulo = Color(0xFF94A3B8)
private val referencia = Color(0xFF64748B)
private val verde = Color(0xFF10B981)

private data class IndicadorEnergetico(
    val titulo: String,
    val icone: ImageVector,
    val cor: Color,
    val valor: String? = null,
    val detalhe: String? = null
)

private val indicadores = listOf(
    IndicadorEnergetico("Consumo Total", Icons.Outlined.Bolt, Color(0xFF3B82F6)),
    IndicadorEnergetico("Custo Estimado", Icons.Outlined.AttachMoney, verde),
    IndicadorEnergetico("Consumo Médio", Icons.Outlined.MonitorHeart, verde),
    IndicadorEnergetico("Pico de Consumo", Icons.AutoMirrored.Outlined.TrendingUp, Color(0xFFF59E0B)),
    IndicadorEnergetico("Maior Consumo", Icons.Outlined.Schedule, Color(0xFF06B6D4)),
    IndicadorEnergetico("Variação", Icons.Outlined.Percent, verde)
)

@Composable
fun IndicadoresEnergeticosSection(periodText: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111827),
        border = BorderStroke(1.dp, borda)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Indicadores Energéticos", Modifier.weight(1f),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = texto,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("Período: $periodText", fontSize = 11.sp, color = rotulo, maxLines = 1)
            }

            indicadores.chunked(2).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    linha.forEach { indicador ->
                        IndicadorMetricCard(indicador, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicadorMetricCard(indicador: IndicadorEnergetico, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = fundo,
        border = BorderStroke(1.dp, borda)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(indicador.icone, contentDescription = null, tint = indicador.cor, modifier = Modifier.size(16.dp))
                Text(
                    indicador.titulo, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = rotulo,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                indicador.valor ?: "—", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = texto,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                if (indicador.valor == null) "Dados indisponíveis" else indicador.detalhe.orEmpty(),
                fontSize = 11.sp, color = referencia, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
