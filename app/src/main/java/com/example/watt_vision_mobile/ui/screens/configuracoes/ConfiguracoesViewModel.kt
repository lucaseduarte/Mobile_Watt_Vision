package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConfiguracoesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracoesUiState())
    val uiState: StateFlow<ConfiguracoesUiState> = _uiState.asStateFlow()

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    // --- GERAL ---
    fun updateNomeEmpresa(nome: String) {
        _uiState.update { it.copy(geral = it.geral.copy(nomeEmpresa = nome)) }
    }

    fun updateFusoHorario(fuso: String) {
        _uiState.update { it.copy(geral = it.geral.copy(fusoHorario = fuso)) }
    }

    fun updateFormatoData(formato: String) {
        _uiState.update { it.copy(geral = it.geral.copy(formatoData = formato)) }
    }

    // --- TARIFAS ---
    fun abrirFormTarifa(tarifa: TarifaItem? = null) {
        _uiState.update {
            it.copy(
                mostrarFormTarifa = true,
                tarifaEditandoId = tarifa?.id
            )
        }
    }

    fun fecharFormTarifa() {
        _uiState.update {
            it.copy(
                mostrarFormTarifa = false,
                tarifaEditandoId = null
            )
        }
    }

    fun salvarTarifa(descricao: String, valor: Double, inicio: String, fim: String, ativa: Boolean) {
        _uiState.update { state ->
            val idEditando = state.tarifaEditandoId
            val listaAtualizada = if (idEditando != null) {
                state.tarifas.map { item ->
                    if (item.id == idEditando) {
                        item.copy(descricao = descricao, valor = valor, inicio = inicio, fim = fim, ativa = ativa)
                    } else if (ativa) {
                        item.copy(ativa = false)
                    } else item
                }
            } else {
                val novaTarifa = TarifaItem(
                    id = System.currentTimeMillis(),
                    descricao = descricao,
                    valor = valor,
                    inicio = inicio,
                    fim = fim,
                    ativa = ativa
                )
                val base = if (ativa) state.tarifas.map { it.copy(ativa = false) } else state.tarifas
                base + novaTarifa
            }
            state.copy(tarifas = listaAtualizada, mostrarFormTarifa = false, tarifaEditandoId = null)
        }
    }

    fun excluirTarifa(id: Long) {
        _uiState.update { state ->
            state.copy(tarifas = state.tarifas.filterNot { it.id == id })
        }
    }

    // --- USUÁRIOS ---
    fun abrirFormUsuario(usuario: UsuarioItem? = null) {
        _uiState.update {
            it.copy(
                mostrarFormUsuario = true,
                usuarioEditandoId = usuario?.id
            )
        }
    }

    fun fecharFormUsuario() {
        _uiState.update {
            it.copy(
                mostrarFormUsuario = false,
                usuarioEditandoId = null
            )
        }
    }

    fun salvarUsuario(nome: String, email: String, perfil: String, status: String) {
        _uiState.update { state ->
            val idEditando = state.usuarioEditandoId
            val listaAtualizada = if (idEditando != null) {
                state.usuarios.map { item ->
                    if (item.id == idEditando) {
                        item.copy(nome = nome, email = email, perfil = perfil, status = status)
                    } else item
                }
            } else {
                val novoUsuario = UsuarioItem(
                    id = System.currentTimeMillis(),
                    nome = nome,
                    email = email,
                    perfil = perfil,
                    local = if (perfil == "Administrador") "Acesso global" else "Não definido",
                    status = status,
                    ultimoAcesso = "—"
                )
                state.usuarios + novoUsuario
            }
            state.copy(usuarios = listaAtualizada, mostrarFormUsuario = false, usuarioEditandoId = null)
        }
    }

    fun excluirUsuario(id: Long) {
        _uiState.update { state ->
            state.copy(usuarios = state.usuarios.filterNot { it.id == id })
        }
    }

    // --- PONTOS DE MEDIÇÃO ---
    fun abrirFormPonto(ponto: PontoMedicaoItem? = null) {
        _uiState.update {
            it.copy(
                mostrarFormPonto = true,
                pontoEditandoId = ponto?.id
            )
        }
    }

    fun fecharFormPonto() {
        _uiState.update {
            it.copy(
                mostrarFormPonto = false,
                pontoEditandoId = null
            )
        }
    }

    fun salvarPonto(nome: String, escopo: String, entidade: String, dispositivo: String, modelo: String) {
        _uiState.update { state ->
            val idEditando = state.pontoEditandoId
            val listaAtualizada = if (idEditando != null) {
                state.pontosMedicao.map { item ->
                    if (item.id == idEditando) {
                        item.copy(nome = nome, escopo = escopo, entidade = entidade, dispositivo = dispositivo, modelo = modelo)
                    } else item
                }
            } else {
                val novoPonto = PontoMedicaoItem(
                    id = System.currentTimeMillis(),
                    nome = nome,
                    escopo = escopo,
                    entidade = entidade,
                    dispositivo = dispositivo,
                    modelo = modelo,
                    ativo = true
                )
                state.pontosMedicao + novoPonto
            }
            state.copy(pontosMedicao = listaAtualizada, mostrarFormPonto = false, pontoEditandoId = null)
        }
    }

    fun excluirPonto(id: Long) {
        _uiState.update { state ->
            state.copy(pontosMedicao = state.pontosMedicao.filterNot { it.id == id })
        }
    }
}
