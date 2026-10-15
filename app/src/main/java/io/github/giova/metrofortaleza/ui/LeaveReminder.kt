package io.github.giova.metrofortaleza.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metrofortaleza.R
import io.github.giova.metrofortaleza.data.Departure
import io.github.giova.metrofortaleza.data.formatTime
import java.util.Calendar
import java.util.TimeZone

/** Uma partida escolhida para lembrar a hora de sair de casa. */
data class LeaveReminder(
    val departure: Departure,
    val stopName: String,
    val routeName: String,
    val headsign: String,
    val vehicle: String,
)

/**
 * Confirma o lembrete e abre a agenda do celular (Google Agenda ou outra) com o
 * evento preenchido: começa na hora de sair (de casa ou do trabalho) e termina
 * na partida. Não usa conta nem internet; a pessoa só confirma na agenda.
 */
@Composable
fun LeaveReminderDialog(reminder: LeaveReminder, homeMinutes: Int, workMinutes: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var fromWork by rememberSaveable { mutableStateOf(false) }
    val minutes = if (fromWork) workMinutes else homeMinutes
    val place = stringResource(if (fromWork) R.string.leave_place_work else R.string.leave_place_home)
    val departureTime = formatTime(reminder.departure.minutes)
    val leaveTime = formatTime(reminder.departure.minutes - minutes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.leave_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !fromWork,
                        onClick = { fromWork = false },
                        label = { Text(stringResource(R.string.leave_from_home)) },
                    )
                    FilterChip(
                        selected = fromWork,
                        onClick = { fromWork = true },
                        label = { Text(stringResource(R.string.leave_from_work)) },
                    )
                }
                Text(
                    stringResource(
                        if (reminder.departure.tomorrow) R.string.leave_text_tomorrow else R.string.leave_text,
                        reminder.vehicle, departureTime, reminder.stopName, minutes, place, leaveTime,
                    ),
                )
                Text(
                    text = stringResource(R.string.leave_calendar_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openCalendar(context, reminder, minutes, place)
                onDismiss()
            }) { Text(stringResource(R.string.leave_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private fun openCalendar(context: Context, reminder: LeaveReminder, minutes: Int, place: String) {
    val departureMillis = departureMillis(reminder.departure)
    val leaveMillis = departureMillis - minutes * 60_000L
    val departureTime = formatTime(reminder.departure.minutes)
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, leaveMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, departureMillis)
        .putExtra(
            CalendarContract.Events.TITLE,
            context.getString(R.string.leave_event_title, place, reminder.vehicle, departureTime),
        )
        .putExtra(
            CalendarContract.Events.EVENT_LOCATION,
            context.getString(R.string.leave_event_location, reminder.stopName),
        )
        .putExtra(
            CalendarContract.Events.DESCRIPTION,
            context.getString(
                R.string.leave_event_description,
                reminder.routeName, reminder.headsign, departureTime, minutes, place,
            ),
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
