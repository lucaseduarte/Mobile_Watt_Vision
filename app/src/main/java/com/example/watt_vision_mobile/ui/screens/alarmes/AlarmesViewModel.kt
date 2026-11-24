package com.example.watt_vision_mobile.ui.screens.alarmes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.data.repository.AuthRepository
import com.example.watt_vision_mobile.data.repository.SessionCredential
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.AlarmesApiService
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.ChartCatalogApiService
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.data.repository.AlarmesRepository
import com.example.watt_vision_mobile.data.repository.AlarmesResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class AlarmesViewModel(application: Application) : AndroidViewModel(application) {
    private val session = AuthRepository.getInstance(application)
    private val repository = AlarmesRepository(ApiClient.createService<AlarmesApiService>(application),
        ApiClient.createService<AuthApiService>(application), { session.currentCredential()?.token })
    private val catalogo = ApiClient.createService<ChartCatalogApiService>(application)
    private val _uiState = MutableStateFlow(AlarmesUiState())
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null
    private var snapshotSession: SessionCredential? = null

    init { refresh() }

    override fun onCleared() {
        request?.cancel()
        snapshotSession = null
        _uiState.value = AlarmesUiState().failed(AlarmesPhase.NO_SESSION)
        super.onCleared()
    }

    suspend fun entidade(alarme: Alarme) = repository.entidade(alarme, catalogo)

    fun updateFilters(filters: AlarmesFilters) { _uiState.update { it.copy(filters = filters) } }

    fun refresh() {
        request?.cancel()
        val credential = session.currentCredential()
        // Nunca reutilizar o snapshot de outra sessão.
        if (credential !== snapshotSession) _uiState.update { AlarmesUiState(filters = it.filters) }
        _uiState.update { it.refreshing() }
        request = viewModelScope.launch {
            val result = repository.load()
            ensureActive()
            if (session.currentCredential() !== credential) {
                _uiState.update { it.failed(AlarmesPhase.NO_SESSION) }
                return@launch
            }
            _uiState.update { current ->
                when (result) {
                    is AlarmesResult.Success -> {
                        snapshotSession = credential
                        current.loaded(result.records, LocalDateTime.now())
                    }
                    AlarmesResult.NoSession -> current.failed(AlarmesPhase.NO_SESSION)
                    AlarmesResult.Forbidden -> current.failed(AlarmesPhase.FORBIDDEN)
                    AlarmesResult.NetworkError -> current.failed(AlarmesPhase.ERROR)
                    AlarmesResult.Unavailable -> current.failed(AlarmesPhase.UNAVAILABLE)
                }
            }
        }
    }
}
