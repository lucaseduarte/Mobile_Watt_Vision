package com.example.watt_vision_mobile.ui.screens.medicoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.watt_vision_mobile.BuildConfig
import com.example.watt_vision_mobile.data.repository.AuthRepository
import com.example.watt_vision_mobile.data.repository.DashboardRepository
import com.example.watt_vision_mobile.data.repository.SessionCredential
import com.example.watt_vision_mobile.model.LIMITE_REGISTROS_SERIE_TEMPORAL
import com.example.watt_vision_mobile.model.MedicaoDto
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal const val PZEM_REFERENCIA = 1
internal val JANELAS_SERIE_REAL = listOf(6, 24, 48, 168)
// Convenção do TCC para DATETIME sem offset. Confirmar o fuso da base MySQL antes do ensaio.
internal val ZONA_SERIE_REAL: ZoneId = ZoneId.of("America/Sao_Paulo")

internal sealed interface ResultadoSerieReal {
    data object NaoIniciado : ResultadoSerieReal
    data object Carregando : ResultadoSerieReal
    data class ComDados(val serie: SerieRealLida) : ResultadoSerieReal
    data class Vazio(val atualizadaEm: Instant) : ResultadoSerieReal
    data class ErroHttp(val codigo: Int) : ResultadoSerieReal
    data object ErroConexao : ResultadoSerieReal
    data object RespostaInvalida : ResultadoSerieReal
    data object AcessoNegado : ResultadoSerieReal
}

internal data class SerieRealLida(
    val registros: List<RegistroMedicao>,
    val atualizadaEm: Instant,
    val quantidade: Int,
    val houveQueda: Boolean
) {
    val primeiroTimestamp: String? get() = registros.first().dados.data_hora
    val ultimoTimestamp: String? get() = registros.last().dados.data_hora
    val possivelTruncamento: Boolean get() = quantidade >= LIMITE_REGISTROS_SERIE_TEMPORAL
}

internal data class EstadoSerieReal(
    val modoReal: Boolean = false,
    val horas: Int = 24,
    val resultado: ResultadoSerieReal = ResultadoSerieReal.NaoIniciado
)

// A instância pertence a uma credencial, mesmo que outra sessão receba o mesmo JWT.
internal class FonteSerieReal(
    private val escopo: CoroutineScope,
    private val habilitada: Boolean,
    private val credencialCorrente: () -> SessionCredential?,
    sessoes: Flow<*>,
    private val consultar: suspend (Int, Int) -> Result<List<MedicaoDto>>,
    private val agora: () -> Instant = Instant::now
) {
    private val titular = credencialCorrente()
    private val mutableEstado = MutableStateFlow(EstadoSerieReal())
    val estado = mutableEstado.asStateFlow()
    private var carga: Job? = null
    private var revisao = 0L

    init {
        escopo.launch {
            sessoes.collect {
                if (!sessaoAtual()) selecionarModoReal(false)
            }
        }
    }

    fun selecionarModoReal(ativo: Boolean) {
        if (ativo && (!habilitada || !sessaoAtual())) return
        val atual = mutableEstado.value
        if (atual.modoReal == ativo) return
        cancelarCarga()
        mutableEstado.value = atual.copy(modoReal = ativo, resultado = ResultadoSerieReal.NaoIniciado)
    }

    fun selecionarJanela(horas: Int) {
        val atual = mutableEstado.value
        if (!atual.modoReal || !sessaoAtual() || horas !in JANELAS_SERIE_REAL || horas == atual.horas) return
        cancelarCarga()
        mutableEstado.value = atual.copy(horas = horas, resultado = ResultadoSerieReal.NaoIniciado)
    }

    fun atualizar() {
        val atual = mutableEstado.value
        if (!habilitada || !atual.modoReal || !sessaoAtual() ||
            atual.resultado == ResultadoSerieReal.Carregando) return

        val tentativa = ++revisao
        mutableEstado.value = atual.copy(resultado = ResultadoSerieReal.Carregando)
        carga = escopo.launch {
            val resultado = try {
                normalizar(consultar(PZEM_REFERENCIA, atual.horas).getOrThrow(), agora())
            } catch (cancelada: CancellationException) {
                throw cancelada
            } catch (erro: Exception) {
                when (erro) {
                    is HttpException -> if (erro.code() == 403) ResultadoSerieReal.AcessoNegado
                        else ResultadoSerieReal.ErroHttp(erro.code())
                    is JsonParseException, is MalformedJsonException, is EOFException -> ResultadoSerieReal.RespostaInvalida
                    is IOException -> ResultadoSerieReal.ErroConexao
                    else -> ResultadoSerieReal.RespostaInvalida
                }
            }
            currentCoroutineContext().ensureActive()
            if (revisao == tentativa && sessaoAtual()) {
                mutableEstado.value = atual.copy(resultado = resultado)
            }
        }
    }

    private fun sessaoAtual() = titular != null && credencialCorrente() === titular

    private fun cancelarCarga() {
        revisao++
        carga?.cancel()
        carga = null
    }

    private fun normalizar(linhas: List<MedicaoDto>, atualizadaEm: Instant): ResultadoSerieReal {
        if (linhas.isEmpty()) return ResultadoSerieReal.Vazio(atualizadaEm)
        val registros = linhas.map { linha ->
            check(linha.id_pzem == PZEM_REFERENCIA && (linha.id_medicao ?: 0L) > 0)
            RegistroMedicao(checkNotNull(instanteMedicao(linha.data_hora, ZONA_SERIE_REAL)), linha)
        }.sortedWith(compareBy({ it.tempo }, { it.dados.id_medicao }))

        val contadores = mutableMapOf<Int, java.math.BigDecimal>()
        var houveQueda = false
        for (registro in registros) {
            val dados = registro.dados
            val ponto = dados.id_ponto_medicao ?: continue
            val valor = dados.energiaAcumuladaRaw?.toString()?.toBigDecimalOrNull() ?: continue
            val anterior = contadores.put(ponto, valor)
            if (anterior != null && valor < anterior) houveQueda = true
        }
        return ResultadoSerieReal.ComDados(SerieRealLida(registros, atualizadaEm, linhas.size, houveQueda))
    }
}

internal class SerieRealViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = AuthRepository.getInstance(application)
    private val repositorio by lazy { DashboardRepository(application) }

    val habilitada = BuildConfig.DEBUG && BuildConfig.LOCAL_SERIES_ENABLED &&
        BuildConfig.API_BASE_URL == "http://10.0.2.2:3000/"

    val fonte = FonteSerieReal(
        viewModelScope,
        habilitada,
        auth::currentCredential,
        auth.session,
        consultar = { pzem, horas -> repositorio.getChartSerieTemporal(pzem, horas) }
    )
}
