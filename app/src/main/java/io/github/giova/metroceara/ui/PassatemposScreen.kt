package io.github.giova.metroceara.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val SITE = "https://giovanildo.github.io/metro-ceara-horarios/"
private const val BASE = SITE + "passatempos/"

/**
 * Os passatempos do site (Caça-estações e Cruzadinha) dentro do app. As páginas são
 * servidas com o endereço do site, para os links e o progresso (localStorage) serem
 * os mesmos de lá, mas o conteúdo sai do aparelho: abre sem sinal, até nas estações
 * subterrâneas. Com internet, a versão do site é baixada para a próxima vez.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PassatemposScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val back by rememberUpdatedState(onBack)
    var web by remember { mutableStateOf<WebView?>(null) }
    BackHandler {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else back()
    }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Passatempos.atualizar(context) } }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val caminho = Passatempos.caminho(request.url) ?: return null
                        val conteudo = Passatempos.abrir(ctx, caminho) ?: return null
                        return WebResourceResponse("text/html", "utf-8", conteudo)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        if (url.startsWith(BASE)) return false
                        // "‹ Metrô Ceará" na página dos passatempos: volta para o app.
                        if (url == SITE || url == SITE + "index.html") {
                            back()
                            return true
                        }
                        // Páginas das estações e fontes: no navegador.
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                        } catch (_: ActivityNotFoundException) {
                        }
                        return true
                    }
                }
                loadUrl(BASE)
                web = this
            }
        },
        onRelease = { it.destroy() },
    )
}

private object Passatempos {
    /** As páginas que o app conhece (as mesmas de docs/passatempos/). */
    private val PAGINAS = listOf("index.html", "caca-estacoes/index.html", "cruzadinha/index.html")

    /** "…/passatempos/cruzadinha/" → "cruzadinha/index.html"; fora dos passatempos, null. */
    fun caminho(uri: Uri): String? {
        val url = uri.buildUpon().query(null).fragment(null).build().toString()
        if (!url.startsWith(BASE)) return null
        val resto = url.removePrefix(BASE)
        val pagina = if (resto.isEmpty() || resto.endsWith("/")) resto + "index.html" else resto
        return pagina.takeIf { it in PAGINAS }
    }

    private fun baixada(context: Context, pagina: String) = File(context.filesDir, "passatempos/$pagina")

    /** A versão baixada do site, se houver; senão, a que veio no app. */
    fun abrir(context: Context, pagina: String): InputStream? {
        val arquivo = baixada(context, pagina)
        if (arquivo.isFile && arquivo.length() > 0) return arquivo.inputStream()
        return try {
            context.assets.open("passatempos/$pagina")
        } catch (_: Exception) {
            null
        }
    }

    /** Baixa do site as páginas novas (histórias e grades novas chegam sem versão nova). */
    fun atualizar(context: Context) {
        for (pagina in PAGINAS) {
            try {
                val conexao = URL(BASE + pagina).openConnection() as HttpURLConnection
                conexao.connectTimeout = 5000
                conexao.readTimeout = 8000
                try {
                    if (conexao.responseCode != 200) continue
                    val texto = conexao.inputStream.bufferedReader().use { it.readText() }
                    // Página cortada no meio (sinal caindo) não substitui a boa.
                    if (!texto.trimEnd().endsWith("</html>")) continue
                    val destino = baixada(context, pagina)
                    destino.parentFile?.mkdirs()
                    val temp = File(destino.parentFile, destino.name + ".tmp")
                    temp.writeText(texto)
                    if (!temp.renameTo(destino)) temp.delete()
                } finally {
                    conexao.disconnect()
                }
            } catch (_: Exception) {
                return // sem internet: fica com o que já tem
            }
        }
    }
}
