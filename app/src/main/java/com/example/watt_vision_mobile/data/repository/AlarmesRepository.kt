package com.example.watt_vision_mobile.data.repository

import com.example.watt_vision_mobile.data.api.AlarmesApiService
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.ChartCatalogApiService
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeTipoEntidade
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

sealed interface AlarmesResult {
    data class Success(val records: List<Alarme>) : AlarmesResult
    data object NoSession : AlarmesResult
    data object Forbidden : AlarmesResult
    data object NetworkError : AlarmesResult
    data object Unavailable : AlarmesResult
}

class AlarmesRepository(
    private val api: AlarmesApiService,
    private val auth: AuthApiService,
    private val readToken: () -> String?
) {
    suspend fun load(): AlarmesResult = withContext(Dispatchers.IO) {
        val session = readToken()?.takeIf { it.isNotBlank() } ?: return@withContext AlarmesResult.NoSession
        try {
            // Os GETs de alertas não são protegidos no servidor atual. Não consultá-los
            // como alternativa à sessão: validar primeiro no endpoint autenticado existente.
            val me = auth.getMe()
            if (!me.isSuccessful) return@withContext failure(me.code())
            if (me.body()?.sucesso != true) return@withContext AlarmesResult.Unavailable
            if (readToken() != session) return@withContext AlarmesResult.NoSession
            val response = api.listar()
            if (readToken() != session) return@withContext AlarmesResult.NoSession
            if (!response.isSuccessful) return@withContext failure(response.code())
            val body = response.body() ?: return@withContext AlarmesResult.Unavailable
            if (!body.sucesso) return@withContext AlarmesResult.Unavailable
            val records = body.dados ?: return@withContext AlarmesResult.Unavailable
            val alarmes = records.map(AlarmeMapper::map)
            val nomes = if (alarmes.any { it.nomeEquipamento == null && it.dispositivoIotId != null }) {
                nomesPorDispositivo()
            } else {
                emptyMap()
            }
            if (readToken() != session) return@withContext AlarmesResult.NoSession
            AlarmesResult.Success(alarmes.map { alarme ->
                val nome = alarme.nomeEquipamento ?: nomes[alarme.dispositivoIotId]
                alarme.copy(nomeEquipamento = nome)
            })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            AlarmesResult.NetworkError
        } catch (_: Exception) {
            AlarmesResult.Unavailable
        }
    }

    suspend fun entidade(alarme: Alarme, catalogo: ChartCatalogApiService): AlarmeEntidade? =
        withContext(Dispatchers.IO) {
            val session = readToken()?.takeIf { it.isNotBlank() } ?: return@withContext null
            val usuario = consultar { auth.getMe() } ?: return@withContext null
            if (readToken() != session || usuario.id == null) return@withContext null

            val vinculo = if (alarme.limiteId != null) {
                consultar { api.limite(alarme.limiteId) }?.let {
                    vinculo(it.id_equipamento, it.id_setor, it.id_local)
                }
            } else {
                alarme.dispositivoIotId?.let { id ->
                    consultar { api.pontos() }.orEmpty()
                        .filter { it.id_dispositivo_iot == id }
                        .map { vinculo(it.id_equipamento, it.id_setor, it.id_local) }
                        .distinct().singleOrNull()
                }
            } ?: return@withContext null

            if (readToken() != session) return@withContext null
            resolverEntidade(vinculo, catalogo).takeIf { readToken() == session }
        }

    private suspend fun resolverEntidade(
        vinculo: Pair<AlarmeTipoEntidade, Int>,
        catalogo: ChartCatalogApiService
    ): AlarmeEntidade? {
        val (tipo, id) = vinculo
        val equipamento = if (tipo == AlarmeTipoEntidade.EQUIPAMENTO) {
            consultar { catalogo.equipamentos() }?.firstOrNull { it.id == id }
        } else null
        val setorId = if (tipo == AlarmeTipoEntidade.SETOR) id else equipamento?.id_setor
        val setor = setorId?.let { consultar { catalogo.setores() }?.firstOrNull { it.id == setorId } }
        val localId = if (tipo == AlarmeTipoEntidade.LOCAL) id else setor?.id_local
        val local = localId?.let { consultar { catalogo.locais() }?.firstOrNull { it.id == localId } }
        val nome = when (tipo) {
            AlarmeTipoEntidade.EQUIPAMENTO -> equipamento?.nome
            AlarmeTipoEntidade.SETOR -> setor?.nome
            AlarmeTipoEntidade.LOCAL -> local?.nome
        }?.takeIf { it.isNotBlank() } ?: return null

        return AlarmeEntidade(
            tipo, id, nome,
            setor = setor?.nome.takeIf { tipo == AlarmeTipoEntidade.EQUIPAMENTO },
            local = local?.nome.takeIf { tipo != AlarmeTipoEntidade.LOCAL }
        )
    }

    private fun vinculo(equipamento: Int?, setor: Int?, local: Int?) = when {
        equipamento != null -> AlarmeTipoEntidade.EQUIPAMENTO to equipamento
        setor != null -> AlarmeTipoEntidade.SETOR to setor
        local != null -> AlarmeTipoEntidade.LOCAL to local
        else -> null
    }

    private suspend fun <T> consultar(chamada: suspend () -> Response<ApiResponse<T>>): T? =
        try {
            chamada().takeIf { it.isSuccessful }?.body()?.takeIf { it.sucesso }?.dados
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    private suspend fun nomesPorDispositivo(): Map<Long, String> {
        val pontos = consultar { api.pontos() }.orEmpty()

        return pontos.mapNotNull { ponto ->
            val id = ponto.id_dispositivo_iot ?: return@mapNotNull null
            val nome = ponto.nome_equipamento?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            id to nome
        }.groupBy({ it.first }, { it.second }).mapNotNull { (id, nomes) ->
            nomes.distinct().singleOrNull()?.let { id to it }
        }.toMap()
    }

    private fun failure(code: Int): AlarmesResult = when (code) {
        401 -> AlarmesResult.NoSession
        403 -> AlarmesResult.Forbidden
        else -> AlarmesResult.Unavailable
    }
}
