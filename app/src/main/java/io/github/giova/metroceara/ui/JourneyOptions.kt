package io.github.giova.metroceara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.TransferFare
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.ui.theme.routeColor

/** Espera numa integração a partir da qual o cartão avisa ("espera 12 min"). */
private const val LONG_WAIT_MINUTES = 10

/** "11 min", "1h05". */
fun formatDuration(minutes: Int): String =
    if (minutes < 60) "$minutes min" else "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}"

/**
 * Um horário possível da viagem, como cartão de uma lista (à moda dos apps de
 * ônibus): número, duração, saída → chegada, integrações e as linhas na cor de
 * cada uma. Aberto ([expanded]), mostra o roteiro inteiro e os botões ([content]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JourneyOptionCard(
    number: Int,
    view: RouteView,
    finalWalk: FinalWalk?,
    /** "Próximo" no primeiro saindo agora. */
    next: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val arrival = view.itinerary.arrival + (finalWalk?.minutes ?: 0)
    // A maior espera entre um trem e o próximo, numa integração.
    val wait = view.legs.zipWithNext { a, b -> b.departure - a.arrival }.maxOrNull() ?: 0
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(if (expanded) 2.dp else 1.dp, if (expanded) primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            if (expanded) primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp),
                        ),
                ) {
                    Text(
                        text = number.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (expanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatDuration(arrival - view.itinerary.departure),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (next) Pill(stringResource(R.string.option_next), MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer)
                // Espera longa na integração: vale saber mesmo no próximo.
                if (wait >= LONG_WAIT_MINUTES) Pill(stringResource(R.string.option_wait, wait),
                    MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            }
            Text(
                text = "${formatTime(view.itinerary.departure)} → ${formatTime(arrival)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.option_transfers, transfersText(view)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                view.legs.forEachIndexed { i, leg ->
                    if (i > 0) Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = leg.routeName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .background(routeColor(leg.colorHex), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                RouteCard(view, finalWalk = finalWalk)
                content()
            }
        }
    }
}

/** "Direto", "1 em Parangaba · passagem grátis", "2: Parangaba e Expedicionários". */
@Composable
private fun transfersText(view: RouteView): String {
    val t = view.transfers
    return when (t.size) {
        0 -> stringResource(R.string.option_direct)
        1 -> stringResource(R.string.option_transfer_one, t.first().stationName) + when (t.first().fare) {
            TransferFare.FREE -> " · " + stringResource(R.string.route_fare_free)
            TransferFare.PAID -> " · " + stringResource(R.string.route_fare_paid)
            TransferFare.UNKNOWN -> ""
        }
        else -> stringResource(R.string.option_transfer_many, t.size, t.joinToString(", ") { it.stationName })
    }
}

@Composable
private fun Pill(text: String, background: Color, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
