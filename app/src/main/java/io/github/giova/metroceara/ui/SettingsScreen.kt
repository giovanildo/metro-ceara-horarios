package io.github.giova.metroceara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.giova.metroceara.data.Stop
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import android.os.Build
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.AppSettings
import kotlin.math.roundToInt

/** Distâncias e tempos ajustáveis, em linguagem de passageiro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    stationGroups: List<StationGroup>,
    nearestStations: List<NearStation>,
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

            SectionTitle(R.string.settings_section_leave)
            Text(
                text = stringResource(R.string.settings_leave_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            StationSetting(
                title = R.string.settings_home_station,
                stopId = settings.homeStopId,
                groups = stationGroups,
                nearest = nearestStations,
            ) { onChange(settings.copy(homeStopId = it)) }
            SettingSlider(
                title = R.string.settings_home_minutes,
                description = R.string.settings_home_minutes_desc,
                value = settings.homeToStationMinutes, range = 1..60, step = 1, format = { "$it min" },
            ) { onChange(settings.copy(homeToStationMinutes = it)) }

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

            SectionTitle(R.string.settings_section_gps)
            SettingSlider(
                title = R.string.settings_home_gps,
                description = R.string.settings_home_gps_desc,
                value = settings.homeGpsSeconds, range = 5..60, step = 5, format = ::formatSeconds,
            ) { onChange(settings.copy(homeGpsSeconds = it)) }

            OutlinedButton(onClick = onReset, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.settings_reset))
            }
        }
    }
}

/** As estações de uma linha, na ordem do percurso, para escolher numa lista. */
data class StationGroup(val line: String, val stops: List<Stop>)

/** Uma estação perto de onde a pessoa está agora. */
data class NearStation(val stopId: String, val name: String, val lines: String, val meters: Int)

/** Escolha de uma estação: mostra a atual e abre a lista (as mais perto no topo). */
@Composable
private fun StationSetting(
    title: Int,
    stopId: String,
    groups: List<StationGroup>,
    nearest: List<NearStation>,
    onPick: (String) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val current = groups.firstNotNullOfOrNull { g -> g.stops.firstOrNull { it.id == stopId }?.name }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { picking = true }
            .padding(top = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = current ?: stringResource(R.string.settings_station_none),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(title)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    item {
                        StationOption(stringResource(R.string.settings_station_none), null) {
                            onPick(""); picking = false
                        }
                    }
                    if (nearest.isNotEmpty()) {
                        item { GroupHeader(stringResource(R.string.settings_station_nearest)) }
                        items(nearest) { near ->
                            StationOption(near.name, "${near.lines} · ${formatMeters(near.meters)}") {
                                onPick(near.stopId); picking = false
                            }
                        }
                    }
                    groups.forEach { group ->
                        item { GroupHeader(group.line) }
                        items(group.stops) { stop ->
                            StationOption(stop.name, null) { onPick(stop.id); picking = false }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun StationOption(name: String, detail: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
