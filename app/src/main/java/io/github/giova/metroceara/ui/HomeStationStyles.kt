package io.github.giova.metroceara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.HomeStyle
import io.github.giova.metroceara.data.RecentTrip
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.minutesUntil
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.ui.theme.routeColor

/** O jeito escolhido e o que a estação aberta precisa para desenhá-lo. */
data class StationStyle(
    val style: HomeStyle = HomeStyle.CLASSIC,
    val trips: List<StationTrip> = emptyList(),
    val onOpen: (RecentTrip) -> Unit = {},
    val onOther: (Station) -> Unit = {},
)

/**
 * Os horários da estação aberta, no jeito escolhido. A viagem que usa um sentido
 * desta estação entra nele; a que não usa (sai de outra linha, ou sem metrô agora)
 * fica na faixa de sempre, embaixo, para nenhum jeito inventar horário.
 */
@Composable
internal fun StyledDepartures(styled: StationStyle, station: Station, hereLabel: String, departures: List<HomeDeparture>, now: Int) {
    val onRows = styled.trips.filter { trip ->
        trip.fromStation && trip.times != null && departures.any { it.directionId == trip.directionId && it.departure != null }
    }
    val rest = styled.trips - onRows.toSet()
    val view = StyleView(station, hereLabel, departures, onRows, now, styled.onOpen)
    val classic = styled.style == HomeStyle.CLASSIC
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        if (departures.isNotEmpty()) when (styled.style) {
            HomeStyle.RAIL -> RailStyle(view)
            HomeStyle.TRIP_FIRST -> TripFirstStyle(view)
            HomeStyle.SIDES -> SidesStyle(view)
            HomeStyle.ASK_FIRST -> AskFirstStyle(view) { styled.onOther(station) }
            HomeStyle.PHRASES -> PhrasesStyle(view)
            HomeStyle.CLASSIC -> departures.forEach { SentidoRow(view, it, showVia = false, leaves = false) }
        }
        when {
            classic && styled.trips.isNotEmpty() -> StationTrips(styled.trips, styled.onOpen) { styled.onOther(station) }
            rest.isNotEmpty() -> StationTrips(rest, styled.onOpen) { styled.onOther(station) }
            classic -> Unit
            styled.style == HomeStyle.ASK_FIRST && departures.isNotEmpty() -> Unit
            else -> OtherLink(styled.trips.isNotEmpty()) { styled.onOther(station) }
        }
    }
}

private data class StyleView(
    val station: Station,
    val hereLabel: String,
    val departures: List<HomeDeparture>,
    val trips: List<StationTrip>,
    val now: Int,
    val onOpen: (RecentTrip) -> Unit,
) {
    val vehicle: String get() = vehicleName(station.routeName)
    val vehicleTitle: String get() = vehicle.replaceFirstChar { it.uppercase() }
    fun tripsOn(item: HomeDeparture) = trips.filter { it.directionId == item.directionId }
    val leftFirst get() = departures.sortedBy { it.directionId }

    /** A viagem usa o próximo metrô deste sentido? */
    fun isNext(trip: StationTrip, item: HomeDeparture) = item.departure?.minutes == trip.times?.first
}

@Composable
private fun OtherLink(hasTrips: Boolean, onClick: () -> Unit) {
    Text(
        text = stringResource(if (hasTrips) R.string.station_trip_other else R.string.station_where_to_link),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}

@Composable
private fun leaveLabel(minutes: Int) = stringResource(R.string.station_leaves, relativeLabel(minutes))

/** "em 5 min" grande e a hora embaixo; sem partida, o motivo. */
@Composable
private fun TimeColumn(view: StyleView, item: HomeDeparture, leaves: Boolean, modifier: Modifier = Modifier) {
    val departure = item.departure
    if (departure == null) {
        Text(
            text = stringResource(if (item.published) R.string.home_no_departures else R.string.home_unpublished),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = modifier,
        )
        return
    }
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        val left = minutesUntil(departure, view.now)
        Text(
            text = if (leaves) leaveLabel(left) else relativeLabel(left),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formatTime(departure.minutes),
            style = MaterialTheme.typography.bodySmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun DayEndLine(item: HomeDeparture) {
    item.dayEnd?.let { dayEndText(it) }?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f))
    }
}

/** No tamanho e na cor do nome do sentido: é o nome que a pessoa mais provavelmente conhece. */
@Composable
private fun ViaLine(item: HomeDeparture, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    item.via?.let { Text(text = stringResource(R.string.station_via, it), style = style) }
}

/** Um sentido em linha, com o desenho do metrô virando. */
@Composable
private fun SentidoRow(view: StyleView, item: HomeDeparture, showVia: Boolean, leaves: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp), verticalAlignment = Alignment.Bottom) {
            DirectionTurn(
                toLeft = item.directionId == 0,
                color = routeColor(view.station.routeColor),
                modifier = Modifier.padding(end = 10.dp, bottom = 2.dp),
                width = 40.dp,
            )
            Column {
                DirectionLabel(headsign = item.headsign)
                if (showVia) ViaLine(item)
            }
        }
        TimeColumn(view, item, leaves)
    }
    DayEndLine(item)
}

/** Integração e trecho a pé, quando houver. */
@Composable
private fun tripNotes(trip: StationTrip): String? = listOfNotNull(
    stringResource(R.string.station_trip_transfer).takeIf { trip.row.lines.size > 1 },
    stringResource(R.string.recent_walk).takeIf { trip.row.walk },
).joinToString(" · ").takeIf { it.isNotEmpty() }

/** "Desça em Parangaba · chega às 08:19", dentro do sentido que a viagem usa. */
@Composable
private fun tripInRow(view: StyleView, trip: StationTrip, item: HomeDeparture): String {
    val (leaves, arrives) = trip.times!!
    val text = if (trip.direct) {
        stringResource(R.string.station_trip_get_off, trip.row.destination, formatTime(arrives))
    } else {
        stringResource(R.string.station_trip_note, trip.row.destination, tripNotes(trip).orEmpty(), formatTime(arrives))
    }
    return if (view.isNext(trip, item)) text else stringResource(R.string.station_trip_take, formatTime(leaves)) + " · " + text
}

@Composable
private fun RailStyle(view: StyleView) {
    val ordered = view.departures.sortedByDescending { it.directionId }
    val top = ordered.takeIf { it.size > 1 }?.first()
    val bottom = ordered.last()
    val color = routeColor(view.station.routeColor)
    val nodes = buildList<@Composable () -> Unit> {
        top?.let { item ->
            add { RailEnd(view, item) }
            view.tripsOn(item).sortedByDescending { it.times!!.second }.forEach { add { RailPin(view, it) } }
        }
        add { RailHere(view.hereLabel) }
        view.tripsOn(bottom).sortedBy { it.times!!.second }.forEach { add { RailPin(view, it) } }
        add { RailEnd(view, bottom) }
    }
    val hereIndex = top?.let { 1 + view.tripsOn(it).size } ?: 0
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        nodes.forEachIndexed { i, node ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Canvas(Modifier.width(34.dp).fillMaxHeight()) {
                    val x = size.width / 2
                    val middle = size.height / 2
                    val stroke = 7.dp.toPx()
                    drawLine(
                        color,
                        Offset(x, if (i == 0) middle else 0f),
                        Offset(x, if (i == nodes.lastIndex) middle else size.height),
                        stroke,
                        if (i == 0 || i == nodes.lastIndex) StrokeCap.Round else StrokeCap.Butt,
                    )
                    if (i == hereIndex) {
                        drawCircle(Color.White, 13.dp.toPx(), Offset(x, middle))
                        drawCircle(Color(0xFF44598F), 9.dp.toPx(), Offset(x, middle))
                    } else {
                        drawCircle(Color.White, 7.dp.toPx(), Offset(x, middle))
                        drawCircle(color, 6.dp.toPx(), Offset(x, middle), style = Stroke(3.dp.toPx()))
                    }
                }
                Box(Modifier.weight(1f).padding(start = 6.dp), contentAlignment = Alignment.CenterStart) { node() }
            }
        }
    }
}

@Composable
private fun RailHere(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun RailEnd(view: StyleView, item: HomeDeparture) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                DirectionLabel(headsign = item.headsign)
                if (view.tripsOn(item).isEmpty()) ViaLine(item)
            }
            TimeColumn(view, item, leaves = true)
        }
        DayEndLine(item)
    }
}

@Composable
private fun RailPin(view: StyleView, trip: StationTrip) {
    val (leaves, arrives) = trip.times!!
    val (background, content) = tripColors()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
        contentColor = content,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { view.onOpen(trip.row.trip) },
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(trip.row.destination, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = listOfNotNull(stringResource(R.string.station_trip_pin, view.vehicle, formatTime(leaves)), tripNotes(trip)).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(formatTime(arrives), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripFirstStyle(view: StyleView) {
    val (background, content) = tripColors()
    val covered = view.trips.mapNotNull { it.directionId }.toSet()
    val others = view.departures.filter { it.directionId !in covered }
    if (view.trips.isNotEmpty()) {
        Surface(shape = RoundedCornerShape(12.dp), color = background, contentColor = content, modifier = Modifier.fillMaxWidth()) {
            Column {
                val single = view.trips.singleOrNull()
                if (single != null) {
                    val (leaves, arrives) = single.times!!
                    Column(Modifier.fillMaxWidth().clickable { view.onOpen(single.row.trip) }.padding(14.dp)) {
                        Text(stringResource(R.string.station_trip_to, single.row.destination), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(leaveLabel(leaves - view.now), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.station_vehicle_arrives, view.vehicleTitle, formatTime(leaves), formatTime(arrives)), style = MaterialTheme.typography.bodyLarge)
                        tripNotes(single)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                } else view.trips.forEachIndexed { i, trip ->
                    val (leaves, arrives) = trip.times!!
                    if (i > 0) HorizontalDivider(color = content.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 14.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { view.onOpen(trip.row.trip) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(stringResource(R.string.station_trip_to, trip.row.destination), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.station_vehicle_arrives, view.vehicleTitle, formatTime(leaves), formatTime(arrives)), style = MaterialTheme.typography.bodyMedium)
                            tripNotes(trip)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                        Text(leaveLabel(leaves - view.now), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        if (others.isNotEmpty()) {
            Text(
                text = stringResource(R.string.station_other_side),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    others.forEach { SentidoRow(view, it, showVia = true) }
}

@Composable
private fun SidesStyle(view: StyleView) {
    val (background, content) = tripColors()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        view.leftFirst.forEach { item ->
            val trip = view.tripsOn(item).firstOrNull()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (trip != null) background else MaterialTheme.colorScheme.surface,
                contentColor = if (trip != null) content else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).fillMaxHeight().then(
                    if (trip != null) Modifier.clickable { view.onOpen(trip.row.trip) } else Modifier,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    DirectionTurn(toLeft = item.directionId == 0, color = routeColor(view.station.routeColor), width = 40.dp)
                    DirectionLabel(headsign = item.headsign, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleSmall)
                    if (trip == null) ViaLine(item, MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.size(6.dp))
                    TimeColumn(view, item, leaves = true, modifier = Modifier.fillMaxWidth())
                    view.tripsOn(item).forEach {
                        HorizontalDivider(color = content.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 7.dp))
                        Text(tripInRow(view, it, item), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    view.departures.forEach { DayEndLine(it) }
}

@Composable
private fun AskFirstStyle(view: StyleView, onAsk: () -> Unit) {
    val (background, content) = tripColors()
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAsk),
    ) {
        Text(
            text = "🔍  " + stringResource(R.string.station_where_to),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
        )
    }
    view.trips.forEach { trip ->
        val (leaves, arrives) = trip.times!!
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = background,
            contentColor = content,
            modifier = Modifier.fillMaxWidth().clickable { view.onOpen(trip.row.trip) },
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(trip.row.destination, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = listOfNotNull(leaveLabel(leaves - view.now), tripNotes(trip)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(stringResource(R.string.dest_arrives, formatTime(arrives)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
    Text(
        text = stringResource(R.string.station_next_departures),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 6.dp),
    )
    view.departures.forEach { SentidoRow(view, it, showVia = view.tripsOn(it).isEmpty()) }
}

@Composable
private fun PhrasesStyle(view: StyleView) {
    val (background, content) = tripColors()
    view.leftFirst.forEach { item ->
        val trips = view.tripsOn(item)
        val mine = trips.isNotEmpty()
        val departure = item.departure
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (mine) background else MaterialTheme.colorScheme.surface,
            contentColor = if (mine) content else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().then(
                trips.firstOrNull()?.let { trip -> Modifier.clickable { view.onOpen(trip.row.trip) } } ?: Modifier,
            ),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                if (departure != null) {
                    Text(leaveLabel(minutesUntil(departure, view.now)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = stringResource(R.string.direction_label) + " " + item.headsign,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    mine -> trips.forEach { Text(tripInRow(view, it, item), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
                    departure != null -> Text(
                        text = item.via?.let { stringResource(R.string.station_via_vehicle, it, view.vehicle, formatTime(departure.minutes)) }
                            ?: stringResource(R.string.station_vehicle_at, view.vehicleTitle, formatTime(departure.minutes)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> Text(
                        text = stringResource(if (item.published) R.string.home_no_departures else R.string.home_unpublished),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                DayEndLine(item)
            }
        }
    }
}
