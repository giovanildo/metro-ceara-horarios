package io.github.giova.metroceara.data

import java.text.Normalizer
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot

/**
 * Um lugar que a pessoa procura em vez da estação ("Rodoviária", "HGF"), do
 * regras.json: a estação mais perto a pé ([station], nome sem o " - NE") e a
 * caminhada até ela pelas ruas ([walkMeters]), calculadas pelo tools/lugares.py.
 */
data class Landmark(
    val name: String,
    val type: String,
    /** Apelidos, sem acento ("hgf", "rodoviaria joao thome"). */
    val search: String,
    val district: String,
    val city: String,
    val lat: Double,
    val lon: Double,
    val station: String,
    val walkMeters: Int,
) {
    /** Longe demais para ir a pé da estação: o app diz isso em vez de montar o roteiro. */
    val far get() = walkMeters > FAR_METERS

    /** Colado na estação ("ao lado da estação"). */
    val beside get() = walkMeters <= BESIDE_METERS

    private val haystack by lazy { plainText("$name $search") }

    /** Todas as palavras de [needle] (já sem acento) aparecem no nome ou nos apelidos? */
    fun matches(needle: List<String>): Boolean = needle.all { haystack.contains(it) }

    /** O nome começa pela busca: vem antes na lista. */
    fun startsWith(needle: String): Boolean = plainText(name).startsWith(needle)

    companion object {
        const val FAR_METERS = 2000
        const val BESIDE_METERS = 150
        /** Em "Perto desta estação", só o que dá uma caminhada curta. */
        const val NEAR_METERS = 1000
        const val WALK_METERS_PER_MINUTE = 75.0
    }
}

/** Os minutos a pé até o lugar, no passo da pessoa ([factor] de Configurações > Como você se desloca). */
fun Landmark.walkMinutes(factor: Double): Int =
    ceil(walkMeters / Landmark.WALK_METERS_PER_MINUTE * factor - 1e-9).toInt().coerceAtLeast(1)

/**
 * Os lugares que batem com [query] (sem acento, cada palavra em qualquer ordem),
 * os que começam pela busca primeiro, no máximo [limit].
 */
fun List<Landmark>.search(query: String, limit: Int = 8): List<Landmark> {
    val needle = plainText(query)
    if (needle.length < 2) return emptyList()
    val words = needle.split(Regex("\\s+"))
    return filter { it.matches(words) }
        .sortedWith(compareBy<Landmark> { !it.startsWith(needle) }.thenBy { it.far }.thenBy { it.name })
        .take(limit)
}

/**
 * Os lugares a uma caminhada curta da estação [stationName] em ([lat], [lon]), do
 * mais perto ao mais longe. A posição separa estações de mesmo nome (Padre Cícero
 * em Fortaleza e no Cariri).
 */
fun List<Landmark>.near(stationName: String, lat: Double, lon: Double): List<Landmark> {
    val name = plainText(JourneyPlanner.plainName(stationName))
    return filter { plainText(it.station) == name && it.walkMeters <= Landmark.NEAR_METERS && it.isNear(lat, lon) }
        .sortedBy { it.walkMeters }
}

/**
 * A estação [stationName] em ([lat], [lon]) é a que serve o lugar? Mesmo nome e
 * perto dele (até onde vai a caminhada), para não confundir cidades.
 */
fun Landmark.servedBy(stationName: String, lat: Double, lon: Double): Boolean =
    plainText(JourneyPlanner.plainName(stationName)) == plainText(station) &&
        isNear(lat, lon, maxOf(3000.0, walkMeters + 1000.0))

private fun Landmark.isNear(lat: Double, lon: Double, meters: Double = 3000.0): Boolean {
    val x = Math.toRadians(lon - this.lon) * cos(Math.toRadians((lat + this.lat) / 2))
    val y = Math.toRadians(lat - this.lat)
    return 6_371_000 * hypot(x, y) <= meters
}

/** "José de Alencar" e "jose de alencar" viram o mesmo texto, para a busca. */
internal fun plainText(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
