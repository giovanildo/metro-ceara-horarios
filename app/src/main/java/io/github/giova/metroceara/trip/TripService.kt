package io.github.giova.metroceara.trip

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.LocationManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.LocationSource
import kotlin.math.roundToInt
import io.github.giova.metroceara.data.formatTime
import io.github.giova.metroceara.data.distanceMeters
import io.github.giova.metroceara.data.TripFix
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.TripLog
import io.github.giova.metroceara.data.SettingsStore
import io.github.giova.metroceara.data.TripEstimator
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.RoutePoint
import io.github.giova.metroceara.data.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

/**
 * Serviço em primeiro plano do modo viagem: segue o GPS com a tela desligada,
 * cai para a grade de horários quando o sinal some, e avisa duas estações
 * antes do destino.
 */
class TripService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var voice: TripVoice? = null
    /** Se a viagem teve problema, para o convite de mandar o registro ([endTrip]). */
    private var review: TripReview? = null
    private var lastTrip: ActiveTrip? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            endTrip("user")
            finish()
            return START_NOT_STICKY
        }
        val started0 = intent?.getStringExtra(EXTRA_PLAN)?.let(TripPlan::fromJson)
        if (started0 == null) {
            finish()
            return START_NOT_STICKY
        }
        // Muda de trecho num roteiro com integração (switchTo, mais abaixo).
        var plan: TripPlan = started0
        TripNotifications.createChannels(this)
        val settings = SettingsStore.get(this)
        var estimator = TripEstimator(plan, gpsLostMinutes = settings.gpsLostSeconds / 60.0)
        TripLog.begin()
        review = TripReview()
        DevLog.log(
            "trip_start", "line" to plan.routeName, "headsign" to plan.headsign,
            "from" to plan.origin.name, "to" to plan.destination.name,
            "dep" to formatTime(plan.origin.scheduled), "arr" to formatTime(plan.destination.scheduled),
            "stops" to plan.stops.joinToString(" > ") { "${it.name} ${formatTime(it.scheduled)}" },
            "via_terminal" to plan.terminal?.takeIf { plan.viaTerminal }?.name,
            "alert_at" to plan.stops[plan.alertIndex].name,
            "sound" to TripTracker.isSoundEnabled(this), "station_alerts" to TripTracker.isStationAlertsEnabled(this),
            "simulated" to plan.simulated,
        )
        var lastLogged: Triple<Int, TripFix, Boolean>? = null
        var trip = ActiveTrip(plan, estimator.progress(nowMinutes()), alerted = false, arrived = false)
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                TripNotifications.ONGOING_ID,
                TripNotifications.ongoing(this, trip),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        }
        if (started.isFailure) {
            // Sem permissão de localização o Android recusa o serviço; não há viagem.
            finish()
            return START_NOT_STICKY
        }
        TripTracker.publish(trip)

        job?.cancel()
        if (voice == null) voice = TripVoice(this)
        job = scope.launch {
            // A origem não é anunciada: a pessoa já está nela.
            var announced = 0
            // Roteiro com integração: a grade e o calculador de roteiros, para o próximo trecho.
            val repo by lazy { ScheduleRepository(this@TripService) }
            val router by lazy { JourneyPlanner.router(repo, settings) }

            // Troca para outro trecho (o próximo, ou o mesmo com o trem seguinte).
            fun switchTo(next: TripPlan, reason: String) {
                plan = next
                estimator = TripEstimator(next, gpsLostMinutes = settings.gpsLostSeconds / 60.0)
                announced = 0
                trip = ActiveTrip(next, estimator.progress(nowMinutes()), alerted = false, arrived = false)
                review?.onLeg()
                DevLog.log(
                    "trip_leg", "reason" to reason, "line" to next.routeName, "from" to next.origin.name,
                    "to" to next.destination.name, "dep" to formatTime(next.origin.scheduled),
                    "leg" to next.journey?.current,
                )
            }

            // O próximo trecho do roteiro, saindo de [from] (a estação de integração ou,
            // se a pessoa perdeu o trem, a partida do trecho atual) a partir de agora.
            fun nextLeg(from: RoutePoint, doneLegs: Int): TripPlan? {
                val journey = plan.journey ?: return null
                val itinerary = router.plan(from, RoutePoint(journey.destRouteId, journey.destStopId),
                    nowMinutes().toInt(), limit = 1).firstOrNull() ?: return null
                return JourneyPlanner.plan(repo, itinerary, settings, plan.simulated, done = journey.legs.take(doneLegs))
            }

            fun update() {
                // Depois de uma integração, o GPS ainda vê a pessoa na plataforma 3 min depois
                // da partida: ela perdeu o trem. Refaz o roteiro com o seguinte.
                val journey = plan.journey
                if (journey != null && journey.current > 0 && estimator.missedDeparture(nowMinutes(), MISSED_AFTER_MINUTES)) {
                    val missed = plan
                    nextLeg(RoutePoint(missed.routeId, missed.origin.stopId), doneLegs = journey.current)
                        ?.takeIf { it.origin.scheduled > missed.origin.scheduled }
                        ?.let { next ->
                            val text = TripNotifications.missed(this@TripService, missed, next)
                            DevLog.log("trip_missed", "dep" to formatTime(missed.origin.scheduled), "next" to formatTime(next.origin.scheduled))
                            if (TripTracker.isSoundEnabled(this@TripService)) voice?.speak(text)
                            switchTo(next, "missed")
                        }
                }
                val progress = estimator.progress(nowMinutes())
                val arrived = progress.index >= plan.stops.lastIndex
                var alerted = trip.alerted
                var alertedNow = false
                // Partiu da origem e chegou na estação de aviso (ou já passou dela). Se já
                // chegou ao destino (GPS que volta depois do túnel), vale só o aviso de chegada.
                if (!alerted && progress.index >= plan.alertIndex && (!arrived || plan.stops.size <= 2)) {
                    alerted = true
                    alertedNow = true
                    trip = trip.copy(progress = progress)
                    val sound = TripTracker.isSoundEnabled(this@TripService)
                    TripNotifications.alert(this@TripService, trip, sound)
                    DevLog.log("trip_alert", "station" to plan.stops.getOrNull(progress.index)?.name, "presumed" to progress.presumed)
                    if (sound) voice?.speak(spokenAlert(trip))
                }
                // Estação nova alcançada. Na estação do aviso principal, ele já basta.
                if (progress.index > announced) {
                    val previous = announced
                    announced = progress.index
                    // Chegou ao terminal de uma viagem "sentado": avisar para ficar no trem.
                    val turn = plan.turnaroundIndex
                    if (plan.viaTerminal && previous < turn && progress.index >= turn) {
                        val reached = trip.copy(progress = progress)
                        val text = TripNotifications.turnaround(this@TripService, reached)
                        DevLog.log("trip_turnaround", "text" to text)
                        if (TripTracker.isSoundEnabled(this@TripService)) voice?.speak(text)
                    }
                    // A partida do terminal é a mesma estação: não anunciar de novo.
                    val isTerminalAgain = plan.viaTerminal && progress.index == turn + 1
                    // Chegada: o aviso que fica na barra depois que a viagem encerra.
                    val arrivedNow = progress.index >= plan.stops.lastIndex
                    if (arrivedNow) {
                        val belongings = SettingsStore.get(this@TripService).remindBelongings
                        val text = TripNotifications.arrived(this@TripService, trip.copy(progress = progress), belongings)
                        if (TripTracker.isSoundEnabled(this@TripService)) voice?.speak(text)
                    }
                    if (!alertedNow && !isTerminalAgain && !arrivedNow && progress.index != turn &&
                        TripTracker.isStationAlertsEnabled(this@TripService)
                    ) {
                        val reached = trip.copy(progress = progress)
                        TripNotifications.station(this@TripService, reached)
                        DevLog.log("trip_station", "station" to plan.stops.getOrNull(progress.index)?.name, "presumed" to progress.presumed)
                        voice?.speak(spokenStation(reached))
                    }
                }
                val state = Triple(progress.index, progress.fix, progress.gpsSilentMinutes != null)
                if (state != lastLogged) {
                    lastLogged = state
                    DevLog.log(
                        "trip_progress", "index" to progress.index,
                        "station" to plan.stops.getOrNull(progress.index)?.name,
                        "fix" to progress.fix.name, "delay_min" to "%.1f".format(java.util.Locale.US, progress.delayMinutes),
                        "gps_silent_min" to progress.gpsSilentMinutes?.let { "%.1f".format(java.util.Locale.US, it) },
                        "eta" to formatTime(progress.arrivalMinutes.roundToInt()),
                    )
                }
                if (arrived && !trip.arrived) review?.onArrived(nowMinutes())
                trip = trip.copy(progress = progress, alerted = alerted, arrived = arrived)
                review?.onUpdate(trip)
                lastTrip = trip
                // Chegou à integração: segue no próximo trecho, sem a pessoa tocar em nada.
                if (arrived && plan.transfer != null && plan.journey != null) {
                    val done = (plan.journey?.current ?: 0) + 1
                    nextLeg(RoutePoint(plan.routeId, plan.destination.stopId), doneLegs = done)
                        ?.let { switchTo(it, "transfer") }
                }
                TripTracker.publish(trip)
                runCatching {
                    getSystemService(NotificationManager::class.java)
                        .notify(TripNotifications.ONGOING_ID, TripNotifications.ongoing(this@TripService, trip))
                }
            }

            launch {
                LocationSource(this@TripService).updates(GPS_INTERVAL_MILLIS, 0f).collect { location ->
                    if (DevLog.isEnabled || TripLog.recording) {
                        val nearest = plan.stops.minBy {
                            distanceMeters(location.latitude, location.longitude, it.lat, it.lon)
                        }
                        DevLog.log(
                            "trip_gps", "provider" to location.provider, "lat" to location.latitude,
                            "lon" to location.longitude, "acc" to location.accuracy,
                            "speed" to location.takeIf { it.hasSpeed() }?.speed,
                            "nearest" to nearest.name,
                            "dist_m" to distanceMeters(location.latitude, location.longitude, nearest.lat, nearest.lon).toInt(),
                        )
                    }
                    estimator.onLocation(
                        location.latitude, location.longitude, location.accuracy, nowMinutes(),
                        fromNetwork = location.provider == LocationManager.NETWORK_PROVIDER,
                    )
                    update()
                }
            }
            while (true) {
                update()
                if (trip.arrived) {
                    endTrip("arrived", "delay_min" to "%.1f".format(java.util.Locale.US, trip.progress.delayMinutes))
                    // Encerra sozinho: só espera a voz terminar de falar a chegada.
                    delay(ARRIVED_LINGER_MILLIS)
                    finish()
                    break
                }
                if (nowMinutes() > plan.destination.scheduled + trip.progress.delayMinutes + settings.tripGiveUpMinutes) {
                    endTrip("give_up")
                    finish()
                    break
                }
                delay(TICK_MILLIS)
            }
        }
        return START_NOT_STICKY
    }

    /** "Padre Cícero." — e "Faltam 3 estações para Parangaba." só quando faltam 3; ou a chegada. */
    private fun spokenStation(trip: ActiveTrip): String {
        val plan = trip.plan
        val index = trip.progress.index.coerceIn(0, plan.stops.lastIndex)
        if (index == plan.stops.lastIndex) return getString(R.string.trip_arrived, plan.destination.name)
        val name = if (trip.progress.presumed) {
            getString(R.string.trip_station_presumed, plan.stops[index].name)
        } else {
            plan.stops[index].name
        }
        return name + "." + (TripNotifications.stationDetail(this, trip)?.let { " $it" } ?: "")
    }

    /** "Prepare-se para descer. Faltam 2 estações para Benfica." */
    private fun spokenAlert(trip: ActiveTrip): String =
        (if (trip.plan.simulated) getString(R.string.trip_sim) + ". " else "") + spokenAlertText(trip)

    private fun spokenAlertText(trip: ActiveTrip): String {
        val left = trip.progress.stationsLeft(trip.plan)
        val prefix = if (trip.progress.presumed) getString(R.string.trip_presumed_prefix) + ", " else ""
        if (trip.plan.transfer != null) {
            return getString(R.string.trip_transfer_alert_title, trip.plan.destination.name) + ". " + prefix +
                TripNotifications.transferAlert(this, trip).replaceFirstChar { if (prefix.isNotEmpty()) it.lowercaseChar() else it }
        }
        return getString(R.string.trip_alert_title) + ". " + prefix + getString(
            R.string.trip_alert_text,
            resources.getQuantityString(R.plurals.trip_left, left, left),
            trip.plan.destination.name,
        ).replaceFirstChar { if (prefix.isNotEmpty()) it.lowercaseChar() else it }.let {
            if (SettingsStore.get(this).remindBelongings) it + " " + getString(R.string.trip_belongings) else it
        }
    }

    /**
     * Fecha o registro da viagem e, se ela teve problema (e não era simulação),
     * convida a pessoa a mandar o registro por e-mail — no máximo uma vez por
     * semana ([TripLog.shouldInvite]).
     */
    private fun endTrip(reason: String, vararg extra: Pair<String, Any?>) {
        val review = review ?: TripReview()
        val problems = review.problems(reason)
        DevLog.log(
            "trip_review", "problems" to problems.joinToString(","),
            "arrival_error_min" to review.arrivalErrorMinutes?.let { "%.1f".format(java.util.Locale.US, it) },
            "max_gps_silent_min" to "%.1f".format(java.util.Locale.US, review.maxGpsSilentMinutes),
        )
        DevLog.log("trip_end", "reason" to reason, *extra)
        val file = TripLog.end()
        this.review = null
        if (problems.isNotEmpty() && file != null && lastTrip?.plan?.simulated != true && TripLog.shouldInvite(this)) {
            TripNotifications.review(this, file.name)
        }
    }

    private fun finish() {
        job?.cancel()
        TripTracker.publish(null)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        voice?.shutdown()
        voice = null
        TripTracker.publish(null)
        super.onDestroy()
    }

    private fun nowMinutes(): Double {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza"))
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) +
            calendar.get(Calendar.SECOND) / 60.0
    }

    companion object {
        const val ACTION_START = "io.github.giova.metroceara.trip.START"
        const val ACTION_STOP = "io.github.giova.metroceara.trip.STOP"
        const val EXTRA_PLAN = "plan"

        private const val GPS_INTERVAL_MILLIS = 5_000L
        private const val TICK_MILLIS = 15_000L
        private const val ARRIVED_LINGER_MILLIS = 8_000L
        /** Depois de uma integração: ainda na plataforma tanto tempo depois da partida = perdeu o trem. */
        private const val MISSED_AFTER_MINUTES = 3.0
    }
}
