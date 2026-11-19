package com.example.watt_vision_mobile.model

data class EquipamentoDto(
    val id: Int?,
    val id_empresa: Int?,
    val nome: String?,
    val tipo: String?,
    val status: String?,
    val localizacao: String?,
    val codigo: String? = null,
    val tipo_equipamento: String? = null,
    val fabricante: String? = null,
    val modelo: String? = null,
    val numero_serie: String? = null,
    val tensao_nominal: Double? = null,
    val data_instalacao: String? = null,
    val descricao: String? = null,
    val nome_setor: String? = null,
    val nome_local: String? = null
)
