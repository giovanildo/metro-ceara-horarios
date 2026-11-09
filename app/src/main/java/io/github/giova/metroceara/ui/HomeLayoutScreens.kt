package io.github.giova.metroceara.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.HomeStyle
import io.github.giova.metroceara.data.RecentTrip
import io.github.giova.metroceara.data.Station
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.HomeBlock
import io.github.giova.metroceara.data.HomeLayout
import kotlinx.coroutines.launch

/** O nome do bloco, como a pessoa vê na lista da ordem. */
@Composable
fun homeBlockName(block: HomeBlock): String = stringResource(
    when (block) {
        HomeBlock.PLACES -> R.string.block_places
        HomeBlock.STATIONS -> R.string.block_stations
        HomeBlock.NOTICES -> R.string.block_notices
        HomeBlock.SEARCH -> R.string.block_search
        HomeBlock.ROUTINES -> R.string.block_routines
        HomeBlock.BIKE -> R.string.block_bike
        HomeBlock.LINES -> R.string.block_lines
    },
)

/** "Sua tela inicial": o jeito dos horários, o que a pessoa usa e a ordem, num lugar só. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSetupScreen(
    layout: HomeLayout,
    onChange: (HomeLayout) -> Unit,
    onOpenStyle: () -> Unit,
    onOpenOrder: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_home_setup)) },
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
            SetupTitle(R.string.style_title)
            Card(
                onClick = onOpenStyle,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.padding(end = 14.dp))
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(stringResource(layout.style.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(layout.style.description), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(stringResource(R.string.style_change) + " ›", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            SetupTitle(R.string.adjust_section, Modifier.padding(top = 16.dp))
            AdjustSwitch("💼", R.string.adjust_commute, R.string.adjust_commute_desc, layout.commute) {
                onChange(layout.withVisible(HomeBlock.PLACES, it))
            }
            AdjustSwitch("🚲", R.string.adjust_bike, R.string.adjust_bike_desc, layout.bike) {
                onChange(layout.copy(bike = it))
            }
            AdjustSwitch("🎫", R.string.adjust_half_fare, R.string.adjust_half_fare_desc, layout.halfFare) {
                onChange(layout.copy(halfFare = it))
            }
            AdjustSwitch("♿", R.string.adjust_access, R.string.adjust_access_desc, layout.access) {
                onChange(layout.copy(access = it))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenOrder)
                    .padding(vertical = 14.dp),
            ) {
                Text("↕", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(36.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.adjust_order), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.adjust_order_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Text(
                text = stringResource(R.string.adjust_private),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun SetupTitle(text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun AdjustSwitch(emoji: String, title: Int, description: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
    HorizontalDivider()
}

/**
 * "Ordem da tela inicial": a prévia em cima, na largura toda, e embaixo cada
 * bloco com liga/desliga e ↑ ↓. Setas em vez de arrastar: um toque cada, e o
 * TalkBack lê "Subir Trabalho e Casa".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOrderScreen(
    layout: HomeLayout,
    onChange: (HomeLayout) -> Unit,
    onBack: () -> Unit,
    /** A tela inicial de verdade, montada com o [HomeLayout] dado. */
    preview: @Composable (HomeLayout) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val undo = stringResource(R.string.order_undo)

    fun move(block: HomeBlock, delta: Int, name: String) {
        val before = layout
        val after = layout.moved(block, delta)
        if (after == before) return
        onChange(after)
        val position = after.homeOrder.indexOf(block) + 1
        val message = if (position == 1) context.getString(R.string.order_moved_first, name)
        else context.getString(R.string.order_moved, name, position)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onChange(before)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.adjust_order)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            HomePreview(layout, preview)
            Text(
                text = stringResource(R.string.order_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            // Com letra grande a lista rola e a prévia fica parada em cima.
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                layout.homeOrder.let { home ->
                home.forEachIndexed { i, block ->
                    OrderRow(
                        block = block,
                        layout = layout,
                        first = i == 0,
                        last = i == home.lastIndex,
                        onVisible = { onChange(layout.withVisible(block, it)) },
                        onMove = { delta, name -> move(block, delta, name) },
                    )
                }
                }
                TextButton(
                    onClick = { confirmReset = true },
                    enabled = !layout.isDefaultOrder,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
                ) { Text("↺ " + stringResource(R.string.order_reset), fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.order_reset_title)) },
            text = { Text(stringResource(R.string.order_reset_text)) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(layout.resetOrder())
                    confirmReset = false
                }) { Text(stringResource(R.string.order_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** O começo da tela inicial, um pouco menor e sem receber toques. */
@Composable
private fun HomePreview(layout: HomeLayout, preview: @Composable (HomeLayout) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clearAndSetSemantics { },
    ) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .graphicsLayer {
                    scaleX = 0.9f
                    scaleY = 0.9f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .padding(12.dp),
        ) {
            preview(layout)
        }
        // Por cima: segura os toques, para a prévia não abrir nada.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                },
        )
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Text(
                text = stringResource(R.string.order_preview),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun OrderRow(
    block: HomeBlock,
    layout: HomeLayout,
    first: Boolean,
    last: Boolean,
    onVisible: (Boolean) -> Unit,
    onMove: (Int, String) -> Unit,
) {
    val name = homeBlockName(block)
    val bikeOff = block == HomeBlock.BIKE && !layout.bike
    val shown = layout.shows(block)
    val sub = when {
        block.fixed -> stringResource(R.string.block_stations_sub)
        block == HomeBlock.NOTICES -> stringResource(R.string.block_notices_sub)
        block == HomeBlock.BIKE && bikeOff -> stringResource(R.string.block_bike_sub)
        else -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
            if (block.fixed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.order_fixed), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Switch(checked = shown, enabled = !bikeOff, onCheckedChange = onVisible)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (shown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sub?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedIconButton(onClick = { onMove(-1, name) }, enabled = !first, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = stringResource(R.string.order_up, name), modifier = Modifier.size(18.dp))
        }
        OutlinedIconButton(onClick = { onMove(1, name) }, enabled = !last, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = stringResource(R.string.order_down, name), modifier = Modifier.size(18.dp))
        }
    }
    HorizontalDivider()
}

private val HomeStyle.title: Int get() = when (this) {
    HomeStyle.RAIL -> R.string.style_rail
    HomeStyle.TRIP_FIRST -> R.string.style_trip
    HomeStyle.SIDES -> R.string.style_sides
    HomeStyle.ASK_FIRST -> R.string.style_ask
    HomeStyle.PHRASES -> R.string.style_phrases
    HomeStyle.CLASSIC -> R.string.style_classic
}

private val HomeStyle.description: Int get() = when (this) {
    HomeStyle.RAIL -> R.string.style_rail_desc
    HomeStyle.TRIP_FIRST -> R.string.style_trip_desc
    HomeStyle.SIDES -> R.string.style_sides_desc
    HomeStyle.ASK_FIRST -> R.string.style_ask_desc
    HomeStyle.PHRASES -> R.string.style_phrases_desc
    HomeStyle.CLASSIC -> R.string.style_classic_desc
}

/** "Como os horários aparecem": a prévia em cima, com um exemplo, e os jeitos embaixo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeStyleScreen(layout: HomeLayout, onChange: (HomeLayout) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.style_title)) },
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
            Text(
                text = stringResource(R.string.style_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(stringResource(R.string.style_preview), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            StylePreview(layout.style, Modifier.padding(top = 6.dp, bottom = 12.dp))
            HomeStyle.entries.forEach { style ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = style == layout.style, role = Role.RadioButton) { onChange(layout.copy(style = style)) }
                        .padding(vertical = 10.dp),
                ) {
                    RadioButton(selected = style == layout.style, onClick = null, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(stringResource(style.title), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(style.description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

/** Um exemplo fixo, para ver o jeito mesmo sem localização ou sem viagem guardada. */
@Composable
private fun StylePreview(style: HomeStyle, modifier: Modifier = Modifier) {
    val now = 480
    val station = Station(stopId = "exemplo", stopName = "Benfica", routeId = "exemplo", routeName = "Linha Sul", routeColor = "CE0F2E", lat = 0.0, lon = 0.0)
    val trip = StationTrip(
        row = RecentRow(RecentTrip("exemplo", "exemplo", "fim", "exemplo"), "Benfica → Parangaba", "Parangaba", listOf("Linha Sul" to "CE0F2E"), walk = false),
        times = (now + 5) to (now + 13),
        fromStation = true,
        directionId = 0,
    )
    val departures = listOf(
        HomeDeparture("Carlito Benevides", 0, Departure(now + 5, tomorrow = false), via = "Padre Cícero"),
        HomeDeparture("Chico da Silva", 1, Departure(now + 4, tomorrow = false), via = "São Benedito"),
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(station.stopName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(station.routeName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            StyledDepartures(StationStyle(style = style, trips = listOf(trip)), station, stringResource(R.string.station_here), departures, now)
        }
    }
}
