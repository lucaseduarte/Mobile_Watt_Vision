package com.example.watt_vision_mobile.data.api

import com.example.watt_vision_mobile.BuildConfig
import com.example.watt_vision_mobile.data.repository.SessionCredential
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Invocation

class AuthInterceptorLocalSeriesTest {
    private val origin = "http://127.0.0.1:13000"
    private val path = "/api/medicoes/serie-temporal"
    private val seriesMethod = WattVisionApiService::class.java.methods.single {
        it.name == "getSerieTemporal"
    }

    private class Transport(
        enabled: Boolean = true,
        var credential: SessionCredential? = SessionCredential("fixture")
    ) {
        val requests = mutableListOf<Request>()
        var status = 200
        var onRequest: () -> Unit = {}

        private val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor({ credential }, { rejected ->
                if (credential === rejected) credential = null
            }, enabled))
            .addInterceptor { chain ->
                requests += chain.request()
                onRequest()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("Test response")
                    .body("".toResponseBody())
                    .build()
            }
            .build()

        fun execute(request: Request): Int = client.newCall(request).execute().use { it.code }

        fun assertBlocked(request: Request) {
            try {
                execute(request)
                fail("A requisição deveria ter sido bloqueada")
            } catch (_: IOException) {
                assertEquals(0, requests.size)
            }
        }
    }

    private fun series(query: String = "id_pzem=1&horas=24"): Request =
        Request.Builder().url("$origin$path?$query")
            .tag(Invocation::class.java, Invocation.of(seriesMethod, emptyList<Any>()))
            .build()

    @Test fun disabledRejectsBeforeTransport() {
        Transport(enabled = false).assertBlocked(series())
    }

    @Test fun debugAcceptsBoundedWindowsAndUsesCurrentBearer() {
        assumeTrue(BuildConfig.DEBUG)
        val transport = Transport()
        for (hours in listOf(6, 24, 48, 168)) {
            val request = series("id_pzem=1&horas=$hours").newBuilder()
                .header("Authorization", "Bearer caller-supplied")
                .build()
            assertEquals(200, transport.execute(request))
        }
        assertEquals(List(4) { "Bearer fixture" }, transport.requests.map { it.header("Authorization") })
    }

    @Test fun rejectsUnexpectedSensorRouteMethodOriginInvocationAndQuery() {
        assumeTrue(BuildConfig.DEBUG)
        val forbidden = listOf(
            series("id_pzem=0&horas=24"),
            series().newBuilder().url("$origin/api/medicoes/resumo?id_pzem=1&horas=24").build(),
            series().newBuilder().post("".toRequestBody()).build(),
            series().newBuilder().url("http://127.0.0.1:3000$path?id_pzem=1&horas=24").build(),
            series().newBuilder().url("http://10.0.2.2:3001$path?id_pzem=1&horas=24").build(),
            series().newBuilder().url("https://10.0.2.2:3000$path?id_pzem=1&horas=24").build(),
            series().newBuilder().tag(Invocation::class.java, null).build(),
            series("id_pzem=1&id_pzem=1&horas=24"),
            series("id_pzem=1&horas=24&horas=24"),
            series("id_pzem=1&horas=24&local=A"),
            series("id_pzem=1&horas=721"),
            series("id_pzem=1")
        )
        forbidden.forEach { Transport().assertBlocked(it) }
    }

    @Test fun sessionIsRequiredEvenWithCallerSuppliedBearer() {
        Transport(credential = null).assertBlocked(
            series().newBuilder().header("Authorization", "Bearer candidate").build()
        )
    }

    @Test fun releaseBlocksEvenWhenExceptionIsRequested() {
        assumeFalse(BuildConfig.DEBUG)
        assertEquals(false, BuildConfig.LOCAL_SERIES_ENABLED)
        Transport(enabled = true).assertBlocked(series())
    }

    @Test fun loginAndMeKeepTheirAuthorizationRules() {
        val transport = Transport(enabled = false)
        fun authRequest(name: String, path: String) = Request.Builder()
            .url("$origin/api/auth/$path")
            .tag(Invocation::class.java, Invocation.of(
                AuthApiService::class.java.methods.single { it.name == name }, emptyList<Any>()
            ))

        transport.execute(authRequest("login", "login").post("".toRequestBody())
            .header("Authorization", "Bearer unwanted").build())
        transport.execute(authRequest("getMe", "me").header("Authorization", "Bearer candidate").build())
        transport.execute(authRequest("getMe", "me").build())

        assertEquals(listOf(null, "Bearer candidate", "Bearer fixture"),
            transport.requests.map { it.header("Authorization") })
    }

    @Test fun current401RejectsCredentialBut403DoesNot() {
        assumeTrue(BuildConfig.DEBUG)
        val transport = Transport()
        val current = transport.credential
        transport.status = 403
        assertEquals(403, transport.execute(series()))
        assertSame(current, transport.credential)
        transport.status = 401
        assertEquals(401, transport.execute(series()))
        assertNull(transport.credential)
    }

    @Test fun delayed401DoesNotInvalidateNewCredential() {
        assumeTrue(BuildConfig.DEBUG)
        val transport = Transport()
        val replacement = SessionCredential("fixture")
        transport.status = 401
        transport.onRequest = { transport.credential = replacement }

        assertEquals(401, transport.execute(series()))
        assertSame(replacement, transport.credential)
    }
}
