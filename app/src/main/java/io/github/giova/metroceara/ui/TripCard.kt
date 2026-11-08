package io.github.giova.metroceara.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName

/** Direto ou sentado pelo terminal: horários dos dois para a pessoa escolher. */
@Composable
fun TripChoiceDialog(
    direct: TripPlan,
    seated: TripPlan,
    onPick: (TripPlan) -> Unit,
    onDismiss: () -> Unit,
) {
    val terminal = seated.terminal ?: return
    val backAt = seated.stops[seated.turnaroundIndex + 1].scheduled
    val extra = seated.destination.scheduled - direct.destination.scheduled
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.trip_choice_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceOption(
                    title = stringResource(R.string.trip_choice_direct),
                    description = stringResource(
                        R.string.trip_choice_direct_desc,
                        formatTime(direct.origin.scheduled),
                        formatTime(direct.destination.scheduled),
                    ),
                    onClick = { onPick(direct) },
                )
                ChoiceOption(
                    title = stringResource(R.string.trip_choice_seated, terminal.name),
                    description = stringResource(
                        R.string.trip_choice_seated_desc,
                        formatTime(seated.origin.scheduled),
                        terminal.name,
                        formatTime(backAt),
                        formatTime(seated.destination.scheduled),
                        extra.coerceAtLeast(0),
                        vehicleName(seated.routeName),
                    ),
                    onClick = { onPick(seated) },
                )
                Text(
                    text = stringResource(R.string.trip_choice_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourceLine("pico")
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ChoiceOption(title: String, description: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** "pelo GPS", "presumida pela tabela de horários" ou, na simulação, "simulação, pela tabela de horários". */
fun fixLabel(fix: TripFix): Int = when (fix) {
    TripFix.GPS -> R.string.trip_fix_gps
    TripFix.SCHEDULE -> R.string.trip_fix_schedule
    TripFix.SIMULATED -> R.string.trip_fix_simulated
}
