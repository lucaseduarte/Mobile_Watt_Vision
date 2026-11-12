package com.example.watt_vision_mobile.ui.screens.configuracoes

data class TarifaItem(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val inicio: String,
    val fim: String = "",
    val ativa: Boolean = false
)

data class UsuarioItem(
    val id: Long,
    val nome: String,
    val email: String,
    val perfil: String, // "Administrador" ou "Gestor"
    val local: String = "Acesso global",
    val status: String = "Ativo", // "Ativo" ou "Inativo"
    val ultimoAcesso: String = "—"
)

data class PontoMedicaoItem(
    val id: Long,
    val nome: String,
    val escopo: String, // "Equipamento", "Setor", "Local"
    val entidade: String,
    val dispositivo: String,
    val modelo: String, // "PZEM-004T V3", "PZEM-016", "Outro"
    val ativo: Boolean = true
)

data class ConfiguracoesGeralState(
    val nomeEmpresa: String = "Watt Vision",
    val fusoHorario: String = "America/Sao_Paulo",
    val formatoData: String = "DD/MM/YYYY",
    val tema: String = "Escuro"
)

data class ConfiguracoesUiState(
    val activeTab: Int = 0, // 0 = Geral, 1 = Tarifas, 2 = Usuários, 3 = Pontos de Medição
    val geral: ConfiguracoesGeralState = ConfiguracoesGeralState(),
    val tarifas: List<TarifaItem> = listOf(
        TarifaItem(1L, "Tarifa 2026 — Convencional", 0.80, "2026-01-01", "", true),
        TarifaItem(2L, "Tarifa 2027 (futura, prevista)", 0.87, "2027-01-01", "", false)
    ),
    val usuarios: List<UsuarioItem> = listOf(
        UsuarioItem(1L, "Gabriel Santos", "gabriel.santos@wattvision.com", "Administrador", "Acesso global", "Ativo", "19/10/2026, 05:42"),
        UsuarioItem(2L, "Mariana Costa", "mariana.costa@wattvision.com", "Gestor", "Planta Industrial Jandira", "Ativo", "18/10/2026, 13:30"),
        UsuarioItem(3L, "Roberto Silva", "roberto.silva@wattvision.com", "Gestor", "Planta Industrial Osasco", "Inativo", "—")
    ),
    val pontosMedicao: List<PontoMedicaoItem> = listOf(
        PontoMedicaoItem(1L, "Painel Elétrico PE-12", "Equipamento", "PE-12", "PZEM-004T", "PZEM-004T V3", true),
        PontoMedicaoItem(2L, "Elétrica", "Setor", "Elétrica", "PZEM-004T", "PZEM-004T V3", true),
        PontoMedicaoItem(3L, "Subestação Central", "Local", "Subestação Central", "PZEM-004T", "PZEM-004T V3", false)
    ),
    val mostrarFormTarifa: Boolean = false,
    val tarifaEditandoId: Long? = null,
    val mostrarFormUsuario: Boolean = false,
    val usuarioEditandoId: Long? = null,
    val mostrarFormPonto: Boolean = false,
    val pontoEditandoId: Long? = null
)
