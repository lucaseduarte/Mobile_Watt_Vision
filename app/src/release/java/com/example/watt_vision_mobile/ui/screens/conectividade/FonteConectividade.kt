package com.example.watt_vision_mobile.ui.screens.conectividade

internal fun fonteConectividade() = FonteConectividade {
    val exemplo = cadastroIoTExemplo("2026-11-19 18:20:46")
    ConectividadeUiState(listOf(
        DispositivoConectividade(exemplo, "Planta Industrial A", "Usinagem e Motores", "Motor Elétrico WEG 01")
    ))
}
