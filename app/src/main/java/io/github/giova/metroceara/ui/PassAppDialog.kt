package io.github.giova.metroceara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.giova.metroceara.R

/**
 * "Passar o app para alguém" (Mais): o QR em tela cheia, para a outra pessoa
 * apontar a câmera ali mesmo, na plataforma ou no vagão. O QR leva à página
 * /play/?de=qr-app: no Android abre a Play Store, no iPhone a versão lite.
 * Enquanto aberto, a tela fica no brilho máximo (QR escuro não lê).
 * Embaixo, o jeito de antes: mandar o link por mensagem ([onSendLink]).
 */
@Composable
fun PassAppDialog(onDismiss: () -> Unit, onSendLink: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        // Só a janela do diálogo: ao fechar, o brilho volta sozinho.
        LaunchedEffect(window) {
            window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 1f } }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.pass_app_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.pass_app_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                // Fundo branco sempre, até no tema escuro: a câmera precisa do contraste.
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.qr_passar),
                        contentDescription = stringResource(R.string.pass_app_qr),
                        filterQuality = FilterQuality.None,
                        modifier = Modifier
                            .background(Color.White)
                            .padding(16.dp)
                            .size(280.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.pass_app_iphone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onSendLink, modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp)) {
                    Text(stringResource(R.string.pass_app_send))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.pass_app_close)) }
            }
        }
    }
}
