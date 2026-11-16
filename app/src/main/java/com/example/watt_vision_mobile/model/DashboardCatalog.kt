package com.example.watt_vision_mobile.model

data class LocalConsultaDto(val id: Int? = null, val id_empresa: Int? = null, val nome: String? = null)
data class SetorConsultaDto(val id: Int? = null, val id_local: Int? = null, val nome: String? = null)
data class PontoConsultaDto(
    val id: Int? = null, val nome: String? = null, val id_dispositivo_iot: Int? = null,
    val id_local: Int? = null, val id_setor: Int? = null, val id_equipamento: Int? = null
)
data class PzemConsultaDto(
    val id: Int? = null, val id_ponto_medicao: Int? = null, val modelo: String? = null,
    val nome_ponto_medicao: String? = null
)
data class DispositivoConsultaDto(
    val id: Int? = null, val id_empresa: Int? = null, val nome: String? = null,
    val status: String? = null, val data_comunicacao: String? = null
)
data class DashboardCatalog(
    val locais: List<LocalConsultaDto> = emptyList(),
    val setores: List<SetorConsultaDto> = emptyList(),
    val equipamentos: List<EquipamentoDto> = emptyList(),
    val pontos: List<PontoConsultaDto> = emptyList(),
    val sensores: List<PzemConsultaDto> = emptyList(),
    val dispositivos: List<DispositivoConsultaDto> = emptyList()
)
