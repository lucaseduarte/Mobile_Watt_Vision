package com.example.watt_vision_mobile

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.watt_vision_mobile.data.repository.SessionState
import com.example.watt_vision_mobile.model.UsuarioDto
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeTipoEntidade
import com.example.watt_vision_mobile.ui.components.WattVisionBottomBar
import com.example.watt_vision_mobile.ui.screens.conectividade.ConectividadeScreen
import com.example.watt_vision_mobile.ui.screens.conectividade.ConectividadeViewModel
import com.example.watt_vision_mobile.ui.screens.conectividade.DispositivoDetalhesScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesViewModel
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardComSerieReal
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardViewModel
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.EquipamentoDetalheScreen
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.EquipamentoDetalheViewModel
import com.example.watt_vision_mobile.ui.screens.login.LoginScreen
import com.example.watt_vision_mobile.ui.screens.login.LoginViewModel
import com.example.watt_vision_mobile.ui.screens.operacional.EntidadeOperacionalScreen
import com.example.watt_vision_mobile.ui.screens.operacional.LocalDetalheScreen
import com.example.watt_vision_mobile.ui.screens.operacional.OperacionalDetalhe
import com.example.watt_vision_mobile.ui.screens.operacional.OperacionalScreen
import com.example.watt_vision_mobile.ui.screens.operacional.OperacionalViewModel
import com.example.watt_vision_mobile.ui.screens.operacional.SetorDetalheScreen
import com.example.watt_vision_mobile.ui.screens.alarmes.AlarmesScreen
import com.example.watt_vision_mobile.ui.screens.alarmes.AlarmesViewModel
import com.example.watt_vision_mobile.ui.screens.medicoes.MedicoesScreen
import com.example.watt_vision_mobile.ui.screens.medicoes.SerieRealViewModel
import com.example.watt_vision_mobile.ui.screens.placeholder.PlaceholderScreen
import com.example.watt_vision_mobile.ui.screens.relatorios.RelatoriosScreen
import com.example.watt_vision_mobile.ui.screens.relatorios.RelatoriosViewModel
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme

private const val ROTA_DETALHES_DISPOSITIVO = "Detalhes do dispositivo"

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            WattVisionTheme {
                val session by loginViewModel.session.collectAsState()
                when (val state = session) {
                    is SessionState.Authenticated -> key(state.generation) {
                        ProtectedNavigation(
                            application, state.user, loginViewModel::logout,
                            loginViewModel.protectedStore(state.generation)
                        )
                    }
                    is SessionState.SignedOut -> LoginScreen(viewModel = loginViewModel)
                    is SessionState.Validating -> {
                        if (state.login) LoginScreen(viewModel = loginViewModel)
                        else SessionStatus()
                    }
                    SessionState.Initializing -> SessionStatus()
                    is SessionState.ValidationFailed -> SessionStatus(
                        message = state.failure.message,
                        onRetry = if (state.clearing) loginViewModel::logout else loginViewModel::retryValidation,
                        onLogout = if (state.clearing) null else loginViewModel::logout
                    )
                }
            }
        }
    }

    @Composable
    private fun SessionStatus(
        message: String? = null,
        onRetry: (() -> Unit)? = null,
        onLogout: (() -> Unit)? = null
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (message == null) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Validando sessão" })
                } else {
                    Text(message)
                    onRetry?.let { TextButton(onClick = it) { Text("Tentar novamente") } }
                    onLogout?.let { TextButton(onClick = it) { Text("Sair da conta") } }
                }
            }
        }
    }
}

@Composable
internal fun ProtectedNavigation(
    application: Application,
    user: UsuarioDto,
    onLogout: () -> Unit,
    sessionStore: ViewModelStore? = null
) {
    val store = sessionStore ?: remember { ViewModelStore() }
    DisposableEffect(store) {
        onDispose { if (sessionStore == null) store.clear() }
    }
    val provider = remember(store) {
        val factory = object : ViewModelProvider.AndroidViewModelFactory(application) {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val model = when (modelClass) {
                    DashboardViewModel::class.java -> DashboardViewModel(application, SavedStateHandle())
                    EquipamentoDetalheViewModel::class.java -> EquipamentoDetalheViewModel(application)
                    OperacionalViewModel::class.java -> OperacionalViewModel(application, SavedStateHandle())
                    ConectividadeViewModel::class.java -> ConectividadeViewModel()
                    else -> return super.create(modelClass)
                }
                return requireNotNull(modelClass.cast(model))
            }
        }
        ViewModelProvider(store, factory)
    }
    val serieReal by remember(provider) { lazy { provider[SerieRealViewModel::class.java] } }
    val dashboardViewModel by remember(provider) { lazy { provider[DashboardViewModel::class.java] } }
    val operacionalViewModel by remember(provider) { lazy { provider[OperacionalViewModel::class.java] } }
    val equipamentoDetalheViewModel by remember(provider) { lazy { provider[EquipamentoDetalheViewModel::class.java] } }
    val configuracoesViewModel by remember(provider) { lazy { provider[ConfiguracoesViewModel::class.java] } }
    val alarmesViewModel by remember(provider) { lazy { provider[AlarmesViewModel::class.java] } }
    val relatoriosViewModel by remember(provider) { lazy { provider[RelatoriosViewModel::class.java] } }
    val conectividadeViewModel by remember(provider) { lazy { provider[ConectividadeViewModel::class.java] } }

    val routeState = rememberSaveableStateHolder()
    var selectedRoute by remember { mutableStateOf("Dashboard") }
    var selectedEquipamentoId by remember { mutableStateOf<String?>(null) }
    var selectedSetorId by remember { mutableStateOf<String?>(null) }
    var entidadeDoAlarme by remember { mutableStateOf<AlarmeEntidade?>(null) }
    var selectedLocalId by remember { mutableStateOf<String?>(null) }
    var selectedDispositivoId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (selectedRoute != ROTA_DETALHES_DISPOSITIVO) {
                WattVisionBottomBar(
                    selectedRoute = selectedRoute,
                    onRouteSelected = { route ->
                        selectedRoute = route
                        selectedEquipamentoId = null
                        selectedSetorId = null
                        selectedLocalId = null
                        entidadeDoAlarme = null
                    }
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (selectedRoute) {
            "Dashboard" -> {
                DashboardComSerieReal(
                    viewModel = dashboardViewModel,
                    serieReal = serieReal,
                    onNavigate = { route -> selectedRoute = route },
                    modifier = contentModifier
                )
            }
            "Operacional" -> {
                val entidade = entidadeDoAlarme
                val voltarAosAlarmes = {
                    entidadeDoAlarme = null
                    selectedRoute = "Alarmes"
                }
                when {
                    entidade?.tipo == AlarmeTipoEntidade.EQUIPAMENTO -> {
                        EquipamentoDetalheScreen(
                            viewModel = equipamentoDetalheViewModel,
                            onBackClick = voltarAosAlarmes,
                            modifier = contentModifier,
                            entidadeDoAlarme = entidade
                        )
                    }
                    entidade != null -> {
                        EntidadeOperacionalScreen(
                            entidade = entidade,
                            onBackClick = voltarAosAlarmes,
                            modifier = contentModifier
                        )
                    }
                    selectedEquipamentoId != null -> {
                        val state by operacionalViewModel.uiState.collectAsState()
                        val voltar = { selectedEquipamentoId = null }
                        OperacionalDetalhe(
                            state, state.equipamentos.firstOrNull { it.id == selectedEquipamentoId },
                            voltar, operacionalViewModel::loadData, contentModifier
                        ) { equipamento ->
                            EquipamentoDetalheScreen(
                                viewModel = equipamentoDetalheViewModel,
                                onBackClick = voltar,
                                modifier = contentModifier,
                                equipamento = equipamento
                            )
                        }
                    }
                    selectedSetorId != null -> {
                        val state by operacionalViewModel.uiState.collectAsState()
                        val voltar = { selectedSetorId = null }
                        OperacionalDetalhe(
                            state, state.setores.firstOrNull { it.id == selectedSetorId },
                            voltar, operacionalViewModel::loadData, contentModifier
                        ) { setor ->
                            SetorDetalheScreen(setor, voltar, contentModifier)
                        }
                    }
                    selectedLocalId != null -> {
                        val state by operacionalViewModel.uiState.collectAsState()
                        val voltar = { selectedLocalId = null }
                        OperacionalDetalhe(
                            state, state.locais.firstOrNull { it.id == selectedLocalId },
                            voltar, operacionalViewModel::loadData, contentModifier
                        ) { local ->
                            LocalDetalheScreen(local, voltar, contentModifier)
                        }
                    }
                    else -> {
                        OperacionalScreen(
                            viewModel = operacionalViewModel,
                            onEquipamentoClick = { selectedEquipamentoId = it },
                            onSetorClick = { selectedSetorId = it.id },
                            onLocalClick = { selectedLocalId = it.id },
                            onNavigate = { route -> selectedRoute = route },
                            modifier = contentModifier
                        )
                    }
                }
            }
            "Configurações" -> {
                ConfiguracoesScreen(
                    viewModel = configuracoesViewModel,
                    carregarConta = { user },
                    onBackClick = { selectedRoute = "Dashboard" },
                    modifier = contentModifier,
                    onLogout = onLogout
                )
            }
            "Alarmes" -> routeState.SaveableStateProvider("Alarmes") {
                AlarmesScreen(
                    viewModel = alarmesViewModel,
                    onEntidadeClick = { entidade ->
                        entidadeDoAlarme = entidade
                        selectedRoute = "Operacional"
                    },
                    modifier = contentModifier
                )
            }
            "Conectividade" -> routeState.SaveableStateProvider("Conectividade") {
                ConectividadeScreen(
                    viewModel = conectividadeViewModel,
                    onNavigate = { route -> selectedRoute = route },
                    onVerDetalhes = { id ->
                        selectedDispositivoId = id
                        selectedRoute = ROTA_DETALHES_DISPOSITIVO
                    },
                    modifier = contentModifier
                )
            }
            ROTA_DETALHES_DISPOSITIVO -> {
                val estado by conectividadeViewModel.estado.collectAsState()
                DispositivoDetalhesScreen(
                    dispositivo = estado.dispositivos.firstOrNull { it.id == selectedDispositivoId },
                    eventos = estado.eventos,
                    onVoltar = {
                        selectedDispositivoId = null
                        selectedRoute = "Conectividade"
                    },
                    modifier = contentModifier
                )
            }
            "Relatórios" -> routeState.SaveableStateProvider("Relatórios") {
                RelatoriosScreen(viewModel = relatoriosViewModel, modifier = contentModifier)
            }
            "Medições" -> routeState.SaveableStateProvider("Medições") {
                MedicoesScreen(
                    onNavigate = { route -> selectedRoute = route },
                    modifier = contentModifier,
                    serieReal = serieReal
                )
            }
            else -> {
                PlaceholderScreen(
                    title = selectedRoute,
                    onBackToDashboard = { selectedRoute = "Dashboard" },
                    modifier = contentModifier
                )
            }
        }
    }
}
