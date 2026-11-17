package com.example.watt_vision_mobile.data.repository

import com.example.watt_vision_mobile.data.api.AlarmesApiService
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.model.alarmes.Alarme
import com.example.watt_vision_mobile.model.alarmes.AlarmeMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            AlarmesResult.Success(records.map(AlarmeMapper::map))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            AlarmesResult.NetworkError
        } catch (_: Exception) {
            AlarmesResult.Unavailable
        }
    }

    private fun failure(code: Int): AlarmesResult = when (code) {
        401 -> AlarmesResult.NoSession
        403 -> AlarmesResult.Forbidden
        else -> AlarmesResult.Unavailable
    }
}
