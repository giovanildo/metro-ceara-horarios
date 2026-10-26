package io.github.giova.metroceara.data

/**
 * Monta o modo viagem de um roteiro com integração: o plano do primeiro trecho
 * (com o aviso da integração no fim) e o roteiro inteiro para a linha do tempo.
 * O app usa ao começar a viagem, e o serviço do modo viagem, ao chegar a uma
 * integração ou quando a pessoa perde o trem: o próximo trecho sai daqui.
 */
object JourneyPlanner {

    fun router(repo: ScheduleRepository, settings: AppSettings) =
        Router(repo.timetable, repo.transferLinks()) { link, arrival -> transferMinutes(link.baseMinutes, arrival, settings) }

    /** "Parangaba - NE", "Expedicionarios - AE": o sufixo da grade só separa as plataformas. */
    fun plainName(name: String) = name.replace(Regex("\\s+-\\s+(AE|NE)$", RegexOption.IGNORE_CASE), "")

    /**
     * O plano do primeiro trecho de [itinerary]. [done]: os trechos já feitos
     * (aparecem na linha do tempo antes dos novos). `null` se a grade não tem
     * as estações do trecho.
     */
    fun plan(
        repo: ScheduleRepository,
        itinerary: Itinerary,
        settings: AppSettings,
        simulated: Boolean,
        done: List<JourneyLeg> = emptyList(),
    ): TripPlan? {
        fun stopName(id: String) = plainName(repo.stop(id)?.name ?: id)
        fun headsign(routeId: String, direction: Int) =
            repo.directions(routeId).firstOrNull { it.id == direction }?.headsign.orEmpty()
        val legs = itinerary.legs.map { leg ->
            val route = repo.route(leg.routeId)
            JourneyLeg(route?.name.orEmpty(), route?.colorHex.orEmpty(), headsign(leg.routeId, leg.directionId),
                stopName(leg.fromStop), stopName(leg.toStop), leg.departure, leg.arrival)
        }
        val first = itinerary.legs.first()
        val route = repo.route(first.routeId) ?: return null
        val stops = repo.tripStops(first.routeId, first.directionId, first.fromStop, first.toStop,
            repo.tripTimes(first.tripId, first.routeId))?.takeIf { it.size >= 2 }
            ?.map { it.copy(name = plainName(it.name)) } ?: return null
        val last = itinerary.legs.last()
        val finalName = legs.last().toName
        val transfer = itinerary.transfers.firstOrNull()?.let { t ->
            val next = legs[1]
            // A regra de passagem usa o nome da grade, com o sufixo.
            val fare = transferRule(repo.stop(t.link.fromStop)?.name.orEmpty(), repo.stop(t.link.toStop)?.name.orEmpty()).fare
            TripTransfer(next.routeName, next.headsign, next.departure, t.needMinutes, finalName,
                when (fare) { TransferFare.FREE -> true; TransferFare.PAID -> false; TransferFare.UNKNOWN -> null })
        }
        return TripPlan(
            first.routeId, route.name, legs.first().headsign, stops, settings.alertStationsBefore,
            transfer = transfer,
            simulated = simulated,
            journey = TripJourney(last.routeId, last.toStop, finalName, done + legs, current = done.size)
                .takeIf { it.legs.size > 1 },
        )
    }
}
