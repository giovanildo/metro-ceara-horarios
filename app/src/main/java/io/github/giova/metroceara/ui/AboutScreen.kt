package io.github.giova.metroceara.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import java.io.File
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.DevLogCloud
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.HorizontalDivider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Switch

// Sem pedido de doação dentro do app: a Play Store restringe pedir dinheiro
// fora do sistema de pagamento dela. A doação (Pix) fica na página do app e
// na versão lite.

private const val REPO_URL = "https://github.com/giovanildo/metro-ceara-horarios"

/** A política de privacidade: a Play Store exige o link também dentro do app. */
private const val PRIVACY_URL = "https://giovanildo.github.io/metro-ceara-horarios/privacidade.html"

/**
 * O link para divulgar: a versão lite (no navegador), porta de entrada que
 * abre na hora, sem os avisos de instalação; de lá a pessoa chega ao app.
 */
private const val SITE_URL = "https://giovanildo.github.io/metro-ceara-horarios/app/"

/** Sobre o app: aviso de não oficial, fontes dos dados, licença e doação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var versionTaps by remember { mutableIntStateOf(0) }
    val devUnlocked by DevLog.unlocked.collectAsState()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Como no Android: 7 toques na versão liberam as opções de desenvolvedor.
                    modifier = Modifier.clickable {
                        if (devUnlocked) return@clickable
                        versionTaps++
                        if (versionTaps >= 7) {
                            DevLog.unlock(context)
                            Toast.makeText(context, R.string.dev_unlocked, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.about_unofficial),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            Section(R.string.about_sources_title, R.string.about_sources)
            Section(R.string.about_privacy_title, R.string.about_privacy)
            OutlinedButton(onClick = { open(context, PRIVACY_URL) }) {
                Text(stringResource(R.string.about_privacy_link))
            }
            Section(R.string.about_license_title, R.string.about_license)
            // O endereço por extenso: dá para ler, tocar para abrir ou copiar e mandar.
            Text(
                text = REPO_URL.removePrefix("https://"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { open(context, REPO_URL) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open(context, REPO_URL) }) {
                    Text(stringResource(R.string.about_source_code))
                }
                OutlinedButton(onClick = { copy(context, "GitHub", REPO_URL, R.string.about_link_copied) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.about_copy_link), modifier = Modifier.padding(start = 6.dp))
                }
            }

            // QR da página da última versão (o mesmo dos stories; o link nunca muda):
            // outra pessoa aponta a câmera para este celular e baixa o app.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.about_share_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Compartilha o link da página do app (com prévia no WhatsApp),
                // não o do .apk, que parece spam para quem não conhece.
                val shareMessage = stringResource(R.string.about_share_message, SITE_URL)
                val shareChooser = stringResource(R.string.about_share_button)
                Button(
                    onClick = { share(context, shareMessage, shareChooser) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(shareChooser, modifier = Modifier.padding(start = 8.dp))
                }
                // Fundo sempre branco: no tema escuro o QR invertido não é lido.
                Surface(
                    color = androidx.compose.ui.graphics.Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.qr_download),
                        contentDescription = stringResource(R.string.about_share_qr),
                        filterQuality = FilterQuality.None,
                        modifier = Modifier.padding(12.dp).size(220.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.about_share_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (devUnlocked) DevOptions()
        }
    }
}

/** Opções de desenvolvedor: o registro de testes de campo. Só aparece depois dos 7 toques. */
@Composable
private fun DevOptions() {
    val context = LocalContext.current
    val logOn by DevLog.enabledFlow.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val (bytes, days) = remember(refresh, logOn) { DevLog.stats() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dev_title), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dev_log), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.dev_log_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = logOn, onCheckedChange = {
                    DevLog.setEnabled(context, it)
                    refresh++
                })
            }
            Text(
                text = stringResource(R.string.dev_log_size, "%.1f".format(bytes / 1024.0), days),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { saveLog(context) }) { Text(stringResource(R.string.dev_save)) }
                OutlinedButton(onClick = {
                    DevLog.clear()
                    refresh++
                    Toast.makeText(context, R.string.dev_cleared, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.dev_clear)) }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            CloudCopy()
        }
    }
}

/** A cópia do registro numa pasta escolhida (de preferência no Google Drive): um arquivo por dia. */
@Composable
private fun CloudCopy() {
    val context = LocalContext.current
    val cloud by DevLogCloud.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree != null && DevLogCloud.setFolder(context, tree)) DevLog.syncCloudNow()
    }
    Text(
        text = stringResource(R.string.dev_cloud_title),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(
        text = stringResource(R.string.dev_cloud_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = when {
            cloud.folder == null -> stringResource(R.string.dev_cloud_none)
            cloud.error != null -> stringResource(R.string.dev_cloud_error, cloud.folder!!, cloud.error!!)
            cloud.lastOkMillis > 0 -> stringResource(R.string.dev_cloud_ok, cloud.folder!!, formatWhen(cloud.lastOkMillis))
            else -> stringResource(R.string.dev_cloud_waiting, cloud.folder!!)
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (cloud.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        Button(onClick = { picker.launch(null) }) {
            Text(stringResource(if (cloud.folder == null) R.string.dev_cloud_choose else R.string.dev_cloud_change))
        }
        if (cloud.folder != null) {
            OutlinedButton(onClick = {
                DevLog.syncCloudNow()
                Toast.makeText(context, R.string.dev_cloud_sending, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.dev_cloud_now)) }
        }
    }
    if (cloud.folder != null) {
        OutlinedButton(onClick = { DevLogCloud.clearFolder(context) }, modifier = Modifier.padding(top = 4.dp)) {
            Text(stringResource(R.string.dev_cloud_stop))
        }
    }
}

private fun formatWhen(millis: Long): String =
    SimpleDateFormat("dd/MM 'às' HH:mm", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }
        .format(Date(millis))

/**
 * Salva o registro na pasta Downloads do próprio celular, sem passar por nenhum
 * app ou serviço. Antes do Android 10 (sem acesso livre a Downloads), vai para a
 * pasta do app em Android/data, que também aparece pelo cabo USB.
 */
private fun saveLog(context: Context) {
    val file = DevLog.export(File(context.cacheDir, "devlog-export"))
    if (file == null) {
        Toast.makeText(context, R.string.dev_empty, Toast.LENGTH_SHORT).show()
        return
    }
    val where = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                // Tipo genérico: com "text/plain" o Android trocaria a extensão para .txt.
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            "Download/" + file.name
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
            file.copyTo(File(dir, file.name), overwrite = true).absolutePath
        }
    }.also { file.delete() }.getOrNull()
    val message = if (where != null) context.getString(R.string.dev_saved, where) else context.getString(R.string.dev_save_failed)
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

@Composable
private fun Section(title: Int, body: Int) {
    Column {
        Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun copy(context: Context, label: String, text: String, toast: Int) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

private fun share(context: Context, message: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, message)
    runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
}

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
