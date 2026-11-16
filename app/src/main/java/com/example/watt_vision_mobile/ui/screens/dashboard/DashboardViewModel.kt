package com.example.watt_vision_mobile.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val loader = DashboardLoader(DashboardRepository(application))
    private val _uiState = MutableStateFlow(DashboardUiState(selection = restore()))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var pollingJob: Job? = null
    private var visible = false
    private var generation = 0L

    private fun restore() = DashboardSelection(
        location = savedStateHandle["dash.local"], sector = savedStateHandle["dash.sector"],
        equipment = savedStateHandle["dash.equipment"], point = savedStateHandle["dash.point"],
        sensor = savedStateHandle["dash.sensor"], period = savedStateHandle["dash.period"] ?: "24h",
        start = savedStateHandle["dash.start"] ?: DashboardSelection().start,
        end = savedStateHandle["dash.end"] ?: DashboardSelection().end
    )
    fun setVisible(value: Boolean) {
        visible = value
        if (value) loadDashboardData() else { generation++; pollingJob?.cancel() }
    }
    fun select(selection: DashboardSelection) {
        savedStateHandle["dash.local"] = selection.location
        savedStateHandle["dash.sector"] = selection.sector
        savedStateHandle["dash.equipment"] = selection.equipment
        savedStateHandle["dash.point"] = selection.point
        savedStateHandle["dash.sensor"] = selection.sensor
        savedStateHandle["dash.period"] = selection.period
        savedStateHandle["dash.start"] = selection.start
        savedStateHandle["dash.end"] = selection.end
        _uiState.value = DashboardUiState(selection = selection, catalog = _uiState.value.catalog, selectedMetric = _uiState.value.selectedMetric)
        loadDashboardData()
    }
    fun loadDashboardData() {
        generation++
        val request = generation
        pollingJob?.cancel()
        if (!visible) return
        pollingJob = viewModelScope.launch {
            do {
                _uiState.update { it.copy(isLoading = true, stale = it.summaryCards.isNotEmpty(), errorMessage = null) }
                try {
                    val result = loader.load(_uiState.value.selection)
                    ensureActive()
                    if (request == generation) _uiState.update { result.copy(selectedMetric = it.selectedMetric, chartPoints = chartPoints(result.measurements, it.selectedMetric)) }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (error: Exception) {
                    if (request == generation) _uiState.update { old ->
                        val forbidden = error is DashboardApiException && error.code in listOf(401, 403)
                        val base = if (forbidden) DashboardUiState(selection = old.selection, selectedMetric = old.selectedMetric) else old
                        base.copy(isLoading = false, stale = base.summaryCards.isNotEmpty(), errorMessage = DashboardValues.error(error))
                    }
                }
                delay(15000)
            } while (isActive && visible && request == generation)
        }
    }
    fun onMetricSelected(metric: String) {
        require(DashboardValues.metrics.any { it.first == metric })
        _uiState.update { it.copy(selectedMetric = metric, chartPoints = chartPoints(it.measurements, metric)) }
    }
}
