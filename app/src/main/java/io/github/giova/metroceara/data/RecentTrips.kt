package io.github.giova.metroceara.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/**
 * As viagens a sugerir a partir da estação aberta na tela inicial: para as duas
 * estações desta viagem, menos a que for a própria estação. Na origem sobra o
 * destino; no destino, a volta; em outra estação, as duas, sempre o destino
 * primeiro. Sem estação conhecida, a viagem e a volta como foram feitas: o app não
 * sabe onde a pessoa está. Destino em estação com um lado em cada linha (Parangaba
 * e Parangaba - NE): vale o lado da linha de quem sai, senão não há metrô direto.
 * [nameOf]: o nome sem o sufixo da linha; só muda de lado com o mesmo nome.
 */
fun RecentTrip.suggestionsFrom(
    stop: String?,
    route: String?,
    links: List<TransferLink>,
    nameOf: (String) -> String?,
): List<RecentTrip> {
    val both = listOf(this, reversed())
    if (stop == null || route == null) return both
    return both.filterNot { isSameStation(stop, it.destStop, links) }.map { trip ->
        val side = links.firstOrNull {
            it.fromStop == trip.destStop && it.fromRoute == trip.destRoute && it.toRoute == route && nameOf(it.toStop) == nameOf(trip.destStop)
        }
        trip.copy(originStop = stop, originRoute = route, destStop = side?.toStop ?: trip.destStop, destRoute = side?.toRoute ?: trip.destRoute)
    }
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
