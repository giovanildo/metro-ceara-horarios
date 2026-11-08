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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.CommuteKind
import io.github.giova.metroceara.data.CommutePick
import io.github.giova.metroceara.data.CommuteReason
import io.github.giova.metroceara.data.Itinerary
import io.github.giova.metroceara.data.Place
import io.github.giova.metroceara.data.Station
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.ui.theme.LocalDarkTheme
import io.github.giova.metroceara.ui.theme.routeColor

/** "Sua ida e volta" pronta para a tela. */
data class CommuteView(
    /** Para onde vai agora (já com a inversão). */
    val destination: Place,
    /** Para onde o app acha que a pessoa vai, sem a inversão. */
    val automatic: Place,
    val kind: CommuteKind,
    val origin: Station,
    val originName: String,
    val destinationName: String,
    val destinationRoute: String,
    val destinationStop: String,
    val lineColors: List<String>,
    val pick: CommutePick?,
    /** O trecho a trecho do metrô em destaque. */
    val route: RouteView?,
    /** Sem metrô: "Voltam segunda às 05:46" ou "Não há mais partidas". */
    val message: String?,
    val target: Int?,
    val vehicle: String,
)

/** "em 11 min" ou, de 1 hora para cima, "em 1h12". */
@Composable
private fun inTime(minutes: Int): String = when {
    minutes >= 60 -> stringResource(R.string.commute_in_hours, minutes / 60, minutes % 60)
    else -> stringResource(R.string.in_minutes, minutes.coerceAtLeast(0))
}

@Composable
private fun commuteTitle(place: Place, kind: CommuteKind): String = when {
    place == Place.HOME -> stringResource(R.string.commute_to_home)
    kind == CommuteKind.SCHOOL -> stringResource(R.string.commute_to_school)
    else -> stringResource(R.string.commute_to_work)
}

@Composable
private fun CommuteMeta(view: CommuteView, pick: CommutePick) {
    val target = view.target?.let(::formatTime).orEmpty()
    val text = when (pick.reason) {
        CommuteReason.NEXT -> stringResource(R.string.commute_meta_next, view.vehicle)
        CommuteReason.ARRIVE_BY -> stringResource(R.string.commute_meta_arrive, target)
        CommuteReason.TOO_LATE -> stringResource(R.string.commute_meta_too_late, target, view.vehicle)
        CommuteReason.LEAVE_AT -> stringResource(
            if (view.kind == CommuteKind.SCHOOL) R.string.commute_meta_leave_school else R.string.commute_meta_leave_work,
            target,
        )
        CommuteReason.EARLY -> stringResource(R.string.commute_meta_early, target, view.vehicle)
    }
    Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
}

/**
 * O bloco do alto da tela inicial: o sentido certo para agora e qual metrô pegar.
 * Tocar abre o roteiro; ⇄ mostra o outro sentido.
 */
@Composable
fun CommuteCard(
    view: CommuteView,
    now: Int,
    canStart: Boolean,
    onOpen: () -> Unit,
    onFlip: () -> Unit,
    onStart: () -> Unit,
) {
    val pick = view.pick
    val early = pick?.reason == CommuteReason.EARLY
    val colors = if (early) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
    val other = if (view.destination == Place.HOME) Place.WORK else Place.HOME
    Card(colors = colors, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (early) "⚠️ " + stringResource(R.string.commute_early_title)
                    else placeEmoji(view.destination, view.kind) + " " + commuteTitle(view.destination, view.kind),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FlipPill(commuteTitle(other, view.kind), onFlip)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                view.lineColors.forEach { color ->
                    Spacer(Modifier.padding(end = 5.dp).size(9.dp).clip(CircleShape).background(routeColor(color)))
                }
                Text("${view.originName} → ${view.destinationName}", style = MaterialTheme.typography.bodyMedium)
            }

            if (pick == null) {
                view.message?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
                }
                return@Column
            }

            CommuteMeta(view, pick)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(pick.main.departure), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(inTime(pick.main.departure - now), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.dest_arrives, formatTime(pick.main.arrival)), style = MaterialTheme.typography.bodyMedium)

            view.route?.takeIf { it.legs.size > 1 }?.let { route ->
                Text(
                    text = route.legs.joinToString(" → ") { "${formatTime(it.departure)} ${it.fromName}" },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                route.transfers.forEach { t ->
                    Text(
                        text = stringResource(R.string.route_transfer, t.stationName, t.needMinutes),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            pick.lastDeparture?.let { last ->
                val text = if (last == pick.main.departure) stringResource(R.string.commute_is_last, view.vehicle)
                else stringResource(
                    R.string.commute_last,
                    view.vehicle,
                    commuteTitle(view.destination, view.kind).replaceFirstChar { it.lowercase() },
                    formatTime(last),
                )
                Text("🌙 $text", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            if (pick.others.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    pick.others.forEach { option -> CommuteOption(option, view, Modifier.weight(1f)) }
                }
            }
            if (canStart && !early) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text(stringResource(R.string.leave_n_start))
                }
            }
        }
    }
}

@Composable
private fun FlipPill(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick),
    ) {
        Text(
            text = "⇄ $label",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CommuteOption(option: Itinerary, view: CommuteView, modifier: Modifier) {
    val late = view.destination == Place.WORK && view.target != null && option.arrival > view.target
    Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.55f), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(formatTime(option.departure), style = MaterialTheme.typography.titleSmall, color = Color.Unspecified)
            Text(
                text = if (late) stringResource(R.string.commute_arrives_late, formatTime(option.arrival))
                else stringResource(R.string.dest_arrives, formatTime(option.arrival)),
                style = MaterialTheme.typography.bodySmall,
                color = if (late) MaterialTheme.colorScheme.error else Color.Unspecified,
            )
        }
    }
}

/**
 * Para quem não definiu Casa nem Trabalho: a viagem mais recente com o próximo
 * horário, ou o caminho de volta ([reversed]) quando a pessoa já foi. ⇄ inverte.
 */
@Composable
fun RecentTripCard(
    row: RecentRow,
    times: Pair<Int, Int>?,
    reversed: Boolean,
    now: Int,
    onOpen: () -> Unit,
    onFlip: () -> Unit,
) {
    // Cor própria, fora do tema e das cores do papel de parede, para não se confundir
    // com os cartões das estações.
    val dark = LocalDarkTheme.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (dark) Color(0xFF1E4A40) else Color(0xFFCDEDE3),
            contentColor = if (dark) Color(0xFFBCEBDD) else Color(0xFF0B3B30),
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🕘 " + stringResource(if (reversed) R.string.commute_recent_back else R.string.commute_recent_title),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                FlipPill(stringResource(R.string.commute_recent_flip), onFlip)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                row.lines.forEach { (_, color) ->
                    Spacer(Modifier.padding(end = 5.dp).size(9.dp).clip(CircleShape).background(routeColor(color)))
                }
                Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            times?.let { (leaves, arrives) ->
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(
                        text = stringResource(R.string.commute_recent_leaves, formatTime(leaves)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(inTime(leaves - now), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = stringResource(R.string.commute_recent_arrival, row.destination, formatTime(arrives)),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
