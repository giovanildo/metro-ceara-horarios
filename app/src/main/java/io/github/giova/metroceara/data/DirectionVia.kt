package io.github.giova.metroceara.data

/**
 * Por onde passa um sentido a partir de uma estação, para quem não sabe qual é
 * a ponta da linha: "Passa por Padre Cícero, Porangabussu, Parangaba,
 * Mondubim… até Carlito Benevides · 16 estações".
 *
 * [shown]: as primeiras estações do caminho (sem a última); [more]: há mais
 * estações depois delas; [end]: a última; [count]: quantas estações faltam
 * até a última, contando ela.
 */
data class DirectionVia(
    val shown: List<String>,
    val more: Boolean,
    val end: String,
    val count: Int,
)

/** Quantas estações do caminho aparecem pelo nome. */
const val VIA_SHOWN = 4

/**
 * O caminho de [stopId] até o fim de [stops] (as estações do sentido, em
 * ordem), ou `null` se a estação não está no sentido ou já é a última.
 */
fun directionVia(stops: List<Stop>, stopId: String): DirectionVia? {
    val index = stops.indexOfFirst { it.id == stopId }
    if (index < 0) return null
    val ahead = stops.drop(index + 1).map { it.name }
    if (ahead.isEmpty()) return null
    val middle = ahead.dropLast(1)
    return DirectionVia(
        shown = middle.take(VIA_SHOWN),
        more = middle.size > VIA_SHOWN,
        end = ahead.last(),
        count = ahead.size,
    )
}
