package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.runtime.*
import com.example.watt_vision_mobile.model.AVISO_LIMITE_SERIE_TEMPORAL
import com.example.watt_vision_mobile.model.LIMITE_REGISTROS_SERIE_TEMPORAL
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartStatus
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardFilterState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal interface FonteSerieSimulada {
    val catalogo: ChartCatalog
    suspend fun consultar(pzem: Int, horas: Int): List<MedicaoDto>
}

internal data class ConsultaMedicoes(
    val catalogo: ChartCatalog = ChartCatalog(),
    val serie: SerieMedicoes = SerieMedicoes(),
    val aviso: String? = null,
    val carregando: Boolean = false,
    val tentarNovamente: (() -> Unit)? = null,
    val registros: List<RegistroMedicao> = emptyList(),
    val carregandoRegistros: Boolean = carregando,
    val avisoRegistros: String? = aviso,
    val zona: java.time.ZoneId = java.time.ZoneId.systemDefault()
)

internal fun consultaMedicoesReal(
    estado: EstadoSerieReal, grandeza: ChartMetric, atualizar: () -> Unit
): ConsultaMedicoes {
    val resultado = estado.resultado
    val lida = (resultado as? ResultadoSerieReal.ComDados)?.serie
    val aviso = when (resultado) {
        ResultadoSerieReal.NaoIniciado -> "Toque em Atualizar para consultar o Backend."
        ResultadoSerieReal.Carregando -> null
        is ResultadoSerieReal.Vazio -> "Sem leituras no período."
        is ResultadoSerieReal.ComDados -> buildList {
            if (resultado.serie.houveQueda) add("Queda no contador de energia neste intervalo.")
            if (resultado.serie.possivelTruncamento) add(AVISO_LIMITE_SERIE_TEMPORAL)
        }.joinToString("\n").ifEmpty { null }
        is ResultadoSerieReal.ErroHttp -> if (resultado.codigo == 401) "Sessão recusada (HTTP 401)."
            else "Falha na consulta (HTTP ${resultado.codigo})."
        ResultadoSerieReal.ErroConexao -> "Não foi possível conectar ao Backend."
        ResultadoSerieReal.RespostaInvalida -> "O Backend devolveu uma série inválida."
        ResultadoSerieReal.AcessoNegado -> "Acesso negado (HTTP 403)."
    }
    val serie = if (lida == null) SerieMedicoes() else montarSerie(
        lida.registros.map { it.dados },
        grandeza,
        JanelaMedicoes(lida.registros.first().tempo, lida.registros.last().tempo, false, estado.horas),
        ZONA_SERIE_REAL,
        pontos = null
    )
    return ConsultaMedicoes(
        serie = serie,
        aviso = aviso,
        carregando = resultado == ResultadoSerieReal.Carregando,
        tentarNovamente = atualizar.takeIf {
            resultado is ResultadoSerieReal.ErroHttp || resultado == ResultadoSerieReal.ErroConexao ||
                resultado == ResultadoSerieReal.RespostaInvalida || resultado == ResultadoSerieReal.AcessoNegado
        },
        registros = lida?.registros?.asReversed().orEmpty(),
        zona = ZONA_SERIE_REAL
    )
}

@Composable
internal fun consultarMedicoes(
    fonte: FonteSerieSimulada,
    filtros: DashboardFilterState,
    revisao: Int,
    atualizar: () -> Unit
): ConsultaMedicoes {
    return key(fonte, filtros, revisao) {
        val consulta by produceState(ConsultaMedicoes(catalogo = fonte.catalogo, carregando = true)) {
            val (pzem, estado) = fonte.catalogo.resolve(filtros)
            if (pzem == null) {
                val aviso = when (estado) {
                    ChartStatus.NO_SENSOR -> "Nenhum medidor vinculado aos filtros selecionados."
                    ChartStatus.AMBIGUOUS -> "Selecione um local, setor ou equipamento vinculado a um único medidor."
                    else -> "Revise os filtros. Não foi possível identificar o medidor."
                }
                value = ConsultaMedicoes(catalogo = fonte.catalogo, aviso = aviso)
                return@produceState
            }

            val agora = java.time.ZonedDateTime.now()
            val janela = janelaMedicoes(filtros.period, agora)
            value = try {
                val linhas = fonte.consultar(pzem, janela.horas)
                currentCoroutineContext().ensureActive()
                check(linhas.all { it.id_pzem == pzem })
                val pontos = fonte.catalogo.scopedPointIds(filtros)
                ConsultaMedicoes(
                    catalogo = fonte.catalogo,
                    serie = montarSerie(linhas, filtros.metric, janela, agora.zone, pontos),
                    registros = montarRegistros(linhas, janela, agora.zone, pontos),
                    aviso = AVISO_LIMITE_SERIE_TEMPORAL.takeIf { linhas.size >= LIMITE_REGISTROS_SERIE_TEMPORAL }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                ConsultaMedicoes(
                    catalogo = fonte.catalogo,
                    aviso = "Não foi possível consultar as medições simuladas.",
                    tentarNovamente = atualizar
                )
            }
        }
        consulta
    }
}
