package io.github.giova.metroceara.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.Rail
import io.github.giova.metroceara.data.RailRow
import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.TripRail
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.nowMinutes
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.trip.ActiveTrip
import io.github.giova.metroceara.trip.TripArrival
import io.github.giova.metroceara.ui.theme.routeColor
import kotlin.math.floor
import kotlin.math.roundToInt

private const val SLIDE_MILLIS = 700
private val RAIL_X = 39.dp
private val TEXT_START = 68.dp

/** A última posição mostrada: ao voltar para o app, o trilho desliza desde ela. */
private object RailMemory {
    var seen: Pair<String, Float>? = null
}

/** Onde cada linha do trilho ficou; o desenho lê daqui, depois do posicionamento. */
private class RailGeometry {
    var ys = FloatArray(0)
    var visible = BooleanArray(0)
    var top = 0f
    var youY = 0f
}

/**
 * O modo viagem: a tela inteira é o trilho. Quem está na viagem fica sempre na
 * mesma altura e as estações descem, do destino (em cima) para as que já
 * passaram. Só desliza quando a estação muda: a tela nunca diz mais do que o
 * estimador sabe. O voltar do celular só tira o app da frente; a viagem segue.
 */
@Composable
fun TripRailScreen(
    trip: ActiveTrip,
    lineColorHex: String,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    stationAlerts: Boolean,
    onToggleStationAlerts: (Boolean) -> Unit,
    onNina: (() -> Unit)?,
    onStop: () -> Unit,
) {
    val plan = trip.plan
    val progress = trip.progress
    val journey = plan.journey
    val rail = remember(plan, progress.index, progress.delayMinutes.roundToInt(), lineColorHex) {
        TripRail.build(plan, progress, lineColorHex)
    }
    val waiting = progress.index < 0
    val estimated = !waiting && (progress.presumed || progress.gpsSilentMinutes != null)
    val finalName = journey?.finalName ?: JourneyPlanner.plainName(plan.destination.name)
    val arrival = if (plan.transfer == null) progress.arrivalMinutes.roundToInt() else journey?.arrival ?: progress.arrivalMinutes.roundToInt()
    var confirmStop by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            RailHeader(trip, rail, finalName, arrival, soundEnabled, onToggleSound, stationAlerts, onToggleStationAlerts)
            progress.gpsSilentMinutes?.let { GpsLostBanner(it.toInt().coerceAtLeast(1)) }

            val currentNote = when {
                waiting -> stringResource(if ((journey?.current ?: 0) > 0) R.string.trip_rail_go_platform else R.string.trip_rail_wait_platform)
                estimated -> stringResource(R.string.trip_rail_estimated)
                progress.fix == TripFix.SIMULATED -> stringResource(fixLabel(progress.fix))
                else -> stringResource(R.string.trip_rail_here, stringResource(fixLabel(progress.fix)))
            }
            val left = pluralStringResource(R.plurals.trip_stations_left, rail.stationsLeft, rail.stationsLeft)
            val description = if (waiting) {
                stringResource(
                    R.string.trip_waiting, plan.origin.name,
                    formatTime((plan.origin.scheduled + progress.delayMinutes).roundToInt()), vehicleName(plan.routeName),
                )
            } else {
                val at = plan.stops[progress.index.coerceAtMost(plan.stops.lastIndex)].name
                stringResource(
                    R.string.trip_progress_line,
                    progress.nextStop(plan)?.let { stringResource(R.string.trip_next_inline, at, it.name) } ?: at,
                    left, formatTime(arrival), stringResource(fixLabel(progress.fix)),
                )
            }
            StationRail(rail, estimated, currentNote, finalName, plan.placeName, plan.routeName, description, Modifier.weight(1f).fillMaxWidth())

            if (waiting) BoardingCard(trip)

            // Com letra grande, "Encerrar viagem" desce em vez de ser espremido.
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (onNina != null) NinaButton(onNina) else Spacer(Modifier.size(0.dp))
                OutlinedButton(onClick = { confirmStop = true }) { Text(stringResource(R.string.trip_stop)) }
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(R.string.trip_stop_title)) },
            text = { Text(stringResource(R.string.trip_stop_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    onStop()
                }) { Text(stringResource(R.string.trip_stop_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.trip_stop_keep)) } },
        )
    }
}

@Composable
private fun RailHeader(
    trip: ActiveTrip,
    rail: Rail,
    finalName: String,
    arrival: Int,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    stationAlerts: Boolean,
    onToggleStationAlerts: (Boolean) -> Unit,
) {
    val plan = trip.plan
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val lines = (plan.journey?.legs?.map { it.routeName }?.distinct() ?: listOf(plan.routeName))
                .joinToString(stringResource(R.string.trip_rail_then))
            Text(
                text = if (plan.simulated) stringResource(R.string.trip_sim) + " · " + lines else lines,
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Text(
                text = stringResource(R.string.trip_rail_to, finalName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            val at = stringResource(R.string.trip_rail_arrives, formatTime(arrival))
            Text(
                text = if (rail.stationsLeft > 0) {
                    at + " · " + pluralStringResource(
                        R.plurals.trip_rail_left, rail.stationsLeft,
                        pluralStringResource(R.plurals.trip_stations_left, rail.stationsLeft, rail.stationsLeft),
                    )
                } else {
                    at
                },
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
            plan.terminal?.takeIf { plan.viaTerminal }?.let {
                Text(stringResource(R.string.trip_via_terminal, it.name), style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
        FilledTonalIconToggleButton(checked = soundEnabled, onCheckedChange = onToggleSound) {
            Icon(
                imageVector = if (soundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(if (soundEnabled) R.string.trip_sound_on else R.string.trip_sound_off),
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.trip_rail_more_options))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.trip_station_alerts)) },
                    onClick = {
                        onToggleStationAlerts(!stationAlerts)
                        menuOpen = false
                    },
                    trailingIcon = {
                        if (stationAlerts) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.trip_rail_on))
                    },
                )
            }
        }
    }
}

@Composable
private fun GpsLostBanner(minutes: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Filled.GpsOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(stringResource(R.string.trip_gps_lost_title, minutes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.trip_gps_lost_body), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Parado antes de embarcar, o trilho precisa dizer quando o metrô sai. */
@Composable
private fun BoardingCard(trip: ActiveTrip) {
    val plan = trip.plan
    val departure = (plan.origin.scheduled + trip.progress.delayMinutes).roundToInt()
    val minutes = departure - nowMinutes()
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(start = TEXT_START, end = 16.dp, top = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.trip_rail_board_title, vehicleName(plan.routeName).replaceFirstChar { it.uppercase() }, formatTime(departure)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (minutes > 0) {
                    stringResource(R.string.trip_rail_board_in, formatDuration(minutes), plan.headsign)
                } else {
                    stringResource(R.string.trip_rail_board_now, plan.headsign)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun NinaButton(onClick: () -> Unit) {
    val description = stringResource(R.string.menu_nina_desc)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }
            .padding(vertical = 4.dp),
    ) {
        Image(painterResource(R.drawable.ic_nina), contentDescription = null, modifier = Modifier.size(38.dp))
        Text(
            text = stringResource(R.string.menu_nina),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun StationRail(
    rail: Rail,
    estimated: Boolean,
    currentNote: String,
    finalName: String,
    placeName: String?,
    routeName: String,
    description: String,
    modifier: Modifier,
) {
    val rows = rail.rows
    val key = rows.first().name + ">" + rows.last().name
    val target = rail.position.toFloat()
    val slide = remember(key) {
        Animatable(RailMemory.seen?.takeIf { it.first == key }?.second?.coerceAtMost(target) ?: target)
    }
    LaunchedEffect(key, target) {
        // Para a frente desliza; uma correção do GPS para trás não faz a linha dar ré na frente da pessoa.
        if (target > slide.value) slide.animateTo(target, tween(SLIDE_MILLIS)) else slide.snapTo(target)
        RailMemory.seen = key to target
    }
    val pos = slide.value

    val scheme = MaterialTheme.colorScheme
    val colors = rows.map { routeColor(it.colorHex) }
    val background = scheme.background
    val density = LocalDensity.current
    val gap = 62.dp * (1f + (density.fontScale - 1f).coerceAtLeast(0f) * 0.6f)
    val geometry = remember { RailGeometry() }
    val hidden = remember { mutableIntStateOf(0) }
    val here = colors[pos.roundToInt().coerceIn(0, rows.lastIndex)]

    Box(
        modifier = modifier
            .clipToBounds()
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Layout(
            content = {
                rows.forEachIndexed { i, row ->
                    RailRowContent(row, i - pos, currentNote, placeName, routeName)
                }
                MoreStationsChip(hidden.intValue, finalName)
                YouMarker(here, estimated, background)
            },
            modifier = Modifier.fillMaxSize().drawBehind {
                drawRail(rows, colors, geometry, pos, estimated, scheme.outline, scheme.outlineVariant, background)
            },
        ) { measurables, constraints ->
            val n = rows.size
            val rowConstraints = Constraints.fixedWidth(constraints.maxWidth)
            val placeables = measurables.take(n).map { it.measure(rowConstraints) }
            val chip = measurables[n].measure(Constraints())
            val marker = measurables[n + 1].measure(Constraints())

            // De baixo para cima, sem deixar nomes longos ou letra grande se encostarem.
            val distance = FloatArray(n)
            for (i in 1 until n) {
                val needed = (placeables[i - 1].height + placeables[i].height) / 2f + 6.dp.toPx()
                distance[i] = distance[i - 1] + maxOf(gap.toPx(), needed)
            }
            val low = floor(pos).toInt().coerceIn(0, n - 1)
            val high = (low + 1).coerceAtMost(n - 1)
            val travelled = distance[low] + (distance[high] - distance[low]) * (pos - low)

            val height = constraints.maxHeight.toFloat()
            val current = placeables[rail.position.coerceIn(0, n - 1)].height.toFloat().coerceAtLeast(marker.height.toFloat())
            // Na origem não há nada abaixo: o trilho aproveita a tela toda.
            val atOrigin = height - current / 2f - 12.dp.toPx()
            val youY = atOrigin + (height * 0.72f - atOrigin) * pos.coerceIn(0f, 1f)
            val ys = FloatArray(n) { youY - (distance[it] - travelled) }

            val fits = ys[n - 1] - placeables[n - 1].height / 2f >= 0f
            val top = if (fits) 0f else chip.height + 10.dp.toPx()
            val visible = BooleanArray(n) { it <= pos || ys[it] - placeables[it].height / 2f >= top }
            val notShown = (0 until n).count { !visible[it] && !rows[it].isWalk && !rows[it].isRepeat }

            geometry.ys = ys
            geometry.visible = visible
            geometry.top = top
            geometry.youY = youY

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeables.forEachIndexed { i, p ->
                    if (visible[i]) p.place(0, (ys[i] - p.height / 2f).roundToInt())
                }
                if (!fits) chip.place((constraints.maxWidth - chip.width) / 2, 2.dp.roundToPx())
                marker.place(RAIL_X.roundToPx() - marker.width / 2, (youY - marker.height / 2f).roundToInt())
                if (hidden.intValue != notShown) hidden.intValue = notShown
            }
        }
        // As que já passaram somem aos poucos; na origem não há nada ali e a faixa cobriria o trem.
        if (pos >= 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, background))),
            )
        }
    }
}

private fun DrawScope.drawRail(
    rows: List<RailRow>,
    colors: List<Color>,
    geometry: RailGeometry,
    pos: Float,
    estimated: Boolean,
    passed: Color,
    passedLine: Color,
    background: Color,
) {
    val ys = geometry.ys
    if (ys.size != rows.size) return
    val x = RAIL_X.toPx()
    val top = geometry.top
    val strong = 4.dp.toPx()
    val thin = 3.dp.toPx()
    val dashed = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx()))
    val dotted = PathEffect.dashPathEffect(floatArrayOf(0.1f, 8.dp.toPx()))

    fun line(from: Float, to: Float, color: Color, width: Float, effect: PathEffect? = null, cap: StrokeCap = StrokeCap.Butt) {
        val upper = maxOf(minOf(from, to), top)
        val lower = maxOf(from, to)
        if (lower > upper) drawLine(color, Offset(x, lower), Offset(x, upper), width, cap, effect)
    }

    for (i in 0 until rows.lastIndex) {
        val from = ys[i]
        val to = ys[i + 1]
        val color = colors[i + 1]
        val ahead: (Float, Float) -> Unit = { a, b -> line(a, b, color, strong, if (estimated) dashed else null) }
        when {
            rows[i].isWalk || rows[i + 1].isWalk -> line(from, to, passed, thin, dotted, StrokeCap.Round)
            i + 1 <= pos -> line(from, to, passedLine, thin)
            i >= pos -> ahead(from, to)
            else -> {
                line(from, geometry.youY, passedLine, thin)
                ahead(geometry.youY, to)
            }
        }
    }

    rows.forEachIndexed { i, row ->
        if (row.isWalk || !geometry.visible[i] || kotlin.math.abs(i - pos) < 0.5f) return@forEachIndexed
        val center = Offset(x, ys[i])
        val isPast = i < pos
        if (row.isDestination && !isPast) {
            drawCircle(colors[i], 8.dp.toPx(), center)
        } else {
            drawCircle(background, 6.dp.toPx(), center)
            drawCircle(if (isPast) passed else colors[i], 4.5.dp.toPx(), center, style = Stroke(3.dp.toPx()))
        }
    }
}

/** [offset]: distância em estações até a posição atual (positivo = ainda vem pela frente). */
@Composable
private fun RailRowContent(row: RailRow, offset: Float, currentNote: String, placeName: String?, routeName: String) {
    val scheme = MaterialTheme.colorScheme
    val closeness = (1f - kotlin.math.abs(offset)).coerceIn(0f, 1f)
    val ahead = offset > 0.01f
    val isCurrent = closeness >= 0.99f
    val fontScale = LocalDensity.current.fontScale

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(end = 14.dp)) {
        Box(modifier = Modifier.width(TEXT_START)) {
            if (row.isWalk) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .offset(x = RAIL_X - 13.dp)
                        .size(26.dp)
                        .background(scheme.background, CircleShape)
                        .padding(2.dp),
                )
            }
        }
        if (row.isWalk) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.trip_journey_transfer, row.name), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                walkNote(row)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
            }
            return@Row
        }
        Column(modifier = Modifier.weight(1f)) {
            // Letra muito grande: a estação atual para de crescer antes de ocupar a tela.
            val size = ((17f + 10f * closeness) * fontScale).coerceAtMost(40f) / fontScale
            Text(
                text = row.name,
                fontSize = size.sp,
                lineHeight = (size * 1.15f).sp,
                fontWeight = FontWeight((500 + 200 * closeness).toInt()),
                color = if (offset < -0.01f && !isCurrent) scheme.outline else scheme.onSurface,
            )
            when {
                isCurrent -> Text(currentNote, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                ahead -> RowNote(row, placeName, routeName)
            }
        }
        if (ahead && closeness < 0.5f && row.minutes != null) {
            Text(
                text = formatTime(row.minutes),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun walkNote(row: RailRow): String? {
    val walk = row.walkMinutes?.let { stringResource(R.string.trip_rail_walk, it) }
    val fare = row.walkFree?.let { stringResource(if (it) R.string.trip_rail_fare_free else R.string.trip_rail_fare_paid) }
    return listOfNotNull(walk, fare).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun RowNote(row: RailRow, placeName: String?, routeName: String) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val text = when {
        row.isDestination && placeName != null -> stringResource(R.string.trip_rail_get_off_place, placeName)
        row.isDestination -> stringResource(R.string.trip_rail_get_off)
        row.isTransfer -> stringResource(R.string.trip_rail_transfer_here)
        row.boardsLine != null && row.boardsAt != null -> stringResource(R.string.trip_rail_boards, row.boardsLine, formatTime(row.boardsAt))
        row.isTurnaround -> stringResource(R.string.trip_rail_turnaround, vehicleName(routeName))
        row.isAlert -> stringResource(R.string.trip_rail_alert_here)
        else -> return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (row.isAlert && !row.isTransfer && row.boardsLine == null && !row.isTurnaround) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = muted, modifier = Modifier.size(14.dp).padding(end = 2.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

@Composable
private fun MoreStationsChip(count: Int, finalName: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
        Text(
            text = pluralStringResource(R.plurals.trip_rail_more, count.coerceAtLeast(1), count.coerceAtLeast(1), finalName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun YouMarker(color: Color, estimated: Boolean, background: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .drawBehind {
                drawCircle(background)
                val radius = 22.dp.toPx()
                if (estimated) {
                    drawCircle(
                        color, radius - 1.5.dp.toPx(),
                        style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                } else {
                    drawCircle(color, radius)
                }
            },
    ) {
        Icon(Icons.Filled.Train, contentDescription = null, tint = if (estimated) color else Color.White, modifier = Modifier.size(26.dp))
    }
}

/** O resumo ao chegar ao destino. Quem encerra a viagem antes vai direto para o início. */
@Composable
fun TripArrivalScreen(arrival: TripArrival, lineColorHex: String, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        ) {
            Spacer(Modifier.height(72.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp).background(routeColor(lineColorHex), CircleShape),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(24.dp))
            if (arrival.simulated) {
                Text(stringResource(R.string.trip_sim), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.trip_arrival_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = arrival.destination,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(32.dp))
            ArrivalLine(stringResource(R.string.trip_arrival_left, arrival.origin), formatTime(arrival.departure))
            ArrivalLine(stringResource(R.string.trip_arrival_arrived), formatTime(arrival.arrival))
            ArrivalLine(stringResource(R.string.trip_arrival_duration), stringResource(R.string.trip_arrival_minutes, arrival.durationMinutes))
            ArrivalLine(stringResource(R.string.trip_arrival_stations), arrival.stations.toString())
            Spacer(Modifier.height(32.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.trip_arrival_done)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArrivalLine(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
    }
    HorizontalDivider()
}
