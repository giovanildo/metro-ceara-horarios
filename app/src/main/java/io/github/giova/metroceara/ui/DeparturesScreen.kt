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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedButton
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
import io.github.giova.metroceara.ui.theme.routeColor
import io.github.giova.metroceara.data.bikeRuleFor
import io.github.giova.metroceara.data.BikeBoarding
import io.github.giova.metroceara.data.bikeBoarding
import io.github.giova.metroceara.data.nowDayOfWeek
import io.github.giova.metroceara.data.DayEnd
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.Landmark
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.Direction
import io.github.giova.metroceara.data.DirectionVia
import io.github.giova.metroceara.data.Route
import io.github.giova.metroceara.data.TransferTarget
import io.github.giova.metroceara.data.ScheduleSource
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.Stop
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.data.isSundayToday
import io.github.giova.metroceara.data.isSundayWithoutService
import io.github.giova.metroceara.data.minutesUntil
import io.github.giova.metroceara.data.nextDepartures
import io.github.giova.metroceara.data.nowMinutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.Mobility
import io.github.giova.metroceara.data.LeaveStore
import io.github.giova.metroceara.data.LeaveAlert
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

private const val NEXT_COUNT = 4
private const val NEARBY_COUNT = 8
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
    /** Por onde o sentido passa a partir desta estação. */
    viaFor: (Direction) -> DirectionVia?,
    onPlanJourney: () -> Unit,
    transfers: List<TransferTarget>,
    transferDepartures: (TransferTarget, Int) -> Departure?,
    onOpenTransfer: (TransferTarget) -> Unit,
    initialDirectionId: Int? = null,
    schedule: ScheduleSource,
    bike: NearbyBike?,
    /** Lugares a uma caminhada curta (regras.json), do mais perto ao mais longe. */
    nearby: List<Landmark>,
    walkMinutes: (Landmark) -> Int,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onOpenFareGuide: () -> Unit,
    onOpenAccess: () -> Unit,
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
    // Domingo comum: a lista é a de segunda ("Horários de segunda", lembrete para amanhã).
    val sundayOff = remember(now) { isSundayWithoutService() }
    // Tocar num horário: o aviso do app, num toque só depois da primeira vez.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val leaveToggle = remember {
        LeaveToggle { message ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }
    LeaveToggleHost(leaveToggle)
    fun reminderFor(departure: Departure) = LeaveReminder(
        departure, stop.name, route.name, direction?.headsign.orEmpty(), vehicleName(route.name),
        stopId = stop.id, routeId = route.id, directionId = direction?.id,
    )
    // Tocar marca o aviso; tocar de novo num horário marcado tira.
    fun onPick(departure: Departure) { scope.launch { leaveToggle.toggle(context, reminderFor(departure)) } }
    // Os horários com aviso marcado ganham o sininho (com a hora do aviso).
    val leaveAlerts by LeaveStore.alerts(context).collectAsState()
    val today = ScheduleStore.today()
    fun alertAt(departure: Departure): LeaveAlert? = leaveAlerts.orEmpty().firstOrNull {
        it.stopId == stop.id && it.routeId == route.id && it.directionId == direction?.id &&
            it.departure == departure.minutes && (it.date == today) != departure.tomorrow
    }
    // No domingo há só operação especial: com ela anunciada mas sem regra de bike,
    // o aviso diz que não se sabe; sem operação anunciada, já há o aviso de domingo
    // (a grade de domingo comum vem igual à de dia útil, então ter a grade do dia não
    // indica operação). Sem mais partidas hoje nesta estação, não há o que embarcar.
    val runningToday = remember(directions, now) {
        directions.any { d -> departuresFor(d).any { it >= now } }
    }
    val bikeRule = remember(route.id, now, runningToday) {
        bikeRuleFor(stop.name, route.name)?.takeIf { runningToday }?.let { rule ->
            when {
                !sunday || rule.special -> bikeBoarding(rule, nowDayOfWeek(), now)
                Regras.current.operations[ScheduleStore.today()] != null -> BikeBoarding.UNKNOWN
                else -> null
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
            DirectionTrack(
                directions = directions,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                lineColor = routeColor(route.colorHex),
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Por onde o sentido passa: quem não conhece a ponta da linha reconhece o caminho.
                direction?.let(viaFor)?.let { via ->
                    item(key = "via-${direction.id}") {
                        Text(
                            text = directionViaText(via),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val info = Regras.current.station(stop.id, stop.name)
                val address = info?.address
                val fare = Regras.current.fare(route.name)
                val wheelchair = SettingsStore.get(context).mobility == Mobility.WHEELCHAIR
                if (address != null || fare != null || info?.notice != null || info?.access != null || wheelchair) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOfNotNull(address, fare?.let { stringResource(R.string.station_fare, it) }).forEach {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // De onde vêm o endereço e a passagem, e até quando foram conferidos.
                            if (address != null || fare != null) SourceLine("passagem")
                            // Meia, gratuidade e onde tirar o cartão: o guia do regras.json.
                            if (fare != null && Regras.current.fareGuide != null) {
                                Text(
                                    text = stringResource(R.string.station_fare_guide),
                                    modifier = Modifier
                                        .clickable(onClick = onOpenFareGuide)
                                        .padding(vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            // Aviso da estação (regras.json), como o de estação subterrânea:
                            // destacado, porque pede uma ação antes de chegar.
                            info?.notice?.let {
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (info?.notice != null && info.underground) SourceLine("subterranea")
                            StationAccessLine(info?.access, wheelchair, onOpen = onOpenAccess)
                        }
                    }
                }

                // Dia de operação anunciada pelo Metrofor: a notícia dela explica o horário
                // diferente; num domingo sem ela, o aviso de que normalmente não há metrô.
                val day = ScheduleStore.today()
                val operation = Regras.current.operations[day]
                if (operation != null) {
                    item { MetroforNotices(listOf(ScheduleStore.Notice(operation.title, operation.link, listOf(day)))) }
                } else if (sunday) {
                    item { SundayNotice() }
                    // Os horários mostrados são os de segunda: vale também a notícia de amanhã.
                    Regras.current.operations[ScheduleStore.tomorrow()]?.let { next ->
                        item { MetroforNotices(listOf(ScheduleStore.Notice(next.title, next.link, listOf(ScheduleStore.tomorrow())))) }
                    }
                }
                if (Regras.current.stopped(day, route.name)) {
                    item { SundayNotice(text = stringResource(R.string.line_stopped_today)) }
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
                                    when {
                                        direction?.published == false -> R.string.direction_unpublished
                                        sundayOff -> R.string.no_departures_today
                                        else -> R.string.no_more_today
                                    },
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
                                        alert = alertAt(departure),
                                        onClick = { onPick(departure) },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = stringResource(
                                if (sundayOff) R.string.leave_hint_monday else R.string.leave_hint,
                                SettingsStore.get(context).leaveBeforeMinutes,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Daqui para qualquer estação, na mesma linha ou com integração (e daí o modo viagem).
                item {
                    OutlinedButton(onClick = onPlanJourney, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Text(stringResource(R.string.journey_from_here), modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (bikeRule != null) {
                    item {
                        // Dá para embarcar com bike hoje: o limite por viagem e as bikes que não entram.
                        val rules = if (bikeRule.allowed || bikeRule.boundary != null) {
                            listOfNotNull(
                                bikeRuleFor(stop.name, route.name)?.perTrip?.let { stringResource(R.string.bike_per_trip, it) },
                                Regras.current.bikeForbidden,
                            ).joinToString(" ").ifEmpty { null }
                        } else {
                            null
                        }
                        Column {
                        BikeBoardingNotice(bikeRule, vehicle = vehicleName(route.name), rules = rules)
                        // A regra vem do regulamento ou, num dia especial, da notícia do dia.
                        val base = bikeRuleFor(stop.name, route.name)
                        val sourcePadding = Modifier.padding(start = 24.dp)
                        if (base?.special == true) {
                            SourceLink(
                                label = stringResource(R.string.source_label),
                                title = stringResource(R.string.bike_source_today_title),
                                facts = listOf(stringResource(R.string.bike_source_today)),
                                detail = null,
                                link = Regras.current.operations[ScheduleStore.today()]?.link,
                                modifier = sourcePadding,
                            )
                        } else {
                            SourceLine(base?.sourceId, sourcePadding)
                        }
                        }
                    }
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
                            SourceLine("bicicletar")
                        }
                    }
                }

                // O que tem perto: hospital, universidade, shopping (o "entorno" da estação).
                if (nearby.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = stringResource(R.string.landmarks_near_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            nearby.take(NEARBY_COUNT).forEach { l ->
                                LandmarkRow(landmark = l, detail = null,
                                    trailing = landmarkWalk(l, walkMinutes(l)), onClick = null)
                                HorizontalDivider()
                            }
                            Text(
                                text = stringResource(R.string.landmarks_walk_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            SourceLine("lugares")
                        }
                    }
                }

                val monday = if (sundayOff) tomorrow.orEmpty() else emptyList()
                if (!sundayOff || monday.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(if (sundayOff) R.string.monday_departures else R.string.all_departures),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    item {
                        if (sundayOff) {
                            // Todos de amanhã, nenhum passou: o "agora" é a meia-noite.
                            AllDepartures(monday, now = 0, marked = { alertAt(Departure(it, tomorrow = true)) != null },
                                onPick = { m: Int -> onPick(Departure(m, tomorrow = true)) })
                        } else {
                            AllDepartures(all, now, marked = { alertAt(Departure(it, tomorrow = false)) != null },
                                onPick = { m: Int -> onPick(Departure(m, tomorrow = false)) })
                        }
                    }
                }

                item {
                    Text(
                        text = scheduleStatusText(schedule, syncing = false),
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
private fun DepartureRow(departure: Departure, now: Int, highlighted: Boolean, alert: LeaveAlert?, onClick: (() -> Unit)?) {
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
            alert?.let { LeaveBell(it) }
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

/** "🔔 07:35": o aviso marcado para esta partida. */
@Composable
private fun LeaveBell(alert: LeaveAlert) {
    Text(
        text = "🔔 " + formatTime(alert.departure - alert.before),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun relativeLabel(remaining: Int): String = when {
    remaining <= 0 -> stringResource(R.string.now)
    remaining < 60 -> stringResource(R.string.in_minutes, remaining)
    else -> stringResource(R.string.in_hours, remaining / 60, remaining % 60)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllDepartures(all: List<Int>, now: Int, marked: (Int) -> Boolean = { false }, onPick: ((Int) -> Unit)?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        all.forEach { minutes ->
            // Os que ainda não passaram ficam em destaque (e abrem o lembrete
            // na agenda); os que passaram ficam apagados.
            val upcoming = minutes >= now
            val bell = upcoming && marked(minutes)
            Surface(
                color = when {
                    bell -> MaterialTheme.colorScheme.primary
                    upcoming -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small,
                modifier = if (upcoming && onPick != null) Modifier.clickable { onPick(minutes) } else Modifier,
            ) {
                Text(
                    text = if (bell) "🔔 ${formatTime(minutes)}" else formatTime(minutes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (upcoming) FontWeight.Medium else FontWeight.Normal,
                    color = if (bell) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (upcoming) {
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
