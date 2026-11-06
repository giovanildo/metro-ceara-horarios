package io.github.giova.metroceara.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.R
import io.github.giova.metroceara.alerts.OperationAlerts

/**
 * Debaixo do aviso do Metrofor, o convite para receber a próxima operação especial
 * por notificação. No Android 13+ a permissão só era pedida pelo modo viagem e pelo
 * aviso da hora de sair; quem nunca usou nenhum dos dois não recebia o aviso do ENEM.
 * Aparece só com um aviso na tela (a hora em que faz sentido), sem a permissão e
 * enquanto a pessoa não disser "Agora não" ou negar no Android.
 */
@Composable
fun OperationNotificationOffer(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    var hidden by remember { mutableStateOf(granted() || OperationAlerts.offerDismissed(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        // Negou no Android: não insiste (depois de duas negativas, o Android nem mostra mais).
        if (!ok) OperationAlerts.dismissOffer(context)
        hidden = true
    }
    if (hidden) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.operations_offer), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text(stringResource(R.string.operations_offer_yes))
            }
            TextButton(onClick = {
                OperationAlerts.dismissOffer(context)
                hidden = true
            }) { Text(stringResource(R.string.operations_offer_no)) }
        }
    }
}
