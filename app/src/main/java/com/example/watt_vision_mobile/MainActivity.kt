package com.example.watt_vision_mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.watt_vision_mobile.ui.components.WattVisionBottomBar
import com.example.watt_vision_mobile.ui.components.DevelopmentModeBar
import com.example.watt_vision_mobile.ui.components.DevelopmentLoginReturn
import com.example.watt_vision_mobile.ui.components.UnavailableDataScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesScreen
import com.example.watt_vision_mobile.ui.screens.configuracoes.ConfiguracoesViewModel
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardScreen
import com.example.watt_vision_mobile.ui.screens.dashboard.DashboardViewModel
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.EquipamentoDetalheScreen
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.EquipamentoDetalheViewModel
import com.example.watt_vision_mobile.ui.screens.login.LoginScreen
import com.example.watt_vision_mobile.ui.screens.login.LoginViewModel
import com.example.watt_vision_mobile.ui.screens.operacional.LocalDetalheScreen
import com.example.watt_vision_mobile.ui.screens.operacional.LocalItem
import com.example.watt_vision_mobile.ui.screens.operacional.OperacionalScreen
import com.example.watt_vision_mobile.ui.screens.operacional.OperacionalViewModel
import com.example.watt_vision_mobile.ui.screens.operacional.SetorDetalheScreen
import com.example.watt_vision_mobile.ui.screens.operacional.SetorItem
import com.example.watt_vision_mobile.ui.screens.placeholder.PlaceholderScreen
import com.example.watt_vision_mobile.ui.theme.WattVisionTheme

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val operacionalViewModel: OperacionalViewModel by viewModels()
    private val equipamentoDetalheViewModel: EquipamentoDetalheViewModel by viewModels()
    private val configuracoesViewModel: ConfiguracoesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WattVisionTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var loginRequested by remember { mutableStateOf(false) }
                var demoSelected by rememberSaveable { mutableStateOf(false) }
                var selectedRoute by remember { mutableStateOf("Dashboard") }
                var selectedEquipamentoId by rememberSaveable { mutableStateOf<String?>(null) }
                var selectedSetorItem by remember { mutableStateOf<SetorItem?>(null) }
                var selectedLocalItem by remember { mutableStateOf<LocalItem?>(null) }

                val canOpenContent = AppAccessPolicy.canOpenContent(
                    debugBuild = BuildConfig.DEBUG,
                    authenticated = isLoggedIn,
                    loginRequested = loginRequested
                )
                val canShowDemo = AppAccessPolicy.canShowDemo(BuildConfig.DEBUG, demoSelected)

                if (!canOpenContent) {
                    BackHandler(enabled = BuildConfig.DEBUG) { loginRequested = false }
                    Column {
                        if (BuildConfig.DEBUG) {
                            DevelopmentLoginReturn(onReturn = { loginRequested = false })
                        }
                        LoginScreen(
                            viewModel = loginViewModel,
                            modifier = Modifier.weight(1f),
                            onLoginSuccess = {
                                Toast.makeText(this@MainActivity, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                isLoggedIn = true
                                selectedRoute = "Dashboard"
                            }
                        )
                    }
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            if (BuildConfig.DEBUG) {
                                DevelopmentModeBar(
                                    selectedRoute = selectedRoute,
                                    demoSelected = demoSelected,
                                    onDemoChanged = { demoSelected = it },
                                    onLoginClick = { isLoggedIn = false; loginRequested = true },
                                    onRouteSelected = { route ->
                                        selectedRoute = route
                                        selectedEquipamentoId = null
                                        selectedSetorItem = null
                                        selectedLocalItem = null
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            WattVisionBottomBar(
                                selectedRoute = selectedRoute,
                                onRouteSelected = { route ->
                                    selectedRoute = route
                                    selectedEquipamentoId = null
                                    selectedSetorItem = null
                                    selectedLocalItem = null
                                }
                            )
                        }
                    ) { innerPadding ->
                        val contentModifier = Modifier.padding(innerPadding)
                        if ((selectedRoute == "Operacional" || selectedRoute == "Configurações") && !canShowDemo) {
                            UnavailableDataScreen(modifier = contentModifier)
                        } else when (selectedRoute) {
                            "Dashboard" -> {
                                DashboardScreen(
                                    viewModel = dashboardViewModel,
                                    onNavigate = { route -> selectedRoute = route },
                                    modifier = contentModifier
                                )
                            }
                            "Operacional" -> {
                                when {
                                    selectedEquipamentoId != null -> {
                                        EquipamentoDetalheScreen(
                                            viewModel = equipamentoDetalheViewModel,
                                            onBackClick = { selectedEquipamentoId = null },
                                            modifier = contentModifier
                                        )
                                    }
                                    selectedSetorItem != null -> {
                                        SetorDetalheScreen(
                                            setor = selectedSetorItem!!,
                                            onBackClick = { selectedSetorItem = null },
                                            modifier = contentModifier
                                        )
                                    }
                                    selectedLocalItem != null -> {
                                        LocalDetalheScreen(
                                            local = selectedLocalItem!!,
                                            onBackClick = { selectedLocalItem = null },
                                            modifier = contentModifier
                                        )
                                    }
                                    else -> {
                                        OperacionalScreen(
                                            viewModel = operacionalViewModel,
                                            onEquipamentoClick = { id ->
                                                equipamentoDetalheViewModel.setEquipamentoId(id)
                                                selectedEquipamentoId = id
                                            },
                                            onSetorClick = { setor ->
                                                selectedSetorItem = setor
                                            },
                                            onLocalClick = { local ->
                                                selectedLocalItem = local
                                            },
                                            onNavigate = { route -> selectedRoute = route },
                                            modifier = contentModifier
                                        )
                                    }
                                }
                            }
                            "Configurações" -> {
                                ConfiguracoesScreen(
                                    viewModel = configuracoesViewModel,
                                    onBackClick = { selectedRoute = "Dashboard" },
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
            }
        }
    }
}
