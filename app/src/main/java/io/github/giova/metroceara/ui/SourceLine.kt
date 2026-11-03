package io.github.giova.metroceara.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.data.YearMonth

/** O mês de hoje, para saber se a próxima conferência de uma fonte já passou. */
fun currentMonth(): YearMonth = YearMonth.parse(ScheduleStore.today()) ?: YearMonth(2026, 1)

/**
 * O link curto embaixo de uma informação ("Fonte ›"); tocar abre a janela com de
 * onde ela vem e até quando foi conferida
 * ([io.github.giova.metroceara.data.InfoSource]). Sem a fonte [id] no
 * regras.json, não mostra nada.
 */
@Composable
fun SourceLine(id: String?, modifier: Modifier = Modifier) {
    val source = Regras.current.source(id) ?: return
    val month = currentMonth()
    SourceLink(source.label(month), source.title, source.facts(month), source.detail, source.link, modifier)
}

/**
 * Um link de fonte com conteúdo pronto (a regra de bike do dia, que vem da
 * notícia do dia): [label] na tela; [title], [facts] e [detail] na janela.
 */
@Composable
fun SourceLink(
    label: String,
    title: String,
    facts: List<String>,
    detail: String?,
    link: String?,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Text(
        text = "$label ›",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clickable { open = true }
            .padding(vertical = 4.dp),
    )
    if (open) SourceDialog(title, facts, detail, link, onDismiss = { open = false })
}

@Composable
private fun SourceDialog(title: String, facts: List<String>, detail: String?, link: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                facts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (link != null) {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                    onDismiss()
                }) { Text(stringResource(R.string.sources_open)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.sources_close)) } },
    )
}
