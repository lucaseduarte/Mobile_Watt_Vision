package com.example.watt_vision_mobile.ui.screens.relatorios

import androidx.compose.ui.graphics.Color
import com.example.watt_vision_mobile.model.LIMITE_REGISTROS_SERIE_TEMPORAL
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal enum class PeriodoRelatorio(val titulo: String) {
    HOJE("Hoje"), SEIS_HORAS("6 horas"), ONTEM("Ontem"),
    SETE_DIAS("Últimos 7 dias"), TRINTA_DIAS("Últimos 30 dias")
}

internal enum class GrandezaRelatorio(val titulo: String, val curto: String, val unidade: String, val cor: Color, val casas: Int = 1) {
    ENERGIA("Consumo (kWh)", "Consumo", "kWh", Color(0xFF3B82F6)),
    POTENCIA("Potência (kW)", "Potência", "kW", Color(0xFF10B981)),
    TENSAO("Tensão (V)", "Tensão", "V", Color(0xFF6366F1)),
    CORRENTE("Corrente (A)", "Corrente", "A", Color(0xFFF59E0B)),
    FREQUENCIA("Frequência (Hz)", "Frequência", "Hz", Color(0xFFA855F7)),
    FATOR("Fator de potência", "Fator Pot.", "", Color(0xFFF97316), 3)
}

internal data class FiltrosRelatorio(
    val periodo: PeriodoRelatorio = PeriodoRelatorio.SETE_DIAS,
    val localId: String? = null,
    val setorId: String? = null,
    val grandeza: GrandezaRelatorio? = null,
    val equipamentoId: String? = null
) {
    val ativos: Int get() = listOf(periodo.takeUnless { it == PeriodoRelatorio.SETE_DIAS }, localId, setorId, grandeza, equipamentoId).count { it != null }
}

internal data class LocalRelatorio(val id: String, val nome: String)
internal data class SetorRelatorio(val id: String, val nome: String, val localId: String)
internal data class EquipamentoRelatorio(val id: String, val nome: String, val localId: String, val setorId: String)
internal data class LeituraRelatorio(
    val medidor: String,
    val localId: String,
    val setorId: String,
    val instante: Instant,
    // Energia é o contador em kWh. Potência já está convertida para kW.
    val valores: Map<GrandezaRelatorio, Double>,
    val equipamentoId: String? = null,
    val id: Long? = null
)
internal data class FonteRelatorio(
    val locais: List<LocalRelatorio>,
    val setores: List<SetorRelatorio>,
    val leituras: List<LeituraRelatorio>,
    val equipamentos: List<EquipamentoRelatorio> = emptyList(),
    val medidores: Map<String, EquipamentoRelatorio> = emptyMap(),
    val cadenciaSegundos: Long? = null,
    val quantidadeRecebida: Int = leituras.size,
    val registrosInvalidos: Boolean = false
) {
    val limiteAtingido get() = quantidadeRecebida >= LIMITE_REGISTROS_SERIE_TEMPORAL
}

internal enum class CoberturaRelatorio(val titulo: String) {
    COMPROVADA("Cobertura comprovada na série simulada"),
    PARCIAL("Cobertura parcial"),
    INDETERMINADA("Cobertura indeterminada"),
    SEM_DADOS("Sem dados")
}

internal data class PontoRelatorio(val instante: Instant, val valor: Double, val medidor: String)
internal data class SerieRelatorio(val pontos: List<PontoRelatorio>, val quantidade: Int)
internal data class DiaRelatorio(val dia: LocalDate, val leituras: Int, val valores: Map<GrandezaRelatorio, Double>, val custo: BigDecimal?)
internal data class Relatorio(
    val leituras: Int,
    val pontosExibidos: Int,
    val series: Map<GrandezaRelatorio, SerieRelatorio>,
    val dias: List<DiaRelatorio>,
    val consumo: Double?,
    val media: Double?,
    val pico: Double?,
    val custo: BigDecimal?,
    val zona: ZoneId,
    val registros: List<LeituraRelatorio>,
    val cobertura: CoberturaRelatorio,
    val limiteAtingido: Boolean,
    val inconsistencias: Boolean
) {
    val podeExportar get() = consumo != null && !inconsistencias
}

internal data class RelatoriosUiState(
    val filtros: FiltrosRelatorio = FiltrosRelatorio(),
    val tarifa: BigDecimal = BigDecimal("0.80"),
    val locais: List<LocalRelatorio> = emptyList(),
    val setores: List<SetorRelatorio> = emptyList(),
    val equipamentos: List<EquipamentoRelatorio> = emptyList(),
    val demonstracaoDisponivel: Boolean = false,
    val demonstracao: Boolean = false,
    val carregando: Boolean = false,
    val erro: String? = null,
    val atualizadoEm: Instant? = null,
    val relatorio: Relatorio? = null
)
