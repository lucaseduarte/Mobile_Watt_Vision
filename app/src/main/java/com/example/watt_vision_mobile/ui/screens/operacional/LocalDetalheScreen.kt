package com.example.watt_vision_mobile.ui.screens.operacional

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.AlertaDetalheItem
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.AlertaSeveridade
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.AlarmesSecao
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoCampo
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoGrupoCard
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.components.InfoLinha
import com.example.watt_vision_mobile.ui.screens.operacional.components.StatusBadge
import com.example.watt_vision_mobile.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDetalheScreen(
    local: LocalItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consumoStr = String.format(Locale.getDefault(), "%.1f kWh", local.consumoAcumuladoKwh)

    val sampleAlarmes = listOf(
        AlertaDetalheItem(
            id = "1",
            titulo = "Falha de comunicação na unidade",
            severidade = AlertaSeveridade.ALERTA,
            condicao = "Sensor PZEM sem resposta há > 5 min",
            dataHoraFormatada = "Hoje, 11:20"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = local.nome,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${local.codigo}  ·  Unidade Operacional",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoGrupoCard(
                    titulo = "Status Operacional",
                    indicador = { StatusBadge(status = local.status) }
                ) {
                    InfoLinha {
                        InfoCampo("Setores Vinculados", "${local.setoresVinculados} setores", Modifier.weight(1f))
                        InfoCampo("Equipamentos", "${local.equipamentosVinculados} unidades", Modifier.weight(1f))
                    }
                    InfoCampo("Consumo Acumulado", consumoStr, corValor = ToneCyanText)
                }
            }

            // Seção de Alarmes do Local
            item {
                AlarmesSecao(alarmes = sampleAlarmes)
            }

            // Identificação
            item {
                InfoGrupoCard(titulo = "Identificação do Local") {
                    InfoCampo(rotulo = "Nome do Local", valor = local.nome)
                    InfoCampo(rotulo = "Código / Identificação", valor = local.codigo, copiavel = true)
                    InfoCampo(rotulo = "Endereço Completo", valor = "Av. Industrial, 1200 - Unidade Principal")
                }
            }

            // Métricas e Responsável
            item {
                InfoGrupoCard(titulo = "Dados Operacionais") {
                    InfoLinha {
                        InfoCampo("Setores Registrados", "${local.setoresVinculados} setores", Modifier.weight(1f))
                        InfoCampo("Total de Dispositivos", "${local.equipamentosVinculados} equipamentos", Modifier.weight(1f))
                    }
                    InfoCampo(rotulo = "Responsável Operacional", valor = "Carlos Eduardo (Gestão)")
                }
            }
        }
    }
}
