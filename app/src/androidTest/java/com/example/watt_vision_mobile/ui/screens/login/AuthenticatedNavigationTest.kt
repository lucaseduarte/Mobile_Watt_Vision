package com.example.watt_vision_mobile.ui.screens.login

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import com.example.watt_vision_mobile.BuildConfig
import com.example.watt_vision_mobile.MainActivity
import com.example.watt_vision_mobile.data.TokenStore
import com.example.watt_vision_mobile.data.api.ApiClient
import com.example.watt_vision_mobile.data.api.AuthApiService
import com.example.watt_vision_mobile.data.api.AuthInterceptor
import com.example.watt_vision_mobile.data.api.ConfiguracaoApi
import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.data.repository.AuthFailure
import com.example.watt_vision_mobile.data.repository.AuthRepository
import com.example.watt_vision_mobile.data.repository.SessionState
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalTestApi::class)
class AuthenticatedNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    // Reuse the actual Activity with an isolated transport for its selected endpoint.
    private val repositories = endpointCache<AuthRepository>(AuthRepository::class.java, "repositorios")
    private val clients = endpointCache<Retrofit>(ApiClient::class.java, "clientes")
    private lateinit var endpoint: String
    private var previousRepository: AuthRepository? = null
    private var previousClient: Retrofit? = null
    private var activity: ActivityScenario<MainActivity>? = null
    private lateinit var repository: AuthRepository
    private lateinit var client: Retrofit
    private val store = MemoryStore()
    private val requests = CopyOnWriteArrayList<String>()
    @Volatile private var answerMe = { 200 }
    private val releaseResponse = CountDownLatch(1)

    @Before
    fun installControlledTransport() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            )
        }
        ConfiguracaoApi.restaurar(context)
        endpoint = ConfiguracaoApi.url
        val http = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(
                { repository.currentCredential() },
                { repository.rejectIfCurrent(it) }
            ))
            .addInterceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                // The Dashboard validates the authenticated scope with its own /me
                // request. Only session transitions use an explicit token argument.
                val sessionValidation = path == "/api/auth/me" &&
                    request.tag(Invocation::class.java)?.arguments()?.firstOrNull() != null
                if (path != "/api/auth/me" || sessionValidation) {
                    requests += "${request.method} $path"
                }
                val code = when (path) {
                    "/api/auth/login" -> {
                        check(request.header("Authorization") == null)
                        200
                    }
                    "/api/auth/me" -> {
                        check(request.header("Authorization") == "Bearer $TOKEN")
                        if (sessionValidation) answerMe() else 200
                    }
                    else -> throw IOException("Requisição inesperada no transporte controlado.")
                }
                val body = when {
                    code != 200 -> """{"sucesso":false}"""
                    path == "/api/auth/login" -> """{"sucesso":true,"dados":{"token":"$TOKEN"}}"""
                    else -> """{"sucesso":true,"dados":{"id":1,"id_empresa":2,"nome":"Pessoa Fictícia","email":"teste@example.invalid","perfil":"Gestor"}}"""
                }
                okhttp3.Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(code).message("Teste").body(body.toResponseBody()).build()
            }
            .build()
        client = Retrofit.Builder().baseUrl("https://example.invalid/").client(http)
            .addConverterFactory(GsonConverterFactory.create()).build()
        repository = AuthRepository(client.create(AuthApiService::class.java), store)
        previousRepository = repositories.put(endpoint, repository)
        previousClient = clients.put(endpoint, client)
    }

    @After
    fun closeActivity() {
        releaseResponse.countDown()
        try {
            activity?.close()
        } finally {
            if (::endpoint.isInitialized) {
                if (previousRepository == null) repositories.remove(endpoint)
                else repositories[endpoint] = requireNotNull(previousRepository)
                if (previousClient == null) clients.remove(endpoint)
                else clients[endpoint] = requireNotNull(previousClient)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> endpointCache(owner: Class<*>, field: String): MutableMap<String, T> =
        owner.getDeclaredField(field).apply { isAccessible = true }.get(null) as MutableMap<String, T>

    @Test fun startsOnLoginWithoutCredentialOrRequests() {
        launch()
        expectLogin()
        assertTrue(repository.session.value is SessionState.SignedOut)
        assertTrue(requests.isEmpty())
        assertBusinessBlocked()
    }

    @Test fun loginWaitsForMeAndBusinessStaysBlockedBeforeDuringAndAfter() {
        holdIdentity()
        launch()
        expectLogin()
        assertBusinessBlocked()
        submitLogin()
        awaitIdentityRequest()
        assertEquals(SessionState.Validating(login = true), repository.session.value)
        compose.onNodeWithContentDescription("Dashboard").assertDoesNotExist()
        assertNull(repository.currentCredential())
        assertBusinessBlocked()

        releaseResponse.countDown()
        expectDashboard()
        assertTrue(repository.session.value is SessionState.Authenticated)
        assertBusinessBlocked()
        assertEquals(authRequests, requests.toList())
    }

    @Test fun current401ClearsStoredCredentialAndLeavesLogin() {
        store.token = TOKEN
        answerMe = { 401 }
        launch()
        expectLogin()
        assertEquals(SessionState.SignedOut(AuthFailure.REJECTED), repository.session.value)
        assertTrue("O armazenamento da sessão deve estar limpo.", store.token == null)
        assertNull(repository.currentCredential())
        assertEquals(listOf("GET /api/auth/me"), requests.toList())
    }

    @Test fun forbiddenIdentityDoesNotOpenProtectedNavigation() {
        answerMe = { 403 }
        launch()
        expectLogin()
        submitLogin()
        waitForText(AuthFailure.FORBIDDEN.message)
        expectLogin()
        assertEquals(SessionState.SignedOut(AuthFailure.FORBIDDEN), repository.session.value)
        assertNull(repository.currentCredential())
        assertEquals(authRequests, requests.toList())
    }

    @Test fun temporaryFailureKeepsNavigationClosedAndAllowsRetry() {
        store.token = TOKEN
        answerMe = { 503 }
        launch()
        waitForText("Tentar novamente")
        compose.onNodeWithContentDescription("Dashboard").assertDoesNotExist()
        assertEquals(SessionState.ValidationFailed(AuthFailure.SERVER), repository.session.value)
        assertTrue("A credencial deve ser preservada para nova tentativa.", store.token == TOKEN)

        answerMe = { 200 }
        compose.onNodeWithText("Tentar novamente").performClick()
        expectDashboard()
        assertTrue(repository.session.value is SessionState.Authenticated)
        assertEquals(List(2) { "GET /api/auth/me" }, requests.toList())
    }

    @Test fun dashboardEMedicoesNaoOferecemDadosSimulados() {
        store.token = TOKEN
        launch()
        expectDashboard()
        compose.onNodeWithText("Ver demonstração do gráfico").assertDoesNotExist()
        compose.onNodeWithText("Dados simulados").assertDoesNotExist()

        compose.onNodeWithContentDescription("Medições").performClick()
        waitForText("Medições")
        compose.onNodeWithText("Demonstração").assertDoesNotExist()
        compose.onNodeWithText("Demonstração · Dados simulados").assertDoesNotExist()
        assertBusinessBlocked()
    }

    @Test fun logoutFromProfileReturnsToLoginAndClearsStoredCredential() {
        store.token = TOKEN
        launch()
        expectDashboard()
        compose.onNodeWithContentDescription("Configurações do sistema").performClick()
        compose.onNodeWithText("Usuários").performClick()
        waitForText("Pessoa Fictícia")
        compose.onNodeWithText("Pessoa Fictícia").performClick()
        compose.onNodeWithText("Sair da conta").performScrollTo().performClick()

        expectLogin()
        assertEquals(SessionState.SignedOut(), repository.session.value)
        assertNull(repository.currentCredential())
        assertTrue("O armazenamento da sessão deve estar limpo.", store.token == null)
        assertEquals(listOf("GET /api/auth/me"), requests.toList())
    }

    @Test fun operationalNavigationReturnsToDashboardAfterLogout() {
        store.token = TOKEN
        launch()
        expectDashboard()
        compose.onNodeWithContentDescription("Operacional").performClick()
        compose.onNodeWithContentDescription("Aba Setores, selecionada: false").performClick()
        compose.onNodeWithContentDescription("Aba Setores, selecionada: true").assertIsDisplayed()

        runBlocking { repository.logout() }
        expectLogin()
        submitLogin()
        expectDashboard()
        compose.onNodeWithContentDescription("Operacional").performClick()
        compose.onNodeWithContentDescription("Aba Equipamentos, selecionada: true").assertIsDisplayed()
        assertBusinessBlocked()
        assertEquals(listOf("GET /api/auth/me") + authRequests, requests.toList())
    }

    @Test fun connectivityDoesNotInventDevicesWhenConsultationIsOff() {
        org.junit.Assume.assumeFalse(com.example.watt_vision_mobile.data.api.integracaoLocal)
        store.token = TOKEN
        launch()
        expectDashboard()
        compose.onNodeWithContentDescription("Conectividade").performClick()
        waitForText("Falha ao consultar a conectividade.")
        compose.onNodeWithText("ESP32-006").assertDoesNotExist()
        compose.onNodeWithText("Dados simulados").assertDoesNotExist()
        assertBusinessBlocked()
    }

    @Test fun delayedIdentityCannotRestoreAnEndedSession() {
        holdIdentity()
        launch()
        expectLogin()
        submitLogin()
        awaitIdentityRequest()
        lateinit var pendingLogin: Job
        requireNotNull(activity).onActivity { screen ->
            val model = ViewModelProvider(screen)[endpoint, LoginViewModel::class.java]
            val operation = LoginViewModel::class.java.getDeclaredField("operation")
            operation.isAccessible = true
            pendingLogin = operation.get(model) as Job
        }
        runBlocking { repository.logout() }
        expectLogin()
        releaseResponse.countDown()
        runBlocking { withTimeout(10_000) { pendingLogin.join() } }
        compose.waitForIdle()
        expectLogin()
        assertEquals(SessionState.SignedOut(), repository.session.value)
        assertNull(repository.currentCredential())
        assertEquals(authRequests, requests.toList())
    }

    private fun launch() {
        activity = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun submitLogin() {
        compose.onNodeWithText("seu@empresa.com.br").performTextInput("teste@example.invalid")
        compose.onNodeWithText("Digite sua senha").performTextInput("senha-ficticia")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
    }

    private fun expectLogin() {
        waitForText("Acesse sua conta")
        compose.onNodeWithText("Acesse sua conta").assertExists()
        compose.onNodeWithContentDescription("Dashboard").assertDoesNotExist()
    }

    private fun expectDashboard() {
        compose.waitUntilAtLeastOneExists(hasContentDescription("Dashboard"), timeoutMillis = 10_000)
        compose.onNodeWithContentDescription("Dashboard").assertIsDisplayed()
        compose.onNodeWithText("Acesse sua conta").assertDoesNotExist()
    }

    private fun waitForText(text: String) {
        compose.waitUntilAtLeastOneExists(hasText(text), timeoutMillis = 10_000)
    }

    private fun awaitIdentityRequest() {
        compose.waitUntil(timeoutMillis = 10_000) { requests.size == 2 }
        assertEquals(authRequests, requests.toList())
    }

    private fun holdIdentity() {
        answerMe = {
            if (!releaseResponse.await(20, TimeUnit.SECONDS)) throw IOException("Teste excedeu o prazo.")
            200
        }
    }

    private fun assertBusinessBlocked() = runBlocking {
        val previous = requests.size
        val api = client.create(WattVisionApiService::class.java)
        assertTrue(runCatching { api.getResumoMedicoes() }.exceptionOrNull() is IOException)
        assertEquals(previous, requests.size)
    }

    private class MemoryStore : TokenStore {
        @Volatile var token: String? = null
        override fun read() = token
        override fun save(token: String) { this.token = token }
        override fun clear() { token = null }
    }

    private companion object {
        const val TOKEN = "eyJhbGciOiJIUzI1NiJ9.e30.dGVzdGU"
        val authRequests = listOf("POST /api/auth/login", "GET /api/auth/me")
    }
}
