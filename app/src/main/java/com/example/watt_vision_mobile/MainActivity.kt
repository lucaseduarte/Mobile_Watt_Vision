package com.example.watt_vision_mobile

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import com.example.watt_vision_mobile.data.api.ConfiguracaoApi
import com.example.watt_vision_mobile.data.api.URL_API_EMULADOR
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
import com.example.watt_vision_mobile.ui.screens.conectividade.fonteConectividadeBackend
import com.example.watt_vision_mobile.ui.screens.conectividade.DispositivoDetalhesScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesViewModel
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardScreen
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
import com.example.watt_vision_mobile.ui.screens.placeholder.PlaceholderScreen
import com.example.watt_vision_mobile.ui.screens.relatorios.RelatoriosScreen
import com.example.watt_vision_mobile.ui.screens.relatorios.RelatoriosViewModel
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme

private const val ROTA_DETALHES_DISPOSITIVO = "Detalhes do dispositivo"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfiguracaoApi.restaurar(applicationContext)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            WattVisionTheme {
                var apiUrl by remember { mutableStateOf(ConfiguracaoApi.url) }
                val usaConfiguracaoWifi = ConfiguracaoApi.permiteConfigurarWifi &&
                    Build.HARDWARE !in listOf("ranchu", "goldfish")
                var editandoIp by rememberSaveable {
                    mutableStateOf(usaConfiguracaoWifi && apiUrl == URL_API_EMULADOR)
                }
                if (editandoIp) {
                    ConfigurarBackendWifi(apiUrl, cancelar = { editandoIp = false }) { ip ->
                        if (!ConfiguracaoApi.salvarWifi(applicationContext, ip)) return@ConfigurarBackendWifi false
                        apiUrl = ConfiguracaoApi.url
                        editandoIp = false
                        true
                    }
                    return@WattVisionTheme
                }
                if (!acessoAoBackendLocal(apiUrl)) return@WattVisionTheme
                val loginViewModel = remember(apiUrl) {
                    ViewModelProvider(this@MainActivity)[apiUrl, LoginViewModel::class.java]
                }
                val alterarIp: (() -> Unit)? = if (usaConfiguracaoWifi) ({ editandoIp = true }) else null
                val session by loginViewModel.session.collectAsState()
                when (val state = session) {
                    is SessionState.Authenticated -> key(state.generation) {
                        ProtectedNavigation(
                            application, state.user, loginViewModel::logout,
                            loginViewModel.protectedStore(state.generation)
                        )
                    }
                    is SessionState.SignedOut -> LoginScreen(viewModel = loginViewModel, onConfigureBackend = alterarIp)
                    is SessionState.Validating -> {
                        if (state.login) LoginScreen(viewModel = loginViewModel)
                        else SessionStatus()
                    }
                    SessionState.Initializing -> SessionStatus()
                    is SessionState.ValidationFailed -> SessionStatus(
                        message = state.failure.message,
                        onRetry = if (state.clearing) loginViewModel::logout else loginViewModel::retryValidation,
                        onLogout = if (state.clearing) null else loginViewModel::logout,
                        onConfigureBackend = if (state.clearing) null else alterarIp
                    )
                }
            }
        }
    }

    @Composable
    private fun acessoAoBackendLocal(apiUrl: String): Boolean {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return true
        val origem = ConfiguracaoApi.origemLocal(apiUrl) ?: return true
        if (origem.host == "127.0.0.1") return true

        val permissao = Manifest.permission.ACCESS_LOCAL_NETWORK
        var concedida by remember { mutableStateOf(checkSelfPermission(permissao) == PackageManager.PERMISSION_GRANTED) }
        var tentativas by rememberSaveable { mutableIntStateOf(0) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            tentativas++
            concedida = checkSelfPermission(permissao) == PackageManager.PERMISSION_GRANTED
        }
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, evento ->
                if (evento == Lifecycle.Event.ON_RESUME) {
                    concedida = checkSelfPermission(permissao) == PackageManager.PERMISSION_GRANTED
                }
            }
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }
        if (concedida) return true

        val abrirConfiguracoes = tentativas > 0 && !shouldShowRequestPermissionRationale(permissao)
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text("O Watt Vision precisa de acesso à rede local para consultar o Backend deste notebook.")
                Button(onClick = {
                    if (abrirConfiguracoes) {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
                    } else {
                        launcher.launch(permissao)
                    }
                }) {
                    Text(if (abrirConfiguracoes) "Abrir configurações" else "Permitir acesso à rede local")
                }
            }
        }
        return false
    }

    @Composable
    private fun ConfigurarBackendWifi(apiUrl: String, cancelar: () -> Unit, salvar: (String) -> Boolean) {
        var ip by rememberSaveable { mutableStateOf(
            ConfiguracaoApi.origemLocal(apiUrl)?.host?.takeIf { apiUrl != URL_API_EMULADOR }.orEmpty()
        ) }
        var invalido by rememberSaveable { mutableStateOf(false) }
        Surface(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text("Backend por Wi-Fi", style = MaterialTheme.typography.headlineSmall)
                Text("Conecte o celular e o notebook à mesma rede. Informe o IPv4 do notebook para acessar a porta 3000.")
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it; invalido = false },
                    label = { Text("IPv4 do notebook") },
                    placeholder = { Text("192.168.1.20") },
                    singleLine = true,
                    isError = invalido,
                    supportingText = if (invalido) ({ Text("Informe um endereço IPv4 de rede privada.") }) else null
                )
                Button(onClick = { invalido = !salvar(ip) }) { Text("Salvar endereço") }
                if (apiUrl != URL_API_EMULADOR) {
                    TextButton(onClick = cancelar) { Text("Cancelar") }
                }
            }
        }
    }

    @Composable
    private fun SessionStatus(
        message: String? = null,
        onRetry: (() -> Unit)? = null,
        onLogout: (() -> Unit)? = null,
        onConfigureBackend: (() -> Unit)? = null
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
                    onConfigureBackend?.let { TextButton(onClick = it) { Text("Configurar Backend Wi-Fi") } }
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
                    ConectividadeViewModel::class.java -> ConectividadeViewModel(fonteConectividadeBackend(application))
                    else -> return super.create(modelClass)
                }
                return requireNotNull(modelClass.cast(model))
            }
        }
        ViewModelProvider(store, factory)
    }
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
                DashboardScreen(
                    viewModel = dashboardViewModel,
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
                    modifier = contentModifier
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
