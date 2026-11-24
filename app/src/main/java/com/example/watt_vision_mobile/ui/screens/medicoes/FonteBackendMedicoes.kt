package com.example.watt_vision_mobile.ui.screens.medicoes

import android.content.Context
import com.example.watt_vision_mobile.data.repository.DashboardRepository

internal suspend fun carregarFonteBackend(context: Context): FonteSerieSimulada {
    val repositorio = DashboardRepository(context)
    val catalogo = repositorio.getChartCatalog().getOrThrow()
    return object : FonteSerieSimulada {
        override val catalogo = catalogo

        override suspend fun consultar(pzem: Int, horas: Int) =
            repositorio.getChartSerieTemporal(pzem, horas).getOrThrow()
    }
}
