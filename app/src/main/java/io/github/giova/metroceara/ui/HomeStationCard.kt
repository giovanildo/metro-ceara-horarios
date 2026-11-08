package io.github.giova.metroceara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.bikeRuleFor
import io.github.giova.metroceara.data.BikeBoarding
import io.github.giova.metroceara.data.bikeBoarding
import io.github.giova.metroceara.data.nowDayOfWeek
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.data.isSundayToday
import io.github.giova.metroceara.data.minutesUntil
import io.github.giova.metroceara.ui.theme.routeColor
import kotlin.math.roundToInt

@Composable
fun HomeStationCard(
    state: HomeStation,
    departures: List<HomeDeparture>,
    now: Int,
    onUseLocation: () -> Unit,
    onOpenStation: (Station) -> Unit,
    modifier: Modifier = Modifier,
    showRefresh: Boolean = true,
    /** O que a pessoa ligou em "Ajustar para mim": bike, meia, acessibilidade. */
    extras: HomeCardExtras = HomeCardExtras(),
    /** Compacto: só nome, linha e distância; tocar abre ([onExpand]). */
    compact: Boolean = false,
    onExpand: () -> Unit = {},
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        when (state) {
            is HomeStation.Idle -> Invitation(
                message = stringResource(R.string.home_invite),
                actionLabel = stringResource(R.string.home_use_location),
                onAction = onUseLocation,
            )

            is HomeStation.Locating -> Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.home_locating),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            is HomeStation.Nearby -> StationSummary(
                station = state.station,
                subtitle = stringResource(
                    if (compact) R.string.distance_straight else R.string.home_distance,
                    formatDistance(state.distanceMeters),
                ),
                icon = Icons.Filled.MyLocation,
                departures = departures,
                now = now,
                onRefresh = onUseLocation.takeIf { showRefresh },
                onClick = { if (compact) onExpand() else onOpenStation(state.station) },
                extras = extras,
                compact = compact,
            )

            is HomeStation.Pinned -> StationSummary(
                station = state.station,
                subtitle = stringResource(R.string.home_your_station),
                icon = Icons.Filled.Star,
                departures = departures,
                now = now,
                onRefresh = null,
                onClick = { if (compact) onExpand() else onOpenStation(state.station) },
                extras = extras,
                compact = compact,
            )

            is HomeStation.Unavailable -> Invitation(
                message = when (state.reason) {
                    HomeStation.Reason.PERMISSION_DENIED -> stringResource(R.string.home_denied)
                    HomeStation.Reason.NO_FIX -> stringResource(R.string.home_no_fix)
                },
                actionLabel = stringResource(R.string.home_try_again),
                onAction = onUseLocation,
            )
        }
    }
}

@Composable
private fun Invitation(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = actionLabel, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun StationSummary(
    station: Station,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    departures: List<HomeDeparture>,
    now: Int,
    onRefresh: (() -> Unit)?,
    onClick: () -> Unit,
    extras: HomeCardExtras,
    compact: Boolean = false,
) {
    if (compact) {
        CompactSummary(station, subtitle, icon, onRefresh, onClick)
        return
    }
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.stopName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(routeColor(station.routeColor)),
                    )
                    Text(text = station.routeName, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onRefresh != null) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.home_try_again),
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            val sunday = remember(now) { isSundayToday() }
            val day = remember(now) { ScheduleStore.today() }
            // Domingo com operação anunciada pelo Metrofor: a notícia dela, no topo, já explica.
            // Sem ela, nenhum horário: só o aviso e a hora em que o metrô volta na segunda.
            val sundayOff = sunday && Regras.current.operations[day] == null
            if (sundayOff) {
                val mondayFirst = departures.mapNotNull { it.departure?.takeIf { d -> d.tomorrow }?.minutes }.minOrNull()
                SundayNotice(modifier = Modifier.padding(bottom = 6.dp), text = sundayOffText(mondayFirst))
            }
            val stopped = Regras.current.stopped(day, station.routeName)
            if (stopped) {
                SundayNotice(modifier = Modifier.padding(bottom = 6.dp), text = stringResource(R.string.line_stopped_today))
            }
            if (!sundayOff) departures.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        DirectionTurn(
                            toLeft = item.directionId == 0,
                            color = routeColor(station.routeColor),
                            modifier = Modifier.padding(end = 10.dp, bottom = 2.dp),
                            width = 40.dp,
                        )
                        DirectionLabel(headsign = item.headsign)
                    }
                    // O tempo que falta é o que a pessoa procura: ele vem grande, a hora embaixo.
                    val departure = item.departure
                    if (departure != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = relativeLabel(minutesUntil(departure, now)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = formatTime(departure.minutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(
                                if (item.published) R.string.home_no_departures else R.string.home_unpublished,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                item.dayEnd?.let { dayEndText(it) }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // No domingo há só operação especial: com ela anunciada mas sem regra de bike,
            // o aviso diz que não se sabe; sem operação anunciada, já há o aviso de domingo
            // (a grade de domingo comum vem igual à de dia útil). Sem mais partidas hoje,
            // "pode embarcar" não faria sentido.
            val runningToday = departures.any { it.departure?.tomorrow == false }
            val bikeRule = bikeRuleFor(station.stopName, station.routeName)
            val known = !sunday || bikeRule?.special == true
            if (extras.bike && runningToday && !stopped && bikeRule != null && (known || Regras.current.operations[day] != null)) {
                BikeBoardingNotice(
                    rule = remember(now, bikeRule, known) {
                        if (known) bikeBoarding(bikeRule, nowDayOfWeek(), now) else BikeBoarding.UNKNOWN
                    },
                    vehicle = vehicleName(station.routeName),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (extras.halfFare) {
                Regras.current.fare(station.routeName)?.let { fare ->
                    Text(
                        text = "🎫 " + stringResource(R.string.station_fare, fare),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (Regras.current.fareGuide != null) {
                        Text(
                            text = stringResource(R.string.station_fare_guide),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = extras.onOpenFareGuide).padding(vertical = 4.dp),
                        )
                    }
                }
            }
            if (extras.access) {
                // Tocar abre a estação, onde estão o elevador, a rampa e o "Conte pra gente".
                StationAccessLine(
                    access = Regras.current.station(station.stopId, station.stopName)?.access,
                    wheelchair = true,
                    onOpen = onClick,
                )
            }
        }
    }
}

/**
 * A estação compacta (2ª e 3ª da tela inicial): nome, linha e distância numa linha,
 * e cada sentido numa linha curta com o tempo que falta. Domingo sem operação,
 * nenhum horário: só "Voltam segunda às HH:MM".
 */
@Composable
private fun CompactSummary(
    station: Station,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRefresh: (() -> Unit)?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = station.stopName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(routeColor(station.routeColor)),
            )
            Text(
                text = "${station.routeName} · $subtitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_try_again), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** O que entra no cartão da estação conforme "Ajustar para mim". */
data class HomeCardExtras(
    val bike: Boolean = false,
    val halfFare: Boolean = false,
    val access: Boolean = false,
    val onOpenFareGuide: () -> Unit = {},
)

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.roundToInt()} m" else "%.1f km".format(meters / 1000)
