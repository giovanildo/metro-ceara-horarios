package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Landmark
import java.util.Locale

/** O desenho de cada tipo de lugar do regras.json. */
fun landmarkEmoji(type: String): String = when (type) {
    "onibus" -> "🚌"
    "aviao" -> "✈️"
    "hospital" -> "🏥"
    "universidade" -> "🎓"
    "escola" -> "🏫"
    "shopping" -> "🛍️"
    "mercado" -> "🧺"
    "teatro" -> "🎭"
    "museu" -> "🏛️"
    "cultura" -> "🎨"
    "igreja" -> "⛪"
    "praca" -> "🌳"
    "parque" -> "🌳"
    "praia" -> "🏖️"
    "estadio" -> "🏟️"
    "governo" -> "🏢"
    "justica" -> "⚖️"
    else -> "📍"
}

/** "300 m", "1,1 km". */
fun formatWalkMeters(meters: Int): String =
    if (meters < 1000) "${(meters + 5) / 10 * 10} m" else String.format(Locale("pt", "BR"), "%.1f km", meters / 1000.0)

/** "300 m · 4 min a pé", ou "ao lado da estação". */
@Composable
fun landmarkWalk(landmark: Landmark, minutes: Int): String =
    if (landmark.beside) stringResource(R.string.landmark_beside)
    else stringResource(R.string.landmark_walk, formatWalkMeters(landmark.walkMeters), minutes)

/**
 * Um lugar numa lista: o desenho do tipo, o nome, uma linha de detalhe e, à
 * direita, a chegada (na busca) ou a caminhada (na tela da estação).
 */
@Composable
fun LandmarkRow(
    landmark: Landmark,
    detail: String?,
    trailing: String?,
    onClick: (() -> Unit)?,
    warning: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = landmarkEmoji(landmark.type),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(landmark.name, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (warning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
}
