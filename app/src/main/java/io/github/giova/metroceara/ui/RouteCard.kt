package io.github.giova.metroceara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Itinerary
import io.github.giova.metroceara.data.TransferFare
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.vehicleName
import io.github.giova.metroceara.ui.theme.routeColor

/** Um trecho do roteiro, já com nomes para mostrar. */
data class LegView(
    val routeName: String,
    val colorHex: String,
    val headsign: String,
    val fromName: String,
    val toName: String,
    val departure: Int,
    val arrival: Int,
)

/** Uma integração do roteiro, já com nomes e a passagem. */
data class TransferView(
    val stationName: String,
    val needMinutes: Int,
    val tight: Boolean,
    val nextDeparture: Int?,
    val fare: TransferFare,
)

/** Um roteiro pronto para a tela: trechos, integrações e avisos (bike). */
data class RouteView(
    val itinerary: Itinerary,
    val legs: List<LegView>,
    val transfers: List<TransferView>,
    val warnings: List<String> = emptyList(),
) {
    val originName get() = legs.first().fromName
    val destinationName get() = legs.last().toName
}

/**
 * O cartão do roteiro: cada trecho na cor da linha, a troca a pé com o tempo da
 * pessoa e a passagem, a chegada, a dica de horário e os avisos (bike).
 */
@Composable
fun RouteCard(view: RouteView, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp),
        ) {
            view.legs.forEachIndexed { i, leg ->
                LegRow(leg)
                view.transfers.getOrNull(i)?.let { TransferRow(it, view.legs[i + 1]) }
            }
            Text(
                text = stringResource(R.string.route_arrival, formatTime(view.itinerary.arrival)),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            view.itinerary.sameArrivalFrom?.let { earlier ->
                Tip(stringResource(R.string.route_tip_same_arrival, vehicleName(view.legs.first().routeName), formatTime(earlier)))
            }
            view.warnings.forEach { Tip(it, warning = true) }
        }
    }
}

/** "Metrô da Linha Sul", "VLT da Linha Nordeste", "VLT Aeroporto". */
fun lineLabel(routeName: String): String =
    if (routeName.startsWith("Linha")) "${vehicleName(routeName).replaceFirstChar { it.uppercase() }} da $routeName" else routeName

@Composable
private fun LegRow(leg: LegView) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Spacer(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(routeColor(leg.colorHex), RoundedCornerShape(3.dp)),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "${formatTime(leg.departure)} ${leg.fromName}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.route_leg, lineLabel(leg.routeName), leg.headsign, leg.toName, formatTime(leg.arrival)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransferRow(transfer: TransferView, next: LegView) {
    val fare = when (transfer.fare) {
        TransferFare.FREE -> " · " + stringResource(R.string.route_fare_free)
        TransferFare.PAID -> " · " + stringResource(R.string.route_fare_paid)
        TransferFare.UNKNOWN -> ""
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.route_transfer, transfer.stationName, transfer.needMinutes) + fare,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Tag(
            text = stringResource(if (transfer.tight) R.string.route_tight else R.string.route_ok),
            color = if (transfer.tight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
        if (transfer.tight) {
            Text(
                text = transfer.nextDeparture?.let {
                    stringResource(R.string.route_tight_next, vehicleName(next.routeName), formatTime(it))
                } ?: stringResource(R.string.route_tight_last),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
private fun Tip(text: String, warning: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(10.dp),
            )
            .padding(10.dp),
    )
}
