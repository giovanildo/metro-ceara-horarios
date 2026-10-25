package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.bikeRuleFor
import io.github.giova.metroceara.data.BikeBoarding
import io.github.giova.metroceara.data.bikeBoarding
import io.github.giova.metroceara.data.nowDayOfWeek
import io.github.giova.metroceara.data.DayEnd
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.Direction
import io.github.giova.metroceara.data.Route
import io.github.giova.metroceara.data.TransferTarget
import io.github.giova.metroceara.data.ScheduleSource
import io.github.giova.metroceara.data.Stop
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.data.isSundayToday
import io.github.giova.metroceara.data.minutesUntil
import io.github.giova.metroceara.data.nextDepartures
import io.github.giova.metroceara.data.nowMinutes
import kotlinx.coroutines.delay

private const val NEXT_COUNT = 4
private const val TICK_MILLIS = 15_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    route: Route,
    stop: Stop,
    directions: List<Direction>,
    departuresFor: (Direction) -> List<Int>,
    tomorrowFor: (Direction) -> List<Int>?,
    dayEndFor: (Direction) -> DayEnd,
    destinationsFor: (Direction) -> List<Stop>,
    onStartTrip: (Direction, Stop) -> Unit,
    tripAllowed: Boolean,
    transfers: List<TransferTarget>,
    transferDepartures: (TransferTarget, Int) -> Departure?,
    onOpenTransfer: (TransferTarget) -> Unit,
    initialDirectionId: Int? = null,
    schedule: ScheduleSource,
    bike: NearbyBike?,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onBack: () -> Unit,
) {
    // A aba é da estação: ao abrir outra, volta para o sentido pedido (ou o primeiro).
    var selectedTab by rememberSaveable(stop.id, route.id) {
        mutableIntStateOf(directions.indexOfFirst { it.id == initialDirectionId }.coerceAtLeast(0))
    }
    var now by remember { mutableIntStateOf(nowMinutes()) }

    // O relógio do app precisa andar sozinho para a contagem regressiva não travar.
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            now = nowMinutes()
        }
    }

    val direction = directions.getOrNull(selectedTab) ?: directions.firstOrNull()
    val all = remember(direction) { direction?.let(departuresFor).orEmpty() }
    val tomorrow = remember(direction) { direction?.let(tomorrowFor) }
    val next = remember(all, tomorrow, now) { nextDepartures(all, now, NEXT_COUNT, tomorrow) }
    val sunday = remember(now) { isSundayToday() }
    // Destinos nos dois sentidos — o da aba aberta primeiro. Só o sentido da
    // aba escondia, por exemplo, Carlito Benevides de quem estava na aba
    // "→ Chico da Silva".
    val destinationGroups = remember(direction, directions) {
        directions.sortedByDescending { it == direction }
            .map { DestinationGroup("→ ${it.headsign}", route.id, stop, it, destinationsFor(it)) }
    }
    val destinations = destinationGroups.flatMap { it.stops }
    var pickingDestination by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf<Departure?>(null) }
    reminder?.let { departure ->
        LeaveReminderDialog(
            reminder = LeaveReminder(departure, stop.name, route.name, direction?.headsign.orEmpty(), vehicleName(route.name)),
            onDismiss = { reminder = null },
        )
    }
    if (pickingDestination && direction != null) {
        DestinationDialog(
            groups = destinationGroups,
            onPick = { group, destination ->
                pickingDestination = false
                onStartTrip(group.direction, destination)
            },
            onDismiss = { pickingDestination = false },
        )
    }
    // No domingo há só operação especial: sem regra de bike anunciada para o dia,
    // o aviso diz que não se sabe (com a grade do dia; sem ela, já há o aviso de
    // domingo). Sem mais partidas hoje nesta estação, não há o que embarcar.
    val runningToday = remember(directions, now) {
        directions.any { d -> departuresFor(d).any { it >= now } }
    }
    val bikeRule = remember(route.id, now, runningToday) {
        bikeRuleFor(stop.name, route.name)?.takeIf { runningToday }?.let { rule ->
            when {
                !sunday || rule.special -> bikeBoarding(rule, nowDayOfWeek(), now)
                schedule.kind == ScheduleSource.Kind.TODAY -> BikeBoarding.UNKNOWN
                else -> null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stop.name)
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(
                                if (isPinned) R.string.unpin_station else R.string.pin_station,
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                directions.forEachIndexed { index, item ->
                    Tab(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        text = { Text("→ ${item.headsign}") },
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Com a grade de hoje baixada, ela já diz se há operação especial.
                if (sunday && schedule.kind != ScheduleSource.Kind.TODAY) {
                    item { SundayNotice() }
                }

                item {
                    Text(
                        text = stringResource(R.string.next_departures),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                if (next.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(
                                    if (direction?.published == false) R.string.direction_unpublished
                                    else R.string.no_more_today,
                                ),
                            )
                            direction?.let { d ->
                                dayEndText(remember(d) { dayEndFor(d) })?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                next.forEachIndexed { index, departure ->
                                    if (index > 0) HorizontalDivider()
                                    DepartureRow(
                                        departure, now, highlighted = index == 0,
                                        onClick = { reminder = departure },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.leave_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (tripAllowed && destinations.isNotEmpty()) {
                    item {
                        Button(
                            onClick = { pickingDestination = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Navigation, contentDescription = null)
                            Text(stringResource(R.string.trip_start), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                if (bikeRule != null) {
                    item { BikeBoardingNotice(bikeRule, vehicle = vehicleName(route.name)) }
                }

                // Baldeação: o primeiro trem da outra linha que ainda dá para pegar a pé.
                if (transfers.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TransferBlock(
                                targets = transfers,
                                nextCatchable = { transferDepartures(it, now + it.walkMinutes) },
                                now = now,
                                onOpen = onOpenTransfer,
                            )
                        }
                    }
                }

                if (bike != null) {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = stringResource(R.string.bike_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            BikeStationRow(
                                bike = bike,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.all_departures),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item {
                    AllDepartures(
                        all, now,
                        onPick = { m: Int -> reminder = Departure(m, tomorrow = false) },
                    )
                }

                item {
                    Text(
                        text = scheduleStatusText(schedule, syncing = false, failed = false),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DepartureRow(departure: Departure, now: Int, highlighted: Boolean, onClick: (() -> Unit)?) {
    val remaining = minutesUntil(departure, now)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = formatTime(departure.minutes),
                style = if (highlighted) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            )
            if (departure.tomorrow) {
                Text(
                    text = stringResource(R.string.tomorrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = relativeLabel(remaining),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun relativeLabel(remaining: Int): String = when {
    remaining <= 0 -> stringResource(R.string.now)
    remaining < 60 -> stringResource(R.string.in_minutes, remaining)
    else -> stringResource(R.string.in_hours, remaining / 60, remaining % 60)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllDepartures(all: List<Int>, now: Int, onPick: ((Int) -> Unit)?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        all.forEach { minutes ->
            // Os que ainda não passaram ficam em destaque (e abrem o lembrete
            // na agenda); os que passaram ficam apagados.
            val upcoming = minutes >= now
            Surface(
                color = if (upcoming) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = if (upcoming && onPick != null) Modifier.clickable { onPick(minutes) } else Modifier,
            ) {
                Text(
                    text = formatTime(minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (upcoming) FontWeight.Medium else FontWeight.Normal,
                    color = if (upcoming) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
