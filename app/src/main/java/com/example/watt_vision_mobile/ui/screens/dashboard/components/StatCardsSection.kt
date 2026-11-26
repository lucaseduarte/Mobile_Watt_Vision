package com.example.watt_vision_mobile.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.dashboard.StatCardData
import com.example.watt_vision_mobile.ui.screens.dashboard.StatTone
import com.example.watt_vision_mobile.ui.theme.*

@Composable
fun StatCardsSection(
    cards: List<StatCardData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Exibe os cards em duplas (2 por linha) no layout mobile
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowCards.forEach { cardData ->
                    Box(modifier = Modifier.weight(1f)) {
                        StatCardItem(card = cardData)
                    }
                }
                if (rowCards.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatCardItem(card: StatCardData) {
    val (iconBgBrush, _, badgeBg, badgeText) = when (card.tone) {
        StatTone.GREEN -> Quadruple(
            Brush.linearGradient(listOf(corTema(0xFF267447, 0xFF15803D), corTema(0xFF12372D, 0xFF15803D))),
            ToneGreenText,
            ToneGreenBg,
            ToneGreenText
        )
        StatTone.RED -> Quadruple(
            Brush.linearGradient(listOf(corTema(0xFF632632, 0xFFDC2626), corTema(0xFF2A1822, 0xFFDC2626))),
            ToneRedText,
            ToneRedBg,
            ToneRedText
        )
        StatTone.YELLOW -> Quadruple(
            Brush.linearGradient(listOf(corTema(0xFFA99A76, 0xFFA84C09), corTema(0xFF51462F, 0xFFA84C09))),
            ToneYellowText,
            ToneYellowBg,
            ToneYellowText
        )
        StatTone.BLUE -> Quadruple(
            Brush.linearGradient(listOf(corTema(0xFF315FAE, 0xFF2563EB), corTema(0xFF17315F, 0xFF2563EB))),
            ToneBlueText,
            ToneBlueBg,
            ToneBlueText
        )
    }

    val iconVector: ImageVector = iconeDashboard(card.iconName) ?: when (card.iconName) {
        "check_circle" -> Icons.Default.CheckCircle
        "wifi_off" -> Icons.Default.WifiOff
        "zap" -> Icons.Default.Bolt
        "flash" -> Icons.Default.FlashOn
        "activity" -> Icons.Default.Speed
        else -> Icons.Default.Memory
    }

    CardComSombreamentoDashboard(
        modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp)
    ) {
        // Camada 4: Conteúdo do Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícone com gradiente
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = corTema(0xFFD0D8E2, 0xFFFFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Badge de variação
                card.change?.let { changeText ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(0.5.dp, badgeText.copy(alpha = 0.4f), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = changeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Valor do indicador
            Text(
                text = card.value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            // Rótulo principal
            Text(
                text = card.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSubtle
            )

            Spacer(modifier = Modifier.weight(1f))

            // Detalhe secundário no rodapé
            Text(
                text = card.detail,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
internal fun CardComSombreamentoDashboard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    fundo: Color = corTema(0xFF0C141F, 0xFFFFFFFF),
    borda: Color = corTema(0xFF1B2A40, 0xFFCBD8E7),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.32f),
                ambientColor = Color.Black.copy(alpha = 0.32f)
            )
            .clip(shape)
            .background(fundo)
            .border(1.dp, borda, shape)
    ) {
        // Camada 1: Gradiente azul interno superior (rgba(36,73,130,0.13) -> rgba(15,27,44,0.05) -> transparent)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            corTema(0xFF244982, 0xFFDCEAFF).copy(alpha = 0.13f),
                            corTema(0xFF0F1B2C, 0xFFFFFFFF).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 140f
                    )
                )
        )

        // Camada 2: Reflexo azul difuso no canto superior esquerdo (#2856A0 com 7-14% opacidade)
        Box(
            modifier = Modifier
                .size(130.dp, 90.dp)
                .offset(x = (-32).dp, y = (-48).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            corTema(0xFF2856A0, 0xFFBFDBFE).copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Camada 3: Linha de brilho inferior com gradiente horizontal (#31558A com 30% opacidade no centro)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            corTema(0xFF31558A, 0xFFB9CEE9).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        content()
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
