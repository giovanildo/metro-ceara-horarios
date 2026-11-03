package io.github.giova.metroceara.ui

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

/** A faixa "Ajustar para mim ›" da tela inicial, debaixo de Trabalho e Casa. */
@Composable
fun AdjustLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.adjust_home_link),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.adjust_home_link_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
    }
}

/** "Ajustar para mim": interruptores que deixam a tela inicial com a cara da pessoa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustScreen(
    layout: HomeLayout,
    onChange: (HomeLayout) -> Unit,
    onOpenOrder: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.adjust_title)) },
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
                text = stringResource(R.string.adjust_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
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
            AdjustSwitch("🧭", R.string.adjust_newcomer, R.string.adjust_newcomer_desc, layout.newcomer) {
                onChange(layout.withNewcomer(it))
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
        val position = after.order.indexOf(block) + 1
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
                layout.order.forEachIndexed { i, block ->
                    OrderRow(
                        block = block,
                        layout = layout,
                        first = i == 0,
                        last = i == layout.order.lastIndex,
                        onVisible = { onChange(layout.withVisible(block, it)) },
                        onMove = { delta, name -> move(block, delta, name) },
                    )
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
