package io.github.giova.metroceara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    val onRoutines: () -> Unit = {},
    val onCommute: () -> Unit = {},
    val onProfile: () -> Unit = {},
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
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
            // Passagem e Nina podem faltar: sem nenhuma, o título não fica sozinho.
            if (actions.onFareGuide != null || actions.onNina != null) MenuGroup(R.string.menu_group_trip)
            actions.onFareGuide?.let {
                MenuRow(Icons.Outlined.ConfirmationNumber, R.string.menu_fare, R.string.menu_fare_sub) { go(it) }
            }
            actions.onNina?.let { onNina ->
                // Só o nome, de propósito: a explicação fica para quem usa leitor de tela.
                val description = stringResource(R.string.menu_nina_desc)
                MenuRowContent(
                    icon = {
                        // Cores próprias do desenho: sem tingir.
                        Image(
                            painter = painterResource(R.drawable.ic_nina),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = R.string.menu_nina,
                    modifier = Modifier.semantics { contentDescription = description },
                    onClick = { go(onNina) },
                )
            }
            MenuGroup(R.string.menu_group_daily)
            MenuRow(Icons.Outlined.SwapHoriz, R.string.menu_commute, R.string.menu_commute_sub) { go(actions.onCommute) }
            MenuRow(Icons.Outlined.Repeat, R.string.routines_title, R.string.menu_routines_sub) { go(actions.onRoutines) }
            MenuGroup(R.string.menu_group_mine)
            MenuRow(Icons.Outlined.Face, R.string.menu_profile, R.string.menu_profile_sub) { go(actions.onProfile) }
            MenuRow(Icons.Outlined.Settings, R.string.settings_title, R.string.menu_settings_sub) { go(actions.onSettings) }
            MenuGroup(R.string.menu_group_app)
            MenuRow(Icons.Outlined.TravelExplore, R.string.sources_title) { go(actions.onSources) }
            MenuRow(Icons.Outlined.Info, R.string.menu_about) { go(actions.onAbout) }
            MenuRow(Icons.Outlined.Share, R.string.menu_share) { go(actions.onShare) }
        }
    }
}

@Composable
private fun MenuGroup(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun MenuRow(icon: ImageVector, title: Int, subtitle: Int? = null, onClick: () -> Unit) =
    MenuRowContent(icon = { Icon(icon, contentDescription = null) }, title = title, subtitle = subtitle, onClick = onClick)

@Composable
private fun MenuRowContent(
    icon: @Composable () -> Unit,
    title: Int,
    modifier: Modifier = Modifier,
    subtitle: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        icon()
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
