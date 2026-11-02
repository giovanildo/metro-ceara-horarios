package io.github.giova.metroceara.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.FareGuide
import io.github.giova.metroceara.data.FareTopic
import io.github.giova.metroceara.data.Regras
import io.github.giova.metroceara.data.ScheduleStore
import io.github.giova.metroceara.ui.theme.routeColor

/**
 * O guia da passagem: quanto custa cada linha e, em telas próprias, a meia de
 * estudante, a gratuidade e o VaiVem Livre. É muita regra que o app não controla,
 * então a tela diz logo no topo que tudo vem do Metrofor, e cada tema termina
 * num botão para conferir na página dele.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FareGuideScreen(guide: FareGuide, fares: List<Pair<String, String>>, onBack: () -> Unit) {
    var topicIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val topic = topicIndex?.let { guide.topics.getOrNull(it) }
    val back = { if (topic != null) topicIndex = null else onBack() }
    BackHandler(enabled = topic != null) { topicIndex = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topic?.title ?: stringResource(R.string.fare_guide_title)) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        // Cada tela começa do topo, onde está a fonte (o tema não herda a rolagem da lista).
        val scroll = key(topicIndex) { rememberScrollState() }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp),
        ) {
            if (topic == null) {
                FareOverview(guide, fares, onTopic = { topicIndex = it })
            } else {
                FareTopicDetail(guide, topic)
            }
        }
    }
}

@Composable
private fun FareOverview(guide: FareGuide, fares: List<Pair<String, String>>, onTopic: (Int) -> Unit) {
    val context = LocalContext.current
    SourceBox(guide, onOpen = guide.link?.let { { open(context, it) } })

    Column {
        fares.forEachIndexed { index, (line, text) ->
            if (index > 0) HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 10.dp),
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(routeColor(Regras.current.lineColor(line, "888888")), CircleShape),
                )
                Column {
                    Text(line, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (guide.topics.isNotEmpty()) {
        Text(
            stringResource(R.string.fare_guide_cheaper),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        guide.topics.forEachIndexed { index, topic ->
            Surface(
                onClick = { onTopic(index) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOf(topic.icon, topic.title).filter { it.isNotBlank() }.joinToString(" "),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(topic.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }

    guide.fullFare?.let { card ->
        OutlinedBox {
            Text(card.title, style = MaterialTheme.typography.titleSmall)
            Text(card.text, style = MaterialTheme.typography.bodyMedium)
            if (card.button != null && card.link != null) {
                TextButton(onClick = { open(context, card.link) }) {
                    Text(card.button)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FareTopicDetail(guide: FareGuide, topic: FareTopic) {
    val context = LocalContext.current
    Text(
        stringResource(R.string.fare_guide_source_line, guide.checked),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
    // Prazos com data, como a renovação anual do cartão de estudante.
    topic.noticesOn(ScheduleStore.today()).forEach {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
        }
    }
    topic.intro?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
    topic.sections.forEach { section ->
        Text(
            section.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        section.boxes.forEach { (title, text) ->
            OutlinedBox {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        section.items.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
    }
    topic.alert?.let {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
        }
    }
    topic.note?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    (topic.link ?: guide.link)?.let { link ->
        OutlinedButton(onClick = { open(context, link) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.fare_guide_check_site), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** O topo da tela Passagem: de onde vêm as informações, com o link para o Metrofor. */
@Composable
private fun SourceBox(guide: FareGuide, onOpen: (() -> Unit)?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.fare_guide_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(guide.source, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.fare_guide_checked, guide.checked), style = MaterialTheme.typography.bodySmall)
            if (onOpen != null) {
                Text(
                    stringResource(R.string.fare_guide_open_site) + " ›",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpen).padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OutlinedBox(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) { content() }
}

private fun open(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
