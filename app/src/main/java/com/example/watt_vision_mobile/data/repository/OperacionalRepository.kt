package com.example.watt_vision_mobile.data.repository

import com.example.watt_vision_mobile.data.api.ChartCatalogApiService
import com.example.watt_vision_mobile.model.ApiResponse
import com.example.watt_vision_mobile.ui.screens.operacional.EquipamentoItem
import com.example.watt_vision_mobile.ui.screens.operacional.LocalItem
import com.example.watt_vision_mobile.ui.screens.operacional.SetorItem

import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.random.Random

data class OperacionalCatalogo(
    val locais: List<LocalItem>,
    val setores: List<SetorItem>,
    val equipamentos: List<EquipamentoItem>
)

fun interface OperacionalRepository {
    suspend fun carregar(): OperacionalCatalogo
}

internal class RetrofitOperacionalRepository(
    private val api: ChartCatalogApiService
) : OperacionalRepository {
    override suspend fun carregar(): OperacionalCatalogo {
        val locais = api.locais().requireData()
        val setores = api.setores().requireData()
        val equipamentos = api.equipamentos().requireData()
        val locaisPorId = locais.associateBy { requireId(it.id) }
        val setoresPorId = setores.associateBy { requireId(it.id) }
        if (locaisPorId.size != locais.size || setoresPorId.size != setores.size ||
            equipamentos.map { requireId(it.id) }.toSet().size != equipamentos.size
        ) throw IOException("Identificadores duplicados no catálogo operacional.")
        if (setores.any { it.id_local !in locaisPorId } ||
            equipamentos.any { it.id_setor !in setoresPorId }
        ) throw IOException("Vínculo ausente no catálogo operacional.")

        val setoresPorLocal = setores.groupBy { it.id_local }
        val equipamentosPorSetor = equipamentos.groupingBy { it.id_setor }.eachCount()

        return OperacionalCatalogo(
            locais = locais.map { local ->
                val vinculados = setoresPorLocal[local.id].orEmpty()
                LocalItem(
                    id = requireId(local.id).toString(),
                    codigo = local.codigo.orEmpty(),
                    nome = local.nome.orEmpty(),
                    setoresVinculados = vinculados.size,
                    equipamentosVinculados = vinculados.sumOf { equipamentosPorSetor[it.id] ?: 0 },
                    consumoAcumuladoKwh = null,
                    status = local.status.orEmpty(),
                    endereco = local.endereco,
                    responsavel = local.responsavel
                )
            },
            setores = setores.map { setor ->
                SetorItem(
                    id = requireId(setor.id).toString(),
                    codigo = setor.codigo.orEmpty(),
                    nome = setor.nome.orEmpty(),
                    localNome = locaisPorId[setor.id_local]?.nome.orEmpty(),
                    equipamentosVinculados = equipamentosPorSetor[setor.id] ?: 0,
                    consumoAcumuladoKwh = null,
                    status = setor.status.orEmpty(),
                    localId = requireId(setor.id_local).toString(),
                    responsavel = setor.responsavel
                )
            },
            equipamentos = equipamentos.map { equipamento ->
                val setor = setoresPorId[equipamento.id_setor]
                EquipamentoItem(
                    id = requireId(equipamento.id).toString(),
                    codigo = equipamento.codigo.orEmpty(),
                    nome = equipamento.nome.orEmpty(),
                    localNome = locaisPorId[setor?.id_local]?.nome.orEmpty(),
                    setorNome = setor?.nome.orEmpty(),
                    potenciaW = equipamento.potencia_nominal,
                    consumoKwh = null,
                    status = equipamento.status.orEmpty(),
                    setorId = requireId(equipamento.id_setor).toString()
                )
            }
        )
    }

    private fun requireId(id: Int?): Int =
        id?.takeIf { it > 0 } ?: throw IOException("Identificador inválido no catálogo operacional.")

    private fun <T> Response<ApiResponse<List<T>>>.requireData(): List<T> {
        if (!isSuccessful) throw HttpException(this)
        val envelope = body()
        return envelope?.takeIf { it.sucesso }?.dados
            ?: throw IOException("Resposta inválida do catálogo operacional.")
    }
}

class MockOperacionalRepository : OperacionalRepository {
    override suspend fun carregar(): OperacionalCatalogo {
        val locais = getLocais()
        val setores = getSetores(locais)
        val equipamentos = getEquipamentos(setores)
        val equipamentosPorSetor = equipamentos.groupingBy { it.setorId }.eachCount()
        val setoresComTotais = setores.map {
            it.copy(equipamentosVinculados = equipamentosPorSetor[it.id] ?: 0)
        }
        val setoresPorLocal = setoresComTotais.groupBy { it.localId }
        return OperacionalCatalogo(
            locais.map { local ->
                val vinculados = setoresPorLocal[local.id].orEmpty()
                local.copy(
                    setoresVinculados = vinculados.size,
                    equipamentosVinculados = vinculados.sumOf { it.equipamentosVinculados }
                )
            },
            setoresComTotais,
            equipamentos
        )
    }

    private fun getLocais(): List<LocalItem> {
        return listOf(
            LocalItem(
                id = "1",
                codigo = "LOC-001",
                nome = "Planta Industrial Jandira",
                setoresVinculados = 12,
                equipamentosVinculados = 45,
                consumoAcumuladoKwh = 1685.3,
                status = "Ativo",
                endereco = "Av. Industrial, 1200 - Unidade Principal",
                responsavel = "Carlos Eduardo (Gestão)"
            ),
            LocalItem(
                id = "2",
                codigo = "LOC-002",
                nome = "Planta Industrial Osasco",
                setoresVinculados = 10,
                equipamentosVinculados = 38,
                consumoAcumuladoKwh = 2613.6,
                status = "Ativo"
            ),
            LocalItem(
                id = "3",
                codigo = "LOC-003",
                nome = "Centro Logístico Alphaville",
                setoresVinculados = 5,
                equipamentosVinculados = 18,
                consumoAcumuladoKwh = 840.0,
                status = "Ativo"
            ),
            LocalItem(
                id = "4",
                codigo = "LOC-004",
                nome = "Subestação Central",
                setoresVinculados = 3,
                equipamentosVinculados = 12,
                consumoAcumuladoKwh = 245.8,
                status = "Inativo"
            )
        )
    }

    private fun getSetores(locaisList: List<LocalItem>): List<SetorItem> {
        val random = Random(0)
        val setoresList = mutableListOf<SetorItem>()

        locaisList.forEachIndexed { lIdx, local ->
            for (s in 1..local.setoresVinculados) {
                val sId = "${lIdx + 1}-$s"
                val sNome = when (s % 5) {
                    1 -> "Produção B"
                    2 -> "Manutenção Elétrica"
                    3 -> "Caldeiraria & Usinagem"
                    4 -> "Pintura Industrial"
                    else -> "Logística & Expedição $s"
                }
                setoresList.add(
                    SetorItem(
                        id = sId,
                        codigo = "SET-0$sId",
                        nome = sNome,
                        localNome = local.nome,
                        equipamentosVinculados = 0,
                        localId = local.id,
                        responsavel = "João Pedro (Operações)".takeIf { sId == "1-1" },
                        consumoAcumuladoKwh = (120..450).random(random).toDouble(),
                        status = if (s % 4 == 0) "Inativo" else "Ativo"
                    )
                )
            }
        }
        return setoresList
    }

    private fun getEquipamentos(setores: List<SetorItem>): List<EquipamentoItem> {
        val random = Random(1)
        val equipamentosList = mutableListOf<EquipamentoItem>()

        var count = 1
        setores.forEach { setor ->
            val numEqs = (3..5).random(random)
            for (e in 1..numEqs) {
                val eqNome = when (count % 6) {
                    1 -> "Motor Elétrico WEG $count"
                    2 -> "Compressor de Ar Industrial $count"
                    3 -> "Bomba Centrífuga $count"
                    4 -> "Ponte Rolante $count"
                    5 -> "Inversor de Frequência $count"
                    else -> "Exaustor de Alta Pressão $count"
                }
                equipamentosList.add(
                    EquipamentoItem(
                        id = "$count",
                        codigo = "EQ-${count.toString().padStart(3, '0')}",
                        nome = eqNome,
                        localNome = setor.localNome,
                        setorNome = setor.nome,
                        potenciaW = (750..15000).random(random).toDouble(),
                        consumoKwh = (10..350).random(random).toDouble(),
                        status = if (count % 5 == 0) "Inativo" else "Ativo",
                        setorId = setor.id
                    )
                )
                count++
            }
        }
        return equipamentosList
    }
}
