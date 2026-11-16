package com.example.watt_vision_mobile.model

data class EquipamentoDto(
    val id: Int?,
    val id_empresa: Int?,
    val nome: String?,
    val tipo: String?,
    val status: String?,
    val id_setor: Int? = null,
    val codigo: String? = null,
    val tipo_equipamento: String? = null,
    val nome_local: String? = null,
    val nome_setor: String? = null,
    val localizacao: String?
)
