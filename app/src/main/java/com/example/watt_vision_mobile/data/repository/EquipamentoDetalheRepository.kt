package com.example.watt_vision_mobile.data.repository

import com.example.watt_vision_mobile.data.api.WattVisionApiService
import com.example.watt_vision_mobile.model.alarmes.AlarmeEntidade
import com.example.watt_vision_mobile.model.EquipamentoDto
import com.example.watt_vision_mobile.model.alarmes.AlarmeTipoEntidade
import com.example.watt_vision_mobile.ui.screens.equipamento_detalhe.*
import com.example.watt_vision_mobile.ui.screens.operacional.EquipamentoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface EquipamentoDetalheRepository {
    suspend fun getDetalheEquipamento(equipamento: EquipamentoItem): Result<EquipamentoDetalheData>
}

class EquipamentoCadastroRepository(private val api: WattVisionApiService) {
    suspend fun carregarEquipamento(equipamento: EquipamentoItem): Result<EquipamentoDetalheData> {
        return try {
            val id = equipamento.id.toInt()
            val resposta = api.getEquipamento(id)
            if (!resposta.isSuccessful) throw retrofit2.HttpException(resposta)
            val cadastro = requireNotNull(resposta.body()?.takeIf { it.sucesso }?.dados)
            check(cadastro.id == id)
            Result.success(mapear(AlarmeEntidade(AlarmeTipoEntidade.EQUIPAMENTO, id, equipamento.nome), cadastro))
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (erro: Exception) {
            Result.failure(erro)
        }
    }

    suspend fun carregar(entidade: AlarmeEntidade): EquipamentoDetalheData {
        val cadastro = if (entidade.demonstracao) null else try {
            api.getEquipamento(entidade.id)
                .takeIf { it.isSuccessful }?.body()?.takeIf { it.sucesso }?.dados
                ?.takeIf { it.id == entidade.id }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

        return mapear(entidade, cadastro)
    }

    private fun mapear(entidade: AlarmeEntidade, cadastro: EquipamentoDto?): EquipamentoDetalheData {
        return EquipamentoDetalheData(
            id = entidade.id.toString(),
            codigo = cadastro?.codigo.orEmpty(),
            nome = cadastro?.nome?.takeIf { it.isNotBlank() } ?: entidade.nome,
            fabricante = cadastro?.fabricante.orEmpty(),
            modelo = cadastro?.modelo.orEmpty(),
            numeroSerie = cadastro?.numero_serie.orEmpty(),
            tensaoNominalV = cadastro?.tensao_nominal,
            dataInstalacao = cadastro?.data_instalacao?.substringBefore('T'),
            localNome = cadastro?.nome_local ?: entidade.local.orEmpty(),
            setorNome = cadastro?.nome_setor ?: entidade.setor.orEmpty(),
            tipo = (cadastro?.tipo_equipamento ?: cadastro?.tipo).orEmpty(),
            status = cadastro?.status.orEmpty(),
            descricao = cadastro?.descricao.orEmpty(),
            conexaoStatus = null,
            ultimaComunicacaoTimestampMs = null
        )
    }
}

class MockEquipamentoDetalheRepository : EquipamentoDetalheRepository {

    private var simulatedTickCount = 0

    override suspend fun getDetalheEquipamento(equipamento: EquipamentoItem): Result<EquipamentoDetalheData> {
        return withContext(Dispatchers.IO) {
            simulatedTickCount++

            val idNum = equipamento.id.toIntOrNull() ?: 0

            // Simula falha de conexão ocasional a cada 7 ticks para testar o estado STALE
            if (simulatedTickCount % 8 == 0) {
                return@withContext Result.failure(Exception("Erro temporário de conexão com a API"))
            }

            val conexaoStatus = if (idNum % 3 == 0 || simulatedTickCount % 9 == 0) ConexaoStatus.OFFLINE else ConexaoStatus.ONLINE
            
            // Simula timestamp da última comunicação (se offline, há 12 min; se online, há alguns segundos)
            val ultimaComunicaoMs = if (conexaoStatus == ConexaoStatus.OFFLINE) {
                System.currentTimeMillis() - (12 * 60 * 1000L)
            } else {
                System.currentTimeMillis() - (simulatedTickCount * 1000L % 15000L)
            }

            // Simulação dos Casos Específicos:
            // idNum == 2: sem alarmes
            // idNum == 6: campos vazios
            // outros: 3 alarmes com simulação de um novo alarme chegando dinamicamente no tick 3
            val alarmesList = mutableListOf<AlertaDetalheItem>()

            if (idNum != 2 && idNum != 6) {
                alarmesList.add(
                    AlertaDetalheItem(
                        id = "a1",
                        titulo = "Sobretensão detectada",
                        severidade = AlertaSeveridade.CRITICO,
                        condicao = "Tensão > 250 V (atual: 263 V)",
                        dataHoraFormatada = "Hoje, 14:32"
                    )
                )
                alarmesList.add(
                    AlertaDetalheItem(
                        id = "a2",
                        titulo = "Corrente elevada",
                        severidade = AlertaSeveridade.ALERTA,
                        condicao = "Corrente > 80 A (atual: 84,3 A)",
                        dataHoraFormatada = "Hoje, 12:15"
                    )
                )
                alarmesList.add(
                    AlertaDetalheItem(
                        id = "a3",
                        titulo = "Temperatura interna alta",
                        severidade = AlertaSeveridade.AVISO,
                        condicao = "Temp. > 70°C (atual: 71,2°C)",
                        dataHoraFormatada = "Ontem, 23:47"
                    )
                )

                // Simula um alarme crítico novíssimo aparecendo no tick 3
                if (simulatedTickCount >= 3) {
                    alarmesList.add(
                        0,
                        AlertaDetalheItem(
                            id = "a4_novo",
                            titulo = "Falha crítica de fase R",
                            severidade = AlertaSeveridade.CRITICO,
                            condicao = "Desequilíbrio de Fase > 15% (atual: 18.4%)",
                            dataHoraFormatada = "Agora mesmo",
                            isNovo = true
                        )
                    )
                }
            }

            // Dados vazios para idNum == 6
            val fabricante = if (idNum == 6) "" else "WEG"
            val modelo = if (idNum == 6) "" else "W22 Super Premium"
            val numSerie = if (idNum == 6) "" else "WV-MOTOR-${idNum.toString().padStart(4, '0')}"
            val tensaoNominal = if (idNum == 6) null else 220.0
            val dataInstalacao = if (idNum == 6) null else "2026-11-06"
            val descricao = if (idNum == 6) "" else "Equipamento demonstrativo do TCC Watt Vision utilizado no monitoramento industrial de alta eficiência."

            Result.success(
                EquipamentoDetalheData(
                    id = equipamento.id,
                    codigo = equipamento.codigo,
                    nome = equipamento.nome,
                    fabricante = fabricante,
                    modelo = modelo,
                    numeroSerie = numSerie,
                    tensaoNominalV = tensaoNominal,
                    dataInstalacao = dataInstalacao,
                    localNome = equipamento.localNome,
                    setorNome = equipamento.setorNome,
                    tipo = "Motor Elétrico",
                    status = equipamento.status,
                    descricao = descricao,
                    conexaoStatus = conexaoStatus,
                    ultimaComunicacaoTimestampMs = ultimaComunicaoMs,
                    alarmes = alarmesList
                )
            )
        }
    }
}
