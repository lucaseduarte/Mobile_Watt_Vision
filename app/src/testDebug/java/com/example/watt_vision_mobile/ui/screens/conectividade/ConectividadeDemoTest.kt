package com.example.watt_vision_mobile.ui.screens.conectividade

import com.example.watt_vision_mobile.ui.screens.operacional.TabFilterState
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ConectividadeDemoTest {
    @Test fun transportReturnsStableDevicesAndEventsWithCoherentTimestamps() = runBlocking {
        val fonte = fonteConectividade(LocalDateTime.parse("2026-11-23T12:00:00"))
        val snapshot = fonte.carregar()
        assertEquals(snapshot, fonte.carregar())
        val dispositivos = snapshot.dispositivos.associateBy { it.id }
        assertEquals(snapshot.dispositivos.size, dispositivos.size)
        assertTrue(snapshot.eventos.all { it.dispositivoId in dispositivos })

        val offline = snapshot.dispositivos.single { it.corresponde(TabFilterState("006", "Offline")) }
        val queda = snapshot.eventos.single { it.dispositivoId == offline.id }
        assertEquals("Desconectado", queda.evento)
        assertTrue(queda.momento.isAfter(LocalDateTime.parse(offline.ultimoContato)))

        val online = snapshot.dispositivos.single { it.corresponde(TabFilterState(statusFilter = "Online")) }
        val reconexao = snapshot.eventos.filter { it.dispositivoId == online.id }.maxBy { it.momento }
        assertEquals("Reconectado", reconexao.evento)
        assertTrue(reconexao.momento.isBefore(LocalDateTime.parse(online.ultimoContato)))
        assertTrue(online.corresponde(TabFilterState(locationFilter = online.local, equipmentFilter = online.equipamento)))
        assertFalse(offline.corresponde(TabFilterState(locationFilter = online.local)))
    }
}
