package com.example.watt_vision_mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.theme.CardBorder
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import com.example.watt_vision_mobile.ui.theme.SurfaceDark
import com.example.watt_vision_mobile.ui.theme.TextMuted
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme

private val HeaderMargin = 16.dp
private val HeaderVerticalPadding = 12.dp
private val HeaderTitleStyle = TextStyle(
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold
)
private val HeaderSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal
)

@Composable
internal fun WattVisionScreenHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = HeaderMargin, vertical = HeaderVerticalPadding),
    leadingAction: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier.fillMaxWidth().padding(contentPadding)
    ) {
        // Android scales text nonlinearly. Estimate the text width at the actual title size.
        val titleWidth = with(LocalDensity.current) {
            176.dp * (HeaderTitleStyle.fontSize.toDp() / 22.dp)
        }
        val actionsFitInline = maxWidth >= titleWidth + 152.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                leadingAction?.invoke()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, Modifier.semantics { heading() }, style = HeaderTitleStyle, color = corTema(0xFFF1F5F9, 0xFF111827))
                    if (subtitle != null) Text(subtitle, style = HeaderSubtitleStyle, color = corTema(0xFF94A3B8, 0xFF475569))
                }
                if (actions != null && actionsFitInline) Row(content = actions)
            }
            if (actions != null && !actionsFitInline) {
                Row(Modifier.align(Alignment.End), content = actions)
            }
        }
    }
}

@Composable
internal fun WattVisionHeaderShortcut(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, description, Modifier.size(18.dp), tint = TextMuted)
            }
        }
    }
}

@Preview(widthDp = 360, fontScale = 2f)
@Preview(widthDp = 360)
@Composable
private fun ScreenHeaderPreview() {
    WattVisionTheme {
        Surface(color = DarkBackground) {
            WattVisionScreenHeader("Dashboard", "Monitoramento Energético em Tempo Real") {
                WattVisionHeaderShortcut(Icons.Filled.Notifications, "Alarmes recentes") {}
                WattVisionHeaderShortcut(Icons.Filled.Settings, "Configurações do sistema") {}
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun DetailHeaderPreview() {
    WattVisionTheme {
        Surface(color = DarkBackground) {
            WattVisionScreenHeader(
                title = "Planta Industrial A",
                subtitle = "Detalhes do local",
                leadingAction = {
                    IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    }
}
