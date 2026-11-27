package io.github.giova.metroceara.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.BikeBoarding
import io.github.giova.metroceara.data.formatTime

/**
 * Uma linha dizendo se dá para levar a bicicleta no trem agora e, se vier [rules]
 * (limite por viagem, bikes proibidas), uma segunda linha com elas.
 */
@Composable
fun BikeBoardingNotice(rule: BikeBoarding, vehicle: String, modifier: Modifier = Modifier, rules: String? = null) {
    TimeWindowNotice(Icons.AutoMirrored.Filled.DirectionsBike, bikeBoardingText(rule, vehicle), rule.allowed, modifier, rules)
}

/** O aviso de compras e volumes grandes, com as medidas ([sizes]) na segunda linha. */
@Composable
fun VolumeBoardingNotice(rule: BikeBoarding, vehicle: String, sizes: String?, modifier: Modifier = Modifier) {
    val text = when {
        rule.allowed && rule.boundary != null ->
            stringResource(R.string.volume_boarding_until, vehicle, formatTime(rule.boundary))
        rule.allowed -> stringResource(R.string.volume_boarding_until_close, vehicle)
        rule.boundary != null -> stringResource(R.string.volume_boarding_from, vehicle, formatTime(rule.boundary))
        rule.anyToday -> stringResource(R.string.volume_boarding_no_more_today, vehicle)
        else -> stringResource(R.string.volume_boarding_not_today, vehicle)
    }
    TimeWindowNotice(Icons.Filled.ShoppingBag, text, rule.allowed, modifier, sizes)
}

/** Ícone + frase (em destaque quando [allowed]) e, se vier, uma segunda linha ([detail]). */
@Composable
private fun TimeWindowNotice(icon: ImageVector, text: String, allowed: Boolean, modifier: Modifier, detail: String?) {
    val color = if (allowed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (allowed) FontWeight.Medium else FontWeight.Normal,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    detail?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, top = 2.dp),
        )
    }
    }
}

/** A frase do aviso de bike ("Pode embarcar com bike no metrô agora, até as 15:00"); também vai no compartilhar. */
@Composable
fun bikeBoardingText(rule: BikeBoarding, vehicle: String): String = when {
    rule.unknown -> stringResource(R.string.bike_boarding_unknown)
    rule.allowed && rule.boundary != null ->
        stringResource(R.string.bike_boarding_until, vehicle, formatTime(rule.boundary))
    rule.allowed -> stringResource(R.string.bike_boarding_until_close, vehicle)
    rule.boundary != null ->
        stringResource(R.string.bike_boarding_from, vehicle, formatTime(rule.boundary))
    rule.anyToday -> stringResource(R.string.bike_boarding_no_more_today, vehicle)
    else -> stringResource(R.string.bike_boarding_not_today, vehicle)
}
