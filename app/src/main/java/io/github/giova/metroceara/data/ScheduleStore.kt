package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
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
 * GitHub (`.github/workflows/dados-web.yml`) baixa o feed várias vezes por dia
 * e guarda o zip de cada tipo de dia no ramo `dados` (o espelho).
 *
 * O app baixa a grade só do Metrofor: ela muda também durante o dia (em 04/11,
 * seis versões da Linha Sul até as 12h27), e o espelho só a vê quando a tarefa
 * roda. O espelho fica para a lite (o navegador não consegue baixar do
 * Metrofor) e para o regras.json. Se o Metrofor falhar, fica a última grade
 * guardada. A grade de um dia comum também é guardada à parte: é a previsão de
 * amanhã quando a guardada for de um dia especial (feriado, eleição).
 */
object ScheduleStore {

    /** O espelho dos dados no GitHub (ramo `dados`): aqui, só o regras.json. */
    const val MIRROR = "https://raw.githubusercontent.com/giovanildo/metro-ceara-horarios/dados/"

    /** O GTFS do Metrofor: gerado a cada pedido, com a grade do dia. */
    const val FEED_URL = "https://info.metrofor.ce.gov.br/gtfs_file"

    /**
     * Antes disto (minutos depois da meia-noite) o Metrofor pode ainda servir a
     * grade de ontem, e o feed não diz de que dia é: nesse intervalo, não baixa.
     */
    private const val DIRECT_AFTER_MINUTES = 40
    private val zone: TimeZone = TimeZone.getTimeZone("America/Fortaleza")

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Date())

    /**
     * Antes das 00:40 o app não baixa a grade (o Metrofor pode ainda servir a de
     * ontem): a tela diz isso, em vez de pedir internet.
     */
    fun beforeFeedTime(): Boolean {
        val now = Calendar.getInstance(zone)
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) < DIRECT_AFTER_MINUTES
    }

    fun todayType(): DayType = typeOf(Calendar.getInstance(zone))

    fun tomorrow(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Calendar.getInstance(zone).apply { add(Calendar.DAY_OF_MONTH, 1) }.time)

    fun tomorrowType(): DayType = typeOf(Calendar.getInstance(zone).apply { add(Calendar.DAY_OF_MONTH, 1) })

    /** Depois de amanhã: a segunda, quando amanhã é domingo. */
    fun dayAfterTomorrow(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = zone }
        .format(Calendar.getInstance(zone).apply { add(Calendar.DAY_OF_MONTH, 2) }.time)

    /**
     * O Metrofor anunciou operação especial nesse dia (feriado, ENEM, eleição) e
     * a notícia não diz que o horário é o de sempre. A grade desse dia só sai
     * depois da meia-noite, e a grade baixada nele não serve de previsão.
     */
    fun isSpecial(date: String?): Boolean = date != null && Regras.current.operations[date]?.normal == false

    private fun typeOf(calendar: Calendar): DayType = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> DayType.SATURDAY
        Calendar.SUNDAY -> DayType.SUNDAY
        else -> DayType.WEEKDAY
    }

    /** A última grade baixada de [type], se houver. */
    fun stored(context: Context, type: DayType): File? =
        file(context, type).takeIf { it.exists() && prefs(context).getString(keyDate(type), null) != null }

    /**
     * A grade para prever um dia de [type]: a última baixada, se não foi de um dia
     * especial; senão, a última de dia comum guardada à parte. Sem isso, a grade
     * de Finados virou a previsão da terça seguinte.
     */
    fun forecast(context: Context, type: DayType): File? {
        stored(context, type)?.takeIf { !isSpecial(prefs(context).getString(keyDate(type), null)) }?.let { return it }
        return normalFile(context, type).takeIf { it.exists() && prefs(context).getString(keyNormalDate(type), null) != null }
    }

    /** O arquivo da grade a usar agora e de onde ele veio. `null` = usar a do APK. */
    fun current(context: Context): Pair<File, ScheduleSource>? {
        val type = todayType()
        val file = file(context, type)
        val date = prefs(context).getString(keyDate(type), null)
        if (!file.exists() || date == null) return null
        val kind = if (date == today()) ScheduleSource.Kind.TODAY else ScheduleSource.Kind.SAME_DAY_TYPE
        return file to ScheduleSource(kind, date, prefs(context).getLong(keySyncedAt(type), 0L))
    }

    /**
     * Baixa do Metrofor a grade de hoje, monta o banco e troca a guardada.
     * Bloqueia: chamar fora da main. Qualquer falha — site fora do ar, download
     * cortado, zip incompleto, banco sem viagens, antes das 00:40 — lança
     * exceção e mantém a grade anterior intacta. Baixa sempre, sem comparar
     * com a guardada: o arquivo é pequeno (~30 KB) e assim qualquer mudança do
     * dia chega na próxima abertura do app.
     */
    fun sync(context: Context, bikeSource: File?) {
        val type = todayType()
        check(!beforeFeedTime()) { "antes de 00:40 o Metrofor pode servir a grade de ontem" }
        val zip = fetch(FEED_URL, connectTimeout = 8_000, readTimeout = 15_000)
        val target = file(context, type)
        installZip(zip, target, bikeSource, FEED_URL)
        val date = today()
        prefs(context).edit {
            putString(keyDate(type), date)
            putLong(keySyncedAt(type), System.currentTimeMillis())
        }
        if (DevLog.isEnabled) {
            DevLog.log("sync_ok", "source" to "metrofor", "day_type" to type.name, "date" to date, "bytes" to zip.size, "lines" to summary(target))
        }
        // Dia comum: guarda também à parte, para a previsão depois de um dia
        // especial. Falhar aqui não desfaz a grade de hoje.
        if (!isSpecial(date)) {
            runCatching {
                // Copia e renomeia: quem estiver lendo a previsão não vê um arquivo pela metade.
                val normal = normalFile(context, type)
                val temp = File(normal.parentFile, "${normal.name}.tmp")
                target.copyTo(temp, overwrite = true)
                check(temp.renameTo(normal)) { "não foi possível guardar ${normal.name}" }
                prefs(context).edit { putString(keyNormalDate(type), date) }
            }.onFailure { DevLog.log("previsao_sync_fail", "reason" to (it.message ?: it.javaClass.simpleName)) }
        }
    }

    /** Monta o banco em [target] a partir do zip; só troca o atual se o novo tiver viagens. */
    private fun installZip(zip: ByteArray, target: File, bikeSource: File?, sourceUrl: String) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        try {
            GtfsImporter.import(zip, temp, bikeSource, sourceUrl)
            // Só troca se o banco novo é utilizável e tem viagens; senão fica a
            // grade que já existe.
            check(MetroforDatabase.hasSchedule(temp)) { "grade baixada sem viagens ou corrompida" }
            // Renomear no fim: um banco pela metade nunca fica no lugar do bom.
            check(temp.renameTo(target)) { "não foi possível instalar ${target.name}" }
        } finally {
            temp.delete()
            File(temp.path + "-journal").delete()
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

    private fun fetch(url: String, connectTimeout: Int = 20_000, readTimeout: Int = 30_000): ByteArray {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "metro-ceara-horarios/1.0")
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

    private fun normalFile(context: Context, type: DayType) =
        File(context.applicationContext.noBackupFilesDir, "schedules/normal_${type.name.lowercase()}.db")

    /**
     * Baixa do espelho as regras do Metrofor (bike, integrações, passagem, pico)
     * e passa a usá-las. Bloqueia; falha (rede ou arquivo com erro) lança e
     * mantém as anteriores.
     *
     * Só baixa se mudou: manda a etiqueta (ETag) da cópia guardada e, se o
     * GitHub responder 304, fica com ela. O arquivo tem ~120 KB e o app o pedia
     * a cada abertura; assim quase sempre a resposta é só "não mudou".
     */
    fun syncRules(context: Context) {
        val url = MIRROR + "regras.json"
        val saved = File(context.applicationContext.filesDir, "regras.json")
        // Sem a cópia guardada, a etiqueta não vale: baixa inteiro.
        val etag = prefs(context).getString(KEY_RULES_ETAG, null)?.takeIf { saved.exists() }
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "metro-ceara-horarios/1.0")
        if (etag != null) connection.setRequestProperty("If-None-Match", etag)
        try {
            val code = connection.responseCode
            if (code == HttpsURLConnection.HTTP_NOT_MODIFIED) {
                DevLog.log("regras_sync", "status" to 304)
                return
            }
            check(code == 200) { "HTTP $code em $url" }
            val bytes = connection.inputStream.use { it.readBytes() }
            val expected = connection.contentLengthLong
            check(expected <= 0 || bytes.size.toLong() == expected) {
                "download incompleto: ${bytes.size} de $expected bytes"
            }
            Regras.update(context, String(bytes))
            // Só depois de guardar: arquivo com erro não deixa etiqueta para trás.
            prefs(context).edit { putString(KEY_RULES_ETAG, connection.getHeaderField("ETag")) }
            DevLog.log("regras_sync", "status" to 200, "bytes" to bytes.size)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * A notícia do Metrofor que explica a operação especial dos dias [days]
     * ("AAAA-MM-DD", em ordem): hoje sozinho, ou os próximos dias com a mesma notícia.
     */
    data class Notice(val title: String, val link: String?, val days: List<String>) {
        val isToday: Boolean get() = days.first() == today()
    }

    /**
     * As notícias de operação especial (feriado, ENEM, eleição) de hoje em diante, do
     * regras.json baixado: avisadas assim que aparecem, não só na véspera (quem vai
     * fazer o ENEM no domingo quer saber na quinta). Dias com a mesma notícia viram um
     * aviso só; o de hoje fica separado ([splitToday]), porque na tela vai junto com os
     * horários. [onlySpecial] tira os dias de "funcionamento normal". Sem operação
     * anunciada, nada: notícia sem critério não entra.
     */
    fun notices(onlySpecial: Boolean = false, splitToday: Boolean = true): List<Notice> {
        val today = today()
        return Regras.current.operations.filter { (day, op) -> day >= today && !(onlySpecial && op.normal) }
            .toSortedMap().entries
            .groupBy { (day, op) -> Triple(splitToday && day == today, op.title, op.link) }
            .map { (key, days) -> Notice(key.second, key.third, days.map { it.key }) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_schedules", Context.MODE_PRIVATE)

    private const val KEY_RULES_ETAG = "regras_etag"

    private fun keyDate(type: DayType) = "date_${type.name.lowercase()}"
    private fun keySyncedAt(type: DayType) = "synced_at_${type.name.lowercase()}"
    private fun keyNormalDate(type: DayType) = "normal_date_${type.name.lowercase()}"
}
