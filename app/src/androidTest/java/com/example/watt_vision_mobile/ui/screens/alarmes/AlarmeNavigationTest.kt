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

    @Test fun opensEquipmentInOperacionalAndReturnsToAlarmList() {
        compose.setContent {
            val application = LocalContext.current.applicationContext as Application
            val user = UsuarioDto(1, 2, "Pessoa Fictícia", "teste@example.invalid", "Gestor", "Ativo")
            WattVisionTheme { ProtectedNavigation(application, user, onLogout = {}) }
        }
        compose.onNodeWithContentDescription("Alarmes recentes").performClick()
        compose.onNodeWithText("Ver demonstração de Alarmes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Equipamento de exemplo 1", substring = true))
        compose.onNodeWithText("Equipamento de exemplo 1", substring = true).performClick()
        compose.onNodeWithText("Ver Equipamento").performScrollTo().performClick()

        compose.onNodeWithContentDescription("Voltar aos alarmes").assertIsDisplayed()
        compose.onNodeWithText("Identificação").assertIsDisplayed()
        compose.onNodeWithText("Local de exemplo  ·  Setor de exemplo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar aos alarmes").performClick()
        compose.onNodeWithText("Voltar aos dados da API").assertIsDisplayed()
    }
}
