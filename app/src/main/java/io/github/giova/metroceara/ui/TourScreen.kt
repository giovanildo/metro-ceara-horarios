package io.github.giova.metroceara.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.BikeBoarding
import io.github.giova.metroceara.widget.StationWidgetSmall
import kotlinx.coroutines.launch

/** O que o "Experimentar" de cada recurso abre. Sem ação, o recurso só se mostra. */
class TourActions(
    val onCommute: () -> Unit,
    val onJourney: () -> Unit,
    val onRoutines: () -> Unit,
)

private enum class TourTry { COMMUTE, JOURNEY, ROUTINES, WIDGET }

/** Um recurso do tour: a tela real ([image]) ou, sem print, um desenho com o componente do app. */
private class TourItem(
    @StringRes val title: Int,
    @StringRes val text: Int,
    @DrawableRes val image: Int? = null,
    val drawing: (@Composable () -> Unit)? = null,
    val tryIt: TourTry? = null,
)

// Os mesmos recursos da página "Conheça o app" do site (tools/site.py RECURSOS_APP), com
// o que só o Android tem. A Nina fica de fora de propósito: no app ela é só o nome.
private val ITEMS = listOf(
    TourItem(R.string.tour_home_title, R.string.tour_home_text, image = R.drawable.tour_casa_trabalho, tryIt = TourTry.COMMUTE),
    TourItem(R.string.tour_journey_title, R.string.tour_journey_text, image = R.drawable.tour_para_onde, tryIt = TourTry.JOURNEY),
    TourItem(R.string.tour_trip_title, R.string.tour_trip_text, image = R.drawable.tour_modo_viagem, tryIt = TourTry.JOURNEY),
    TourItem(R.string.tour_tell_title, R.string.tour_tell_text, drawing = { TellDrawing() }, tryIt = TourTry.JOURNEY),
    TourItem(R.string.tour_leave_title, R.string.tour_leave_text, image = R.drawable.tour_hora_de_sair, tryIt = TourTry.ROUTINES),
    TourItem(R.string.tour_widget_title, R.string.tour_widget_text, image = R.drawable.tour_widget, tryIt = TourTry.WIDGET),
    TourItem(R.string.tour_bike_title, R.string.tour_bike_text, drawing = { BikeDrawing() }),
    TourItem(R.string.tour_special_title, R.string.tour_special_text, drawing = { SpecialDrawing() }),
)

/**
 * "Conheça o app" (no Mais): um recurso por vez, passando para o lado, com a tela
 * real em cima. "Experimentar" abre o lugar de verdade; na volta, o tour continua
 * no mesmo recurso ([page] fica com quem chama). Nada aqui abre sozinho.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourScreen(page: Int, onPage: (Int) -> Unit, actions: TourActions, onBack: () -> Unit) {
    val context = LocalContext.current
    val pager = rememberPagerState(initialPage = page.coerceIn(0, ITEMS.lastIndex)) { ITEMS.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect(onPage) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tour_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
                TourPage(ITEMS[i], position = stringResource(R.string.tour_position, i + 1, ITEMS.size))
            }
            Dots(count = ITEMS.size, current = pager.currentPage)
            val item = ITEMS[pager.currentPage]
            val last = pager.currentPage == ITEMS.lastIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                item.tryIt?.let { t ->
                    TextButton(onClick = {
                        when (t) {
                            TourTry.COMMUTE -> actions.onCommute()
                            TourTry.JOURNEY -> actions.onJourney()
                            TourTry.ROUTINES -> actions.onRoutines()
                            TourTry.WIDGET -> pinWidget(context)
                        }
                    }) {
                        Text(stringResource(if (t == TourTry.WIDGET) R.string.tour_widget_add else R.string.tour_try))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { if (last) onBack() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(if (last) R.string.tour_done else R.string.tour_next))
                }
            }
        }
    }
}

@Composable
private fun TourPage(item: TourItem, position: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        // A moldura de celular: a tela real ou o desenho, no mesmo tamanho.
        Box(
            modifier = Modifier
                .width(210.dp)
                .aspectRatio(540f / 960f)
                .clip(RoundedCornerShape(24.dp))
                .border(6.dp, Color(0xFF1D1B1E), RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            if (item.image != null) {
                Image(
                    painter = painterResource(item.image),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Box(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(18.dp))) { item.drawing?.invoke() }
            }
        }
        Text(
            text = stringResource(item.title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp).semantics { contentDescription = position },
        )
        Text(
            text = stringResource(item.text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == current) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** "Pôr na tela": o Android pergunta onde pôr o widget; onde não dá, o caminho à mão. */
private fun pinWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val asked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported &&
        runCatching { manager.requestPinAppWidget(ComponentName(context, StationWidgetSmall::class.java), null, null) }.getOrDefault(false)
    if (!asked) Toast.makeText(context, R.string.tour_widget_manual, Toast.LENGTH_LONG).show()
}

// ---------- desenhos dos recursos sem print (exemplos, com a palavra "Exemplo" em cima)

@Composable
private fun DrawingLabel(color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(stringResource(R.string.tour_example), style = MaterialTheme.typography.labelSmall, color = color)
}

/** Avisar alguém: a mensagem como chega numa conversa. */
@Composable
private fun TellDrawing() {
    Column(Modifier.fillMaxSize().background(Color(0xFFEFE9DF))) {
        Box(Modifier.fillMaxWidth().background(Color(0xFF3D5A80)).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.tour_tell_contact), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.padding(10.dp)) {
            DrawingLabel(Color(0xFF5A5A66))
            Surface(
                color = Color(0xFFDCF3D0),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    stringResource(R.string.tour_tell_sample),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1B1B21),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

/** Bike: o aviso de verdade da tela da estação, num dia útil antes das 15h. */
@Composable
private fun BikeDrawing() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
        DrawingLabel()
        Text(
            stringResource(R.string.tour_bike_station),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
        )
        BikeBoardingNotice(
            rule = BikeBoarding(allowed = true, boundary = 15 * 60),
            vehicle = stringResource(R.string.tour_bike_vehicle),
            rules = stringResource(R.string.bike_per_trip, 4),
        )
    }
}

/** Dias especiais: o aviso no topo da tela inicial. */
@Composable
private fun SpecialDrawing() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
        DrawingLabel()
        Surface(
            color = Color(0xFFFFF8DC),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
        ) {
            Row(Modifier.padding(10.dp)) {
                Text("⚠️", modifier = Modifier.size(22.dp))
                Text(
                    stringResource(R.string.tour_special_sample),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4A3D00),
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.tour_special_sunday),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
