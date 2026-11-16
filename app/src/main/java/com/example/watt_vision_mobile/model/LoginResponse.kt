package com.example.watt_vision_mobile.model

data class LoginResponse(
    val sucesso: Boolean,
    val mensagem: String?,
    val dados: AuthData?
) {
    // Sucesso sem token não concede acesso ao conteúdo autenticado.
    val hasSessionToken: Boolean
        get() = sucesso && !dados?.token.isNullOrBlank()
}

data class AuthData(
    val token: String?,
    val tipo: String?,
    val usuario: UsuarioDto?
)
