package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Consulta demonstrativa; integração das listas e persistência pessoal ficam para suas etapas. */
class ConfiguracoesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConfiguracoesUiState())
    val uiState: StateFlow<ConfiguracoesUiState> = _uiState.asStateFlow()

    fun setActiveTab(tabIndex: Int) {
        require(tabIndex in 0..3)
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun updateTema(tema: String) {
        require(tema in listOf("Claro", "Escuro"))
        _uiState.update { it.copy(preferencias = it.preferencias.copy(tema = tema)) }
    }
}
