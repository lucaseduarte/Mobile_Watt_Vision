package com.example.watt_vision_mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

@Composable
@ReadOnlyComposable
internal fun corTema(escuro: Long, claro: Long): Color =
    Color(if (LocalTemaClaro.current) claro else escuro)

val TextOnAccent = Color.White

val DarkBackground: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF070C15, 0xFFEEF3F8)

val InputBackground: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF0D1829, 0xFFF1F6FC)

val CardBackground: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF0C141F, 0xFFFFFFFF)

val CardBorder: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF1B2A40, 0xFFCBD8E7)

val SurfaceDark: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF0D151F, 0xFFF8FBFF)

val PrimaryBlue: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF2B66C6, 0xFF2456A6)

val TextWhite: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFFFFFFFF, 0xFF111827)

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF6B7B94, 0xFF475569)

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF94A3B8, 0xFF334155)

val TextSubtle: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFFAFB5BA, 0xFF475569)

val LinkBlue: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF3B82F6, 0xFF1D4ED8)

val Slate800: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF1E293B, 0xFFDBE6F2)

val Slate900: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF0F172A, 0xFFFFFFFF)

val ToneBlueText: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF78B5FF, 0xFF1D4E91)

val ToneBlueBg: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF17315F, 0xFFE5EEFF)

val ToneGreenText: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF42C879, 0xFF166534)

val ToneGreenBg: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF103324, 0xFFDCFCE7)

val ToneRedText: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFFE47D8B, 0xFF991B1B)

val ToneRedBg: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF351B25, 0xFFFEE2E2)

val ToneYellowText: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFFD5B86C, 0xFF854D0E)

val ToneYellowBg: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF49391B, 0xFFFEF3C7)

val ToneCyanText: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF22D3EE, 0xFF155E75)

val ToneCyanBg: Color
    @Composable @ReadOnlyComposable get() = corTema(0xFF164E63, 0xFFCFFAFE)
