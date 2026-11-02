package io.github.giova.metroceara.ui

import android.content.ActivityNotFoundException
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.data.LeaveAlert
import io.github.giova.metroceara.data.LeaveScheduler
import io.github.giova.metroceara.data.dateOf
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.minutesUntil
import io.github.giova.metroceara.data.nowMinutes
import java.util.Calendar
import java.util.TimeZone

/**
 * Uma partida escolhida para avisar a hora de sair. Com [stopId]/[routeId]/
 * [directionId], dá para o próprio app avisar (e conferir a grade na hora);
 * com o destino, o aviso oferece o modo viagem.
 */
data class LeaveReminder(
    val departure: Departure,
    val stopName: String,
    val routeName: String,
    val headsign: String,
    val vehicle: String,
    val stopId: String? = null,
    val routeId: String? = null,
    val directionId: Int? = null,
    val destStopId: String? = null,
    val destRouteId: String? = null,
    val destName: String? = null,
    val arrival: Int? = null,
)

/** As antecedências oferecidas, em minutos. */
private val BEFORE_OPTIONS = listOf(5, 10, 15, 20, 30, 45)

/**
 * Avisar a hora de sair, de três jeitos:
 * - **pelo app** (o principal): o app avisa na hora, mesmo com a tela bloqueada,
 *   confere a grade do dia e mostra a contagem até a partida ([LeaveScheduler]);
 * - **alarme do relógio**: o app manda o alarme pronto para o relógio do celular,
 *   que o salva (ali dá para mudar ou apagar). Só esta partida, sem repetir: um
 *   alarme do relógio não sabe de domingo nem de feriado;
 * - **agenda**, para quem prefere o aviso discreto.
 * A antecedência é lembrada da última vez (`AppSettings.leaveBeforeMinutes`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeaveReminderDialog(reminder: LeaveReminder, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val departureTime = formatTime(reminder.departure.minutes)
    var before by remember { mutableIntStateOf(SettingsStore.get(context).leaveBeforeMinutes) }
    var askExact by remember { mutableStateOf(false) }
    val now = nowMinutes()
    // Minutos até a partida; as antecedências que já passaram somem.
    val untilDeparture = minutesUntil(reminder.departure, now)
    val options = BEFORE_OPTIONS.filter { it < untilDeparture }
    val chosen = before.takeIf { it in options } ?: options.lastOrNull()
    val alarmMinutes = chosen?.let { Math.floorMod(reminder.departure.minutes - it, 24 * 60) }
    // O alarme do relógio toca na próxima vez que a hora chegar: para amanhã, só se
    // essa hora já passou hoje (num domingo de madrugada, o de segunda tocaria no domingo).
    val alarmOk = alarmMinutes != null && (!reminder.departure.tomorrow || alarmMinutes <= now)
    val appOk = chosen != null && reminder.stopId != null && reminder.routeId != null && reminder.directionId != null

    fun remember(minutes: Int) =
        SettingsStore.save(context, SettingsStore.get(context).copy(leaveBeforeMinutes = minutes))

    fun scheduleApp() {
        val minutes = chosen ?: return
        remember(minutes)
        val date = dateOf(Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza"))
            .apply { if (reminder.departure.tomorrow) add(Calendar.DAY_OF_MONTH, 1) })
        LeaveScheduler.add(
            context,
            LeaveAlert(
                id = "p-${reminder.stopId}-${reminder.routeId}-$date-${reminder.departure.minutes}",
                date = date, departure = reminder.departure.minutes, before = minutes,
                stopId = reminder.stopId!!, routeId = reminder.routeId!!, directionId = reminder.directionId!!,
                stopName = reminder.stopName, routeName = reminder.routeName, headsign = reminder.headsign,
                destStopId = reminder.destStopId, destRouteId = reminder.destRouteId, destName = reminder.destName,
                arrival = reminder.arrival,
            ),
        )
        Toast.makeText(context, context.getString(R.string.leave_app_done, formatTime(alarmMinutes!!)), Toast.LENGTH_LONG).show()
        if (LeaveScheduler.canExact(context)) onDismiss() else askExact = true
    }

    // Sem a permissão de notificação (Android 13+), o aviso não aparece: pede antes.
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { scheduleApp() }

    if (askExact) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.leave_exact_title)) },
            text = { Text(stringResource(R.string.leave_exact_text)) },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }
                    }
                    onDismiss()
                }) { Text(stringResource(R.string.leave_exact_allow)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.leave_exact_later)) } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.leave_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        if (reminder.departure.tomorrow) R.string.leave_text_tomorrow else R.string.leave_text,
                        reminder.vehicle, departureTime, reminder.stopName,
                    ),
                )
                if (options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.leave_too_late, untilDeparture),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(stringResource(R.string.leave_before), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { minutes ->
                            FilterChip(
                                selected = minutes == chosen,
                                onClick = { before = minutes },
                                label = { Text(stringResource(R.string.leave_minutes, minutes)) },
                            )
                        }
                    }
                    val at = formatTime(alarmMinutes!!)
                    Text(
                        text = when {
                            appOk -> stringResource(if (reminder.departure.tomorrow) R.string.leave_app_at_tomorrow else R.string.leave_app_at, at)
                            alarmOk -> stringResource(if (reminder.departure.tomorrow) R.string.leave_alarm_at_tomorrow else R.string.leave_alarm_at, at)
                            else -> stringResource(R.string.leave_alarm_wrong_day)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (appOk) {
                    Button(onClick = {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else scheduleApp()
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.leave_app_button)) }
                }
                if (alarmOk && chosen != null && alarmMinutes != null) {
                    OutlinedButton(onClick = {
                        remember(chosen)
                        openAlarm(context, reminder, alarmMinutes)
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.leave_alarm_button_clock)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        openCalendar(context, reminder)
                        onDismiss()
                    }) { Text(stringResource(R.string.leave_add)) }
                }
            }
        },
    )
}

/** Manda o alarme pronto para o relógio do celular, que o salva e o mostra. */
private fun openAlarm(context: Context, reminder: LeaveReminder, alarmMinutes: Int) {
    val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, alarmMinutes / 60)
        .putExtra(AlarmClock.EXTRA_MINUTES, alarmMinutes % 60)
        .putExtra(
            AlarmClock.EXTRA_MESSAGE,
            context.getString(R.string.leave_alarm_message, reminder.vehicle, formatTime(reminder.departure.minutes), reminder.stopName),
        )
        .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.leave_no_clock, Toast.LENGTH_LONG).show()
    }
}

/** O evento dura uns minutos a partir da partida; o alarme é o padrão da agenda (antes do início). */
private const val EVENT_MINUTES = 10

private fun openCalendar(context: Context, reminder: LeaveReminder) {
    val departureMillis = departureMillis(reminder.departure)
    val departureTime = formatTime(reminder.departure.minutes)
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, departureMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, departureMillis + EVENT_MINUTES * 60_000L)
        .putExtra(
            CalendarContract.Events.TITLE,
            context.getString(R.string.leave_event_title, reminder.vehicle, departureTime),
        )
        .putExtra(
            CalendarContract.Events.EVENT_LOCATION,
            context.getString(R.string.leave_event_location, reminder.stopName),
        )
        .putExtra(
            CalendarContract.Events.DESCRIPTION,
            context.getString(R.string.leave_event_description, reminder.routeName, reminder.headsign),
        )
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.leave_no_calendar, Toast.LENGTH_LONG).show()
    }
}

/** Instante da partida: meia-noite de hoje (ou amanhã) em Fortaleza + os minutos do GTFS. */
private fun departureMillis(departure: Departure): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (departure.tomorrow) add(Calendar.DAY_OF_MONTH, 1)
        // Minutos acima de 24h (partidas depois da meia-noite) viram o dia sozinhos.
        add(Calendar.MINUTE, departure.minutes)
    }
    return calendar.timeInMillis
}
