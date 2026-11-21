package io.github.giova.metroceara.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R

/** Abre o compartilhar do Android com [text]: a pessoa escolhe o app e o contato. */
fun shareArrival(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** "Avisar alguém": manda a previsão de chegada. [text] só é montado no toque. */
@Composable
fun ShareArrivalButton(text: () -> String, modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    OutlinedButton(onClick = { shareArrival(context, text()) }, modifier = modifier) {
        Icon(Icons.Filled.Share, contentDescription = null)
        Text(stringResource(R.string.share_arrival), modifier = Modifier.padding(start = 8.dp))
    }
}
