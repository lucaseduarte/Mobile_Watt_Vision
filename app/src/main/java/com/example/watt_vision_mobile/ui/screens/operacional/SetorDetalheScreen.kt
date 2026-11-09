package com.example.watt_vision_mobile.ui.screens.operacional

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.watt_vision_mobile.ui.screens.operacional.components.StatusBadge
import com.example.watt_vision_mobile.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetorDetalheScreen(
    setor: SetorItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consumoStr = String.format(Locale.getDefault(), "%.1f kWh", setor.consumoAcumuladoKwh)

    val sampleAlarmes = listOf(
        AlertaDetalheItem(
            id = "1",
            titulo = "Sobretensão detectada",
            severidade = AlertaSeveridade.CRITICO,
            condicao = "Tensão > 250 V (atual: 263 V)",
            dataHoraFormatada = "Hoje, 14:32"
        ),
        AlertaDetalheItem(
            id = "2",
            titulo = "Corrente elevada",
            severidade = AlertaSeveridade.ALERTA,
            condicao = "Corrente > 80 A (atual: 84,3 A)",
            dataHoraFormatada = "Hoje, 12:15"
        )
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
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
                            text = setor.nome,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${setor.codigo}  ·  Local: ${setor.localNome}",
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
            // Card Resumo
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status Operacional",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            StatusBadge(status = setor.status)
                        }

                        HorizontalDivider(color = CardBorder, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Equipamentos", fontSize = 10.sp, color = TextMuted)
                                Text(text = "${setor.equipamentosVinculados} unidades", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Consumo Acumulado", fontSize = 10.sp, color = TextMuted)
                                Text(text = consumoStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ToneCyanText)
                            }
                        }
                    }
                }
            }

            // Seção de Alarmes do Setor
            item {
                AlarmesSecao(alarmes = sampleAlarmes)
            }

            // Identificação
            item {
                InfoGrupoCard(titulo = "Identificação do Setor") {
                    InfoCampo(rotulo = "Nome do Setor", valor = setor.nome)
                    InfoCampo(rotulo = "Código / Identificação", valor = setor.codigo, copiavel = true)
                    InfoCampo(rotulo = "Local de Pertencimento", valor = setor.localNome)
                }
            }

            // Métricas e Pertença
            item {
                InfoGrupoCard(titulo = "Dados Operacionais") {
                    InfoCampo(rotulo = "Dispositivos Vinculados", valor = "${setor.equipamentosVinculados} equipamentos")
                    InfoCampo(rotulo = "Consumo Periódico", valor = consumoStr)
                    InfoCampo(rotulo = "Responsável Técnico", valor = "João Pedro (Operações)")
                }
            }
        }
    }
}
