package io.github.giova.metroceara.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.giova.metroceara.trip.LeaveReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private val FORTALEZA: TimeZone = TimeZone.getTimeZone("America/Fortaleza")

/**
 * Um aviso da hora de sair, agendado pelo próprio app: na hora, ele confere a
 * grade do dia e mostra, na tela de bloqueio, quanto falta para a partida.
 * [destStopId]/[destRouteId] vêm de um roteiro ou de uma rotina (o aviso então
 * oferece o modo viagem); sem eles, o aviso é de uma partida da estação.
 */
data class LeaveAlert(
    val id: String,
    /** Dia da partida, `yyyy-MM-dd` em Fortaleza. */
    val date: String,
    /** Minutos desde a meia-noite de [date]. */
    val departure: Int,
    val before: Int,
    val stopId: String,
    val routeId: String,
    val directionId: Int,
    val stopName: String,
    val routeName: String,
    val headsign: String,
    val destStopId: String? = null,
    val destRouteId: String? = null,
    val destName: String? = null,
    /** Chegada prevista no destino (minutos), quando o aviso vem de um roteiro. */
    val arrival: Int? = null,
    val routineId: String? = null,
    /** "Avisar de novo em 5 min": o próximo aviso, em millis, no lugar de partida − antecedência. */
    val snoozeUntil: Long? = null,
    /** Já avisou (e não pediram "Mais 5 min"): remarcar ao abrir o app não avisa de novo. */
    val fired: Boolean = false,
) {
    val departureMillis: Long get() = millisAt(date, departure)
    val fireAtMillis: Long get() = snoozeUntil ?: (departureMillis - before * 60_000L)
}

/**
 * Uma rotina: o trajeto do dia a dia. Nos [days] marcados, o app calcula de
 * manhã o metrô do dia pela grade (sem domingo sem operação, sem linha parada)
 * e agenda o aviso da hora de sair. [arriveBy]: chegar até [minutes]; senão,
 * sair a partir de [minutes].
 */
data class Routine(
    val id: String,
    val name: String,
    val originStopId: String,
    val originRouteId: String,
    val originName: String,
    val destStopId: String,
    val destRouteId: String,
    val destName: String,
    /** Dias da semana, como em [Calendar.DAY_OF_WEEK]. */
    val days: Set<Int>,
    val arriveBy: Boolean,
    val minutes: Int,
    val before: Int,
    val enabled: Boolean = true,
) {
    companion object {
        val WEEKDAYS = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** Guarda os avisos marcados e as rotinas no aparelho, e avisa a interface quando mudam. */
object LeaveStore {
    private val alertsState = MutableStateFlow<List<LeaveAlert>?>(null)
    private val routinesState = MutableStateFlow<List<Routine>?>(null)

    fun alerts(context: Context): StateFlow<List<LeaveAlert>?> {
        if (alertsState.value == null) alertsState.value = loadAlerts(context)
        return alertsState.asStateFlow()
    }

    fun routines(context: Context): StateFlow<List<Routine>?> {
        if (routinesState.value == null) routinesState.value = loadRoutines(context)
        return routinesState.asStateFlow()
    }

    /** A explicação de "Alarmes e lembretes" já apareceu (uma vez só). */
    fun exactExplained(context: Context): Boolean = prefs(context).getBoolean("exact_explained", false)
    fun setExactExplained(context: Context) = prefs(context).edit().putBoolean("exact_explained", true).apply()

    fun alert(context: Context, id: String): LeaveAlert? = alerts(context).value.orEmpty().firstOrNull { it.id == id }
    fun routine(context: Context, id: String): Routine? = routines(context).value.orEmpty().firstOrNull { it.id == id }

    fun putAlert(context: Context, alert: LeaveAlert) =
        saveAlerts(context, alerts(context).value.orEmpty().filterNot { it.id == alert.id } + alert)

    fun removeAlert(context: Context, id: String) =
        saveAlerts(context, alerts(context).value.orEmpty().filterNot { it.id == id })

    fun putRoutine(context: Context, routine: Routine) {
        val list = routines(context).value.orEmpty()
        val i = list.indexOfFirst { it.id == routine.id }
        saveRoutines(context, if (i < 0) list + routine else list.toMutableList().also { it[i] = routine })
    }

    fun removeRoutine(context: Context, id: String) =
        saveRoutines(context, routines(context).value.orEmpty().filterNot { it.id == id })

    private fun saveAlerts(context: Context, list: List<LeaveAlert>) {
        prefs(context).edit().putString("alerts", JSONArray(list.map(::alertJson)).toString()).apply()
        alertsState.value = list
    }

    private fun saveRoutines(context: Context, list: List<Routine>) {
        prefs(context).edit().putString("routines", JSONArray(list.map(::routineJson)).toString()).apply()
        routinesState.value = list
    }

    private fun loadAlerts(context: Context): List<LeaveAlert> =
        parse(prefs(context).getString("alerts", null)) { o ->
            LeaveAlert(
                id = o.getString("id"), date = o.getString("date"), departure = o.getInt("departure"),
                before = o.getInt("before"), stopId = o.getString("stopId"), routeId = o.getString("routeId"),
                directionId = o.getInt("directionId"), stopName = o.getString("stopName"),
                routeName = o.getString("routeName"), headsign = o.getString("headsign"),
                destStopId = o.optString("destStopId").ifEmpty { null },
                destRouteId = o.optString("destRouteId").ifEmpty { null },
                destName = o.optString("destName").ifEmpty { null },
                arrival = if (o.has("arrival")) o.getInt("arrival") else null,
                routineId = o.optString("routineId").ifEmpty { null },
                snoozeUntil = if (o.has("snoozeUntil")) o.getLong("snoozeUntil") else null,
                fired = o.optBoolean("fired", false),
            )
        }

    private fun loadRoutines(context: Context): List<Routine> =
        parse(prefs(context).getString("routines", null)) { o ->
            val days = o.getJSONArray("days")
            Routine(
                id = o.getString("id"), name = o.getString("name"),
                originStopId = o.getString("originStopId"), originRouteId = o.getString("originRouteId"),
                originName = o.getString("originName"), destStopId = o.getString("destStopId"),
                destRouteId = o.getString("destRouteId"), destName = o.getString("destName"),
                days = (0 until days.length()).map { days.getInt(it) }.toSet(),
                arriveBy = o.getBoolean("arriveBy"), minutes = o.getInt("minutes"), before = o.getInt("before"),
                enabled = o.optBoolean("enabled", true),
            )
        }

    private fun alertJson(a: LeaveAlert) = JSONObject().apply {
        put("id", a.id); put("date", a.date); put("departure", a.departure); put("before", a.before)
        put("stopId", a.stopId); put("routeId", a.routeId); put("directionId", a.directionId)
        put("stopName", a.stopName); put("routeName", a.routeName); put("headsign", a.headsign)
        a.destStopId?.let { put("destStopId", it) }; a.destRouteId?.let { put("destRouteId", it) }
        a.destName?.let { put("destName", it) }; a.arrival?.let { put("arrival", it) }
        a.routineId?.let { put("routineId", it) }; a.snoozeUntil?.let { put("snoozeUntil", it) }
        put("fired", a.fired)
    }

    private fun routineJson(r: Routine) = JSONObject().apply {
        put("id", r.id); put("name", r.name); put("originStopId", r.originStopId); put("originRouteId", r.originRouteId)
        put("originName", r.originName); put("destStopId", r.destStopId); put("destRouteId", r.destRouteId)
        put("destName", r.destName); put("days", JSONArray(r.days.sorted())); put("arriveBy", r.arriveBy)
        put("minutes", r.minutes); put("before", r.before); put("enabled", r.enabled)
    }

    private fun <T> parse(json: String?, read: (JSONObject) -> T): List<T> = runCatching {
        val array = JSONArray(json ?: return emptyList())
        (0 until array.length()).mapNotNull { runCatching { read(array.getJSONObject(it)) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_leave", Context.MODE_PRIVATE)
}

/**
 * Agenda os avisos no AlarmManager. Com a permissão "Alarmes e lembretes"
 * (Android 12+; no 14+ vem negada), o aviso sai no minuto certo; sem ela, o
 * Android pode atrasá-lo alguns minutos. Rotinas ganham, a cada dia marcado,
 * um "planejamento" de manhã, que calcula o metrô do dia e agenda o aviso.
 */
object LeaveScheduler {

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(context).canScheduleExactAlarms()

    /** Marca (ou remarca) um aviso. */
    fun add(context: Context, alert: LeaveAlert) {
        LeaveStore.putAlert(context, alert)
        schedule(context, alert)
    }

    fun cancel(context: Context, id: String) {
        alarmManager(context).cancel(pending(context, LeaveReceiver.ACTION_FIRE, id))
        LeaveStore.removeAlert(context, id)
    }

    /** Remarca tudo: depois de reiniciar o celular, atualizar o app ou liberar o horário exato. */
    fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        LeaveStore.alerts(context).value.orEmpty().forEach { alert ->
            when {
                alert.departureMillis < now -> LeaveStore.removeAlert(context, alert.id)
                !alert.fired -> schedule(context, alert)
            }
        }
        LeaveStore.routines(context).value.orEmpty().forEach { planNext(context, it) }
    }

    /**
     * Agenda o planejamento da próxima vez da rotina: no dia marcado, umas horas
     * antes (nunca antes das 00:50, quando a grade do dia já saiu). Se é hoje e o
     * horário de planejar já passou, planeja na hora. [fromTomorrow]: hoje já foi
     * resolvido (aviso dado, "Hoje não vou" ou sem metrô hoje).
     */
    fun planNext(context: Context, routine: Routine, fromTomorrow: Boolean = false) {
        val pi = pending(context, LeaveReceiver.ACTION_PLAN, routine.id)
        alarmManager(context).cancel(pi)
        if (!routine.enabled || routine.days.isEmpty()) return
        val planMinutes = (routine.minutes - if (routine.arriveBy) PLAN_AHEAD_ARRIVE else routine.before + PLAN_AHEAD_LEAVE)
            .coerceAtLeast(EARLIEST_PLAN)
        val now = Calendar.getInstance(FORTALEZA)
        val today = dateOf(now)
        val alreadyToday = LeaveStore.alerts(context).value.orEmpty().any { it.routineId == routine.id && it.date == today }
        for (offset in (if (fromTomorrow) 1 else 0)..7) {
            val day = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            if (day.get(Calendar.DAY_OF_WEEK) !in routine.days) continue
            val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            if (offset == 0) {
                if (alreadyToday) continue
                // Hoje, passou do planejamento mas não da hora: planeja já.
                if (nowMinutes >= routine.minutes) continue
                if (nowMinutes >= planMinutes) {
                    setAlarm(context, System.currentTimeMillis() + 1_000, pi, exact = true)
                    return
                }
            }
            setAlarm(context, millisAt(dateOf(day), planMinutes), pi, exact = true)
            return
        }
    }

    fun cancelRoutine(context: Context, routine: Routine) {
        alarmManager(context).cancel(pending(context, LeaveReceiver.ACTION_PLAN, routine.id))
        LeaveStore.alerts(context).value.orEmpty().filter { it.routineId == routine.id }.forEach { cancel(context, it.id) }
    }

    private fun schedule(context: Context, alert: LeaveAlert) {
        val at = maxOf(alert.fireAtMillis, System.currentTimeMillis() + 1_000)
        setAlarm(context, at, pending(context, LeaveReceiver.ACTION_FIRE, alert.id), exact = true)
    }

    private fun setAlarm(context: Context, at: Long, pi: PendingIntent, exact: Boolean) {
        val am = alarmManager(context)
        if (exact && canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun pending(context: Context, action: String, id: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, (action + id).hashCode(),
            Intent(context, LeaveReceiver::class.java).setAction(action).putExtra(LeaveReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

    /** Rotina "chegar até": planeja 3 h antes. */
    private const val PLAN_AHEAD_ARRIVE = 180
    /** Rotina "sair às": planeja 1 h antes do aviso. */
    private const val PLAN_AHEAD_LEAVE = 60
    /** 00:50: a grade do dia já saiu (o Metrofor publica logo depois da meia-noite). */
    private const val EARLIEST_PLAN = 50
}

/** `yyyy-MM-dd` de hoje (ou de [calendar]) em Fortaleza. */
fun dateOf(calendar: Calendar = Calendar.getInstance(FORTALEZA)): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = FORTALEZA }.format(calendar.time)

/** O instante de [minutes] desde a meia-noite de [date] (Fortaleza). */
fun millisAt(date: String, minutes: Int): Long {
    val (y, m, d) = date.split("-").map { it.toInt() }
    return Calendar.getInstance(FORTALEZA).apply {
        clear()
        set(y, m - 1, d, 0, 0, 0)
        add(Calendar.MINUTE, minutes)
    }.timeInMillis
}
