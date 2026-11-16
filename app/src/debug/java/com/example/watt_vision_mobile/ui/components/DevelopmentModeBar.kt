package com.example.watt_vision_mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DevelopmentLoginReturn(onReturn: () -> Unit) {
    TextButton(onClick = onReturn, modifier = Modifier.statusBarsPadding()) {
        Text("Voltar ao desenvolvimento sem login")
    }
}

@Composable
fun DevelopmentModeBar(
    selectedRoute: String,
    demoSelected: Boolean,
    onDemoChanged: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onRouteSelected: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Desenvolvimento · Debug", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onLoginClick) { Text("Entrar na API") }
                Box {
                    TextButton(onClick = { menuOpen = true }) { Text("Módulos") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        wattVisionNavigationItems.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(if (selectedRoute == item.title) "${item.title} •" else item.title) },
                                onClick = { menuOpen = false; onRouteSelected(item.title) }
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Liberar telas de demonstração", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Switch(checked = demoSelected, onCheckedChange = onDemoChanged)
            }
            Text(
                if (selectedRoute == "Operacional" || selectedRoute == "Configurações") {
                    if (demoSelected) "DEMONSTRAÇÃO · Dados simulados, sem conexão com a API."
                    else "Este módulo ainda não possui consulta real integrada."
                } else "Dashboard consulta a API. O acesso Debug não cria uma sessão no servidor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
