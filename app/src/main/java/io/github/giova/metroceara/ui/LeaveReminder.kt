package io.github.giova.metroceara.ui

import android.content.ActivityNotFoundException
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import io.github.giova.metroceara.data.LeaveStore
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
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

/** O dia da partida (`yyyy-MM-dd`, Fortaleza): hoje ou amanhã. */
private fun departureDate(departure: Departure): String =
    dateOf(Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza")).apply { if (departure.tomorrow) add(Calendar.DAY_OF_MONTH, 1) })

/** O id do aviso de uma partida: o mesmo horário da mesma estação é sempre o mesmo aviso. */
fun leaveAlertId(reminder: LeaveReminder): String =
    "p-${reminder.stopId}-${reminder.routeId}-${reminder.directionId}-${departureDate(reminder.departure)}-${reminder.departure.minutes}"

/** O aviso do app para esta partida, com [before] minutos de antecedência; `null` sem estação/linha/sentido. */
fun leaveAlertFor(reminder: LeaveReminder, before: Int): LeaveAlert? {
    val stopId = reminder.stopId ?: return null
    val routeId = reminder.routeId ?: return null
    val directionId = reminder.directionId ?: return null
    return LeaveAlert(
        id = leaveAlertId(reminder),
        date = departureDate(reminder.departure), departure = reminder.departure.minutes, before = before,
        stopId = stopId, routeId = routeId, directionId = directionId,
        stopName = reminder.stopName, routeName = reminder.routeName, headsign = reminder.headsign,
        destStopId = reminder.destStopId, destRouteId = reminder.destRouteId, destName = reminder.destName,
        arrival = reminder.arrival,
    )
}

/**
 * O toque único: depois que a pessoa marcou o primeiro aviso pela janela (e já
 * liberou a notificação), tocar num horário marca o aviso do app direto, com a
 * última antecedência. `null` quando é preciso a janela (primeira vez, sem
 * permissão, ou a antecedência de sempre já passou para esta partida).
 */
fun quickLeaveAlert(context: Context, reminder: LeaveReminder): LeaveAlert? {
    if (!LeaveStore.quickEnabled(context)) return null
    val notificationsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    if (!notificationsOk) return null
    val before = SettingsStore.get(context).leaveBeforeMinutes
    if (before >= minutesUntil(reminder.departure, nowMinutes())) return null
    return leaveAlertFor(reminder, before)
}

/**
 * Avisar a hora de sair. O padrão é o **aviso do app**: na hora, mesmo com a tela
 * bloqueada, ele confere a grade do dia e mostra a contagem até a partida
 * ([LeaveScheduler]). Discretos, no rodapé: o **alarme do relógio** (só esta
 * partida, sem repetir: não sabe de domingo nem de feriado) e a **agenda**.
 * Com [existing], é a janela do aviso já marcado ("Mais opções" ou tocar de
 * novo no horário): mudar a antecedência, trocar de jeito ou tirar o aviso.
 * A antecedência é lembrada da última vez (`AppSettings.leaveBeforeMinutes`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LeaveReminderDialog(reminder: LeaveReminder, onDismiss: () -> Unit, existing: LeaveAlert? = null) {
    val context = LocalContext.current
    val departureTime = formatTime(reminder.departure.minutes)
    var before by remember { mutableIntStateOf(existing?.before ?: SettingsStore.get(context).leaveBeforeMinutes) }
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
    val firstTime = !LeaveStore.quickEnabled(context)

    fun remember(minutes: Int) =
        SettingsStore.save(context, SettingsStore.get(context).copy(leaveBeforeMinutes = minutes))

    fun scheduleApp() {
        val minutes = chosen ?: return
        val alert = leaveAlertFor(reminder, minutes) ?: return
        remember(minutes)
        LeaveScheduler.add(context, alert)
        LeaveStore.setQuickEnabled(context, true)
        Toast.makeText(context, context.getString(R.string.leave_app_done, formatTime(alarmMinutes!!)), Toast.LENGTH_LONG).show()
        if (LeaveScheduler.canExact(context)) onDismiss() else askExact = true
    }

    // Trocar o aviso do app por alarme ou agenda: o do app sai, para não avisar duas vezes.
    fun dropExisting() = existing?.let { LeaveScheduler.cancel(context, it.id) }

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
        title = {
            Text(
                existing?.let { stringResource(R.string.leave_existing_title, formatTime(it.departure - it.before)) }
                    ?: stringResource(R.string.leave_title),
            )
        },
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
                            appOk -> stringResource(if (reminder.departure.tomorrow) R.string.leave_app_at_tomorrow else R.string.leave_app_at, at) +
                                if (firstTime) " " + stringResource(R.string.leave_quick_hint) else ""
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (appOk) {
                    Button(onClick = {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else scheduleApp()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (existing != null) R.string.leave_save else R.string.leave_app_button))
                    }
                }
                // Os outros jeitos, discretos: a pergunta e, embaixo, os dois links.
                Text(
                    stringResource(R.string.leave_other_ways),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                    if (alarmOk && chosen != null && alarmMinutes != null) {
                        LinkText(stringResource(R.string.leave_alarm_button_clock)) {
                            remember(chosen)
                            dropExisting()
                            openAlarm(context, reminder, alarmMinutes)
                            onDismiss()
                        }
                        Text("·", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
                    }
                    LinkText(stringResource(R.string.leave_calendar_link)) {
                        dropExisting()
                        openCalendar(context, reminder)
                        onDismiss()
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                    if (existing != null) {
                        TextButton(onClick = {
                            LeaveScheduler.cancel(context, existing.id)
                            onDismiss()
                        }) { Text(stringResource(R.string.leave_remove)) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                }
            }
        },
    )
}

/**
 * O toque num horário: a janela na primeira vez (ou quando precisa dela); depois,
 * o aviso do app marcado na hora, com a faixa "Desfazer · Mais opções". Tocar num
 * horário já marcado abre a janela dele. [host] mostra a faixa.
 */
class LeaveTap(private val host: SnackbarHostState) {
    var dialog by mutableStateOf<Pair<LeaveReminder, LeaveAlert?>?>(null)
    var last by mutableStateOf<Pair<LeaveReminder, LeaveAlert>?>(null)

    suspend fun tap(context: Context, reminder: LeaveReminder) {
        val existing = LeaveStore.alert(context, leaveAlertId(reminder))
        if (existing != null) {
            dialog = reminder to existing
            return
        }
        val alert = quickLeaveAlert(context, reminder)
        if (alert == null) {
            dialog = reminder to null
            return
        }
        LeaveScheduler.add(context, alert)
        last = reminder to alert
        host.currentSnackbarData?.dismiss()
        host.showSnackbar(
            context.getString(R.string.leave_quick_done, formatTime(alert.departure - alert.before), alert.before,
                reminder.vehicle, formatTime(alert.departure)),
            duration = SnackbarDuration.Long,
        )
    }

    fun undo(context: Context) {
        last?.let { LeaveScheduler.cancel(context, it.second.id) }
        last = null
        host.currentSnackbarData?.dismiss()
    }

    fun more() {
        last?.let { dialog = it.first to it.second }
        host.currentSnackbarData?.dismiss()
    }
}

/** A faixa do toque único: a mensagem, "Desfazer" e "Mais opções". */
@Composable
fun LeaveSnackbarHost(host: SnackbarHostState, tap: LeaveTap) {
    val context = LocalContext.current
    SnackbarHost(host) { data ->
        Snackbar(
            modifier = Modifier.padding(12.dp),
            action = {
                // A cor de ação própria da faixa (clara sobre o fundo escuro).
                val actionColor = SnackbarDefaults.actionColor
                Row {
                    TextButton(onClick = { tap.undo(context) }) { Text(stringResource(R.string.leave_undo), color = actionColor) }
                    TextButton(onClick = { tap.more() }) { Text(stringResource(R.string.leave_more), color = actionColor) }
                }
            },
        ) { Text(data.visuals.message) }
    }
    tap.dialog?.let { (reminder, existing) ->
        LeaveReminderDialog(reminder = reminder, existing = existing, onDismiss = { tap.dialog = null })
    }
}

/** Um link de texto discreto, com área de toque folgada. */
@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 2.dp),
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
