package io.github.giova.metroceara.ui

import io.github.giova.metroceara.data.LeaveStore
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.data.LeaveAlert
import io.github.giova.metroceara.data.LeaveScheduler
import io.github.giova.metroceara.data.dateOf
import android.content.Context
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

/** O dia da partida (`yyyy-MM-dd`, Fortaleza): hoje ou amanhã. */
private fun departureDate(departure: Departure): String =
    dateOf(Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza")).apply { if (departure.tomorrow) add(Calendar.DAY_OF_MONTH, 1) })

/** O id do aviso de uma partida: o mesmo horário da mesma estação é sempre o mesmo aviso. */
fun leaveAlertId(reminder: LeaveReminder): String =
    "p-${reminder.stopId}-${reminder.routeId}-${reminder.directionId}-${departureDate(reminder.departure)}-${reminder.departure.minutes}"

/** Os avisos marcados para esta partida (mesma estação, linha, sentido, dia e horário). */
fun leaveAlertsAt(context: Context, reminder: LeaveReminder): List<LeaveAlert> {
    val date = departureDate(reminder.departure)
    return LeaveStore.alerts(context).value.orEmpty().filter {
        it.stopId == reminder.stopId && it.routeId == reminder.routeId && it.directionId == reminder.directionId &&
            it.date == date && it.departure == reminder.departure.minutes
    }
}

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
 * Avisar a hora de sair num toque: tocar num horário marca o aviso do app (com a
 * antecedência das Configurações) e tocar de novo tira. Na hora, mesmo com a tela
 * bloqueada, o aviso confere a grade do dia e mostra a contagem até a partida
 * ([LeaveScheduler]). No primeiro toque o Android pede a permissão de
 * notificação e, uma vez só, o app explica "Alarmes e lembretes".
 * [show] mostra a confirmação curta ("Aviso às 13:59…", "Aviso tirado.").
 */
class LeaveToggle(private val show: suspend (String) -> Unit) {
    var askExact by mutableStateOf(false)
    /** O pedido de permissão de notificação, ligado pelo [LeaveToggleHost]. */
    internal var askNotifications: ((LeaveReminder) -> Unit)? = null

    suspend fun toggle(context: Context, reminder: LeaveReminder) {
        // Todos os avisos deste horário, pelo que eles são (e não pelo id): os da 1.61/1.62
        // tinham outro id, e o de uma rotina também tem o seu.
        val existing = leaveAlertsAt(context, reminder)
        if (existing.isNotEmpty()) {
            existing.forEach { LeaveScheduler.cancel(context, it.id) }
            val first = existing.first()
            show(context.getString(R.string.leave_removed, formatTime(first.departure - first.before)))
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission && askNotifications != null) {
            askNotifications?.invoke(reminder)
            return
        }
        schedule(context, reminder)
    }

    /** Marca o aviso (depois da permissão de notificação, se foi pedida). */
    suspend fun schedule(context: Context, reminder: LeaveReminder) {
        val before = SettingsStore.get(context).leaveBeforeMinutes
        val until = minutesUntil(reminder.departure, nowMinutes())
        if (before >= until) {
            show(context.getString(R.string.leave_too_late, until))
            return
        }
        val alert = leaveAlertFor(reminder, before) ?: return
        LeaveScheduler.add(context, alert)
        show(context.getString(if (reminder.departure.tomorrow) R.string.leave_quick_done_tomorrow else R.string.leave_quick_done,
            formatTime(alert.departure - before), before, reminder.vehicle, formatTime(alert.departure)))
        if (!LeaveScheduler.canExact(context) && !LeaveStore.exactExplained(context)) {
            LeaveStore.setExactExplained(context)
            askExact = true
        }
    }
}

/** A peça de tela do [LeaveToggle]: o pedido de notificação e a explicação de "Alarmes e lembretes". */
@Composable
fun LeaveToggleHost(toggle: LeaveToggle) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<LeaveReminder?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.let { r -> scope.launch { toggle.schedule(context, r) } }
        pending = null
    }
    toggle.askNotifications = { r ->
        pending = r
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (toggle.askExact) {
        AlertDialog(
            onDismissRequest = { toggle.askExact = false },
            title = { Text(stringResource(R.string.leave_exact_title)) },
            text = { Text(stringResource(R.string.leave_exact_text)) },
            confirmButton = {
                TextButton(onClick = {
                    toggle.askExact = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }
                    }
                }) { Text(stringResource(R.string.leave_exact_allow)) }
            },
            dismissButton = { TextButton(onClick = { toggle.askExact = false }) { Text(stringResource(R.string.leave_exact_later)) } },
        )
    }
}
