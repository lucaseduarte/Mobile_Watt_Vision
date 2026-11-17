package com.example.watt_vision_mobile.model.alarmes

/** Campos de GET /api/alertas. IDs BIGINT são Long; os joins não existem em todo GET. */
data class AlarmeDto(
    val id: Long? = null,
    val id_tipo_alerta: Long? = null,
    val id_limite: Long? = null,
    val id_dispositivo_iot: Long? = null,
    val id_medicao: Long? = null,
    val valor_detectado: Double? = null,
    val valor_limite: Double? = null,
    val severidade: String? = null,
    val mensagem: String? = null,
    val data_hora: String? = null,
    val status: String? = null,
    val reconhecido_por: Long? = null,
    val data_reconhecimento: String? = null,
    val tipo_alerta: String? = null,
    val categoria_alerta: String? = null
)
