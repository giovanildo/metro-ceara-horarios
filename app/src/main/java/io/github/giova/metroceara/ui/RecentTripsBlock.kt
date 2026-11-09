package io.github.giova.metroceara.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.RecentTrip
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.ui.theme.LocalDarkTheme
import io.github.giova.metroceara.ui.theme.routeColor

/** Uma viagem recente pronta para a tela: "Benfica → Papicu" e as linhas na cor de cada uma. */
data class RecentRow(
    val trip: RecentTrip,
    val title: String,
    /** Onde a viagem termina: a estação ou o lugar procurado. */
    val destination: String,
    /** (nome, cor) da linha de partida e, se for outra, da de chegada. */
    val lines: List<Pair<String, String>>,
    val walk: Boolean,
)

/** Uma viagem sugerida no cartão da estação aberta: saída e chegada de quem sai agora. */
data class StationTrip(
    val row: RecentRow,
    val times: Pair<Int, Int>?,
    /** Sai da estação do cartão. Sem estação conhecida, a linha diz de onde sai. */
    val fromStation: Boolean,
    /** O sentido do metrô a pegar na estação do cartão; `null` se sai de outra linha ou não há metrô. */
    val directionId: Int? = null,
    /** Onde trocar de linha pela primeira vez; `null` sem integração. */
    val transferAt: String? = null,
    /** A troca é logo na estação do cartão: o primeiro metrô é de outra linha. */
    val transferHere: Boolean = false,
) {
    /** Termina na estação de descida, sem integração nem trecho a pé. */
    val direct: Boolean get() = row.lines.size == 1 && !row.walk
}

/** Fundo e texto da viagem da pessoa: cor própria, fora do tema e do papel de parede. */
@Composable
internal fun tripColors(): Pair<Color, Color> =
    if (LocalDarkTheme.current) Color(0xFF1E4A40) to Color(0xFFBCEBDD) else Color(0xFFCDEDE3) to Color(0xFF0B3B30)

/**
 * No cartão da estação aberta: para onde a pessoa costuma ir dali, com o metrô a
 * pegar e a chegada. Cor própria, fora do tema e do papel de parede, a mesma da
 * última viagem desde a 1.90.
 */
@Composable
fun StationTrips(trips: List<StationTrip>, onOpen: (RecentTrip) -> Unit, onOther: () -> Unit) {
    val (background, content) = tripColors()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = background,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            trips.forEachIndexed { i, trip ->
                if (i > 0) HorizontalDivider(color = content.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 13.dp))
                StationTripRow(trip, onClick = { onOpen(trip.row.trip) })
            }
            Text(
                text = stringResource(R.string.station_trip_other),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOther).padding(start = 13.dp, end = 13.dp, top = 4.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun StationTripRow(trip: StationTrip, onClick: () -> Unit) {
    val row = trip.row
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "🕘 " + if (trip.fromStation) stringResource(R.string.station_trip_to, row.destination) else row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val transfer = when {
                trip.times == null || trip.transferAt == null -> null
                trip.transferHere -> stringResource(R.string.station_integration_here, trip.transferAt)
                else -> stringResource(R.string.station_integration, trip.transferAt)
            }
            // Trocar logo aqui é a primeira coisa a fazer: vem antes do metrô a pegar.
            if (trip.transferHere && transfer != null) Text(transfer, style = MaterialTheme.typography.bodyMedium)
            trip.times?.let { (leaves, _) ->
                Text(stringResource(R.string.station_trip_take, formatTime(leaves)), style = MaterialTheme.typography.bodyMedium)
            }
            val notes = listOfNotNull(
                transfer.takeIf { !trip.transferHere },
                stringResource(R.string.station_trip_transfer).takeIf { transfer == null && row.lines.size > 1 },
                stringResource(R.string.recent_walk).takeIf { row.walk },
            )
            if (notes.isNotEmpty() && trip.times != null) Text(notes.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
        }
        trip.times?.let { (_, arrives) ->
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.station_trip_arrives), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(arrives), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** "Recentes", debaixo de Casa e Trabalho: um toque repete, tocar e segurar remove. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentTripsBlock(
    rows: List<RecentRow>,
    /** Saída e chegada de quem sai agora; sem metrô hoje (domingo, linha parada), fica sem horário. */
    times: Map<RecentTrip, Pair<Int, Int>>,
    onOpen: (RecentTrip) -> Unit,
    onRemove: (RecentTrip) -> Unit,
) {
    if (rows.isEmpty()) return
    var removing by remember { mutableStateOf<RecentRow?>(null) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.recent_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.recent_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        rows.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onOpen(row.trip) }, onLongClick = { removing = row })
                    .padding(horizontal = 4.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        row.lines.forEach { (_, color) ->
                            Spacer(
                                Modifier
                                    .padding(end = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(routeColor(color)),
                            )
                        }
                        Text(
                            text = (row.lines.joinToString(" → ") { it.first }) +
                                if (row.walk) " · " + stringResource(R.string.recent_walk) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    }
                }
                times[row.trip]?.let { (leaves, arrives) ->
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = stringResource(R.string.dest_arrives, formatTime(arrives)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.recent_leaves, formatTime(leaves)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
    removing?.let { row ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(row.title) },
            confirmButton = {
                TextButton(onClick = { onRemove(row.trip); removing = null }) { Text(stringResource(R.string.recent_remove)) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
