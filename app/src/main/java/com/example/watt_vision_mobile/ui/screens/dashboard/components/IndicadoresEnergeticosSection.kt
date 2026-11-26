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
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val fundo
    @Composable get() = corTema(0xFF0B111E, 0xFFFFFFFF)
private val borda
    @Composable get() = corTema(0xFF1E293B, 0xFFCBD8E7)
private val texto
    @Composable get() = corTema(0xFFF1F5F9, 0xFF111827)
private val rotulo
    @Composable get() = corTema(0xFF94A3B8, 0xFF475569)
private val referencia
    @Composable get() = corTema(0xFF64748B, 0xFF475569)
private val verde
    @Composable get() = corTema(0xFF10B981, 0xFF166534)

private data class IndicadorEnergetico(
    val titulo: String,
    val icone: ImageVector,
    val cor: Color,
    val valor: String? = null,
    val detalhe: String? = null
)

private val indicadores
    @Composable get() = listOf(
    IndicadorEnergetico("Consumo Total", Icons.Outlined.Bolt, corTema(0xFF3B82F6, 0xFF1D4ED8)),
    IndicadorEnergetico("Custo Estimado", Icons.Outlined.AttachMoney, verde),
    IndicadorEnergetico("Consumo Médio", Icons.Outlined.MonitorHeart, verde),
    IndicadorEnergetico("Pico de Consumo", Icons.AutoMirrored.Outlined.TrendingUp, corTema(0xFFF59E0B, 0xFF854D0E)),
    IndicadorEnergetico("Maior Consumo", Icons.Outlined.Schedule, corTema(0xFF06B6D4, 0xFF155E75)),
    IndicadorEnergetico("Variação", Icons.Outlined.Percent, verde)
)

@Composable
fun IndicadoresEnergeticosSection(periodText: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = corTema(0xFF111827, 0xFFF8FBFF),
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
