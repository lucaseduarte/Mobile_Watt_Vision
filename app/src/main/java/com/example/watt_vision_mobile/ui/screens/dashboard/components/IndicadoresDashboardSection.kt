package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.ui.screens.dashboard.IndicadorDashboard
import java.util.Locale

private val icones = mapOf(
    "lucide_activity" to "M22 12h-4l-3 9L9 3l-3 9H2",
    "lucide_check" to "M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4 12 14.01l-3-3",
    "lucide_wifi_off" to "m2 2 20 20 M8.53 16.11a6 6 0 0 1 6.95 0 M5 12.55a11 11 0 0 1 5.17-2.39 M19 12.55a11 11 0 0 0-1.97-1.18 M2 8.82a16 16 0 0 1 4.19-2.65 M22 8.82A16 16 0 0 0 10.34 5.1 M12 20h.01",
    "lucide_zap" to "M13 2 3 14h9l-1 8 10-12h-9l1-8z",
    "lucide_trend" to "m22 7-8.5 8.5-5-5L2 17 M16 7h6v6",
    "lucide_clock" to "M12 6v6h4 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
    "lucide_dollar" to "M12 2v20 M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
    "lucide_percent" to "m19 5-14 14 M9 7a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z M19 17a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z"
).mapValues { (nome, caminho) ->
    ImageVector.Builder(nome, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString(caminho).toNodes(),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }.build()
}

internal fun iconeDashboard(nome: String): ImageVector? = icones[nome]

@Composable
internal fun IndicadoresDashboardSection(
    grandeza: String,
    periodo: String,
    indicadores: List<IndicadorDashboard>
) {
    val forma = RoundedCornerShape(16.dp)
    val cores = listOf(
        corTema(0xFF3B82F6, 0xFF1D4ED8), corTema(0xFF10B981, 0xFF166534), corTema(0xFFFBBF24, 0xFF854D0E),
        corTema(0xFF22D3EE, 0xFF155E75), corTema(0xFF22C55E, 0xFF166534), corTema(0xFF22C55E, 0xFF166534)
    )
    Column(
        Modifier.fillMaxWidth().background(corTema(0xFF0D151F, 0xFFFFFFFF), forma)
            .border(1.dp, corTema(0xFF334155, 0xFFCBD8E7), forma).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Indicadores de ${grandeza.lowercase(Locale.forLanguageTag("pt-BR"))}",
                Modifier.weight(1f), color = corTema(0xFFE2E8F0, 0xFF111827),
                style = MaterialTheme.typography.titleSmall
            )
            Text("Período: $periodo", color = corTema(0xFF94A3B8, 0xFF475569), style = MaterialTheme.typography.bodySmall)
        }
        indicadores.withIndex().chunked(2).forEach { linha ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { (indice, indicador) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        iconeDashboard(indicador.icone)?.let {
                            Icon(it, null, Modifier.size(18.dp), tint = cores[indice])
                        }
                        Text(indicador.titulo, color = corTema(0xFF94A3B8, 0xFF475569), style = MaterialTheme.typography.labelMedium)
                        Text(indicador.valor, color = corTema(0xFFE2E8F0, 0xFF111827), style = MaterialTheme.typography.titleMedium)
                        Text(indicador.detalhe, color = corTema(0xFF94A3B8, 0xFF475569), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
