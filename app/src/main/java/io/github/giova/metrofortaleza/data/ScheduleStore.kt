package io.github.giova.metrofortaleza.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.HttpsURLConnection

/** Tipo de dia, para escolher a grade guardada mais parecida com hoje. */
enum class DayType { WEEKDAY, SATURDAY, SUNDAY }

/** De onde vêm os horários que o app está mostrando. */
data class ScheduleSource(
    val kind: Kind,
    /** Dia em que a grade foi baixada (`yyyy-MM-dd`), ou `null` para a do APK. */
    val date: String?,
    /** Quando foi baixada, em millis, ou 0 para a do APK. */
    val syncedAt: Long,
) {
    enum class Kind {
        /** Baixada hoje: é a grade do dia. */
        TODAY,

        /** Baixada num dia anterior do mesmo tipo (dia útil, sábado ou domingo). */
        SAME_DAY_TYPE,

        /** A que veio dentro do APK (gerada num dia útil). */
        BUNDLED,
    }
}

/**
 * Grades de horário baixadas no próprio celular.
 *
 * O GTFS do Metrofor é gerado a cada requisição e traz a grade **do dia**:
 * dia útil, sábado, ou a operação especial de um domingo de eleição — mesmo
 * com o `calendar.txt` dizendo que vale para todos os dias. Uma tarefa do
 * GitHub (`.github/workflows/dados-web.yml`) baixa o feed todo dia às 04:30 e
 * guarda o zip de cada tipo de dia no ramo `dados`. O app baixa de lá — com
 * certificado válido, ao contrário do site do Metrofor — e guarda a última
 * grade de cada [DayType], com a data que o índice informa.
 */
object ScheduleStore {

    /** O espelho dos dados no GitHub (ramo `dados`). */
    const val MIRROR = "https://raw.githubusercontent.com/giovanildo/metro-fortaleza-horarios/dados/"
    private const val RETRY_MILLIS = 15L * 60 * 1000

    private val zone: TimeZone = TimeZone.getTimeZone("America/Fortaleza")

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Date())

    fun todayType(): DayType = typeOf(Calendar.getInstance(zone))

    fun tomorrowType(): DayType = typeOf(Calendar.getInstance(zone).apply { add(Calendar.DAY_OF_MONTH, 1) })

    private fun typeOf(calendar: Calendar): DayType = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> DayType.SATURDAY
        Calendar.SUNDAY -> DayType.SUNDAY
        else -> DayType.WEEKDAY
    }

    /** A última grade baixada de [type], se houver. */
    fun stored(context: Context, type: DayType): File? =
        file(context, type).takeIf { it.exists() && prefs(context).getString(keyDate(type), null) != null }

    /** O arquivo da grade a usar agora e de onde ele veio. `null` = usar a do APK. */
    fun current(context: Context): Pair<File, ScheduleSource>? {
        val type = todayType()
        val file = file(context, type)
        val date = prefs(context).getString(keyDate(type), null)
        if (!file.exists() || date == null) return null
        val kind = if (date == today()) ScheduleSource.Kind.TODAY else ScheduleSource.Kind.SAME_DAY_TYPE
        return file to ScheduleSource(kind, date, prefs(context).getLong(keySyncedAt(type), 0L))
    }

    /** Se ainda não há grade de hoje e não tentamos há pouco. */
    fun needsSync(context: Context): Boolean {
        val prefs = prefs(context)
        if (prefs.getString(keyDate(todayType()), null) == today()) return false
        return System.currentTimeMillis() - prefs.getLong(KEY_LAST_ATTEMPT, 0L) > RETRY_MILLIS
    }

    /**
     * Baixa do espelho a grade do tipo de hoje, monta o banco e troca a
     * guardada. Bloqueia: chamar fora da main. Qualquer falha — índice sem
     * esse tipo, download cortado, zip incompleto, banco sem viagens — lança
     * exceção e mantém a grade anterior intacta. Se a guardada já é a mesma
     * do espelho, não baixa de novo: o resultado seria idêntico.
     */
    fun sync(context: Context, bikeSource: File?) {
        prefs(context).edit { putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis()) }
        val type = todayType()
        val entry = JSONObject(String(fetch(MIRROR + "indice.json"))).getJSONObject("tipos")
            .optJSONObject(mirrorName(type)) ?: error("o espelho ainda não tem grade de ${mirrorName(type)}")
        val zipName = entry.optString("gtfs").ifBlank { error("o espelho ainda não tem o GTFS de ${mirrorName(type)}") }
        val date = entry.getString("data")
        if (prefs(context).getString(keyDate(type), null) == date && file(context, type).exists()) {
            DevLog.log("sync_same", "day_type" to type.name, "date" to date)
            return
        }
        val zip = fetch(MIRROR + zipName)
        check(entry.optLong("bytes", -1) < 0 || entry.getLong("bytes") == zip.size.toLong()) {
            "GTFS com tamanho diferente do índice: ${zip.size} de ${entry.getLong("bytes")} bytes"
        }
        val target = file(context, type)
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        try {
            GtfsImporter.import(zip, temp, bikeSource, MIRROR + zipName)
            // Só troca se o banco novo é utilizável e tem viagens; senão fica a
            // grade que já existe.
            check(MetroforDatabase.hasSchedule(temp)) { "grade baixada sem viagens ou corrompida" }
            // Renomear no fim: um banco pela metade nunca fica no lugar do bom.
            check(temp.renameTo(target)) { "não foi possível instalar ${target.name}" }
        } finally {
            temp.delete()
            File(temp.path + "-journal").delete()
        }
        // A hora é a de quando o espelho baixou do Metrofor, não a deste download.
        val publishedAt = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(entry.getString("atualizado"))!!.time
        }.getOrDefault(System.currentTimeMillis())
        prefs(context).edit {
            putString(keyDate(type), date)
            putLong(keySyncedAt(type), publishedAt)
        }
        if (DevLog.isEnabled) {
            DevLog.log("sync_ok", "day_type" to type.name, "date" to date, "bytes" to zip.size, "lines" to summary(target))
        }
    }

    /** Por linha: viagens, primeira e última partida — para comparar grades no registro. */
    private fun summary(file: File): String = runCatching {
        android.database.sqlite.SQLiteDatabase.openDatabase(
            file.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
        ).use { db ->
            db.rawQuery(
                """
                SELECT r.name, COUNT(DISTINCT d.trip_id), MIN(d.minutes), MAX(d.minutes)
                FROM departure d JOIN route r ON r.route_id = d.route_id
                GROUP BY r.route_id ORDER BY r.sort_order
                """.trimIndent(),
                null,
            ).use { c ->
                buildList {
                    while (c.moveToNext()) add("${c.getString(0)}: ${c.getInt(1)} viagens ${formatTime(c.getInt(2))}-${formatTime(c.getInt(3))}")
                }.joinToString(" | ")
            }
        }
    }.getOrDefault("?")

    private fun mirrorName(type: DayType) = when (type) {
        DayType.WEEKDAY -> "dia-util"
        DayType.SATURDAY -> "sabado"
        DayType.SUNDAY -> "domingo"
    }

    private fun fetch(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "metro-fortaleza-horarios/1.0")
        return try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode} em $url" }
            val bytes = connection.inputStream.use { it.readBytes() }
            // Conexão que cai no meio pode entregar menos bytes sem dar erro.
            val expected = connection.contentLengthLong
            check(expected <= 0 || bytes.size.toLong() == expected) {
                "download incompleto: ${bytes.size} de $expected bytes"
            }
            bytes
        } finally {
            connection.disconnect()
        }
    }

    /** Apaga a grade guardada de [type] (arquivo e datas), por exemplo se ela estiver corrompida. */
    fun forget(context: Context, type: DayType) {
        file(context, type).delete()
        prefs(context).edit {
            remove(keyDate(type))
            remove(keySyncedAt(type))
        }
    }

    private fun file(context: Context, type: DayType) =
        File(context.applicationContext.noBackupFilesDir, "schedules/${type.name.lowercase()}.db")

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_schedules", Context.MODE_PRIVATE)

    private fun keyDate(type: DayType) = "date_${type.name.lowercase()}"
    private fun keySyncedAt(type: DayType) = "synced_at_${type.name.lowercase()}"
    private const val KEY_LAST_ATTEMPT = "last_attempt"
}
