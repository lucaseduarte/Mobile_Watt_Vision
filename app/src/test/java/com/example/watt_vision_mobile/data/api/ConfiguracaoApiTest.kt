package com.example.watt_vision_mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfiguracaoApiTest {
    @Test fun aceitaIpv4PrivadoNaPortaDoBackend() {
        for (ip in listOf("10.107.148.12", "172.16.3.4", "192.168.1.20")) {
            assertEquals("http://$ip:3000/", ConfiguracaoApi.urlWifi(" $ip "))
        }
    }

    @Test fun rejeitaEnderecosForaDaRedePrivadaEUrlsComComponentesExtras() {
        for (entrada in listOf(
            "8.8.8.8", "172.32.0.1", "127.0.0.1", "10.0.2.2", "10.0.0.256",
            "10.0.0.10:3000", "10.0.0.10/api", "http://10.0.0.10", "wattvision.local"
        )) {
            assertNull(entrada, ConfiguracaoApi.urlWifi(entrada))
        }
        assertNull(ConfiguracaoApi.origemLocal("http://10.0.0.10:3001/"))
    }
}
