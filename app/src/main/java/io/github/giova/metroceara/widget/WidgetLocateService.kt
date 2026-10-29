package io.github.giova.metroceara.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.LocationSource
import io.github.giova.metroceara.data.PinnedStation
import io.github.giova.metroceara.data.ScheduleRepository
import io.github.giova.metroceara.data.nearestPerRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * O botão ↻ do widget: pega a localização uma vez e põe no widget a estação
 * mais perto. Roda em primeiro plano por poucos segundos porque o Android só
 * entrega a localização a um app fechado assim, e só quando foi a pessoa que
 * tocou no widget — sem a permissão de localização "o tempo todo".
 */
class WidgetLocateService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel(this)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.widget_locating))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        val started = runCatching {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        }
        if (started.isFailure) {
            DevLog.log("widget_locate", "result" to "no_foreground", "error" to started.exceptionOrNull().toString())
            stopSelf()
            return START_NOT_STICKY
        }
        // Toques seguidos no ↻ não abrem uma segunda busca.
        if (job?.isActive == true) return START_NOT_STICKY
        job = scope.launch {
            StationWidget.status = StationWidget.Status.LOCATING
            StationWidget.update(this@WidgetLocateService)
            // Sempre uma posição nova: no trem, a de 1 min atrás já está quase 1 km para trás.
            val position = LocationSource(this@WidgetLocateService).current(TIMEOUT_MILLIS, maxAgeMillis = MAX_AGE_MILLIS)
            val nearest = position?.let { p ->
                withContext(Dispatchers.IO) {
                    ScheduleRepository(this@WidgetLocateService).stations().nearestPerRoute(p.latitude, p.longitude)
                }.firstOrNull()
            }
            if (nearest != null) {
                val pinned = PinnedStation(this@WidgetLocateService)
                pinned.setLastNearby(nearest.first.stopId, nearest.first.routeId)
                // Quem tocou no ↻ quer a estação de onde está agora, não a fixada nem a última aberta.
                pinned.widgetNearby = true
                StationWidget.status = null
                DevLog.log(
                    "widget_locate", "result" to "ok", "station" to nearest.first.stopName,
                    "dist_m" to nearest.second.toInt(), "provider" to position.provider, "acc" to position.accuracy,
                )
            } else {
                StationWidget.status = StationWidget.Status.FAILED
                DevLog.log("widget_locate", "result" to "no_fix")
            }
            StationWidget.update(this@WidgetLocateService)
            ServiceCompat.stopForeground(this@WidgetLocateService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "widget_locate"
        private const val NOTIFICATION_ID = 2001
        private const val TIMEOUT_MILLIS = 20_000L
        private const val MAX_AGE_MILLIS = 0L

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.widget_locate_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
    }
}
