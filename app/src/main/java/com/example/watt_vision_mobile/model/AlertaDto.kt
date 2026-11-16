package com.example.watt_vision_mobile.model

data class AlertaDto(
    val id: Long?,
    val id_equipamento: Int?,
    val tipo: String?,
    val mensagem: String?,
    val severidade: String?,
    val data_hora: String?,
    val status: String? = null,
    val tipo_alerta: String? = null,
    val categoria_alerta: String? = null,
    val id_dispositivo_iot: Int? = null,
    val id_medicao: Long? = null,
    val id_limite: Int? = null,
    val resolvido: Boolean?
)
