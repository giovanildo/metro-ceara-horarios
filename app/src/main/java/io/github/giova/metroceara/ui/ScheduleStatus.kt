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
import io.github.giova.metroceara.data.DayType
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleSource
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.isSundayWithoutService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Diz de onde vêm os horários na tela. A grade de um dia útil comum é igual à
 * do dia útil anterior: a de outro dia do mesmo tipo só preocupa (e só aí vira
 * aviso) quando o Metrofor anunciou operação especial para hoje. Num domingo
 * sem operação, a tela mostra a grade de segunda.
 */
@Composable
fun scheduleStatusText(source: ScheduleSource, syncing: Boolean): String = when {
    syncing -> stringResource(R.string.schedule_syncing)
    isSundayWithoutService() -> stringResource(R.string.schedule_day_type, stringResource(R.string.day_type_weekday))
    source.kind == ScheduleSource.Kind.TODAY ->
        stringResource(R.string.schedule_today, formatClock(source.syncedAt))
    source.kind == ScheduleSource.Kind.SAME_DAY_TYPE && specialToday() -> stringResource(R.string.schedule_special_missing)
    source.kind == ScheduleSource.Kind.SAME_DAY_TYPE -> stringResource(
        R.string.schedule_day_type,
        stringResource(
            when (ScheduleStore.todayType()) {
                DayType.SATURDAY -> R.string.day_type_saturday
                DayType.SUNDAY -> R.string.day_type_sunday
                DayType.WEEKDAY -> R.string.day_type_weekday
            },
        ),
    )
    else -> stringResource(R.string.schedule_bundled)
}

/** Em vermelho só o que pede atenção: a grade do APK, ou dia especial sem a grade do dia. */
private fun scheduleStatusIsWarning(source: ScheduleSource): Boolean = !isSundayWithoutService() && when (source.kind) {
    ScheduleSource.Kind.TODAY -> false
    ScheduleSource.Kind.SAME_DAY_TYPE -> specialToday()
    else -> true
}

private fun specialToday(): Boolean = Regras.current.operations[ScheduleStore.today()] != null

/**
 * Linha de status dos horários. Sem botão de baixar de novo: o app baixa a
 * grade do dia sozinho, toda vez que é aberto.
 */
@Composable
fun ScheduleStatus(
    source: ScheduleSource,
    syncing: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = scheduleStatusText(source, syncing),
            style = MaterialTheme.typography.bodySmall,
            color = if (syncing || !scheduleStatusIsWarning(source)) {
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

internal fun formatClock(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }
        .format(Date(millis))

/**
 * A notícia do Metrofor que explica por que hoje (ou amanhã) o horário é outro
 * (feriado, ENEM, eleição): a manchete, com a fonte e o dia na frente. Tocar
 * abre a notícia no navegador, no site do Metrofor.
 */
@Composable
fun MetroforNotices(notices: List<ScheduleStore.Notice>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    notices.forEach { notice ->
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(stringResource(if (notice.tomorrow) R.string.notice_tomorrow else R.string.notice_today))
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
