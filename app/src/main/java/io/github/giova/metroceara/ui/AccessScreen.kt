package io.github.giova.metroceara.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.AccessWay
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.StationAccess

private const val REPORT_WHATSAPP = "5585997619295"
private const val REPORT_EMAIL = "giovanildos@gmail.com"

/** "2 elevadores e escada rolante", "Acesso por rampa": o resumo curto da tela da estação. */
fun accessSummary(context: Context, access: StationAccess): String? = when (access.way) {
    AccessWay.ELEVATOR -> context.resources.getQuantityString(R.plurals.access_elevators, access.elevators, access.elevators)
        .let { if (access.escalators > 0) context.getString(R.string.access_summary_escalator, it) else it }
    AccessWay.RAMP -> context.getString(R.string.access_summary_ramp)
    AccessWay.LIFT, AccessWay.STAIRS_ONLY -> null
}

/**
 * A acessibilidade na tela da estação. Com elevador ou rampa, uma linha curta;
 * sem elevador, um aviso destacado, para todos (quem leva carrinho de bebê
 * também precisa saber). Sem dados, só para quem escolheu "cadeira de rodas".
 */
@Composable
fun StationAccessLine(access: StationAccess?, wheelchair: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    when {
        access == null -> if (wheelchair) {
            Text(
                text = "♿ " + stringResource(R.string.access_unknown) + " " + stringResource(R.string.access_unknown_tell),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onOpen).padding(vertical = 4.dp),
            )
        }
        access.way == AccessWay.LIFT || access.way == AccessWay.STAIRS_ONLY -> Surface(
            onClick = onOpen,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "♿ " + stringResource(
                        if (access.way == AccessWay.LIFT) R.string.access_lift_notice else R.string.access_stairs_notice,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.access_open), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
        else -> Text(
            text = "♿ " + accessSummary(context, access) + " · " + stringResource(R.string.access_see_all),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onOpen).padding(vertical = 4.dp),
        )
    }
}

/**
 * Elevador, plataforma elevatória, rampa e escada da estação, quem pode usar e
 * de onde vem o dado. A tabela do Metrofor é de 2015: o "Conte pra gente" é
 * como o dado se mantém em dia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessScreen(stationName: String, lineName: String, access: StationAccess?, onBack: () -> Unit) {
    var reporting by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.access_title))
                        Text("$stationName · $lineName", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (access == null) {
                Text(stringResource(R.string.access_unknown), style = MaterialTheme.typography.bodyLarge)
            } else {
                Column {
                    EquipmentRow("🛗", stringResource(R.string.access_elevator), access.elevators)
                    if (access.lifts > 0) {
                        HorizontalDivider()
                        EquipmentRow("♿", stringResource(R.string.access_lift), access.lifts, stringResource(R.string.access_lift_desc))
                    }
                    HorizontalDivider()
                    EquipmentRow("↗", stringResource(R.string.access_ramp), access.ramps)
                    HorizontalDivider()
                    EquipmentRow("🪜", stringResource(R.string.access_stairs), access.stairs)
                    HorizontalDivider()
                    EquipmentRow("⇅", stringResource(R.string.access_escalator), access.escalators)
                }
            }
            InfoBox(stringResource(R.string.access_vehicle_title), stringResource(R.string.access_vehicle_text))
            InfoBox(stringResource(R.string.access_who_title), stringResource(R.string.access_who_text))
            Text(
                stringResource(if (access == null) R.string.access_unknown_tell else R.string.access_tell),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { reporting = true }.padding(vertical = 4.dp),
            )
            if (access != null) {
                SourceLine("acessibilidade")
                Text(
                    stringResource(R.string.access_maintenance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (reporting) AccessReportDialog(stationName, lineName, onDismiss = { reporting = false })
}

@Composable
private fun EquipmentRow(icon: String, name: String, count: Int, description: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(vertical = 10.dp),
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            if (count > 0) count.toString() else stringResource(R.string.access_none),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun InfoBox(title: String, text: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "O que você encontrou?": uma escolha e a mensagem pronta, pelo WhatsApp ou por e-mail. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccessReportDialog(stationName: String, lineName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val options = listOf(
        R.string.access_report_elevator_broken,
        R.string.access_report_lift_no_staff,
        R.string.access_report_ramp_blocked,
        R.string.access_report_wrong,
        R.string.access_report_ok,
        R.string.access_report_other,
    )
    var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
    val message = chosen?.let { stringResource(R.string.access_report_message, stationName, lineName, stringResource(it)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.access_report_title, stationName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.access_report_text))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(selected = chosen == option, onClick = { chosen = option }, label = { Text(stringResource(option)) })
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = message != null, onClick = {
                DevLog.log("acesso_relato", "estacao" to stationName, "via" to "whatsapp")
                send(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$REPORT_WHATSAPP?text=" + Uri.encode(message))))
                onDismiss()
            }) { Text(stringResource(R.string.access_report_whatsapp)) }
        },
        dismissButton = {
            TextButton(enabled = message != null, onClick = {
                DevLog.log("acesso_relato", "estacao" to stationName, "via" to "email")
                send(
                    context,
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
                        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.access_report_subject, stationName))
                        .putExtra(Intent.EXTRA_TEXT, message),
                )
                onDismiss()
            }) { Text(stringResource(R.string.access_report_email)) }
        },
    )
}

private fun send(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        DevLog.log("acesso_relato_sem_app")
    }
}
