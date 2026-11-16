package com.example.watt_vision_mobile.ui.screens.dashboard

import com.example.watt_vision_mobile.data.repository.*
import com.example.watt_vision_mobile.model.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Consulta independente da UI, testável com respostas controladas, sem cliente HTTP. */
class DashboardLoader(private val source: DashboardSource) {
    suspend fun load(s: DashboardSelection): DashboardUiState = coroutineScope {
        val catalog = source.catalog()
        if ((s.location != null && catalog.locais.none { it.id == s.location }) ||
            (s.sector != null && catalog.setores.none { it.id == s.sector && (s.location == null || it.id_local == s.location) }) ||
            (s.equipment != null && catalog.equipamentos.none { it.id == s.equipment }) ||
            (s.point != null && catalog.pointsFor(s).none { it.id == s.point }) ||
            (s.sensor != null && catalog.sensorsFor(s).none { it.id == s.sensor })) throw DashboardApiException(403)
        val sensor = catalog.sensorsFor(s).firstOrNull { it.id == s.sensor }
        val points = catalog.pointsFor(s).filter { (s.sensor == null || sensor != null) && (sensor == null || it.id == sensor.id_ponto_medicao) }
        val deviceIds = points.mapNotNull { it.id_dispositivo_iot }.toSet()
        val devices = catalog.dispositivos.filter { it.id in deviceIds }
        val hours = DashboardValues.periods[s.period]
        val seriesJob = async {
            capture {
                if (sensor?.id != null && hours != null) source.series(sensor.id, hours) else emptyList()
            }
        }
        val consumptionJob = async {
            capture {
                if (sensor?.id == null || !DashboardValues.validDates(s.start, s.end)) null
                else source.consumption(sensor.id, s.start, s.end).also {
                    if (it.id_pzem != sensor.id || it.inicio != s.start || it.fim != s.end) throw DashboardApiException(502)
                }
            }
        }
        val alarmsJob = async { capture { if (deviceIds.isEmpty()) emptyList() else source.alarms() } }
        val seriesResult = seriesJob.await()
        val consumptionResult = consumptionJob.await()
        val alarmsResult = alarmsJob.await()
        val failures = listOf(seriesResult, consumptionResult, alarmsResult).mapNotNull { it.exceptionOrNull() }
        failures.firstOrNull { it is DashboardApiException && it.code in listOf(401, 403) }?.let { throw it }
        val raw = seriesResult.getOrNull().orEmpty()
        // A API pode carregar histórico de outro ponto após transferência; não misturar escopos.
        val measurements = raw.filter { it.id_pzem == sensor?.id && it.id_ponto_medicao == sensor?.id_ponto_medicao }
        val stamped = measurements.mapNotNull { m -> DashboardValues.timestamp(m.data_hora)?.let { m to it } }
        val kind = stamped.firstOrNull()?.second?.kind
        val latest = stamped.filter { it.second.kind == kind }.maxByOrNull { it.second.position }?.first
        val alarmRows = alarmsResult.getOrNull().orEmpty().filter { it.id_dispositivo_iot in deviceIds && it.status == "ativo" }
            .sortedWith(compareByDescending<AlertaDto> { DashboardValues.timestamp(it.data_hora)?.position ?: Long.MIN_VALUE }.thenByDescending { it.id })
        val consumo = consumptionResult.getOrNull()
        val validConsumption = consumo?.takeIf { (it.quantidade_medicoes ?: 0) >= 2 && it.consumo_kwh?.let { value -> value.isFinite() && value >= 0 } == true }
        val messages = buildList {
            if (sensor == null) add(if (s.sensor == null) "Selecione um medidor para consultar grandezas, gráfico e consumo." else "Medidor selecionado indisponível neste escopo. Selecione outro medidor.")
            if (catalog.sensores.isEmpty()) add("Nenhum medidor com vínculo autorizado foi retornado.")
            if (hours == null) add("30 dias indisponível no gráfico: a série permite no máximo 7 dias. O cálculo por datas tem consulta própria.")
            if (raw.size >= 10000) add("Limite de 10.000 amostras atingido. Janela possivelmente incompleta; a última amostra recebida pode não ser a mais recente do período.")
            if (raw.size != measurements.size) add("Amostras sem vínculo compatível com o medidor/ponto selecionado foram excluídas.")
            if (measurements.any { DashboardValues.timestamp(it.data_hora) == null }) add("Há amostras sem data válida; não entram no gráfico ou na seleção da última amostra.")
            if (measurements.mapNotNull { DashboardValues.timestamp(it.data_hora)?.kind }.distinct().size > 1) add("Timestamps com e sem fuso foram retornados juntos. O gráfico não combina essas bases temporais.")
            if (sensor != null && seriesResult.isSuccess && measurements.isEmpty() && hours != null) add("Nenhuma medição retornada para este medidor na janela consultada.")
            if (consumo != null && validConsumption?.consumo_kwh == null) add("Consumo/custo indisponíveis: medições insuficientes ou valores ausentes na resposta.")
            if (devices.any { it.status !in listOf("online", "offline") }) add("Há dispositivos com status diferente de online/offline ou não informado; não foram reclassificados.")
            seriesResult.exceptionOrNull()?.let { add("Gráfico: ${DashboardValues.error(it)}") }
            consumptionResult.exceptionOrNull()?.let { add("Consumo: ${DashboardValues.error(it)}") }
            alarmsResult.exceptionOrNull()?.let { add("Alarmes: ${DashboardValues.error(it)}") }
        }
        val measurementDetail = "Última amostra retornada · PZEM ${sensor?.id ?: "—"}"
        val summaries = listOf(
            StatCardData("Tensão", DashboardValues.number(latest?.tensao?.takeIf { it >= 0 }, "V"), measurementDetail, iconName = "flash"),
            StatCardData("Dispositivos online", devices.count { it.status == "online" }.toString(), "Status cadastrado · escopo selecionado", tone = StatTone.GREEN, iconName = "check_circle"),
            StatCardData("Dispositivos offline", devices.count { it.status == "offline" }.toString(), "Status cadastrado · escopo selecionado", tone = StatTone.RED, iconName = "wifi_off"),
            StatCardData("Fator de potência", DashboardValues.number(latest?.let { DashboardValues.value(it, "fp") }, digits = 3), measurementDetail, iconName = "activity")
        )
        val calendar = "${DashboardValues.displayDate(s.start)} a ${DashboardValues.displayDate(s.end)} · PZEM ${sensor?.id ?: "—"}"
        DashboardUiState(
            selection = s, catalog = catalog, summaryCards = summaries,
            indicators = DashboardValues.metrics.map { (key, label) ->
                EnergeticMetricData(label, DashboardValues.number(latest?.let { DashboardValues.value(it, key) },
                    if (key == "fp") "" else DashboardValues.unit(key), if (key == "fp") 3 else 2), measurementDetail, "activity")
            }, measurements = measurements,
            alarms = alarmRows.take(5).map { alarmData(it, it.id_dispositivo_iot!!) },
            alarmsUnavailable = alarmsResult.isFailure,
            alarmCount = if (alarmsResult.isSuccess) alarmRows.size else null,
            consumptionCards = listOf(
                StatCardData("Consumo calculado pela API", DashboardValues.number(validConsumption?.consumo_kwh?.takeIf { it >= 0 }, "kWh"), calendar),
                StatCardData("Custo energético estimado", DashboardValues.money(validConsumption?.takeIf { it.moeda == "BRL" && it.tarifa?.valor_kwh?.let { price -> price.isFinite() && price >= 0 } == true }?.custo_estimado), "${calendar}\nEnergia × tarifa; não é previsão da fatura")
            ), messages = messages,
            scopeText = "${devices.size} dispositivos com ponto no escopo · ${points.mapNotNull { it.id_equipamento }.distinct().size} equipamentos com ponto direto. Alarmes ativos desses dispositivos, sem filtro de período/medidor.",
            lastUpdatedText = "Consulta concluída às ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss"))} · horário do aparelho",
            measurementTime = latest?.data_hora?.let { DashboardValues.timestamp(it)?.label } ?: "Sem amostra disponível"
        )
    }
}

internal suspend fun <T> capture(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { Result.failure(error) }
