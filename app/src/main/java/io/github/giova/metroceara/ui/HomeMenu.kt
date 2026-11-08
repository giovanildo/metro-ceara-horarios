package io.github.giova.metroceara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import kotlinx.coroutines.launch

/**
 * O que a lista do Mais abre. Item sem ação (null) não aparece: Passagem sem
 * guia no regras.json, Nina longe das linhas da Super NINA.
 */
class HomeMenuActions(
    val onFareGuide: (() -> Unit)? = null,
    /** Abre direto o WhatsApp da Super NINA. */
    val onNina: (() -> Unit)? = null,
    val onAdjust: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onSources: () -> Unit = {},
    val onAbout: () -> Unit = {},
    val onShare: () -> Unit = {},
)

/**
 * A lista do Mais (barra de baixo): sobe de baixo por cima da tela, que escurece.
 * Fecha arrastando para baixo, tocando fora ou no voltar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMoreSheet(actions: HomeMenuActions, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Fecha a lista e depois abre o destino, para a volta cair na tela inicial.
    fun go(action: () -> Unit) {
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
        action()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.tab_more),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            actions.onFareGuide?.let { MenuItem(Icons.Outlined.ConfirmationNumber, R.string.menu_fare) { go(it) } }
            actions.onNina?.let { onNina ->
                val description = stringResource(R.string.menu_nina_desc)
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_nina)) },
                    icon = {
                        // Cores próprias do desenho: sem tingir.
                        Image(
                            painter = painterResource(R.drawable.ic_nina),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    selected = false,
                    onClick = { go(onNina) },
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            MenuItem(Icons.Outlined.Tune, R.string.menu_adjust) { go(actions.onAdjust) }
            MenuItem(Icons.Outlined.Settings, R.string.settings_title) { go(actions.onSettings) }
            MenuItem(Icons.Outlined.TravelExplore, R.string.sources_title) { go(actions.onSources) }
            MenuItem(Icons.Outlined.Info, R.string.menu_about) { go(actions.onAbout) }
            MenuItem(Icons.Outlined.Share, R.string.menu_share) { go(actions.onShare) }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: Int, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(stringResource(label)) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
    )
}
