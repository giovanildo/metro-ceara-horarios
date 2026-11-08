package io.github.giova.metroceara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DirectionsSubway
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.HomeBlock
import io.github.giova.metroceara.data.Route
import io.github.giova.metroceara.ui.theme.routeColor

/** Os botões da barra de baixo (Início, Planeje sua viagem, Linhas); Mais não é aba, abre a lista por cima. */
enum class HomeTab { HOME, ROUTES, LINES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    routes: List<Route>,
    stationCount: (Route) -> Int,
    onRouteClick: (Route) -> Unit,
    /** A aba aberta; fica com quem chama para sobreviver à ida e volta de uma linha. */
    tab: HomeTab = HomeTab.HOME,
    onTab: (HomeTab) -> Unit = {},
    /** Os blocos da tela inicial que aparecem, na ordem da pessoa. */
    blocks: List<HomeBlock> = emptyList(),
    /** Os blocos da aba "Planeje sua viagem": "Para onde você vai?" e Trabalho e Casa. */
    routeBlocks: List<HomeBlock> = emptyList(),
    /** Desenha um bloco. */
    block: @Composable (HomeBlock) -> Unit = {},
    /** Por cima de tudo: o modo viagem em andamento e o aviso de horários antigos. */
    header: (@Composable () -> Unit)? = null,
    /** O que a lista do Mais abre. */
    menu: HomeMenuActions = HomeMenuActions(),
) {
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    if (moreOpen) HomeMoreSheet(actions = menu, onDismiss = { moreOpen = false })
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val (title, subtitle) = when (tab) {
                        HomeTab.HOME -> R.string.app_name to R.string.subtitle
                        HomeTab.ROUTES -> R.string.routes_title to R.string.routes_subtitle
                        HomeTab.LINES -> R.string.routes_lines to R.string.lines_subtitle
                    }
                    Column {
                        Text(stringResource(title))
                        Text(
                            text = stringResource(subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TabItem(tab == HomeTab.HOME, Icons.Outlined.Home, R.string.tab_home) { onTab(HomeTab.HOME) }
                TabItem(tab == HomeTab.ROUTES, Icons.Outlined.Explore, R.string.tab_routes) { onTab(HomeTab.ROUTES) }
                TabItem(tab == HomeTab.LINES, Icons.Outlined.DirectionsSubway, R.string.routes_lines) { onTab(HomeTab.LINES) }
                TabItem(moreOpen, Icons.Filled.Menu, R.string.tab_more) { moreOpen = true }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tab) {
                HomeTab.HOME -> {
                    header?.let { h -> item(key = "topo") { h() } }
                    blocks.forEach { b -> item(key = b.key) { block(b) } }
                    item {
                        Text(
                            text = stringResource(R.string.unofficial),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                HomeTab.ROUTES -> routeBlocks.forEach { b -> item(key = b.key) { block(b) } }
                HomeTab.LINES -> items(routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        stationCount = stationCount(route),
                        onClick = { onRouteClick(route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(selected: Boolean, icon: ImageVector, label: Int, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        // "Planeje sua viagem" quebra em duas linhas em vez de virar "Planeje…".
        label = { Text(stringResource(label), textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RouteCard(route: Route, stationCount: Int, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            // IntrinsicSize.Min dá altura ao Row para a faixa colorida poder esticar.
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Faixa com a cor oficial da linha, vinda do próprio GTFS.
            Spacer(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(routeColor(route.colorHex)),
            )
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(route.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = route.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Linha sem viagens na grade do dia (o VLT Aeroporto, no sábado).
                Text(
                    text = if (stationCount == 0) {
                        stringResource(R.string.route_not_running_today)
                    } else {
                        pluralStringResource(R.plurals.stations_count, stationCount, stationCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
