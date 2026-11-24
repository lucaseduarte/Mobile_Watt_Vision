package com.example.watt_vision_mobile.data.repository

import android.content.Context
import com.example.watt_vision_mobile.data.TokenManager
import com.example.watt_vision_mobile.data.TokenStore
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.model.LoginRequest
import com.example.watt_vision_mobile.model.UsuarioDto
import com.google.gson.JsonParser
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal enum class AuthFailure(val message: String) {
    INVALID_CREDENTIALS("E-mail ou senha inválidos."),
    REJECTED("Sessão rejeitada. Entre novamente."),
    FORBIDDEN("Acesso negado para esta conta."),
    NETWORK("Não foi possível conectar à API. Verifique a conexão e tente novamente."),
    SERVER("O servidor está indisponível. Tente novamente."),
    INVALID_RESPONSE("A API retornou uma resposta de autenticação inválida."),
    STORAGE("Não foi possível acessar o armazenamento seguro da sessão.")
}

internal sealed interface SessionState {
    data object Initializing : SessionState
    data class SignedOut(val failure: AuthFailure? = null) : SessionState
    data class Validating(val login: Boolean) : SessionState
    data class Authenticated(val generation: Long, val user: UsuarioDto) : SessionState
    data class ValidationFailed(val failure: AuthFailure, val clearing: Boolean = false) : SessionState
}

// Identidade do objeto distingue sessões mesmo quando o servidor devolve o mesmo JWT.
internal class SessionCredential(val token: String) {
    fun authorization() = "Bearer $token"
}

internal class AuthRepository(
    private val api: AuthApiService,
    private val storage: TokenStore
) {
    private val lock = Any()
    private val mutableSession = MutableStateFlow<SessionState>(SessionState.Initializing)
    val session = mutableSession.asStateFlow()

    private var generation = 0L
    private var credential: SessionCredential? = null

    fun currentCredential(): SessionCredential? = synchronized(lock) { credential }

    suspend fun restore() = withContext(Dispatchers.IO) {
        val attempt = synchronized(lock) {
            val state = mutableSession.value
            if (state != SessionState.Initializing &&
                (state !is SessionState.ValidationFailed || state.clearing)
            ) return@withContext
            begin(login = false)
        }
        try {
            val token = synchronized(lock) {
                if (attempt != generation) return@withContext
                storageOperation(attempt) { storage.read() } ?: run {
                    if (mutableSession.value is SessionState.Validating) mutableSession.value = SessionState.SignedOut()
                    return@withContext
                }
            }
            if (usableJwt(token)) {
                validate(attempt, token, persist = false)
            } else {
                synchronized(lock) {
                    if (attempt == generation) clearSession(AuthFailure.REJECTED)
                }
            }
        } catch (cancelled: CancellationException) {
            complete(attempt, SessionState.Initializing)
            throw cancelled
        } catch (failure: Exception) {
            complete(attempt, SessionState.ValidationFailed(networkFailure(failure)))
        }
    }

    suspend fun login(email: String, password: String, rememberMe: Boolean) = withContext(Dispatchers.IO) {
        val attempt = synchronized(lock) {
            if (mutableSession.value !is SessionState.SignedOut || email.isBlank() || password.isBlank()) {
                return@withContext
            }
            begin(login = true)
        }
        try {
            val response = api.login(LoginRequest(email.trim(), password))
            currentCoroutineContext().ensureActive()
            val body = response.body()
            val token = body?.dados?.token
            when {
                response.code() == 400 || response.code() == 401 -> signedOut(attempt, AuthFailure.INVALID_CREDENTIALS)
                response.code() == 403 -> signedOut(attempt, AuthFailure.FORBIDDEN)
                response.code() >= 500 -> signedOut(attempt, AuthFailure.SERVER)
                !response.isSuccessful || body?.sucesso != true || token == null || !usableJwt(token) ->
                    signedOut(attempt, AuthFailure.INVALID_RESPONSE)
                else -> validate(attempt, token, persist = rememberMe, newLogin = true)
            }
        } catch (cancelled: CancellationException) {
            complete(attempt, SessionState.SignedOut())
            throw cancelled
        } catch (failure: Exception) {
            signedOut(attempt, networkFailure(failure))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        synchronized(lock) { clearSession() }
    }

    fun rejectIfCurrent(rejected: SessionCredential) {
        synchronized(lock) {
            if (credential === rejected) clearSession(AuthFailure.REJECTED)
        }
    }

    private suspend fun validate(attempt: Long, token: String, persist: Boolean, newLogin: Boolean = false) {
        if (!synchronized(lock) { attempt == generation }) return
        val response = api.getMe("Bearer $token")
        currentCoroutineContext().ensureActive()
        val user = response.body()?.takeIf { it.sucesso }?.dados
        val failure = when {
            response.code() == 401 -> AuthFailure.REJECTED
            response.code() == 403 -> AuthFailure.FORBIDDEN
            response.code() >= 500 -> AuthFailure.SERVER
            !response.isSuccessful || user == null || (user.id ?: 0) <= 0 || user.email.isNullOrBlank() ->
                AuthFailure.INVALID_RESPONSE
            else -> null
        }
        synchronized(lock) {
            if (attempt != generation) return
            if (failure != null) {
                when {
                    failure == AuthFailure.REJECTED && !newLogin -> clearSession(failure)
                    newLogin -> mutableSession.value = SessionState.SignedOut(failure)
                    else -> mutableSession.value = SessionState.ValidationFailed(failure)
                }
                return
            }
            if (persist) {
                try {
                    storage.save(token)
                } catch (_: Exception) {
                    clearSession(AuthFailure.STORAGE)
                    return
                }
            }
            credential = SessionCredential(token)
            mutableSession.value = SessionState.Authenticated(attempt, requireNotNull(user))
        }
    }

    private fun begin(login: Boolean): Long {
        mutableSession.value = SessionState.Validating(login)
        return ++generation
    }

    private fun signedOut(attempt: Long, failure: AuthFailure) = complete(attempt, SessionState.SignedOut(failure))

    private fun complete(attempt: Long, state: SessionState) {
        synchronized(lock) {
            if (generation == attempt && mutableSession.value is SessionState.Validating) {
                mutableSession.value = state
            }
        }
    }

    private fun clearSession(failure: AuthFailure? = null) {
        ++generation
        credential = null
        mutableSession.value = SessionState.Validating(login = false)
        try {
            storage.clear()
            mutableSession.value = SessionState.SignedOut(failure)
        } catch (_: Exception) {
            mutableSession.value = SessionState.ValidationFailed(AuthFailure.STORAGE, clearing = true)
        }
    }

    private inline fun <T> storageOperation(attempt: Long, operation: () -> T): T? = try {
        operation()
    } catch (_: Exception) {
        complete(attempt, SessionState.ValidationFailed(AuthFailure.STORAGE, clearing = false))
        null
    }

    private fun networkFailure(error: Exception) =
        if (error is IOException) AuthFailure.NETWORK else AuthFailure.INVALID_RESPONSE

    companion object {
        @Volatile private var instance: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository = instance ?: synchronized(this) {
            instance ?: context.applicationContext.let { app ->
                AuthRepository(ApiClient.createService<AuthApiService>(app), TokenManager(app))
                    .also { instance = it }
            }
        }
    }
}

private val jwtSegment = Regex("[A-Za-z0-9_-]+")

// Checagem estrutural. Somente o servidor confirma assinatura, expiração e identidade.
private fun usableJwt(token: String): Boolean {
    val segments = token.split('.', limit = 4)
    if (segments.size != 3 || segments.any { !jwtSegment.matches(it) }) return false
    return runCatching {
        val decoder = Base64.getUrlDecoder()
        val header = JsonParser.parseString(decoder.decode(segments[0]).toString(Charsets.UTF_8)).asJsonObject
        val claims = JsonParser.parseString(decoder.decode(segments[1]).toString(Charsets.UTF_8))
        val algorithm = header.get("alg")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        !algorithm.isNullOrBlank() && !algorithm.equals("none", ignoreCase = true) &&
            claims.isJsonObject && decoder.decode(segments[2]).isNotEmpty()
    }.getOrDefault(false)
}
