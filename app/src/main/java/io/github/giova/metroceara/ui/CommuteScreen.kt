package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.CommuteKind
import io.github.giova.metroceara.data.CommuteSchedule
import io.github.giova.metroceara.data.Place
import io.github.giova.metroceara.data.formatTime
import java.util.Calendar

private val COMMUTE_DAYS = listOf(
    Calendar.MONDAY to R.string.commute_day_mon,
    Calendar.TUESDAY to R.string.commute_day_tue,
    Calendar.WEDNESDAY to R.string.commute_day_wed,
    Calendar.THURSDAY to R.string.commute_day_thu,
    Calendar.FRIDAY to R.string.commute_day_fri,
    Calendar.SATURDAY to R.string.commute_day_sat,
)

/** Item do Mais: as estações, os horários e os dias da ida e volta. Grava a cada mudança. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CommuteScreen(
    schedule: CommuteSchedule,
    homeName: String?,
    workName: String?,
    onChange: (CommuteSchedule) -> Unit,
    onPickStation: (Place) -> Unit,
    onBack: () -> Unit,
) {
    // true = chegar até; false = sair de lá.
    var pickingArrive by remember { mutableStateOf<Boolean?>(null) }
    val choose = stringResource(R.string.commute_choose)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_commute)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.commute_intro), style = MaterialTheme.typography.bodyMedium)

            SectionLabel(R.string.commute_goes)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CommuteKind.entries.forEach { kind ->
                    FilterChip(
                        selected = schedule.kind == kind,
                        onClick = { onChange(schedule.copy(kind = kind)) },
                        label = { Text(placeEmoji(Place.WORK, kind) + " " + stringResource(placeLabel(Place.WORK, kind))) },
                    )
                }
            }
            CommuteRow(
                title = placeEmoji(Place.HOME) + " " + stringResource(R.string.place_home),
                value = homeName ?: choose,
                onClick = { onPickStation(Place.HOME) },
            )
            CommuteRow(
                title = placeEmoji(Place.WORK, schedule.kind) + " " + stringResource(placeLabel(Place.WORK, schedule.kind)),
                value = workName ?: choose,
                onClick = { onPickStation(Place.WORK) },
            )

            SectionLabel(R.string.commute_times)
            CommuteRow(
                title = stringResource(R.string.commute_arrive),
                value = schedule.arriveBy?.let(::formatTime) ?: choose,
                onClick = { pickingArrive = true },
            )
            CommuteRow(
                title = stringResource(if (schedule.kind == CommuteKind.SCHOOL) R.string.commute_leave_school else R.string.commute_leave_work),
                value = schedule.leaveAt?.let(::formatTime) ?: choose,
                onClick = { pickingArrive = false },
            )

            SectionLabel(R.string.commute_days)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                COMMUTE_DAYS.forEach { (day, label) ->
                    FilterChip(
                        selected = day in schedule.days,
                        onClick = {
                            onChange(schedule.copy(days = if (day in schedule.days) schedule.days - day else schedule.days + day))
                        },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(stringResource(R.string.commute_save))
            }
            Text(
                text = stringResource(R.string.commute_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    pickingArrive?.let { arrive ->
        val initial = (if (arrive) schedule.arriveBy else schedule.leaveAt) ?: if (arrive) 8 * 60 else 17 * 60
        val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingArrive = null },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = state.hour * 60 + state.minute
                    onChange(if (arrive) schedule.copy(arriveBy = minutes) else schedule.copy(leaveAt = minutes))
                    pickingArrive = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingArrive = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SectionLabel(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun CommuteRow(title: String, value: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("$value ›", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
