package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R

/**
 * O "esconder/mostrar" da tela inicial, como o saldo dos apps de banco. Aberto, é
 * um título pequeno com "Esconder ˄" à direita; fechado, uma linha fina com
 * "Mostrar ˅" no lugar do que sumiu. A linha toda é tocável.
 */
@Composable
fun CollapseToggle(
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** A cor do texto, quando a linha fica dentro de um cartão colorido. */
    color: Color = Color.Unspecified,
) {
    val open = stringResource(R.string.collapse_open)
    val closed = stringResource(R.string.collapse_closed)
    val row: @Composable (Modifier) -> Unit = { inner ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = inner,
        ) {
            Text(
                text = title,
                style = if (collapsed) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                color = if (color != Color.Unspecified) color
                else if (collapsed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).padding(end = 12.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (collapsed) R.string.collapse_show else R.string.collapse_hide),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    val tap = modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) { stateDescription = if (collapsed) closed else open }
        .clickable(role = Role.Button, onClick = onToggle)
    if (collapsed) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (color != Color.Unspecified) LocalContentColor.current.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = tap,
        ) { row(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)) }
    } else {
        row(tap.padding(vertical = 6.dp))
    }
}
