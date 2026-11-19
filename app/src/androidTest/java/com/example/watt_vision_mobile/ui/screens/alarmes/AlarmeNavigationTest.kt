package com.example.watt_vision_mobile.ui.screens.alarmes

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.watt_vision_mobile.MainActivity
import org.junit.Rule
import org.junit.Test

class AlarmeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun opensEquipmentInOperacionalAndReturnsToAlarmList() {
        compose.onNodeWithContentDescription("Alarmes recentes").performClick()
        compose.onNodeWithText("Ver demonstração de Alarmes").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Equipamento de exemplo 1"))
        compose.onNodeWithText("Equipamento de exemplo 1").performClick()
        compose.onNodeWithText("Ver equipamento").performScrollTo().performClick()

        compose.onNodeWithContentDescription("Voltar aos alarmes").assertIsDisplayed()
        compose.onNodeWithText("Identificação").assertIsDisplayed()
        compose.onNodeWithText("Local de exemplo  ·  Setor de exemplo").assertIsDisplayed()
        compose.onNodeWithContentDescription("Voltar aos alarmes").performClick()
        compose.onNodeWithText("Voltar aos dados da API").assertIsDisplayed()
    }
}
