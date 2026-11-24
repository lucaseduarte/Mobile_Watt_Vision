package com.example.watt_vision_mobile.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OperacionalRepositoryTest {
    @Test fun mockCatalogKeepsStableIdsAndCountsChildrenById() = runBlocking {
        val repository = MockOperacionalRepository()
        val catalogo = repository.carregar()
        assertEquals(catalogo, repository.carregar())

        val locais = catalogo.locais.associateBy { it.id }
        val setores = catalogo.setores.associateBy { it.id }
        for (equipamento in catalogo.equipamentos) {
            val setor = setores.getValue(equipamento.setorId)
            assertEquals(setor.nome, equipamento.setorNome)
            assertEquals(locais.getValue(setor.localId).nome, equipamento.localNome)
        }
        for (setor in catalogo.setores) {
            assertEquals(locais.getValue(setor.localId).nome, setor.localNome)
            assertEquals(catalogo.equipamentos.count { it.setorId == setor.id }, setor.equipamentosVinculados)
        }
        for (local in catalogo.locais) {
            val vinculados = catalogo.setores.filter { it.localId == local.id }
            assertEquals(vinculados.size, local.setoresVinculados)
            assertEquals(vinculados.sumOf { it.equipamentosVinculados }, local.equipamentosVinculados)
        }
    }

    @Test fun equipmentDetailUsesTheSelectedCatalogRecord() = runBlocking {
        val catalogo = MockOperacionalRepository().carregar()
        val selecionado = catalogo.equipamentos.single { it.id == "2" }
        val detalhe = MockEquipamentoDetalheRepository().getDetalheEquipamento(selecionado).getOrThrow()
        assertEquals(selecionado.id, detalhe.id)
        assertEquals(selecionado.codigo, detalhe.codigo)
        assertEquals(selecionado.nome, detalhe.nome)
        assertEquals(selecionado.localNome, detalhe.localNome)
        assertEquals(selecionado.setorNome, detalhe.setorNome)
        assertEquals(selecionado.status, detalhe.status)
    }
}
