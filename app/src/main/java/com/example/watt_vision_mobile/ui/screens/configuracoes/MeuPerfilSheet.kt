package com.example.watt_vision_mobile.ui.screens.configuracoes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.watt_vision_mobile.ui.theme.corTema
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val fundoPerfil
    @Composable get() = corTema(0xFF111827, 0xFFFFFFFF)
private val fundoCampo
    @Composable get() = corTema(0xFF0B111E, 0xFFF1F6FC)
private val bordaPerfil
    @Composable get() = corTema(0xFF1E293B, 0xFFCBD8E7)
private val tituloPerfil
    @Composable get() = corTema(0xFFF1F5F9, 0xFF111827)
private val legendaPerfil
    @Composable get() = corTema(0xFF94A3B8, 0xFF475569)
private val linkPerfil
    @Composable get() = corTema(0xFF3B82F6, 0xFF1D4ED8)
private val botaoPerfil = Color(0xFF2563EB)
private val verdePerfil
    @Composable get() = corTema(0xFF10B981, 0xFF166534)

private enum class CampoSenha { ATUAL, NOVA, CONFIRMACAO }
private data class ErroSenha(val campo: CampoSenha, val mensagem: String)

private fun validarSenhaLocal(atual: String, nova: String, confirmacao: String): ErroSenha? = when {
    atual.isBlank() -> ErroSenha(CampoSenha.ATUAL, "Informe a senha atual.")
    nova.isBlank() -> ErroSenha(CampoSenha.NOVA, "Informe a nova senha.")
    nova.length < 8 -> ErroSenha(CampoSenha.NOVA, "Use pelo menos 8 caracteres.")
    nova.toByteArray(Charsets.UTF_8).size > 72 -> ErroSenha(CampoSenha.NOVA, "Use uma senha de até 72 bytes.")
    confirmacao.isBlank() -> ErroSenha(CampoSenha.CONFIRMACAO, "Confirme a nova senha.")
    nova != confirmacao -> ErroSenha(CampoSenha.CONFIRMACAO, "A confirmação da senha não confere.")
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeuPerfilSheet(
    usuario: UsuarioItem,
    demonstracao: Boolean,
    onLogout: (() -> Unit)? = null,
    onFechar: () -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    var senhaAtual by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<ErroSenha?>(null) }
    var validacaoConcluida by remember { mutableStateOf(false) }
    var geracaoFormulario by remember { mutableIntStateOf(0) }
    val foco = LocalFocusManager.current
    fun limparFormulario() {
        foco.clearFocus()
        senhaAtual = ""
        novaSenha = ""
        confirmacao = ""
        erro = null
        expandido = false
        geracaoFormulario++
    }
    val fechar = {
        limparFormulario()
        onFechar()
    }

    ModalBottomSheet(
        onDismissRequest = fechar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = fundoPerfil,
        contentColor = tituloPerfil
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CONTA ATUAL", color = linkPerfil, fontSize = 11.sp,
                        letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
                    Text("Meu perfil", color = tituloPerfil, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Informações do usuário conectado ao Watt Vision", color = legendaPerfil, fontSize = 13.sp)
                }
                IconButton(onClick = fechar) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar perfil", tint = legendaPerfil)
                }
            }
            if (demonstracao) {
                Text("DEBUG · DADOS SIMULADOS", fontSize = 12.sp, color = corTema(0xFFFBBF24, 0xFF854D0E))
            }
            DestaquePerfil(usuario)
            DadoPerfil(Icons.Outlined.Person, "Nome", usuario.nome)
            DadoPerfil(Icons.Outlined.Email, "E-mail", usuario.email)
            DadoPerfil(Icons.Outlined.VerifiedUser, "Perfil de acesso", usuario.perfil)

            HorizontalDivider(color = bordaPerfil)
            TextButton(
                onClick = {
                    validacaoConcluida = false
                    if (expandido) limparFormulario() else expandido = true
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    if (expandido) "Cancelar alteração de senha" else "Alterar senha",
                    color = linkPerfil, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }

            AnimatedVisibility(visible = expandido) {
                key(geracaoFormulario) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoSenhaPerfil("Senha atual", senhaAtual, erro?.takeIf { it.campo == CampoSenha.ATUAL }?.mensagem) {
                            senhaAtual = it
                            erro = null
                        }
                        CampoSenhaPerfil("Nova senha", novaSenha, erro?.takeIf { it.campo == CampoSenha.NOVA }?.mensagem) {
                            novaSenha = it
                            erro = null
                        }
                        CampoSenhaPerfil("Confirmar nova senha", confirmacao,
                            erro?.takeIf { it.campo == CampoSenha.CONFIRMACAO }?.mensagem) {
                            confirmacao = it
                            erro = null
                        }
                        BotaoPerfil("Salvar nova senha") {
                            erro = validarSenhaLocal(senhaAtual, novaSenha, confirmacao)
                            if (erro == null) {
                                limparFormulario()
                                validacaoConcluida = true
                            }
                        }
                    }
                }
            }
            if (validacaoConcluida) {
                Text(
                    "Validação local concluída. A senha da conta permanece a mesma.",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    fontSize = 13.sp, color = legendaPerfil
                )
            }
            HorizontalDivider(color = bordaPerfil)
            if (!demonstracao && onLogout != null) {
                TextButton(onClick = {
                    fechar()
                    onLogout()
                }) {
                    Text("Sair da conta", color = linkPerfil)
                }
            }
            BotaoPerfil("Fechar", onClick = fechar)
        }
    }
}

@Composable
private fun DestaquePerfil(usuario: UsuarioItem) {
    val iniciais = remember(usuario.nome) {
        val nomes = usuario.nome.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        listOfNotNull(nomes.firstOrNull()?.firstOrNull(), nomes.getOrNull(nomes.lastIndex.takeIf { it > 0 } ?: -1)?.firstOrNull())
            .joinToString("").uppercase().ifBlank { "WV" }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = corTema(0xFF0E1A2E, 0xFFE5EEFF),
        border = BorderStroke(1.dp, corTema(0xFF1E3A5F, 0xFF9EBAE2))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(Modifier.size(48.dp), shape = CircleShape, color = botaoPerfil) {
                Box(contentAlignment = Alignment.Center) {
                    Text(iniciais, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(usuario.nome, color = tituloPerfil, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(usuario.status, color = legendaPerfil, fontSize = 13.sp)
            }
            if (usuario.status.equals("Ativo", ignoreCase = true)) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = verdePerfil, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun DadoPerfil(icone: ImageVector, rotulo: String, valor: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = fundoCampo,
        border = BorderStroke(1.dp, bordaPerfil)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp), tint = corTema(0xFF64748B, 0xFF475569))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(rotulo, color = legendaPerfil, fontSize = 11.sp)
                Text(valor, color = tituloPerfil, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CampoSenhaPerfil(rotulo: String, senha: String, erro: String?, onChange: (String) -> Unit) {
    var mostrar by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rotulo, color = corTema(0xFFCBD5E1, 0xFF334155), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = senha,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = erro != null,
            supportingText = erro?.let { mensagem -> { Text(mensagem) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (mostrar) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fundoCampo, unfocusedContainerColor = fundoCampo,
                errorContainerColor = fundoCampo, focusedBorderColor = linkPerfil,
                unfocusedBorderColor = bordaPerfil, focusedTextColor = tituloPerfil,
                unfocusedTextColor = tituloPerfil, errorTextColor = tituloPerfil
            ),
            trailingIcon = {
                IconButton(onClick = { mostrar = !mostrar }) {
                    Icon(
                        if (mostrar) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (mostrar) "Ocultar $rotulo" else "Mostrar $rotulo",
                        tint = legendaPerfil
                    )
                }
            }
        )
    }
}

@Composable
private fun BotaoPerfil(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = botaoPerfil, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text(texto) }
}
