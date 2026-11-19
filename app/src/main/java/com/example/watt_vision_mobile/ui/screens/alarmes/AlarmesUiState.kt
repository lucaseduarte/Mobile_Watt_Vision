package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.model.alarmes.*
import java.text.Normalizer
import java.time.LocalDateTime
import java.util.Locale

// Filtros são locais sobre a lista recebida, sem supor parâmetros inexistentes na API.
data class AlarmesFilters(
    val query: String = "",
    val status: AlarmeStatus? = null,
    val severidade: AlarmeSeveridade? = null,
    val categoria: AlarmeCategoria? = null
) {
    val activeCount get() = listOf(status, severidade, categoria).count { it != null }
}

data class AlarmesCounts(val ativos: Int, val criticos: Int, val atencao: Int, val resolvidos: Int)

object AlarmesRules {
    fun counts(records: List<Alarme>): AlarmesCounts {
        val ativos = records.filter { it.status == AlarmeStatus.ATIVO || it.status == AlarmeStatus.RECONHECIDO }
        return AlarmesCounts(
            ativos.size,
            ativos.count { it.severidade == AlarmeSeveridade.CRITICO },
            ativos.count { it.severidade == AlarmeSeveridade.ATENCAO },
            records.count { it.status == AlarmeStatus.RESOLVIDO }
        )
    }

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)

    fun filter(records: List<Alarme>, filters: AlarmesFilters): List<Alarme> {
        val search = normalize(filters.query.trim())
        return records.filter { record ->
            (filters.status == null || record.status == filters.status) &&
                (filters.severidade == null || record.severidade == filters.severidade) &&
                (filters.categoria == null || record.categoria == filters.categoria) &&
                (search.isEmpty() || normalize(listOfNotNull(
                    record.mensagem, record.tipo, record.nomeEquipamento, record.id?.toString(), record.statusLabel,
                    record.severidadeLabel, record.categoriaLabel,
                    record.dispositivoIotId?.let { "Dispositivo IoT $it" }, record.medicaoId?.let { "Medição $it" }
                ).joinToString(" ")).contains(search))
        }
    }
}

enum class AlarmesPhase { LOADING, CONTENT, EMPTY, ERROR, NO_SESSION, FORBIDDEN, UNAVAILABLE }

data class AlarmesUiState(
    val records: List<Alarme> = emptyList(),
    val filters: AlarmesFilters = AlarmesFilters(),
    val phase: AlarmesPhase = AlarmesPhase.LOADING,
    val hasSnapshot: Boolean = false,
    val isRefreshing: Boolean = true,
    val isStale: Boolean = false,
    val updatedAt: LocalDateTime? = null
) {
    fun refreshing() = copy(phase = AlarmesPhase.LOADING, isRefreshing = true)
    fun loaded(data: List<Alarme>, now: LocalDateTime) = copy(
        records = data, phase = if (data.isEmpty()) AlarmesPhase.EMPTY else AlarmesPhase.CONTENT,
        hasSnapshot = true, isRefreshing = false, isStale = false, updatedAt = now
    )
    fun failed(reason: AlarmesPhase): AlarmesUiState {
        val clear = reason == AlarmesPhase.NO_SESSION || reason == AlarmesPhase.FORBIDDEN
        return copy(records = if (clear) emptyList() else records, phase = reason,
            hasSnapshot = !clear && hasSnapshot, isRefreshing = false,
            isStale = !clear && hasSnapshot, updatedAt = if (clear) null else updatedAt)
    }
}
