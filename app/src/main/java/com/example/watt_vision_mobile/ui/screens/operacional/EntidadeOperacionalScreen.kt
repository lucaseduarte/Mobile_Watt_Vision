package com.example.watt_vision_mobile.ui.screens.operacional

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.alarmes.AlarmeTipoEntidade
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoCampo
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoGrupoCard
import com.example.watt_vision_mobile.ui.theme.DarkBackground
import com.example.watt_vision_mobile.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntidadeOperacionalScreen(
    entidade: AlarmeEntidade,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text(entidade.nome) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar aos alarmes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextWhite,
                    navigationIconContentColor = TextWhite
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoGrupoCard(titulo = entidade.tipo.label) {
                InfoCampo(rotulo = "Nome", valor = entidade.nome)
                InfoCampo(rotulo = "Identificador", valor = entidade.id.toString(), copiavel = true)
                if (entidade.tipo == AlarmeTipoEntidade.EQUIPAMENTO) {
                    InfoCampo(rotulo = "Setor", valor = entidade.setor ?: "Não informado")
                }
                if (entidade.tipo != AlarmeTipoEntidade.LOCAL) {
                    InfoCampo(rotulo = "Local", valor = entidade.local ?: "Não informado")
                }
            }
        }
    }
}
