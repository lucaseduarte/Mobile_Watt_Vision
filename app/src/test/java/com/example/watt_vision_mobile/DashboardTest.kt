package com.example.watt_vision_mobile

import com.example.watt_vision_mobile.data.api.DashboardApiService
import com.example.watt_vision_mobile.data.repository.*
import com.example.watt_vision_mobile.model.*
import com.example.watt_vision_mobile.ui.screens.dashboard.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.http.GET

class DashboardTest {
    private val gson = Gson()
    private fun equipment(id: Int = 30, sector: Int = 20) = gson.fromJson("""{"id":$id,"id_setor":$sector,"nome":"Motor"}""", EquipamentoDto::class.java)
    private fun fixtureCatalog() = DashboardCatalog(
        locais = listOf(LocalConsultaDto(10, 42, "Local")),
        setores = listOf(SetorConsultaDto(20, 10, "Setor")),
        equipamentos = listOf(equipment()),
        pontos = listOf(PontoConsultaDto(40, "Ponto", 50, id_equipamento = 30)),
        sensores = listOf(PzemConsultaDto(60, 40, "PZEM", "Ponto")),
        dispositivos = listOf(DispositivoConsultaDto(50, 42, "Dispositivo", "online"))
    )
    private fun selection(period: String = "24h") = DashboardSelection(sensor = 60, period = period, start = "2026-11-16", end = "2026-11-16")
    private fun measurement(time: String = "2026-11-16 12:00:00", watts: Any? = 1500) = MedicaoDto(
        id_pzem = 60, id_ponto_medicao = 40, data_hora = time, tensaoRaw = 220,
        potenciaAtivaRaw = watts, energiaAcumuladaRaw = 25, fatorPotenciaRaw = 0.95
    )
    private fun fixtureConsumption() = gson.fromJson("""{"id_pzem":60,"inicio":"2026-11-16","fim":"2026-11-16","quantidade_medicoes":2,"consumo_kwh":0,"custo_estimado":0,"moeda":"BRL"}""", ConsumoDto::class.java)
    private open inner class Fake : DashboardSource {
        var seriesCalls = 0
        var consumptionCalls = 0
        var alarmCalls = 0
        var hoursReceived: Int? = null
        var data = listOf(measurement())
        var consumptionError: Exception? = null
        var seriesError: Exception? = null
        var alarmError: Exception? = null
        var consumptionData = fixtureConsumption()
        var alarmData = emptyList<AlertaDto>()
        var catalogData = fixtureCatalog()
        override suspend fun catalog() = catalogData
        override suspend fun series(id: Int, hours: Int): List<MedicaoDto> {
            assertEquals(60, id); hoursReceived = hours; seriesCalls++
            seriesError?.let { throw it }; return data
        }
        override suspend fun consumption(id: Int, start: String, end: String): ConsumoDto {
            assertEquals(60, id); assertEquals("2026-11-16", start); assertEquals(start, end)
            consumptionCalls++; consumptionError?.let { throw it }; return consumptionData
        }
        override suspend fun alarms(): List<AlertaDto> { alarmCalls++; alarmError?.let { throw it }; return alarmData }
    }

    @Test fun wattsAreConvertedOnceAndAbsenceIsNotZero() {
        assertEquals(1.5, DashboardValues.kw(1500.0)!!, 0.000001)
        assertEquals("0,00 kW", DashboardValues.number(DashboardValues.kw(0.0), "kW"))
        assertEquals("Indisponível", DashboardValues.number(DashboardValues.kw(null), "kW"))
        assertNull(DashboardValues.kw(Double.NaN)); assertNull(DashboardValues.kw(-1.0))
        assertEquals("1,50 kW", DashboardValues.number(DashboardValues.value(measurement(), "potencia"), "kW"))
    }
    @Test fun jsonMapsAliasesDecimalsAndBigIdsWithoutInventedValues() {
        val dto = gson.fromJson("""{"id":3000000000,"potencia_ativa":"1500.5","frequencia":null,"fator_potencia":"NaN"}""", MedicaoDto::class.java)
        assertEquals(3000000000L, dto.id_medicao)
        assertEquals(1500.5, dto.potencia_ativa!!, 0.0)
        assertNull(dto.frequencia); assertNull(dto.fator_potencia)
        val alert = gson.fromJson("""{"id":3000000001,"tipo_alerta":"Tensão","status":"reconhecido"}""", AlertaDto::class.java)
        assertEquals(3000000001L, alert.id); assertEquals("reconhecido", alert.status)
        assertNull(alert.resolvido)
    }
    @Test fun datesAreStrictAndOffsetsArePreserved() {
        assertNull(DashboardValues.date("31/02/2026"))
        assertEquals("2026-11-16", DashboardValues.date("16/11/2026"))
        assertFalse(DashboardValues.validDates("2026-11-17", "2026-11-16"))
        assertTrue(DashboardValues.timestamp("2026-11-16T12:00:00-03:00")!!.label.contains("12:00:00 -03:00"))
        assertEquals("16/11/2026 12:00:00", DashboardValues.timestamp("2026-11-16 12:00:00")!!.label)
        assertNull(DashboardValues.timestamp("não é uma data"))
    }
    @Test fun chartSortsRealTimesKeepsZeroAndOmitsInvalidValues() {
        val data = listOf(measurement("2026-11-16 12:30:00", 0), measurement("2026-11-16 12:00:00", 1000),
            measurement("2026-11-16 12:10:00", null), measurement("inválido", 2000))
        val points = chartPoints(data, "potencia")
        assertEquals(2, points.size); assertEquals(1f, points[0].valor)
        assertEquals(0f, points[1].valor); assertEquals(1800000L, points[1].position - points[0].position)
    }
    @Test fun hierarchyFiltersByIdsAndNeverNames() {
        val c = fixtureCatalog()
        assertEquals(1, c.sensorsFor(selection().copy(location = 10, sector = 20, equipment = 30, point = 40)).size)
        assertTrue(c.sensorsFor(selection().copy(location = 999)).isEmpty())
        assertTrue(c.sensorsFor(selection().copy(equipment = 999)).isEmpty())
        assertTrue(c.sensorsFor(selection().copy(point = 999)).isEmpty())
    }
    @Test fun catalogRejectsCrossCompanyBrokenAndAmbiguousLinks() {
        val c = fixtureCatalog()
        val raw = c.copy(locais = c.locais + LocalConsultaDto(11, 99, "Local"),
            pontos = c.pontos + PontoConsultaDto(41, "Inválido", 50, id_local = 10, id_equipamento = 30) +
                PontoConsultaDto(42, "Outra empresa", 51, id_local = 10),
            dispositivos = c.dispositivos + DispositivoConsultaDto(51, 99),
            sensores = c.sensores + PzemConsultaDto(61, 41) + PzemConsultaDto(62, 42))
        val safe = authorizedCatalog(raw, 42)
        assertEquals(listOf(10), safe.locais.map { it.id })
        assertEquals(listOf(40), safe.pontos.map { it.id }); assertEquals(listOf(60), safe.sensores.map { it.id })
    }
    @Test fun zeroConsumptionIsKeptAndMissingMetricsStayUnavailable() = runBlocking {
        val state = DashboardLoader(Fake()).load(selection())
        assertEquals("0,00 kWh", state.consumptionCards.first().value)
        assertEquals("1", state.summaryCards[1].value)
        assertEquals("Indisponível", state.indicators.first { it.title == "Corrente" }.value)
        assertFalse(state.measurementTime.contains("Sem amostra"))
    }
    @Test fun eachSupportedPeriodSendsTheCorrectHours() = runBlocking {
        for ((period, hours) in DashboardValues.periods) {
            val fake = Fake(); DashboardLoader(fake).load(selection(period)); assertEquals(hours, fake.hoursReceived)
        }
    }
    @Test fun thirtyDaysDoesNotSendSevenDaysAndConsumptionHasIndependentDates() = runBlocking {
        val fake = Fake(); val state = DashboardLoader(fake).load(selection("30d"))
        assertEquals(0, fake.seriesCalls); assertEquals(1, fake.consumptionCalls)
        assertTrue(state.messages.any { it.contains("30 dias indisponível") })
    }
    @Test fun absentSensorNeverDefaultsToIdOne() = runBlocking {
        val fake = Fake(); val state = DashboardLoader(fake).load(selection().copy(sensor = null))
        assertEquals(0, fake.seriesCalls); assertEquals(0, fake.consumptionCalls)
        assertEquals("Indisponível", state.summaryCards.first().value)
    }
    @Test fun emptyAndPartialResponsesAreExplicit() = runBlocking {
        val fake = Fake().apply { data = emptyList(); consumptionError = DashboardApiException(422) }
        val state = DashboardLoader(fake).load(selection())
        assertTrue(state.messages.any { it.contains("Nenhuma medição") })
        assertTrue(state.messages.any { it.contains("resets") })
        assertEquals("Indisponível", state.consumptionCards.first().value)
        assertEquals("1", state.summaryCards[1].value)
    }
    @Test fun responseAtLimitWarnsAboutIncompleteWindow() = runBlocking {
        val fake = Fake().apply { data = List(10000) { measurement() } }
        assertTrue(DashboardLoader(fake).load(selection()).messages.any { it.contains("possivelmente incompleta") })
    }
    @Test fun wrongSensorOrHistoricalPointIsNeverMixed() = runBlocking {
        val fake = Fake().apply { data = listOf(measurement().copy(id_ponto_medicao = 99), measurement().copy(id_pzem = 99)) }
        val state = DashboardLoader(fake).load(selection())
        assertTrue(state.measurements.isEmpty()); assertTrue(state.messages.any { it.contains("excluídas") })
    }
    @Test fun failedCatalogNeverContinuesToMeasurementReads() = runBlocking {
        val fake = object : Fake() { override suspend fun catalog(): DashboardCatalog = throw DashboardApiException(401) }
        try { DashboardLoader(fake).load(selection()); fail("Sessão não validada") } catch (e: DashboardApiException) { assertEquals(401, e.code) }
        assertEquals(0, fake.seriesCalls); assertEquals(0, fake.consumptionCalls); assertEquals(0, fake.alarmCalls)
    }
    @Test fun protectedFailureDoesNotProduceSuccessfulDashboard() = runBlocking {
        val fake = Fake().apply { alarmError = DashboardApiException(403) }
        try { DashboardLoader(fake).load(selection()); fail("Deveria negar acesso") } catch (e: DashboardApiException) { assertEquals(403, e.code) }
    }
    @Test fun cancellationIsPropagatedInsteadOfBecomingAnErrorState() = runBlocking {
        try { capture<Unit> { throw CancellationException("cancelado") }; fail("Cancelamento engolido") }
        catch (_: CancellationException) { /* esperado */ }
    }
    @Test fun financialSimulationIsExplicitInputWithoutFallback() {
        assertNull(DashboardValues.simulation("", "0,80")); assertNull(DashboardValues.simulation("100", ""))
        assertNull(DashboardValues.simulation("-1", "1")); assertNull(DashboardValues.simulation("NaN", "1"))
        assertEquals("8.01", DashboardValues.simulation("10", "0,8005").toString())
        assertEquals("0.00", DashboardValues.simulation("0", "0").toString())
    }
    @Test fun alarmsAreRecentScopedAndUnknownSeverityIsNotInformational() = runBlocking {
        val fake = Fake().apply {
            alarmData = (1..7).map { gson.fromJson("""{"id":$it,"id_dispositivo_iot":50,"data_hora":"2026-11-16 12:0$it:00","status":"ativo","severidade":"desconhecida"}""", AlertaDto::class.java) } +
                gson.fromJson("""{"id":99,"id_dispositivo_iot":999,"status":"ativo"}""", AlertaDto::class.java)
        }
        val state = DashboardLoader(fake).load(selection())
        assertEquals(7, state.alarmCount); assertEquals(5, state.alarms.size)
        assertEquals("Alarme #7", state.alarms.first().title)
        assertEquals(AlarmType.UNKNOWN, state.alarms.first().type)
    }
    @Test fun catalogApiContainsOnlyReviewedGetRoutes() {
        val expected = setOf("api/locais", "api/setores", "api/equipamentos", "api/pontos-medicao", "api/pzems", "api/dispositivos-iot")
        assertEquals(expected, DashboardApiService::class.java.declaredMethods.filterNot { it.isSynthetic }.map { requireNotNull(it.getAnnotation(GET::class.java)).value }.toSet())
    }
}
