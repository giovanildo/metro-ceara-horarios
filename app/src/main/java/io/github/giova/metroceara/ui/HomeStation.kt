package io.github.giova.metroceara.ui

import io.github.giova.metroceara.data.DayEnd
import io.github.giova.metroceara.data.Departure
import io.github.giova.metroceara.data.Station

/** A próxima partida de um sentido, para o resumo da tela inicial. */
data class HomeDeparture(
    val headsign: String,
    val departure: Departure?,
    /** Só quando não há [departure]: o último de hoje e o primeiro do próximo dia. */
    val dayEnd: DayEnd? = null,
    /** `false`: o Metrofor não publicou hoje os horários deste sentido. */
    val published: Boolean = true,
)

/**
 * Qual estação a tela inicial destaca no topo.
 *
 * As partidas ficam de fora de propósito: elas dependem do relógio, que anda
 * sozinho, então são recalculadas na renderização em vez de viverem no estado.
 */
sealed interface HomeStation {

    /** Ainda não pedimos nada: a pessoa decide se quer usar a localização. */
    data object Idle : HomeStation

    data object Locating : HomeStation

    /** Achada por GPS; guarda também onde a pessoa estava. */
    data class Nearby(
        val station: Station,
        val distanceMeters: Double,
        val userLat: Double,
        val userLon: Double,
        /** As estações mais próximas das outras linhas, a até 3 km. */
        val others: List<Pair<Station, Double>> = emptyList(),
    ) : HomeStation

    /** Escolhida à mão pela pessoa. */
    data class Pinned(val station: Station) : HomeStation

    data class Unavailable(val reason: Reason) : HomeStation

    enum class Reason { PERMISSION_DENIED, NO_FIX }
}

/** A estação em destaque, se houver uma. */
val HomeStation.station: Station?
    get() = when (this) {
        is HomeStation.Nearby -> station
        is HomeStation.Pinned -> station
        else -> null
    }
