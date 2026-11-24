package com.example.watt_vision_mobile.data.repository

import com.example.watt_vision_mobile.data.TokenStore
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.AuthInterceptor
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.model.AuthData
import com.example.watt_vision_mobile.model.LoginRequest
import com.example.watt_vision_mobile.model.LoginResponse
import com.example.watt_vision_mobile.model.UsuarioDto
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepositoryTest {
    private val user = UsuarioDto(1, 2, "Pessoa Fictícia", "teste@example.invalid", "Gestor", "Ativo")
    private val token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0ZSJ9.dGVzdGU"

    private class Storage(var token: String? = null) : TokenStore {
        var saveFails = false
        var readFails = false
        var clearFails = false
        var saves = 0
        var clears = 0

        override fun read(): String? {
            if (readFails) throw IOException()
            return token
        }

        override fun save(token: String) {
            saves++
            this.token = token
            if (saveFails) throw IOException()
        }

        override fun clear() {
            clears++
            if (clearFails) throw IOException()
            token = null
        }
    }

    private inner class Api : AuthApiService {
        val loginCalls = AtomicInteger()
        val meCalls = AtomicInteger()
        var onLogin: suspend () -> Response<LoginResponse> = { loginResponse(token) }
        var onMe: suspend () -> Response<ApiResponse<UsuarioDto>> = { meResponse() }

        override suspend fun login(request: LoginRequest): Response<LoginResponse> {
            loginCalls.incrementAndGet()
            return onLogin()
        }

        override suspend fun getMe(authorization: String?): Response<ApiResponse<UsuarioDto>> {
            meCalls.incrementAndGet()
            assertEquals("Bearer $token", authorization)
            return onMe()
        }
    }

    @Test fun startsSignedOutWithoutTokenOrTraffic() = runBlocking {
        val api = Api()
        val auth = AuthRepository(api, Storage())
        assertSame(SessionState.Initializing, auth.session.value)
        auth.restore()
        assertEquals(SessionState.SignedOut(), auth.session.value)
        assertNull(auth.currentCredential())
        assertEquals(0, api.meCalls.get())
    }

    @Test fun loginValidatesIdentityBeforePersisting() = runBlocking {
        val storage = Storage()
        val api = Api().apply {
            onMe = {
                assertEquals(0, storage.saves)
                meResponse()
            }
        }
        val auth = AuthRepository(api, storage)
        auth.restore()
        auth.login("teste@example.invalid", "ficticia", rememberMe = true)
        assertEquals(user, (auth.session.value as SessionState.Authenticated).user)
        assertEquals(token, storage.token)
        assertEquals(1, api.meCalls.get())
    }

    @Test fun loginWithoutRememberMeKeepsTokenOnlyInMemory() = runBlocking {
        val storage = Storage()
        val auth = AuthRepository(Api(), storage)
        auth.restore()
        auth.login("teste@example.invalid", "ficticia", rememberMe = false)
        assertNotNull(auth.currentCredential())
        assertNull(storage.token)
    }

    @Test fun missingBlankOrMalformedTokensNeverReachMe() = runBlocking {
        for (invalid in listOf(null, "", "abc.def.ghi")) {
            val api = Api().apply { onLogin = { loginResponse(invalid) } }
            val storage = Storage()
            val auth = AuthRepository(api, storage)
            auth.restore()
            auth.login("teste@example.invalid", "ficticia", true)
            assertEquals(SessionState.SignedOut(AuthFailure.INVALID_RESPONSE), auth.session.value)
            assertEquals(0, api.meCalls.get())
            assertNull(storage.token)
        }
    }

    @Test fun requiredFieldsAndInvalidCredentialsDoNotOpenSession() = runBlocking {
        val api = Api().apply { onLogin = { error(401) } }
        val auth = AuthRepository(api, Storage())
        auth.restore()
        auth.login("", "", true)
        assertEquals(0, api.loginCalls.get())
        auth.login("teste@example.invalid", "ficticia", true)
        assertEquals(SessionState.SignedOut(AuthFailure.INVALID_CREDENTIALS), auth.session.value)
        assertEquals(0, api.meCalls.get())
    }

    @Test fun me401RejectsStoredTokenAndMe403PreservesItForRetry() = runBlocking {
        for (code in listOf(401, 403)) {
            val storage = Storage(token)
            val auth = AuthRepository(Api().apply { onMe = { error(code) } }, storage)
            auth.restore()
            assertNull(auth.currentCredential())
            if (code == 401) {
                assertEquals(SessionState.SignedOut(AuthFailure.REJECTED), auth.session.value)
                assertNull(storage.token)
            } else {
                assertEquals(SessionState.ValidationFailed(AuthFailure.FORBIDDEN), auth.session.value)
                assertEquals(token, storage.token)
            }
        }
    }

    @Test fun rejectedOrForbiddenLoginCandidatesAreNeverPersisted() = runBlocking {
        for ((status, expected) in listOf(401 to AuthFailure.REJECTED, 403 to AuthFailure.FORBIDDEN)) {
            val storage = Storage()
            val auth = AuthRepository(Api().apply { onMe = { error(status) } }, storage)
            auth.restore()
            auth.login("teste@example.invalid", "ficticia", true)
            assertEquals(SessionState.SignedOut(expected), auth.session.value)
            assertEquals(0, storage.saves)
            assertNull(auth.currentCredential())
        }
    }

    @Test fun timeoutAndServerFailurePermitRetryWithoutClearingStoredToken() = runBlocking {
        val storage = Storage(token)
        val api = Api().apply { onMe = { throw SocketTimeoutException() } }
        val auth = AuthRepository(api, storage)
        auth.restore()
        assertEquals(SessionState.ValidationFailed(AuthFailure.NETWORK), auth.session.value)
        api.onMe = { error(503) }
        auth.restore()
        assertEquals(SessionState.ValidationFailed(AuthFailure.SERVER), auth.session.value)
        assertEquals(0, storage.clears)
        api.onMe = { meResponse() }
        auth.restore()
        assertEquals(user, (auth.session.value as SessionState.Authenticated).user)
        assertEquals(0, storage.saves)
    }

    @Test fun logoutClearsMemoryAndPersistentStorage() = runBlocking {
        val storage = Storage(token)
        val auth = AuthRepository(Api(), storage)
        auth.restore()
        auth.logout()
        assertEquals(SessionState.SignedOut(), auth.session.value)
        assertNull(storage.token)
        assertNull(auth.currentCredential())
    }

    @Test fun staleValidationDoesNotReplaceAnotherSession() = runBlocking {
        withTimeout(5_000) {
            val entered = CompletableDeferred<Unit>()
            val delayed = CompletableDeferred<Response<ApiResponse<UsuarioDto>>>()
            val api = Api().apply {
                onMe = {
                    entered.complete(Unit)
                    delayed.await()
                }
            }
            val auth = AuthRepository(api, Storage(token))
            val previous = async { auth.restore() }
            entered.await()
            auth.logout()
            api.onMe = { meResponse() }
            auth.login("teste@example.invalid", "ficticia", false)
            val current = auth.session.value
            delayed.complete(meResponse(user.copy(id = 99)))
            previous.await()
            assertSame(current, auth.session.value)
        }
    }

    @Test fun concurrentLoginSubmitsOnce() = runBlocking {
        withTimeout(5_000) {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val api = Api().apply {
                onLogin = {
                    entered.complete(Unit)
                    release.await()
                    loginResponse(token)
                }
            }
            val auth = AuthRepository(api, Storage())
            auth.restore()
            val first = async { auth.login("teste@example.invalid", "ficticia", true) }
            entered.await()
            auth.login("outra@example.invalid", "ficticia", true)
            release.complete(Unit)
            first.await()
            assertEquals(1, api.loginCalls.get())
            assertEquals(1, api.meCalls.get())
        }
    }

    @Test fun current401LogsOutButDelayed401And403DoNot() = runBlocking {
        val storage = Storage(token)
        val auth = AuthRepository(Api(), storage)
        auth.restore()
        val old = requireNotNull(auth.currentCredential())
        auth.logout()
        auth.login("teste@example.invalid", "ficticia", true)
        val current = auth.session.value
        auth.rejectIfCurrent(old)
        assertSame(current, auth.session.value)

        var status = 403
        val client = controlledRetrofit(auth) { status }
        val authService = client.create(AuthApiService::class.java)
        assertEquals(403, authService.getMe().code())
        assertSame(current, auth.session.value)
        status = 401
        assertEquals(401, authService.getMe().code())
        assertEquals(SessionState.SignedOut(AuthFailure.REJECTED), auth.session.value)
        assertNull(storage.token)
    }

    @Test fun storageFailuresKeepSessionClosed() = runBlocking {
        val storage = Storage().apply { readFails = true }
        val auth = AuthRepository(Api(), storage)
        auth.restore()
        assertEquals(SessionState.ValidationFailed(AuthFailure.STORAGE), auth.session.value)
        storage.readFails = false
        auth.restore()
        storage.saveFails = true
        auth.login("teste@example.invalid", "ficticia", true)
        assertEquals(SessionState.SignedOut(AuthFailure.STORAGE), auth.session.value)
        assertNull(storage.token)

        storage.saveFails = false
        auth.login("teste@example.invalid", "ficticia", true)
        storage.clearFails = true
        auth.logout()
        assertEquals(SessionState.ValidationFailed(AuthFailure.STORAGE, clearing = true), auth.session.value)
        assertNull(auth.currentCredential())
        storage.clearFails = false
        auth.logout()
        assertEquals(SessionState.SignedOut(), auth.session.value)
    }

    @Test fun businessRequestsCannotReachTransportDuringOrAfterValidation() = runBlocking {
        withTimeout(5_000) {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val api = Api().apply {
                onMe = {
                    entered.complete(Unit)
                    release.await()
                    meResponse()
                }
            }
            val auth = AuthRepository(api, Storage(token))
            val hits = AtomicInteger()
            val client = controlledRetrofit(auth) {
                hits.incrementAndGet()
                200
            }
            val business = client.create(WattVisionApiService::class.java)
            val restoring = async { auth.restore() }
            entered.await()
            assertEquals(SessionState.Validating(login = false), auth.session.value)
            assertTrue(runCatching { business.getResumoMedicoes() }.exceptionOrNull() is IOException)
            release.complete(Unit)
            restoring.await()
            assertTrue(auth.session.value is SessionState.Authenticated)
            assertTrue(runCatching { business.getResumoMedicoes() }.exceptionOrNull() is IOException)
            assertEquals(0, hits.get())
        }
    }

    @Test fun retrofitSendsCandidateOnlyToMeAndLoginWithoutBearer() = runBlocking {
        lateinit var auth: AuthRepository
        val paths = mutableListOf<String>()
        val storage = Storage()
        val gson = com.google.gson.Gson()
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor({ auth.currentCredential() }, { auth.rejectIfCurrent(it) }))
            .addInterceptor { chain ->
                val request = chain.request()
                assertNull(auth.currentCredential())
                paths += request.url.encodedPath
                val json = when (request.url.encodedPath) {
                    "/api/auth/login" -> {
                        assertEquals("POST", request.method)
                        assertNull(request.header("Authorization"))
                        gson.toJson(LoginResponse(true, null, AuthData(token, null, null)))
                    }
                    "/api/auth/me" -> {
                        assertEquals("GET", request.method)
                        assertEquals("Bearer $token", request.header("Authorization"))
                        gson.toJson(ApiResponse(true, null, user))
                    }
                    else -> throw AssertionError("Rota inesperada.")
                }
                okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(200).message("Teste").body(json.toResponseBody()).build()
            }
            .build()
        val api = Retrofit.Builder().baseUrl("https://example.invalid/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApiService::class.java)
        auth = AuthRepository(api, storage)
        auth.restore()
        auth.login("teste@example.invalid", "ficticia", true)
        assertEquals(listOf("/api/auth/login", "/api/auth/me"), paths)
        assertEquals(user, (auth.session.value as SessionState.Authenticated).user)
        assertEquals(token, storage.token)
    }

    private fun controlledRetrofit(auth: AuthRepository, status: () -> Int): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(auth::currentCredential, auth::rejectIfCurrent))
            .addInterceptor { chain ->
                check("Bearer $token" == chain.request().header("Authorization"))
                okhttp3.Response.Builder().request(chain.request())
                    .protocol(Protocol.HTTP_1_1).code(status()).message("Teste")
                    .body("{}".toResponseBody()).build()
            }
            .build()
        return Retrofit.Builder().baseUrl("https://example.invalid/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }

    private fun loginResponse(token: String?) = Response.success(LoginResponse(true, null, AuthData(token, null, null)))
    private fun meResponse(user: UsuarioDto = this.user) = Response.success(ApiResponse(true, null, user))
    private fun <T> error(code: Int): Response<T> = Response.error(code, "{}".toResponseBody())
}
