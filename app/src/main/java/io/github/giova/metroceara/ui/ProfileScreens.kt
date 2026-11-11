package io.github.giova.metroceara.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.HomeBlock
import io.github.giova.metroceara.data.HomeLayout
import io.github.giova.metroceara.data.HomeStyle
import io.github.giova.metroceara.data.Place
import io.github.giova.metroceara.data.RideFrequency

private const val QUESTIONS = 6

private val RideFrequency.title: Int get() = when (this) {
    RideFrequency.DAILY -> R.string.profile_daily
    RideFrequency.SOMETIMES -> R.string.profile_sometimes
    RideFrequency.VISITOR -> R.string.profile_visitor
}

private val RideFrequency.description: Int get() = when (this) {
    RideFrequency.DAILY -> R.string.profile_daily_desc
    RideFrequency.SOMETIMES -> R.string.profile_sometimes_desc
    RideFrequency.VISITOR -> R.string.profile_visitor_desc
}

private val RideFrequency.emoji: String get() = when (this) {
    RideFrequency.DAILY -> "📅"
    RideFrequency.SOMETIMES -> "🔁"
    RideFrequency.VISITOR -> "🧳"
}

/**
 * "Conte sobre você": a apresentação e as 6 perguntas, uma por tela. Cada resposta
 * já fica gravada; "Pular" (em qualquer tela) fecha sem mudar o resto.
 */
@Composable
fun ProfileQuestionsScreen(
    layout: HomeLayout,
    homeName: String?,
    workName: String?,
    onChange: (HomeLayout) -> Unit,
    onPickPlace: (Place) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    // 0 = apresentação; 1 a 6 = as perguntas.
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Sim/Não só aparece marcado depois de a pessoa tocar: o padrão do app não é resposta.
    var commute by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var bike by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var halfFare by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var access by rememberSaveable { mutableStateOf<Boolean?>(null) }

    BackHandler { if (step > 0) step-- else onSkip() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                if (step == 0) {
                    Spacer(Modifier.height(48.dp))
                    Text("🙋", style = MaterialTheme.typography.displayMedium)
                    Text(
                        stringResource(R.string.profile_intro_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    Text(stringResource(R.string.profile_intro_text), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.profile_intro_later),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        stringResource(R.string.adjust_private),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { step / QUESTIONS.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        stringResource(R.string.profile_step, step, QUESTIONS),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    when (step) {
                        1 -> {
                            Question(R.string.profile_q_frequency)
                            RideFrequency.entries.forEach { f ->
                                AnswerOption(f.emoji, stringResource(f.title), stringResource(f.description), layout.frequency == f) {
                                    onChange(layout.withFrequency(f))
                                }
                            }
                        }

                        2 -> {
                            Question(R.string.profile_q_commute)
                            AnswerOption("👍", stringResource(R.string.profile_yes), null, commute == true) {
                                commute = true
                                onChange(layout.withVisible(HomeBlock.PLACES, true))
                            }
                            if (commute == true) {
                                PlaceField(R.string.profile_home_station, homeName) { onPickPlace(Place.HOME) }
                                PlaceField(R.string.profile_work_station, workName) { onPickPlace(Place.WORK) }
                                Spacer(Modifier.height(10.dp))
                            }
                            AnswerOption("👎", stringResource(R.string.profile_no), null, commute == false) {
                                commute = false
                                onChange(layout.withVisible(HomeBlock.PLACES, false))
                            }
                        }

                        3 -> YesNo(R.string.profile_q_bike, null, R.string.adjust_bike_desc, bike) {
                            bike = it
                            onChange(layout.copy(bike = it))
                        }

                        4 -> YesNo(R.string.profile_q_half_fare, R.string.profile_q_half_fare_sub, R.string.adjust_half_fare_desc, halfFare) {
                            halfFare = it
                            onChange(layout.copy(halfFare = it))
                        }

                        5 -> YesNo(R.string.profile_q_access, R.string.profile_q_access_sub, R.string.adjust_access_desc, access) {
                            access = it
                            onChange(layout.copy(access = it))
                        }

                        else -> {
                            Question(R.string.profile_q_style, R.string.style_intro)
                            StylePreview(layout.style, Modifier.padding(bottom = 8.dp))
                            HomeStyle.entries.forEach { style ->
                                StyleRow(style, style == layout.style) { onChange(layout.copy(style = style)) }
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.profile_skip), fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { if (step < QUESTIONS) step++ else onFinish() }) {
                    Text(
                        stringResource(
                            when (step) {
                                0 -> R.string.profile_start
                                QUESTIONS -> R.string.profile_finish
                                else -> R.string.profile_next
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Question(title: Int, subtitle: Int? = null) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = if (subtitle == null) 16.dp else 4.dp),
    )
    subtitle?.let {
        Text(
            stringResource(it),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun YesNo(title: Int, subtitle: Int?, yesDescription: Int, answer: Boolean?, onAnswer: (Boolean) -> Unit) {
    Question(title, subtitle)
    AnswerOption("👍", stringResource(R.string.profile_yes), stringResource(yesDescription), answer == true) { onAnswer(true) }
    AnswerOption("👎", stringResource(R.string.profile_no), null, answer == false) { onAnswer(false) }
}

/** Uma resposta grande, de tocar; a marcada fica com a cor do app. */
@Composable
private fun AnswerOption(emoji: String, title: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.primaryContainer else colors.background,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(40.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun PlaceField(label: Int, value: String?, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(start = 40.dp, bottom = 8.dp).clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value ?: stringResource(R.string.commute_choose), style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun StyleRow(style: HomeStyle, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(stringResource(style.title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(style.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider()
}

/**
 * O resumo das respostas: no fim das perguntas e, depois, pelo Mais. Tudo muda
 * aqui mesmo; o visual e a ordem abrem as telas deles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    layout: HomeLayout,
    homeName: String?,
    workName: String?,
    /** Veio do fim das perguntas: título de "Pronto!" e o botão para a tela inicial. */
    justFinished: Boolean,
    onChange: (HomeLayout) -> Unit,
    onPickPlace: (Place) -> Unit,
    onOpenStyle: () -> Unit,
    onOpenOrder: () -> Unit,
    onBack: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (justFinished) {
                Text("✅", style = MaterialTheme.typography.displaySmall)
                Text(
                    stringResource(R.string.profile_done_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.profile_done_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            val notAnswered = stringResource(R.string.profile_not_answered)
            ChangeRow(
                emoji = layout.frequency?.emoji ?: "📅",
                title = stringResource(R.string.profile_frequency_row),
                value = layout.frequency?.let { stringResource(it.title) } ?: notAnswered,
            ) { picking = "frequency" }
            AdjustSwitch("💼", R.string.adjust_commute, R.string.adjust_commute_desc, layout.commute) {
                onChange(layout.withVisible(HomeBlock.PLACES, it))
            }
            if (layout.commute) {
                PlaceField(R.string.profile_home_station, homeName) { onPickPlace(Place.HOME) }
                PlaceField(R.string.profile_work_station, workName) { onPickPlace(Place.WORK) }
                HorizontalDivider(Modifier.padding(top = 4.dp))
            }
            AdjustSwitch("🚲", R.string.adjust_bike, R.string.adjust_bike_desc, layout.bike) { onChange(layout.copy(bike = it)) }
            AdjustSwitch("🎫", R.string.adjust_half_fare, R.string.adjust_half_fare_desc, layout.halfFare) {
                onChange(layout.copy(halfFare = it))
            }
            AdjustSwitch("♿", R.string.adjust_access, R.string.adjust_access_desc, layout.access) { onChange(layout.copy(access = it)) }
            ChangeRow("🎨", stringResource(R.string.profile_style_row), stringResource(layout.style.title), onClick = onOpenStyle)
            ChangeRow("↕", stringResource(R.string.adjust_order), stringResource(R.string.adjust_order_desc), arrow = true, onClick = onOpenOrder)
            Text(
                text = stringResource(R.string.adjust_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
            if (justFinished) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    Button(onClick = onBack) { Text(stringResource(R.string.profile_see_times)) }
                }
            }
        }
    }

    when (picking) {
        "frequency" -> ChoiceDialog(
            title = stringResource(R.string.profile_q_frequency),
            options = RideFrequency.entries.map { stringResource(it.title) },
            selected = RideFrequency.entries.indexOf(layout.frequency),
            onPick = { onChange(layout.withFrequency(RideFrequency.entries[it])) },
            onDismiss = { picking = null },
        )

    }
}

@Composable
private fun ChangeRow(emoji: String, title: String, value: String, arrow: Boolean = false, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (arrow) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Text(stringResource(R.string.profile_change), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider()
}

@Composable
private fun ChoiceDialog(title: String, options: List<String>, selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEachIndexed { i, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = i == selected, role = Role.RadioButton) {
                                onPick(i)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                    ) {
                        RadioButton(selected = i == selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
