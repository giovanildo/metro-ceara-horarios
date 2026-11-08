package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

/**
 * Uma viagem consultada: de onde (estação e linha), para onde (estação e linha)
 * e, se o destino era um lugar ("Rodoviária"), o nome dele.
 */
data class RecentTrip(
    val originStop: String,
    val originRoute: String,
    val destStop: String,
    val destRoute: String,
    val landmark: String? = null,
    /** Quando foi vista (milissegundos); 0 numa viagem guardada antes da 1.89. */
    val at: Long = 0,
) {
    /** O mesmo trajeto, para não repetir na lista. */
    fun sameAs(other: RecentTrip): Boolean =
        originStop == other.originStop && originRoute == other.originRoute &&
            destStop == other.destStop && destRoute == other.destRoute && landmark == other.landmark

    internal fun encode(): String = listOf(originStop, originRoute, destStop, destRoute, landmark.orEmpty(), at.toString()).joinToString("\t")

    /** O caminho de volta, entre as mesmas estações. */
    fun reversed(): RecentTrip = RecentTrip(destStop, destRoute, originStop, originRoute, landmark = null, at = at)

    companion object {
        internal fun decode(line: String): RecentTrip? {
            val p = line.split("\t")
            if (p.size < 4 || p.take(4).any { it.isBlank() }) return null
            return RecentTrip(p[0], p[1], p[2], p[3], p.getOrNull(4)?.takeIf { it.isNotBlank() }, p.getOrNull(5)?.toLongOrNull() ?: 0)
        }
    }
}

/** A lista nova com [trip] no topo, sem repetir o mesmo trajeto, até [max]. */
fun List<RecentTrip>.withTrip(trip: RecentTrip, max: Int = RecentTrips.MAX): List<RecentTrip> =
    (listOf(trip) + filterNot { it.sameAs(trip) }).take(max)

private fun dayOf(millis: Long): Int =
    Calendar.getInstance(TimeZone.getTimeZone("America/Fortaleza")).run {
        timeInMillis = millis
        get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
    }

/**
 * A última viagem deve aparecer invertida? Perto de uma das duas estações, sim
 * se a mais perto for o destino. Sem localização, ou longe das duas, sim se ela
 * foi vista hoje, há pelo menos [returnHours]: a pessoa provavelmente já foi.
 */
fun shouldReverseRecent(
    distanceOrigin: Double?,
    distanceDestination: Double?,
    nearMeters: Double,
    seenAt: Long,
    now: Long,
    returnHours: Int,
): Boolean {
    if (distanceOrigin != null && distanceDestination != null && minOf(distanceOrigin, distanceDestination) <= nearMeters) {
        return distanceDestination < distanceOrigin
    }
    return dayOf(seenAt) == dayOf(now) && now - seenAt >= returnHours * 3_600_000L
}

/**
 * As últimas viagens vistas em "Planeje sua viagem" (até [MAX]), só neste celular:
 * estações, nunca o endereço. Entra quando a pessoa vê o resultado.
 */
object RecentTrips {
    const val MAX = 3
    private val state = MutableStateFlow<List<RecentTrip>?>(null)

    fun all(context: Context): StateFlow<List<RecentTrip>?> {
        if (state.value == null) state.value = load(context)
        return state.asStateFlow()
    }

    fun add(context: Context, trip: RecentTrip) =
        save(context, load(context).withTrip(trip.copy(at = System.currentTimeMillis())))

    fun remove(context: Context, trip: RecentTrip) = save(context, load(context).filterNot { it.sameAs(trip) })

    fun clear(context: Context) = save(context, emptyList())

    private fun save(context: Context, list: List<RecentTrip>) {
        prefs(context).edit { putString("recent", list.joinToString("\n") { it.encode() }) }
        state.value = list
    }

    private fun load(context: Context): List<RecentTrip> =
        prefs(context).getString("recent", null).orEmpty().lines().mapNotNull { RecentTrip.decode(it) }

    // Arquivo próprio: "Restaurar padrões" das configurações não apaga as viagens.
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("metrofor_recent_trips", Context.MODE_PRIVATE)
}
