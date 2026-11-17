package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConfiguracoesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracoesUiState())
    val uiState: StateFlow<ConfiguracoesUiState> = _uiState.asStateFlow()

    // Somente preferências pessoais em memória; dados corporativos não são alteráveis.
    private var savedTema = _uiState.value.geral.tema

    fun setActiveTab(tabIndex: Int) { _uiState.update { it.copy(activeTab = tabIndex) } }

    fun salvarPreferenciasLocais() {
        savedTema = _uiState.value.geral.tema
    }
    fun cancelarPreferenciasLocais() {
        _uiState.update { it.copy(geral = it.geral.copy(tema = savedTema)) }
    }

    fun updateTema(tema: String) { _uiState.update { it.copy(geral = it.geral.copy(tema = tema)) } }
}
