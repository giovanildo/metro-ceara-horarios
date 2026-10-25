package io.github.giova.metroceara.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DayEnd
import io.github.giova.metroceara.data.formatTime

/** "Último de hoje: 17:08 · primeiro de segunda: 05:30"; `null` se não há nada a dizer. */
@Composable
fun dayEndText(end: DayEnd): String? {
    val parts = listOfNotNull(
        end.lastToday?.let { stringResource(R.string.day_end_last, formatTime(it)) },
        end.nextFirst?.let {
            stringResource(
                if (end.nextIsMonday) R.string.day_end_next_monday else R.string.day_end_next_tomorrow,
                formatTime(it),
            )
        },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")?.replaceFirstChar { it.uppercase() }
}
