package io.github.giova.metroceara.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import io.github.giova.metroceara.MainActivity
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.PinnedStation
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleRepository
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.isSundayToday
import io.github.giova.metroceara.data.nowMinutes
import kotlin.concurrent.thread

/** Widget pequeno (2×2): o próximo de cada sentido. */
class StationWidgetSmall : StationWidgetProvider()

/** Widget largo (4×2): os dois próximos de cada sentido e o aviso do dia. */
class StationWidgetWide : StationWidgetProvider()

open class StationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = StationWidget.update(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == StationWidget.ACTION_TICK) StationWidget.update(context)
    }

    override fun onDisabled(context: Context) = StationWidget.cancelTick(context)
}

/**
 * O widget da estação: a fixada com a estrela ou, sem ela, a última aberta.
 * Os horários saem da grade já baixada (sem internet nem GPS). Ele se
 * atualiza logo depois de cada partida mostrada (um alarme sem hora exata,
 * que quase não gasta bateria), quando o app baixa a grade ou as regras, e
 * quando a estação muda.
 */
object StationWidget {

    const val ACTION_TICK = "io.github.giova.metroceara.widget.TICK"
    const val EXTRA_STOP = "widget_stop"
    const val EXTRA_ROUTE = "widget_route"

    /** Um sentido no widget: o destino e as próximas partidas (ou a primeira do próximo dia). */
    private data class Row(val headsign: String, val times: List<Int>, val nextDayLabel: String?)

    private data class Content(
        val stopId: String,
        val routeId: String,
        val stopName: String,
        val routeName: String,
        val colorHex: String,
        val rows: List<Row>,
        val note: String?,
        val warning: Boolean,
        val nextTick: Int?,
    )

    /** Redesenha todos os widgets, fora da thread principal. */
    fun update(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val small = manager.getAppWidgetIds(ComponentName(app, StationWidgetSmall::class.java))
        val wide = manager.getAppWidgetIds(ComponentName(app, StationWidgetWide::class.java))
        if (small.isEmpty() && wide.isEmpty()) return
        thread(name = "station-widget") {
            val content = runCatching { load(app) }.getOrNull()
            small.forEach { manager.updateAppWidget(it, render(app, content, wide = false)) }
            wide.forEach { manager.updateAppWidget(it, render(app, content, wide = true)) }
            scheduleTick(app, content?.nextTick)
        }
    }

    private fun load(context: Context): Content? {
        val pinned = PinnedStation(context)
        val (stopId, routeId) = pinned.get() ?: pinned.lastOpened() ?: return null
        // Os bancos ficam abertos: MetroforDatabase reaproveita a mesma conexão
        // que o app usa (fechar aqui derrubava o app aberto).
        val repo = ScheduleRepository(context)
        run {
            val station = repo.station(stopId, routeId) ?: return null
            val now = nowMinutes()
            val rows = repo.boardingDirections(stopId, routeId).take(2).map { direction ->
                val today = repo.departures(stopId, routeId, direction.id).filter { it >= now }.take(2)
                val end = if (today.isEmpty()) repo.dayEnd(stopId, routeId, direction.id) else null
                Row(
                    headsign = direction.headsign,
                    times = today.ifEmpty { listOfNotNull(end?.nextFirst) },
                    nextDayLabel = end?.nextFirst?.let {
                        context.getString(if (end.nextIsMonday) R.string.widget_monday else R.string.widget_tomorrow)
                    },
                )
            }
            val day = ScheduleStore.today()
            val operation = Regras.current.operations[day]
            val (note, warning) = when {
                Regras.current.stopped(day, station.routeName) -> context.getString(R.string.line_stopped_today) to true
                operation != null -> context.getString(R.string.widget_operation, operation.title) to false
                isSundayToday() -> context.getString(R.string.widget_sunday) to true
                else -> null to false
            }
            // Atualiza depois que o primeiro horário de hoje passar.
            val next = rows.filter { it.nextDayLabel == null }.mapNotNull { it.times.firstOrNull() }.minOrNull()
            return Content(stopId, routeId, plainName(station.stopName), station.routeName, station.routeColor,
                rows, note, warning, next)
        }
    }

    private fun render(context: Context, content: Content?, wide: Boolean): RemoteViews {
        val views = RemoteViews(context.packageName, if (wide) R.layout.widget_wide else R.layout.widget_small)
        if (content == null) {
            views.setTextViewText(R.id.widget_station, context.getString(R.string.app_name))
            views.setViewVisibility(R.id.widget_line, View.GONE)
            views.setViewVisibility(R.id.widget_dot, View.GONE)
            views.setViewVisibility(R.id.widget_row1, View.GONE)
            views.setViewVisibility(R.id.widget_row2, View.GONE)
            views.setTextViewText(R.id.widget_note, context.getString(R.string.widget_no_station))
            views.setViewVisibility(R.id.widget_note, View.VISIBLE)
            views.setViewVisibility(R.id.widget_warning, View.GONE)
            views.setOnClickPendingIntent(R.id.widget_root, openApp(context, null))
            return views
        }
        views.setTextViewText(R.id.widget_station, content.stopName)
        views.setTextViewText(R.id.widget_line, content.routeName)
        views.setViewVisibility(R.id.widget_line, View.VISIBLE)
        views.setViewVisibility(R.id.widget_dot, View.VISIBLE)
        runCatching { views.setTextColor(R.id.widget_dot, Color.parseColor("#" + content.colorHex.removePrefix("#"))) }
        listOf(
            Triple(R.id.widget_row1, R.id.widget_dir1, R.id.widget_time1) to R.id.widget_more1,
            Triple(R.id.widget_row2, R.id.widget_dir2, R.id.widget_time2) to R.id.widget_more2,
        ).forEachIndexed { i, (ids, moreId) ->
            val row = content.rows.getOrNull(i)
            views.setViewVisibility(ids.first, if (row == null) View.GONE else View.VISIBLE)
            if (row == null) return@forEachIndexed
            views.setTextViewText(ids.second, "→ " + plainName(row.headsign))
            views.setTextViewText(ids.third, row.times.firstOrNull()?.let(::formatTime) ?: "—")
            val more = when {
                row.nextDayLabel != null && row.times.isNotEmpty() -> row.nextDayLabel
                row.times.isEmpty() -> context.getString(R.string.widget_no_more)
                wide -> row.times.getOrNull(1)?.let(::formatTime).orEmpty()
                else -> ""
            }
            views.setTextViewText(moreId, more)
            views.setViewVisibility(moreId, if (more.isEmpty()) View.GONE else View.VISIBLE)
        }
        // Dois avisos prontos no layout (normal e alerta), com as cores do tema:
        // uma cor posta aqui não acompanharia a troca para o tema escuro.
        val shown = if (content.warning) R.id.widget_warning else R.id.widget_note
        val hidden = if (content.warning) R.id.widget_note else R.id.widget_warning
        views.setTextViewText(shown, content.note.orEmpty())
        views.setViewVisibility(shown, if (content.note == null) View.GONE else View.VISIBLE)
        views.setViewVisibility(hidden, View.GONE)
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, content))
        return views
    }

    /** Tocar abre o app na tela da estação. */
    private fun openApp(context: Context, content: Content?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            content?.let {
                putExtra(EXTRA_STOP, it.stopId)
                putExtra(EXTRA_ROUTE, it.routeId)
            }
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun tickIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, StationWidgetWide::class.java).setAction(ACTION_TICK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Próxima atualização: um minuto depois da partida mais próxima, ou em
     * [MAX_WAIT_MINUTES] (o dia vira, a grade muda). Sem hora exata: o Android
     * pode juntar com outros alarmes e atrasar um pouco, sem pedir permissão.
     */
    private fun scheduleTick(context: Context, nextDeparture: Int?) {
        val now = nowMinutes()
        val wait = ((nextDeparture ?: (now + MAX_WAIT_MINUTES)) - now + 1).coerceIn(1, MAX_WAIT_MINUTES)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.set(AlarmManager.RTC, System.currentTimeMillis() + wait * 60_000L, tickIntent(context))
    }

    fun cancelTick(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(tickIntent(context))
    }

    /** "Parangaba - NE" vira "Parangaba": o sufixo da grade só separa as plataformas. */
    private fun plainName(name: String) = name.replace(Regex("\\s+-\\s+(AE|NE)$", RegexOption.IGNORE_CASE), "")

    private const val MAX_WAIT_MINUTES = 30
}
