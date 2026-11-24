package com.example.watt_vision_mobile.ui.screens.operacional

import android.app.Application
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.watt_vision_mobile.data.api.ChartCatalogApiService
import com.example.watt_vision_mobile.data.repository.RetrofitOperacionalRepository
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OperacionalCatalogoTest {
    @get:Rule val compose = createComposeRule()

    private val store = ViewModelStore()
    private val requests = CopyOnWriteArrayList<String>()
    private val release = CountDownLatch(1)

    @After
    fun clear() {
        release.countDown()
        compose.runOnUiThread { store.clear() }
    }

    @Test
    fun loadsCatalogAndKeepsNavigationAndFilters() {
        val viewModel = launch { path ->
            when (path) {
                "/api/locais" -> {
                    if (!release.await(10, TimeUnit.SECONDS)) throw IOException("Resposta de teste não liberada.")
                    200 to """[{"id":10,"codigo":"L-10","nome":"Fábrica Sul","status":"Ativo"}]"""
                }
                "/api/setores" -> 200 to """[{"id":20,"id_local":10,"codigo":"S-20","nome":"Usinagem","status":"Ativo"}]"""
                "/api/equipamentos" -> 200 to """[{"id":30,"id_setor":20,"codigo":"E-30","nome":"Prensa","potencia_nominal":"1250.00","status":"Inativo"}]"""
                else -> error("Requisição inesperada: $path")
            }
        }

        compose.waitUntil(5_000) { requests.isNotEmpty() }
        assertTrue(viewModel.uiState.value.isLoading)
        release.countDown()
        compose.waitUntil(5_000) { !viewModel.uiState.value.isLoading }
        compose.onNodeWithText("Prensa").assertIsDisplayed()

        compose.runOnIdle {
            val state = viewModel.uiState.value
            assertEquals("Fábrica Sul", state.equipamentos.single().localNome)
            assertEquals("Usinagem", state.equipamentos.single().setorNome)
            assertEquals("10", state.setores.single().localId)
            assertEquals("20", state.equipamentos.single().setorId)
            assertEquals(1250.0, state.equipamentos.single().potenciaW)
            assertNull(state.equipamentos.single().consumoKwh)
            assertEquals(1, state.setores.single().equipamentosVinculados)
            assertEquals(1, state.locais.single().setoresVinculados)
            assertEquals(1, state.locais.single().equipamentosVinculados)
            viewModel.updateSearchQuery(0, "E-30")
            assertEquals("30", viewModel.getFilteredEquipamentos().single().id)
            viewModel.updateStatusFilter(0, "Ativo")
            assertTrue(viewModel.getFilteredEquipamentos().isEmpty())
        }

        compose.onNodeWithContentDescription("Aba Setores, selecionada: false").performClick()
        compose.onNodeWithText("Usinagem").assertIsDisplayed()
        compose.onNodeWithContentDescription("Aba Locais, selecionada: false").performClick()
        compose.onNodeWithText("Fábrica Sul").assertIsDisplayed()
        assertEquals(listOf("/api/locais", "/api/setores", "/api/equipamentos"), requests)
    }

    @Test
    fun displaysErrorThenEmptyCatalogAfterRetry() {
        val first = AtomicBoolean(true)
        val viewModel = launch { path ->
            if (path == "/api/locais" && first.getAndSet(false)) 503 to "[]" else 200 to "[]"
        }

        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage != null }
        compose.onNodeWithText("Erro ao carregar dados operacionais").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitUntil(5_000) { requests.size == 4 && !viewModel.uiState.value.isLoading }
        compose.onNodeWithText(
            "Nenhum registro cadastrado. Os cadastros são feitos pela versão web."
        ).assertIsDisplayed()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun discardsPartialCatalogAndRetriesAllLists() {
        val first = AtomicBoolean(true)
        val viewModel = launch { path ->
            when (path) {
                "/api/locais" -> 200 to """[{"id":10,"nome":"Fábrica Sul"}]"""
                "/api/setores" -> if (first.getAndSet(false)) 503 to "[]" else 200 to "[]"
                "/api/equipamentos" -> 200 to "[]"
                else -> error("Requisição inesperada: $path")
            }
        }
        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage != null }
        assertTrue(viewModel.uiState.value.locais.isEmpty())
        assertEquals(listOf("/api/locais", "/api/setores"), requests.toList())
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitUntil(5_000) { viewModel.uiState.value.locais.isNotEmpty() }
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf("/api/locais", "/api/setores", "/api/locais", "/api/setores", "/api/equipamentos"), requests.toList())
    }

    @Test
    fun clearingSessionStoreDiscardsPendingCatalog() {
        val finished = CountDownLatch(1)
        val viewModel = launch { path ->
            check(path == "/api/locais")
            try {
                release.await(10, TimeUnit.SECONDS)
                200 to """[{"id":10,"nome":"Fábrica Sul"}]"""
            } finally {
                finished.countDown()
            }
        }
        compose.waitUntil(5_000) { requests.isNotEmpty() }
        assertTrue(viewModel.uiState.value.isLoading)
        compose.runOnUiThread { store.clear() }
        release.countDown()
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        compose.waitForIdle()
        assertEquals(OperacionalUiState(), viewModel.uiState.value)
        assertEquals(listOf("/api/locais"), requests.toList())
    }

    @Test
    fun rejectsEquipmentWithAnUnknownSector() {
        val viewModel = launch { path ->
            when (path) {
                "/api/locais" -> 200 to """[{"id":10,"nome":"Fábrica Sul"}]"""
                "/api/setores" -> 200 to """[{"id":20,"id_local":10,"nome":"Usinagem"}]"""
                "/api/equipamentos" -> 200 to """[{"id":30,"id_setor":99,"nome":"Prensa"}]"""
                else -> error("Requisição inesperada: $path")
            }
        }
        compose.waitUntil(5_000) { viewModel.uiState.value.errorMessage != null }
        assertTrue(viewModel.uiState.value.equipamentos.isEmpty())
        assertTrue(viewModel.uiState.value.setores.isEmpty())
        assertTrue(viewModel.uiState.value.locais.isEmpty())
        compose.onNodeWithText("Tentar novamente").assertIsDisplayed()
    }

    @Test
    fun showsMissingRecordAfterCatalogLoads() {
        val viewModel = launch(showMissingRecord = true) { path ->
            when (path) {
                "/api/locais" -> 200 to """[{"id":10,"nome":"Fábrica Sul"}]"""
                "/api/setores" -> 200 to """[{"id":20,"id_local":10,"nome":"Usinagem"}]"""
                "/api/equipamentos" -> 200 to """[{"id":30,"id_setor":20,"nome":"Prensa"}]"""
                else -> error("Requisição inesperada: $path")
            }
        }
        compose.waitUntil(5_000) { viewModel.uiState.value.equipamentos.isNotEmpty() }
        compose.onNodeWithText("Cadastro não encontrado.").assertIsDisplayed()
        compose.onNodeWithText("Voltar").assertIsDisplayed()
    }

    private fun launch(
        showMissingRecord: Boolean = false,
        reply: (String) -> Pair<Int, String>
    ): OperacionalViewModel {
        val transport = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            check(request.method == "GET")
            val path = request.url.encodedPath
            requests += path
            val (status, rows) = reply(path)
            val body = """{"sucesso":${status == 200},"dados":$rows}"""
            okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("Teste")
                .body(body.toResponseBody())
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl("https://example.invalid/")
            .client(transport)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChartCatalogApiService::class.java)
        val application = ApplicationProvider.getApplicationContext<Application>()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                OperacionalViewModel(application, SavedStateHandle(), RetrofitOperacionalRepository(api)) as T
        }
        lateinit var viewModel: OperacionalViewModel
        compose.runOnUiThread {
            viewModel = ViewModelProvider(store, factory)[OperacionalViewModel::class.java]
        }
        compose.setContent {
            WattVisionTheme {
                if (showMissingRecord) {
                    val state = viewModel.uiState.collectAsState().value
                    OperacionalDetalhe(
                        state, state.equipamentos.firstOrNull { it.id == "999" },
                        {}, viewModel::loadData, Modifier
                    ) { error("Um ID inexistente não pode abrir um registro.") }
                } else {
                    OperacionalScreen(viewModel)
                }
            }
        }
        return viewModel
    }
}
