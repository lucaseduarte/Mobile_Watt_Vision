package com.example.watt_vision_mobile.ui.screens.alarmes

import android.app.Application
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.watt_vision_mobile.ProtectedNavigation
import com.example.watt_vision_mobile.model.UsuarioDto
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme
import org.junit.Rule
import org.junit.Test

class AlarmeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun opensAlarmListWithoutDemonstrationControls() {
        compose.setContent {
            val application = LocalContext.current.applicationContext as Application
            val user = UsuarioDto(1, 2, "Pessoa Fictícia", "teste@example.invalid", "Gestor", "Ativo")
            WattVisionTheme { ProtectedNavigation(application, user, onLogout = {}) }
        }
        compose.onNodeWithContentDescription("Alarmes recentes").performClick()
        compose.onNodeWithText("Lista de alarmes").assertExists()
        compose.onNodeWithText("Ver demonstração de Alarmes").assertDoesNotExist()
        compose.onNodeWithText("Equipamento de exemplo 1", substring = true).assertDoesNotExist()
    }
}
