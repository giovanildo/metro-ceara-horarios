package io.github.giova.metroceara.trip

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.github.giova.metroceara.data.DevLog
import io.github.giova.metroceara.data.JourneyPlanner
import io.github.giova.metroceara.data.TripRail
import io.github.giova.metroceara.data.TripPlan
import io.github.giova.metroceara.data.TripProgress
import io.github.giova.metroceara.data.nowMinutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Uma viagem em andamento, como a tela e a notificação a mostram. */
data class ActiveTrip(
    val plan: TripPlan,
    val progress: TripProgress,
    val alerted: Boolean,
    val arrived: Boolean,
)

/** A viagem que acabou de chegar ao destino, para a tela de chegada. */
data class TripArrival(
    val routeId: String,
    val origin: String,
    val destination: String,
    val departure: Int,
    val arrival: Int,
    val stations: Int,
    val simulated: Boolean,
    val atMillis: Long,
) {
    val durationMinutes: Int get() = (arrival - departure + 24 * 60) % (24 * 60)

    /** Quem abre o app horas depois de chegar não precisa do resumo. */
    fun isRecent(nowMillis: Long): Boolean = nowMillis - atMillis < SHOWN_MILLIS

    companion object {
        private const val SHOWN_MILLIS = 30 * 60_000L

        fun from(trip: ActiveTrip, now: Int, nowMillis: Long): TripArrival {
            val plan = trip.plan
            return TripArrival(
                routeId = plan.routeId,
                origin = plan.journey?.originName ?: JourneyPlanner.plainName(plan.origin.name),
                destination = JourneyPlanner.plainName(plan.destination.name),
                departure = plan.journey?.legs?.first()?.departure ?: plan.origin.scheduled,
                arrival = now,
                stations = TripRail.build(plan, trip.progress, lineHex = "").stationsRidden,
                simulated = plan.simulated,
                atMillis = nowMillis,
            )
        }
    }
}

/**
 * Ponte entre o [TripService] e a interface: o serviço publica o estado aqui,
 * as telas observam. Também guarda a preferência do aviso sonoro.
 */
object TripTracker {

    private val _active = MutableStateFlow<ActiveTrip?>(null)
    val active: StateFlow<ActiveTrip?> = _active.asStateFlow()

    private val _arrival = MutableStateFlow<TripArrival?>(null)
    /** A última chegada, até a pessoa sair da tela de chegada ou começar outra viagem. */
    val arrival: StateFlow<TripArrival?> = _arrival.asStateFlow()

    private val _soundEnabled = MutableStateFlow<Boolean?>(null)
    private val _stationAlerts = MutableStateFlow<Boolean?>(null)

    internal fun publish(trip: ActiveTrip?) {
        // Só na passagem para "chegou": depois disso o serviço ainda publica por alguns segundos.
        if (trip?.arrived == true && _active.value?.arrived != true) {
            _arrival.value = TripArrival.from(trip, nowMinutes(), System.currentTimeMillis())
        }
        _active.value = trip
    }

    fun dismissArrival() {
        _arrival.value = null
    }

    fun start(context: Context, plan: TripPlan) {
        _arrival.value = null
        val intent = Intent(context, TripService::class.java)
            .setAction(TripService.ACTION_START)
            .putExtra(TripService.EXTRA_PLAN, plan.toJson())
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(Intent(context, TripService::class.java).setAction(TripService.ACTION_STOP))
    }

    /** Se o aviso de chegada toca som (o padrão) ou só aparece e vibra. */
    fun soundEnabled(context: Context): StateFlow<Boolean?> {
        if (_soundEnabled.value == null) _soundEnabled.value = prefs(context).getBoolean(KEY_SOUND, true)
        return _soundEnabled
    }

    fun isSoundEnabled(context: Context): Boolean = soundEnabled(context).value ?: true

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_SOUND, enabled) }
        _soundEnabled.value = enabled
        DevLog.log("trip_option", "option" to "sound", "on" to enabled)
    }

    /** Se cada estação alcançada gera um aviso curto com som (desligado por padrão). */
    fun stationAlerts(context: Context): StateFlow<Boolean?> {
        if (_stationAlerts.value == null) _stationAlerts.value = prefs(context).getBoolean(KEY_STATION_ALERTS, false)
        return _stationAlerts
    }

    fun isStationAlertsEnabled(context: Context): Boolean = stationAlerts(context).value ?: false

    fun setStationAlerts(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_STATION_ALERTS, enabled) }
        _stationAlerts.value = enabled
        DevLog.log("trip_option", "option" to "station_alerts", "on" to enabled)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_prefs", Context.MODE_PRIVATE)

    private const val KEY_SOUND = "trip_alert_sound"
    private const val KEY_STATION_ALERTS = "trip_station_alerts"
}
