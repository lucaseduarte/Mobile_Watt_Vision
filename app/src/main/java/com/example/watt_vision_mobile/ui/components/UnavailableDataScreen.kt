package com.example.watt_vision_mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UnavailableDataScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Consulta à API ainda não integrada neste módulo. " +
                "Nenhum dado de demonstração foi carregado.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
