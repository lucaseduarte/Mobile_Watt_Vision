package com.example.watt_vision_mobile.model.alarmes

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// DESCONHECIDO representa um problema de dados, nunca um estado criado no servidor.
enum class AlarmeStatus(val code: String, val label: String) {
    ATIVO("ativo", "Ativo"), RECONHECIDO("reconhecido", "Reconhecido"), RESOLVIDO("resolvido", "Resolvido"), DESCONHECIDO("", "Status não informado")
}
enum class AlarmeSeveridade(val code: String, val label: String) {
    CRITICO("critico", "Crítico"), ATENCAO("atencao", "Atenção"), INFORMATIVO("informativo", "Informativo"), DESCONHECIDA("", "Severidade não informada")
}
enum class AlarmeCategoria(val code: String, val label: String) {
    ENERGETICO("energetico", "Energético"), COMUNICACAO("comunicacao", "Comunicação"), DESCONHECIDA("", "Categoria não informada")
}

enum class AlarmeTipoEntidade(val label: String) {
    LOCAL("Local"), SETOR("Setor"), EQUIPAMENTO("Equipamento")
}

data class AlarmeEntidade(
    val tipo: AlarmeTipoEntidade,
    val id: Int,
    val nome: String,
    val setor: String? = null,
    val local: String? = null,
    val demonstracao: Boolean = false
)

data class Alarme(
    val id: Long?,
    val tipo: String?,
    val mensagem: String?,
    val status: AlarmeStatus,
    val severidade: AlarmeSeveridade,
    val categoria: AlarmeCategoria,
    val statusOriginal: String?,
    val severidadeOriginal: String?,
    val categoriaOriginal: String?,
    val dataHora: String?,
    val valorDetectado: Double?,
    val valorLimite: Double?,
    val tipoId: Long?,
    val limiteId: Long?,
    val dispositivoIotId: Long?,
    val medicaoId: Long?,
    val reconhecidoPor: Long?,
    val dataReconhecimento: String?,
    val nomeEquipamento: String? = null
) {
    val titulo: String get() = tipo?.takeIf { it.isNotBlank() } ?: "Alarme"
    val statusLabel get() = knownLabel(status.label, statusOriginal, status == AlarmeStatus.DESCONHECIDO, "Status não reconhecido")
    val severidadeLabel get() = knownLabel(severidade.label, severidadeOriginal, severidade == AlarmeSeveridade.DESCONHECIDA, "Severidade não reconhecida")
    val categoriaLabel get() = knownLabel(categoria.label, categoriaOriginal, categoria == AlarmeCategoria.DESCONHECIDA, "Categoria não reconhecida")

    private fun knownLabel(label: String, raw: String?, unknown: Boolean, unknownLabel: String): String =
        if (unknown && !raw.isNullOrBlank()) "$unknownLabel: $raw" else label
}

object AlarmeMapper {
    fun map(dto: AlarmeDto): Alarme {
        fun code(raw: String?) = raw?.trim()?.lowercase(Locale.ROOT)
        val status = code(dto.status).let { if (it == "inativo") "resolvido" else it }
        return Alarme(
            dto.id, dto.tipo_alerta, dto.mensagem,
            AlarmeStatus.entries.firstOrNull { it.code.isNotEmpty() && it.code == status } ?: AlarmeStatus.DESCONHECIDO,
            AlarmeSeveridade.entries.firstOrNull { it.code.isNotEmpty() && it.code == code(dto.severidade) } ?: AlarmeSeveridade.DESCONHECIDA,
            AlarmeCategoria.entries.firstOrNull { it.code.isNotEmpty() && it.code == code(dto.categoria_alerta) } ?: AlarmeCategoria.DESCONHECIDA,
            dto.status, dto.severidade, dto.categoria_alerta, dto.data_hora,
            dto.valor_detectado?.takeIf { it.isFinite() }, dto.valor_limite?.takeIf { it.isFinite() },
            dto.id_tipo_alerta, dto.id_limite, dto.id_dispositivo_iot, dto.id_medicao,
            dto.reconhecido_por, dto.data_reconhecimento, dto.nome_equipamento?.takeIf { it.isNotBlank() }
        )
    }
}

object AlarmeDate {
    private val display = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR"))
    fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return "Não informada"
        val normalized = raw.trim().replace(' ', 'T')
        val offset = runCatching { OffsetDateTime.parse(normalized) }.getOrNull()
        if (offset != null) return "${offset.format(display)} ${offset.offset.id}"
        return runCatching { LocalDateTime.parse(normalized).format(display) }
            .getOrElse { "Data inválida" }
    }
}
