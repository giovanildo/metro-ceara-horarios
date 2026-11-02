package io.github.giova.metroceara.trip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.giova.metroceara.MainActivity
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.LeaveAlert
import io.github.giova.metroceara.data.LeaveScheduler
import io.github.giova.metroceara.data.LeaveStore
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.Routine
import io.github.giova.metroceara.data.RoutePoint
import io.github.giova.metroceara.data.ScheduleRepository
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.dateOf
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.isSundayWithoutService
import io.github.giova.metroceara.data.nowMinutes
import io.github.giova.metroceara.data.vehicleName
import kotlin.concurrent.thread

/**
 * Os avisos da hora de sair, na hora certa:
 * - [ACTION_FIRE]: confere a grade do dia (domingo sem operação, linha parada,
 *   horário que sumiu) e mostra a notificação, na tela de bloqueio, com a
 *   contagem até a partida e os botões;
 * - [ACTION_PLAN]: de manhã, num dia de rotina, calcula o metrô do dia e marca o aviso;
 * - [ACTION_SNOOZE] ("Avisar de novo em 5 min") e [ACTION_DISMISS];
 * - reinício do celular, atualização do app e liberação do horário exato: remarca tudo.
 */
class LeaveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val id = intent.getStringExtra(EXTRA_ID)
        // A grade e o roteiro leem o banco: fora da thread principal.
        val pending = goAsync()
        thread {
            try {
                when (intent.action) {
                    ACTION_FIRE -> id?.let { fire(app, it) }
                    ACTION_PLAN -> id?.let { plan(app, it) }
                    ACTION_SNOOZE -> id?.let { snooze(app, it) }
                    ACTION_DISMISS -> id?.let { dismiss(app, it) }
                    else -> LeaveScheduler.rescheduleAll(app)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun fire(context: Context, id: String) {
        val alert = LeaveStore.alert(context, id) ?: return
        createChannel(context)
        val now = System.currentTimeMillis()
        if (alert.departureMillis < now) {
            LeaveStore.removeAlert(context, id)
            return
        }
        val today = alert.date == dateOf()
        val repo = ScheduleRepository(context)
        var departure = alert.departure
        var next: Int? = null
        var problem: String? = null
        if (today) {
            when {
                isSundayWithoutService() -> problem = context.getString(R.string.leave_n_sunday)
                Regras.current.stopped(ScheduleStore.today(), alert.routeName) ->
                    problem = context.getString(R.string.leave_n_stopped, alert.routeName)
                else -> {
                    val times = repo.departures(alert.stopId, alert.routeId, alert.directionId)
                    // O horário pode ter mudado (operação especial): vale o seguinte na grade de hoje.
                    if (times.isNotEmpty() && departure !in times) {
                        times.firstOrNull { it >= departure }?.let { departure = it }
                            ?: run { problem = context.getString(R.string.leave_n_no_more, alert.stopName) }
                    }
                    next = times.firstOrNull { it > departure }
                }
            }
        }
        val shown = alert.copy(departure = departure, snoozeUntil = null, fired = true)
        LeaveStore.putAlert(context, shown)
        val vehicle = vehicleName(alert.routeName)
        val title = problem?.let { context.getString(R.string.leave_n_title_problem) }
            ?: context.getString(R.string.leave_n_title, vehicle, formatTime(departure))
        val text = problem ?: buildString {
            append(context.getString(R.string.leave_n_text, alert.stopName, alert.headsign))
            alert.destName?.let { dest ->
                append(' ')
                append(
                    alert.arrival?.let { context.getString(R.string.leave_n_arrival, dest, formatTime(it + departure - alert.departure)) }
                        ?: context.getString(R.string.leave_n_to, dest),
                )
            }
            next?.let { append(' ').append(context.getString(R.string.leave_n_next, formatTime(it))) }
        }
        val untilMillis = shown.departureMillis - now
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open(context, shown, startTrip = false))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
        if (problem == null) {
            // A contagem até a partida, atualizada pelo próprio sistema; some 2 min depois dela.
            builder.setWhen(shown.departureMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setTimeoutAfter(untilMillis + 2 * 60_000L)
                .setSubText(context.getString(R.string.leave_n_subtext))
            if (shown.destStopId != null) {
                builder.addAction(0, context.getString(R.string.leave_n_start), open(context, shown, startTrip = true))
            } else {
                builder.addAction(0, context.getString(R.string.leave_n_schedule), open(context, shown, startTrip = false))
            }
            if (untilMillis > SNOOZE_MILLIS + 60_000L) {
                builder.addAction(0, context.getString(R.string.leave_n_snooze), broadcast(context, ACTION_SNOOZE, id))
            }
        } else {
            builder.setTimeoutAfter(untilMillis + 2 * 60_000L)
        }
        builder.addAction(
            0,
            context.getString(if (alert.routineId != null) R.string.leave_n_skip else R.string.leave_n_dismiss),
            broadcast(context, ACTION_DISMISS, id),
        )
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(context).notify(notificationId(id), builder.build()) }
        }
        // A rotina já fica com a próxima vez marcada.
        alert.routineId?.let { rid -> LeaveStore.routine(context, rid)?.let { LeaveScheduler.planNext(context, it) } }
    }

    /** Rotina: o metrô de hoje, pela grade do dia, e o aviso marcado para ele. */
    private fun plan(context: Context, routineId: String) {
        val routine = LeaveStore.routine(context, routineId) ?: return
        if (!routine.enabled) return
        val alert = RoutinePlanner.today(context, routine)
        if (alert != null) LeaveScheduler.add(context, alert) else LeaveScheduler.planNext(context, routine, fromTomorrow = true)
    }

    private fun snooze(context: Context, id: String) {
        val alert = LeaveStore.alert(context, id) ?: return
        NotificationManagerCompat.from(context).cancel(notificationId(id))
        val at = System.currentTimeMillis() + SNOOZE_MILLIS
        if (at < alert.departureMillis) LeaveScheduler.add(context, alert.copy(snoozeUntil = at, fired = false))
    }

    private fun dismiss(context: Context, id: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(id))
        val alert = LeaveStore.alert(context, id)
        LeaveScheduler.cancel(context, id)
        alert?.routineId?.let { rid -> LeaveStore.routine(context, rid)?.let { LeaveScheduler.planNext(context, it, fromTomorrow = true) } }
    }

    private fun open(context: Context, alert: LeaveAlert, startTrip: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_LEAVE_STOP, alert.stopId)
            .putExtra(MainActivity.EXTRA_LEAVE_ROUTE, alert.routeId)
        if (startTrip && alert.destStopId != null && alert.destRouteId != null) {
            intent.putExtra(MainActivity.EXTRA_LEAVE_DEST_STOP, alert.destStopId)
                .putExtra(MainActivity.EXTRA_LEAVE_DEST_ROUTE, alert.destRouteId)
        }
        return PendingIntent.getActivity(
            context, (alert.id + startTrip).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun broadcast(context: Context, action: String, id: String): PendingIntent =
        LeaveScheduler.pending(context, action, id)

    companion object {
        const val ACTION_FIRE = "io.github.giova.metroceara.LEAVE_FIRE"
        const val ACTION_PLAN = "io.github.giova.metroceara.LEAVE_PLAN"
        const val ACTION_SNOOZE = "io.github.giova.metroceara.LEAVE_SNOOZE"
        const val ACTION_DISMISS = "io.github.giova.metroceara.LEAVE_DISMISS"
        const val EXTRA_ID = "leave_id"
        private const val CHANNEL = "leave_time"
        private const val SNOOZE_MILLIS = 5 * 60_000L

        private fun notificationId(id: String) = 2000 + (id.hashCode() and 0xFFFF)

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.leave_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                },
            )
        }
    }
}

/** O metrô do dia de uma rotina, pela grade e pelo roteiro de hoje. */
object RoutinePlanner {

    /** O aviso de hoje, ou `null` se hoje não há como (domingo sem operação, linha parada, nada que chegue). */
    fun today(context: Context, routine: Routine): LeaveAlert? {
        if (isSundayWithoutService()) return null
        val repo = ScheduleRepository(context)
        val settings = SettingsStore.get(context)
        val router = JourneyPlanner.router(repo, settings)
        val day = ScheduleStore.today()
        val stopped = repo.routes().filter { Regras.current.stopped(day, it.name) }.map { it.id }.toSet()
        val origin = RoutePoint(routine.originRouteId, routine.originStopId)
        val dest = RoutePoint(routine.destRouteId, routine.destStopId)
        val now = nowMinutes()
        val itinerary = if (routine.arriveBy) {
            // Chegar até: o que sai mais tarde e ainda chega a tempo.
            router.plan(origin, dest, maxOf(now, routine.minutes - 240), 30, stopped)
                .filter { it.arrival <= routine.minutes }
                .maxByOrNull { it.departure }
        } else {
            router.plan(origin, dest, maxOf(now, routine.minutes), 1, stopped).firstOrNull()
        } ?: return null
        val leg = itinerary.legs.first()
        val route = repo.route(leg.routeId) ?: return null
        val headsign = repo.directions(leg.routeId).firstOrNull { it.id == leg.directionId }?.headsign.orEmpty()
        return LeaveAlert(
            id = "r-${routine.id}-$day",
            date = day,
            departure = leg.departure,
            before = routine.before,
            stopId = leg.fromStop,
            routeId = leg.routeId,
            directionId = leg.directionId,
            stopName = JourneyPlanner.plainName(repo.stop(leg.fromStop)?.name.orEmpty()),
            routeName = route.name,
            headsign = headsign,
            destStopId = routine.destStopId,
            destRouteId = routine.destRouteId,
            destName = routine.destName,
            arrival = itinerary.arrival,
            routineId = routine.id,
        )
    }
}
