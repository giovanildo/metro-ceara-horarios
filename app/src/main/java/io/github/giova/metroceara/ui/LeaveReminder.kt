package io.github.giova.metroceara.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.formatTime
import java.util.Calendar
import java.util.TimeZone

/** Uma partida escolhida para lembrar na agenda. */
data class LeaveReminder(
    val departure: Departure,
    val stopName: String,
    val routeName: String,
    val headsign: String,
    val vehicle: String,
)

/**
 * Confirma o lembrete e abre a agenda do celular (Google Agenda ou outra) com o
 * evento preenchido na hora da partida. Quanto tempo antes avisar fica com a
 * agenda: a pessoa escolhe lá, ao salvar. Não usa conta, internet nem configuração.
 */
@Composable
fun LeaveReminderDialog(reminder: LeaveReminder, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val departureTime = formatTime(reminder.departure.minutes)
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
                Text(
                    text = stringResource(R.string.leave_calendar_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openCalendar(context, reminder)
                onDismiss()
            }) { Text(stringResource(R.string.leave_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
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
