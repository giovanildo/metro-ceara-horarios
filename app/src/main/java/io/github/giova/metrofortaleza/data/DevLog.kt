package io.github.giova.metrofortaleza.data

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/**
 * Registro de testes de campo, só para o desenvolvedor.
 *
 * Desligado por padrão e escondido (as opções aparecem depois de tocar 7 vezes
 * na versão, na tela Sobre). Ligado, grava um evento por linha (JSON Lines) em
 * `files/devlog/AAAA-MM-DD.jsonl`, com hora, o nome do evento e os dados —
 * posições de GPS exatas incluídas. Fica só no aparelho; o botão "Salvar" copia para a pasta Downloads.
 * Guarda no máximo [MAX_DAYS] dias e [MAX_BYTES] no total; o mais antigo sai.
 */
object DevLog {

    private const val MAX_DAYS = 7
    private const val MAX_BYTES = 5L * 1024 * 1024

    private val writer = Executors.newSingleThreadExecutor()
    private val zone = TimeZone.getTimeZone("America/Fortaleza")

    @Volatile private var dir: File? = null
    @Volatile private var enabled = false

    /** Para evitar calcular dados caros quando o registro está desligado. */
    val isEnabled: Boolean get() = enabled

    private val _unlocked = MutableStateFlow(false)
    /** Se as opções de desenvolvedor já foram liberadas neste aparelho. */
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _enabledFlow = MutableStateFlow(false)
    val enabledFlow: StateFlow<Boolean> = _enabledFlow.asStateFlow()

    fun init(context: Context) {
        val app = context.applicationContext
        dir = File(app.filesDir, "devlog")
        val prefs = prefs(app)
        enabled = prefs.getBoolean(KEY_ENABLED, false)
        _enabledFlow.value = enabled
        _unlocked.value = prefs.getBoolean(KEY_UNLOCKED, false)
    }

    fun unlock(context: Context) {
        prefs(context).edit { putBoolean(KEY_UNLOCKED, true) }
        _unlocked.value = true
    }

    fun setEnabled(context: Context, on: Boolean) {
        if (on && !enabled) {
            enabled = true
            log("log_on")
        } else if (!on && enabled) {
            log("log_off")
            enabled = false
        }
        prefs(context).edit { putBoolean(KEY_ENABLED, on) }
        _enabledFlow.value = on
    }

    /** Grava um evento, se o registro estiver ligado. Barato quando desligado. */
    fun log(event: String, vararg fields: Pair<String, Any?>) {
        if (!enabled) return
        val target = dir ?: return
        val now = System.currentTimeMillis()
        val line = JSONObject().apply {
            put("t", timestamp(now))
            put("ev", event)
            fields.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
        }.toString()
        writer.execute {
            runCatching {
                target.mkdirs()
                File(target, day(now) + ".jsonl").appendText(line + "\n")
                trim(target)
            }
        }
    }

    /** Erro que derrubou o app: grava na hora, sem fila, antes de o processo morrer. */
    fun crash(error: Throwable) {
        if (!enabled) return
        val target = dir ?: return
        val now = System.currentTimeMillis()
        val line = JSONObject()
            .put("t", timestamp(now))
            .put("ev", "crash")
            .put("error", error.stackTraceToString())
            .toString()
        runCatching {
            target.mkdirs()
            File(target, day(now) + ".jsonl").appendText(line + "\n")
        }
    }

    /** Tamanho total guardado, em bytes, e quantos dias. */
    fun stats(): Pair<Long, Int> {
        val files = dir?.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty()
        return files.sumOf { it.length() } to files.size
    }

    /** Junta tudo num arquivo só (em [outDir]) para enviar. `null` se não há nada. */
    fun export(outDir: File): File? {
        val files = dir?.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty().sortedBy { it.name }
        if (files.isEmpty()) return null
        outDir.mkdirs()
        val out = File(outDir, "metro-fortaleza-log-${day(System.currentTimeMillis())}.log")
        out.writeText("")
        files.forEach { out.appendText(it.readText()) }
        return out
    }

    fun clear() {
        writer.execute { dir?.listFiles()?.forEach { it.delete() } }
    }

    /** Dados do aparelho e do app, para abrir cada sessão do registro. */
    fun deviceInfo(context: Context): Array<Pair<String, Any?>> {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
        return arrayOf(
            "app" to version,
            "android" to Build.VERSION.RELEASE,
            "sdk" to Build.VERSION.SDK_INT,
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        )
    }

    private fun trim(target: File) {
        val files = target.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty().sortedBy { it.name }.toMutableList()
        while (files.size > MAX_DAYS || (files.sumOf { it.length() } > MAX_BYTES && files.size > 1)) {
            files.removeAt(0).delete()
        }
    }

    private fun timestamp(millis: Long) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        .apply { timeZone = zone }.format(Date(millis))

    private fun day(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }.format(Date(millis))

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_devlog", Context.MODE_PRIVATE)

    private const val KEY_ENABLED = "enabled"
    private const val KEY_UNLOCKED = "unlocked"
}
