package com.example.watt_vision_mobile.data.api

import android.content.Context
import com.example.watt_vision_mobile.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal object ConfiguracaoApi {
    @Volatile
    var url: String = BuildConfig.API_BASE_URL
        private set

    val permiteConfigurarWifi = BuildConfig.DEBUG && BuildConfig.API_BASE_URL == URL_API_EMULADOR

    fun restaurar(context: Context) {
        if (!permiteConfigurarWifi) return
        url = urlWifi(preferencias(context).getString("ip", null).orEmpty()) ?: BuildConfig.API_BASE_URL
    }

    fun salvarWifi(context: Context, entrada: String): Boolean {
        if (!permiteConfigurarWifi) return false
        val destino = urlWifi(entrada) ?: return false
        preferencias(context).edit().putString("ip", entrada.trim()).apply()
        url = destino
        return true
    }

    fun urlWifi(entrada: String): String? {
        val octetos = entrada.trim().split('.')
        if (octetos.size != 4) return null
        val ip = octetos.map { parte ->
            val numero = parte.toIntOrNull() ?: return null
            if (numero !in 0..255 || numero.toString() != parte) return null
            numero
        }
        val local = ip[0] == 10 || (ip[0] == 172 && ip[1] in 16..31) ||
            (ip[0] == 192 && ip[1] == 168)
        return if (local && ip.joinToString(".") != "10.0.2.2") "http://${ip.joinToString(".")}:3000/" else null
    }

    fun origemLocal(url: String) = url.toHttpUrlOrNull()?.takeIf {
        it.scheme == "http" && it.encodedPath == "/" && url in listOf(
            URL_API_EMULADOR, "http://127.0.0.1:3000/", "http://127.0.0.1:13000/", urlWifi(it.host)
        )
    }

    private fun preferencias(context: Context) =
        context.getSharedPreferences("watt_vision_prefs", Context.MODE_PRIVATE)
}
