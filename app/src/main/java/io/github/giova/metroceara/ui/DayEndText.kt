package io.github.giova.metroceara.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DayEnd
import io.github.giova.metroceara.data.formatTime

/**
 * "Último de hoje: 17:08 · primeiro de segunda: 05:30" ou, com domingo de ENEM,
 * "Último de hoje: 17:08 · amanhã (domingo): das 10:30 às 19:10, aproximado · segunda: 05:30";
 * `null` se não há nada a dizer.
 */
@Composable
fun dayEndText(end: DayEnd): String? {
    val parts = listOfNotNull(
        end.lastToday?.let { stringResource(R.string.day_end_last, formatTime(it)) },
        if (end.sundaySpecial) {
            end.sundayHours?.let { (start, last) -> stringResource(R.string.day_end_sunday_hours, formatTime(start), formatTime(last)) }
                ?: stringResource(R.string.day_end_sunday_special)
        } else {
            null
        },
        // Com o domingo especial na frente, a segunda vai sem o "(domingo, só em operação especial)".
        end.nextFirst?.let {
            stringResource(
                when {
                    end.sundaySpecial -> R.string.day_end_next_monday_plain
                    end.nextIsMonday -> R.string.day_end_next_monday
                    else -> R.string.day_end_next_tomorrow
                },
                formatTime(it),
            )
        } ?: if (end.nextSpecial) {
            stringResource(if (end.nextIsMonday) R.string.day_end_special_monday else R.string.day_end_special_tomorrow)
        } else {
            null
        },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")?.replaceFirstChar { it.uppercase() }
}
