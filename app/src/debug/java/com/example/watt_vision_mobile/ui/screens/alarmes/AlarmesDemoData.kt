package com.example.watt_vision_mobile.ui.screens.alarmes

import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeDto
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeTipoEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeMapper
import java.time.LocalDateTime

/** Exemplos artificiais, independentes do banco, gerados somente após ativação explícita. */
object AlarmesDemoData {
    suspend fun entidade(alarme: Alarme): AlarmeEntidade? {
        val id = alarme.id?.toInt() ?: return null
        val nome = alarme.nomeEquipamento ?: return null
        return AlarmeEntidade(
            AlarmeTipoEntidade.EQUIPAMENTO, id, nome,
            setor = "Setor de exemplo", local = "Local de exemplo", demonstracao = true
        )
    }

    fun create(now: LocalDateTime): List<Alarme> {
        val samples = listOf(
            Triple("ativo", "critico", "energetico"), Triple("ativo", "atencao", "energetico"),
            Triple("ativo", "informativo", "comunicacao"), Triple("reconhecido", "critico", "energetico"),
            Triple("reconhecido", "atencao", "comunicacao"), Triple("ativo", "critico", "comunicacao"),
            Triple("ativo", "atencao", "energetico"), Triple("ativo", "informativo", "comunicacao"),
            Triple("reconhecido", "informativo", "energetico")
        )
        val messages = listOf(
            "Valor acima do limite na leitura demonstrativa.", "Variação elétrica em acompanhamento.",
            "Evento de comunicação registrado.", "Ocorrência elétrica já reconhecida no sistema de exemplo.",
            "Comunicação intermitente registrada e reconhecida.", "Interrupção de comunicação na amostra demonstrativa.",
            "Valor próximo ao limite de referência da demonstração.", "Evento informativo do dispositivo de exemplo.",
            "Ocorrência informativa já reconhecida no sistema de exemplo."
        )
        val records = samples.mapIndexed { index, (status, severity, category) ->
            AlarmeMapper.map(AlarmeDto(
                id = 10001L + index, id_tipo_alerta = if (category == "energetico") 101 else 102,
                id_limite = if (category == "energetico") 201 else null,
                id_dispositivo_iot = 301L + index, id_medicao = if (category == "energetico") 401L + index else null,
                valor_detectado = if (category == "energetico") 245.5 + index else null,
                valor_limite = if (category == "energetico") 240.0 else null,
                mensagem = messages[index], severidade = severity, status = status,
                data_hora = now.minusMinutes(index * 30L).toString(),
                tipo_alerta = if (category == "energetico") "Limite excedido" else "Dispositivo offline",
                nome_equipamento = "Equipamento de exemplo ${index + 1}",
                categoria_alerta = category,
                reconhecido_por = if (status == "reconhecido") 501 else null,
                data_reconhecimento = if (status == "reconhecido") now.minusMinutes(index * 30L - 5).toString() else null
            ))
        }
        return records + AlarmeMapper.map(AlarmeDto(id = 10010L, mensagem = "Exemplo de registro com classificação ausente.", data_hora = now.minusDays(1).toString()))
    }
}
