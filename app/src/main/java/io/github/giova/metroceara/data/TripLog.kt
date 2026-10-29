package io.github.giova.metroceara.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.content.edit
import io.github.giova.metroceara.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * O registro das viagens: como foi cada viagem do modo viagem, para a pessoa
 * mandar ao desenvolvedor por e-mail se quiser. Diferente do [DevLog] (escondido,
 * desligado, tudo o que o app faz), este vem ligado e guarda só as viagens:
 * do embarque até o fim, sem a tela inicial nem a caminhada até a estação (as
 * posições de antes do embarque podem mostrar onde a pessoa mora).
 *
 * Um arquivo por viagem em `files/viagens/`, com as mesmas linhas do [DevLog]
 * (os eventos `trip_*`). Fica só no celular, por [MAX_DAYS] dias. Nada sai do
 * aparelho sozinho: o envio é um e-mail que a pessoa vê e manda ([emailIntent]).
 */
object TripLog {

    private const val MAX_DAYS = 7
    private const val MAX_FILES = 40
    private const val EMAIL = "giovanildos@gmail.com"
    private const val PREFS = "metrofor_triplog"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INVITE_LAST = "invite_last"
    private const val KEY_INVITE_PENDING = "invite_pending"
    private const val KEY_INVITE_IGNORED = "invite_ignored"
    private const val MAX_IGNORED = 2
    private const val INVITE_EVERY_MILLIS = 7 * 24 * 60 * 60 * 1000L

    private val zone = TimeZone.getTimeZone("America/Fortaleza")

    @Volatile private var dir: File? = null
    /** O arquivo da viagem em andamento, ou `null` fora de viagem (ou com o registro desligado). */
    @Volatile private var current: File? = null
    /** Já embarcou? Antes disso, as posições do GPS não entram (a caminhada até a estação). */
    @Volatile private var boarded = false

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun init(context: Context) {
        dir = File(context.filesDir, "viagens")
        _enabled.value = prefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, on: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, on) }
        _enabled.value = on
        if (!on) current = null
    }

    /** Começa o arquivo de uma viagem. Chamar antes do `trip_start`. */
    fun begin() {
        val target = dir ?: return
        boarded = false
        current = if (_enabled.value) File(target, fileName(System.currentTimeMillis())) else null
    }

    /** Se a viagem em andamento está sendo guardada. */
    val recording: Boolean get() = current != null

    /** Fim da viagem; devolve o arquivo dela. Chamar depois do `trip_end`: a linha dele já está na fila. */
    fun end(): File? = current.also { current = null }

    /** Uma viagem guardada, pelo nome do arquivo (o do convite de envio). */
    fun trip(name: String): File? = dir?.let { File(it, name) }?.takeIf { it.isFile }

    /**
     * Para onde vai uma linha do [DevLog], ou `null` se ela não entra: só eventos
     * de viagem, e o GPS só depois do embarque. Roda na hora do evento, para a
     * linha ir para o arquivo da viagem certa mesmo gravada depois.
     */
    internal fun target(event: String, fields: Array<out Pair<String, Any?>>): File? {
        val file = current ?: return null
        if (!event.startsWith("trip_")) return null
        if (event == "trip_progress" && (fields.firstOrNull { it.first == "index" }?.second as? Int ?: -1) >= 0) {
            boarded = true
        }
        if (event == "trip_gps" && !boarded) return null
        return file
    }

    /** Grava a linha (na thread do [DevLog]) e apaga as viagens velhas. */
    internal fun write(file: File, line: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText(line + "\n")
            trim()
        }
    }

    /**
     * Quando convidar a mandar o registro de uma viagem com problema: no máximo
     * um convite por semana, e nenhum depois de dois convites seguidos sem
     * resposta (a pessoa já mostrou que não quer). Mandar o registro, pelo
     * convite ou pelas Configurações, zera a conta. Puro, para os testes.
     */
    data class InviteState(val lastMillis: Long = 0L, val pending: Boolean = false, val ignored: Int = 0) {
        /** O que muda numa viagem com problema: o novo estado e se mostra o convite. */
        fun onProblem(nowMillis: Long): Pair<InviteState, Boolean> {
            // O convite anterior ficou sem resposta.
            val ignoredNow = if (pending) ignored + 1 else ignored
            val base = copy(pending = false, ignored = ignoredNow)
            if (ignoredNow >= MAX_IGNORED) return base to false
            if (nowMillis - lastMillis < INVITE_EVERY_MILLIS) return base to false
            return base.copy(lastMillis = nowMillis, pending = true) to true
        }

        fun onSent() = copy(pending = false, ignored = 0)
    }

    /** Viagem com problema: grava a conta dos convites e diz se convida agora. */
    fun shouldInvite(context: Context): Boolean {
        val p = prefs(context)
        val state = InviteState(p.getLong(KEY_INVITE_LAST, 0L), p.getBoolean(KEY_INVITE_PENDING, false), p.getInt(KEY_INVITE_IGNORED, 0))
        val (next, invite) = state.onProblem(System.currentTimeMillis())
        save(context, next)
        return invite
    }

    /** A pessoa abriu o e-mail do registro: volta a poder ser convidada. */
    fun markSent(context: Context) {
        val p = prefs(context)
        save(context, InviteState(p.getLong(KEY_INVITE_LAST, 0L)).onSent())
    }

    private fun save(context: Context, state: InviteState) = prefs(context).edit {
        putLong(KEY_INVITE_LAST, state.lastMillis)
        putBoolean(KEY_INVITE_PENDING, state.pending)
        putInt(KEY_INVITE_IGNORED, state.ignored)
    }

    /** As viagens guardadas, da mais antiga para a mais nova. */
    fun trips(): List<File> {
        val cutoff = day(System.currentTimeMillis() - MAX_DAYS * DAY_MILLIS)
        return dir?.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty()
            .filter { it.name.take(10) >= cutoff && it != current }
            .sortedBy { it.name }
    }

    fun clear() {
        dir?.listFiles()?.filter { it != current }?.forEach { it.delete() }
    }

    /** Também serve para saber se uma viagem merece o convite de envio. */
    data class Summary(
        val start: Long?,
        val line: String?,
        val from: String?,
        val to: String?,
        val end: String?,
        val problems: List<String>,
        val arrivalErrorMin: String?,
        val maxGpsSilentMin: String?,
        val simulated: Boolean,
        val version: String?,
    )

    fun summary(file: File): Summary {
        var start: Long? = null
        var line: String? = null
        var from: String? = null
        var to: String? = null
        var end: String? = null
        var problems = emptyList<String>()
        var arrivalError: String? = null
        var maxSilent: String? = null
        var simulated = false
        var version: String? = null
        file.forEachLine { text ->
            val o = runCatching { JSONObject(text) }.getOrNull() ?: return@forEachLine
            if (version == null) version = o.optString("v").takeIf { it.isNotEmpty() && it != "null" }
            when (o.optString("ev")) {
                "trip_start" -> {
                    start = parseTime(o.optString("t"))
                    line = o.optString("line")
                    from = o.optString("from")
                    to = o.optString("to")
                    simulated = o.optBoolean("simulated")
                }
                "trip_review" -> {
                    problems = o.optString("problems").split(",").filter { it.isNotBlank() }
                    arrivalError = o.optString("arrival_error_min").takeIf { it.isNotEmpty() && it != "null" }
                    maxSilent = o.optString("max_gps_silent_min").takeIf { it.isNotEmpty() && it != "null" }
                }
                "trip_end" -> end = o.optString("reason")
            }
        }
        return Summary(start, line, from, to, end, problems, arrivalError, maxSilent, simulated, version)
    }

    /**
     * O e-mail para o desenvolvedor, pronto para a pessoa conferir e mandar:
     * assunto, um resumo de cada viagem no corpo e as viagens num .zip anexo.
     * `null` se não há viagem guardada.
     */
    fun emailIntent(context: Context, files: List<File> = trips()): Intent? {
        if (files.isEmpty()) return null
        markSent(context)
        val summaries = files.map(::summary)
        val outDir = File(context.cacheDir, "registro-viagens").apply { deleteRecursively(); mkdirs() }
        val zip = File(outDir, "metro-ceara-viagens-${day(System.currentTimeMillis())}.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            files.forEach { f ->
                out.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".arquivos", zip)
        val version = summaries.mapNotNull { it.version }.lastOrNull() ?: "?"
        val subject = if (summaries.size == 1) {
            val s = summaries.single()
            context.getString(R.string.trip_log_subject_one, version, s.start?.let(::shortDate) ?: "?", s.from ?: "?", s.to ?: "?")
        } else {
            context.getString(R.string.trip_log_subject_many, version, summaries.size)
        }
        val body = buildString {
            append(context.getString(R.string.trip_log_body_intro)).append("\n\n")
            summaries.forEach { append("• ").append(describe(context, it)).append("\n") }
            append("\n").append(DevLog.deviceInfo(context).joinToString(" · ") { (k, v) -> "$k: $v" })
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/zip")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(zip.name, uri)
        return Intent.createChooser(send, context.getString(R.string.trip_log_send))
    }

    /** "29/10 20:52 · Linha Sul · Benfica → Carlito Benevides · chegou · erro da chegada 1,0 min" */
    private fun describe(context: Context, s: Summary): String = listOfNotNull(
        s.start?.let(::dateTime),
        s.line,
        "${s.from ?: "?"} → ${s.to ?: "?"}",
        when (s.end) {
            "arrived" -> context.getString(R.string.trip_log_end_arrived)
            "user" -> context.getString(R.string.trip_log_end_user)
            "give_up" -> context.getString(R.string.trip_log_end_give_up)
            else -> context.getString(R.string.trip_log_end_unknown)
        },
        s.arrivalErrorMin?.let { context.getString(R.string.trip_log_arrival_error, it.replace('.', ',')) },
        s.maxGpsSilentMin?.takeIf { (it.toDoubleOrNull() ?: 0.0) > 0 }
            ?.let { context.getString(R.string.trip_log_gps_silent, it.replace('.', ',')) },
        context.getString(R.string.trip_sim).lowercase().takeIf { s.simulated },
    ).joinToString(" · ")

    private fun trim() {
        val files = dir?.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty().sortedBy { it.name }.toMutableList()
        val cutoff = day(System.currentTimeMillis() - MAX_DAYS * DAY_MILLIS)
        files.filter { it.name.take(10) < cutoff && it != current }.forEach { it.delete(); files.remove(it) }
        while (files.size > MAX_FILES) files.removeAt(0).takeIf { it != current }?.delete()
    }

    private fun fileName(millis: Long) = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        .apply { timeZone = zone }.format(Date(millis)) + ".jsonl"

    private fun day(millis: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }.format(Date(millis))

    private fun shortDate(millis: Long) = SimpleDateFormat("dd/MM", Locale.US)
        .apply { timeZone = zone }.format(Date(millis))

    private fun dateTime(millis: Long) = SimpleDateFormat("dd/MM HH:mm", Locale.US)
        .apply { timeZone = zone }.format(Date(millis))

    private fun parseTime(text: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).parse(text)?.time
    }.getOrNull()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
}
