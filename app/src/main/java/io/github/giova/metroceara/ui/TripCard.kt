package io.github.giova.metroceara.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.giova.metroceara.data.TripJourney
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.shape.CircleShape
import io.github.giova.metroceara.ui.theme.routeColor
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.trip.ActiveTrip
import kotlin.math.roundToInt

/** A viagem em andamento, no topo da tela inicial. */
@Composable
fun TripCard(
    trip: ActiveTrip,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    stationAlerts: Boolean,
    onToggleStationAlerts: (Boolean) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = trip.plan
    val progress = trip.progress
    val left = progress.stationsLeft(plan)
    val journey = plan.journey
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (journey != null) {
                            stringResource(
                                if (plan.simulated) R.string.trip_card_title_journey_sim else R.string.trip_card_title_journey,
                                journey.originName, journey.finalName,
                            )
                        } else {
                            stringResource(
                                if (plan.simulated) R.string.trip_card_title_sim else R.string.trip_card_title,
                                plan.routeName,
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = if (journey != null) {
                            stringResource(
                                R.string.trip_journey_arrival,
                                formatTime(if (plan.transfer == null) progress.arrivalMinutes.roundToInt() else journey.arrival),
                            )
                        } else {
                            "${plan.origin.name} → ${plan.destination.name}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    plan.terminal?.takeIf { plan.viaTerminal }?.let { terminal ->
                        Text(
                            text = stringResource(R.string.trip_via_terminal, terminal.name),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                FilledTonalIconToggleButton(checked = soundEnabled, onCheckedChange = onToggleSound) {
                    Icon(
                        imageVector = if (soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = stringResource(
                            if (soundEnabled) R.string.trip_sound_on else R.string.trip_sound_off,
                        ),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { ((progress.index.coerceAtLeast(0)).toFloat() / plan.stops.lastIndex.coerceAtLeast(1)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )

            Text(
                text = when {
                    trip.arrived -> stringResource(R.string.trip_arrived, plan.destination.name)
                    progress.index < 0 && (journey?.current ?: 0) > 0 -> stringResource(
                        R.string.trip_journey_walking, io.github.giova.metroceara.data.lineWithArticle(plan.routeName),
                        vehicleName(plan.routeName).replaceFirstChar { it.uppercase() },
                        formatTime(plan.origin.scheduled), plan.headsign,
                        (plan.origin.scheduled + progress.delayMinutes - io.github.giova.metroceara.data.nowMinutes())
                            .roundToInt().coerceAtLeast(0),
                    )
                    progress.index < 0 -> stringResource(
                        R.string.trip_waiting,
                        plan.origin.name,
                        formatTime((plan.origin.scheduled + progress.delayMinutes).roundToInt()),
                        vehicleName(plan.routeName),
                    )
                    else -> stringResource(
                        R.string.trip_progress_line,
                        progress.nextStop(plan)?.let { next ->
                            stringResource(R.string.trip_next_inline, plan.stops[progress.index].name, next.name)
                        } ?: plan.stops[progress.index].name,
                        pluralStringResource(R.plurals.trip_stations_left, left, left),
                        formatTime(progress.arrivalMinutes.roundToInt()),
                        stringResource(fixLabel(progress.fix)),
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            progress.gpsSilentMinutes?.let { silent ->
                if (!trip.arrived) {
                    // Quadro próprio, de fundo neutro: texto vermelho pequeno sobre o
                    // fundo do cartão ficava difícil de ler, justo quando importa.
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Icon(
                                Icons.Filled.GpsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.trip_gps_lost_title, silent.toInt().coerceAtLeast(1)),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = stringResource(R.string.trip_gps_lost_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            journey?.let { JourneyTimeline(it) }
            Text(
                text = if (trip.alerted) {
                    stringResource(R.string.trip_alert_sent)
                } else {
                    stringResource(
                        R.string.trip_alert_at,
                        plan.stops[plan.alertIndex].name,
                        pluralStringResource(R.plurals.trip_stations_left, plan.alertStationsBefore, plan.alertStationsBefore),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                // A cor do cartão, um pouco mais clara: o cinza do tema sumia no fundo lilás.
                color = LocalContentColor.current.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            // Com letra grande, "Encerrar viagem" desce para a linha de baixo em vez
            // de ser espremido (o texto quebrava letra por letra).
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f, fill = false),
                    selected = stationAlerts,
                    onClick = { onToggleStationAlerts(!stationAlerts) },
                    label = { Text(stringResource(R.string.trip_station_alerts)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (stationAlerts) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                        )
                    },
                )
                OutlinedButton(onClick = onStop) { Text(stringResource(R.string.trip_stop)) }
            }
        }
    }
}

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

/**
 * A linha do tempo de um roteiro com integração: cada trecho na cor da linha,
 * com as integrações entre eles; o que já passou fica apagado.
 */
@Composable
private fun JourneyTimeline(journey: TripJourney) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        journey.legs.forEachIndexed { i, leg ->
            val state = when {
                i < journey.current -> stringResource(R.string.trip_journey_done)
                i == journey.current -> stringResource(R.string.trip_journey_current)
                else -> null
            }
            TimelineRow(
                color = routeColor(leg.colorHex),
                title = "${formatTime(leg.departure)} ${leg.fromName}",
                detail = listOfNotNull(stringResource(R.string.trip_journey_leg, lineLabel(leg.routeName), leg.toName), state)
                    .joinToString(" · "),
                past = i < journey.current,
                current = i == journey.current,
            )
            if (i < journey.legs.lastIndex) {
                TimelineRow(
                    color = MaterialTheme.colorScheme.outline,
                    title = stringResource(R.string.trip_journey_transfer, leg.toName),
                    detail = null,
                    past = i < journey.current,
                    current = false,
                )
            }
        }
        val last = journey.legs.last()
        TimelineRow(
            color = routeColor(last.colorHex),
            title = "${formatTime(last.arrival)} ${last.toName}",
            detail = null,
            past = false,
            current = false,
        )
    }
}

@Composable
private fun TimelineRow(color: Color, title: String, detail: String?, past: Boolean, current: Boolean) {
    val alpha = if (past) 0.55f else 1f
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 3.dp)) {
        Spacer(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(if (current) 12.dp else 10.dp)
                .background(color.copy(alpha = alpha), CircleShape),
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                color = LocalContentColor.current.copy(alpha = alpha),
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = alpha * 0.8f),
                )
            }
        }
    }
}
