package io.github.giova.metroceara.trip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.giova.metroceara.MainActivity
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.TripJourney
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.TripTransfer
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.lineWithArticle
import io.github.giova.metroceara.data.vehicleName
import kotlin.math.roundToInt

/**
 * Canais e notificações do modo viagem.
 *
 * O som de um canal não pode ser mudado depois de criado, então o aviso tem
 * dois canais: um com som de alarme (toca mesmo no modo vibrar) e um só com
 * vibração. O botão de som da tela escolhe qual usar.
 */
internal object TripNotifications {

    /** O aviso de cada estação diz quantas faltam só nesta (veja [stationDetail]). */
    private const val COUNT_AT_STATIONS_LEFT = 3
    const val ONGOING_ID = 1001
    private const val ALERT_ID = 1002
    private const val STATION_ID = 1003
    private const val MISSED_ID = 1004

    private const val CHANNEL_ONGOING = "trip_ongoing"
    private const val CHANNEL_ALERT_SOUND = "trip_alert_sound"
    private const val CHANNEL_ALERT_SILENT = "trip_alert_silent"
    private const val CHANNEL_STATION = "trip_station"

    private val VIBRATION = longArrayOf(0, 600, 300, 600, 300, 600)
    private const val STATION_TIMEOUT_MILLIS = 60_000L

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.trip_channel_ongoing),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT_SOUND,
                context.getString(R.string.trip_channel_alert_sound),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(
                    alarmUri(),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
        )
        // Aviso de cada estação: som curto de notificação, não o de alarme.
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATION,
                context.getString(R.string.trip_channel_station),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERT_SILENT,
                context.getString(R.string.trip_channel_alert_silent),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
        )
    }

    /**
     * O que vem depois do nome da estação no aviso de cada estação: a chegada, ou
     * "Faltam 3 estações para X." só quando faltam [COUNT_AT_STATIONS_LEFT] — em
     * toda estação ficava repetitivo, e com 2 já vem o aviso de descida.
     */
    fun stationDetail(context: Context, trip: ActiveTrip): String? {
        val plan = trip.plan
        val index = trip.progress.index.coerceIn(0, plan.stops.lastIndex)
        val left = trip.progress.stationsLeft(plan)
        return when {
            index == plan.stops.lastIndex -> context.getString(R.string.trip_arrived, plan.destination.name)
            left == COUNT_AT_STATIONS_LEFT -> context.getString(
                R.string.trip_station_text,
                context.resources.getQuantityString(R.plurals.trip_left, left, left),
                plan.destination.name,
            )
            else -> null
        }
    }

    /** Aviso curto ao alcançar cada estação, quando a pessoa ligou essa opção. */
    fun station(context: Context, trip: ActiveTrip) {
        val plan = trip.plan
        val index = trip.progress.index.coerceIn(0, plan.stops.lastIndex)
        val text = stationDetail(context, trip)
        val notification = NotificationCompat.Builder(context, CHANNEL_STATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                if (trip.progress.presumed) {
                    context.getString(R.string.trip_station_presumed, plan.stops[index].name)
                } else {
                    plan.stops[index].name
                },
            )
            .apply { if (text != null) setContentText(text) }
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setTimeoutAfter(STATION_TIMEOUT_MILLIS)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(STATION_ID, notification)
        }
    }

    /**
     * No terminal de uma viagem "sentado pelo terminal": ficar no trem, que volta.
     * Devolve o texto, para a voz falar o mesmo.
     */
    fun turnaround(context: Context, trip: ActiveTrip): String {
        val plan = trip.plan
        val terminal = plan.terminal ?: return ""
        // O trem não sai do terminal antes do horário: adiantamento não conta.
        val backAt = plan.stops[plan.turnaroundIndex + 1].scheduled + trip.progress.delayMinutes.coerceAtLeast(0.0)
        val title = context.getString(R.string.trip_turnaround_title, terminal.name, vehicleName(plan.routeName))
        val text = context.getString(R.string.trip_turnaround_text, formatTime(backAt.roundToInt()), plan.headsign)
        val notification = NotificationCompat.Builder(context, CHANNEL_STATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(STATION_ID, notification)
        }
        return "$title. $text"
    }

    /**
     * Na chegada: "Você chegou a X." (com o lembrete dos pertences, se ligado).
     * Fica na barra depois que a viagem encerra. Devolve o texto, para a voz.
     */
    fun arrived(context: Context, trip: ActiveTrip, belongings: Boolean): String {
        val text = simulated(context, trip.plan, trip.plan.transfer?.let { transferArrived(context, trip.plan.destination.name, it) }
            ?: context.getString(
                if (belongings) R.string.trip_arrived_belongings else R.string.trip_arrived,
                trip.plan.destination.name,
            ))
        val notification = NotificationCompat.Builder(context, CHANNEL_STATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(trip.plan.destination.name)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(STATION_ID, notification)
        }
        return text
    }

    /**
     * Na simulação, "(simulação)" no fim, para ninguém confundir com uma viagem de
     * verdade. No fim e não na frente: o relógio só mostra o começo do texto.
     */
    fun simulated(context: Context, plan: TripPlan, text: String): String =
        if (plan.simulated) text + " (" + context.getString(R.string.trip_sim).lowercase() + ")" else text

    /** "Desça em Parangaba e vá para a Linha Nordeste, 2 min a pé. VLT às 08:10, sentido Iate. Passagem grátis." */
    fun transferArrived(context: Context, station: String, t: TripTransfer): String =
        context.getString(
            R.string.trip_transfer_arrived, station, lineWithArticle(t.nextRoute), t.walkMinutes,
            vehicleName(t.nextRoute).replaceFirstChar { it.uppercase() }, formatTime(t.nextDeparture), t.nextHeadsign,
        ) + when (t.free) {
            true -> " " + context.getString(R.string.trip_transfer_free)
            false -> " " + context.getString(R.string.trip_transfer_paid)
            null -> ""
        }

    /** "Faltam 2 estações para Parangaba. Depois, Linha Nordeste às 08:10." */
    fun transferAlert(context: Context, trip: ActiveTrip): String {
        val left = trip.progress.stationsLeft(trip.plan)
        val t = trip.plan.transfer ?: return ""
        return context.getString(
            R.string.trip_transfer_alert_text,
            context.resources.getQuantityString(R.plurals.trip_left, left, left),
            trip.plan.destination.name, t.nextRoute, formatTime(t.nextDeparture),
        )
    }

    /**
     * "Você perdeu o VLT das 08:10. Próximo às 08:50, sentido Iate. Chega ao
     * Papicu às 09:20." Com vibração e o som de alarme, como o aviso de descida.
     */
    fun missed(context: Context, missed: TripPlan, next: TripPlan): String {
        val vehicle = vehicleName(missed.routeName)
        val title = simulated(context, next, context.getString(R.string.trip_missed_title, vehicle, formatTime(missed.origin.scheduled)))
        val text = context.getString(
            R.string.trip_missed_text, formatTime(next.origin.scheduled), next.headsign,
            next.journey?.finalName ?: next.destination.name, formatTime(next.journey?.arrival ?: next.destination.scheduled),
        )
        val sound = TripTracker.isSoundEnabled(context)
        val notification = NotificationCompat.Builder(context, if (sound) CHANNEL_ALERT_SOUND else CHANNEL_ALERT_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .apply { if (sound) setSound(alarmUri(), AudioManager.STREAM_ALARM) }
            .build()
        // Espaço próprio: o aviso de descida do trecho novo (logo em seguida, num trecho curto) não o apaga.
        runCatching { context.getSystemService(NotificationManager::class.java).notify(MISSED_ID, notification) }
        return "$title. $text"
    }

    /**
     * O que acontece agora num roteiro com integração: "Agora: Linha Sul, em Couto
     * Fernandes · Integração em Parangaba às 08:01 · Linha Nordeste às 08:10", ou,
     * esperando o trem depois de uma integração, "Indo para a Linha Nordeste ·
     * VLT às 08:10, sentido Iate · em 8 min".
     */
    fun journeyLine(context: Context, trip: ActiveTrip, journey: TripJourney): String? {
        val plan = trip.plan
        val progress = trip.progress
        if (trip.arrived && plan.transfer == null) return null
        if (progress.index < 0) {
            if (journey.current == 0) return null
            val wait = (plan.origin.scheduled + progress.delayMinutes - nowOfDay()).roundToInt().coerceAtLeast(0)
            return context.getString(
                R.string.trip_journey_walking, lineWithArticle(plan.routeName),
                vehicleName(plan.routeName).replaceFirstChar { it.uppercase() },
                formatTime(plan.origin.scheduled), plan.headsign, wait,
            )
        }
        val now = context.getString(R.string.trip_journey_now, plan.routeName, plan.stops[progress.index].name)
        val then = plan.transfer?.let {
            context.getString(R.string.trip_journey_then, plan.destination.name, formatTime(plan.destination.scheduled),
                it.nextRoute, formatTime(it.nextDeparture))
        }
        return listOfNotNull(now, then).joinToString(" · ")
    }

    /** Minutos desde a meia-noite, no fuso de Fortaleza. */
    private fun nowOfDay(): Double {
        val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("America/Fortaleza"))
        return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE) + c.get(java.util.Calendar.SECOND) / 60.0
    }

    /** A notificação fixa enquanto a viagem dura. */
    fun ongoing(context: Context, trip: ActiveTrip): android.app.Notification {
        val plan = trip.plan
        val progress = trip.progress
        val left = progress.stationsLeft(plan)
        val journey = plan.journey
        val title = simulated(context, plan, when {
            journey != null -> context.getString(
                R.string.trip_journey_title, journey.originName, journey.finalName,
                formatTime((if (plan.transfer == null) progress.arrivalMinutes.roundToInt() else journey.arrival)),
            )
            plan.transfer != null -> context.getString(R.string.trip_notification_title_transfer, plan.transfer.finalDestination, plan.destination.name)
            else -> context.getString(R.string.trip_notification_title, plan.destination.name, formatTime(progress.arrivalMinutes.roundToInt()))
        })
        val text = journey?.let { journeyLine(context, trip, it) } ?: when {
            trip.arrived -> context.getString(R.string.trip_arrived, plan.destination.name)
            progress.index < 0 -> context.getString(
                R.string.trip_waiting, plan.origin.name, formatTime((plan.origin.scheduled + progress.delayMinutes).roundToInt()),
                vehicleName(plan.routeName),
            )
            else -> context.getString(
                R.string.trip_progress_line,
                plan.stops[progress.index].name,
                context.resources.getQuantityString(R.plurals.trip_stations_left, left, left),
                formatTime(progress.arrivalMinutes.roundToInt()),
                context.getString(io.github.giova.metroceara.ui.fixLabel(progress.fix)),
            ) + (progress.gpsSilentMinutes?.let {
                " · " + context.getString(R.string.trip_gps_lost_short, it.toInt().coerceAtLeast(1))
            } ?: "")
        }
        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context))
            .addAction(0, context.getString(R.string.trip_stop), stopIntent(context))
            .apply {
                journey?.let { j ->
                    val total = (j.arrival - j.legs.first().departure).coerceAtLeast(1)
                    val done = (nowOfDay() - j.legs.first().departure).roundToInt().coerceIn(0, total)
                    setProgress(total, done, false)
                }
            }
            .build()
    }

    /**
     * O aviso de que o destino está chegando: "Benfica: prepare-se para descer" /
     * "Faltam 2 estações.". A estação vem primeiro e "simulação" e "pela tabela"
     * vão para o fim, porque o relógio mostra só o começo.
     */
    fun alert(context: Context, trip: ActiveTrip, sound: Boolean) {
        val plan = trip.plan
        val left = context.resources.getQuantityString(R.plurals.trip_left, trip.progress.stationsLeft(plan), trip.progress.stationsLeft(plan))
        val transfer = plan.transfer
        val text = listOfNotNull(
            if (transfer != null) {
                context.getString(R.string.trip_transfer_alert_notification_text, left, transfer.nextRoute, formatTime(transfer.nextDeparture))
            } else {
                "$left."
            },
            context.getString(R.string.trip_presumed_suffix).takeIf { trip.progress.presumed },
            context.getString(R.string.trip_belongings).takeIf { SettingsStore.get(context).remindBelongings && transfer == null },
        ).joinToString(" ")
        val title = simulated(context, plan, context.getString(
            if (transfer != null) R.string.trip_transfer_alert_notification_title else R.string.trip_alert_notification_title,
            plan.destination.name,
        ))
        val notification = NotificationCompat.Builder(context, if (sound) CHANNEL_ALERT_SOUND else CHANNEL_ALERT_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(VIBRATION)
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            // Antes do Android 8 não há canais: o som vai na própria notificação.
            .apply { if (sound) setSound(alarmUri(), AudioManager.STREAM_ALARM) }
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, notification)
        }
    }

    private fun alarmUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopIntent(context: Context): PendingIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, TripService::class.java).setAction(TripService.ACTION_STOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
