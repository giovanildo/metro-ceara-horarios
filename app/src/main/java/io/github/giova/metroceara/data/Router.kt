package io.github.giova.metroceara.data

/** Uma viagem de um trem entre duas estações da mesma linha: partida e chegada, em minutos. */
data class LegTrip(val tripId: String, val directionId: Int, val departure: Int, val arrival: Int)

/** De onde vêm os horários do roteiro (o banco do dia, ou uma grade fixa nos testes). */
interface Timetable {
    /** As viagens de [routeId] que passam por [fromStop] e depois por [toStop], pela partida. */
    fun trips(routeId: String, fromStop: String, toStop: String): List<LegTrip>
}

/** Onde dá para trocar de linha: da estação [fromStop] da linha [fromRoute] a pé até [toStop] da [toRoute]. */
data class TransferLink(
    val fromRoute: String,
    val fromStop: String,
    val toRoute: String,
    val toStop: String,
    /** Tempo a pé medido na estação, antes de perfil e pico. */
    val baseMinutes: Int,
)

/** Uma estação numa linha: o ponto de partida ou de chegada do roteiro. */
data class RoutePoint(val routeId: String, val stopId: String)

/** Um trecho do roteiro, num trem só. */
data class ItineraryLeg(
    val routeId: String,
    val directionId: Int,
    val tripId: String,
    val fromStop: String,
    val toStop: String,
    val departure: Int,
    val arrival: Int,
)

/** A troca de linha entre dois trechos. */
data class ItineraryTransfer(
    val link: TransferLink,
    /** Quanto a pessoa precisa para trocar (perfil, pico e ajuste já somados). */
    val needMinutes: Int,
    /** Quanto sobra até o próximo trem sair, além do que ela precisa. */
    val slackMinutes: Int,
    /** O trem seguinte, se ela perder este; `null` se era o último. */
    val nextDeparture: Int?,
) {
    /** Sobra 1 minuto ou menos: dá, mas sem margem. */
    val tight: Boolean get() = slackMinutes <= TIGHT_MINUTES

    companion object {
        const val TIGHT_MINUTES = 1
    }
}

/**
 * Um roteiro: os trechos, as integrações entre eles e, se um trem anterior
 * chega no mesmo horário, a partida dele ([sameArrivalFrom]), para a dica
 * "o metrô das 07:40 chega no mesmo horário".
 */
data class Itinerary(
    val legs: List<ItineraryLeg>,
    val transfers: List<ItineraryTransfer>,
    val sameArrivalFrom: Int? = null,
) {
    val departure: Int get() = legs.first().departure
    val arrival: Int get() = legs.last().arrival
}

/**
 * Monta roteiros entre linhas. A rede é pequena (um caminho por par de linhas,
 * com até [MAX_TRANSFERS] integrações), então testa cada trem do primeiro
 * trecho e segue a pé e no próximo trem de cada integração.
 *
 * [need] diz quantos minutos a pessoa precisa numa integração que ela alcança
 * no horário dado: o tempo da estação vezes o perfil, mais o pico.
 */
class Router(
    private val timetable: Timetable,
    private val links: List<TransferLink>,
    private val need: (TransferLink, Int) -> Int,
) {
    private val cache = mutableMapOf<Triple<String, String, String>, List<LegTrip>>()

    // A tela e o cálculo das chegadas (em segundo plano) usam o mesmo Router.
    private fun trips(routeId: String, from: String, to: String) = synchronized(cache) {
        cache.getOrPut(Triple(routeId, from, to)) { timetable.trips(routeId, from, to) }
    }

    /** As integrações do caminho mais curto de [routeId] até [target] (vazio = mesma linha; `null` = não chega). */
    fun via(routeId: String, target: String): List<TransferLink>? = paths(routeId, target).minByOrNull { it.size }

    /** As linhas que dá para alcançar a partir de [routeId], com integração. */
    fun reachable(routeId: String): Set<String> = paths(routeId, null).map { it.lastOrNull()?.toRoute ?: routeId }.toSet()

    /**
     * Os roteiros de [origin] a [destination] saindo a partir de [from], um por
     * horário de chegada, o primeiro sendo o que chega mais cedo. Cada um sai
     * o mais tarde possível para aquela chegada, sem deixar a integração justa
     * se um trem anterior chega igual com folga.
     */
    fun plan(origin: RoutePoint, destination: RoutePoint, from: Int, limit: Int = 4, avoid: Set<String> = emptySet()): List<Itinerary> {
        val candidates = paths(origin.routeId, destination.routeId)
            .filter { path -> path.none { it.toRoute in avoid } && origin.routeId !in avoid }
            .flatMap { path -> simulate(origin, destination, path, from) }
        return candidates.groupBy { it.arrival }
            .toSortedMap()
            .values
            .map { same -> pick(same) }
            .take(limit)
    }

    /** O último horário do dia que ainda leva ao destino, ou `null` se nenhum leva. */
    fun lastDeparture(origin: RoutePoint, destination: RoutePoint, avoid: Set<String> = emptySet()): Int? =
        paths(origin.routeId, destination.routeId)
            .filter { path -> path.none { it.toRoute in avoid } && origin.routeId !in avoid }
            .flatMap { simulate(origin, destination, it, 0) }
            .maxOfOrNull { it.departure }

    private fun pick(same: List<Itinerary>): Itinerary {
        val byDeparture = same.sortedBy { it.departure }
        val comfortable = byDeparture.lastOrNull { it.transfers.none(ItineraryTransfer::tight) }
        val chosen = comfortable ?: byDeparture.last()
        val earlier = byDeparture.first().departure
        return chosen.copy(sameArrivalFrom = earlier.takeIf { it < chosen.departure })
    }

    /** Cada trem do primeiro trecho, seguido do primeiro trem possível em cada integração. */
    private fun simulate(origin: RoutePoint, destination: RoutePoint, path: List<TransferLink>, from: Int): List<Itinerary> {
        // Trechos: (linha, embarque, desembarque). Um trecho vazio (a pessoa já está
        // na estação de integração) é pulado.
        val segments = buildList {
            var route = origin.routeId
            var stop = origin.stopId
            path.forEach { link ->
                add(Triple(route, stop, link.fromStop))
                route = link.toRoute
                stop = link.toStop
            }
            add(Triple(route, stop, destination.stopId))
        }
        val first = segments.first()
        val firstTrips = if (first.second == first.third) listOf(null) else trips(first.first, first.second, first.third)
            .filter { it.departure >= from }
        return firstTrips.mapNotNull { firstTrip ->
            val legs = mutableListOf<ItineraryLeg>()
            val transfers = mutableListOf<ItineraryTransfer>()
            var clock = from
            if (firstTrip != null) {
                legs += ItineraryLeg(first.first, firstTrip.directionId, firstTrip.tripId, first.second, first.third,
                    firstTrip.departure, firstTrip.arrival)
                clock = firstTrip.arrival
            }
            for (i in 1 until segments.size) {
                val (route, board, alight) = segments[i]
                val link = path[i - 1]
                val needed = need(link, clock)
                val options = trips(route, board, alight)
                val index = options.indexOfFirst { it.departure >= clock + needed }
                if (index < 0) return@mapNotNull null
                val trip = options[index]
                // Sem trecho antes (já estava na integração), não há troca a mostrar.
                if (legs.isNotEmpty()) {
                    transfers += ItineraryTransfer(link, needed, trip.departure - clock - needed,
                        options.getOrNull(index + 1)?.departure)
                }
                legs += ItineraryLeg(route, trip.directionId, trip.tripId, board, alight, trip.departure, trip.arrival)
                clock = trip.arrival
            }
            Itinerary(legs, transfers).takeIf { legs.isNotEmpty() }
        }
    }

    /** Os caminhos de linha em linha a partir de [start] (até [target], ou todos se `null`). */
    private fun paths(start: String, target: String?): List<List<TransferLink>> {
        val found = mutableListOf<List<TransferLink>>()
        fun walk(route: String, path: List<TransferLink>, seen: Set<String>) {
            if (target == null || route == target) {
                found += path
                if (target != null) return
            }
            if (path.size == MAX_TRANSFERS) return
            links.filter { it.fromRoute == route && it.toRoute !in seen }
                .distinctBy { it.toRoute to it.fromStop }
                .forEach { walk(it.toRoute, path + it, seen + it.toRoute) }
        }
        walk(start, emptyList(), setOf(start))
        return found
    }

    companion object {
        /** Caucaia → Aeroporto: Chico da Silva, Parangaba e Expedicionários. */
        const val MAX_TRANSFERS = 3
    }
}
