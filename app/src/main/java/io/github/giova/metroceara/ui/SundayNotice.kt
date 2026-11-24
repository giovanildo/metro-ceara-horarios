package io.github.giova.metroceara.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.MondayReturn
import io.github.giova.metroceara.data.formatTime

/**
 * O aviso de domingo sem operação, com o que se sabe da segunda ([monday]). Nenhum
 * horário aparece no domingo comum: hora na tela dá a impressão de que hoje tem metrô.
 */
fun sundayOffText(context: Context, monday: MondayReturn): String = when (monday) {
    is MondayReturn.At -> context.getString(R.string.sunday_off_back, formatTime(monday.minutes))
    MondayReturn.Stopped -> context.getString(R.string.sunday_off_monday_stopped)
    MondayReturn.Special -> context.getString(R.string.sunday_off_monday_special)
    MondayReturn.Unknown -> context.getString(R.string.sunday_warning)
}

/**
 * Domingo sem operação, na tela inicial: o dia em que não há horário para mostrar e a
 * pessoa não tem pressa. Um convite discreto para passar o app a quem pega metrô na semana.
 * Só aparece nesse dia; nos outros, a tela inicial é só do horário.
 */
@Composable
fun SundayShareCard(onShare: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.sunday_share_text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            TextButton(onClick = onShare, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.menu_share), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** Aviso de domingo (ou de linha parada, com outro [text]). */
@Composable
fun SundayNotice(modifier: Modifier = Modifier, text: String = stringResource(R.string.sunday_warning)) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
