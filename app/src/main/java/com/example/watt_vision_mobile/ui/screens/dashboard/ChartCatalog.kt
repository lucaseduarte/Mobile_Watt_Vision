package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.data.api.*

data class ChartCatalog(
    val locais: List<ChartLocalDto> = emptyList(),
    val setores: List<ChartSectorDto> = emptyList(),
    val equipamentos: List<ChartEquipmentDto> = emptyList(),
    val pontos: List<ChartPointDto> = emptyList(),
    val sensores: List<ChartSensorDto> = emptyList()
) {
    fun sectors(localId: Int?) = setores.filter { localId == null || it.id_local == localId }
    fun equipment(localId: Int?, sectorId: Int?) = equipamentos.filter { equipment ->
        (sectorId == null || equipment.id_setor == sectorId) &&
            (localId == null || setores.any { it.id == equipment.id_setor && it.id_local == localId })
    }

    fun scopedPointIds(filters: DashboardFilterState): Set<Int>? {
        if (filters.locationId == null && filters.sectorId == null && filters.equipmentId == null) return null
        return pontos.filter { point ->
            val equipment = equipamentos.singleOrNull { it.id == point.id_equipamento }
            val sectorId = point.id_setor ?: equipment?.id_setor
            val localId = point.id_local ?: setores.singleOrNull { it.id == sectorId }?.id_local
            (filters.locationId == null || localId == filters.locationId) &&
                (filters.sectorId == null || sectorId == filters.sectorId) &&
                (filters.equipmentId == null || point.id_equipamento == filters.equipmentId)
        }.mapNotNull { it.id }.toSet()
    }

    /** Nunca confundir nome, ID do equipamento, ID do ponto e ID do medidor. */
    fun resolve(filters: DashboardFilterState): Pair<Int?, ChartStatus?> {
        if ((filters.location == "Todos" && filters.locationId != null) ||
            (filters.sector == "Todos" && filters.sectorId != null) ||
            (filters.equipment == "Todos" && filters.equipmentId != null) ||
            sensores.map { it.id }.distinct().size != sensores.size) return null to ChartStatus.FILTER_UNSUPPORTED
        if ((filters.location != "Todos" && locais.none { it.id == filters.locationId && it.nome == filters.location }) ||
            (filters.sector != "Todos" && setores.none { it.id == filters.sectorId && it.nome == filters.sector }) ||
            (filters.equipment != "Todos" && equipamentos.none { it.id == filters.equipmentId && it.nome == filters.equipment })) {
            return null to ChartStatus.FILTER_UNSUPPORTED
        }
        val candidates = mutableSetOf<Int>()
        for (sensor in sensores) {
            val point = pontos.singleOrNull { it.id == sensor.id_ponto_medicao }
                ?: return null to ChartStatus.FILTER_UNSUPPORTED
            if (sensor.id == null || sensor.id <= 0) return null to ChartStatus.FILTER_UNSUPPORTED
            val equipment = point.id_equipamento?.let { id -> equipamentos.singleOrNull { it.id == id } }
            val sectorId = point.id_setor ?: equipment?.id_setor
            val sector = sectorId?.let { id -> setores.singleOrNull { it.id == id } }
            val localId = point.id_local ?: sector?.id_local
            if ((point.id_equipamento != null && equipment == null) || (sectorId != null && sector == null)) {
                return null to ChartStatus.FILTER_UNSUPPORTED
            }
            val matches = (filters.locationId == null || localId == filters.locationId) &&
                (filters.sectorId == null || sectorId == filters.sectorId) &&
                (filters.equipmentId == null || point.id_equipamento == filters.equipmentId)
            if (matches) candidates += sensor.id
        }
        return when (candidates.size) {
            0 -> null to ChartStatus.NO_SENSOR
            1 -> candidates.single() to null
            else -> null to ChartStatus.AMBIGUOUS
        }
    }
}
