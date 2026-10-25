package io.github.giova.metroceara.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.ScheduleSource
import io.github.giova.metroceara.data.ScheduleStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Diz de onde vêm os horários na tela: de hoje, de outro dia, ou do APK. */
@Composable
fun scheduleStatusText(source: ScheduleSource, syncing: Boolean, failed: Boolean): String = when {
    syncing -> stringResource(R.string.schedule_syncing)
    source.kind == ScheduleSource.Kind.TODAY ->
        stringResource(R.string.schedule_today, formatClock(source.syncedAt))
    source.kind == ScheduleSource.Kind.SAME_DAY_TYPE -> stringResource(
        if (failed) R.string.schedule_same_type_failed else R.string.schedule_same_type,
        source.date.orEmpty().let { if (it.length == 10) "${it.substring(8)}/${it.substring(5, 7)}" else it },
    )
    else -> stringResource(R.string.schedule_bundled)
}

/**
 * Linha de status dos horários. Sem botão de baixar de novo: o app baixa a
 * grade do dia sozinho, toda vez que é aberto.
 */
@Composable
fun ScheduleStatus(
    source: ScheduleSource,
    syncing: Boolean,
    failed: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = scheduleStatusText(source, syncing, failed),
            style = MaterialTheme.typography.bodySmall,
            color = if (source.kind == ScheduleSource.Kind.TODAY || syncing) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.weight(1f),
        )
        if (syncing) {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp).size(20.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }
        .format(Date(millis))

/**
 * Os avisos de operação do site do Metrofor (horário especial, gratuidade,
 * interrupção): a manchete, com a fonte na frente. Tocar abre a notícia no
 * navegador, no site do Metrofor.
 */
@Composable
fun MetroforNotices(notices: List<ScheduleStore.Notice>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    notices.forEach { notice ->
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(
                        if (notice.day.isEmpty()) stringResource(R.string.notice_source)
                        else stringResource(R.string.notice_source_day, notice.day),
                    )
                }
                append(" ")
                append(notice.title)
                if (notice.link != null) append(" ›")
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.then(
                notice.link?.let { link ->
                    Modifier.clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                    }
                } ?: Modifier,
            ),
        )
    }
}
