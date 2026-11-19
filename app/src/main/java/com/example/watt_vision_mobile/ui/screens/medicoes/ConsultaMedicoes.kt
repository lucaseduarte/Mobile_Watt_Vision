package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.watt_vision_mobile.data.repository.DashboardRepository
import com.example.watt_vision_mobile.model.MedicaoDto
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartCatalog
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartMetric
import com.example.watt_vision_mobile.ui.screens.dashboard.ChartStatus
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardFilterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

internal data class ConsultaMedicoes(
    val catalogo: ChartCatalog = ChartCatalog(),
    val serie: SerieMedicoes = SerieMedicoes(),
    val aviso: String? = null,
    val carregando: Boolean = false,
    val tentarNovamente: (() -> Unit)? = null,
    val registros: List<RegistroMedicao> = emptyList(),
    val carregandoRegistros: Boolean = carregando,
    val avisoRegistros: String? = aviso
)

private data class RespostaMedicoes(
    val linhas: List<MedicaoDto>,
    val janela: JanelaMedicoes,
    val zona: ZoneId,
    val pontos: Set<Int>?
)

private fun erroConsulta(erro: Throwable?): String = when ((erro as? HttpException)?.code()) {
    401 -> "Entre na API para consultar as medições."
    403 -> "Seu perfil não tem acesso a estas medições."
    else -> "Não foi possível consultar as medições."
}

@Composable
internal fun consultarMedicoes(filtros: DashboardFilterState, grandeza: ChartMetric): ConsultaMedicoes {
    val contexto = LocalContext.current.applicationContext
    val repositorio = remember(contexto) { DashboardRepository(contexto) }
    var revisaoCatalogo by remember { mutableIntStateOf(0) }
    val catalogo by produceState<Result<ChartCatalog>?>(null, repositorio, revisaoCatalogo) {
        value = null
        value = repositorio.getChartCatalog()
    }
    val estrutura = catalogo?.getOrNull()
    if (estrutura == null) {
        val resultado = catalogo ?: return ConsultaMedicoes(carregando = true)
        return ConsultaMedicoes(aviso = erroConsulta(resultado.exceptionOrNull()), tentarNovamente = { revisaoCatalogo++ })
    }

    val (medidor, estado) = remember(estrutura, filtros) { estrutura.resolve(filtros) }
    if (medidor == null) {
        val aviso = when (estado) {
            ChartStatus.NO_SENSOR -> "Nenhum medidor vinculado aos filtros selecionados."
            ChartStatus.AMBIGUOUS -> "Selecione um local, setor ou equipamento vinculado a um único medidor."
            else -> "Revise os filtros. Não foi possível identificar o medidor."
        }
        return ConsultaMedicoes(catalogo = estrutura, aviso = aviso, tentarNovamente = { revisaoCatalogo++ })
    }

    var revisaoSerie by remember { mutableIntStateOf(0) }
    var resposta by remember(filtros, estrutura) { mutableStateOf<RespostaMedicoes?>(null) }
    var erro by remember(filtros, estrutura) { mutableStateOf<Throwable?>(null) }
    var carregando by remember(filtros, estrutura) { mutableStateOf(true) }
    LaunchedEffect(repositorio, filtros, estrutura, revisaoSerie) {
        erro = null
        carregando = true
        val pontos = estrutura.scopedPointIds(filtros)
        while (true) {
            val agora = ZonedDateTime.now()
            val janela = janelaMedicoes(filtros.period, agora)
            repositorio.getChartSerieTemporal(medidor, janela.horas).fold(
                onSuccess = { linhas ->
                    if (linhas.any { it.id_pzem != medidor }) {
                        resposta = null
                        erro = IllegalStateException("Medidor divergente")
                    } else {
                        resposta = RespostaMedicoes(linhas, janela, agora.zone, pontos)
                        erro = null
                    }
                },
                onFailure = { falha ->
                    if ((falha as? HttpException)?.code() in listOf(401, 403)) resposta = null
                    erro = falha
                }
            )
            carregando = false
            delay(30_000)
        }
    }
    val dados = resposta
    val serie by key(filtros, grandeza) {
        produceState<SerieMedicoes?>(null, dados) {
            value = if (dados == null) null else withContext(Dispatchers.Default) {
                montarSerie(dados.linhas, grandeza, dados.janela, dados.zona, dados.pontos)
            }
        }
    }
    val registros by key(filtros) {
        produceState<List<RegistroMedicao>?>(null, dados) {
            value = if (dados == null) null else withContext(Dispatchers.Default) {
                montarRegistros(dados.linhas, dados.janela, dados.zona, dados.pontos)
            }
        }
    }
    val validadeRegistros = registros?.firstOrNull()?.tempo?.let {
        Duration.between(it, java.time.Instant.now())
    }
    val validade = serie?.amostras?.lastOrNull()?.tempo?.let { Duration.between(it, java.time.Instant.now()) }
    fun aviso(validade: Duration?): String? = when {
        erro != null && dados != null -> "Falha ao atualizar. Exibindo a última consulta disponível."
        erro != null -> erroConsulta(erro)
        dados?.linhas?.size == 10_000 -> "Limite da API atingido. Os dados deste período podem estar incompletos."
        filtros.period == "Hoje" && validade != null && validade >= Duration.ofMinutes(15) ->
            "Última leitura recebida há pelo menos 15 minutos."
        else -> null
    }
    return ConsultaMedicoes(
        catalogo = estrutura,
        serie = if (dados == null) SerieMedicoes() else serie ?: SerieMedicoes(),
        aviso = aviso(validade),
        carregando = carregando || (dados != null && serie == null),
        tentarNovamente = if (erro != null) ({ revisaoSerie++ }) else null,
        registros = if (dados == null) emptyList() else registros.orEmpty(),
        carregandoRegistros = carregando || (dados != null && registros == null),
        avisoRegistros = aviso(validadeRegistros)
    )
}
