package com.example.watt_vision_mobile.ui.screens.medicoes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watt_vision_mobile.ui.theme.*

private data class PontoMedicaoItem(
    val id: Long,
    val nome: String,
    val escopo: String, // "Equipamento", "Setor", "Local"
    val entidade: String,
    val dispositivo: String,
    val modelo: String, // "PZEM-004T V3", "PZEM-016", "Outro"
    val ativo: Boolean = true
)

private val pontosExemplo = listOf(
    PontoMedicaoItem(1L, "Painel Elétrico PE-12", "Equipamento", "PE-12", "PZEM-004T", "PZEM-004T V3", true),
    PontoMedicaoItem(2L, "Elétrica", "Setor", "Elétrica", "PZEM-004T", "PZEM-004T V3", true),
    PontoMedicaoItem(3L, "Subestação Central", "Local", "Subestação Central", "PZEM-004T", "PZEM-004T V3", false)
)

@Composable
internal fun BotaoPontosMedicao(modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF3768C5)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF101E36),
            contentColor = Color(0xFF8EB5FF)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text("Pontos de Medição", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PontosMedicaoSheet(onFechar: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceDark,
        contentColor = TextWhite
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pontos de Medição", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onFechar) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar pontos de medição", tint = TextMuted)
                }
            }
            HorizontalDivider(color = CardBorder)
            ListaPontosMedicao()
        }
    }
}

@Composable
private fun ListaPontosMedicao() {
    val totalAtivos = pontosExemplo.count { it.ativo }
    val totalInativos = pontosExemplo.size - totalAtivos
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Dados de exemplo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${pontosExemplo.size} pontos ($totalAtivos ativos · $totalInativos inativos)", fontSize = 11.sp, color = TextMuted)
                    }

                }
            }
        }

        items(pontosExemplo, key = { it.id }) { ponto ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = ponto.nome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text(text = "${ponto.dispositivo}  ·  Modelo: ${ponto.modelo}", fontSize = 11.sp, color = TextMuted)
                        Text(text = "Escopo: ${ponto.escopo} (${ponto.entidade})", fontSize = 10.sp, color = TextSubtle)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (ponto.ativo) ToneGreenBg else ToneRedBg,
                            border = BorderStroke(1.dp, if (ponto.ativo) ToneGreenText else ToneRedText)
                        ) {
                            Text(
                                text = if (ponto.ativo) "Ativo" else "Inativo",
                                fontSize = 10.sp,
                                color = if (ponto.ativo) ToneGreenText else ToneRedText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                    }
                }
            }
        }
    }
}
