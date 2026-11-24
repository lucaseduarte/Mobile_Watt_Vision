package com.example.watt_vision_mobile.ui.screens.relatorios

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

internal class RelatoriosViewModel(
    application: Application,
    private val fonteDemo: (suspend (ZonedDateTime) -> FonteRelatorio)?,
    private val carregarBackend: (suspend (ZonedDateTime) -> FonteRelatorio)? = null,
    private val relogio: () -> ZonedDateTime
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application, null,
        if (com.example.watt_vision_mobile.data.api.integracaoLocal) ({ carregarRelatorioBackend(application) }) else null,
        { ZonedDateTime.now(com.example.watt_vision_mobile.ui.screens.medicoes.ZONA_SERIE_REAL) }
    )
    private val estado = MutableStateFlow(
        RelatoriosUiState(
            demonstracaoDisponivel = fonteDemo != null
        )
    )
    val uiState = estado.asStateFlow()

    private var consulta: Job? = null
    private var fonte: FonteRelatorio? = null
    private var referencia: ZonedDateTime? = null

    init { if (carregarBackend != null) atualizar() }

    fun demonstracao(ativa: Boolean) {
        consulta?.cancel()
        fonte = null
        referencia = null
        estado.update {
            it.copy(demonstracao = ativa, locais = emptyList(), setores = emptyList(), equipamentos = emptyList(), relatorio = null,
                atualizadoEm = null, erro = null, carregando = false, filtros = it.filtros.copy(localId = null, setorId = null, equipamentoId = null))
        }
        if (ativa) atualizar()
    }

    fun aplicar(filtros: FiltrosRelatorio) {
        estado.update { it.copy(filtros = filtros) }
        if (carregarBackend != null || estado.value.demonstracao) consultar(recarregar = false)
    }

    fun atualizar() {
        if (carregarBackend != null || estado.value.demonstracao) consultar(recarregar = true)
    }

    override fun onCleared() {
        consulta?.cancel()
        fonte = null
        referencia = null
        estado.value = RelatoriosUiState()
        super.onCleared()
    }

    private fun consultar(recarregar: Boolean) {
        val carregar = carregarBackend ?: fonteDemo ?: return
        consulta?.cancel()
        val atual = estado.value
        estado.update { it.copy(carregando = true, erro = null, relatorio = null) }
        consulta = viewModelScope.launch {
            try {
                val agora = if (recarregar) relogio() else referencia ?: relogio()
                val (dados, relatorio) = withContext(Dispatchers.Default) {
                    val dados = if (recarregar) carregar(agora) else fonte ?: carregar(agora)
                    dados to montarRelatorio(dados, atual.filtros, atual.tarifa.takeIf { atual.demonstracao }, agora)
                }
                ensureActive()
                fonte = dados
                referencia = agora
                estado.update {
                    it.copy(carregando = false, locais = dados.locais, setores = dados.setores,
                        equipamentos = dados.equipamentos, relatorio = relatorio, atualizadoEm = agora.toInstant())
                }
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (_: Exception) {
                ensureActive()
                estado.update { it.copy(carregando = false, erro = "Não foi possível montar o relatório.", atualizadoEm = null) }
            }
        }
    }
}
