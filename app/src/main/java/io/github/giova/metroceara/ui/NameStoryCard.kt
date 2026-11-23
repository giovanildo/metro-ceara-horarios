package io.github.giova.metroceara.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.NameStory
import io.github.giova.metroceara.data.ShareTexts
import io.github.giova.metroceara.data.StationNames
import java.io.File

/**
 * "Por que esse nome?" na tela da estação (regras.json "nomes", a mesma pesquisa
 * do site), com a fonte e o botão que manda o cartão: uma imagem com o texto e o
 * nome do app, e na legenda o texto, a página da estação no site e os links do app.
 */
@Composable
fun NameStorySection(stationName: String, story: NameStory, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(stringResource(R.string.name_story_title), style = MaterialTheme.typography.titleSmall)
        Text(
            text = story.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        SourceLink(
            label = stringResource(R.string.source_label),
            title = story.source,
            facts = emptyList(),
            detail = null,
            link = story.link,
            modifier = Modifier.padding(top = 4.dp),
        )
        ShareActionButton(onClick = { shareNameCard(context, stationName, story) })
    }
}

/** Desenha o cartão, guarda no cache e abre o compartilhar com a imagem e a legenda. */
fun shareNameCard(context: Context, stationName: String, story: NameStory) {
    val caption = ShareTexts.nameStory(stationName, story)
    val uri = runCatching {
        val dir = File(context.cacheDir, "compartilhar").apply { mkdirs() }
        val file = File(dir, "por-que-esse-nome.png")
        file.outputStream().use { nameCard(context, stationName, story).compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.arquivos", file)
    }.onFailure { DevLog.log("cartao_nome_erro", "erro" to it.toString()) }.getOrNull()
    if (uri == null) {
        shareArrival(context, caption)
        return
    }
    val send = Intent(Intent.ACTION_SEND).setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, caption)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private const val CARD_WIDTH = 1080
private const val PAD = 72f
private const val BRAND_RED = 0xFFCE0F2E.toInt()

/** O cartão em 1080 px de largura: faixa vermelha com o nome, o texto, e o app no rodapé. */
internal fun nameCard(context: Context, stationName: String, story: NameStory): Bitmap {
    val name = StationNames.official(JourneyPlanner.plainName(stationName)).replace(Regex("\\s*\\(.*\\)$"), "")
    val inner = (CARD_WIDTH - 2 * PAD).toInt()
    fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
    fun layout(text: String, p: TextPaint, spacing: Float = 1f) =
        StaticLayout.Builder.obtain(text, 0, text.length, p, inner)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, spacing).build()

    val kicker = layout(context.getString(R.string.name_story_title).uppercase(), paint(36f, 0xE6FFFFFF.toInt(), bold = true))
    val title = layout(context.getString(R.string.name_story_station, name), paint(72f, Color.WHITE, bold = true), 1.05f)
    val body = layout(story.text, paint(46f, 0xFF222222.toInt()), 1.3f)
    val foot = paint(34f, 0xFF555555.toInt())
    val footText = context.getString(R.string.name_story_footer)

    val header = PAD + kicker.height + 16f + title.height + PAD * 0.8f
    val footer = 150f
    val height = (header + PAD + body.height + PAD + footer).toInt()
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bitmap)
    c.drawColor(Color.WHITE)
    c.drawRect(0f, 0f, CARD_WIDTH.toFloat(), header, Paint().apply { color = BRAND_RED })
    c.save(); c.translate(PAD, PAD); kicker.draw(c); c.translate(0f, kicker.height + 16f); title.draw(c); c.restore()
    c.save(); c.translate(PAD, header + PAD); body.draw(c); c.restore()
    // Rodapé: uma linha fina, o ícone do app e o nome.
    val top = height - footer
    c.drawRect(PAD, top, CARD_WIDTH - PAD, top + 2f, Paint().apply { color = 0xFFE6E3EA.toInt() })
    val icon = 76f
    val iconTop = top + (footer - icon) / 2
    ContextCompat.getDrawable(context, R.drawable.ic_launcher)?.let { d ->
        c.save()
        c.clipPath(Path().apply { addRoundRect(RectF(PAD, iconTop, PAD + icon, iconTop + icon), 18f, 18f, Path.Direction.CW) })
        d.setBounds(PAD.toInt(), iconTop.toInt(), (PAD + icon).toInt(), (iconTop + icon).toInt())
        d.draw(c)
        c.restore()
    }
    c.drawText(footText, PAD + icon + 24f, iconTop + icon / 2 - (foot.descent() + foot.ascent()) / 2, foot)
    return bitmap
}
