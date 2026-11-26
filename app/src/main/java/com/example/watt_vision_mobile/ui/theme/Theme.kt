package com.example.watt_vision_mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalTemaClaro = staticCompositionLocalOf { false }
internal val LocalAlterarTema = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun WattVisionTheme(
    temaClaro: Boolean = false,
    alterarTema: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTemaClaro provides temaClaro, LocalAlterarTema provides alterarTema) {
        val esquema = if (temaClaro) {
            lightColorScheme(
                primary = PrimaryBlue,
                onPrimary = TextOnAccent,
                primaryContainer = ToneBlueBg,
                onPrimaryContainer = ToneBlueText,
                secondary = PrimaryBlue,
                onSecondary = TextOnAccent,
                secondaryContainer = ToneBlueBg,
                onSecondaryContainer = ToneBlueText,
                background = DarkBackground,
                onBackground = TextWhite,
                surface = SurfaceDark,
                onSurface = TextWhite,
                surfaceVariant = InputBackground,
                onSurfaceVariant = TextMuted,
                surfaceContainerLowest = CardBackground,
                surfaceContainerLow = SurfaceDark,
                surfaceContainer = CardBackground,
                surfaceContainerHigh = InputBackground,
                surfaceContainerHighest = ToneBlueBg,
                outline = TextMuted,
                outlineVariant = CardBorder,
                error = ToneRedText,
                onError = TextOnAccent,
                errorContainer = ToneRedBg,
                onErrorContainer = ToneRedText
            )
        } else {
            darkColorScheme(
                primary = PrimaryBlue,
                background = DarkBackground,
                surface = InputBackground,
                onPrimary = TextWhite,
                onBackground = TextWhite,
                onSurface = TextWhite
            )
        }
        MaterialTheme(colorScheme = esquema, typography = Typography, content = content)
    }
}
