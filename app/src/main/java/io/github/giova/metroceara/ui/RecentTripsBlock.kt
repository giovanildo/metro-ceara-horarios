package io.github.giova.metroceara.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.RecentTrip
import io.github.giova.metroceara.ui.theme.routeColor

/** Uma viagem recente pronta para a tela: "Benfica → Papicu" e as linhas na cor de cada uma. */
data class RecentRow(
    val trip: RecentTrip,
    val title: String,
    /** (nome, cor) da linha de partida e, se for outra, da de chegada. */
    val lines: List<Pair<String, String>>,
    val walk: Boolean,
)

/** "Recentes", debaixo de Casa e Trabalho: um toque repete, tocar e segurar remove. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentTripsBlock(rows: List<RecentRow>, onOpen: (RecentTrip) -> Unit, onRemove: (RecentTrip) -> Unit) {
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
