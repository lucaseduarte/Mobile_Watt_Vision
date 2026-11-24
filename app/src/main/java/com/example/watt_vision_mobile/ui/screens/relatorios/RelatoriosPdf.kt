package com.example.watt_vision_mobile.ui.screens.relatorios

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.getSystemService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.zip.GZIPInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class RelatorioDocumento(val relatorio: Relatorio, val estado: RelatoriosUiState)
internal data class PdfRelatorio(val bytes: ByteArray, val paginas: Int)

private fun entradaPdf(documento: RelatorioDocumento): String {
    val estado = documento.estado
    val filtros = estado.filtros
    val metricas = GrandezaRelatorio.entries
    val nomes = metricas.map { it.titulo }
    return JSONObject().apply {
        put("demonstracao", estado.demonstracao)
        put("custoKwh", estado.tarifa)
        put("grandezas", JSONArray(filtros.grandeza?.let { listOf(it.titulo) } ?: nomes))
        put("filtros", JSONObject().apply {
            val cobertura = documento.relatorio.cobertura.titulo
            val limite = if (documento.relatorio.limiteAtingido) " · possível truncamento" else ""
            put("periodo", "${filtros.periodo.titulo} · $cobertura$limite · valores observados")
            put("metrica", filtros.grandeza?.titulo ?: "Todas as grandezas")
            put("local", estado.locais.firstOrNull { it.id == filtros.localId }?.nome ?: "Todos")
            put("setor", estado.setores.firstOrNull { it.id == filtros.setorId }?.nome ?: "Todos")
            put("equipamento", estado.equipamentos.firstOrNull { it.id == filtros.equipamentoId }?.nome)
        })
        put("metricas", JSONArray(metricas.map { grandeza ->
            JSONObject().apply {
                put("nome", grandeza.titulo)
                put("unit", grandeza.unidade)
                put("cor", "#%06x".format(grandeza.cor.toArgb() and 0xFFFFFF))
                put("acumulada", grandeza == GrandezaRelatorio.ENERGIA)
                if (grandeza == GrandezaRelatorio.FATOR) put("casas", grandeza.casas)
            }
        }))
        put("registros", JSONArray(documento.relatorio.registros.map { leitura ->
            JSONObject().apply {
                put("data_hora", leitura.instante.toEpochMilli())
                put("medidor", leitura.medidor)
                put("valores", JSONObject().apply {
                    leitura.valores.forEach { (grandeza, valor) ->
                        if (valor.isFinite() && (grandeza != GrandezaRelatorio.ENERGIA || valor >= 0)) {
                            put(grandeza.titulo, valor)
                        }
                    }
                })
            }
        }))
    }.toString()
}

internal suspend fun gerarRelatorioPdf(contexto: Context, documento: RelatorioDocumento): PdfRelatorio {
    require(documento.estado.demonstracao && documento.relatorio.podeExportar)
    val (codigo, entrada) = withContext(Dispatchers.IO) {
        val compactado = contexto.assets.open("relatorios/desktop-pdf.js.gz.base64").bufferedReader().use { it.readText() }
        val codigo = GZIPInputStream(Base64.decode(compactado, Base64.DEFAULT).inputStream())
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        codigo to entradaPdf(documento)
    }
    val resultado = withTimeout(45_000) { executarGeradorPdf(contexto, codigo, entrada) }
    return withContext(Dispatchers.Default) {
        PdfRelatorio(Base64.decode(resultado.getString("pdf"), Base64.DEFAULT), resultado.getInt("paginas"))
    }
}

@SuppressLint("SetJavaScriptEnabled")
private suspend fun executarGeradorPdf(contexto: Context, codigo: String, entrada: String): JSONObject =
    withContext(Dispatchers.Main.immediate) {
        val navegador = WebView(contexto.applicationContext)
        try {
            suspendCancellableCoroutine { espera ->
                navegador.settings.apply {
                    javaScriptEnabled = true
                    blockNetworkLoads = true
                    allowFileAccess = false
                    allowContentAccess = false
                }
                navegador.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (!espera.isActive) return
                        view.evaluateJavascript(
                            """
                            (() => {
                              try {
                                $codigo
                                return JSON.stringify(globalThis.gerarRelatorioWattVision($entrada));
                              } catch (erro) {
                                return JSON.stringify({ erro: String(erro) });
                              }
                            })()
                            """.trimIndent()
                        ) { valor ->
                            if (!espera.isActive) return@evaluateJavascript
                            val resultado = runCatching {
                                val json = JSONObject(JSONTokener(valor).nextValue() as String)
                                check(!json.has("erro")) { json.optString("erro") }
                                check(json.has("pdf") && json.getInt("paginas") > 0) { "PDF vazio" }
                                json
                            }
                            resultado.fold(espera::resume, espera::resumeWithException)
                        }
                    }
                }
                navegador.loadDataWithBaseURL(null, "<!doctype html><meta charset=utf-8>", "text/html", "UTF-8", null)
            }
        } finally {
            navegador.destroy()
        }
    }

internal fun imprimirRelatorio(contexto: Context, pdf: PdfRelatorio, nome: String): Boolean {
    val impressora = contexto.getSystemService<PrintManager>() ?: return false
    return runCatching {
        impressora.print("Relatório Watt Vision", object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback, extras: Bundle?
            ) {
                if (cancellationSignal.isCanceled) callback.onLayoutCancelled()
                else callback.onLayoutFinished(
                    PrintDocumentInfo.Builder(nome)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(pdf.paginas).build(),
                    oldAttributes != newAttributes
                )
            }

            override fun onWrite(
                pages: Array<out PageRange>, destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal, callback: WriteResultCallback
            ) {
                Thread {
                    val resultado = runCatching {
                        ParcelFileDescriptor.AutoCloseOutputStream(destination).use {
                            if (!cancellationSignal.isCanceled) it.write(pdf.bytes)
                        }
                    }
                    when {
                        cancellationSignal.isCanceled -> callback.onWriteCancelled()
                        resultado.isSuccess -> callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        else -> callback.onWriteFailed("Não foi possível imprimir o relatório.")
                    }
                }.start()
            }
        }, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR).build())
    }.isSuccess
}
