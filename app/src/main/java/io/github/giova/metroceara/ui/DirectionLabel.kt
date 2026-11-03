package io.github.giova.metroceara.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DirectionVia

/**
 * Um sentido da linha: "sentido" pequeno em cima e o nome da ponta embaixo,
 * para não parecer o nome de uma estação de destino. Assim o nome não se
 * parte com a letra grande.
 */
@Composable
fun DirectionLabel(
    headsign: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = stringResource(R.string.direction_label),
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
        )
        Text(text = headsign, style = style)
    }
}

/** "Passa por **A, B, C, D**… até X · N estações", com os nomes do caminho em negrito. */
@Composable
fun directionViaText(via: DirectionVia): AnnotatedString {
    // Só falta a última: "Próxima estação: X, a última".
    if (via.shown.isEmpty()) {
        return buildAnnotatedString {
            append(stringResource(R.string.direction_via_next))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(via.end) }
            append(stringResource(R.string.direction_via_last))
        }
    }
    val count = stringResource(R.string.direction_via_count, via.count)
    val and = stringResource(R.string.direction_via_and)
    val names = if (via.more || via.shown.size == 1) via.shown.joinToString(", ")
    else via.shown.dropLast(1).joinToString(", ") + and + via.shown.last()
    return buildAnnotatedString {
        append(stringResource(R.string.direction_via_prefix))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(names) }
        if (via.more) append("…")
        append(stringResource(R.string.direction_via_until, via.end))
        append(" · ")
        append(count)
    }
}
