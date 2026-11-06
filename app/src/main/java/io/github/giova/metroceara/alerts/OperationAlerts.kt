package io.github.giova.metroceara.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import io.github.giova.metroceara.MainActivity
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.ScheduleStore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Notificação de operação especial (ENEM, eleição, feriado) assim que ela aparece no
 * regras.json, sem a pessoa precisar abrir o app: quem vai fazer a prova no domingo
 * quer saber na quinta. Cada dia é avisado uma vez só; com o app aberto, o aviso já
 * está na tela inicial e o dia só é marcado como visto ([markSeen]).
 */
object OperationAlerts {

    private const val CHANNEL = "metrofor_operations"
    private const val KEY_SEEN = "operations_seen"
    private const val NOTIFICATION_BASE_ID = 2000

    /** "hoje", "amanhã", "sábado (14/11)"; até três dias em lista, mais que isso de um a outro. */
    fun daysLabel(context: Context, days: List<String>): String {
        val zone = TimeZone.getTimeZone("America/Fortaleza")
        val today = ScheduleStore.today()
        val tomorrow = ScheduleStore.tomorrow()
        fun label(day: String): String = when (day) {
            today -> context.getString(R.string.today_word)
            tomorrow -> context.getString(R.string.tomorrow)
            else -> runCatching {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.parse(day)!!
                SimpleDateFormat("EEEE '('d/M')'", Locale("pt", "BR")).apply { timeZone = zone }.format(parsed)
            }.getOrDefault(day)
        }
        return when {
            days.size > 3 -> context.getString(R.string.notice_days_range, label(days.first()), label(days.last()))
            days.size == 1 -> label(days[0])
            else -> days.dropLast(1).joinToString(", ") { label(it) } +
                " " + context.getString(R.string.and_word) + " " + label(days.last())
        }
    }

    /** As operações de hoje em diante que mudam o horário (não as de "funcionamento normal"), uma por notícia. */
    private fun pending(): List<ScheduleStore.Notice> =
        ScheduleStore.notices(onlySpecial = true, splitToday = false)

    /** Com o app aberto: o aviso está na tela, então os dias contam como já avisados. */
    fun markSeen(context: Context) {
        save(context, seen(context) + pending().flatMap { it.days })
    }

    /** Em segundo plano: notifica as operações com algum dia ainda não avisado. */
    fun notifyNew(context: Context) {
        val seen = seen(context)
        val fresh = pending().filter { n -> n.days.any { it !in seen } }
        if (fresh.isEmpty()) return
        createChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        fresh.forEach { n ->
            val title = context.getString(R.string.notice_days, daysLabel(context, n.days)).removeSuffix(":")
            val tap = n.link?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) }
                ?: Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(n.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(n.title))
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, n.days.first().hashCode(), tap,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .setAutoCancel(true)
                .build()
            // Um id por primeiro dia: a mesma operação avisada de novo troca a anterior.
            runCatching { manager.notify(NOTIFICATION_BASE_ID + Math.floorMod(n.days.first().hashCode(), 1000), notification) }
            DevLog.log("operacao_notificada", "dias" to n.days.joinToString(","))
        }
        save(context, seen + fresh.flatMap { it.days })
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.operations_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.operations_channel_description) },
        )
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_alerts", Context.MODE_PRIVATE)

    private fun seen(context: Context): Set<String> = prefs(context).getStringSet(KEY_SEEN, null).orEmpty()

    /** Guarda só os dias de ontem em diante: os passados não voltam. */
    private fun save(context: Context, days: Set<String>) {
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }
            .format(System.currentTimeMillis() - 86_400_000L)
        prefs(context).edit { putStringSet(KEY_SEEN, days.filter { it >= yesterday }.toSet()) }
    }
}
