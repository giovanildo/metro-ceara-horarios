package io.github.giova.metroceara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.giova.metroceara.data.LeaveStore
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.JourneyPlanner.plainName
import io.github.giova.metroceara.data.RoutePoint
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.Stop
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.nowMinutes
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.ui.theme.routeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Uma linha na lista de destinos: as estações dela e como se chega a partir da
 * origem. [transfers] vazio = mesma linha; `null` = ainda sem origem.
 */
data class JourneyLine(
    val routeId: String,
    val routeName: String,
    val colorHex: String,
    val transfers: List<String>?,
    val stops: List<Stop>,
    /** O Metrofor avisou que a linha não circula (hoje, ou amanhã no domingo sem metrô). */
    val stopped: Boolean,
)

/** Os roteiros até o destino; [monday]: num domingo sem metrô, são os de segunda. */
data class JourneyResult(val options: List<RouteView>, val monday: Boolean, val message: String?)

/**
 * "Para onde você vai?": de onde (a estação em destaque, já escolhida), para onde
 * (busca sem acento, as estações por linha, com a chegada de quem sai agora) e o
 * roteiro, na mesma linha ou com integração, saindo agora ou no horário escolhido.
 * Saindo agora, o roteiro vira modo viagem; no horário escolhido (ou no roteiro de
 * segunda), vira lembrete na agenda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    initialOrigin: Station?,
    stations: List<Station>,
    linesFor: (Station?) -> List<JourneyLine>,
    arrivalsFor: (Station) -> Map<String, Int>,
    plan: (Station, RoutePoint, Int?) -> JourneyResult,
    canStartTrip: (Station) -> Boolean,
    onStartTrip: (Station, RouteView) -> Unit,
    onRepeat: (Station, JourneyLine, Stop, RouteView?) -> Unit,
    onBack: () -> Unit,
    /** Destino já escolhido (linha, estação): vindo do aviso da hora de sair, abre o roteiro direto. */
    initialDestination: Pair<String, String>? = null,
) {
    var origin by remember { mutableStateOf(initialOrigin) }
    var query by rememberSaveable { mutableStateOf("") }
    var destination by remember { mutableStateOf<Pair<JourneyLine, Stop>?>(null) }
    var pickingOrigin by remember { mutableStateOf(initialOrigin == null) }
    val lines = remember(origin) { linesFor(origin) }
    LaunchedEffect(lines) {
        initialDestination?.let { (routeId, stopId) ->
            val line = lines.firstOrNull { it.routeId == routeId }
            line?.stops?.firstOrNull { it.id == stopId }?.let { if (destination == null) destination = line to it }
        }
    }
    // A chegada de quem sai agora, calculada fora da tela.
    val arrivals by produceState(emptyMap<String, Int>(), origin) {
        value = origin?.let { o -> withContext(Dispatchers.IO) { arrivalsFor(o) } }.orEmpty()
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(origin) { if (origin != null && destination == null) runCatching { focus.requestFocus() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journey_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickingOrigin = true }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(stringResource(R.string.journey_from), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = origin?.let { "${plainName(it.stopName)} · ${it.routeName}" }
                                ?: stringResource(R.string.journey_pick_origin),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.journey_to)) },
                        placeholder = { Text(stringResource(R.string.dest_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).focusRequester(focus),
                    )
                }
            }
            StationList(
                lines = lines,
                query = query,
                detail = { line, stop ->
                    when {
                        line.stopped -> stringResource(R.string.dest_stopped)
                        else -> arrivals["${line.routeId}:${stop.id}"]?.let { stringResource(R.string.dest_arrives, formatTime(it)) }
                    }
                },
                header = { line -> lineHeader(line) },
                onPick = { line, stop -> destination = line to stop },
            )
        }
    }

    if (pickingOrigin) {
        StationPickerDialog(
            stations = stations,
            title = stringResource(R.string.journey_pick_origin),
            onPick = {
                origin = it
                destination = null
                pickingOrigin = false
            },
            onDismiss = { if (origin == null) onBack() else pickingOrigin = false },
        )
    }

    val from = origin
    destination?.let { (line, stop) ->
        if (from != null) {
            ModalBottomSheet(
                onDismissRequest = { destination = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                JourneyResultSheet(
                    origin = from,
                    destinationName = plainName(stop.name),
                    plan = { time -> plan(from, RoutePoint(line.routeId, stop.id), time) },
                    canStartTrip = canStartTrip(from),
                    onStartTrip = { onStartTrip(from, it) },
                    destination = line to stop,
                    onRepeat = { view -> onRepeat(from, line, stop, view) },
                )
            }
        }
    }
}

@Composable
private fun lineHeader(line: JourneyLine): String {
    val via = line.transfers?.let { t ->
        when (t.size) {
            0 -> stringResource(R.string.journey_same_line)
            1 -> stringResource(R.string.journey_via_one, t.first())
            else -> stringResource(R.string.journey_via_many, t.size)
        }
    }
    return listOfNotNull(line.routeName, if (line.stopped) stringResource(R.string.dest_stopped) else via).joinToString(" · ")
}

/** As estações por linha, com busca sem acento. */
@Composable
private fun StationList(
    lines: List<JourneyLine>,
    query: String,
    detail: @Composable (JourneyLine, Stop) -> String?,
    header: @Composable (JourneyLine) -> String,
    onPick: (JourneyLine, Stop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val needle = plainText(query)
    val shown = lines.map { line -> line to line.stops.filter { needle.isEmpty() || plainText(it.name).contains(needle) } }
        .filter { it.second.isNotEmpty() }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        if (shown.isEmpty()) {
            item { Text(stringResource(R.string.dest_none), modifier = Modifier.padding(vertical = 16.dp)) }
        }
        shown.forEach { (line, stops) ->
            item(key = "h${line.routeId}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp, bottom = 2.dp),
                ) {
                    Spacer(
                        modifier = Modifier
                            .size(10.dp)
                            .background(routeColor(line.colorHex), RoundedCornerShape(50)),
                    )
                    Text(
                        text = header(line),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            items(stops, key = { "${line.routeId}-${it.id}" }) { stop ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !line.stopped) { onPick(line, stop) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = plainName(stop.name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (line.stopped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    detail(line, stop)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

/** Escolher uma estação: todas, por linha, com busca. */
@Composable
fun StationPickerDialog(stations: List<Station>, title: String, onPick: (Station) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val lines = remember(stations) {
        stations.groupBy { it.routeId }.values.map { list ->
            val first = list.first()
            JourneyLine(first.routeId, first.routeName, first.routeColor, null,
                list.map { Stop(it.stopId, it.stopName, seq = 0) }, stopped = false)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.dest_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                StationList(
                    lines = lines,
                    query = query,
                    detail = { _, _ -> null },
                    header = { it.routeName },
                    onPick = { line, stop -> stations.firstOrNull { it.routeId == line.routeId && it.stopId == stop.id }?.let(onPick) },
                    modifier = Modifier.heightIn(max = 420.dp),
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * O resultado: o horário (agora ou escolhido), o cartão do roteiro, os outros
 * horários e o modo viagem (saindo agora) ou o lembrete na agenda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JourneyResultSheet(
    origin: Station,
    destinationName: String,
    plan: (Int?) -> JourneyResult,
    canStartTrip: Boolean,
    onStartTrip: (RouteView) -> Unit,
    destination: Pair<JourneyLine, Stop>,
    onRepeat: (RouteView?) -> Unit,
) {
    var time by remember { mutableStateOf<Int?>(null) }
    var selected by remember { mutableStateOf(0) }
    var pickingTime by remember { mutableStateOf(false) }
    var reminder by remember { mutableStateOf<LeaveReminder?>(null) }
    val result = remember(time) { plan(time) }
    val view = result.options.getOrNull(selected.coerceAtMost(result.options.lastIndex))

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.route_title, plainName(origin.stopName), destinationName),
            style = MaterialTheme.typography.titleLarge,
        )
        if (result.monday) SundayNotice(text = stringResource(R.string.journey_monday_note))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = time == null,
                onClick = { time = null; selected = 0 },
                label = { Text(stringResource(if (result.monday) R.string.journey_first else R.string.journey_now)) },
            )
            FilterChip(
                selected = time != null,
                onClick = { pickingTime = true },
                label = {
                    Text(time?.let { stringResource(R.string.journey_at, formatTime(it)) } ?: stringResource(R.string.journey_pick_time))
                },
            )
        }
        if (view == null) {
            Text(result.message.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            RouteCard(view)
            if (result.options.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.journey_other_times), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.options.forEachIndexed { i, option ->
                        FilterChip(
                            selected = option === view,
                            onClick = { selected = i },
                            label = { Text("${formatTime(option.itinerary.departure)} → ${formatTime(option.itinerary.arrival)}") },
                        )
                    }
                }
            }
            val first = view.legs.first()
            // Saindo agora, o roteiro vira viagem; no horário escolhido ou na segunda, lembrete.
            if (time == null && !result.monday && canStartTrip) {
                Button(onClick = { onStartTrip(view) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Navigation, contentDescription = null)
                    Text(stringResource(R.string.journey_start), modifier = Modifier.padding(start = 8.dp))
                }
                Text(stringResource(R.string.journey_trip_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (result.monday || first.departure >= nowMinutes()) {
                OutlinedButton(
                    onClick = {
                        val leg = view.itinerary.legs.first()
                        reminder = LeaveReminder(
                            Departure(first.departure, tomorrow = result.monday), first.fromName,
                            first.routeName, first.headsign, vehicleName(first.routeName),
                            stopId = leg.fromStop, routeId = leg.routeId, directionId = leg.directionId,
                            destStopId = destination.second.id, destRouteId = destination.first.routeId,
                            destName = destinationName, arrival = view.itinerary.arrival,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null)
                    Text(
                        text = if (result.monday) stringResource(R.string.journey_reminder_monday, formatTime(first.departure))
                        else stringResource(R.string.journey_reminder),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        // O trajeto do dia a dia vira rotina: o app avisa sozinho nos dias marcados.
        TextButton(onClick = { onRepeat(view) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.routine_repeat))
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(
            initialHour = (time ?: nowMinutes()) / 60, initialMinute = (time ?: nowMinutes()) % 60, is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(if (result.monday) R.string.journey_pick_time_monday else R.string.journey_pick_time)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    time = state.hour * 60 + state.minute
                    selected = 0
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    val context = LocalContext.current
    reminder?.let {
        // Já marcado (pelo toque num horário ou antes): abre o aviso dele.
        val existing = remember(it) { LeaveStore.alert(context, leaveAlertId(it)) }
        LeaveReminderDialog(reminder = it, existing = existing, onDismiss = { reminder = null })
    }
}

/** A barra "Para onde você vai?" do topo da tela inicial. */
@Composable
fun JourneySearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = stringResource(R.string.journey_search_hint),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}

/** "José de Alencar" e "jose de alencar" viram o mesmo texto, para a busca. */
private fun plainText(text: String): String =
    java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
