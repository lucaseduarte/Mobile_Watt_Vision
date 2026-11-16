package com.example.watt_vision_mobile.ui.components

import androidx.compose.runtime.Composable

@Suppress("UNUSED_PARAMETER")
@Composable
fun DevelopmentLoginReturn(onReturn: () -> Unit) = Unit

/** Release não contém controles de desenvolvimento ou demonstração. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DevelopmentModeBar(
    selectedRoute: String,
    demoSelected: Boolean,
    onDemoChanged: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onRouteSelected: (String) -> Unit
) = Unit
