package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChip
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.AppSettings
import io.github.giova.metroceara.data.Mobility
import io.github.giova.metroceara.data.TripLog
import kotlin.math.roundToInt

/** Distâncias e tempos ajustáveis, em linguagem de passageiro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle(R.string.settings_section_appearance)
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                listOf(
                    AppSettings.THEME_AUTO to R.string.settings_theme_auto,
                    AppSettings.THEME_LIGHT to R.string.settings_theme_light,
                    AppSettings.THEME_DARK to R.string.settings_theme_dark,
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onChange(settings.copy(themeMode = mode)) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_wallpaper), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(R.string.settings_wallpaper_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.wallpaperColors,
                        onCheckedChange = { onChange(settings.copy(wallpaperColors = it)) },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))

            SectionTitle(R.string.settings_section_distances)
            SettingSlider(
                title = R.string.settings_nearby_lines,
                description = R.string.settings_nearby_lines_desc,
                value = settings.nearbyLinesMeters, range = 500..5_000, step = 500, format = ::formatMeters,
            ) { onChange(settings.copy(nearbyLinesMeters = it)) }
            SettingSlider(
                title = R.string.settings_bike_you,
                description = R.string.settings_bike_you_desc,
                value = settings.bikeNearYouMeters, range = 500..5_000, step = 500, format = ::formatMeters,
            ) { onChange(settings.copy(bikeNearYouMeters = it)) }
            SettingSlider(
                title = R.string.settings_bike_station,
                description = R.string.settings_bike_station_desc,
                value = settings.bikeNearStationMeters, range = 200..1_500, step = 100, format = ::formatMeters,
            ) { onChange(settings.copy(bikeNearStationMeters = it)) }

            SectionTitle(R.string.settings_section_trip)
            SettingSwitch(
                title = R.string.settings_belongings,
                description = R.string.settings_belongings_desc,
                checked = settings.remindBelongings,
            ) { onChange(settings.copy(remindBelongings = it)) }
            SettingSlider(
                title = R.string.settings_alert_before,
                description = R.string.settings_alert_before_desc,
                value = settings.alertStationsBefore, range = 1..4, step = 1,
                format = { if (it == 1) "1 estação" else "$it estações" },
            ) { onChange(settings.copy(alertStationsBefore = it)) }
            SettingSlider(
                title = R.string.settings_seated,
                description = R.string.settings_seated_desc,
                value = settings.seatedMaxStations, range = 0..5, step = 1,
                format = { when (it) { 0 -> "não oferecer"; 1 -> "1 estação"; else -> "$it estações" } },
            ) { onChange(settings.copy(seatedMaxStations = it)) }
            SettingSlider(
                title = R.string.settings_trip_warn,
                description = R.string.settings_trip_warn_desc,
                value = settings.tripWarnMeters, range = 500..3_000, step = 250, format = ::formatMeters,
            ) {
                // O aviso nunca passa do limite em que o modo viagem some.
                onChange(settings.copy(tripWarnMeters = it, tripMaxMeters = maxOf(settings.tripMaxMeters, it)))
            }
            SettingSlider(
                title = R.string.settings_trip_max,
                description = R.string.settings_trip_max_desc,
                value = settings.tripMaxMeters, range = 1_000..10_000, step = 1_000, format = ::formatMeters,
            ) { onChange(settings.copy(tripMaxMeters = it, tripWarnMeters = minOf(settings.tripWarnMeters, it))) }
            SettingSlider(
                title = R.string.settings_gps_lost,
                description = R.string.settings_gps_lost_desc,
                value = settings.gpsLostSeconds, range = 30..300, step = 30, format = ::formatSeconds,
            ) { onChange(settings.copy(gpsLostSeconds = it)) }
            SettingSlider(
                title = R.string.settings_late_boarding,
                description = R.string.settings_late_boarding_desc,
                value = settings.lateBoardingMinutes, range = 0..10, step = 1, format = { "$it min" },
            ) { onChange(settings.copy(lateBoardingMinutes = it)) }
            SettingSlider(
                title = R.string.settings_give_up,
                description = R.string.settings_give_up_desc,
                value = settings.tripGiveUpMinutes, range = 10..60, step = 5, format = { "$it min" },
            ) { onChange(settings.copy(tripGiveUpMinutes = it)) }

            SectionTitle(R.string.settings_section_transfer)
            Text(
                text = stringResource(R.string.settings_mobility),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.settings_mobility_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                Mobility.WALKING to (R.string.mobility_walking to R.string.mobility_walking_desc),
                Mobility.RELAXED to (R.string.mobility_relaxed to R.string.mobility_relaxed_desc),
                Mobility.BIKE to (R.string.mobility_bike to R.string.mobility_bike_desc),
                Mobility.WHEELCHAIR to (R.string.mobility_wheelchair to R.string.mobility_wheelchair_desc),
            ).forEach { (mobility, texts) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(settings.copy(mobility = mobility)) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = settings.mobility == mobility, onClick = { onChange(settings.copy(mobility = mobility)) })
                    Column {
                        Text(stringResource(texts.first), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(texts.second),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            SettingSlider(
                title = R.string.settings_transfer_extra,
                description = R.string.settings_transfer_extra_desc,
                value = settings.transferExtraMinutes, range = 0..10, step = 1, format = { "$it min" },
            ) { onChange(settings.copy(transferExtraMinutes = it)) }

            SectionTitle(R.string.settings_section_gps)
            SettingSlider(
                title = R.string.settings_home_gps,
                description = R.string.settings_home_gps_desc,
                value = settings.homeGpsSeconds, range = 5..60, step = 5, format = ::formatSeconds,
            ) { onChange(settings.copy(homeGpsSeconds = it)) }

            TripLogSection()

            OutlinedButton(onClick = onReset, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.settings_reset))
            }
        }
    }
}

/**
 * O registro das viagens ([TripLog]): ligar e desligar, mandar por e-mail (a
 * pessoa vê o e-mail inteiro antes de enviar) e apagar.
 */
@Composable
private fun TripLogSection() {
    val context = LocalContext.current
    val on by TripLog.enabled.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val count = remember(refresh, on) { TripLog.trips().size }
    SectionTitle(R.string.settings_section_trip_log)
    SettingSwitch(
        title = R.string.settings_trip_log,
        description = R.string.settings_trip_log_desc,
        checked = on,
    ) { TripLog.setEnabled(context, it) }
    Text(
        // Em português o plural trata 0 como singular ("0 viagem"): o zero tem texto próprio.
        text = if (count == 0) {
            stringResource(R.string.settings_trip_log_none)
        } else {
            pluralStringResource(R.plurals.settings_trip_log_count, count, count)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            enabled = count > 0,
            onClick = { TripLog.emailIntent(context)?.let { runCatching { context.startActivity(it) } } },
        ) { Text(stringResource(R.string.settings_trip_log_send)) }
        OutlinedButton(
            enabled = count > 0,
            onClick = {
                TripLog.clear()
                refresh++
            },
        ) { Text(stringResource(R.string.settings_trip_log_clear)) }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp),
    )
}

/** Um ajuste de ligar e desligar, com explicação curta. */
@Composable
private fun SettingSwitch(title: Int, description: Int, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.padding(start = 12.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
}

/** Um ajuste: nome, explicação curta e um controle deslizante com o valor atual. */
@Composable
private fun SettingSlider(
    title: Int,
    description: Int,
    value: Int,
    range: IntRange,
    step: Int,
    format: (Int) -> String,
    onValue: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = (((raw - range.first) / step).roundToInt() * step + range.first)
                    .coerceIn(range.first, range.last)
                if (snapped != value) onValue(snapped)
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first) / step - 1,
        )
        HorizontalDivider()
    }
}

private fun formatMeters(meters: Int): String =
    if (meters < 1_000) "$meters m" else String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
        .replace(".0 km", " km").replace('.', ',')

private fun formatSeconds(seconds: Int): String = when {
    seconds < 60 -> "$seconds s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60} min ${seconds % 60} s"
}
