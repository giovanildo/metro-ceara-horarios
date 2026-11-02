package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.LeaveAlert
import io.github.giova.metroceara.data.Routine
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import java.util.Calendar

/** "seg a sex", "todos os dias" ou "seg, qua, sex". */
@Composable
fun routineDays(days: Set<Int>): String = when (days) {
    Routine.WEEKDAYS -> stringResource(R.string.routine_days_weekdays)
    (1..7).toSet() -> stringResource(R.string.routine_days_every)
    else -> DAY_ORDER.filter { it in days }.joinToString(", ") { DAY_SHORT.getValue(it) }
}

@Composable
private fun routineSummary(r: Routine): String = stringResource(
    if (r.arriveBy) R.string.routine_summary_arrive else R.string.routine_summary_leave,
    r.originName, r.destName, routineDays(r.days), formatTime(r.minutes), r.before,
)

/** Domingo por último, como no calendário de quem trabalha de segunda a sexta. */
private val DAY_ORDER = listOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
    Calendar.SATURDAY, Calendar.SUNDAY,
)
private val DAY_SHORT = mapOf(
    Calendar.MONDAY to "seg", Calendar.TUESDAY to "ter", Calendar.WEDNESDAY to "qua", Calendar.THURSDAY to "qui",
    Calendar.FRIDAY to "sex", Calendar.SATURDAY to "sáb", Calendar.SUNDAY to "dom",
)
private val DAY_LETTER = mapOf(
    Calendar.MONDAY to "S", Calendar.TUESDAY to "T", Calendar.WEDNESDAY to "Q", Calendar.THURSDAY to "Q",
    Calendar.FRIDAY to "S", Calendar.SATURDAY to "S", Calendar.SUNDAY to "D",
)

/** As rotinas: cada uma com a chave de ligar e, se já calculado, o aviso marcado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    routines: List<Routine>,
    alerts: List<LeaveAlert>,
    onToggle: (Routine, Boolean) -> Unit,
    onEdit: (Routine) -> Unit,
    onNew: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routines_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            if (routines.isEmpty()) {
                Text(stringResource(R.string.routines_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            routines.forEach { r ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(r) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(routineSummary(r), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            alerts.firstOrNull { it.routineId == r.id }?.let { a ->
                                Text(
                                    stringResource(R.string.leave_next_alert, formatTime(a.departure - a.before),
                                        vehicleName(a.routeName), formatTime(a.departure)),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Switch(checked = r.enabled, onCheckedChange = { onToggle(r, it) })
                    }
                }
            }
            OutlinedButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.routines_new), modifier = Modifier.padding(start = 8.dp))
            }
            Text(stringResource(R.string.routines_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Criar ou mudar uma rotina: nome, de, para, dias, "chegar até"/"sair a partir de" e a antecedência. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineEditScreen(
    initial: Routine,
    isNew: Boolean,
    stations: List<Station>,
    onSave: (Routine) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var r by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = origem, false = destino
    var pickingTime by remember { mutableStateOf(false) }
    val pickTitle = stringResource(R.string.routine_pick_station)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) stringResource(R.string.routines_new) else stringResource(R.string.routine_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = r.name,
                onValueChange = { r = r.copy(name = it) },
                label = { Text(stringResource(R.string.routine_name)) },
                placeholder = { Text(stringResource(R.string.routine_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            StationField(stringResource(R.string.journey_from), r.originName.ifEmpty { pickTitle }) { picking = true }
            StationField(stringResource(R.string.journey_to), r.destName.ifEmpty { pickTitle }) { picking = false }
            Text(stringResource(R.string.routine_days), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DAY_ORDER.forEach { day ->
                    FilterChip(
                        selected = day in r.days,
                        onClick = { r = r.copy(days = if (day in r.days) r.days - day else r.days + day) },
                        label = { Text(DAY_LETTER.getValue(day)) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = r.arriveBy, onClick = { r = r.copy(arriveBy = true) },
                    label = { Text(stringResource(R.string.routine_arrive_by)) })
                FilterChip(selected = !r.arriveBy, onClick = { r = r.copy(arriveBy = false) },
                    label = { Text(stringResource(R.string.routine_leave_at)) })
                TextButton(onClick = { pickingTime = true }) {
                    Text(formatTime(r.minutes), style = MaterialTheme.typography.titleLarge)
                }
            }
            Text(stringResource(R.string.routine_before), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20, 30, 45).forEach { m ->
                    FilterChip(selected = r.before == m, onClick = { r = r.copy(before = m) },
                        label = { Text(stringResource(R.string.leave_minutes, m)) })
                }
            }
            val ready = r.originStopId.isNotEmpty() && r.destStopId.isNotEmpty() && r.days.isNotEmpty()
            Button(
                onClick = { onSave(r.copy(name = r.name.trim().ifEmpty { r.destName })) },
                enabled = ready,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.routine_save)) }
            if (!isNew) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error)
                }
            }
            Text(stringResource(R.string.routines_note), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    picking?.let { origin ->
        StationPickerDialog(
            stations = stations,
            title = pickTitle,
            onPick = { st ->
                val name = io.github.giova.metroceara.data.JourneyPlanner.plainName(st.stopName)
                r = if (origin) r.copy(originStopId = st.stopId, originRouteId = st.routeId, originName = name)
                else r.copy(destStopId = st.stopId, destRouteId = st.routeId, destName = name)
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = r.minutes / 60, initialMinute = r.minutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    r = r.copy(minutes = state.hour * 60 + state.minute)
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun StationField(label: String, value: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
