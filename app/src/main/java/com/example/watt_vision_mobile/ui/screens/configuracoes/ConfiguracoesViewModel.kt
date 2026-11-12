package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConfiguracoesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracoesUiState())
    val uiState: StateFlow<ConfiguracoesUiState> = _uiState.asStateFlow()

    private var savedGeral = ConfiguracoesGeralState()

    fun setActiveTab(tabIndex: Int) { _uiState.update { it.copy(activeTab = tabIndex) } }

    fun salvarAlteracoes() { savedGeral = _uiState.value.geral }
    fun cancelarAlteracoes() { _uiState.update { it.copy(geral = savedGeral) } }

    // --- GERAL ---
    fun updateNomeEmpresa(nome: String) { _uiState.update { it.copy(geral = it.geral.copy(nomeEmpresa = nome)) } }
    fun updateFusoHorario(fuso: String) { _uiState.update { it.copy(geral = it.geral.copy(fusoHorario = fuso)) } }
    fun updateFormatoData(formato: String) { _uiState.update { it.copy(geral = it.geral.copy(formatoData = formato)) } }
    fun updateTema(tema: String) { _uiState.update { it.copy(geral = it.geral.copy(tema = tema)) } }

    // --- TARIFAS ---
    fun abrirFormTarifa(tarifa: TarifaItem? = null) { _uiState.update { it.copy(mostrarFormTarifa = true, tarifaEditandoId = tarifa?.id) } }
    fun fecharFormTarifa() { _uiState.update { it.copy(mostrarFormTarifa = false, tarifaEditandoId = null) } }
    fun salvarTarifa(descricao: String, valor: Double, inicio: String, fim: String, ativa: Boolean) {
        _uiState.update { state ->
            val id = state.tarifaEditandoId
            val lista = if (id != null) state.tarifas.map { item ->
                when {
                    item.id == id -> item.copy(descricao = descricao, valor = valor, inicio = inicio, fim = fim, ativa = ativa)
                    ativa -> item.copy(ativa = false)
                    else -> item
                }
            } else {
                val base = if (ativa) state.tarifas.map { it.copy(ativa = false) } else state.tarifas
                base + TarifaItem(System.currentTimeMillis(), descricao, valor, inicio, fim, ativa)
            }
            state.copy(tarifas = lista, mostrarFormTarifa = false, tarifaEditandoId = null)
        }
    }
    fun excluirTarifa(id: Long) { _uiState.update { state -> state.copy(tarifas = state.tarifas.filterNot { it.id == id }) } }

    // --- USUÁRIOS ---
    fun abrirFormUsuario(usuario: UsuarioItem? = null) { _uiState.update { it.copy(mostrarFormUsuario = true, usuarioEditandoId = usuario?.id) } }
    fun fecharFormUsuario() { _uiState.update { it.copy(mostrarFormUsuario = false, usuarioEditandoId = null) } }
    fun salvarUsuario(nome: String, email: String, senha: String, perfil: String, status: String) {
        _uiState.update { state ->
            val id = state.usuarioEditandoId
            val lista = if (id != null) state.usuarios.map { item ->
                if (item.id == id) item.copy(nome = nome, email = email, perfil = perfil, status = status) else item
            } else state.usuarios + UsuarioItem(System.currentTimeMillis(), nome, email, perfil, if (perfil == "Administrador") "Acesso global" else "Não definido", status)
            state.copy(usuarios = lista, mostrarFormUsuario = false, usuarioEditandoId = null)
        }
    }
    fun excluirUsuario(id: Long) { _uiState.update { state -> state.copy(usuarios = state.usuarios.filterNot { it.id == id }) } }

    // --- PONTOS DE MEDIÇÃO ---
    fun abrirFormPonto(ponto: PontoMedicaoItem? = null) { _uiState.update { it.copy(mostrarFormPonto = true, pontoEditandoId = ponto?.id) } }
    fun fecharFormPonto() { _uiState.update { it.copy(mostrarFormPonto = false, pontoEditandoId = null) } }
    fun salvarPonto(nome: String, escopo: String, entidade: String, dispositivo: String, modelo: String, ativo: Boolean) {
        _uiState.update { state ->
            val id = state.pontoEditandoId
            val lista = if (id != null) state.pontosMedicao.map { item ->
                if (item.id == id) item.copy(nome = nome, escopo = escopo, entidade = entidade, dispositivo = dispositivo, modelo = modelo, ativo = ativo) else item
            } else state.pontosMedicao + PontoMedicaoItem(System.currentTimeMillis(), nome, escopo, entidade, dispositivo, modelo, ativo)
            state.copy(pontosMedicao = lista, mostrarFormPonto = false, pontoEditandoId = null)
        }
    }
    fun excluirPonto(id: Long) { _uiState.update { state -> state.copy(pontosMedicao = state.pontosMedicao.filterNot { it.id == id }) } }
}