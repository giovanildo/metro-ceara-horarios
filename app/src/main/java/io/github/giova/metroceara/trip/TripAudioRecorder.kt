package io.github.giova.metroceara.trip

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import io.github.giova.metroceara.BuildConfig
import io.github.giova.metroceara.data.DevLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Grava o som da viagem, inteiro, para processar depois (os avisos sonoros do metrô, as
 * portas, o motor: análise de 27/11/2026). Só na versão de teste ([BuildConfig.TESTE]), só
 * com a opção de desenvolvedor ligada ([DevLog.audioEnabled]) e só durante o modo viagem.
 *
 * Um arquivo .m4a por viagem em Download/MetroCearaTeste (AAC mono, ~48 kbit/s: uns
 * 17 MB por viagem de 40 min). O registro de testes anota `audio_start` (nome do arquivo e
 * a hora em que a gravação começou, para alinhar com o resto) e `audio_stop` (duração).
 * O som pega as conversas de quem estiver perto: fica só no aparelho do desenvolvedor.
 */
class TripAudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var uri: Uri? = null
    private var fd: ParcelFileDescriptor? = null
    private var startedAt = 0L
    private var name = ""

    fun start() {
        if (!canRecord(context)) {
            DevLog.log("audio_start", "error" to "sem permissão de microfone")
            return
        }
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("America/Fortaleza") }.format(Date())
        name = "viagem-$stamp.m4a"
        val source = source()
        try {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(source)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(SAMPLE_RATE)
            r.setAudioEncodingBitRate(BIT_RATE)
            output(r)
            r.prepare()
            r.start()
            startedAt = SystemClock.elapsedRealtime()
            recorder = r
            DevLog.log(
                "audio_start", "file" to name,
                "source" to if (source == MediaRecorder.AudioSource.UNPROCESSED) "unprocessed" else "mic",
                "rate" to SAMPLE_RATE, "kbps" to BIT_RATE / 1000,
            )
        } catch (e: Exception) {
            DevLog.log("audio_start", "file" to name, "error" to e.toString())
            release(keep = false)
        }
    }

    fun stop() {
        val r = recorder ?: return
        recorder = null
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000
        val ok = runCatching { r.stop() }.isSuccess
        release(keep = ok)
        DevLog.log("audio_stop", "file" to name, "s" to seconds, "ok" to ok)
    }

    /** Arquivo em Download/MetroCearaTeste (Android 10+) ou na pasta do app. */
    private fun output(r: MediaRecorder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MetroCearaTeste")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val target = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore recusou o arquivo")
            uri = target
            val pfd = context.contentResolver.openFileDescriptor(target, "w") ?: error("sem arquivo")
            fd = pfd
            r.setOutputFile(pfd.fileDescriptor)
        } else {
            val dir = File(context.getExternalFilesDir(null), "audio").apply { mkdirs() }
            r.setOutputFile(File(dir, name).absolutePath)
        }
    }

    private fun release(keep: Boolean) {
        runCatching { recorder?.release() }
        recorder = null
        runCatching { fd?.close() }
        fd = null
        val target = uri ?: return
        uri = null
        runCatching {
            if (keep) {
                context.contentResolver.update(
                    target, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null,
                )
            } else {
                context.contentResolver.delete(target, null, null)
            }
        }
    }

    /** O microfone sem o tratamento de voz do Android, se o celular oferecer; senão o comum. */
    private fun source(): Int {
        val am = context.getSystemService(AudioManager::class.java)
        val unprocessed = am?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        return if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
    }

    companion object {
        private const val SAMPLE_RATE = 32_000
        private const val BIT_RATE = 48_000

        /** Versão de teste, com a permissão de microfone dada. */
        fun canRecord(context: Context): Boolean = BuildConfig.TESTE &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
}
