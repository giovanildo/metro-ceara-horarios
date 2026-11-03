package io.github.giova.metroceara.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleSource

/**
 * "De onde vem cada informação": os horários (baixados do Metrofor) e cada fonte
 * do regras.json, com a data da conferência e a próxima. É o lugar para mandar
 * quem duvidar de algo; e, para quem mantém o app, a lista do que reconferir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(schedule: ScheduleSource, onBack: () -> Unit) {
    val context = LocalContext.current
    val month = currentMonth()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.sources_intro), style = MaterialTheme.typography.bodyMedium)

            // Os horários mudam todo dia: a data que conta é a do download.
            Entry(
                title = stringResource(R.string.sources_schedule_title),
                line = when (schedule.kind) {
                    ScheduleSource.Kind.TODAY -> stringResource(R.string.sources_schedule_today, formatClock(schedule.syncedAt))
                    else -> stringResource(
                        R.string.sources_schedule_other,
                        schedule.date?.split("-")?.reversed()?.joinToString("/") ?: "—",
                    )
                },
                link = null,
                detail = stringResource(R.string.sources_schedule_detail),
            )
            Regras.current.sources.values.forEach { source ->
                HorizontalDivider()
                Entry(source.title, source.line(month), source.link, source.detail) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                }
            }
        }
    }
}

@Composable
private fun Entry(title: String, line: String, link: String?, detail: String? = null, onOpen: (String) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (link != null) {
            TextButton(onClick = { onOpen(link) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.sources_open))
            }
        }
    }
}
