package io.github.giova.metroceara

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import io.github.giova.metroceara.ui.OpenStationRequest
import io.github.giova.metroceara.widget.StationWidget
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.giova.metroceara.data.AppSettings
import io.github.giova.metroceara.data.TripLog
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.ui.AppRoot
import io.github.giova.metroceara.ui.theme.MetroCearaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openFromWidget(intent)
        sendTripLog(intent)
        setContent {
            val settings by SettingsStore.flow(this).collectAsState()
            val current = settings ?: AppSettings()
            val dark = when (current.themeMode) {
                AppSettings.THEME_LIGHT -> false
                AppSettings.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            // Os ícones da barra de status acompanham o tema do app, não o do
            // celular: com "Claro" escolhido no celular escuro, ficariam
            // brancos sobre fundo branco.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            MetroCearaTheme(darkTheme = dark, wallpaperColors = current.wallpaperColors) {
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFromWidget(intent)
        sendTripLog(intent)
    }

    /** Toque no convite depois de uma viagem com problema: o e-mail com o registro dela. */
    private fun sendTripLog(intent: Intent?) {
        val name = intent?.getStringExtra(EXTRA_SEND_TRIP) ?: return
        intent.removeExtra(EXTRA_SEND_TRIP)
        val file = TripLog.trip(name) ?: return
        TripLog.emailIntent(this, listOf(file))?.let { runCatching { startActivity(it) } }
    }

    companion object {
        const val EXTRA_SEND_TRIP = "send_trip_log"
    }

    /** Toque no widget: abre a tela da estação dele. */
    private fun openFromWidget(intent: Intent?) {
        val stop = intent?.getStringExtra(StationWidget.EXTRA_STOP) ?: return
        val route = intent.getStringExtra(StationWidget.EXTRA_ROUTE) ?: return
        OpenStationRequest.value = stop to route
        intent.removeExtra(StationWidget.EXTRA_STOP)
    }
}
