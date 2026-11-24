package com.example.watt_vision_mobile.ui.screens.configuracoes

data class UsuarioItem(
    val id: Long,
    val nome: String,
    val email: String,
    val perfil: String, // "Administrador" ou "Gestor"
    val local: String = "Acesso global",
    val status: String = "Ativo", // "Ativo" ou "Inativo"
    val ultimoAcesso: String = "—"
)

data class ConfiguracoesGeralState(
    val nomeEmpresa: String = "Watt Vision",
    val fusoHorario: String = "America/Sao_Paulo",
    val tema: String = "Escuro"
) {
    val formatoData: String get() = "DD/MM/AAAA"
}

data class ConfiguracoesUiState(
    val activeTab: Int = 0, // 0 = Geral, 1 = Usuários
    val geral: ConfiguracoesGeralState = ConfiguracoesGeralState(),
    val usuarios: List<UsuarioItem> = listOf(
        UsuarioItem(1L, "Gabriel Santos", "gabriel.santos@wattvision.com", "Administrador", "Acesso global", "Ativo", "19/10/2026, 05:42"),
        UsuarioItem(2L, "Mariana Costa", "mariana.costa@wattvision.com", "Gestor", "Planta Industrial Jandira", "Ativo", "18/10/2026, 13:30"),
        UsuarioItem(3L, "Roberto Silva", "roberto.silva@wattvision.com", "Gestor", "Planta Industrial Osasco", "Inativo", "—")
    ),

)
