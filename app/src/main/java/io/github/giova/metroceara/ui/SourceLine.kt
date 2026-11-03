package io.github.giova.metroceara.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.YearMonth

/** O mês de hoje, para saber se a próxima conferência de uma fonte já passou. */
fun currentMonth(): YearMonth = YearMonth.parse(ScheduleStore.today()) ?: YearMonth(2026, 1)

/**
 * A linha pequena embaixo de uma informação dizendo de onde ela vem e até quando
 * foi conferida ([io.github.giova.metroceara.data.InfoSource.line]). Com link,
 * tocar abre a fonte. Sem a fonte [id] no regras.json, não mostra nada.
 */
@Composable
fun SourceLine(id: String?, modifier: Modifier = Modifier) {
    val source = Regras.current.source(id) ?: return
    SourceText(source.line(currentMonth()), source.link, modifier)
}

/** Uma linha de fonte com texto pronto (a regra de bike do dia, que vem da notícia). */
@Composable
fun SourceText(text: String, link: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Text(
        text = if (link != null) "$text ›" else text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .then(
                if (link != null) {
                    Modifier.clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                    }
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp),
    )
}
