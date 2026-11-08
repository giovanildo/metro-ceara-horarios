package io.github.giova.metroceara.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.CommuteKind
import io.github.giova.metroceara.data.Place

/**
 * Um atalho da tela inicial. [name]: a estação guardada (`null`: ainda não
 * escolhida); [detail]: "chega 18:12", "você está aqui" ou nada.
 */
data class PlaceShortcut(val place: Place, val name: String?, val detail: String?, val here: Boolean)

fun placeEmoji(place: Place, kind: CommuteKind = CommuteKind.WORK) = when {
    place == Place.HOME -> "🏠"
    kind == CommuteKind.SCHOOL -> "🎓"
    else -> "💼"
}

fun placeLabel(place: Place, kind: CommuteKind = CommuteKind.WORK) = when {
    place == Place.HOME -> R.string.place_home
    kind == CommuteKind.SCHOOL -> R.string.place_school
    else -> R.string.place_work
}

/**
 * Casa e Trabalho, debaixo de "Para onde você vai?": um toque abre o roteiro
 * (ou, sem estação guardada, a escolha dela); tocar e segurar troca a estação.
 * O lugar onde a pessoa já está vai para o fim, sem destaque.
 */
@Composable
fun PlaceShortcuts(
    shortcuts: List<PlaceShortcut>,
    kind: CommuteKind = CommuteKind.WORK,
    onOpen: (Place) -> Unit,
    onEdit: (Place) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        shortcuts.sortedBy { it.here }.forEach { shortcut ->
            val label = stringResource(placeLabel(shortcut.place, kind))
            val filled = shortcut.name != null && !shortcut.here
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (filled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                contentColor = if (filled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.combinedClickable(
                    onClickLabel = shortcut.name?.let { stringResource(R.string.place_open, it) },
                    onLongClickLabel = stringResource(R.string.place_change),
                    onLongClick = { onEdit(shortcut.place) },
                    onClick = { if (shortcut.name == null) onEdit(shortcut.place) else onOpen(shortcut.place) },
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(placeEmoji(shortcut.place, kind), style = MaterialTheme.typography.titleLarge)
                    if (shortcut.name == null) {
                        Text(
                            text = stringResource(R.string.place_define, label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    } else {
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = listOfNotNull(shortcut.name.takeUnless { shortcut.here }, shortcut.detail).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
