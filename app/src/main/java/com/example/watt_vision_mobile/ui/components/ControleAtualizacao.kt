package com.example.watt_vision_mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.watt_vision_mobile.ui.theme.CardBorder
import com.example.watt_vision_mobile.ui.theme.InputBackground
import com.example.watt_vision_mobile.ui.theme.LinkBlue
import com.example.watt_vision_mobile.ui.theme.TextWhite
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun ControleAtualizacao(
    horario: LocalTime?,
    onAtualizar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    descricao: String = "Atualizar dados"
) {
    OutlinedButton(
        onClick = onAtualizar,
        enabled = habilitado,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = InputBackground,
            contentColor = TextWhite
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.Refresh, descricao, Modifier.size(18.dp), tint = LinkBlue)
        Spacer(Modifier.width(6.dp))
        Text(horario?.format(formatoHora)?.let { "Atualizado às $it" } ?: "Atualizar", fontSize = 12.sp)
    }
}

@Composable
internal fun AtualizacaoPeriodica(habilitada: Boolean, atualizar: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val atual by rememberUpdatedState(atualizar)
    LaunchedEffect(lifecycle, habilitada) {
        if (!habilitada) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(5_000)
                atual()
            }
        }
    }
}
